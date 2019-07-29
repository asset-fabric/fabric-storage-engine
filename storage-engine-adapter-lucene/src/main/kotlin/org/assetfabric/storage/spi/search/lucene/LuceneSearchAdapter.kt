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

package org.assetfabric.storage.spi.search.lucene

import org.apache.logging.log4j.LogManager
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.SortedDocValuesField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Sort
import org.apache.lucene.search.SortField
import org.apache.lucene.search.grouping.GroupingSearch
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.BytesRef
import org.assetfabric.storage.ListType
import org.assetfabric.storage.Path
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.Session
import org.assetfabric.storage.TypedList
import org.assetfabric.storage.spi.search.Query
import org.assetfabric.storage.spi.search.SearchAdapter
import org.assetfabric.storage.spi.search.support.AllTextQuery
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.File
import javax.annotation.PostConstruct

@Component
@ConditionalOnProperty("assetfabric.storage.search.adapter.type", havingValue = "lucene")
class LuceneSearchAdapter: SearchAdapter {

    private val log = LogManager.getLogger(LuceneSearchAdapter::class.java)

    @Value("\${assetfabric.storage.search.adapter.lucene.dir:lucene-index}")
    private lateinit var indexDirPath: String

    private lateinit var indexDir: FSDirectory

    private lateinit var writer: IndexWriter

    val analyzer = StandardAnalyzer()

    @PostConstruct
    private fun init() {
        val indexFile = File(indexDirPath)
        indexFile.mkdirs()
        indexDir = FSDirectory.open(indexFile.toPath())
        val writerConfig = IndexWriterConfig(analyzer)

        writer = IndexWriter(indexDir, writerConfig)
    }

    override fun addSearchEntry(path: Path, revision: RevisionNumber, currentProperties: Map<String, Any>, removedProperties: Map<String, Any>?): Mono<Void> {
        val doc = Document()
        doc.add(SortedDocValuesField("path", BytesRef(path.toString())))
        doc.add(TextField("path", path.toString(), Field.Store.YES))
        doc.add(SortedDocValuesField("revision", BytesRef(revision.toString())))
        doc.add(TextField("revision", revision.toString(), Field.Store.YES))

        val currentStringBuilder = StringBuilder()
        val oldStringBuilder = StringBuilder()

        /**
         * Returns the appropriate type of Lucene field with which the given property
         * should be represented. In some cases, the extracted value will be appended
         * to the supplied StringBuilder for inclusion in "all" searches.
         */
        fun getField(key: String, value: Any, builder: StringBuilder): Field? {
            return when(value) {
                is String -> {
                    builder.append(value)
                    builder.append(" ")
                    TextField(key, value, Field.Store.NO)
                }
                is TypedList -> {
                    when(value.listType) {
                        ListType.STRING -> {
                            val stringText = value.values.joinToString(" ")
                            builder.append(stringText)
                            builder.append(" ")
                            TextField(key, stringText, Field.Store.NO)
                        }
                        else -> null
                    }
                }
                else -> null
            }
        }

        currentProperties.forEach {
            val field: Field? = getField(it.key, it.value, currentStringBuilder)
            if (field != null) {
                doc.add(field)
            }
        }
        removedProperties?.forEach {
            val field: Field? = getField("old_${it.key}", it.value, oldStringBuilder)
            if (field != null) {
                doc.add(field)
            }
        }

        doc.add(TextField("all", currentStringBuilder.toString(), Field.Store.NO))
        doc.add(TextField("old_all", oldStringBuilder.toString(), Field.Store.NO))

        return Mono.fromCallable {
            log.debug("Writing document")
            writer.addDocument(doc)
            writer.commit()
            log.debug("Committed document")
        }.then()
    }

    override fun addWorkingAreaSearchEntry(path: Path, sessionId: String, properties: Map<String, Any>): Mono<Void> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeWorkingAreaSearchEntries(sessionId: String): Mono<Void> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun search(session: Session, query: Query, start: Int, count: Int): Flux<Path> {
        val parser = QueryParser("all", analyzer)
        val revisionQuery = parser.parse("revision: [* TO ${session.revision()}]")
        val currentQuery = parser.parse("all:${(query as AllTextQuery).text}")
        val oldQuery = parser.parse("old_all:${(query).text}")
        val luceneQuery = BooleanQuery.Builder()
                .add(revisionQuery, BooleanClause.Occur.MUST)
                .add(currentQuery, BooleanClause.Occur.SHOULD)
                .add(oldQuery, BooleanClause.Occur.SHOULD).build()

        val reader = DirectoryReader.open(indexDir)
        val searcher = IndexSearcher(reader)

        val groupSearch = GroupingSearch("path")
        val groupSort = Sort(SortField("revision", SortField.Type.STRING, true))
        groupSearch.setSortWithinGroup(groupSort)
        groupSearch.setGroupDocsLimit(1)

        val topGroups = groupSearch.search<BytesRef>(searcher, luceneQuery, start, count)
        val nodes = topGroups.groups.map { groupDocs ->
            val docNum = groupDocs.scoreDocs.first().doc
            val firstDoc = searcher.doc(docNum)
            val curMatch = searcher.explain(currentQuery, docNum).isMatch
            Pair(curMatch, firstDoc)
        }.filter { (currentMatch, _) ->
            currentMatch
        }.map {
            it.second.get("path")
        }

        nodes.forEach { path ->
            println("Got path $path")
        }

        reader.close()

        return Flux.fromIterable(nodes).map { pathString -> Path(pathString) }
    }

    fun reset() {
        writer.deleteAll()
        writer.commit()
    }
}