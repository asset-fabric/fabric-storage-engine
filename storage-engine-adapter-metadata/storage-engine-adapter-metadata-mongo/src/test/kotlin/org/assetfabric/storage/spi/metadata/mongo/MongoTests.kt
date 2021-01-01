package org.assetfabric.storage.spi.metadata.mongo

import org.assetfabric.storage.spi.metadata.test.DataPartitionAdapterTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.runner.JUnitPlatform
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.SelectPackages
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.support.AnnotationConfigContextLoader

@ExtendWith(SpringExtension::class)
@DirtiesContext
@ContextConfiguration(loader = AnnotationConfigContextLoader::class)
@TestPropertySource(properties = ["assetfabric.storage.metadata.adapter.type=mongo", "assetfabric.storage.metadata.adapter.mongo=embedded"])
@SelectClasses(DataPartitionAdapterTest::class)
class MongoTests {

    @Configuration
    @Import(MongoTemplateConfiguration::class, MongoClientFactory::class,
            MongoDataPartitionAdapter::class, MongoCommittedNodeIndexPartitionAdapter::class, MongoJournalPartitionAdapter::class)
    internal class Config

}