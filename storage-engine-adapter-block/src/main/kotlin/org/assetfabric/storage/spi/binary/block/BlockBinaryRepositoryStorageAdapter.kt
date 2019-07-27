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

package org.assetfabric.storage.spi.binary.block

import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.InputStreamWithLength
import org.assetfabric.storage.spi.FileInfo
import org.assetfabric.storage.spi.binary.BinaryRepositoryStorageAdapter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.annotation.PostConstruct

@Component
@ConditionalOnProperty("assetfabric.storage.binary.adapter.type", havingValue = "block")
class BlockBinaryRepositoryStorageAdapter: BinaryRepositoryStorageAdapter {

    private val log = LogManager.getLogger(BlockBinaryRepositoryStorageAdapter::class.java)

    @Value("\${assetfabric.storage.binary.adapter.block.folder:assetfabric}")
    private lateinit var topLevelFolderPath: String

    @Value("\${assetfabric.storage.binary.adapter.block.tempFolder:temp}")
    private lateinit var tempFolderName: String

    @Value("\${assetfabric.storage.binary.adapter.block.permanentFolder:permanent}")
    private lateinit var permanentFolderName: String

    private lateinit var tempFolder: File

    private lateinit var permanentFolder: File

    @PostConstruct
    private fun init() {
        val topLevelFile = File(topLevelFolderPath)
        if (!topLevelFile.exists()) {
            topLevelFile.mkdirs()
            log.debug("Top-level block storage created")
        }

        val tempFile = File(topLevelFile, tempFolderName)
        if (!tempFile.exists()) {
            tempFile.mkdirs()
            log.debug("Temp block storage created")
        }
        tempFolder = tempFile

        val permFile = File(topLevelFile, permanentFolderName)
        if (!permFile.exists()) {
            permFile.mkdirs()
            log.debug("Permanent block storage created")
        }
        permanentFolder = permFile

    }

    fun getTempFolder(): File = tempFolder

    fun getPermanentFolder(): File = permanentFolder

    override fun createTempFile(streamWithLength: InputStreamWithLength): FileInfo {
        // find a unique temp file location for the input stream
        var tempLocation: String
        do {
            tempLocation = UUID.randomUUID().toString()
            log.debug("Checking temp location $tempLocation")
        } while (File(tempFolder, tempLocation).exists())

        val outFile = File(tempFolder, tempLocation)
        val outStream = FileOutputStream(outFile)
        outStream.use {
            IOUtils.copyLarge(streamWithLength.inputStream, outStream)
        }
        return FileInfo(tempLocation, outFile.length())
    }

    override fun permanentLocationExists(location: String): Boolean {
        return File(tempFolder, location).exists()
    }

    override fun inputStreamForTemporaryLocation(location: String): InputStream {
        return FileInputStream(File(tempFolder, location))
    }

    override fun deleteTempFile(path: String) {
        val tempFile = File(tempFolder, path)
        val deleted = tempFile.delete()
        if (!deleted) throw RuntimeException("Failed to delete file ${tempFile.absolutePath}")
    }

    override fun permanentFilesWithHashPrefix(hashPrefix: String): List<FileInfo> {
        val prefixFolder = File(permanentFolder, hashPrefix)
        val files: Array<File>? = prefixFolder.listFiles()
        return when(files) {
            null -> listOf()
            else -> return files.map {
                FileInfo("$hashPrefix/${it.name}", it.length())
            }
        }
    }

    override fun inputStreamForPermanentLocation(location: String): InputStream {
        val normalizedLocation = when(location.startsWith("/")) {
            true -> location.substring(1)
            false -> location
        }
        val file = File(permanentFolder, normalizedLocation)
        return FileInputStream(file)
    }

    override fun moveTempFileToPermanentLocation(tempPath: String, targetLocation: String) {
        val tempFile = File(tempFolder, tempPath)
        val permFile = File(permanentFolder, targetLocation)
        val permFileParent = permFile.parentFile
        permFileParent.mkdirs()
        FileInputStream(tempFile).use { inStream ->
            FileOutputStream(permFile).use { outStream ->
                IOUtils.copyLarge(inStream, outStream)
            }
        }
        deleteTempFile(tempPath)
    }
}