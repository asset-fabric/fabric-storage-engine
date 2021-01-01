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

package org.assetfabric.storage.server.runtime.service.support

import org.apache.logging.log4j.LogManager
import org.assetfabric.storage.Credentials
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.Session
import org.assetfabric.storage.server.model.DefaultSession
import org.assetfabric.storage.server.runtime.service.MetadataManagerService
import org.assetfabric.storage.server.runtime.service.SessionService
import org.assetfabric.storage.server.service.clustering.ClusterSessionInfo
import org.assetfabric.storage.server.service.clustering.ClusterSynchronizationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID
import javax.annotation.PostConstruct

@Service
class DefaultSessionService: SessionService {

    private val log = LogManager.getLogger(DefaultSessionService::class.java)

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var authProviders: List<AuthenticationProvider>

    @Autowired
    private lateinit var clusterSynchronizationService: ClusterSynchronizationService

    @Autowired
    private lateinit var metadataManagerService: MetadataManagerService

    private lateinit var providerManager: ProviderManager

    @PostConstruct
    private fun init() {
        providerManager = ProviderManager(authProviders)
        log.debug("Initialized provider manager with providers $authProviders")
    }

    override fun getSession(credentials: Credentials): Mono<Session> {
        log.info("Getting session for user {}", credentials.username)
        if (credentials.username == null || credentials.password == null) {
            return Mono.empty()
        } else {
            val creds = UsernamePasswordAuthenticationToken(credentials.username, credentials.password)
            try {
                val authCredentials = providerManager.authenticate(creds)
                return when (authCredentials.isAuthenticated) {
                    true -> {
                        metadataManagerService.repositoryRevision().map { revision ->
                            val sessionKey = UUID.randomUUID().toString()
                            val sessionInfo = ClusterSessionInfo(sessionKey, authCredentials.name, revision.toString())
                            val session = applicationContext.getBean(DefaultSession::class.java, sessionKey, authCredentials.name, revision)
                            log.info("Adding session $sessionKey at revision $revision")
                            clusterSynchronizationService.addSessionInfo(sessionKey, sessionInfo)
                            session
                        }
                    }
                    false -> Mono.empty()
                }
            } catch (bce: BadCredentialsException) {
                return Mono.empty()
            }
        }
    }


    override fun updateSession(token: String, revision: RevisionNumber): Mono<Void> {
        val sessionInfoMono = clusterSynchronizationService.getSessionInfo(token)
        return sessionInfoMono.map { sessionInfo ->
            clusterSynchronizationService.updateSessionInfo(sessionInfo.getSessionID(), ClusterSessionInfo(token, sessionInfo.getUserID(), revision.toString()))
        }.then()
    }

    override fun getSession(token: String): Mono<Session> {
        val sessionInfoOpt = clusterSynchronizationService.getSessionInfo(token)
        return sessionInfoOpt.map { info -> applicationContext.getBean(DefaultSession::class.java, info.getSessionID(), info.getUserID(), RevisionNumber(info.getRevision()))}
    }

    override fun closeSession(session: Session): Mono<Void> {
        return Mono.defer {
            clusterSynchronizationService.removeSessionInfo(session.getSessionID())
            Mono.empty<Void>()
        }
    }
}