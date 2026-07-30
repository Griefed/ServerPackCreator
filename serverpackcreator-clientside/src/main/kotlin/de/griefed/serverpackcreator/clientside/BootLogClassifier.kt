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

import de.griefed.serverpackcreator.clientside.BootResult.CRASHED
import de.griefed.serverpackcreator.clientside.BootResult.SURVIVED


/**
 * Outcome of booting a server with the candidate mod force-included. Note the asymmetry: only
 * [CRASHED] is a strong positive for "clientside" — a graceful clientside mod boots fine
 * ([SURVIVED]), so SURVIVED does not prove server-safety.
 *
 * @author Griefed
 */
enum class BootResult {
    /** The server reached its ready-line — the mod did not prevent startup. */
    SURVIVED,

    /** The server exited non-zero before becoming ready — the mod likely broke it. */
    CRASHED,

    /** Neither ready nor a clear crash within the time-budget (timeout / clean early exit). */
    INCONCLUSIVE
}

/**
 * Classifies a finished server-boot from its console-output and exit-state, with no I/O of its own so
 * it is fully unit-testable. The Minecraft server's `Done (…)! For help` line is the canonical
 * ready-signal; absent that, a non-zero exit means a crash and a timeout/clean-early-exit is
 * inconclusive.
 *
 * @author Griefed
 */
object BootLogClassifier {

    /** The vanilla server's ready-line, e.g. `[12:00:00] [Server thread/INFO]: Done (21.5s)! For help, ...`. */
    private val readyLine = Regex("""Done \([^)]*\)! For help""")

    /**
     * Signatures of a **pre-launch setup abort** — `start.sh`'s `crashServer` messages for
     * environment/loader/install failures that stop the server *before the mod is ever loaded*: the
     * loader not supporting the Minecraft version, a loader/launcher-jar download failure, the
     * ServerStarterJar install failing, a Java setup failure, a missing `variables.txt`, an
     * unrecognized modloader, or a declined EULA. None of these are the mod's fault, so they are
     * [BootResult.INCONCLUSIVE], not [BootResult.CRASHED] — scoring them as a crash would be a false
     * clientside HIGH. Kept specific so a genuine mod-load crash (a stacktrace, a mixin error) does
     * **not** match.
     */
    private val setupAbortMarkers = Regex(
        "(is not available for Minecraft" +
            "|servers are having trouble" +
            "|Something went wrong during the server installation" +
            "|Java install-script failed" +
            "|Java installation failed" +
            "|wget or curl is required" +
            "|variables\\.txt not present" +
            "|Incorrect modloader specified" +
            "|did not agree to Mojang's EULA)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Classify a boot from its [consoleLines], the process [exitCode] (`null` if it was killed/never
     * exited) and whether the time-budget was exceeded ([timedOut]).
     *
     * The ready-line wins outright — when present the boot [BootResult.SURVIVED] even though the
     * process is subsequently killed (yielding a non-zero exit). Otherwise a timeout is
     * [BootResult.INCONCLUSIVE]; a pre-launch [setupAbortMarkers] hit is [BootResult.INCONCLUSIVE]
     * (the mod was never tested — the loader/env/install failed first); a clean `0` exit without ever
     * reaching ready is [BootResult.INCONCLUSIVE]; and any other non-zero exit is [BootResult.CRASHED].
     */
    fun classify(consoleLines: List<String>, exitCode: Int?, timedOut: Boolean): BootResult {
        if (consoleLines.any { readyLine.containsMatchIn(it) }) {
            return BootResult.SURVIVED
        }
        if (timedOut) {
            return BootResult.INCONCLUSIVE
        }
        if (consoleLines.any { setupAbortMarkers.containsMatchIn(it) }) {
            return BootResult.INCONCLUSIVE
        }
        return when (exitCode) {
            null, 0 -> BootResult.INCONCLUSIVE
            else -> BootResult.CRASHED
        }
    }
}

/**
 * Pulls the relevant slice of a crashed server's console-output for a maintainer to read in the
 * issue-comment, without making them open the full log-artifact. A crash raises confidence that the
 * mod is clientside-only but is no proof — the excerpt lets a human judge *why* it crashed.
 *
 * @author Griefed
 */
object BootLogExcerpt {

    /** Lines that mark the onset of a crash; the excerpt starts at the first match. */
    private val crashMarkers = Regex(
        "(Exception|Error|Caused by:|Failed to|crash report|FATAL|A problem occurred|NoClassDefFound|NoSuchMethod|could not be loaded)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Extract a crash-excerpt from [consoleLines]: from the first crash-marker to the end (capped at
     * [maxLines] and [maxChars]), falling back to the tail when no marker is found. Returns `null`
     * for empty input.
     */
    fun crashExcerpt(consoleLines: List<String>, maxLines: Int = 60, maxChars: Int = 4_000): String? {
        if (consoleLines.isEmpty()) {
            return null
        }
        val firstMarker = consoleLines.indexOfFirst { crashMarkers.containsMatchIn(it) }
        val from = if (firstMarker >= 0) firstMarker else (consoleLines.size - maxLines).coerceAtLeast(0)
        val slice = consoleLines.subList(from, consoleLines.size).take(maxLines)
        return slice.joinToString("\n").take(maxChars).ifBlank { null }
    }
}
