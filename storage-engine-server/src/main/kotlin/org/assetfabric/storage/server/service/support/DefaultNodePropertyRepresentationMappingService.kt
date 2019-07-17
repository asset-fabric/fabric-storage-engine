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

package org.assetfabric.storage.server.service.support

import org.assetfabric.storage.BinaryReference
import org.assetfabric.storage.InputStreamWithLength
import org.assetfabric.storage.ListType
import org.assetfabric.storage.NodeReference
import org.assetfabric.storage.TypedList
import org.assetfabric.storage.rest.MultiValueNodeProperty
import org.assetfabric.storage.rest.NodeProperty
import org.assetfabric.storage.rest.NodePropertyType
import org.assetfabric.storage.rest.SingleValueNodeProperty
import org.assetfabric.storage.server.service.BinaryManagerService
import org.assetfabric.storage.server.service.NodePropertyRepresentationMappingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import javax.annotation.PostConstruct

@Service
class DefaultNodePropertyRepresentationMappingService: NodePropertyRepresentationMappingService {

    private lateinit var dateFormat: DateFormat

    @Value("\${assetfabric.storage.web.host}")
    private lateinit var host: String

    @Value("\${assetfabric.storage.web.port}")
    private lateinit var port: String

    @Autowired
    private lateinit var binaryManagerService: BinaryManagerService

    @PostConstruct
    private fun init() {
        val tz = TimeZone.getTimeZone("UTC")
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") // Quoted "Z" to indicate UTC, no timezone offset
        df.timeZone = tz
        dateFormat = df
    }

    override fun getInternalPropertyRepresentation(map: Map<String, NodeProperty>, binaryMap: Map<String, InputStreamWithLength>): MutableMap<String, Any> {

        val retMap = HashMap<String, Any>()

        fun getInternalProp(np: NodeProperty): Any {
            return when(np) {
                is SingleValueNodeProperty -> when(np.getType()) {
                    NodePropertyType.STRING -> np.getValue()
                    NodePropertyType.INTEGER -> np.getValue().toInt()
                    NodePropertyType.LONG -> np.getValue().toLong()
                    NodePropertyType.BOOLEAN -> np.getValue().toBoolean()
                    NodePropertyType.DATE -> stringToDate(np.getValue())
                    NodePropertyType.BINARY -> np.getValue()
                    NodePropertyType.BINARY_INPUT -> inputToReference(binaryMap[np.getValue()]!!)
                    NodePropertyType.NODE -> np.getValue()
                    else -> throw RuntimeException("Unknown property type ${np.getType()}")
                }
                is MultiValueNodeProperty -> when (np.getType()) {
                    NodePropertyType.BOOLEAN -> TypedList(ListType.BOOLEAN, np.getValues().map { it.toBoolean() })
                    NodePropertyType.DATE -> TypedList(ListType.DATE, np.getValues().map { stringToDate(it) })
                    NodePropertyType.INTEGER -> TypedList(ListType.INTEGER, np.getValues().map { it.toInt() })
                    NodePropertyType.LONG -> TypedList(ListType.LONG, np.getValues().map { it.toLong() })
                    NodePropertyType.STRING -> TypedList(ListType.STRING, np.getValues())
                    NodePropertyType.NODE -> TypedList(ListType.NODE, np.getValues())
                    else -> throw RuntimeException("Unknown property type ${np.getType()}")
                } else ->
                    throw RuntimeException("Unknown property $np")
            }
        }

        for (prop in map) {
            retMap.put(prop.key, getInternalProp(prop.value))
        }
        return retMap
    }

    override fun getExternalPropertyRepresentation(map: Map<String, Any>): MutableMap<String, NodeProperty> {
        val retMap = HashMap<String, NodeProperty>()

        fun getExternalProp(value: Any): NodeProperty {
            return when(value) {
                is String -> SingleValueNodeProperty(NodePropertyType.STRING, value)
                is Int -> SingleValueNodeProperty(NodePropertyType.INTEGER, value.toString())
                is Long -> SingleValueNodeProperty(NodePropertyType.LONG, value.toString())
                is Boolean -> SingleValueNodeProperty(NodePropertyType.BOOLEAN, value.toString())
                is Date -> SingleValueNodeProperty(NodePropertyType.DATE, dateToString(value))
                is BinaryReference -> SingleValueNodeProperty(NodePropertyType.BINARY, binaryReferenceToUrl(value).path)
                is NodeReference -> SingleValueNodeProperty(NodePropertyType.NODE, value.path)
                is TypedList -> {
                    val nodePropertyType = when(value.listType) {
                        ListType.BOOLEAN -> NodePropertyType.BOOLEAN
                        ListType.INTEGER -> NodePropertyType.INTEGER
                        ListType.DATE -> NodePropertyType.DATE
                        ListType.NODE -> NodePropertyType.NODE
                        ListType.LONG -> NodePropertyType.LONG
                        ListType.STRING -> NodePropertyType.STRING
                    }
                    // TODO: convert these values into strings
                    val propList: List<String> = when(value.values.isEmpty()) {
                        true -> listOf()
                        false -> {
                            when(value.listType) {
                                ListType.INTEGER -> value.values.map { it.toString() }
                                ListType.STRING -> value.values.map { it as String }
                                ListType.BOOLEAN -> value.values.map { it.toString() }
                                ListType.DATE -> value.values.map { dateToString(it as Date)}
                                ListType.NODE -> value.values.map { it as String }
                                ListType.LONG -> value.values.map { it.toString() }
                            }
                        }
                    }
                    MultiValueNodeProperty(nodePropertyType, propList)
                }
                else -> {
                    throw RuntimeException("Not implemented for type ${value::class.java}")
                }
            }
        }

        for (entry in map) {
            retMap.put(entry.key, getExternalProp(entry.value))
        }

        return retMap
    }

    private fun dateToString(d: Date) = dateFormat.format(d)
    private fun stringToDate(s: String) = dateFormat.parse(s)

    private fun binaryReferenceToUrl(br: BinaryReference): BinaryReference {
        val path = br.path
        return BinaryReference("http://$host:$port/v1/binary?path=$path")
    }

    private fun inputToReference(b: InputStreamWithLength): BinaryReference {
        return BinaryReference(binaryManagerService.createFile(b))
    }

}