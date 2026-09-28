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
 * Pins **where** an operator's rule sits in the classifier ladder, which is the whole design decision.
 *
 * Above the exit code and above the built-in client-class marker, so a rule can both raise a crash the exit
 * status would have excused and excuse a console the marker would have crashed. Below the timeout, the
 * killed/OOM guard and the environment guards, so a hand-edited file can never manufacture a `HIGH` out of
 * host trouble — the most expensive failure this engine has on record was a memory-starved VM turning fat
 * mods into HIGH-confidence clientside crashes, systematically.
 */
internal class ConsoleRuleLadderTest {

    private fun rules(vararg rules: ConsoleRule) = ConsoleRuleSet(rules.toList(), emptyList(), "test")

    private fun rule(id: String, pattern: String, verdict: BootResult = BootResult.INCONCLUSIVE, note: String? = null) =
        ConsoleRule(id, pattern, verdict, note)

    /**
     * The verified gap this feature exists to close. FML refuses a client-only class with
     * `Attempted to load class … for invalid dist DEDICATED_SERVER`, and NeoForge's ServerStarterJar can
     * print that and still exit **0** — so the ladder's exit-code fallback scored it INCONCLUSIVE. The
     * string appears nowhere in the codebase; only a rule can catch it today.
     */
    @Test
    fun aRuleCrashesAConsoleThatAZeroExitWouldHaveExcused() {
        // Deliberately a synthetic signature. This test used FML's "for invalid dist DEDICATED_SERVER"
        // until 2026-09-04, when that became a *built-in* rule — which closed the very gap the test was
        // demonstrating and broke it. The mechanism is what is being pinned, not any one string, so the
        // example is one that can never be promoted into the defaults and take this guard with it.
        val console = listOf(
            "[main/FATAL]: ExampleMod: this build is client-only and refuses to start on a server"
        )

        val plain = BootLogClassifier.classify(console, exitCode = 0, timedOut = false)
        val ruled = BootLogClassifier.classify(
            console, exitCode = 0, timedOut = false,
            rules = rules(rule("client-only-build", "this build is client-only and refuses to start", BootResult.CRASHED))
        )

        Assertions.assertEquals(BootResult.INCONCLUSIVE, plain, "without a rule this is the gap being closed")
        Assertions.assertEquals(BootResult.CRASHED, ruled.result)
        Assertions.assertEquals("client-only-build", ruled.firedRule?.rule?.id)
    }

