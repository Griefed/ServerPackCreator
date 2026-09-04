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

/**
 * The mod-ids a jar already carries inside itself, via Fabric/Quilt **jar-in-jar**.
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
    fun idsIn(jar: File): Set<String> = runCatching {
        ZipFile(jar).use { archive ->
            val descriptor = readDescriptor(archive, "fabric.mod.json")
                ?: readDescriptor(archive, "quilt.mod.json")
                ?: return emptySet()
            declaredNestedPaths(descriptor)
                .flatMap { path -> idsOfNested(archive, path) }
                .toSet()
        }
    }.getOrDefault(emptySet())

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
     * The ids the nested jar at [path] provides, read from *its* descriptor rather than guessed from its
     * file name — names like `xaerolib-fabric-26.2-1.7.1.jar` carry a version and a loader that the declared
     * id does not.
     */
    private fun idsOfNested(archive: ZipFile, path: String): Set<String> {
        val entry = archive.getEntry(path) ?: return emptySet()
        val nested = runCatching {
            archive.getInputStream(entry).use { stream ->
                // A nested jar is a zip inside a zip, so it has to be spooled out before it can be opened.
                val spooled = File.createTempFile("spc-nested-", ".jar").apply { deleteOnExit() }
                spooled.outputStream().use { stream.copyTo(it) }
                spooled
            }
        }.getOrNull() ?: return emptySet()

        return try {
            ZipFile(nested).use { inner ->
                val descriptor = readDescriptor(inner, "fabric.mod.json")
                    ?: readDescriptor(inner, "quilt.mod.json")
                    ?: return emptySet()
                idsOf(descriptor)
            }
        } finally {
            nested.delete()
        }
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
