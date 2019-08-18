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

package org.assetfabric.storage.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests that Asset Fabric REST model objects can be correctly marshalled and unmarshalled.
 */
@DisplayName("An asset fabric REST application")
class RestParsingTest {

    private val mapper = ObjectMapper()

    @Test
    @DisplayName("should be able to read a binary property")
    fun testBinary() {
        val str = """
            {
                "type": "binary",
                "value": "/ac/12/34/13"
            }
        """.trimIndent()
        val prop = mapper.readValue(str, BinaryProperty::class.java)
        assertEquals("/ac/12/34/13", prop.getValue())
    }

    @Test
    @DisplayName("should be able to read a binary list property")
    fun testBinaryList() {
        val str = """
            {
                "type": "binary[]",
                "values": ["/ac", "/b2"]
            }
        """.trimIndent()
        val prop = mapper.readValue(str, BinaryListProperty::class.java)
        assertEquals(listOf("/ac", "/b2"), prop.getValues())
    }

    @Test
    @DisplayName("should be able to read a boolean property")
    fun testBoolean() {
        val str = """
            {
                "type": "boolean",
                "value": "false"
            }
        """.trimIndent()
        val prop = mapper.readValue(str, BooleanProperty::class.java)
        assertEquals("false", prop.getValue())
    }

    @Test
    @DisplayName("should be able to read a boolean list property")
    fun testBooleanList() {
        val str = """
            {
                "type": "boolean[]",
                "values": ["true", "false"]
            }
        """.trimIndent()
        val prop = mapper.readValue(str, BooleanListProperty::class.java)
        assertEquals(listOf("true", "false"), prop.getValues())
    }

    @Test
    @DisplayName("should be able to read a date property")
    fun testDate() {
        val str = """
            {
                "type": "date",
                "value": "2019-01-01 00:00:00Z"
            }
        """.trimIndent()
        val prop = mapper.readValue(str, DateProperty::class.java)
        assertEquals("2019-01-01 00:00:00Z", prop.getValue())
    }

    @Test
    @DisplayName("should be able to read a date list property")
    fun testDateList() {
        val str = """
            {
                "type": "date[]",
                "values": ["2019-01-01 00:00:00Z", "2019-01-01 00:00:00Z"]
            }
        """.trimIndent()
        val prop = mapper.readValue(str, DateListProperty::class.java)
        assertEquals(listOf("2019-01-01 00:00:00Z", "2019-01-01 00:00:00Z"), prop.getValues())
    }

    @Test
    @DisplayName("should be able to read a double property")
    fun testDouble() {
        val str = """
            {
                "type": "double",
                "value": "3.2"
            }
        """.trimIndent()
        val prop = mapper.readValue(str, DoubleProperty::class.java)
        assertEquals("3.2", prop.getValue())
    }

    @Test
    @DisplayName("should be able to read a double list property")
    fun testDoubleList() {
        val str = """
            {
                "type": "double[]",
                "values": ["0.45", "0.2"]
            }
        """.trimIndent()
        val prop = mapper.readValue(str, DoubleListProperty::class.java)
        assertEquals(listOf("0.45", "0.2"), prop.getValues())
    }

    @Test
    @DisplayName("should be able to read an int property")
    fun testInteger() {
        val str = """
            {
                "type": "int",
                "value": "3"
            }
        """.trimIndent()
        val prop = mapper.readValue(str, IntegerProperty::class.java)
        assertEquals("3", prop.getValue())
    }

    @Test
    @DisplayName("should be able to read a int list property")
    fun testIntegerList() {
        val str = """
            {
                "type": "int[]",
                "values": ["2", "4"]
            }
        """.trimIndent()
        val prop = mapper.readValue(str, IntegerListProperty::class.java)
        assertEquals(listOf("2", "4"), prop.getValues())
    }

    @Test
    @DisplayName("should be able to read a long property")
    fun testLong() {
        val str = """
            {
                "type": "long",
                "value": "3"
            }
        """.trimIndent()
        val prop = mapper.readValue(str, LongProperty::class.java)
        assertEquals("3", prop.getValue())
    }

    @Test
    @DisplayName("should be able to read a long list property")
    fun testLongList() {
        val str = """
            {
                "type": "long[]",
                "values": ["2", "4"]
            }
        """.trimIndent()
        val prop = mapper.readValue(str, LongListProperty::class.java)
        assertEquals(listOf("2", "4"), prop.getValues())
    }

    @Test
    @DisplayName("should be able to read a node reference property")
    fun testNodeReference() {
        val str = """
            {
                "type": "node",
                "value": "/node/path"
            }
        """.trimIndent()
        val prop = mapper.readValue(str, NodeReferenceProperty::class.java)
        assertEquals("/node/path", prop.getValue())
    }

    @Test
    @DisplayName("should be able to read a node reference list property")
    fun testNodeReferenceList() {
        val str = """
            {
                "type": "node[]",
                "values": ["/path1", "/path2"]
            }
        """.trimIndent()
        val prop = mapper.readValue(str, NodeReferenceListProperty::class.java)
        assertEquals(listOf("/path1", "/path2"), prop.getValues())
    }

