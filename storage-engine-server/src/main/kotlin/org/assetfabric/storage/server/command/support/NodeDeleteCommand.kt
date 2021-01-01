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

import org.assetfabric.storage.NodeNotFoundException
import org.assetfabric.storage.Path
import org.assetfabric.storage.Session
import org.assetfabric.storage.State
import org.assetfabric.storage.server.command.Command
import org.assetfabric.storage.spi.metadata.DataPartitionAdapter
import org.assetfabric.storage.spi.metadata.WorkingAreaPartitionAdapter
import org.assetfabric.storage.spi.metadata.support.DefaultNodeContentRepresentation
import org.assetfabric.storage.spi.metadata.support.DefaultWorkingAreaNodeRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class NodeDeleteCommand(val session: Session, val nodePath: Path): Command {

    @Autowired
    private lateinit var workingAreaPartitionAdapter: WorkingAreaPartitionAdapter

    @Autowired
    private lateinit var dataPartitionAdapter: DataPartitionAdapter

    override fun execute(): Mono<Void> {
        return workingAreaPartitionAdapter.nodeRepresentation(session.getSessionID(), nodePath).flatMap { war ->
            war.workingAreaRepresentation().setState(State.DELETED)
            workingAreaPartitionAdapter.updateWorkingAreaRepresentation(war)
        }.switchIfEmpty(Mono.defer {
            dataPartitionAdapter.nodeRepresentation(session.revision(), nodePath).flatMap { existingNode ->
                val contentRepr = DefaultNodeContentRepresentation(hashMapOf())
                contentRepr.setState(State.DELETED)
                val newWorkingAreaRepr = DefaultWorkingAreaNodeRepresentation(session.getSessionID(), nodePath, existingNode.nodeType(), existingNode, contentRepr)
                workingAreaPartitionAdapter.createNodeRepresentation(newWorkingAreaRepr)
            }.switchIfEmpty(Mono.error(NodeNotFoundException("Node $nodePath not found")))
        }).then()
    }

}