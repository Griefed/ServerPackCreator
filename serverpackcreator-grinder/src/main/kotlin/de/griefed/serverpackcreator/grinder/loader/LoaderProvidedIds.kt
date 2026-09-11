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
package de.griefed.serverpackcreator.grinder.loader

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.zip.ZipFile

/**
 * What an **installed** modloader declares it provides, read out of the install layer itself.
 *
 * **Why it has to be read rather than known.** A Fabric-family loader answers to more ids than its own:
 * quilt-loader's `quilt.mod.json` declares `provides: [{ "id": "fabricloader", "version": "0.19.3" }]`, and
 * the version differs per build — `0.30.1` provides `0.19.3` while `0.31.0-beta.4` provides `0.19.5`. A
 * table of those pairs would be a snapshot that goes stale with every loader release, which is the
 * un-pinned-lookup shape this repository has paid for repeatedly; the jar on disk is the fact.
 *
 * **What it is for.** `fabricloader` is environment-provided, so staging never downloads it and
 * `DependencyBacktrack` had nothing to compare a demand against — a requirement naming it looked like a
 * requirement naming something absent, which that judge skips by design. Measured on the public grinder
 * 2026-09-11: **twelve** published `DEPENDENCY_FAILURE` rows are `fabric-language-kotlin` demanding
 * `fabricloader [0.19.5, ∞)` against the `0.19.3` quilt-loader 0.30.1 provides. With the pair in hand the
 * demanding jar is demoted to a build the installed loader can satisfy, exactly as any other version
 * conflict is.
 *
 * Answers an empty map for anything it cannot read — no install, no descriptor, an unreadable archive — so
 * the judge falls back to the behaviour it had before this existed rather than refusing on doubt.
 *
 * @author Griefed
 */
class LoaderProvidedIds(private val cache: LoaderCache) {

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    private val mapper = jacksonObjectMapper()

    /**
     * The id → version pairs the installed [loader] build declares it provides, including its own id.
     *
     * Its own id is included because a mod may depend on `quilt_loader` or `fabricloader` by name, and the
     * difference between "the loader itself" and "an id it stands in for" is one the *demand* does not draw.
     */
    fun of(loader: String, loaderVersion: String, minecraftVersion: String): Map<String, String> {
        val baseDir = cache.baseDirFor(loader, loaderVersion, minecraftVersion)
        if (!baseDir.isDirectory) {
            return emptyMap()
        }
        val jar = loaderJarIn(baseDir) ?: return emptyMap()
        return runCatching { providesOf(jar) }
            .onFailure { log.debug("Could not read what $loader $loaderVersion provides: ${it.message}") }
            .getOrDefault(emptyMap())
    }

    /**
     * The loader's own jar inside an install layer, or `null`.
     *
     * Found by name because that is what the installers write and nothing inside the layer indexes it:
     * `libraries/org/quiltmc/quilt-loader/<v>/quilt-loader-<v>.jar`,
     * `libraries/net/fabricmc/fabric-loader/<v>/fabric-loader-<v>.jar`. The **newest** by name wins where a
     * layer somehow holds two, so a re-install that left the old jar behind cannot decide the answer.
     *
     * Forge and NeoForge are not looked for at all: they publish no `provides` block, so there would be
     * nothing to read, and an empty answer is already the right one.
     */
    private fun loaderJarIn(baseDir: File): File? = baseDir.walkTopDown()
        .maxDepth(MAX_DEPTH)
        .filter { it.isFile && LOADER_JAR.matches(it.name) }
        .maxByOrNull { it.name }

    /** The `provides` block of [jar]'s own descriptor, plus its own id, as id → version. */
    private fun providesOf(jar: File): Map<String, String> = ZipFile(jar).use { archive ->
        val descriptor = readDescriptor(archive, "quilt.mod.json")
            ?: readDescriptor(archive, "fabric.mod.json")
            ?: return emptyMap()
        val root = if (descriptor.has(QUILT_LOADER)) descriptor.path(QUILT_LOADER) else descriptor
        val own = root.path("id").textOrNull()?.let { id ->
            root.path("version").textOrNull()?.let { version -> id to version }
        }
        val provided = root.path("provides").mapNotNull { node ->
            val id = if (node.isTextual) node.asText() else node.path("id").textOrNull()
            val version = if (node.isTextual) root.path("version").textOrNull() else node.path("version").textOrNull()
            if (id.isNullOrBlank() || version.isNullOrBlank()) null else id to version
        }
        (listOfNotNull(own) + provided).toMap()
    }

    /** Parse one descriptor out of [archive], or `null` when absent or unreadable. */
    private fun readDescriptor(archive: ZipFile, name: String): JsonNode? {
        val entry = archive.getEntry(name) ?: return null
        return runCatching { archive.getInputStream(entry).use { mapper.readTree(it) } }.getOrNull()
    }

    /** `asText(null)` yields the literal `"null"` for a JSON null, which would read as a real value. */
    private fun JsonNode.textOrNull(): String? = if (isTextual) asText().takeIf { it.isNotBlank() } else null

    private companion object {
        /** Quilt nests everything under this key; Fabric puts the same fields at the top level. */
        const val QUILT_LOADER = "quilt_loader"

        /** The two loader jars that carry a `provides` block, by the name their installers write. */
        val LOADER_JAR = Regex("""(quilt-loader|fabric-loader)-.+\.jar""")

        /**
         * How deep to look. An install layer is `libraries/<group path>/<artifact>/<version>/<jar>`, which is
         * six or seven levels; the cap is what stops a walk of a whole cache if this is ever handed the wrong
         * directory.
         */
        const val MAX_DEPTH = 10
    }
}
