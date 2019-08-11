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

package org.assetfabric.storage

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface Node {

    /**
     * Returns the name of this node.
     */
    fun name(): String

    /**
     * Returns the path of this node.
     */
    fun path(): Path

    /**
     * Returns the revision number of this node, if it has one.
     */
    fun revision(): RevisionNumber?

    /**
     * Returns the type of this node.
     */
    fun nodeType(): NodeType

    /**
     * Returns the state of this node.
     */
    fun state(): State

    /**
     * Returns all properties of this node.
     */
    fun properties(): Map<String, Any>

    /**
     * Creates a new node as a child of this node.
     * @param name the name of the new child node
     * @param nodeType the type of the new child node
     * @param properties the properties of the new child node
     */
    fun createChild(name: String, nodeType: NodeType, properties: MutableMap<String, Any>): Mono<Node>

    /**
     * Returns the child of this node with the given name, if available.
     */
    fun child(name: String): Mono<Node>

    /**
     * Returns all the children of this node.
     */
    fun children(): Flux<Node>

    /**
     * Updates the properties of this node.
     */
    fun setProperties(properties: MutableMap<String, Any>): Mono<Void>

    /**
     * Returns the given property as a string.
     */
    fun stringProperty(name: String): String?

    /**
     * Returns the given property as a Node mono.
     */
    fun nodeProperty(name: String): Mono<Node>

    /**
     * Returns the nodes that have [NodeReference]s pointing at this node.
     */
    fun referringNodes(): Flux<Node>

    /**
     * Deletes this node.
     */
    fun delete(): Mono<Void>

}