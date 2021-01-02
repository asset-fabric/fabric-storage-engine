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

package org.assetfabric.storage.server.runtime.command.support

import org.assetfabric.storage.InputStreamWithLength
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import java.io.InputStream
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector
import java.util.stream.Collectors

/**
 * Collects a stream of DataBuffers into a single InputStream.
 */
class DataBufferCollector : Collector<DataBuffer, MutableList<DataBuffer>, InputStreamWithLength> {

    override fun supplier(): Supplier<MutableList<DataBuffer>> {
        return Supplier { mutableListOf<DataBuffer>() }
    }

    override fun accumulator(): BiConsumer<MutableList<DataBuffer>, DataBuffer> {
        return BiConsumer { list, buffer -> list.add(buffer) }
    }

    override fun combiner(): BinaryOperator<MutableList<DataBuffer>>? {
        return BinaryOperator { bufferList1, bufferList2 ->
            val factory = DefaultDataBufferFactory()
            val buffer1 = factory.join(bufferList1)
            val buffer2 = factory.join(bufferList2)
            mutableListOf(buffer1, buffer2)
        }
    }

    override fun finisher(): Function<MutableList<DataBuffer>, InputStreamWithLength>? {
        return Function { bufferList ->
            val length = bufferList.stream().collect(Collectors.summingLong { it.readableByteCount().toLong() })

            class BufferInputStream(val buffers: List<DataBuffer>): InputStream()  {
                var currentBufferIndex = 0
                var currentStream = buffers[currentBufferIndex].asInputStream()

                override fun read(): Int {
                   return when(val next = currentStream.read()) {
                       -1 -> {
                           if (currentBufferIndex < buffers.size - 1) {
                               currentBufferIndex++
                               currentStream = buffers[currentBufferIndex].asInputStream()
                               read()
                           } else {
                               -1
                           }
                       }
                       else -> next
                   }
                }

            }
            val stream = BufferInputStream(bufferList)
            InputStreamWithLength(stream, length)
        }
    }

    override fun characteristics(): Set<Collector.Characteristics>? {
        return hashSetOf()
    }
}
