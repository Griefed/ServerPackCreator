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

/**
 * The modloader build a console says actually started — as opposed to the one staging asked for.
 *
 * **The two really do differ, systematically.** Measured across the public grinder's kept consoles on
 * 2026-09-11: every one of **16 of 16** Quilt boots printed `Quilt Loader 0.30.1` while its verdict
 * reported `Quilt 0.31.0-beta.4`, the build `preferredVersion` had chosen. That is not cosmetic — the two
 * builds differ in what they *provide*: quilt-loader `0.30.1` declares `fabricloader 0.19.3` and
 * `0.31.0-beta.4` declares `0.19.5`, and `fabric-language-kotlin` demands `[0.19.5, ∞)`. So twelve published
 * rows failed to load a library the build we believed we were running would have satisfied, and every one of
 * them named the wrong build while doing it.
 *
 * A verdict that cannot name its own evidence cannot be audited, and "which loader build ran?" is the first
 * thing a reader of such a row asks.
 *
 * **Best-effort by design.** Each loader announces itself in its own words and only once it has got far
 * enough to speak; a boot that dies before then yields `null`, which every caller reads as *"we did not
 * observe one"* and falls back to the requested build. Nothing is inferred from a jar name on the classpath:
 * a pack carries several, and picking one would be a guess dressed as an observation.
 *
 * @author Griefed
 */
object BootLoaderVersion {

    /**
     * The phrasings each loader announces its own build with, most reliable first.
     *
     * Fabric and Quilt print theirs before loading anything, which is why the two loaders whose rows this
     * exists for are also the two it can always answer. Forge and NeoForge announce only once mod loading
     * starts, so a pack they refuse outright leaves nothing to read — deliberately not worked around by
     * reading the classpath instead.
     */
    private val announcements = listOf(
        // "Loading Minecraft 26.2 with Quilt Loader 0.30.1" / "… with Fabric Loader 0.19.5"
        Regex("""with (?:Quilt|Fabric) Loader (\S+)"""),
        // The crash report's own header, which survives when the line above scrolled past a cap.
        Regex("""(?:Quilt|Fabric) Loader Version: (\S+)"""),
        // "Forge mod loading, version 47.4.23, for MC 1.20.1 with MCP …"
        Regex("""(?:Forge|NeoForge) mod loading, version ([^,\s]+)"""),
        // "MinecraftForge v47.4.23 Initialized"
        Regex("""(?:MinecraftForge|NeoForge) v(\S+) Initialized""")
    )

    /**
     * The build [consoleLines] says started, or `null` when none of them announced one.
     *
     * The first *phrasing* that matches anywhere wins, rather than the first matching line: the loaders
     * announce in a fixed order relative to their own output, and a crash report quoting an earlier line
     * would otherwise outrank the live one.
     */
    fun observedIn(consoleLines: List<String>): String? = announcements.firstNotNullOfOrNull { pattern ->
        consoleLines.firstNotNullOfOrNull { line ->
            pattern.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
        }
    }

    /**
     * A note for the verdict's detail when [observed] contradicts [requested], or `null` when they agree,
     * when nothing was observed, or when nothing was requested.
     *
     * Appended rather than rewriting the detail: what staging asked for is a true fact about the attempt and
     * is what the install cache is keyed by, so a reader chasing the tuple needs both halves. Naming only
     * one of them is how twelve rows came to describe a build they never ran.
     */
    fun disagreementNote(requested: String?, observed: String?): String? {
        if (requested.isNullOrBlank() || observed.isNullOrBlank() || requested == observed) {
            return null
        }
        return "(the pack was staged for $requested but booted $observed)"
    }
}
