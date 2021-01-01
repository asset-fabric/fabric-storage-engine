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
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.spi.metadata.CatalogPartitionAdapter
import org.assetfabric.storage.spi.metadata.mongo.document.CatalogDocumentType
import org.assetfabric.storage.spi.metadata.mongo.document.RepositoryRevisionDocument
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@ConditionalOnProperty("assetfabric.storage.metadata.adapter.type", havingValue = "mongo")
class MongoCatalogPartitionAdapter: CatalogPartitionAdapter {

    private val log = LogManager.getLogger(MongoCatalogPartitionAdapter::class.java)

    @Value("\${assetfabric.storage.metadata.adapter.mongo.collection.catalog:catalog}")
    private lateinit var catalogCollectionName: String

    @Autowired
    @Qualifier(MongoTemplateConfiguration.CATALOG)
    private lateinit var provider: MongoTemplateProvider

    private fun getRevisionDocument(): Mono<RepositoryRevisionDocument> {
        val query = Query()
        query.addCriteria(Criteria.where("documentType").`is`(CatalogDocumentType.REVISION_DOCUMENT))
        return provider.template.findOne(query, RepositoryRevisionDocument::class.java, catalogCollectionName)
    }

    override fun setRepositoryRevision(revisionNumber: RevisionNumber): Mono<Void> {
        val existingMono = getRevisionDocument().map { doc ->
            doc.revision = revisionNumber.toString()
            doc
        }
        val createRevisionMono = existingMono.switchIfEmpty(Mono.just(RepositoryRevisionDocument(null, revisionNumber.toString())))
        return createRevisionMono.flatMap {
            provider.template.save(it, catalogCollectionName)
        }.doOnTerminate {
            log.debug("Wrote repository revision $revisionNumber")
        }.then()
    }

    override fun currentRepositoryRevision(): Mono<RevisionNumber> {
        return getRevisionDocument().map { revisionDocument -> RevisionNumber(revisionDocument.revision) }
    }

    override fun reset(): Mono<Void> {
        return setRepositoryRevision(RevisionNumber(0))
    }
}