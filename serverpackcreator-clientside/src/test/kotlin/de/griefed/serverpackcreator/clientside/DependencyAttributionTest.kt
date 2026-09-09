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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins blaming a crash on an **injected dependency** rather than on the candidate.
 *
 * Injecting dependencies makes a boot more faithful to a real pack, but it also puts other people's code
 * in the pack — so a crash may belong to a dependency. The decision taken here (2026-08-29) is that
 * attribution **annotates and requeues, never changes the verdict**:
 *
 * The candidate did crash a server in the configuration a real pack produces. Downgrading that on a string
 * heuristic trades a false positive for a *lost true positive*, which is the more expensive direction for a
 * list that decides what SPC strips from every pack built against it. Requeuing the dependency as its own
 * candidate answers the question with *data* — grind it, see whether it crashes alone — instead of with a
 * guess, and `attributionNeverChangesTheBootResult` is what makes that safe by construction.
 */
internal class DependencyAttributionTest {

    private fun dependency(fileName: String, modId: String? = null) =
        InjectedDependency(fileName, modId ?: fileName.substringBefore("-"))

    /** A stack frame naming a dependency's own package is the case this exists for. */
    @Test
    fun aStackFrameNamingAnInjectedDependencyIsAttributedToIt() {
        val console = listOf(
            "java.lang.NoClassDefFoundError: com/benbenlaw/core/screen/util/slot/FilterSlot",
            "\tat com.benbenlaw.core.CoreMod.init(CoreMod.java:42)"
        )

        val blamed = DependencyAttribution.blame(
            console, listOf(dependency("benbenlaw-core-1.20.1.jar", "benbenlaw")), candidateStem = "strawberrymod"
        )

        Assertions.assertEquals("benbenlaw-core-1.20.1.jar", blamed?.fileName)
    }

    /**
     * **The false-positive guard, and the one that matters most.** A dependency's name appears in every
     * "loading mod" line of a normal boot. Blaming on a bare mention would attribute nearly every crash to
     * whichever dependency happened to be listed, which is worse than not attributing at all.
     */
    @Test
    fun anInformationalMentionOfADependencyIsNotAttributed() {
        val console = listOf(
            "[main/INFO]: Found 3 mods: strawberrymod, benbenlaw-core, fabric-api",
            "[main/INFO]: Loading benbenlaw-core-1.20.1.jar",
            "java.lang.NoClassDefFoundError: net/minecraft/client/gui/screens/Screen"
        )

        Assertions.assertNull(
            DependencyAttribution.blame(
                console, listOf(dependency("benbenlaw-core-1.20.1.jar", "benbenlaw")), candidateStem = "strawberrymod"
            ),
            "a mention outside a crash frame is not evidence"
        )
    }

    /** When the candidate's own name is in the same frame, the dependency is not the obvious culprit. */
    @Test
    fun aCrashNamingTheCandidateItselfIsNotAttributedToADependency() {
        val console = listOf(
            "java.lang.NoClassDefFoundError: com/benbenlaw/core/Thing",
            "\tat com.strawberrymod.Main.init(Main.java:10)"
        )

        Assertions.assertNull(
            DependencyAttribution.blame(
                console, listOf(dependency("benbenlaw-core-1.20.1.jar", "benbenlaw")), candidateStem = "strawberrymod"
            )
        )
    }

    /** Nothing injected means nothing to blame, which is every boot before this feature existed. */
    @Test
    fun noInjectedDependenciesMeansNoAttribution() {
        Assertions.assertNull(
            DependencyAttribution.blame(
                listOf("java.lang.NoClassDefFoundError: com/whatever/Thing"), emptyList(), candidateStem = "jei"
            )
        )
    }

    /**
     * **The load-bearing guard.** Attribution may annotate and requeue; it may never move the verdict. If
     * this ever fails, the feature has silently become a downgrade mechanism and starts eating true
     * positives — the expensive direction.
     */
    @Test
    fun attributionNeverChangesTheBootResult() {
        val console = listOf(
            "java.lang.NoClassDefFoundError: com/benbenlaw/core/screen/util/slot/FilterSlot",
            "\tat com.benbenlaw.core.CoreMod.init(CoreMod.java:42)"
        )
        val run = RunResult.Completed(console, exitCode = 1, timedOut = false)
        val injected = listOf(dependency("benbenlaw-core-1.20.1.jar", "benbenlaw"))

        val withoutDependencies = BootVerifier.attribute(
            BootVerifier.BootOutcome(BootResult.CRASHED, null, "crashed", console = console.joinToString("\n")),
            emptyList(),
            candidateStem = "strawberrymod"
        )
        val withDependencies = BootVerifier.attribute(
            BootVerifier.BootOutcome(BootResult.CRASHED, null, "crashed", console = console.joinToString("\n")),
            injected,
            candidateStem = "strawberrymod"
        )

        Assertions.assertEquals(BootResult.CRASHED, withoutDependencies.result)
        Assertions.assertEquals(
            withoutDependencies.result, withDependencies.result,
            "attribution must annotate, never move the verdict"
        )
        Assertions.assertNull(withoutDependencies.blamedDependency)
        Assertions.assertEquals("benbenlaw-core-1.20.1.jar", withDependencies.blamedDependency)
        Assertions.assertTrue(
            withDependencies.detail.contains("benbenlaw-core"),
            "and it has to say so: ${withDependencies.detail}"
        )
        Assertions.assertTrue(run.lines.isNotEmpty())
    }

    /** A boot that did not crash has nothing to attribute, whatever is in its console. */
    @Test
    fun onlyACrashIsAttributed() {
        val outcome = BootVerifier.attribute(
            BootVerifier.BootOutcome(
                BootResult.SURVIVED, null, "survived",
                console = "java.lang.NoClassDefFoundError: com/benbenlaw/core/Thing\n\tat com.benbenlaw.core.X.y(X.java:1)"
            ),
            listOf(dependency("benbenlaw-core-1.20.1.jar", "benbenlaw")),
            candidateStem = "strawberrymod"
        )

        Assertions.assertNull(outcome.blamedDependency)
    }
}
