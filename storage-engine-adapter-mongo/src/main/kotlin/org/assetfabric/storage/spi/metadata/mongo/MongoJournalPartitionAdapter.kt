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

import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.spi.JournalEntryNodeRepresentation
import org.assetfabric.storage.spi.metadata.JournalPartitionAdapter
import org.assetfabric.storage.spi.metadata.mongo.MongoTemplateConfiguration.MongoTemplateConfiguration.JOURNAL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
@ConditionalOnProperty("assetfabric.storage.metadata.adapter.type", havingValue = "mongo")
class MongoJournalPartitionAdapter: JournalPartitionAdapter {

    private val log = LogManager.getLogger(MongoJournalPartitionAdapter::class.java)

    @Value("\${assetfabric.storage.metadata.adapter.mongo.collection.journal:journal}")
    private lateinit var journalCollectionName: String

    @Autowired
    @Qualifier(JOURNAL)
    private lateinit var provider: MongoTemplateProvider

    override fun createJournalEntrySet(nodeRepresentations: Flux<JournalEntryNodeRepresentation>): Mono<Boolean> {
        val createdPaths = mutableListOf<Path>()
        var count = 0
        return nodeRepresentations.doOnNext { representation ->
            log.info("Adding journal entry")
            if (createdPaths.contains(representation.path())) {
                throw RuntimeException("Path ${representation.path()} already created")
            } else {
                count++
            }
        }.flatMap { representation ->
            createdPaths.add(representation.path())
            provider.template.save(representation, journalCollectionName)
        }.then(Mono.defer { Mono.just(count > 0) })
    }

    override fun getNextJournalRevision(): Mono<RevisionNumber> {
        log.debug("Searching for next journal entry set")
        val query = Query()
                .with(Sort(Sort.Direction.ASC, "revision"))
                .limit(1)
        val reprMono = provider.template.findOne(query, JournalEntryNodeRepresentation::class.java, journalCollectionName)
        return reprMono.map { representation -> representation.revision() }
    }

    override fun getJournalEntrySet(revision: RevisionNumber): Flux<JournalEntryNodeRepresentation> {
        val nodesForRevisionQuery = Query().addCriteria(Criteria.where("revision").`is`(revision.toString()))
        return provider.template.find(nodesForRevisionQuery, JournalEntryNodeRepresentation::class.java, journalCollectionName)
    }

    override fun removeJournalEntrySet(revision: RevisionNumber): Mono<Void> {
        val query = Query().addCriteria(Criteria.where("revision").`is`(revision.toString()))
        return provider.template.remove(query, JournalEntryNodeRepresentation::class.java, journalCollectionName).doOnTerminate {
            log.debug("Removed journal entry set for revision $revision")
        }.then()
    }

    override fun reset(): Mono<Void> {
        val delete = provider.template.remove(Query(), journalCollectionName).map { result ->
            log.debug("Removed ${result.deletedCount} documents")
        }

        return delete.then()
    }
}