    @Test
    @DisplayName("should be able to read a parameterized node reference property")
    fun testParamNodeReference() {
        val str = """
            {
                "type": "paramnode",
                "value": "/node/path",
                "properties": {
                    "intprop": {
                        "type": "int",
                        "value": "3"
                    },
                    "intlist": {
                        "type": "int[]",
                        "values": ["1", "2"]
                    }
                }
            }
        """.trimIndent()
        val prop = mapper.readValue(str, ParameterizedNodeReferenceProperty::class.java)
        assertEquals("/node/path", prop.getValue())
        assertEquals(mapOf("intprop" to IntegerProperty(3), "intlist" to IntegerListProperty(1, 2)), prop.getProperties())
    }

    @Test
    @DisplayName("should be able to read a parameterized node reference list property")
    fun testParamNodeReferenceList() {
        val str = """
            {
                "type": "paramnode[]",
                "values": [
                    {
                        "type": "paramnode",
                        "value": "/node1/path",
                        "properties": {
                            "intprop": {
                                "type": "int",
                                "value": "3"
                            }
                        }
                    },
                    {
                        "type": "paramnode",
                        "value": "/node2/path",
                        "properties": {
                            "intprop": {
                                "type": "int",
                                "value": "3"
                            }
                        }
                    }
                ]
            }
        """.trimIndent()
        val prop = mapper.readValue(str, ParameterizedNodeReferenceListProperty::class.java)
        assertEquals(listOf(
                ParameterizedNodeReferenceProperty("/node1/path", mapOf("intprop" to IntegerProperty(3))),
                ParameterizedNodeReferenceProperty("/node2/path", mapOf("intprop" to IntegerProperty(3)))
        ), prop.getValues())
    }

    @Test
    @DisplayName("should be able to read a string property")
    fun testString() {
        val str = """
            {
                "type": "string",
                "value": "3af"
            }
        """.trimIndent()
        val prop = mapper.readValue(str, StringProperty::class.java)
        assertEquals("3af", prop.getValue())
    }

    @Test
    @DisplayName("should be able to read a long list property")
    fun testStringList() {
        val str = """
            {
                "type": "string[]",
                "values": ["2a", "4b"]
            }
        """.trimIndent()
        val prop = mapper.readValue(str, StringListProperty::class.java)
        assertEquals(listOf("2a", "4b"), prop.getValues())
    }

    @Test
    @DisplayName("should be able read a node content representation")
    fun testNodeContentRepresentation() {
        val str = """
            {
                "properties": {
                    "intProp": {
                        "type": "int",
                        "value": "3"
                    },
                    "intListProp": {
                        "type": "int[]",
                        "values": ["1", "2"]
                    },
                    "longProp": {
                        "type": "long",
                        "value": "3"
                    },
                    "longListProp": {
                        "type": "long[]",
                        "values": ["1", "2"]
                    },
                    "doubleProp": {
                        "type": "double",
                        "value": "3.14"
                    },
                    "doubleListProp": {
                        "type": "double[]",
                        "values": ["1.4", "2.2"]
                    },
                    "booleanProp": {
                        "type": "boolean",
                        "value": "true"
                    },
                    "booleanListProp": {
                        "type": "boolean[]",
                        "values": ["true"]
                    },
                    "stringProp": {
                        "type": "string",
                        "value": "hello"
                    },
                    "stringListProp": {
                        "type": "string[]",
                        "values": ["2a", "4b"]
                    },
                    "dateProp": {
                        "type": "date",
                        "value": "2019-01-01 00:00:00.000Z"
                    },
                    "dateListProp": {
                        "type": "date[]",
                        "values": ["2019-01-01 00:00:00.000Z"]
                    },
                    "nodeProp": {
                        "type": "node",
                        "value": "/node"
                    },
                    "nodeListProp": {
                        "type": "node[]",
                        "values": ["/node"]
                    },
                    "binaryProp": {
                        "type": "binary",
                        "value": "/a/1"
                    },
                    "binaryListProp": {
                        "type": "binary[]",
                        "values": ["/b/2"]
                    },
                    "binaryInputProp": {
                        "type": "binaryinput",
                        "value": "label"
                    },
                    "paramNodeProp": {
                        "type": "paramnode",
                        "value": "/node/path",
                        "properties": {
                            "stringP": {
                                "type": "string",
                                "value": "hi"
                            }
                        }
                    },
                    "paramNodeListProp": {
                        "type": "paramnode[]",
                        "values": [
                            {
                                "type": "paramnode",
                                "value": "/node/path",
                                "properties": {
                                    "stringP": {
                                        "type": "string",
                                        "value": "hi"
                                    }
                                }
                            }
                        ]
                    }
                }
            }
        """.trimIndent()
        mapper.readValue(str, NodeContentRepresentation::class.java)
    }


}