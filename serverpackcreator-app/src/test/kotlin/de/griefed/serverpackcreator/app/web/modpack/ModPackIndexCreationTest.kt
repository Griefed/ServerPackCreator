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
package de.griefed.serverpackcreator.app.web.modpack

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import java.util.Properties

/**
 * Guards that the index the upload duplicate-check is built on actually comes into existence.
 *
 * `@Indexed` alone does **not** create an index: Spring Data MongoDB's `MongoMappingContext` defaults
 * `autoIndexCreation` to `false`, and Spring Boot only overrides it when
 * `spring.data.mongodb.auto-index-creation` is present. Declaring the annotation and never enabling the
 * switch leaves the duplicate-check's hash lookup a collection scan while every doc comment claims
 * otherwise, which is exactly the state an audit found. Both halves are asserted here because either one
 * alone is worthless.
 */
internal class ModPackIndexCreationTest {

    /** A mapping context that knows [ModPack], built the way Spring Boot builds its own. */
    private fun mappingContext(): MongoMappingContext {
        val context = MongoMappingContext()
        context.setSimpleTypeHolder(MongoCustomConversions(emptyList<Any>()).simpleTypeHolder)
        context.setInitialEntitySet(setOf(ModPack::class.java))
        context.afterPropertiesSet()
        return context
    }

    /**
     * The `application.properties` the running app reads — deliberately **not** via
     * `getResourceAsStream`, because `src/test/resources` ships its own copy that shadows it on the test
     * classpath. Every copy is enumerated and the test one discarded, so this asserts against what is
     * actually shipped.
     */
    private fun shippedProperties(): Properties {
        val copies = ModPackIndexCreationTest::class.java.classLoader
            .getResources("application.properties")
            .toList()
            .filterNot { it.path.contains("/resources/test/") || it.path.contains("/test-classes/") }
        Assertions.assertEquals(
            1, copies.size,
            "Expected exactly one non-test application.properties on the classpath, found: $copies"
        )
        val properties = Properties()
        copies.single().openStream().use { properties.load(it) }
        return properties
    }

    /**
     * Pins that the shipped configuration switches annotation-driven index creation on. Without this
     * property the mapping context keeps its `false` default and no `@Indexed` in the module has any
     * effect.
     */
    @Test
    fun theShippedConfigurationCreatesDeclaredIndexes() {
        Assertions.assertEquals(
            "true",
            shippedProperties().getProperty("spring.data.mongodb.auto-index-creation"),
            "Without spring.data.mongodb.auto-index-creation=true, @Indexed creates nothing and the " +
                    "upload duplicate-check silently scans the collection"
        )
    }

    /**
     * Pins that an index on `sha256` is what the switch above will create, by running Spring Data's own
     * resolver over the mapped entity rather than by asserting the annotation is present.
     */
    @Test
    fun anIndexOnTheUploadHashIsResolvedFromTheEntity() {
        val resolved = MongoPersistentEntityIndexResolver(mappingContext())
            .resolveIndexFor(ModPack::class.java)
            .map { it.indexKeys.keys }
            .flatten()

        Assertions.assertTrue(
            resolved.contains("sha256"),
            "ModPack.sha256 must resolve to an index — it is the key the duplicate-check looks up. " +
                    "Resolved instead: $resolved"
        )
    }
}
