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

package org.assetfabric.storage.spi.metadata

import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.spi.CommittedInverseNodeReferenceRepresentation
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CommittedNodeIndexPartitionAdapter {

    /**
     * Creates the given node references in the committed node index.
     */
    fun createInverseNodeReferences(refs: Flux<CommittedInverseNodeReferenceRepresentation>): Flux<CommittedInverseNodeReferenceRepresentation>

    /**
     * Returns the current references to the given node at the given revision.
     * @param nodePath the path of the node for which references should be returned
     * @param revision the revision at which the node references should be returned
     */
    fun nodeReferences(nodePath: Path, revision: RevisionNumber): Flux<CommittedInverseNodeReferenceRepresentation>

    /**
     * Removes all entries from the node index. USE WITH CAUTION!
     */
    fun reset(): Mono<Void>

}