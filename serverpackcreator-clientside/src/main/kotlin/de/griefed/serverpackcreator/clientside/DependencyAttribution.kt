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

/** One dependency jar staged alongside a candidate: what it is called, and the mod id it declares. */
data class InjectedDependency(
    /** The published jar file name, which is what a reader needs to reproduce the boot exactly. */
    val fileName: String,
    /** The mod id it was resolved by, matched against crash text alongside the file name's stem. */
    val modId: String?
)

/**
 * Decides whether a crash belongs to an **injected dependency** rather than to the candidate.
 *
 * Injecting dependencies makes a boot faithful to a real pack, but it also puts other people's code in the
 * pack — so a crash may not be the candidate's. This **annotates only**: it never changes a
 * [BootResult]. The candidate did crash a server in the configuration a real pack produces, and
 * downgrading that on a string heuristic trades a false positive for a *lost true positive* — the more
 * expensive direction for a list that decides what gets stripped from every pack built against it. The
 * grinder requeues a blamed dependency as its own candidate instead, so the question is answered by
 * grinding it rather than by guessing.
 *
 * @author Griefed
 */
object DependencyAttribution {

    /**
     * Lines that carry an actual failure, as opposed to a mod merely being *mentioned*. Deliberately the
     * same vocabulary `BootLogExcerpt.crashMarkers` uses, plus the stack-frame shape.
     */
    private val crashContext = Regex(
        "(Exception|Error|Caused by:|\\bat [\\w.$]+\\(|NoClassDefFound|NoSuchMethod|could not be loaded)",
        RegexOption.IGNORE_CASE
    )

    /**
     * The injected dependency [consoleLines] blames, or `null`.
     *
     * **A bare mention is never enough**, and that guard is what makes this worth having: a dependency's
     * name appears in every "loading mod" line of a normal boot, so blaming on a mention would attribute
     * nearly every crash to whichever dependency happened to be listed. A line must carry a crash marker or
     * be a stack frame, and blame stands down entirely when [candidateStem] appears anywhere in that crash
     * context — an exception and the frames beneath it are one crash, and where the candidate is named in it
     * the candidate is at least as likely to be the culprit.
     */
    fun blame(
        consoleLines: List<String>,
        injected: List<InjectedDependency>,
        candidateStem: String?
    ): InjectedDependency? {
        if (injected.isEmpty()) {
            return null
        }
        val candidate = candidateStem?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val crashLines = consoleLines.filter { crashContext.containsMatchIn(it) }.map { it.lowercase() }
        // Stand down if the candidate appears ANYWHERE in the crash context, not merely on the same line.
        // An exception line and the `at` frames beneath it are one crash: `NoClassDefFoundError: a/b/Thing`
        // thrown from `at com.candidate.Main` names both, and the candidate is at least as likely to be the
        // culprit. Judging line by line would blame the dependency on the strength of the first line alone.
        if (candidate != null && crashLines.any { it.contains(candidate) }) {
            return null
        }
        for (line in crashLines) {
            injected.firstOrNull { dependency -> namesIt(line, dependency) }?.let { return it }
        }
        return null
    }

    /**
     * Whether [line] names [dependency], by its jar-name stem or its mod id.
     *
     * The stem is cut at the first digit group so `benbenlaw-core-1.20.1.jar` matches a package written
     * `com/benbenlaw/core/...`; a package path uses `/` where a file name uses `-`, so both separators are
     * normalised away before comparing.
     */
    private fun namesIt(line: String, dependency: InjectedDependency): Boolean {
        val normalised = line.replace("/", "").replace("-", "").replace("_", "").replace(".", "")
        val stem = dependency.fileName.substringBeforeLast(".")
            .takeWhile { !it.isDigit() }
            .trim('-', '_', '.')
            .lowercase()
        val candidates = listOfNotNull(stem.takeIf { it.length >= 4 }, dependency.modId?.lowercase()?.takeIf { it.length >= 4 })
        return candidates.any { normalised.contains(it.replace("-", "").replace("_", "").replace(".", "")) }
    }
}
