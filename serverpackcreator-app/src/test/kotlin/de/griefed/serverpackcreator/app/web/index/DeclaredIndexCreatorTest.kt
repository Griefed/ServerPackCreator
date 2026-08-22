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

import de.griefed.serverpackcreator.app.web.modpack.ModPack
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.index.IndexDefinition
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

/**
 * Guards what [DeclaredIndexCreator] asks the database for, and when.
 *
 * Both are decisions that can only go wrong silently — an index never created makes queries slower, not
 * wrong, and a failure that escapes takes down an application that is already serving. Asserted against a
 * recording double rather than a live MongoDB, the same seam `RunConfigurationListMigrationRunnerTest`
 * uses.
 */
internal class DeclaredIndexCreatorTest {

    /** Records what was asked for, and optionally refuses. */
    private class RecordingIndexStore(private val failWith: Exception? = null) : IndexStore {
        val created = mutableListOf<Pair<String, IndexDefinition>>()

        override fun create(collection: String, definition: IndexDefinition): String {
            created += collection to definition
            failWith?.let { throw it }
            return definition.indexKeys.keys.joinToString("_")
        }
    }

    /** A mapping context that knows [ModPack], built the way Spring Boot builds its own. */
    private fun mappingContext(): MongoMappingContext {
        val context = MongoMappingContext()
        context.setSimpleTypeHolder(MongoCustomConversions(emptyList<Any>()).simpleTypeHolder)
        context.setInitialEntitySet(setOf(ModPack::class.java))
        context.afterPropertiesSet()
        return context
    }

    /**
     * Pins that the upload-hash index is among what gets created, and on the collection modpacks are
     * stored in. This is the index the duplicate-check depends on; without it that lookup scans.
     */
    @Test
    fun theDeclaredUploadHashIndexIsCreated() {
        val store = RecordingIndexStore()

        DeclaredIndexCreator(mappingContext(), store).createDeclaredIndexes()

        val keysByCollection = store.created.map { it.first to it.second.indexKeys.keys.toList() }
        Assertions.assertTrue(
            keysByCollection.any { it.first == "modPack" && it.second == listOf("sha256") },
            "Expected an index on modPack.sha256, asked for: $keysByCollection"
        )
    }

    /**
     * Pins that a database refusing the request does not propagate. A thrown exception here would surface
     * as a failed `ApplicationReadyEvent` in an application that is already accepting requests.
     */
    @Test
    fun aRefusedIndexDoesNotEscape() {
        val store = RecordingIndexStore(IllegalStateException("no index for you"))

        DeclaredIndexCreator(mappingContext(), store).createDeclaredIndexes()

        Assertions.assertTrue(
            store.created.isNotEmpty(),
            "The creator must have attempted the index before swallowing the failure"
        )
    }

    /**
     * Pins that creation is triggered by `ApplicationReadyEvent` and nothing earlier. Asserted on the
     * annotation because *when* is the whole point: during refresh, an unreachable database cancels the
     * context, which is the regression this class exists to prevent.
     */
    @Test
    fun creationIsTriggeredAfterTheApplicationIsReady() {
        val listener = DeclaredIndexCreator::class.java.methods
            .single { it.isAnnotationPresent(EventListener::class.java) }

        // Compared by name: Kotlin surfaces the annotation's Java `Class<?>[]` member as
        // `Array<KClass<*>>`, so the two sides are never equal as objects however right they are.
        Assertions.assertEquals(
            listOf(ApplicationReadyEvent::class.java.name),
            listener.getAnnotation(EventListener::class.java).value.map { it.java.name },
            "Declared indexes must be created on ApplicationReadyEvent, not during context refresh"
        )
    }
}
