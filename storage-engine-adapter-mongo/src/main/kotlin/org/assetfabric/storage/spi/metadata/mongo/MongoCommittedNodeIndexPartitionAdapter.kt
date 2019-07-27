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

import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.spi.CommittedInverseNodeReferenceRepresentation
import org.assetfabric.storage.spi.metadata.CommittedNodeIndexPartitionAdapter
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
import java.util.regex.Pattern

@Component
@ConditionalOnProperty("assetfabric.storage.metadata.adapter.type", havingValue = "mongo")
class MongoCommittedNodeIndexPartitionAdapter: CommittedNodeIndexPartitionAdapter {

    @Value("\${assetfabric.storage.metadata.adapter.mongo.collection.nodeIndex:committed.nodeIndex}")
    private lateinit var partitionCollectionName: String

    @Autowired
    @Qualifier(MongoTemplateConfiguration.COMMITTED_NODE_INDEX)
    private lateinit var provider: MongoTemplateProvider

    override fun createInverseNodeReferences(refs: Flux<CommittedInverseNodeReferenceRepresentation>): Flux<CommittedInverseNodeReferenceRepresentation> {
        return refs.flatMap { repr -> provider.template.save(repr, partitionCollectionName) }
    }

    override fun nodeReferences(nodePath: Path, revision: RevisionNumber): Flux<CommittedInverseNodeReferenceRepresentation> {
        // find the nodes with the given parent path
        val criteria = Criteria().andOperator(
                Criteria.where("nodePath").`is`(nodePath.toString()),
                Criteria.where("revision").lte(revision.toString()))
        return referencesForCriteria(criteria)
    }

    override fun nodeReferencesAtOrBelow(nodePath: Path, revision: RevisionNumber): Flux<CommittedInverseNodeReferenceRepresentation> {
        val criteria = Criteria()
                .regex(Pattern.compile("^${nodePath.path}"))
                .andOperator(Criteria.where("revision").lte(revision.toString()))

        return referencesForCriteria(criteria)
    }

    private fun referencesForCriteria(criteria: Criteria): Flux<CommittedInverseNodeReferenceRepresentation> {
        val matchStage = Aggregation.match(criteria)

        // sort by path ascending and revision descending (most recent first)
        val sortStage = Aggregation.sort(Sort(Sort.Direction.ASC, "referringNodePath").and(Sort(Sort.Direction.DESC, "revision")))

        // group the documents by path and then extract the actual documents into the grouped result
        val groupStage = Aggregation.group(Fields.fields("nodePath", "referringNodePath")).push("\$\$ROOT").`as`("documents")

        // replace the output of each group with the first document in the group (the most recent revision)
        val replaceStage = Aggregation.replaceRoot {
            Document("\$arrayElemAt", listOf("\$documents", 0))
        }

        // sort the documents again because they're losing their sort order after grouping
        val sortStage2 = Aggregation.sort(Sort.Direction.ASC, "referringNodePath")

        val aggregation = Aggregation.newAggregation(matchStage, sortStage, groupStage, replaceStage, sortStage2)

        return provider.template.aggregate(aggregation, partitionCollectionName, CommittedInverseNodeReferenceRepresentation::class.java)
    }

    override fun reset(): Mono<Void> {
        return provider.template.remove(Query(), partitionCollectionName).then()
    }
}