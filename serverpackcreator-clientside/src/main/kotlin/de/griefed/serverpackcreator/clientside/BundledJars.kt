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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * The mod-ids — and the versions — a jar already carries inside itself, via Fabric/Quilt **jar-in-jar**.
 *
 * A mod may ship its own libraries as nested jars, which the loader puts on the classpath — so a dependency
 * naming one of them is satisfied before anything is downloaded. Nothing here looked, which is how
 * `xaeros-world-map` came to be refused for `xaerolib`: its Quilt/26.2 jar declares
 * `depends: { "xaerolib": ">=1.0" }` **and** ships `META-INF/jars/xaerolib-fabric-26.2-1.7.1.jar`.
 *
 * **The near-miss is what made that fatal.** A Modrinth project `xaerolib` exists, so the manifest id
 * *mapped* — but publishes nothing tagged Quilt or 26.2, so nothing could be staged, and a
 * mapped-then-unstageable id refuses the boot where an unmappable one would not have. Jar-in-jar is
 * ordinary (sodium ships nine), so this is a class of false refusal rather than one mod's quirk.
 *
 * **The version half exists for a different question.** [idsIn] keeps staging from re-downloading
 * something already bundled; [versionsIn] lets `DependencyBacktrack` see that bundled copy as part of the
 * pack, so a requirement contradicting *it* is a conflict rather than a silence. Measured live 2026-09-07:
 * `create` demanding `ponder [1.0.82,)` against the `1.0.64` nested in another jar booted anyway, and the
 * candidate wore the resulting INCONCLUSIVE.
 *
 * **Only what the descriptor declares counts, and that restraint is the load-bearing part.** Fabric loads
 * the jars its descriptor lists; a stray file under `META-INF/jars/` is not on the classpath. Treating one
 * as satisfied would skip staging something genuinely needed and produce a failure to blame on the mod —
 * the one direction in which being generous here is dangerous.
 *
 * @author Griefed
 */
object BundledJars {

    /** Fabric declares `jars: [{ "file": "…" }]` at the top level. */
    private const val FABRIC_JARS = "jars"

    /** Quilt declares `quilt_loader.jars: [ "…" ]` — a list of plain strings, not objects. */
    private const val QUILT_LOADER = "quilt_loader"

    private val mapper = jacksonObjectMapper()

    /**
     * Every mod-id [jar] provides from its own nested jars — each nested descriptor's `id` plus its
     * `provides` aliases, since a dependant may name any of them.
     *
     * Empty for a jar that bundles nothing, declares nothing, or cannot be read at all: staging must not die
     * on a malformed file, and "we could not look" has to mean "assume nothing is bundled" so a genuinely
     * missing dependency is still staged.
     */
    fun idsIn(jar: File): Set<String> = nestedDescriptorsOf(jar).flatMap { idsOf(it) }.toSet()

    /**
     * The **version** each nested mod-id is shipped at, for the ids whose nested descriptor states one.
     *
     * Where [idsIn] answers *"is this dependency already inside the jar?"*, this answers *"and which build
     * of it?"* — the question `BootVerifier.dependencyToDemote` has to ask, because a library the loader
     * loads out of `META-INF/jars/` is on the classpath just as much as a staged file, and a requirement
     * can contradict its version exactly as it can contradict a staged one.
     *
     * **An id shipped at two different versions by one jar is omitted rather than guessed at.** Which copy
     * a loader picks is loader-specific resolution behaviour, and this module's standing rule is to fail
     * toward proceeding: no opinion costs at most a missed conflict, while a wrong one manufactures a
     * demotion. An id whose descriptor states no version is likewise absent — [idsIn] still reports it, so
     * it still counts as *present*, just not as a version anything can be compared against.
     */
    fun versionsIn(jar: File): Map<String, String> {
        val perId = mutableMapOf<String, MutableSet<String>>()
        for (descriptor in nestedDescriptorsOf(jar)) {
            val version = versionOf(descriptor) ?: continue
            idsOf(descriptor).forEach { id -> perId.getOrPut(id) { mutableSetOf() }.add(version) }
        }
        return perId.filterValues { it.size == 1 }.mapValues { (_, versions) -> versions.single() }
    }

