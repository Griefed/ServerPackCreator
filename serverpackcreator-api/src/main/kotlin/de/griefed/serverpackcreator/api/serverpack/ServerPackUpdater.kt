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
package de.griefed.serverpackcreator.api.serverpack

import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.deleteQuietly
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * Everything that makes regenerating a server pack *over an existing one* safe: whether this run is
 * an update at all, which paths it must keep its hands off, and the removal of what the previous
 * run produced but this one did not.
 *
 * It exists because a server pack is routinely run in place. The moment somebody starts a server out
 * of the generated directory, that directory contains a world, a ban-list and a hand-tuned
 * `server.properties` that ServerPackCreator did not write and must never take — while still owing
 * the user a pack that matches their modpack. The previous run's `manifest.json` is what makes the
 * distinction possible: it names exactly what ServerPackCreator produced, so anything else in the
 * directory is somebody else's, and [ApiProperties.updateProtectedPaths] covers the remainder, the
 * files that came *from* ServerPackCreator and have been written to since.
 *
 * @param apiProperties Supplies the update-toggle and the protected paths.
 * @param objectMapper  Reads the previous run's manifest back.
 *
 * @author Griefed
 */
class ServerPackUpdater(
    private val apiProperties: ApiProperties,
    private val objectMapper: ObjectMapper
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Whether regenerating into [serverPack] is an update: updating must be enabled *and* a previous
     * run must have left a manifest there. Without that manifest nothing can tell which files
     * ServerPackCreator produced, so there is nothing to update — only to generate.
     */
    fun isUpdateRun(serverPack: File): Boolean =
        apiProperties.isUpdatingServerPacksEnabled && ServerPackManifest.inside(serverPack).isFile

    /**
     * Whether [relativePath] is covered by [ApiProperties.updateProtectedPaths] — matching either the
     * entry itself or anything beneath it, so `world` protects the whole world. Separators and case
     * are normalised first, because a manifest written on Windows carries backslashes and a pack may
     * well be updated on another machine than it was generated on.
     */
    fun protects(relativePath: String): Boolean {
        val candidate = normalize(relativePath)
        if (candidate.isEmpty()) {
            return false
        }
        return apiProperties.updateProtectedPaths.any { entry ->
            val protected = normalize(entry)
            protected.isNotEmpty() &&
                    (candidate.equals(protected, ignoreCase = true) ||
                            candidate.startsWith("$protected/", ignoreCase = true))
        }
    }

    /**
     * Whether this run must leave `serverPack/relativePath` exactly as it found it: only on an
     * update, only for a protected path, and only when the file is actually there. The last
     * condition is what keeps a *first* generation able to ship a world or a `server.properties` at
     * all — protection guards what exists, it does not forbid creating it.
     */
    fun preserves(serverPack: File, relativePath: String): Boolean =
        isUpdateRun(serverPack) && protects(relativePath) && File(serverPack, relativePath).exists()

    /**
     * [file] as a `/`-separated path relative to [serverPack], or `null` when it lies outside the
     * pack — which is a mis-resolved destination rather than something to record, so it is logged
     * and dropped instead of ending up in the manifest as garbage.
     */
    fun relativize(serverPack: File, file: File): String? {
        val root = serverPack.absoluteFile.toPath().normalize()
        val target = file.absoluteFile.toPath().normalize()
        val relative = try {
            root.relativize(target).toString()
        } catch (ex: IllegalArgumentException) {
            log.warn("Can not express ${file.absolutePath} relative to ${serverPack.absolutePath}.", ex)
            return null
        }
        val normalized = normalize(relative)
        if (normalized.isEmpty() || normalized == ".." || normalized.startsWith("../")) {
            log.warn("Skipping ${file.absolutePath}; it is not inside the server pack at ${serverPack.absolutePath}.")
            return null
        }
        return normalized
    }

    /**
     * Removes from [serverPack] everything the previous run's manifest lists that [produced] does
     * not — the files the modpack no longer contains — and then the directories that leaves empty,
     * deepest first. Protected paths are never touched, and neither is anything absent from the old
     * manifest, which is how a world and an `ops.json` survive.
     *
     * An empty [produced] means this run copied nothing, which is a broken run and not an empty
     * modpack; pruning against it would delete the whole pack, so it prunes nothing instead.
     */
    fun prune(serverPack: File, produced: Set<String>) {
        if (produced.isEmpty()) {
            log.warn("This run produced no files at all. Refusing to prune, so the existing server pack survives.")
            return
        }
        val previous = readManifest(serverPack) ?: return
        val keep = produced.mapTo(HashSet()) { normalize(it).lowercase() }
        val emptiedDirectories = mutableListOf<String>()
        for (entry in previous.files) {
            val relative = normalize(entry)
            if (relative.isEmpty() || keep.contains(relative.lowercase()) || protects(relative)) {
                continue
            }
            val stale = File(serverPack, relative)
            when {
                stale.isDirectory -> emptiedDirectories.add(relative)
                stale.isFile -> {
                    log.debug("Removing ${stale.absolutePath}; the modpack no longer contains it.")
                    stale.deleteQuietly()
                }
            }
        }
        // Deepest first, so a parent left empty by the removal of its children can go with them.
        for (relative in emptiedDirectories.sortedByDescending { it.count { character -> character == '/' } }) {
            val directory = File(serverPack, relative)
            if (directory.isDirectory && directory.list()?.isEmpty() == true) {
                log.debug("Removing the now-empty directory ${directory.absolutePath}.")
                directory.delete()
            }
        }
    }

    /**
     * The previous run's manifest, or `null` when there is none or it can not be read. An
     * unreadable manifest is reported and then treated as no manifest, because the alternative —
     * guessing — would delete files on the strength of a file we just failed to understand.
     */
    fun readManifest(serverPack: File): ServerPackManifest? {
        val manifest = ServerPackManifest.inside(serverPack)
        if (!manifest.isFile) {
            return null
        }
        return try {
            objectMapper.readValue(manifest, ServerPackManifest::class.java)
        } catch (ex: Exception) {
            log.error("Could not read ${manifest.absolutePath}. Nothing will be pruned from this server pack.", ex)
            null
        }
    }

    /**
     * A path in the one shape everything here compares against: `/`-separated, with no leading or
     * trailing separator. Without it a manifest written on Windows would match nothing on Linux.
     */
    private fun normalize(path: String): String =
        path.replace('\\', '/').trim('/')
}
