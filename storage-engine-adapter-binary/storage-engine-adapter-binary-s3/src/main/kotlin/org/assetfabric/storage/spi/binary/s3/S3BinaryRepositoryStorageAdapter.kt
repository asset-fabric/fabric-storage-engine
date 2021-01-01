/*
 * Copyright (C) 2019 Asset Fabric contributors (https://github.com/orgs/asset-fabric/teams/asset-fabric-contributors)
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.assetfabric.storage.spi.binary.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.InputStreamWithLength
import org.assetfabric.storage.spi.binary.BinaryRepositoryStorageAdapter
import org.assetfabric.storage.spi.binary.FileInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.InputStream
import java.util.UUID

@Component
@ConditionalOnProperty("assetfabric.storage.binary.adapter.type", havingValue = "s3")
class S3BinaryRepositoryStorageAdapter: BinaryRepositoryStorageAdapter {

    private val log = LogManager.getLogger(S3BinaryRepositoryStorageAdapter::class.java)

    @Value("\${assetfabric.storage.binary.adapter.s3.bucketName:fabric}")
    private lateinit var bucketName: String

    @Value("\${assetfabric.storage.binary.adapter.s3.tempPrefix:temp}")
    private lateinit var tempLocationPrefix: String

    @Value("\${assetfabric.storage.binary.adapter.s3.permanentPrefix:permanent}")
    private lateinit var permanentLocationPrefix: String

    @Autowired
    private lateinit var s3: AmazonS3

    override fun createTempFile(streamWithLength: InputStreamWithLength): FileInfo {
        log.debug("Writing temp file")

        // find a unique temp file location for the input stream
        var tempLocation: String
        do {
            tempLocation = UUID.randomUUID().toString()
            log.debug("Checking temp location $tempLocation")
        } while (s3.doesObjectExist(bucketName, "$tempLocationPrefix/$tempLocation"))

        // write the data to that location and return it
        val metadata = ObjectMetadata()
        metadata.contentLength = streamWithLength.length
        val objectPath = "$tempLocationPrefix/$tempLocation"
        log.debug("Putting S3 object $objectPath")

        s3.putObject(bucketName, objectPath, streamWithLength.inputStream, metadata)
        log.debug("Put complete")
        val objectMetadata = s3.getObjectMetadata(bucketName, objectPath)
        log.debug("Metadata acquired")
        return FileInfo(tempLocation, objectMetadata.contentLength)
    }

    override fun inputStreamForTemporaryLocation(location: String): InputStream {
        return s3.getObject(bucketName, "$tempLocationPrefix/$location").objectContent
    }

    override fun temporaryFileExists(location: String): Boolean {
        return s3.doesObjectExist(bucketName, "$tempLocationPrefix/$location")
    }

    override fun deleteTempFile(path: String) {
        s3.deleteObject(bucketName, "$tempLocationPrefix/$path")
    }

    override fun permanentFilesWithHashPrefix(hashPrefix: String): List<FileInfo> {
        val request = ListObjectsRequest().withBucketName(bucketName).withPrefix("$permanentLocationPrefix/$hashPrefix")
        return s3.listObjects(request).objectSummaries.map { FileInfo(it.key.substring(permanentLocationPrefix.length + 1), it.size) }
    }

    override fun permanentLocationExists(location: String): Boolean {
        val normalizedLocation = when(location.startsWith("/")) {
            true -> location
            false -> "/$location"
        }
        return s3.doesObjectExist(bucketName, "$permanentLocationPrefix$normalizedLocation")
    }

    override fun inputStreamForPermanentLocation(location: String): InputStream {
        val normalizedLocation = when(location.startsWith("/")) {
            true -> location
            false -> "/$location"
        }
        return s3.getObject(bucketName, "$permanentLocationPrefix$normalizedLocation").objectContent
    }

    override fun moveTempFileToPermanentLocation(tempPath: String, targetLocation: String) {
        s3.copyObject(bucketName, "$tempLocationPrefix/$tempPath", bucketName, "$permanentLocationPrefix/$targetLocation")
        deleteTempFile(tempPath)
    }
}