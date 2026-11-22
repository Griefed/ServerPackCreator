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
package de.griefed.serverpackcreator.clientside

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Holds the module's package graph to the "May depend on" table in `serverpackcreator-clientside/CLAUDE.md`, and
 * therefore acyclic. Edges come from code: an import counts only when the imported name is used outside comments
 * and import lines, because some imports exist only so a KDoc link resolves.
 */
internal class PackageGraphTest {

    private val sourceRoot = File("src/main/kotlin/de/griefed/serverpackcreator/clientside")

    /** The table, with the root package as `""`. */
    private val mayDependOn = mapOf(
        "" to setOf("boot", "verdict", "selection", "platform", "metadata", "dependency", "version"),
        "boot" to setOf("verdict", "selection", "platform", "metadata", "dependency", "version"),
        "verdict" to setOf("platform"),
        "selection" to setOf("platform", "version"),
        "platform" to setOf("metadata"),
        "metadata" to setOf("version"),
        "dependency" to setOf("version"),
        "version" to emptySet()
    )

    @Test
    fun everyPackageDependsOnlyOnWhatTheTableAllows() {
        val graph = PackageGraph.of(sourceRoot)
        Assertions.assertTrue(graph.isNotEmpty(), "no sources found under ${sourceRoot.absolutePath}")

        val violations = graph.flatMap { (from, targets) ->
            targets.filterNot { it in mayDependOn[from].orEmpty() }.map { "'$from' -> '$it'" }
        }
        Assertions.assertEquals(emptyList<String>(), violations, "edges the CLAUDE.md table does not allow")
        Assertions.assertEquals(mayDependOn.keys, mayDependOn.keys + graph.keys, "a package the table does not list")
    }

    @Test
    fun thePackageGraphHasNoCycle() {
        Assertions.assertNull(PackageGraph.cycleIn(PackageGraph.of(sourceRoot)))
    }

    /** The reader must ignore what only documentation uses, and still see what code uses, or the guards mean nothing. */
    @Test
    fun aKDocOnlyImportIsNoEdgeAndACodeUseIs() {
        val source = """
            package de.griefed.serverpackcreator.clientside.version

            import de.griefed.serverpackcreator.clientside.boot.BootVerifier
            import de.griefed.serverpackcreator.clientside.platform.ModFile

            /** See [BootVerifier]. https://example.org/a//b */
            fun size(file: ModFile) = 1 // BootVerifier is only mentioned here
        """.trimIndent()

        Assertions.assertEquals(setOf("platform"), PackageGraph.edgesOf(source, "version"))
    }

    @Test
    fun aCycleIsFound() {
        Assertions.assertEquals(
            listOf("a", "b", "a"),
            PackageGraph.cycleIn(mapOf("a" to setOf("b"), "b" to setOf("a")))
        )
    }

    /** Package edges read from the module's sources. */
    private object PackageGraph {
        private const val BASE = "de.griefed.serverpackcreator.clientside"
        private val importLine = Regex("""^import\s+${Regex.escape(BASE)}\.([a-z]+)\.(\w+)""", RegexOption.MULTILINE)
        private val qualifiedUse = Regex("""${Regex.escape(BASE)}\.([a-z]+)\.[A-Za-z]""")
        private val blockComment = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
        private val lineComment = Regex("""(?<!:)//.*""")
        private val headerLine = Regex("""^(import|package)\s.*$""", RegexOption.MULTILINE)

        /** Each package (root as `""`) mapped to the other packages its code uses. */
        fun of(root: File): Map<String, Set<String>> =
            root.walkTopDown().filter { it.isFile && it.extension == "kt" }
                .groupBy { it.parentFile.relativeTo(root).path.replace(File.separatorChar, '.') }
                .mapValues { (pkg, files) -> files.flatMap { edgesOf(it.readText(), pkg) }.toSet() }

        /** The packages [source], a file in [pkg], uses in code. */
        fun edgesOf(source: String, pkg: String): Set<String> {
            val code = source.replace(blockComment, "").replace(lineComment, "").replace(headerLine, "")
            val imported = importLine.findAll(source)
                .filter { Regex("""\b${Regex.escape(it.groupValues[2])}\b""").containsMatchIn(code) }
                .map { it.groupValues[1] }
            val qualified = qualifiedUse.findAll(code).map { it.groupValues[1] }
            return (imported + qualified).filter { it != pkg }.toSet()
        }

        /** A cycle in [graph] as the path that closes it, or `null`. */
        fun cycleIn(graph: Map<String, Set<String>>): List<String>? {
            val done = mutableSetOf<String>()
            fun visit(node: String, path: List<String>): List<String>? {
                if (node in path) {
                    return path.drop(path.indexOf(node)) + node
                }
                if (!done.add(node)) {
                    return null
                }
                return graph[node].orEmpty().firstNotNullOfOrNull { visit(it, path + node) }
            }
            return graph.keys.firstNotNullOfOrNull { visit(it, emptyList()) }
        }
    }
}
