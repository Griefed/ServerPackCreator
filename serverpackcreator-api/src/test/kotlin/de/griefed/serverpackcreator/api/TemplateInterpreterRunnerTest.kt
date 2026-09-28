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
package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.DONE_MARKER
import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.FAILURE_PREFIX
import de.griefed.serverpackcreator.api.TemplateInterpreterRunner.Companion.SETUP_MARKER
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

/**
 * Guards the harness itself: a container started by [TemplateInterpreterRunner] must actually see the
 * templates it was given.
 *
 * Without this, every check built on that runner is only as trustworthy as its transport — and the
 * transport failed silently in exactly the way that matters. A `-v <hostPath>:/templates` bind is
 * resolved by the **daemon's** filesystem, so on Forgejo's runner, whose daemon is a sibling that holds
 * no copy of the job container's `/tmp`, Docker created an empty directory and mounted that. The fish
 * check then reported a `FAIL` line naming the fish glob itself, unexpanded because nothing matched it
 * (run 646, job 1650); the PowerShell parse check matched zero files and **passed**; and the
 * installer-Java probe found no script and skipped.
 * One transport defect, three different-looking verdicts, none of them about a template.
 *
 * Reproduced and fixed against a real remote daemon rather than argued: `docker run -d --privileged
 * -e DOCKER_TLS_CERTDIR= -p 12375:2375 docker:dind`, then `DOCKER_HOST=tcp://127.0.0.1:12375`. With a
 * bind this test fails; with the copy the runner does now it passes, on that daemon and on a local one.
 *
 * @author Griefed
 */
internal class TemplateInterpreterRunnerTest {

    private val runner = TemplateInterpreterRunner()

    /**
     * Stage the real templates, then have the container name back what it can see.
     *
     * `busybox` rather than the interpreter images: this asks about the transport, not about parsing, and
     * it is already pulled for the grinder's container tests.
     */
    @Test
    fun theContainerSeesTheStagedTemplates() {
        val names = listOf("default_template.fish", "default_java_template.fish")
        val staged = runner.stageTemplates(names)

        val outcome = runner.runInContainer(
            image = "busybox:latest",
            staged = staged,
            script = "echo $SETUP_MARKER; " +
                    "for f in /templates/*; do echo \"SAW \$(basename \"\$f\")\"; done; " +
                    "echo $DONE_MARKER"
        )

        Assumptions.assumeTrue(outcome != null, "no Docker daemon — the runner's transport was NOT checked")
        Assumptions.assumeTrue(
            outcome!!.setUp && outcome.completed,
            "the probe container never ran to its end, so it checked nothing: ${outcome.output.take(400)}"
        )

        for (name in names) {
            Assertions.assertTrue(
                outcome.output.lineSequence().any { it.trim() == "SAW $name" },
                "${outcome.ranWith} could not see the staged '$name'; the templates never reached it:\n${outcome.output}"
            )
        }
        Assertions.assertTrue(
            outcome.failures.isEmpty(),
            "the probe reported failures it should not have:\n${outcome.output}"
        )
    }

    /**
     * A container that cannot be created is an environment answer, not a verdict — it must skip.
     *
     * Named an image no registry serves, so the creation fails after the daemon has been reached. The
     * point is the *shape* of the result: null or `setUp == false`, never an outcome a caller could read
     * as "the template is fine".
     *
     * @see TemplateInterpreterRunner.Outcome
     */
    @Test
    fun anImageThatCannotBeCreatedSkipsRatherThanPasses() {
        val staged = runner.stageTemplates(listOf("default_template.fish"))

        val outcome = runner.runInContainer(
            image = "spc-no-such-image-should-ever-exist:0",
            staged = staged,
            script = "echo $SETUP_MARKER; echo \"$FAILURE_PREFIX never reached\"; echo $DONE_MARKER"
        )

        Assumptions.assumeTrue(runner.onPath("docker") != null, "no docker binary — nothing to check")
        Assertions.assertFalse(
            outcome != null && outcome.setUp,
            "an unusable image must not report a prepared environment, or a broken run reads as a clean one: $outcome"
        )
    }
}
