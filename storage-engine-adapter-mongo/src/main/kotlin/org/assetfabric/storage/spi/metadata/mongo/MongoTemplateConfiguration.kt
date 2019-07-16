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

package org.assetfabric.storage.spi.metadata.mongo

import com.mongodb.reactivestreams.client.MongoClient
import org.assetfabric.storage.spi.metadata.mongo.converter.RevisionedNodeRepresentationReadConverter
import org.assetfabric.storage.spi.metadata.mongo.converter.RevisionedNodeRepresentationWriteConverter
import org.assetfabric.storage.spi.metadata.mongo.converter.WorkingAreaNodeRepresentationReadConverter
import org.assetfabric.storage.spi.metadata.mongo.converter.WorkingAreaNodeRepresentationWriteConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

@Configuration
@ConditionalOnProperty("assetfabric.storage.metadata.adapter.type", havingValue = "mongo")
class MongoTemplateConfiguration {

    companion object MongoTemplateConfiguration {

        const val DATA_PARTITION = "dataPartition"

        const val WORKING_AREA = "workingArea"

        const val JOURNAL = "journal"

        const val CATALOG = "catalog"

    }

    @Value("\${assetfabric.storage.metadata.adapter.mongo.databaseName:assetfabric}")
    private lateinit var dbName: String

    @Autowired
    private lateinit var client: MongoClient

    @Bean
    @Qualifier(CATALOG)
    fun createCatalogTemplate(): MongoTemplateProvider {
        return MongoTemplateProvider(ReactiveMongoTemplate(client, dbName))
    }

    @Bean
    @Qualifier(WORKING_AREA)
    fun createWorkingAreaTemplate(): MongoTemplateProvider {
        return createTemplate(listOf(WorkingAreaNodeRepresentationReadConverter(), WorkingAreaNodeRepresentationWriteConverter()))
    }

    @Bean
    @Qualifier(JOURNAL)
    fun createJournalTemplate(): MongoTemplateProvider {
        return createTemplate(listOf(RevisionedNodeRepresentationReadConverter(), RevisionedNodeRepresentationWriteConverter()))
    }

    @Bean
    @Qualifier(DATA_PARTITION)
    fun createDataPartitionTemplate(): MongoTemplateProvider {
        return createTemplate(listOf(RevisionedNodeRepresentationReadConverter(), RevisionedNodeRepresentationWriteConverter()))
    }

    private fun createTemplate(converters: List<*>): MongoTemplateProvider {
        val dbFactory = SimpleReactiveMongoDatabaseFactory(client, dbName)

        val mongoConversions = MongoCustomConversions(converters)
        val template = ReactiveMongoTemplate(dbFactory)
        (template.converter as MappingMongoConverter).setCustomConversions(mongoConversions)
        (template.converter as MappingMongoConverter).afterPropertiesSet()
        return MongoTemplateProvider(template)
    }


}