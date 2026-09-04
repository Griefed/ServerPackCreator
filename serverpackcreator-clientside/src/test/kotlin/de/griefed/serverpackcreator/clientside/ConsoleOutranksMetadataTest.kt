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
 * Pins the precedence between the two rule streams: **the console decides, the metadata only declares.**
 *
 * What a mod *says* about itself is a self-report and has always been the unreliable half — it is the entire
 * reason the expensive boot exists. So a declaration, whatever it says, may never stand in for a boot and may
 * never overturn one:
 *
 *  - a mod declaring client, both or server is still booted, and the console decides its verdict
 *  - a mod declaring **server** whose console then reaches a client-only class is CONFIRMED **client**
 *
 * That second case is not an edge case, it is the target. A mod that is honestly declared client-only is
 * already excludable from its own metadata and costs nothing to find; the ones worth a container are the
 * ones **coded unclean — claiming server-side while calling client classes**. The console rules are the
 * instrument for catching exactly that, which is why they are the ones worth crafting delicately and why a
 * declaration must never pre-empt them.
 *
 * Stage 3 originally let a metadata rule reach [Verdict.CONFIRMED] on its own. That was a short-circuit: it
 * would have published mods on their own say-so, without a boot, and would have made the metadata capable of
 * outranking the very evidence it is unreliable about.
 */
internal class ConsoleOutranksMetadataTest {

    private fun facts(serverSide: DeclaredSupport, jarScan: JarScan) =
        MetadataFacts.line(serverSide = serverSide, clientSide = DeclaredSupport.UNKNOWN, jarScan = jarScan)

    private fun declaration(serverSide: DeclaredSupport, jarScan: JarScan): Declaration? =
        DefaultBootRules.bundled()
            .firstMatch(listOf(facts(serverSide, jarScan)), RuleSource.METADATA)
            ?.rule?.declares

    /**
     * **The money case.** Modrinth says the server is required, the jar agrees, and the boot then dies on a
     * client-only class. The verdict is CONFIRMED and the declaration does not soften it.
     */
    @Test
    fun aModDeclaringServerThatCallsClientClassesIsConfirmedClient() {
        val verdict = VerdictPolicy.decide(
            staging = StagingOutcome.Staged,
            boot = BootObservation.Crashed(exitCode = 1),
            confirmedByRule = "client-only-class",
            declared = Declaration.SERVER
        )

        Assertions.assertEquals(
            Verdict.CONFIRMED, verdict,
            "a server declaration is a self-report; the console is what the boot exists to read"
        )
    }

    /**
     * No metadata rule may carry a [Verdict] at all. Giving one a verdict is how the short-circuit would
     * come back, and it would come back silently — the file would simply start publishing mods that were
     * never booted.
     */
    @Test
    fun noMetadataRuleCarriesAVerdict() {
        val offenders = DefaultBootRules.bundled().rules
            .filter { it.source == RuleSource.METADATA && it.verdict != null }

        Assertions.assertTrue(
            offenders.isEmpty(),
            "metadata may declare but never decide; offenders: ${offenders.map { it.id }}"
        )
    }

    /** Every declaration shape is recognised, so the report can say what a mod claimed. */
    @Test
    fun everyDeclarationShapeIsRecognised() {
        Assertions.assertEquals(
            Declaration.CLIENT, declaration(DeclaredSupport.UNSUPPORTED, JarScan.CLIENT),
            "platform says server unsupported and the jar says client"
        )
        Assertions.assertEquals(
            Declaration.SERVER, declaration(DeclaredSupport.REQUIRED, JarScan.SERVER_OR_BOTH),
            "both sources say the server is supported"
        )
        Assertions.assertEquals(
            Declaration.CONTRADICTORY, declaration(DeclaredSupport.UNSUPPORTED, JarScan.SERVER_OR_BOTH),
            "the platform and the jar disagree"
        )
    }

    /**
     * A declaration alone decides nothing, in either direction: a mod declaring client-only still has to be
     * booted, and a clean boot with no console match is CLEAR rather than CONFIRMED.
     */
    @Test
    fun aClientDeclarationDoesNotConfirmWithoutAConsoleMatch() {
        val verdict = VerdictPolicy.decide(
            staging = StagingOutcome.Staged,
            boot = BootObservation.Survived,
            confirmedByRule = null,
            declared = Declaration.CLIENT
        )

        Assertions.assertEquals(
            Verdict.CLEAR, verdict,
            "the mod said client, the server booted anyway, and nothing in the console agreed with the mod"
        )
    }

    /**
     * The inverse of the money case, and the reason CLEAR is not the same as "declared server". A mod
     * declaring client that boots clean is CLEAR on the evidence — the declaration does not drag it back.
     */
    @Test
    fun aServerDeclarationDoesNotRescueAConsoleConfirmation() {
        listOf(Declaration.SERVER, Declaration.CLIENT, Declaration.CONTRADICTORY, null).forEach { declared ->
            Assertions.assertEquals(
                Verdict.CONFIRMED,
                VerdictPolicy.decide(
                    StagingOutcome.Staged, BootObservation.Crashed(1), "client-only-class", declared
                ),
                "declaration $declared changed a console confirmation"
            )
        }
    }

    /**
     * A declaration must not turn a boot that learned nothing into a verdict either. An unexplained crash
     * stays INCONCLUSIVE however the mod described itself.
     */
    @Test
    fun aDeclarationDoesNotDecideAnUnexplainedCrash() {
        listOf(Declaration.SERVER, Declaration.CLIENT, Declaration.CONTRADICTORY, null).forEach { declared ->
            Assertions.assertEquals(
                Verdict.INCONCLUSIVE,
                VerdictPolicy.decide(StagingOutcome.Staged, BootObservation.Crashed(1), null, declared),
                "declaration $declared decided a crash no rule explained"
            )
        }
    }
}
