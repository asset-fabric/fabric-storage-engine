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

package org.assetfabric.storage.server.command.support

import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.server.command.Command
import org.assetfabric.storage.server.service.clustering.ClusterSynchronizationService
import org.assetfabric.storage.spi.NodeState
import org.assetfabric.storage.spi.metadata.CatalogPartitionAdapter
import org.assetfabric.storage.spi.metadata.DataPartitionAdapter
import org.assetfabric.storage.spi.support.DefaultRevisionedNodeRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Initializes the metadata store.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class MetadataStoreInitializationCommand: Command {

    private val logger = LogManager.getLogger(MetadataStoreInitializationCommand::class.java)

    private val initLockName = "metadataInitLock"

    @Autowired
    private lateinit var syncService: ClusterSynchronizationService

    @Autowired
    private lateinit var catalogPartitionAdapter: CatalogPartitionAdapter

    @Autowired
    private lateinit var dataAdapter: DataPartitionAdapter

    override fun execute(): Mono<Void> {
        syncService.executeWithGlobalLock(initLockName) {
            val revisionOpt = catalogPartitionAdapter.currentRepositoryRevision().blockOptional()
            val result = when (revisionOpt.isPresent) {
                true -> {
                    logger.info("Metadata is current at revision ${revisionOpt.get()}")
                    Mono.empty()
                }
                false -> {
                    logger.info("Initializing repository")
                    val revisionNumber = RevisionNumber(0)
                    val rootNode = DefaultRevisionedNodeRepresentation("", Path("/"), revisionNumber, NodeType.UNSTRUCTURED, hashMapOf(), NodeState.NORMAL)

                    val initMono = dataAdapter.writeNodeRepresentations(Flux.just(rootNode))
                    val catMono = catalogPartitionAdapter.setRepositoryRevision(revisionNumber)
                    initMono.then(catMono)
                }
            }
            result.block()
            logger.info("Repository init complete")
        }
        return Mono.empty()
    }

}