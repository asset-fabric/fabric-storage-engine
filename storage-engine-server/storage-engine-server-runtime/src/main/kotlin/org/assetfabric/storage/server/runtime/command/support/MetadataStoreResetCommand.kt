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

import org.assetfabric.storage.server.command.Command
import org.assetfabric.storage.spi.metadata.CatalogPartitionAdapter
import org.assetfabric.storage.spi.metadata.DataPartitionAdapter
import org.assetfabric.storage.spi.metadata.JournalPartitionAdapter
import org.assetfabric.storage.spi.metadata.WorkingAreaPartitionAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Resets the metadata store.  USE WITH EXTREME CAUTION!
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class MetadataStoreResetCommand: Command {

    @Autowired
    private lateinit var catalogAdapter: CatalogPartitionAdapter

    @Autowired
    private lateinit var workingAreaAdapter: WorkingAreaPartitionAdapter

    @Autowired
    private lateinit var journalAdapter: JournalPartitionAdapter

    @Autowired
    private lateinit var dataAdapter: DataPartitionAdapter

    override fun execute(): Mono<Void> {
        val catReset = catalogAdapter.reset()
        val workReset = workingAreaAdapter.reset()
        val journalReset = journalAdapter.reset()
        val dataReset = dataAdapter.reset()

        return catReset.then(workReset).then(journalReset).then(dataReset)
    }

}