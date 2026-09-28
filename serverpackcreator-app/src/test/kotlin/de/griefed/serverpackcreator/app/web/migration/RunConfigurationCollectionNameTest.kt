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
package de.griefed.serverpackcreator.app.web.migration

import de.griefed.serverpackcreator.app.web.serverpack.customizing.RunConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

/**
 * Guards the one string in the migration that can be wrong without anything saying so.
 *
 * `RunConfigurationListMigrationRunner.COLLECTION` claims to be "the collection Spring Data maps
 * `RunConfiguration` to". If it ever stops being that — a rename, or a `@Document("…")` naming the
 * collection explicitly — `findAll` reads a collection that does not exist, returns nothing, rewrites
 * nothing, drops nothing, logs no failure, and the migration reports success. Every guard in
 * `RunConfigurationListMigrationRunnerTest` still passes, because they all drive the store through this
 * same constant.
 *
 * The three `ORPHANED_COLLECTIONS` cannot be checked this way and deliberately are not: their classes
 * were deleted, which is the whole point of dropping them, so a literal is the only thing left.
 */
internal class RunConfigurationCollectionNameTest {

    /** A mapping context that knows [RunConfiguration], built the way Spring Boot builds its own. */
    private fun mappingContext(): MongoMappingContext {
        val context = MongoMappingContext()
        context.setSimpleTypeHolder(MongoCustomConversions(emptyList<Any>()).simpleTypeHolder)
        context.setInitialEntitySet(setOf(RunConfiguration::class.java))
        context.afterPropertiesSet()
        return context
    }

    /**
     * Pins the migrated collection's name against the mapping it mirrors, by asking Spring Data rather
     * than by repeating its naming rule.
     */
    @Test
    fun theMigratedCollectionIsTheOneRunConfigurationsAreStoredIn() {
        val mapped = mappingContext()
            .getRequiredPersistentEntity(RunConfiguration::class.java)
            .collection

        Assertions.assertEquals(
            mapped, RunConfigurationListMigrationRunner.COLLECTION,
            "The migration would silently rewrite nothing: it reads '" +
                    "${RunConfigurationListMigrationRunner.COLLECTION}' while run-configurations are " +
                    "stored in '$mapped'"
        )
    }
}
