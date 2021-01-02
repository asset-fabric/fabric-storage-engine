package org.assetfabric.storage.spi.search.lucene

import org.assetfabric.storage.spi.search.test.SearchAdapterTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.AnnotationConfigContextLoader

@ExtendWith(SpringExtension::class)
@ContextConfiguration(loader = AnnotationConfigContextLoader::class)
@TestPropertySource(properties = [
    "assetfabric.storage.search.adapter.type=lucene",
    "assetfabric.storage.search.adapter.lucene.dir=target/lucene-index"
])
@DisplayName("a lucene storage adapter")
class LuceneSearchAdapterTest: SearchAdapterTest() {

    @Configuration
    @Import(LuceneSearchAdapter::class)
    internal class Config

    @BeforeEach
    private fun reset() {
        (searchAdapter as LuceneSearchAdapter).reset()
        com.nhaarman.mockito_kotlin.reset(session)
    }

}