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
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * Pins that moving the built-in ladder into an editable rules file **changes no verdict**.
 *
 * Stage 2 of the result-system redesign takes the eleven hardcoded marker groups out of
 * [BootLogClassifier] and ships them as ordinary rules, so an operator can read, edit or disable any of
 * them — "no hardcoded rules". That is only safe if the extraction is behaviour-preserving, and green tests
 * written by the same pass that moved the code do not prove it. So every sample below is classified **twice**,
 * once by the surviving hardcoded ladder and once by the bundled rules, and the two must agree.
 *
 * The ladder is not a flat match-list and the file order has to reproduce it exactly:
 *
 *  1. the "never got a fair run" guards — setup abort, launch failure, loader bootstrap failure, OOM
 *  2. the decisive client-only evidence
 *  3. the excuses — dependency failure, sandbox network, mixin apply, loader solver, runtime mismatch
 *
 * Getting 2 and 3 the wrong way round is the inversion this project has warned about since 2026-08-29: *an
 * excuse may never outrank decisive client-only evidence*, because a clientside mod may perfectly well phone
 * home **and** die on a client class, and the marker must still win. Getting 1 wrong is worse still — it lets
 * host trouble be published as a mod's fault, which is the missing-runtime-image and poisoned-loader-cache
 * failure both.
 *
 * The ready-line, the timeout and the exit codes are deliberately **not** rules. They are structural readings
 * of how the process ended rather than of what it said, which is what `BootObservation` models; a rules file
 * is for the console.
 */
internal class DefaultBootRulesTest {

    /** One console line, and the rung of the ladder it must land on. */
    private data class Sample(val line: String, val expectedRuleId: String)

    /**
     * One line per extracted marker group, taken from the evidence the group's own documentation cites, so a
     * pattern that stops matching its founding case fails here.
     */
    private val samples = listOf(
        Sample("Something went wrong during the server installation", "setup-abort"),
        Sample("Error: Unable to access jarfile forge.jar", "launch-failure"),
        Sample("Could not find parent layer for module", "loader-bootstrap-failure"),
        Sample("java.lang.OutOfMemoryError: Java heap space", "out-of-memory"),
        Sample("NoClassDefFoundError: net/minecraft/client/Minecraft", "client-only-class"),
        Sample("Missing or unsupported mandatory dependencies", "dependency-failure"),
        Sample("java.net.UnknownHostException: api.polyfrost.org", "sandbox-network"),
        Sample("InvalidInjectionException: Critical injection failure", "mixin-apply-failure"),
        Sample("Unhandled solver error involving the following rules:", "loader-solver-failure"),
        Sample("NoClassDefFoundError: org/apache/logging/log4j/LogManager", "runtime-mismatch")
    )

    /**
     * Each sample must be decided by the rule its group became, proving the pattern survived the move and
     * that nothing earlier in file order swallows it.
     */
    @TestFactory
    fun everyExtractedMarkerGroupIsDecidedByItsOwnRule(): List<DynamicTest> =
        samples.map { sample ->
            DynamicTest.dynamicTest(sample.expectedRuleId) {
                val fired = DefaultBootRules.bundled().firstMatch(listOf(sample.line))
                Assertions.assertNotNull(fired, "no rule matched: ${sample.line}")
                Assertions.assertEquals(
                    sample.expectedRuleId, fired?.rule?.id,
                    "wrong rung for '${sample.line}' — file order does not reproduce the ladder"
                )
            }
        }

    /**
     * **The inversion guard.** A console carrying both an excuse and the decisive marker must still confirm.
     * Ordering the file with excuses first would silently convert true positives into INCONCLUSIVE, and no
     * single-line sample above would notice.
     */
    @Test
    fun decisiveClientEvidenceOutranksEveryExcuse() {
        val excuses = listOf(
            "Missing or unsupported mandatory dependencies",
            "java.net.UnknownHostException: api.polyfrost.org",
            "InvalidInjectionException: Critical injection failure",
            "Unhandled solver error involving the following rules:",
            "NoClassDefFoundError: org/apache/logging/log4j/LogManager"
        )

        excuses.forEach { excuse ->
            val console = listOf(excuse, "NoClassDefFoundError: net/minecraft/client/Minecraft")
            Assertions.assertEquals(
                "client-only-class",
                DefaultBootRules.bundled().firstMatch(console)?.rule?.id,
                "an excuse outranked decisive client evidence: $excuse"
            )
        }
    }

