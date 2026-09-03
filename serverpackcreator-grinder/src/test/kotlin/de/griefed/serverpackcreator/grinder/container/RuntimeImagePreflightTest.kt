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
package de.griefed.serverpackcreator.grinder.container

import de.griefed.serverpackcreator.grinder.grinderMainBody
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * **A host defect must not be published as thousands of verdicts about mods.**
 *
 * Measured on the live daemon 2026-09-03: `spc-grinder-runtime:latest` had been removed from the Docker
 * daemon (nothing in `install-grinder.sh` removes it — a `docker system prune -a` does, because the image is
 * only in use *during* a boot). Every install then threw `Status 404: {"message":"No such image:
 * spc-grinder-runtime:latest"}`, every tuple went on install cooldown, and every candidate wanting one was
 * scored INCONCLUSIVE — thousands of them, each carrying a sentence about a loader tuple. Since
 * `JsonVerdictStore.record` replaces by identity and the re-verify TTL is 30 days, projects that had held a
 * decisive HIGH lost it, and with it their line in `/as-properties`.
 *
 * This is the loader-cache-poisoning lesson one level up: *an environment defect looks exactly like a subject
 * defect unless something distinguishes them*. The per-tuple cooldown actively disguised it, because a single
 * host-wide failure is recorded as one independent failure per tuple. The image is knowable in one call before
 * the first candidate, so it is checked there.
 */
internal class RuntimeImagePreflightTest {

    /** A [ContainerEngine] that answers the image question and nothing else; boots are out of scope here. */
    private class ImageEngine(private val present: Boolean) : ContainerEngine {
        override fun run(spec: ContainerSpec, readyPattern: Regex, timeout: Duration, onLine: (String) -> Unit): ContainerRunOutput =
            Assertions.fail("the preflight must not run a container to find out whether the image exists")

        override fun hasImage(image: String): Boolean = present
    }

    /** An engine with no notion of images at all — every test fake in this module is one. */
    private class ImagelessEngine : ContainerEngine {
        override fun run(spec: ContainerSpec, readyPattern: Regex, timeout: Duration, onLine: (String) -> Unit): ContainerRunOutput =
            Assertions.fail("not reached")
    }

    @Test
    fun aMissingRuntimeImageIsRefusedByNameAndSaysHowToRebuildIt() {
        val refusal = RuntimeImagePreflight.refusalFor(ImageEngine(present = false), "spc-grinder-runtime:latest")
            ?: Assertions.fail("a missing runtime image must stop the daemon, not be ground into verdicts")

        Assertions.assertTrue(
            refusal.contains("spc-grinder-runtime:latest"),
            "the refusal must name the image, since it is configurable (SPC_GRINDER_IMAGE). Was: $refusal"
        )
        Assertions.assertTrue(
            refusal.contains("docker build"),
            "the refusal must carry the repair; an operator reading it at 3am should not have to find the " +
                "Dockerfile themselves. Was: $refusal"
        )
    }

    @Test
    fun aPresentImageStartsTheDaemon() {
        Assertions.assertNull(
            RuntimeImagePreflight.refusalFor(ImageEngine(present = true), "spc-grinder-runtime:latest"),
            "the normal case must cost nothing and say nothing"
        )
    }

    /**
     * The default has to be permissive, or every fake engine in the suite would refuse to start a grinder.
     * Only the docker-java engine can actually answer this question.
     */
    @Test
    fun anEngineWithNoNotionOfImagesDoesNotBlockStartup() {
        Assertions.assertNull(RuntimeImagePreflight.refusalFor(ImagelessEngine(), "spc-grinder-runtime:latest"))
    }

    /**
     * The unit is worthless unless `main` consults it *before* it starts grinding — the whole failure being
     * fixed is thousands of candidates ground against an image that is not there. Asserted against the source
     * because `main` builds an `ApiWrapper` and a Docker client and cannot be executed here, the same
     * technique `ShutdownWiringTest` and `ReportBindWiringTest` use.
     */
    @Test
    fun mainRefusesToGrindBeforeItBuildsAPool() {
        val body = grinderMainBody()
        val preflight = body.indexOf("RuntimeImagePreflight.refusalFor(")
        val pool = body.indexOf("GrindPool(")

        Assertions.assertTrue(preflight >= 0, "main() no longer checks that the runtime image exists")
        Assertions.assertTrue(pool >= 0, "main() no longer builds a GrindPool — has the entry point changed shape?")
        Assertions.assertTrue(
            preflight < pool,
            "the image must be checked BEFORE any candidate is ground; checking it afterwards is exactly the " +
                "outage this guard exists for"
        )
    }
}
