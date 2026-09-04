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
package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.clientside.AttemptDirectory
import de.griefed.serverpackcreator.clientside.BootArtifacts
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * The durable home for the console of a boot that **crashed** — the one artefact a HIGH verdict cannot be
 * re-derived without.
 *
 * **Why staging is not that home.** `BootWorkspaceReaper` already keeps one `boot.log` per attempt
 * directory, but staging *wipes and re-creates* that directory, so the next re-grind of the same
 * `(platform, slug, loader)` destroys the console belonging to the verdict still being published. A crash is
 * the only outcome that reaches [de.griefed.serverpackcreator.clientside.Verdict.CONFIRMED], and its usual
 * cause — a server loading a mod that reaches for a client-only class, `NoClassDefFoundError:
 * net/minecraft/client/…` — is legible from the console and from nothing else. So the crashing consoles are
 * copied out from under the sweep that produced them.
 *
 * **Growth is bounded by the catalog, not by uptime**, because a log is named after its tuple: re-grinding a
 * project replaces its log instead of adding one. That is deliberate — the unbounded-growth failure this
 * daemon already paid for (98 GB across 1750 attempt directories) came from names nothing ever reused.
 *
 * @param directory Where the logs live; created on first write.
 * @author Griefed
 */
class BootLogStore(private val directory: File, private val budgetBytes: Long = DEFAULT_BUDGET_BYTES) {

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Write every artifact of one attempt into the store, returning the names written. [owner] is the
     * attempt directory's name (the `(platform, slug, loader)` tuple), [attemptKey] distinguishes the
     * attempts of one candidate from each other.
     *
     * Failure is an empty list and a warning, never a throw — keeping evidence must not fail a grind that
     * has already produced its verdict.
     */
    fun keep(owner: String, attemptKey: String, artifacts: List<BootArtifacts.Artifact>): List<String> =
        runCatching {
            directory.mkdirs()
            artifacts.map { artifact ->
                val name = fileName(owner, attemptKey, artifact.name)
                File(directory, name).writeText(artifact.content)
                name
            }
        }.getOrElse {
            log.warn("Could not keep the boot artifacts for $owner ($attemptKey): ${it.message}")
            emptyList()
        }

    /**
     * Every artifact kept for a tuple, alphabetical. Rebuilds the prefix and filters on it rather than
     * parsing a stored name apart: slugs and loaders both contain `-`, so a name has no unambiguous split,
     * which is why [ATTEMPT_SEPARATOR] is a character neither of them uses.
     */
    fun namesFor(platform: String, slug: String, loader: String): List<String> {
        val prefix = AttemptDirectory.nameFor(platform, slug, loader) + ATTEMPT_SEPARATOR
        return list().filter { it.startsWith(prefix) }
    }

    /**
     * Delete this tuple's kept artifacts except [keep], returning how many went.
     *
     * **The bound that actually holds.** Naming an attempt after what it booted means a re-grind replaces
     * its own files — but only the ones it writes again. A re-check that samples a different loader or
     * Minecraft line writes *new* names, so the previous grind's files would survive forever and the store
     * would grow with uptime rather than with the catalog. That is the failure this daemon already paid for
     * once, at 98 GB.
     */
    fun pruneExcept(platform: String, slug: String, loader: String, keep: Set<String>): Int =
        namesFor(platform, slug, loader)
            .filterNot { it in keep }
            .count { name -> runCatching { File(directory, name).delete() }.getOrDefault(false) }

