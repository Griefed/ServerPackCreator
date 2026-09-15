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
 * Reads a console for one specific complaint: **the loader build itself is older than a mod demands**.
 *
 * That is a statement about the harness, not about the mod. The grinder deliberately boots a *cached* loader
 * build rather than installing every release (`CachedLoaderVersions`, ~150 MB per tuple), so this is the
 * failure that choice produces — and until it is re-checked on the newest build, the candidate wears an
 * INCONCLUSIVE for a decision the harness made. Measured 2026-09-08: 17 of 42 dependency failures on the
 * live daemon, all 511 Fabric boots pinned to loader 0.19.3 while 0.19.5 was current.
 *
 * **Not a `BootRule`, deliberately.** The rules in `boot-rules.default.json` map a console onto a *verdict*;
 * this maps a console onto *"try again differently"*, which is a different question and must not be
 * reachable by an operator's rule file — a rule that could trigger re-boots would let a typo cost containers
 * rather than accuracy.
 *
 * **The three loaders word it three ways** (all verbatim from live logs, see `LoaderTooOldRecheckTest`), so
 * the shared fact is matched instead of the phrasing: a version demand in the same line as one of the ids
 * the *runtime* provides. `BootVerifier.environmentProvidedIds` holds the same set for staging, but it is
 * not reused here — that set answers "never download this", and coupling the two would make either one
 * unsafe to extend on its own.
 *
 * @author Griefed
 */
object LoaderVersionDemand {

    /**
     * The ids a loader answers to in its own error messages. `quilted_fabric_loader` is Quilt's shim for the
     * Fabric id and appears beside it in the same complaint.
     */
    private val loaderIds = setOf("fabricloader", "quilt_loader", "quilted_fabric_loader", "forge", "neoforge")

    /**
     * Phrasings that introduce a version demand. Fabric writes "requires version X or later of mod …",
     * Quilt "requires version [X, ∞) of …", FML "Expected range: '[X,)'".
     */
    private val demandPhrases = listOf("requires version", "expected range")

    /**
     * Whether [consoleLines] say the boot failed because the loader build was too old for a mod in the pack.
     *
     * Both halves must appear **on one line**: a demand phrase, and a loader id as a whole word. Requiring
     * them together is what keeps `Mod ID: 'ponder', … Expected range: '[1.0.82,)'` — a demand against
     * another mod, which no newer loader can satisfy — from triggering a second container.
     *
     * A false positive costs one extra boot; a false negative costs a wrong INCONCLUSIVE on a mod that never
     * ran. The asymmetry is why the match is generous within the line rather than pinned to one phrasing.
     */
    fun unmetIn(consoleLines: List<String>): Boolean = consoleLines.any { line ->
        val lowered = line.lowercase()
        demandPhrases.any { phrase -> lowered.contains(phrase) } && loaderIds.any { id -> mentions(lowered, id) }
    }

    /**
     * Whether [lowered] names [id] as a whole word, so `forge` does not match inside `forgeconfigapiport`
     * and `neoforge` does not match a mod that merely has it in its name.
     */
    private fun mentions(lowered: String, id: String): Boolean =
        Regex("(?<![a-z0-9_-])" + Regex.escape(id) + "(?![a-z0-9_-])").containsMatchIn(lowered)
}
