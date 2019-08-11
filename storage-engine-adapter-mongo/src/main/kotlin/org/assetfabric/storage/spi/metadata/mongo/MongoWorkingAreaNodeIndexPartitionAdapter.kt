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
import org.assetfabric.storage.spi.WorkingAreaInverseNodeReferenceRepresentation
import org.assetfabric.storage.spi.metadata.WorkingAreaNodeIndexPartitionAdapter
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
import java.util.regex.Pattern

@Component
@ConditionalOnProperty("assetfabric.storage.metadata.adapter.type", havingValue = "mongo")
class MongoWorkingAreaNodeIndexPartitionAdapter: WorkingAreaNodeIndexPartitionAdapter {

    @Value("\${assetfabric.storage.metadata.adapter.mongo.collection.workingAreaNodeIndex:workingArea.nodeIndex}")
    private lateinit var workingAreaCollectionName: String

    @Autowired
    @Qualifier(MongoTemplateConfiguration.WORKING_AREA_NODE_INDEX)
    private lateinit var provider: MongoTemplateProvider

    override fun createInverseNodeReferences(refs: Flux<WorkingAreaInverseNodeReferenceRepresentation>): Flux<WorkingAreaInverseNodeReferenceRepresentation> {
        return refs.flatMap { provider.template.save(it, workingAreaCollectionName) }
    }

    override fun nodeReferences(sessionId: String, nodePath: Path): Flux<WorkingAreaInverseNodeReferenceRepresentation> {
        return nodeReferencesFor(sessionId, Criteria.where("nodePath").`is`(nodePath.toString()))
    }

    override fun nodeReferencesAtOrBelow(sessionId: String, nodePath: Path): Flux<WorkingAreaInverseNodeReferenceRepresentation> {
        val criteria = Criteria.where("nodePath").regex(Pattern.compile("^${nodePath.path}"))
        return nodeReferencesFor(sessionId, criteria)
    }

    private fun nodeReferencesFor(sessionId: String, criteria: Criteria): Flux<WorkingAreaInverseNodeReferenceRepresentation> {
        val topCriteria = Criteria()
                .andOperator(
                        Criteria.where("sessionId").`is`(sessionId),
                        criteria)
        var query = Query()
        query = query.addCriteria(topCriteria).with(Sort(Sort.Direction.ASC, "referringNodePath"))
        return provider.template.find(query, WorkingAreaInverseNodeReferenceRepresentation::class.java, workingAreaCollectionName)

    }

    override fun deleteNodeReferences(sessionId: String): Mono<Void> {
        val query = Query().addCriteria(Criteria.where("sessionId").`is`(sessionId))
        return provider.template.remove(query, workingAreaCollectionName).then()
    }

    override fun reset(): Mono<Void> {
        return provider.template.remove(Query(), workingAreaCollectionName).then()
    }
}