    /**
     * Delete oldest-first until the store is under [budgetBytes], returning the bytes reclaimed. The
     * backstop behind the per-attempt caps: retention keeps every non-survived boot, and a daemon that runs
     * for months on a fixed disk needs a ceiling that does not depend on the catalog's shape.
     */
    fun enforceBudget(): Long {
        val files = directory.listFiles().orEmpty().filter { it.isFile }
        var total = files.sumOf { it.length() }
        if (total <= budgetBytes) {
            return 0L
        }
        var reclaimed = 0L
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= budgetBytes) {
                break
            }
            val size = file.length()
            if (runCatching { file.delete() }.getOrDefault(false)) {
                total -= size
                reclaimed += size
            }
        }
        log.info("Boot-log store exceeded ${budgetBytes / (1024 * 1024)} MiB; reclaimed ${reclaimed / 1024} KiB oldest-first.")
        return reclaimed
    }

    /**
     * Move the logs of the superseded `crash-logs` directory into this store once, returning how many were
     * moved, and remove [legacyDirectory] when it empties. They are evidence for verdicts still being
     * published, and leaving them under a name that now contradicts what it holds costs more to explain
     * than to migrate.
     */
    fun adoptLegacy(legacyDirectory: File): Int {
        val legacy = legacyDirectory.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(SUFFIX) }
        if (legacy.isEmpty()) {
            runCatching { legacyDirectory.takeIf { it.isDirectory && it.listFiles().orEmpty().isEmpty() }?.delete() }
            return 0
        }
        directory.mkdirs()
        // Renamed into this store's own shape rather than moved verbatim: a legacy name is `<tuple>.log`
        // with no attempt segment, so `namesFor` -- which filters on `<tuple>~` -- would never find it and
        // the console would be listed on the index yet unreachable from the row it belongs to.
        val moved = legacy.count { file ->
            val adopted = fileName(file.name.removeSuffix(SUFFIX), LEGACY_ATTEMPT, BootArtifacts.CONSOLE_NAME)
            runCatching { file.renameTo(File(directory, adopted)) }.getOrDefault(false)
        }
        runCatching { legacyDirectory.takeIf { it.listFiles().orEmpty().isEmpty() }?.delete() }
        log.info("Adopted $moved log(s) from the superseded ${legacyDirectory.absolutePath}.")
        return moved
    }


    /**
     * Read a kept log by [name], or `null` when there is none.
     *
     * **[name] is untrusted**: it arrives on a query string, and this daemon's report is documented as
     * something an operator may put behind a reverse proxy. So the name is required to be a plain file name
     * within this directory — anything with a separator, any `..`, and anything resolving outside the store
     * reads as absent rather than as a file the daemon's user happens to be able to open.
     */
    fun read(name: String): String? {
        val candidate = File(directory, name)
        if (!isInsideStore(name, candidate)) {
            log.warn("Refusing a crash-log name that escapes the store: '$name'.")
            return null
        }
        return runCatching { candidate.takeIf { it.isFile }?.readText() }.getOrNull()
    }

    /**
     * Every kept artifact's name, alphabetical, so an index page has something stable to list. Recognised by
     * *shape* — `<tuple>~<attempt>~<artifact>` — rather than by extension, so an artifact keeps whatever
     * extension it was born with and a stray file in the directory is still not mistaken for one of ours.
     */
    fun list(): List<String> =
        directory.listFiles()
            ?.filter { it.isFile && it.name.split(ATTEMPT_SEPARATOR).size >= 3 }
            ?.map { it.name }?.sorted()
            ?: emptyList()

    /**
     * Whether [name] addresses a file *directly inside* this store. Checked on the name first — a separator or
     * a `..` segment is rejected before the filesystem is consulted at all — and then confirmed against the
     * canonical paths, which is what catches a symlink pointing out of the directory.
     */
    private fun isInsideStore(name: String, candidate: File): Boolean {
        if (name.isBlank() || name != File(name).name || name == "." || name == "..") {
            return false
        }
        return runCatching {
            candidate.canonicalFile.parentFile == directory.canonicalFile
        }.getOrDefault(false)
    }

    /**
     * One artifact's file name: the attempt directory's own name (from [AttemptDirectory], so staging and
     * this store cannot drift), then the attempt, then the artifact. Joined with [ATTEMPT_SEPARATOR]
     * because both a slug and a loader may contain `-`, which makes the tuple's own name unsplittable.
     *
     * The artifact keeps its **own** extension (`console.log`, `index.txt`) rather than having one appended:
     * a name is recognised as this store's by its *shape* — two separators — which is a stronger check than
     * a suffix anyway, and appending one would leave `…~index.txt.log` for a reader to puzzle over.
     */
    private fun fileName(owner: String, attemptKey: String, artifact: String) =
        "$owner$ATTEMPT_SEPARATOR$attemptKey$ATTEMPT_SEPARATOR$artifact"

    /** Naming vocabulary and the store's default ceiling. */
    companion object {
        /** Extension the superseded `crash-logs` store used, which is what [adoptLegacy] recognises. */
        const val SUFFIX = ".log"

        /**
         * Separator between the tuple, the attempt and the artifact. Deliberately **not** `-`: both a slug
         * and a loader contain those, so a name built with one could never be read back apart.
         */
        const val ATTEMPT_SEPARATOR = "~"

        /**
         * Attempt segment given to a console adopted from the superseded `crash-logs` directory. Those names
         * predate per-attempt keeping and record nothing about what was booted, which is exactly what this
         * placeholder says.
         */
        const val LEGACY_ATTEMPT = "archived-crash"

        /** Default ceiling for the whole store, matching `SPC_GRINDER_BOOT_LOG_BUDGET_MIB`'s documented default. */
        const val DEFAULT_BUDGET_BYTES = 2048L * 1024 * 1024

        /**
         * Names one attempt of a candidate by what it booted. Deterministic rather than a counter, so a
         * re-grind replaces its own attempts instead of accumulating — and unique per candidate, since the
         * first boot, the newest-build re-check and each other-version re-check differ in exactly these.
         */
        fun attemptKey(loader: String, loaderVersion: String, minecraftVersion: String) =
            "${loader}_${loaderVersion}_mc$minecraftVersion"

    }
}
