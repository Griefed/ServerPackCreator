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
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.RandomAccessFile

/**
 * The durable home for the console of a boot that **crashed** — the one artefact a HIGH verdict cannot be
 * re-derived without.
 *
 * **Why staging is not that home.** `BootWorkspaceReaper` already keeps one `boot.log` per attempt
 * directory, but staging *wipes and re-creates* that directory, so the next re-grind of the same
 * `(platform, slug, loader)` destroys the console belonging to the verdict still being published. A crash is
 * the only outcome that reaches [de.griefed.serverpackcreator.clientside.Confidence.HIGH], and its usual
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
class BootLogStore(private val directory: File) {

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Copy [console] into the store under [platform]/[slug]/[loader]'s name, returning that name, or `null`
     * when nothing could be kept.
     *
     * An oversized console is kept **from its tail** with the truncation stated in the file: a mod can spew
     * for minutes before it dies, the stack trace is at the end, and a silently shortened log is one nobody
     * can trust. Failure is a `null` and a warning, never a throw — keeping evidence must not fail a grind
     * that has already produced its verdict.
     */
    fun keep(platform: String, slug: String, loader: String, console: File): String? {
        val name = fileName(platform, slug, loader)
        return runCatching {
            directory.mkdirs()
            File(directory, name).writeText(tailOf(console))
            name
        }.getOrElse {
            log.warn("Could not keep the crash console for $platform/$slug ($loader): ${it.message}")
            null
        }
    }

    /**
     * [console]'s content, or its last [MAX_BYTES] with the truncation stated, **without ever holding the
     * whole file**.
     *
     * The cap has to bound what is *read* and not only what is written. Boot consoles are streamed to disk
     * uncapped, bounded only by the boot timeout, so a chatty mod can leave hundreds of megabytes — which
     * `readText` would then inflate to roughly double as a UTF-16 `String`. [keep]'s `runCatching` catches
     * `Throwable`, so the resulting `OutOfMemoryError` would be swallowed and the daemon would carry on in an
     * unknown heap state: a failure that is worse than the one it hides.
     *
     * Seeks instead. Decoding may clip a multi-byte character at the seek point, which is why the notice sits
     * in front of it — the first line is already declared incomplete.
     */
    private fun tailOf(console: File): String {
        if (console.length() <= MAX_BYTES) {
            return console.readText()
        }
        val tail = ByteArray(MAX_BYTES)
        RandomAccessFile(console, "r").use { file ->
            file.seek(console.length() - MAX_BYTES)
            file.readFully(tail)
        }
        return TRUNCATION_NOTICE + String(tail, Charsets.UTF_8)
    }

    /** The kept log's name for a tuple, or `null` when none is kept — which is what stops a report linking a 404. */
    fun nameFor(platform: String, slug: String, loader: String): String? =
        fileName(platform, slug, loader).takeIf { File(directory, it).isFile }

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

    /** Every kept log's name, alphabetical, so an index page has something stable to list. */
    fun list(): List<String> =
        directory.listFiles()?.filter { it.isFile && it.name.endsWith(SUFFIX) }?.map { it.name }?.sorted()
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
     * The log's file name, built from the same [AttemptDirectory] helper that names the staging it is copied
     * out of — so the two cannot drift, and so the platform is part of the identity here as well (the same
     * slug on Modrinth and CurseForge is two projects, and two crashes).
     */
    private fun fileName(platform: String, slug: String, loader: String) =
        AttemptDirectory.nameFor(platform, slug, loader) + SUFFIX

    /** Size ceiling and the marker a truncated log carries. */
    companion object {
        /** Largest console kept in full; beyond this the tail is kept, because that is where the crash is. */
        const val MAX_BYTES = 2 * 1024 * 1024

        /** Extension every kept log carries, which is also what [list] filters on. */
        const val SUFFIX = ".log"

        /** Prefixed to a shortened log, so nobody reads a tail as if it were the whole boot. */
        private const val TRUNCATION_NOTICE =
            "[… earlier output truncated: this console exceeded ${MAX_BYTES / (1024 * 1024)} MiB, " +
                "and the crash is at the end …]\n"
    }
}
