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

import java.io.File
import java.io.RandomAccessFile

/**
 * Everything a finished boot has to say about itself: the container console plus the server's own
 * `logs/` and `crash-reports/`. Kept as *separate* entries rather than one blob because they disagree
 * in useful ways — `logs/latest.log` is log4j's file appender, so it carries entries stdout never sees
 * and misses the shell/launcher output stdout has, and that difference is exactly what a disputed
 * verdict turns on.
 *
 * Pure apart from reading the pack it is handed, so it is unit-testable without a container, and it
 * lives here rather than in the grinder so the CLI verb and the daemon cannot disagree about which
 * boots are worth keeping.
 *
 * @author Griefed
 */
object BootArtifacts {

    /** Name given to the captured container console, distinguishing it from the server's own logs. */
    const val CONSOLE_NAME = "console.log"

    /** Name of the manifest listing every file that existed, kept or not — what makes the caps observable. */
    const val INDEX_NAME = "index.txt"

    /** Most log/crash-report files kept per attempt, newest first; the rest are named in the index only. */
    const val MAX_ARTIFACTS = 8

    /** Per-artifact ceiling. Beyond it the **tail** is kept, since a stack trace ends a log rather than starts it. */
    const val MAX_BYTES_PER_ARTIFACT = 2 * 1024 * 1024

    /** Prepended to a truncated artifact, so nobody reasons about a partial log believing it complete. */
    const val TRUNCATION_NOTICE = "[... truncated: only the last $MAX_BYTES_PER_ARTIFACT bytes are kept ...]\n"

    /** Directories inside a staged pack whose contents are evidence. The pack itself is reproducible. */
    private val evidenceDirectories = listOf("logs", "crash-reports")

    /** Rotated archives: a boot of minutes never rotates, so decompressing one to find out is cost for nothing. */
    private val archiveSuffixes = listOf(".gz", ".zip", ".xz", ".bz2")

    /** One captured file: its flattened [name], its (possibly truncated) [content], and whether it was cut. */
    data class Artifact(
        /** Flattened name — `console.log`, or `<directory>-<file>` so a pack's tree becomes a flat store. */
        val name: String,
        /** The file's text, or its last [MAX_BYTES_PER_ARTIFACT] bytes behind [TRUNCATION_NOTICE]. */
        val content: String,
        /** Whether [content] is a tail rather than the whole file. */
        val truncated: Boolean
    )

    /**
     * Whether a boot with this [result] is worth keeping artifacts for. A boot that reached its ready-line
     * explains nothing and proves nothing about sideness; everything else is either evidence or a failure
     * of the checking itself, and both are worth being able to read afterwards.
     */
    fun worthKeeping(result: BootResult): Boolean = result != BootResult.SURVIVED

    /**
     * Collect [serverPack]'s evidence together with [console]. Files are taken newest-first and capped at
     * [MAX_ARTIFACTS]; whatever existed is named in an [INDEX_NAME] entry either way, so a reader can tell
     * "there was nothing else" from "we chose not to keep it". Returns empty when there is nothing to say,
     * rather than an index announcing its own emptiness.
     */
    fun collect(serverPack: File, console: String?): List<Artifact> {
        val found = evidenceDirectories.flatMap { directory ->
            File(serverPack, directory).listFiles().orEmpty()
                .filter { it.isFile }
                .map { directory to it }
        }.sortedByDescending { (_, file) -> file.lastModified() }

        val (readable, archived) = found.partition { (_, file) ->
            archiveSuffixes.none { file.name.endsWith(it, ignoreCase = true) }
        }
        val kept = readable.take(MAX_ARTIFACTS)
        val dropped = readable.drop(MAX_ARTIFACTS)

        if (console == null && found.isEmpty()) {
            return emptyList()
        }

        val artifacts = mutableListOf<Artifact>()
        console?.let { artifacts.add(Artifact(CONSOLE_NAME, it, truncated = false)) }
        for ((directory, file) in kept) {
            artifacts.add(readCapped("$directory-${file.name}", file))
        }
        if (found.isNotEmpty()) {
            artifacts.add(Artifact(INDEX_NAME, indexOf(kept, dropped, archived), truncated = false))
        }
        return artifacts
    }

    /**
     * The manifest of what was found. Every file is listed with why it is or is not present, because a
     * silently-capped set of logs is indistinguishable from a complete one.
     */
    private fun indexOf(
        kept: List<Pair<String, File>>,
        dropped: List<Pair<String, File>>,
        archived: List<Pair<String, File>>
    ): String = buildString {
        appendLine("Files found in the staged pack (${kept.size} kept, ${dropped.size + archived.size} not kept):")
        kept.forEach { (directory, file) -> appendLine("  kept      $directory/${file.name} (${file.length()} bytes)") }
        dropped.forEach { (directory, file) -> appendLine("  not kept  $directory/${file.name} — beyond the $MAX_ARTIFACTS-file cap") }
        archived.forEach { (directory, file) -> appendLine("  not kept  $directory/${file.name} — a rotated archive is not read") }
    }

    /**
     * [file]'s text, or its last [MAX_BYTES_PER_ARTIFACT] bytes behind [TRUNCATION_NOTICE], **without ever
     * holding the whole file in memory** — the point of seeking rather than reading and then trimming.
     * An unreadable file becomes an artifact stating so, since the failure to read it is itself evidence.
     */
    private fun readCapped(name: String, file: File): Artifact = runCatching {
        if (file.length() <= MAX_BYTES_PER_ARTIFACT) {
            return@runCatching Artifact(name, file.readText(), truncated = false)
        }
        val tail = ByteArray(MAX_BYTES_PER_ARTIFACT)
        RandomAccessFile(file, "r").use { handle ->
            handle.seek(file.length() - MAX_BYTES_PER_ARTIFACT)
            handle.readFully(tail)
        }
        Artifact(name, TRUNCATION_NOTICE + String(tail, Charsets.UTF_8), truncated = true)
    }.getOrElse { failure ->
        Artifact(name, "[could not be read: ${failure.message}]", truncated = false)
    }
}