    /**
     * Every nested jar's descriptor in [jar], parsed once so [idsIn] and [versionsIn] read the same jars by
     * the same rules.
     *
     * Empty for a jar that bundles nothing, declares nothing, or cannot be read at all: staging must not die
     * on a malformed file, and "we could not look" has to mean "assume nothing is bundled" so a genuinely
     * missing dependency is still staged.
     */
    private fun nestedDescriptorsOf(jar: File): List<JsonNode> = runCatching {
        ZipFile(jar).use { archive ->
            val descriptor = readDescriptor(archive, "fabric.mod.json")
                ?: readDescriptor(archive, "quilt.mod.json")
                ?: return emptyList()
            declaredNestedPaths(descriptor).mapNotNull { path -> descriptorOfNested(archive, path) }
        }
    }.getOrDefault(emptyList())

    /** Parse one descriptor out of [archive], or `null` when absent or unreadable. */
    private fun readDescriptor(archive: ZipFile, name: String): JsonNode? {
        val entry = archive.getEntry(name) ?: return null
        return runCatching { archive.getInputStream(entry).use { mapper.readTree(it) } }.getOrNull()
    }

    /**
     * The nested-jar paths [descriptor] declares, in either loader's spelling — Fabric's list of objects
     * carrying a `file`, and Quilt's list of plain strings under `quilt_loader`.
     */
    private fun declaredNestedPaths(descriptor: JsonNode): List<String> {
        val fabric = descriptor.path(FABRIC_JARS).mapNotNull { it.path("file").textOrNull() }
        val quilt = descriptor.path(QUILT_LOADER).path(FABRIC_JARS).mapNotNull { node ->
            if (node.isTextual) node.asText() else node.path("file").textOrNull()
        }
        return fabric + quilt
    }

    /**
     * The descriptor of the nested jar at [path], which is where its id, aliases and version come from —
     * never its file name, since a name like `xaerolib-fabric-26.2-1.7.1.jar` carries a version and a
     * loader the declared id does not, and states them in a shape nothing should be parsing.
     *
     * **Streamed, never spooled to disk.** An earlier cut wrote each nested jar to a temp file with
     * `deleteOnExit()`; that registers the path in `java.io.DeleteOnExitHook`'s static set, which never
     * shrinks even after the file is deleted. This runs per staged jar, per boot attempt, for every
     * candidate of a catalog sweep — `sodium` declares nine nested jars — so a daemon running for weeks
     * accumulated a dead entry for each. Reading the entry as a [ZipInputStream] needs no file at all.
     */
    private fun descriptorOfNested(archive: ZipFile, path: String): JsonNode? {
        val entry = archive.getEntry(path) ?: return null
        return runCatching {
            archive.getInputStream(entry).use { raw ->
                ZipInputStream(raw).use { nested ->
                    generateSequence { nested.nextEntry }
                        .firstOrNull { it.name == "fabric.mod.json" || it.name == "quilt.mod.json" }
                        // Read from the stream positioned at this entry; `mapper` stops at its end.
                        ?.let { mapper.readTree(nested) }
                }
            }
        }.getOrNull()
    }

    /** The version a nested descriptor states for itself, in either loader's spelling. */
    private fun versionOf(descriptor: JsonNode): String? {
        val loaderNode = if (descriptor.has(QUILT_LOADER)) descriptor.path(QUILT_LOADER) else descriptor
        return loaderNode.path("version").textOrNull()
    }

    /** A descriptor's own id plus its `provides` aliases, in either loader's spelling. */
    private fun idsOf(descriptor: JsonNode): Set<String> {
        val loaderNode = if (descriptor.has(QUILT_LOADER)) descriptor.path(QUILT_LOADER) else descriptor
        val id = loaderNode.path("id").textOrNull()
        val provides = loaderNode.path("provides").mapNotNull { node ->
            if (node.isTextual) node.asText() else node.path("id").textOrNull()
        }
        return (listOfNotNull(id) + provides).filter { it.isNotBlank() }.toSet()
    }

    /** `asText(null)` yields the literal `"null"` for a JSON null, which would read as a real id. */
    private fun JsonNode.textOrNull(): String? = if (isTextual) asText().takeIf { it.isNotBlank() } else null
}
