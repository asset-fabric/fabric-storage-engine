import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectResult
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.reset
import org.assetfabric.storage.spi.binary.s3.S3BinaryRepositoryStorageAdapter
import org.assetfabric.storage.spi.binary.test.BinaryManagerServiceStorageAdapterTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.AnnotationConfigContextLoader
import java.util.UUID

@ExtendWith(SpringExtension::class)
@ContextConfiguration(loader = AnnotationConfigContextLoader::class)
@TestPropertySource(properties = [
    "assetfabric.storage.binary.adapter.type=s3",
    "assetfabric.storage.binary.adapter.s3.tempPrefix=tempo",
    "assetfabric.storage.binary.adapter.s3.bucketName=assetfabric",
    "assetfabric.storage.binary.adapter.s3.permanentPrefix=perm"])
@DisplayName("an S3 binary repository storage adapter")
class S3BinaryManagerServiceStorageAdapterTest: BinaryManagerServiceStorageAdapterTest() {

    @Configuration
    @Import(S3BinaryRepositoryStorageAdapter::class)
    internal class Config

    @MockBean
    lateinit var s3: AmazonS3

    @Value("\${assetfabric.storage.binary.adapter.s3.bucketName}")
    lateinit var bucketName: String

    @Value("\${assetfabric.storage.binary.adapter.s3.tempPrefix}")
    lateinit var tempPrefix: String

    @Value("\${assetfabric.storage.binary.adapter.s3.permanentPrefix}")
    lateinit var permanentPrefix: String

    @BeforeEach
    private fun init() {
        reset(s3)
    }

    override fun prepareCreateFileResponse() {
        val mockResult = mock(PutObjectResult::class.java)
        val mockMetadata = mock(ObjectMetadata::class.java)
        `when`(mockResult.metadata).thenReturn(mockMetadata)
        `when`(mockMetadata.contentLength).thenReturn(3)
        `when`(s3.getObjectMetadata(any(), any())).thenReturn(mockMetadata)
        `when`(s3.putObject(any(), any(), any(), eq(null))).thenReturn(mockResult)
    }

    override fun prepareTemporaryInputStreamResponse(): String {
        val location = UUID.randomUUID().toString().replace("-", "")
        val s3Object = mock(S3Object::class.java)
        val mockStream = mock(S3ObjectInputStream::class.java)
        `when`(s3Object.objectContent).thenReturn(mockStream)
        `when`(s3.getObject(eq(bucketName), eq("$tempPrefix/$location"))).thenReturn(s3Object)
        return location
    }

    override fun prepareTempFile(): String {
        return prepareTemporaryInputStreamResponse()
    }

    override fun prepareFileMove(): String {
        return prepareTempFile()
    }

    override fun prepareFileListing(paths: List<String>) {
        val mockListing = mock(ObjectListing::class.java)
        val summaryList = paths.map {
            val summary = mock(S3ObjectSummary::class.java)
            `when`(summary.key).thenReturn("$permanentPrefix/$it")
            `when`(summary.size).thenReturn(10)
            summary
        }
        `when`(mockListing.objectSummaries).thenReturn(summaryList)
        `when`(s3.listObjects(any<ListObjectsRequest>())).thenReturn(mockListing)
    }

    override fun preparePermanentInputStreamResponse(path: String) {
        val s3Object = mock(S3Object::class.java)
        val mockStream = mock(S3ObjectInputStream::class.java)
        `when`(s3Object.objectContent).thenReturn(mockStream)
        `when`(s3.getObject(eq(bucketName), eq("$permanentPrefix/$path"))).thenReturn(s3Object)
    }

    override fun verifyFileMove(tempPath: String, permPath: String) {
        Mockito.verify(s3).copyObject(eq(bucketName), eq("$tempPrefix/$tempPath"), eq(bucketName), eq("$permanentPrefix/$permPath"))
        Mockito.verify(s3).deleteObject(eq(bucketName), eq("$tempPrefix/$tempPath"))
    }
}