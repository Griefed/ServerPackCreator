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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Keeps `SPC_GRINDER_CPUS` connected to every container it is supposed to cap.
 *
 * Exactly the defect class `ReportBindWiringTest` was written for, and the one this knob started as: every
 * container-creating collaborator has accepted a `ContainerResources` since it existed, and `main` passed
 * none — so the cap was the hardcoded default and no environment could change it. Both call sites matter:
 * the mod-boot (`ContainerCandidateVerifier`, one per worker, continuously) and the loader install
 * (`DockerLoaderInstaller`, the CPU-heaviest single container the daemon runs). `main` boots Docker, so its
 * own text is the only guard available for the join.
 */
internal class CpuLimitWiringTest {

    /** The name `main` reads the knob into, failing loudly if it stopped reading it at all. */
    private fun cpuVariable(body: String): String =
        Regex("""val\s+(\w+)\s*=\s*env\("SPC_GRINDER_CPUS"""").find(body)?.groupValues?.get(1)
            ?: Assertions.fail("main() does not read SPC_GRINDER_CPUS — the CPU cap would be unconfigurable")

    /** The `ContainerResources` name `main` derives from that knob. */
    private fun resourcesVariable(body: String, cpus: String): String =
        Regex("""val\s+(\w+)\s*=\s*ContainerResources\.forCpus\($cpus\)""").find(body)?.groupValues?.get(1)
            ?: Assertions.fail("main() reads SPC_GRINDER_CPUS into `$cpus` but never turns it into ContainerResources")

    /** Constructor arguments of [type] in [body], matched across newlines — the calls are wrapped. */
    private fun construction(body: String, type: String): String =
        Regex("""$type\((.*?)\n\s*\)""", RegexOption.DOT_MATCHES_ALL).find(body)?.groupValues?.get(1)
            ?: Assertions.fail("main() no longer constructs a $type")

    @Test
    fun theConfiguredCpuCapReachesTheModBootContainers() {
        val body = grinderMainBody()
        val resources = resourcesVariable(body, cpuVariable(body))

        Assertions.assertTrue(
            construction(body, "ContainerCandidateVerifier").contains("resources = $resources"),
            "main() never hands its ContainerResources to ContainerCandidateVerifier — every mod boot would " +
                "run on the hardcoded default however SPC_GRINDER_CPUS is set"
        )
    }

    @Test
    fun theConfiguredCpuCapReachesTheLoaderInstallContainers() {
        val body = grinderMainBody()
        val resources = resourcesVariable(body, cpuVariable(body))

        Assertions.assertTrue(
            construction(body, "DockerLoaderInstaller").contains("resources = $resources"),
            "main() never hands its ContainerResources to DockerLoaderInstaller — a loader install is the " +
                "heaviest container the daemon runs and would stay uncapped by the knob"
        )
    }

    /** Two cores is what every install has been running on; the knob must not silently re-tune them. */
    @Test
    fun theDefaultCpuCapIsTheOneShippedBeforeTheKnobExisted() {
        Assertions.assertTrue(
            grinderMainBody().contains("""env("SPC_GRINDER_CPUS", "2")"""),
            "the per-container CPU cap must default to 2 cores — the value every existing install runs on"
        )
    }
}
