package org.assetfabric.storage.spi.binary.block

import org.assetfabric.storage.InputStreamWithLength
import org.assetfabric.storage.spi.binary.test.BinaryManagerServiceStorageAdapterTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.AnnotationConfigContextLoader
import java.io.ByteArrayInputStream

@ExtendWith(SpringExtension::class)
@ContextConfiguration(loader = AnnotationConfigContextLoader::class)
@TestPropertySource(properties = [
    "assetfabric.storage.binary.adapter.type=block",
    "assetfabric.storage.binary.adapter.block.folder=target/assetfabric"
])
@DisplayName("a block binary repository storage adapter")
class BlockBinaryRepositoryStorageAdapterTest : BinaryManagerServiceStorageAdapterTest() {

    @Configuration
    @Import(BlockBinaryRepositoryStorageAdapter::class)
    internal class Config

    override fun prepareCreateFileResponse() {

    }

    override fun prepareTempFile(): String {
        val iswl = InputStreamWithLength(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)), 5)
        return storageAdapter.createTempFile(iswl).path
    }

    override fun prepareTemporaryInputStreamResponse(): String {
        return prepareTempFile()
    }

    override fun prepareFileListing(paths: List<String>) {
        paths.forEach { path ->
            val tempFile = prepareTempFile()
            storageAdapter.moveTempFileToPermanentLocation(tempFile, path)
        }
    }

    override fun preparePermanentInputStreamResponse(path: String) {
        val iswl = InputStreamWithLength(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)), 5)
        val fileInfo = storageAdapter.createTempFile(iswl)
        storageAdapter.moveTempFileToPermanentLocation(fileInfo.path, path)
    }

    override fun prepareFileMove(): String {
        val iswl = InputStreamWithLength(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)), 5)
        return storageAdapter.createTempFile(iswl).path
    }

    override fun verifyFileMove(tempPath: String, permPath: String) {

    }

}