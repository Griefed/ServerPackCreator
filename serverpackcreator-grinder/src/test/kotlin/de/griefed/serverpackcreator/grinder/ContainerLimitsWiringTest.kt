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

import de.griefed.serverpackcreator.grinder.container.ContainerResources
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Keeps `SPC_GRINDER_CPUS` and `SPC_GRINDER_MEMORY_GIB` connected to every container they are supposed to cap.
 *
 * Exactly the defect class `ReportBindWiringTest` was written for, and the one these knobs started as: every
 * container-creating collaborator has accepted a `ContainerResources` since it existed, and `main` passed
 * none — so both caps were hardcoded defaults and no environment could change them. Both call sites matter:
 * the mod-boot (`ContainerCandidateVerifier`, one per worker, continuously) and the loader install
 * (`DockerLoaderInstaller`, the heaviest single container the daemon runs). `main` boots Docker, so its own
 * text is the only guard available for the join.
 */
internal class ContainerLimitsWiringTest {

    /** The name `main` reads [knob] into, failing loudly if it stopped reading it at all. */
    private fun knobVariable(body: String, knob: String): String =
        Regex("""val\s+(\w+)\s*=\s*env\("$knob"""").find(body)?.groupValues?.get(1)
            ?: Assertions.fail("main() does not read $knob — that cap would be unconfigurable")

    /** The `ContainerResources` name `main` derives from both knobs, in the order [ContainerResources.forLimits] takes them. */
    private fun resourcesVariable(body: String): String {
        val cpus = knobVariable(body, "SPC_GRINDER_CPUS")
        val memory = knobVariable(body, "SPC_GRINDER_MEMORY_GIB")
        return Regex("""val\s+(\w+)\s*=\s*ContainerResources\.forLimits\($cpus,\s*$memory\)""")
            .find(body)?.groupValues?.get(1)
            ?: Assertions.fail(
                "main() reads the caps into `$cpus`/`$memory` but never turns both into one ContainerResources"
            )
    }

    /** Constructor arguments of [type] in [body], matched across newlines — the calls are wrapped. */
    private fun construction(body: String, type: String): String =
        Regex("""$type\((.*?)\n\s*\)""", RegexOption.DOT_MATCHES_ALL).find(body)?.groupValues?.get(1)
            ?: Assertions.fail("main() no longer constructs a $type")

    @Test
    fun theConfiguredCpuCapReachesTheModBootContainers() {
        val body = grinderMainBody()
        val resources = resourcesVariable(body)

        Assertions.assertTrue(
            construction(body, "ContainerCandidateVerifier").contains("resources = $resources"),
            "main() never hands its ContainerResources to ContainerCandidateVerifier — every mod boot would " +
                "run on the hardcoded defaults however the knobs are set"
        )
    }

    @Test
    fun theConfiguredCpuCapReachesTheLoaderInstallContainers() {
        val body = grinderMainBody()
        val resources = resourcesVariable(body)

        Assertions.assertTrue(
            construction(body, "DockerLoaderInstaller").contains("resources = $resources"),
            "main() never hands its ContainerResources to DockerLoaderInstaller — a loader install is the " +
                "heaviest container the daemon runs and would stay on the hardcoded defaults"
        )
    }

    /**
     * The startup line must state the cap, and state it as [ContainerResources.cpuCapDescription] renders it.
     *
     * That line is the only place an operator sees what the daemon actually resolved — the same role it
     * already plays for `containerUser=`, which exists because the alternative was reproducing a permission
     * failure to find out. A raw quota there would answer in a unit nobody set.
     */
    @Test
    fun theStartupLineStatesTheCapInTheOperatorsUnit() {
        val body = grinderMainBody()
        // Bounded by the closing paren *on its own line*: a lazy match to the first `)` stops inside the
        // very call being asserted on, and the guard then fails on a line that is in fact correct.
        val startupLine = Regex("""log\.info\(\s*\n\s*"Grinder starting(.*?)\n\s*\)""", RegexOption.DOT_MATCHES_ALL)
            .find(body) ?: Assertions.fail("main() no longer logs a `Grinder starting` line")

        Assertions.assertTrue(
            startupLine.groupValues[1].contains("cpuCapDescription()"),
            "the startup line must render the CPU cap through cpuCapDescription() — a bare quota reads as a " +
                "microsecond count nobody set, and `0` reads as no CPU when it means uncapped. Line was: " +
                startupLine.value
        )
        Assertions.assertTrue(
            startupLine.groupValues[1].contains("memoryCapDescription()"),
            "the startup line must render the memory cap too — it is the value the per-boot heap is derived " +
                "from, so it belongs where an operator can see what the daemon resolved. Line was: " +
                startupLine.value
        )
    }

    /** Two cores and 3 GiB is what every install has been running on; the knobs must not re-tune them. */
    @Test
    fun theDefaultCapsAreTheOnesShippedBeforeTheKnobsExisted() {
        val body = grinderMainBody()

        Assertions.assertTrue(
            body.contains("""env("SPC_GRINDER_CPUS", "2")"""),
            "the per-container CPU cap must default to 2 cores — the value every existing install runs on"
        )
        Assertions.assertTrue(
            body.contains("""env("SPC_GRINDER_MEMORY_GIB", "3")"""),
            "the per-container memory cap must default to 3 GiB — the value the worker-sizing advice divides by"
        )
    }

    /**
     * The two places a default lives must agree: `main`'s fallback string, and `ContainerResources`' own
     * property defaults.
     *
     * Neither of the literal pins above can catch a drift *between* them — a class default moved to 4 GiB
     * with `main` still falling back to `"3"` leaves every code path that constructs `ContainerResources()`
     * directly (the fallbacks in `ContainerCandidateVerifier`, `ContainerServerRunner` and
     * `DockerLoaderInstaller`, and `ReadmeConfigurationTest`'s own sizing check) disagreeing with the
     * running daemon. So this asserts the identity rather than the values.
     */
    @Test
    fun theKnobDefaultsAgreeWithTheClassDefaults() {
        val body = grinderMainBody()
        val shipped = ContainerResources()

        val cpus = Regex("""env\("SPC_GRINDER_CPUS",\s*"([^"]*)"\)""").find(body)?.groupValues?.get(1)
            ?: Assertions.fail("main() no longer reads SPC_GRINDER_CPUS with a literal default")
        val memoryGiB = Regex("""env\("SPC_GRINDER_MEMORY_GIB",\s*"([^"]*)"\)""").find(body)?.groupValues?.get(1)
            ?: Assertions.fail("main() no longer reads SPC_GRINDER_MEMORY_GIB with a literal default")

        Assertions.assertEquals(
            shipped, ContainerResources.forLimits(cpus.toDouble(), memoryGiB.toDouble()),
            "main()'s fallbacks ($cpus cores / $memoryGiB GiB) must resolve to exactly the ContainerResources " +
                "defaults every other construction site falls back to"
        )
    }
}