    /**
     * The guard that keeps the feature safe. A rule must not be able to turn host trouble into evidence
     * about a mod — same principle already pinned for the built-in console marker.
     */
    @Test
    fun aRuleVerdictDoesNotBeatTheTimeoutOrTheKilledGuards() {
        val console = listOf("java.lang.RuntimeException: for invalid dist DEDICATED_SERVER")
        val ruleSet = rules(rule("fml-invalid-dist", "for invalid dist DEDICATED_SERVER", BootResult.CRASHED))

        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(console, exitCode = null, timedOut = true, rules = ruleSet).result,
            "a timed-out boot never gave the mod a fair run"
        )
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(console, exitCode = 137, timedOut = false, rules = ruleSet).result,
            "a SIGKILLed boot is host trouble, not a mod's fault"
        )
    }

    /** Above the built-in marker, so a known-broken loader build printing a client class can be excused. */
    @Test
    fun aRuleCanExcuseAConsoleTheClientClassMarkerWouldHaveCrashed() {
        val console = listOf("java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft")

        Assertions.assertEquals(
            BootResult.CRASHED,
            BootLogClassifier.classify(console, exitCode = 1, timedOut = false),
            "the built-in marker still crashes this on its own"
        )
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(
                console, exitCode = 1, timedOut = false,
                rules = rules(rule("known-broken-build", "NoClassDefFoundError: net/minecraft/client", BootResult.INCONCLUSIVE))
            ).result,
            "an operator must be able to excuse a signature they know to be the loader's fault"
        )
    }

    /** The file's order is the precedence. Anything else would make a rule set unreadable to its author. */
    @Test
    fun theFirstMatchingRuleInFileOrderWins() {
        val console = listOf("something went sideways")

        val decided = BootLogClassifier.classify(
            console, exitCode = 1, timedOut = false,
            rules = rules(
                rule("first", "went sideways", BootResult.INCONCLUSIVE),
                rule("second", "something", BootResult.CRASHED)
            )
        )

        Assertions.assertEquals(BootResult.INCONCLUSIVE, decided.result)
        Assertions.assertEquals("first", decided.firedRule?.rule?.id)
    }

    /**
     * **By default a verdict-less rule defers to the ladder and merely names itself** — "if none is
     * specified, determine by grinder". The operator wrote a pattern to *label* a signature, and the ladder
     * is still the better judge of what it means; the rule riding along is how they can see it matched.
     */
    @Test
    fun aRuleWithNoVerdictLeavesTheLadderToDecideAndStillNamesItself() {
        val console = listOf("java.lang.NoClassDefFoundError: com/benbenlaw/core/screen/util/slot/FilterSlot")
        val undecided = ConsoleRule("third-party-screen-class", "NoClassDefFoundError: .*/screen/", note = "a screen class from another mod")

        val plain = BootLogClassifier.classify(console, exitCode = 1, timedOut = false)
        val ruled = BootLogClassifier.classify(console, exitCode = 1, timedOut = false, rules = rules(undecided))

        Assertions.assertEquals(BootResult.CRASHED, plain, "the ladder alone crashes this on its non-zero exit")
        Assertions.assertEquals(plain, ruled.result, "an undecided rule must not move the verdict by default")
        Assertions.assertEquals("third-party-screen-class", ruled.firedRule?.rule?.id)
        Assertions.assertEquals("a screen class from another mod", ruled.firedRule?.rule?.note)
    }

    /**
     * **Opting in makes an undecided rule conservative instead.** INCONCLUSIVE is the one outcome that can
     * never publish, so a run where unfinished rules are expected can make them cost coverage rather than
     * risk a false positive — without touching the rules themselves.
     */
    @Test
    fun theOptInMakesAnUndecidedRuleInconclusive() {
        val console = listOf("java.lang.NoClassDefFoundError: com/benbenlaw/core/screen/util/slot/FilterSlot")
        val undecided = ConsoleRule("third-party-screen-class", "NoClassDefFoundError: .*/screen/")
        val conservative = ConsoleRuleSet(listOf(undecided), emptyList(), "test", BootResult.INCONCLUSIVE)

        val ruled = BootLogClassifier.classify(console, exitCode = 1, timedOut = false, rules = conservative)

        Assertions.assertEquals(BootResult.INCONCLUSIVE, ruled.result)
        Assertions.assertEquals("third-party-screen-class", ruled.firedRule?.rule?.id)
    }

    /**
     * The opt-in governs only *undecided* rules. A rule that states a verdict still means what it says —
     * otherwise turning the setting on would silently rewrite an operator's deliberate CRASHED rules.
     */
    @Test
    fun theOptInDoesNotOverrideARuleThatStatesAVerdict() {
        val console = listOf("for invalid dist DEDICATED_SERVER")
        val stated = ConsoleRule("fml-invalid-dist", "for invalid dist DEDICATED_SERVER", BootResult.CRASHED)

        Assertions.assertEquals(
            BootResult.CRASHED,
            BootLogClassifier.classify(
                console, exitCode = 0, timedOut = false,
                rules = ConsoleRuleSet(listOf(stated), emptyList(), "test", BootResult.INCONCLUSIVE)
            ).result
        )
    }

    /** A ready-line is decisive above everything, rules included: the server demonstrably started. */
    @Test
    fun noRuleOutranksAReadyLine() {
        val console = listOf("for invalid dist DEDICATED_SERVER", "[Server thread/INFO]: Done (21.5s)! For help")

        Assertions.assertEquals(
            BootResult.SURVIVED,
            BootLogClassifier.classify(
                console, exitCode = 1, timedOut = false,
                rules = rules(rule("fml-invalid-dist", "for invalid dist DEDICATED_SERVER", BootResult.CRASHED))
            ).result
        )
    }

    /** An empty rule set has to leave every existing verdict exactly where it was. */
    @Test
    fun anEmptyRuleSetChangesNothing() {
        val cases = listOf(
            Triple(listOf("Done (1.0s)! For help"), 0, BootResult.SURVIVED),
            Triple(listOf("java.lang.NoClassDefFoundError: net/minecraft/client/Foo"), 0, BootResult.CRASHED),
            Triple(listOf("nothing interesting"), 1, BootResult.CRASHED),
            Triple(listOf("nothing interesting"), 0, BootResult.INCONCLUSIVE)
        )

        for ((console, exit, expected) in cases) {
            Assertions.assertEquals(
                expected,
                BootLogClassifier.classify(console, exit, timedOut = false, rules = ConsoleRuleSet.EMPTY).result,
                "an empty rule set changed the verdict for $console"
            )
            Assertions.assertEquals(
                expected, BootLogClassifier.classify(console, exit, timedOut = false),
                "the three-argument overload must agree with it"
            )
        }
    }

    /** A rule that throws while matching must cost its own match and nothing else. */
    @Test
    fun aRuleThatCannotBeCompiledNeverFires() {
        val decided = BootLogClassifier.classify(
            listOf("anything at all"), exitCode = 0, timedOut = false,
            rules = ConsoleRuleSet(listOf(ConsoleRule("broken", "[unclosed", BootResult.CRASHED)), emptyList(), "test")
        )

        Assertions.assertEquals(BootResult.INCONCLUSIVE, decided.result)
        Assertions.assertNull(decided.firedRule)
    }
}
