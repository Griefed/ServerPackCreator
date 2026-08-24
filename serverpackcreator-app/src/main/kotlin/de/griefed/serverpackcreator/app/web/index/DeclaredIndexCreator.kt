/* Copyright (C) 2026 Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.app.web.index

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.stereotype.Component

/**
 * Creates the indexes the web module's entities declare with `@Indexed`, once the application is up.
 *
 * **Why this exists rather than `spring.data.mongodb.auto-index-creation=true`.** That property makes
 * `MongoTemplate`'s own bean creation create the indexes, during context refresh — measured, an absent
 * MongoDB then costs a ~30 s `createIndexes` wait, a `MongoTimeoutException`, and a cancelled refresh, so
 * the application will not start until the database does. `docker/docker-compose.yml` starts the app
 * alongside its `db` service, so that race is the normal first boot. Running on `ApplicationReadyEvent`
 * instead means an unreachable database delays the indexes rather than blocking the boot — the same trade
 * `RunConfigurationListMigrationRunner` makes, for the same reason.
 *
 * **`@Indexed` stays the single declaration.** The definitions are resolved from the mapping context with
 * Spring Data's own [MongoPersistentEntityIndexResolver], so no index is restated here and adding one to an
 * entity needs no change to this class.
 */
@Component
class DeclaredIndexCreator(
    private val mappingContext: MongoMappingContext,
    private val indexStore: IndexStore
) {

    /** This creator's logger. Index failures are logged and swallowed here, so this is where a missing index shows up. */
    companion object {
        private val log by lazy { cachedLoggerOf(DeclaredIndexCreator::class.java) }
    }

    /**
     * Resolves every declared index from the mapped entities and creates it.
     *
     * Failures are logged and swallowed, deliberately: a missing index makes a query slower, while a
     * thrown exception here would take down an application that is already serving. The same call on the
     * next start creates it, since creating an existing identical index is a server-side no-op.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun createDeclaredIndexes() {
        val resolver = MongoPersistentEntityIndexResolver(mappingContext)
        for (entity in mappingContext.persistentEntities) {
            for (declared in resolver.resolveIndexFor(entity.typeInformation)) {
                try {
                    val created = indexStore.create(declared.collection, declared)
                    log.info("Ensured index '$created' on '${declared.collection}'.")
                } catch (ex: Exception) {
                    log.warn(
                        "Could not create the index declared on '${declared.collection}' for " +
                                "'${declared.path}'. Queries using it will scan until the next start.", ex
                    )
                }
            }
        }
    }
}