    /**
     * **The fair-run guard.** A console carrying both a "never got a fair run" signal and the decisive marker
     * must NOT confirm: if the loader never bootstrapped, the client-class line came from something other
     * than this mod being exercised. These sit above the evidence for exactly that reason.
     */
    @Test
    fun aRunThatNeverHappenedOutranksTheDecisiveMarker() {
        val console = listOf(
            "Could not find parent layer for module",
            "NoClassDefFoundError: net/minecraft/client/Minecraft"
        )

        Assertions.assertEquals(
            "loader-bootstrap-failure",
            DefaultBootRules.bundled().firstMatch(console)?.rule?.id,
            "a mod that never loaded cannot have been proven clientside"
        )
    }

    /**
     * Only the decisive rule confirms **from a console**; every other extracted group means the mod got no
     * fair run and is worth no verdict.
     *
     * Scoped to [RuleSource.CONSOLE] since stage 3, which added metadata rules that also confirm — from what
     * a mod *declares* rather than from what a boot *did*. That is a different question with its own guards
     * in `MetadataRuleTest`, and folding the two sets together here would let a metadata rule silently
     * satisfy a console assertion.
     */
    @Test
    fun onlyDecisiveClientEvidenceConfirmsFromAConsole() {
        val confirming = DefaultBootRules.bundled().rules
            .filter { it.source == RuleSource.CONSOLE && it.verdict == Verdict.CONFIRMED }

        Assertions.assertEquals(
            listOf("client-only-class", "lwjgl-on-a-dedicated-server", "fml-invalid-dist"),
            confirming.map { it.id },
            "only unfakeable client-only evidence may confirm — every other group means no fair run"
        )
    }

    /** A shipped rule set with an unparseable pattern or a duplicate id would silently mis-order the ladder. */
    @Test
    fun theBundledRulesAreWellFormed() {
        val ruleSet = DefaultBootRules.bundled()

        Assertions.assertTrue(ruleSet.errors.isEmpty(), "bundled rules must parse cleanly: ${ruleSet.errors}")
        Assertions.assertTrue(ruleSet.rules.isNotEmpty(), "the bundled set may not be empty")
        Assertions.assertEquals(
            ruleSet.rules.map { it.id }.distinct().size, ruleSet.rules.size,
            "duplicate rule ids make precedence ambiguous"
        )
    }

    /**
     * **A mixin subsystem exception is a mixin failure, whatever it was reaching for.**
     * `ClassMetadataNotFoundException` sat in `dependency-failure`, and on the public grinder 2026-09-11 it
     * caught two rows that are nothing of the kind: `CurseForge/ars-nouveau` reaching
     * `net.minecraft.core.BlockSourceImpl` (a class its Minecraft no longer has) and
     * `CurseForge/yungs-better-caves` reaching `com.llamalad7.mixinextras.injector.wrapoperation.Operation`.
     *
     * Both stay INCONCLUSIVE either way — the rungs are neighbours — so nothing published changes. What
     * changes is that the `Decision` column, which is how an operator filters, stops calling a mixin failure
     * a missing dependency.
     */
    @Test
    fun aMixinMetadataFailureIsDecidedByTheMixinRung() {
        val decided = BootLogClassifier.classify(
            listOf(
                "Caused by: org.spongepowered.asm.mixin.throwables.ClassMetadataNotFoundException: " +
                    "net.minecraft.core.BlockSourceImpl"
            ),
            exitCode = 1,
            timedOut = false,
            ConsoleRuleSet.EMPTY
        )

        Assertions.assertEquals(BootDecision.MIXIN_APPLY_FAILURE, decided.decidedBy)
        Assertions.assertEquals(BootResult.INCONCLUSIVE, decided.result)
    }

    /** The MixinTweaker miss stays where it is: a 1.12.2 coremod really is an absent dependency. */
    @Test
    fun theMissingMixinTweakerStaysADependencyFailure() {
        val decided = BootLogClassifier.classify(
            listOf("java.lang.ClassNotFoundException: org.spongepowered.asm.launch.MixinTweaker"),
            exitCode = 1,
            timedOut = false,
            ConsoleRuleSet.EMPTY
        )

        Assertions.assertEquals(BootDecision.DEPENDENCY_FAILURE, decided.decidedBy)
    }
}
