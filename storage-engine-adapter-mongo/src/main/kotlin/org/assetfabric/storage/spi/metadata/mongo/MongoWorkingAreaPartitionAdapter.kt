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
import org.assetfabric.storage.spi.WorkingAreaNodeRepresentation
import org.assetfabric.storage.spi.metadata.WorkingAreaPartitionAdapter
import org.assetfabric.storage.spi.metadata.mongo.MongoTemplateConfiguration.MongoTemplateConfiguration.WORKING_AREA
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
class MongoWorkingAreaPartitionAdapter: WorkingAreaPartitionAdapter {

    private val log = LogManager.getLogger(MongoWorkingAreaPartitionAdapter::class.java)

    @Value("\${assetfabric.storage.metadata.adapter.mongo.collection.workingArea:workingArea}")
    private lateinit var workingAreaCollectionName: String

    @Autowired
    @Qualifier(WORKING_AREA)
    private lateinit var provider: MongoTemplateProvider

    override fun createNodeRepresentation(representation: WorkingAreaNodeRepresentation): Mono<WorkingAreaNodeRepresentation> {
        return provider.template.save(representation, workingAreaCollectionName)
    }

    override fun updateWorkingAreaRepresentation(representation: WorkingAreaNodeRepresentation): Mono<WorkingAreaNodeRepresentation> {
        // TODO: is there a more efficient way to do this?
        var query = Query()
        val criteria = Criteria().andOperator(
                Criteria.where("sessionId").`is`(representation.sessionId()),
                Criteria.where("path").`is`(representation.path().toString()))
        query = query.addCriteria(criteria)
        return provider.template.remove(query, workingAreaCollectionName).flatMap {
            createNodeRepresentation(representation)
        }
    }

    override fun nodeRepresentation(sessionId: String, path: Path): Mono<WorkingAreaNodeRepresentation> {
        var query = Query()
        val criteria = Criteria().andOperator(
                Criteria.where("sessionId").`is`(sessionId),
                Criteria.where("path").`is`(path.path))
        query = query.addCriteria(criteria)
                .with(Sort(Sort.Direction.DESC, "revision"))
                .limit(1)

        return provider.template.findOne(query, WorkingAreaNodeRepresentation::class.java, workingAreaCollectionName)
    }

    override fun nodeChildRepresentations(sessionId: String, parentPath: Path): Flux<WorkingAreaNodeRepresentation> {
        var query = Query()
        val criteria = Criteria().andOperator(
                Criteria.where("sessionId").`is`(sessionId),
                Criteria.where("parentPath").`is`(parentPath.path))
        query = query.addCriteria(criteria)
                .with(Sort(Sort.Direction.ASC, "path"))
        return provider.template.find(query, WorkingAreaNodeRepresentation::class.java, workingAreaCollectionName)
    }

    override fun getWorkingAreaRepresentations(sessionId: String): Flux<WorkingAreaNodeRepresentation> {
        var query = Query()
        val criteria = Criteria.where("sessionId").`is`(sessionId)
        query = query.addCriteria(criteria)
        return provider.template.find(query, WorkingAreaNodeRepresentation::class.java, workingAreaCollectionName)
    }

    override fun deleteWorkingAreaRepresentations(sessionId: String): Mono<Void> {
        var query = Query()
        val criteria = Criteria.where("sessionId").`is`(sessionId)
        query = query.addCriteria(criteria)
        log.debug("Removing working area representations for session ID $sessionId")
        return provider.template.remove(query, workingAreaCollectionName).then()
    }

    override fun reset(): Mono<Void> {
        return provider.template.remove(Query(), workingAreaCollectionName).then()
    }
}