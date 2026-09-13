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
import kotlin.test.Test

/**
 * **A hard dependency the loader itself refuses as client-only is evidence about the candidate, not an
 * excuse for it.** Fabric says so in its own words — *"which is disabled for this environment
 * (client/server only)"* — and a mod that cannot load without a dependency the server will never have
 * cannot run on a server either. That is exactly what the fallback list is for.
 *
 * Measured on the public grinder 2026-09-13: `Modrinth/voxy` and `Modrinth/cull-less-leaves` both name
 * `sodium` this way and were published `INCONCLUSIVE / DEPENDENCY_FAILURE` — the boot was paid for and its
 * strongest finding discarded, because `Incompatible mods found` on the line above matches the
 * dependency-failure rung first.
 *
 * @author Griefed
 */
internal class ClientOnlyDependencyTest {

    private val voxy =
        " - Mod 'Voxy' (voxy) 0.2.16-beta requires version 0.8.4, version 0.8.6, version 0.8.7, " +
            "version 0.8.11 or version 0.8.12 of sodium, which is disabled for this environment " +
            "(client/server only)!"

    private val cullLessLeaves =
        " - Mod 'Cull Less Leaves' (cull-less-leaves) 1.4.2+1.21-fabric requires any version before 0.6 " +
            "of sodium, which is disabled for this environment (client/server only)!"

    /** The whole console, in the order Fabric prints it — the excuse line really does come first. */
    private fun console(detail: String) = listOf(
        "[04:00:00] [main/ERROR]: Incompatible mods found!",
        "net.fabricmc.loader.impl.FormattedException: Some of your mods are incompatible with the game or each other!",
        detail
    )

    private fun classify(lines: List<String>) =
        BootLogClassifier.classify(lines, exitCode = 1, timedOut = false, ConsoleRuleSet.EMPTY)

    @Test
    fun aDependencyTheLoaderCallsClientOnlyIsACrash() {
        for (detail in listOf(voxy, cullLessLeaves)) {
            val classification = classify(console(detail))
            Assertions.assertEquals(BootResult.CRASHED, classification.result, detail)
            Assertions.assertEquals(
                BootDecision.CLIENT_ONLY_DEPENDENCY, classification.decidedBy, detail
            )
        }
    }

    /** Decisive, so it may publish — that is the entire point of separating it from the excuse. */
    @Test
    fun theRungIsDecisive() {
        Assertions.assertTrue(BootDecision.CLIENT_ONLY_DEPENDENCY.decisive)
    }

    /**
     * **But it does not cross loaders.** `provesClientOnly` licenses a crash to clear every other build and
     * loader of the project, and this evidence is about *one* build's declared dependencies: a mod's Fabric
     * jar may depend on Sodium where its NeoForge jar depends on nothing of the sort.
     */
    @Test
    fun theRungDoesNotProveTheProjectClientOnly() {
        Assertions.assertFalse(BootDecision.CLIENT_ONLY_DEPENDENCY.provesClientOnly)
    }

    /** An ordinary unmet dependency is still the excuse it always was. */
    @Test
    fun anOrdinaryMissingDependencyIsUnchanged() {
        val classification = classify(
            listOf(
                "[main/ERROR] [ModSorter/LOADING]: Missing or unsupported mandatory dependencies:",
                "\tMod ID: 'botanypots', Requested by: 'botanytrees', Expected range: '[21.1.34,21.2)', " +
                    "Actual version: '[MISSING]'"
            )
        )
        Assertions.assertEquals(BootResult.INCONCLUSIVE, classification.result)
        Assertions.assertEquals(BootDecision.DEPENDENCY_FAILURE, classification.decidedBy)
    }
}
