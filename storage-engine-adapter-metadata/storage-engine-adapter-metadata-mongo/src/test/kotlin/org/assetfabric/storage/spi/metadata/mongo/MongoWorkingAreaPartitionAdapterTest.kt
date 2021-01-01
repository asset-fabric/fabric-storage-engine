package org.assetfabric.storage.spi.metadata.mongo

import org.assetfabric.storage.spi.metadata.test.WorkingAreaPartitionAdapterTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.AnnotationConfigContextLoader

@ExtendWith(SpringExtension::class)
@DirtiesContext
@ContextConfiguration(loader = AnnotationConfigContextLoader::class)
@TestPropertySource(properties = ["assetfabric.storage.metadata.adapter.type=mongo", "assetfabric.storage.metadata.adapter.mongo=embedded", "spring.data.mongodb.port=27017"])
@DisplayName("A Mongo working area partition adapter")
class MongoWorkingAreaPartitionAdapterTest: WorkingAreaPartitionAdapterTest() {

    @Configuration
    @Import(MongoTemplateConfiguration::class, MongoWorkingAreaPartitionAdapter::class, EmbeddedMongoAutoConfiguration::class, MongoClientFactory::class)
    internal class Config

}