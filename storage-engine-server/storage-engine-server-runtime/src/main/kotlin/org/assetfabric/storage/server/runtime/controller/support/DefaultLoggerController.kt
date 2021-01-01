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

package org.assetfabric.storage.server.runtime.controller.support

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.assetfabric.storage.server.runtime.controller.LoggerController
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class DefaultLoggerController: LoggerController {

    override fun getLogLevel(@PathVariable("loggerName") loggerName: String): String {
        return LogManager.getLogger(loggerName).level.standardLevel.toString()
    }

    override fun setLogLevel(@PathVariable("loggerName") loggerName: String, @PathVariable("level") level: Level) {
        val ctx = LogManager.getContext(false) as LoggerContext
        val config = ctx.configuration
        val loggerConfig = config.getLoggerConfig(loggerName)
        loggerConfig.level = level
        ctx.updateLoggers()
        val logger = LogManager.getLogger(loggerName)
        logger.log(level, "Set logger level to {}", level)
    }

}