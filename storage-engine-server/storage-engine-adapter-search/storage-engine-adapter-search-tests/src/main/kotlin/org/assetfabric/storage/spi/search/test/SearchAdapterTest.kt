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

package org.assetfabric.storage.spi.search.test

import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.ListType
import org.assetfabric.storage.NodeType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.Session
import org.assetfabric.storage.State
import org.assetfabric.storage.TypedList
import org.assetfabric.storage.spi.search.SearchAdapter
import org.assetfabric.storage.spi.search.SearchEntry
import org.assetfabric.storage.spi.search.support.AllTextQuery
import org.assetfabric.storage.spi.search.support.AndQuery
import org.assetfabric.storage.spi.search.support.NodeTypeQuery
import org.assetfabric.storage.spi.search.support.OrQuery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Flux
import java.util.Date

open class SearchAdapterTest {

    private val log = LogManager.getLogger(SearchAdapterTest::class.java)

    @Autowired
    protected lateinit var searchAdapter: SearchAdapter

    val session = mock(Session::class.java)

    @Test
    @DisplayName("should be able to retrieve a node path from a search index using all applicable fields")
    fun testRetrieveDocument() {

        searchAdapter.addSearchEntry(SearchEntry(Path("/node1"), NodeType.UNSTRUCTURED, RevisionNumber(1), State.NORMAL, mapOf(
                "stringProp" to "test",
                "intProp" to 3,
                "booleanProp" to true,
                "longProp" to 4L,
                "dateProp" to Date(),
                "stringListProp" to TypedList(ListType.STRING, listOf("a", "b", "c")),
                "intListProp" to TypedList(ListType.INTEGER, listOf(1, 2, 3)),
                "booleanListProp" to TypedList(ListType.BOOLEAN, listOf(true, false)),
                "longListProp" to TypedList(ListType.LONG, listOf(1L, 10L, 20L)),
                "dateListProp" to TypedList(ListType.DATE, listOf(Date(), Date()))
        ), null)).block()


        `when`(session.revision()).thenReturn(RevisionNumber(2))

        log.debug("Running test search")
        // search by the string property
        val paths = searchAdapter.search(session, AllTextQuery("test"), 0, 5).collectList().block()!!
        assertEquals(1, paths.size, "Path count mismatch")

        log.debug("Running mother search")
        // search by the string list property
        val paths2 = searchAdapter.search(session, AllTextQuery("b"), 0, 5).collectList().block()!!
        assertEquals(1, paths2.size, "Path count mismatch")
    }

    @Test
    @DisplayName("should be able to retrieve a matching node path from a prior revision")
    fun testRetrievePriorRevisionDoc() {
        searchAdapter.addSearchEntry(SearchEntry(Path("/node1"),  NodeType.UNSTRUCTURED, RevisionNumber(1), State.NORMAL, mapOf("stringProp" to "test"), null)).block()
        searchAdapter.addSearchEntry(SearchEntry(Path("/node1"),  NodeType.UNSTRUCTURED, RevisionNumber(2), State.NORMAL, mapOf("stringProp" to "stable"), null)).block()

        `when`(session.revision()).thenReturn(RevisionNumber(1))
        val paths = searchAdapter.search(session, AllTextQuery("test"), 0, 5).collectList().block()!!
        assertEquals(1, paths.size, "Path count mismatch")
    }

    @Test
    @DisplayName("should not retrieve matching node paths from a future revision")
    fun testRetrieveFutureRevision() {
        searchAdapter.addSearchEntry(SearchEntry(Path("/node1"),  NodeType.UNSTRUCTURED, RevisionNumber(1), State.NORMAL, mapOf("stringProp" to "test"), null)).block()
        searchAdapter.addSearchEntry(SearchEntry(Path("/node1"),  NodeType.UNSTRUCTURED, RevisionNumber(2), State.NORMAL, mapOf("stringProp" to "stable"), null)).block()

        `when`(session.revision()).thenReturn(RevisionNumber(1))
        val paths = searchAdapter.search(session, AllTextQuery("stable"), 0, 5).collectList().block()!!
        assertEquals(0, paths.size, "Path count mismatch")
    }

    @Test
    @DisplayName("should not retrieve matching node paths when they are matched by later, blocking changes")
    fun testRetrieveBlockingRevision() {
        searchAdapter.addSearchEntry(SearchEntry(Path("/node1"),  NodeType.UNSTRUCTURED, RevisionNumber(1), State.NORMAL, mapOf("stringProp" to "test"), null)).block()

        // this demonstrates how changes to node properties MUST be filed with the adapter.
        // new values must be given in the first set of properties, and removed or altered properties must be given in the second set of properties.

        searchAdapter.addSearchEntry(SearchEntry(Path("/node1"),  NodeType.UNSTRUCTURED, RevisionNumber(2), State.NORMAL, mapOf("stringProp" to "stable"), mapOf("stringProp" to "test"))).block()

        `when`(session.revision()).thenReturn(RevisionNumber(2))

        // without the blocking established by submitted changed/removed properties, this search would match revision 1 of the node,
        // instead of matching revision 2 of the node and enabling the adapter to hide the path from the search results.

        val paths = searchAdapter.search(session, AllTextQuery("test"), 0, 5).collectList().block()!!
        assertEquals(0, paths.size, "Path count mismatch")
    }

