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
package de.griefed.serverpackcreator.api.modscanning

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Pins every shape Quilt's `unless` clause is written in, on the field that carries it —
 * [ModDependency.unlessProvided].
 *
 * **Why this exists as its own guard.** `unlessProvided` shipped with exactly one assertion anywhere, an
 * integration test in `-clientside` that writes the **bare-string** form; the object and array forms
 * `readUnless` also handles were exercised nowhere. This is a parser, in the module published to Maven, and
 * a wrong branch here yields a *plausible* value rather than an error — the class of change this repository
 * requires a test for before the code, precisely because nothing fails loudly when it is wrong.
 *
 * The shapes are Quilt's own: `unless` accepts whatever `depends` accepts, so an entry may be a bare id, an
 * object stating `id` (plus, in the wild, a `versions` range beside it), or an array of either. Only the ids
 * are kept — a consumer asking *"what would satisfy this?"* needs the id, and enforcing a range on the
 * substitute is the loader's business.
 *
 * @author Griefed
 */
internal class UnlessClauseShapesTest {

    // The PROCESSED copy under build/, never src/test/resources -- see MinecraftConstraintTest for why.
    private val modScanner = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties")).modScanner

    private fun quiltJar(dir: File, name: String, depends: String): File =
        File(dir, name).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry("quilt.mod.json"))
                out.write(
                    """{"schema_version":1,"quilt_loader":{"id":"subject","version":"1.0.0",
                       "depends":[$depends]}}""".toByteArray()
                )
                out.closeEntry()
            }
        }

    /** What one jar's scan reports for the single dependency it declares. */
    private fun unlessOf(dir: File, name: String, depends: String): List<String> {
        val scanner = requireNotNull(modScanner.scannerFor("Quilt", "1.21.1")) { "no Quilt scanner" }
        val scanned = scanner.scan(listOf(quiltJar(dir, name, depends)))
        return scanned.single().dependencies.single { it.modID == "quilt_resource_loader" }.unlessProvided
    }

    /** The form every measured jar uses: `geophilic`, `terralith`, `trek` and `true-ending` all write this. */
    @Test
    fun aBareStringNamesOneAlternative(@TempDir dir: File) {
        Assertions.assertEquals(
            listOf("fabric-resource-loader-v0"),
            unlessOf(
                dir, "bare.jar",
                """{"id":"quilt_resource_loader","versions":"*","unless":"fabric-resource-loader-v0"}"""
            )
        )
    }

    /** An object, which is what `depends` entries look like — so `unless` is written that way too. */
    @Test
    fun anObjectIsReadByItsId(@TempDir dir: File) {
        Assertions.assertEquals(
            listOf("fabric-resource-loader-v0"),
            unlessOf(
                dir, "object.jar",
                """{"id":"quilt_resource_loader",
                   "unless":{"id":"fabric-resource-loader-v0","versions":">=0.100.0"}}"""
            ),
            "the id is what a consumer can act on; the range on the substitute is the loader's business"
        )
    }

    /** An array, mixing both forms, because nothing stops an author writing it that way. */
    @Test
    fun anArrayKeepsEveryAlternativeInOrder(@TempDir dir: File) {
        Assertions.assertEquals(
            listOf("fabric-resource-loader-v0", "fabric-api-base"),
            unlessOf(
                dir, "array.jar",
                """{"id":"quilt_resource_loader",
                   "unless":["fabric-resource-loader-v0",{"id":"fabric-api-base"}]}"""
            ),
            "order is preserved: staging tries them in the order the descriptor offers them"
        )
    }

    /** Nothing usable in the clause is the same as no clause — never a blank id something could resolve. */
    @Test
    fun anUnusableClauseYieldsNothing(@TempDir dir: File) {
        Assertions.assertEquals(
            emptyList<String>(),
            unlessOf(dir, "blank.jar", """{"id":"quilt_resource_loader","unless":["   ",{"versions":"*"},7]}"""),
            "a blank id, an object with no id and a number are all absences, not alternatives"
        )
    }

    /** A dependency stating no `unless` reports none — the shape of every non-Quilt descriptor. */
    @Test
    fun noClauseIsAnEmptyList(@TempDir dir: File) {
        Assertions.assertEquals(
            emptyList<String>(),
            unlessOf(dir, "none.jar", """{"id":"quilt_resource_loader","versions":"*"}""")
        )
    }

    /** A bare-string *dependency* (no object at all) cannot carry a clause, and must not throw looking. */
    @Test
    fun aBareStringDependencyCarriesNoClause(@TempDir dir: File) {
        val scanner = requireNotNull(modScanner.scannerFor("Quilt", "1.21.1"))
        val scanned = scanner.scan(listOf(quiltJar(dir, "bareDep.jar", """"quilt_base"""")))

        Assertions.assertEquals(
            emptyList<String>(),
            scanned.single().dependencies.single { it.modID == "quilt_base" }.unlessProvided
        )
    }
}
