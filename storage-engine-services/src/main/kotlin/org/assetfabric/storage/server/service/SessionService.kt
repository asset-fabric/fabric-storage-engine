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

package org.assetfabric.storage.server.service

import org.assetfabric.storage.Credentials
import org.assetfabric.storage.RevisionNumber
import org.assetfabric.storage.Session
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Provides access to sessions and allows them to be closed.
 */
@Service
interface SessionService {

    /**
     * Creates a session for a user with the given credentials, at the current repository revision.
     * @param credentials the credentials to use to authenticate a user
     * @return a session if the credentials are valid, or an empty optional if not
     */
    fun getSession(credentials: Credentials): Mono<Session>

    /**
     * Gets the active session with the given session token.
     * @param token the token for the session to retrieve.
     */
    fun getSession(token: String): Mono<Session>

    /**
     * Promotes a session to the given revision number.
     * @param token the token of the session to update
     * @param revision the revision the session will use
     */
    fun updateSession(token: String, revision: RevisionNumber): Mono<Void>

    /**
     * Closes a session
     * @param session the session to close
     */
    fun closeSession(session: Session)

}