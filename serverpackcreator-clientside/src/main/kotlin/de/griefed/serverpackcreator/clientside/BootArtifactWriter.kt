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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Writes every kept boot attempt's evidence to disk, one directory per attempt, under [root].
 *
 * The counterpart of the grinder's budgeted `BootLogStore` for callers that simply want the files: it is
 * what makes [BootArtifacts] reachable from the CLI verb, whose only consumer is a CI job uploading the
 * tree afterwards. Retention is [BootArtifacts.worthKeeping], shared with the daemon so the two cannot
 * disagree about which boots are worth reading.
 *
 * **Why a directory per attempt rather than per candidate.** Staging wipes and re-creates
 * `<work>/boot/<attemptName>` before every attempt, and the newest-build and other-version re-checks all
 * stage into the *crashing* attempt's directory on purpose — so one candidate's three boots share one path
 * and overwrite each other. Anything read after the run can only ever see the last of them, which is
 * rarely the one the verdict rests on.
 *
 * @param root Directory the attempt directories are created under; created on demand.
 * @author Griefed
 */
class BootArtifactWriter(private val root: File) {

    /** Attempts counted per attempt name, so the numbering is per candidate rather than global. */
    private val attemptCounters = ConcurrentHashMap<String, AtomicInteger>()

    /**
     * Keep [outcome]'s evidence for [pack], returning the files written — empty when the boot is not worth
     * keeping or produced nothing. Throws rather than swallowing: `BootVerifier` already guards the sink,
     * and a writer that hides a full disk is a writer that reports success having kept nothing.
     */
    fun keep(pack: BootVerifier.Prepared.Ready, outcome: BootVerifier.BootOutcome): List<File> {
        if (!BootArtifacts.worthKeeping(outcome.result)) {
            return emptyList()
        }
        val artifacts = BootArtifacts.collect(pack.serverPack, outcome.console)
        if (artifacts.isEmpty()) {
            return emptyList()
        }
        val directory = directoryFor(pack)
        directory.mkdirs()
        return artifacts.map { artifact ->
            File(directory, artifact.name).apply { writeText(artifact.content) }
        }
    }

    /**
     * `<root>/<attemptName>/<n>-<loader>-<loaderVersion>`. The ordinal is what guarantees uniqueness — a
     * newest-build re-check differs from the boot it re-checks in the loader *version* alone, and an
     * other-version re-check can repeat a tuple outright — while the loader build is what lets a reader
     * tell the attempts apart without a verdict beside them.
     */
    private fun directoryFor(pack: BootVerifier.Prepared.Ready): File {
        val attempt = attemptCounters.computeIfAbsent(pack.attemptName) { AtomicInteger() }.incrementAndGet()
        val build = sanitise("${pack.loader}-${pack.loaderVersion}")
        return File(File(root, pack.attemptName), "$attempt-$build")
    }

    /**
     * Reduce [name] to characters every filesystem in the pipeline accepts. A loader version is dotted
     * numerics today, so this normally changes nothing; it exists because the value is upstream data and a
     * path separator arriving in one would write outside the attempt directory.
     */
    private fun sanitise(name: String): String = name.map { character ->
        if (character.isLetterOrDigit() || character in "._-") character else '_'
    }.joinToString("")
}
