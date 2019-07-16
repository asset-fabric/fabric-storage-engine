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

package org.assetfabric.storage.server.security

import org.springframework.beans.factory.FactoryBean
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

/**
 * Creates a custom AuthenticationProvider that authenticates users against the current
 * set of internally-created users (i.e. users not authenticated against an external
 * system like LDAP).
 */
@Component
class LocalUserAuthenticationProviderFactory: FactoryBean<AuthenticationProvider> {

    private class TestProvider: AuthenticationProvider {
        override fun authenticate(auth: Authentication?): Authentication? {
           val isAuthenticated = (auth?.credentials.toString() == "test" && auth?.name == "test")
            if (isAuthenticated) {
                return UsernamePasswordAuthenticationToken(auth!!.name, auth.credentials.toString(), listOf<GrantedAuthority>(SimpleGrantedAuthority("SUPERDUDE")))
            } else {
                return null
            }
        }

        override fun supports(clazz: Class<*>?): Boolean {
            return clazz == UsernamePasswordAuthenticationToken::class.java
        }
    }

    private val provider = TestProvider()

    override fun getObject(): AuthenticationProvider? {
        return provider
    }

    override fun getObjectType(): Class<*>? {
        return AuthenticationProvider::class.java
    }
}