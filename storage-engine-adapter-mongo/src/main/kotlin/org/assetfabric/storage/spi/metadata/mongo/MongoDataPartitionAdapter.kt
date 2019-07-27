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
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.State
import org.assetfabric.storage.spi.JournalEntryNodeRepresentation
import org.assetfabric.storage.spi.RevisionedNodeRepresentation
import org.assetfabric.storage.spi.metadata.DataPartitionAdapter
import org.assetfabric.storage.spi.metadata.mongo.MongoTemplateConfiguration.MongoTemplateConfiguration.COMMITTED_DATA_PARTITION
import org.assetfabric.storage.spi.support.DefaultRevisionedNodeRepresentation
import org.bson.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.Fields
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
@ConditionalOnProperty("assetfabric.storage.metadata.adapter.type", havingValue = "mongo")
class MongoDataPartitionAdapter: DataPartitionAdapter {

    private val log = LogManager.getLogger(MongoDataPartitionAdapter::class.java)

    @Value("\${assetfabric.storage.metadata.adapter.mongo.collection.data:data}")
    private lateinit var partitionCollectionName: String

    @Autowired
    @Qualifier(COMMITTED_DATA_PARTITION)
    private lateinit var provider: MongoTemplateProvider

    override fun writeNodeRepresentations(representations: Flux<RevisionedNodeRepresentation>): Mono<Void> {
        return representations.flatMap { repr ->
            provider.template.save(repr, partitionCollectionName).doOnTerminate {
                log.debug("Wrote node ${repr.path()} to data partition at revision ${repr.revision()}")
            }
        }.then()
    }

    override fun writeJournalEntries(entries: Flux<JournalEntryNodeRepresentation>): Mono<Void> {
        val reprFlux = entries.map<RevisionedNodeRepresentation> { journalEntry ->
            DefaultRevisionedNodeRepresentation(journalEntry.path(), journalEntry.revision(), journalEntry.nodeType(), journalEntry.content().properties(), journalEntry.content().state())
        }
        return writeNodeRepresentations(reprFlux)
    }

    override fun nodeRepresentation(revision: RevisionNumber, path: Path): Mono<RevisionedNodeRepresentation> {
        val query = Query()
        val criteria = Criteria().andOperator(
                Criteria.where("path").`is`(path.path),
                Criteria.where("revision").lte(revision.toString()))
        query.addCriteria(criteria)
                .with(Sort(Sort.Direction.DESC, "revision"))
                .limit(1)

       return provider.template.findOne(query, RevisionedNodeRepresentation::class.java, partitionCollectionName)
    }

    override fun nodeChildRepresentations(revision: RevisionNumber, path: Path): Flux<RevisionedNodeRepresentation> {
        // find the nodes with the given parent path
        val criteria = Criteria().andOperator(
                Criteria.where("parentPath").`is`(path.path),
                Criteria.where("revision").lte(revision.toString()))
        val matchStage = Aggregation.match(criteria)

        // sort by path ascending and revision descending (most recent first)
        val sortStage = Aggregation.sort(Sort(Sort.Direction.ASC, "path").and(Sort(Sort.Direction.DESC, "revision")))

        // group the documents by path and then extract the actual documents into the grouped result
        val groupStage = Aggregation.group(Fields.fields("path")).push("\$\$ROOT").`as`("documents")

        // replace the output of each group with the first document in the group (the most recent revision)
        val replaceStage = Aggregation.replaceRoot {
            Document("\$arrayElemAt", listOf("\$documents", 0))
        }

        // sort the documents again because they're losing their sort order after grouping
        val sortStage2 = Aggregation.sort(Sort.Direction.ASC, "path")

        val aggregation = Aggregation.newAggregation(matchStage, sortStage, groupStage, replaceStage, sortStage2)

        return provider.template.aggregate(aggregation, partitionCollectionName, RevisionedNodeRepresentation::class.java)
    }

    override fun reset(): Mono<Void> {
        val createMono = writeNodeRepresentations(Flux.just(DefaultRevisionedNodeRepresentation(Path("/"), RevisionNumber(0), NodeType.UNSTRUCTURED, hashMapOf(), State.NORMAL)))
        return provider.template.remove(Query(), partitionCollectionName)
                .then(createMono)
    }
}