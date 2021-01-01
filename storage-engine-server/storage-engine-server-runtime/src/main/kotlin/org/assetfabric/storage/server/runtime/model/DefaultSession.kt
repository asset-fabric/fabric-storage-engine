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

package org.assetfabric.storage.server.model

import org.assetfabric.storage.Node
import org.assetfabric.storage.Path
import org.assetfabric.storage.Query
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.Session
import org.assetfabric.storage.server.runtime.service.MetadataManagerService
import org.assetfabric.storage.server.runtime.service.SessionService
import org.assetfabric.storage.spi.metadata.NodeRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class DefaultSession(private val sessionID: String, private val userID: String, revision: RevisionNumber): Session {

    private var currentRevision = revision

    @Autowired
    private lateinit var metadataManagerService: MetadataManagerService

    @Autowired
    private lateinit var sessionService: SessionService

    @Autowired
    private lateinit var context: ApplicationContext

    override fun getSessionID(): String = sessionID

    override fun getUserID(): String = userID

    override fun revision(): RevisionNumber = currentRevision

    fun setRevision(rev: RevisionNumber) {
        currentRevision = rev
    }

    override fun rootNode(): Mono<Node> {
        return node("/").switchIfEmpty(Mono.error(RuntimeException("Root node not found")))
    }

    override fun node(path: String): Mono<Node> {
        val representationMono =  metadataManagerService.nodeRepresentation(this, Path(path))
        return representationMono.map { context.getBean(DefaultNode::class.java, this, it)  }
    }

    override fun search(term: String): Flux<Node> {
        val representationFlux: Flux<NodeRepresentation> = metadataManagerService.search(this, term)
        return representationFlux.map { context.getBean(DefaultNode::class.java, this, it) }
    }

    override fun search(query: Query): Flux<Node> {
        val representationFlux: Flux<NodeRepresentation> = metadataManagerService.search(this, query)
        return representationFlux.map { context.getBean(DefaultNode::class.java, this, it) }
    }

    override fun commit(): Mono<Void> {
        return metadataManagerService.commitSession(this)
    }

    override fun close(): Mono<Void> {
        return metadataManagerService.destroySessionChanges(this).then(
            sessionService.closeSession(this)
        )
    }

    override fun toString(): String {
        return "DefaultSession(sessionID='$sessionID')"
    }


}