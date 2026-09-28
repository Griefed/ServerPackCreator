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
package de.griefed.serverpackcreator.grinder

import de.griefed.serverpackcreator.clientside.BootDecision
import de.griefed.serverpackcreator.clientside.BootLogClassifier
import de.griefed.serverpackcreator.clientside.BootResult
import de.griefed.serverpackcreator.clientside.ConsoleRuleFile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Holds the **shipped** `deploy/boot-rules.example.json` to the real consoles it was written from.
 *
 * A rule is decisive evidence — `BootDecision.OPERATOR_RULE` is one of only two decisions allowed to publish
 * a clientside entry — so an example file that ships wrong or over-broad rules is worse than one that ships
 * none. These excerpts are verbatim from `grinder.serverpackcreator.de`, sampled 2026-08-31, and they pin
 * both directions: what the rules must catch, and what they must leave alone.
 */
internal class ShippedBootRulesTest {

    private val rules = ConsoleRuleFile(File("deploy/boot-rules.example.json")).current()

    @Test
    fun theShippedExampleLoadsWithoutErrors() {
        Assertions.assertTrue(rules.rules.isNotEmpty(), "the example ships rules or it is not an example")
        Assertions.assertTrue(rules.errors.isEmpty(), "a shipped example must not carry errors: ${rules.errors}")
    }

    /**
     * `sodium-extra`, `reeses-sodium-options` and `better-block-entities` all died this way and were all
     * published HIGH with nothing decisive behind them. LWJGL is the client's windowing and OpenGL binding;
     * a dedicated server never ships it, so reaching it is proof the mod cannot run server-side.
     */
    @Test
    fun theLwjglRuleCatchesAClientLibraryOnADedicatedServer() {
        val console = listOf(
            "java.lang.NoClassDefFoundError: org/lwjgl/Version",
            "\tat FML Early Services//net.caffeinemc.mods.sodium.client.compatibility.checks.PreLaunchChecks" +
                ".isUsingKnownCompatibleLwjglVersion(PreLaunchChecks.java:136)",
            "Caused by: java.lang.ClassNotFoundException: org.lwjgl.Version"
        )

        val decided = BootLogClassifier.classify(console, exitCode = 1, timedOut = false, rules)

        Assertions.assertEquals(BootResult.CRASHED, decided.result)
        Assertions.assertEquals(BootDecision.OPERATOR_RULE, decided.decidedBy)
        Assertions.assertEquals("lwjgl-on-a-dedicated-server", decided.firedRule?.rule?.id)
        Assertions.assertTrue(decided.decidedBy.decisive, "a verified signature is what makes a rule publishable")
    }

    /**
     * **The guard that keeps the rule file honest.** `better-ping-display`, `immersive-ui` and
     * `certain-questing-additions` are all published HIGH today and all die on a missing **log4j-core** — a
     * logging library the server is supposed to have, so its absence is a defect in the pack we generated,
     * not evidence about the mod. Recovering three entries by writing a rule for it would launder a harness
     * bug into a published clientside entry, which is precisely what the decisive-evidence gate exists to
     * prevent.
     */
    @Test
    fun noShippedRuleClaimsAMissingLoggingLibrary() {
        val console = listOf(
            "java.lang.NoClassDefFoundError: org/apache/logging/log4j/core/config/ConfigurationSource",
            "Caused by: java.lang.ClassNotFoundException: org.apache.logging.log4j.core.config.ConfigurationSource"
        )

        val decided = BootLogClassifier.classify(console, exitCode = 1, timedOut = false, rules)

        Assertions.assertNotEquals(
            BootDecision.OPERATOR_RULE, decided.decidedBy,
            "a missing logging library is our packaging problem, not the mod's sideness"
        )
        Assertions.assertFalse(decided.decidedBy.decisive)
    }

    /** The FML rule closes the verified gap: that string is in no built-in guard, and exits 0 on NeoForge. */
    @Test
    fun theFmlRuleCatchesAClientClassRefusedOnADedicatedServer() {
        val console = listOf(
            "java.lang.RuntimeException: Attempted to load class net/minecraft/client/gui/screens/Screen " +
                "for invalid dist DEDICATED_SERVER"
        )

        val decided = BootLogClassifier.classify(console, exitCode = 0, timedOut = false, rules)

        Assertions.assertEquals(BootResult.CRASHED, decided.result)
        Assertions.assertEquals("fml-invalid-dist", decided.firedRule?.rule?.id)
    }

    /**
     * No shipped rule may fire on a boot that reached its ready-line, whatever else the console said. A rule
     * sits below the ready-line rung, and a rule file that could overturn a demonstrated successful start
     * would be able to publish anything.
     */
    @Test
    fun noShippedRuleOverturnsASuccessfulStart() {
        val console = listOf(
            "java.lang.NoClassDefFoundError: org/lwjgl/Version",
            "[Server thread/INFO]: Done (21.5s)! For help, type \"help\""
        )

        Assertions.assertEquals(
            BootResult.SURVIVED,
            BootLogClassifier.classify(console, exitCode = 0, timedOut = false, rules).result
        )
    }

    /** Nor may one turn host trouble into evidence — the timeout and killed guards outrank every rule. */
    @Test
    fun noShippedRuleOverturnsHostTrouble() {
        val console = listOf("java.lang.NoClassDefFoundError: org/lwjgl/Version")

        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(console, exitCode = 137, timedOut = false, rules).result,
            "a SIGKILLed boot is host trouble even when the console carries a client-library signature"
        )
    }
}