    @Test
    @DisplayName("should be able to write multiple search entries at once")
    fun testAddMultipleEntries() {
        val node1 = SearchEntry(Path("/node1"),  NodeType.UNSTRUCTURED, RevisionNumber(1), State.NORMAL, mapOf("stringProp" to "test"), null)
        val node2 = SearchEntry(Path("/node2"),  NodeType.UNSTRUCTURED, RevisionNumber(1), State.NORMAL, mapOf("stringProp" to "test"), null)
        val nodeFlux = Flux.just(node1, node2)

        searchAdapter.addSearchEntries(nodeFlux).block()

        `when`(session.revision()).thenReturn(RevisionNumber(1))
        val paths = searchAdapter.search(session, AllTextQuery("test"), 0, 5).collectList().block()!!
        assertEquals(2, paths.size, "Path count mismatch")

    }

    @Test
    @DisplayName("should be able to retrieve a matching node path by node type")
    fun testRetrieveByNodeType() {
        searchAdapter.addSearchEntry(SearchEntry(Path("/node1"),  NodeType.UNSTRUCTURED, RevisionNumber(1), State.NORMAL, mapOf("stringProp" to "test"), null)).block()

        `when`(session.revision()).thenReturn(RevisionNumber(1))
        val paths = searchAdapter.search(session, NodeTypeQuery(NodeType.UNSTRUCTURED), 0, 5).collectList().block()!!
        assertEquals(1, paths.size, "Path count mismatch")
    }

    @Test
    @DisplayName("should be able to retrieve a matching node path by all text and node type")
    fun testRetrieveByAllTextAndNodeType() {
        searchAdapter.addSearchEntry(SearchEntry(Path("/node1"),  NodeType.UNSTRUCTURED, RevisionNumber(1), State.NORMAL, mapOf("stringProp" to "test"), null)).block()
        searchAdapter.addSearchEntry(SearchEntry(Path("/node2"),  NodeType("af:other:1"), RevisionNumber(1), State.NORMAL, mapOf("stringProp" to "test"), null)).block()
        searchAdapter.addSearchEntry(SearchEntry(Path("/node3"),  NodeType("af:other:1"), RevisionNumber(1), State.NORMAL, mapOf("stringProp" to "people"), null)).block()

        `when`(session.revision()).thenReturn(RevisionNumber(1))

        val paths = searchAdapter.search(session, NodeTypeQuery(NodeType.UNSTRUCTURED), 0, 5).collectList().block()!!
        assertEquals(1, paths.size, "Path count mismatch")

        val paths2 = searchAdapter.search(session, NodeTypeQuery(NodeType("af:other:1")), 0, 5).collectList().block()!!
        assertEquals(2, paths2.size, "Path count mismatch")

        val paths3 = searchAdapter.search(session, AllTextQuery("test"), 0, 5).collectList().block()!!
        assertEquals(2, paths3.size, "Path count mismatch")

        val paths4 = searchAdapter.search(session, AndQuery(AllTextQuery("test"), NodeTypeQuery(NodeType("af:other:1"))), 0, 5).collectList().block()!!
        assertEquals(1, paths4.size, "Path count mismatch")

    }

    @Test
    @DisplayName("should be able to retrieve a matching node path by all text and multiple node types")
    fun testRetrieveByAllTextAndOrQuery() {
        searchAdapter.addSearchEntry(SearchEntry(Path("/node1"),  NodeType.UNSTRUCTURED, RevisionNumber(1), State.NORMAL, mapOf("stringProp" to "test"), null)).block()
        searchAdapter.addSearchEntry(SearchEntry(Path("/node2"),  NodeType("af:other:1"), RevisionNumber(1), State.NORMAL, mapOf("stringProp" to "test"), null)).block()
        searchAdapter.addSearchEntry(SearchEntry(Path("/node3"),  NodeType("af:third:1"), RevisionNumber(1), State.NORMAL, mapOf("stringProp" to "test"), null)).block()

        `when`(session.revision()).thenReturn(RevisionNumber(1))

        val textQuery = AllTextQuery("test")
        val orQuery = OrQuery(NodeTypeQuery(NodeType("af:other:1")), NodeTypeQuery(NodeType.UNSTRUCTURED))
        val andQuery = AndQuery(textQuery, orQuery)

        val paths = searchAdapter.search(session, andQuery, 0, 5).collectList().block()!!
        assertEquals(2, paths.size, "Path count mismatch")


    }

}