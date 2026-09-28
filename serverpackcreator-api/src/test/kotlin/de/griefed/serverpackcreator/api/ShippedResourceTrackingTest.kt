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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Pins that the resources SPC *ships* stay tracked by git, and that the home directory it *creates* stays ignored.
 *
 * `.gitignore` carries a bare `server_files` rule, which matches that directory name at any depth. The shipped
 * start-script templates under `src/main/resources` were therefore tracked only because they predate the rule:
 * anything added later was invisible. `variables.txt` needed `git add -f`, and a subsequent template fix drew the
 * same "paths are ignored" warning — a packaging bug whose only symptom is a file missing from a release.
 *
 * A re-include fixed it, but nothing pinned it, and the regression is silent in exactly the way the original was.
 * Both directions matter and pull against each other, which is why a single assertion will not do: root-anchoring
 * the rule would expose every module's generated `tests/server_files` instead.
 *
 * **Measurement landmine:** `git check-ignore` skips paths that contain tracked files unless `--no-index` is
 * passed, and will report a still-ignored directory as clean. The first reading of this very finding was wrong
 * for that reason. Always `--no-index` here.
 */
internal class ShippedResourceTrackingTest {

    /** Repository root, found by walking up from this module rather than assuming a working directory. */
    private val repositoryRoot: File = generateSequence(File("").absoluteFile) { it.parentFile }
        .firstOrNull { File(it, ".gitignore").isFile && File(it, ".git").exists() }
        ?: File("..").absoluteFile

    /** Run `git check-ignore --no-index -q <path>`; true when git would ignore it. Null when git is unavailable. */
    private fun isIgnored(relativePath: String): Boolean? {
        val process = runCatching {
            ProcessBuilder("git", "check-ignore", "--no-index", "-q", relativePath)
                .directory(repositoryRoot)
                .redirectErrorStream(true)
                .start()
        }.getOrNull() ?: return null
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return null
        }
        // 0 = ignored, 1 = not ignored, anything else = git could not answer.
        return when (process.exitValue()) {
            0 -> true
            1 -> false
            else -> null
        }
    }

    /** The shipped templates and `variables.txt` must be visible to git, or a new one ships untracked. */
    @Test
    fun theShippedServerFilesResourcesAreNotIgnored() {
        val shipped = "serverpackcreator-api/src/main/resources/de/griefed/resources/server_files"
        Assertions.assertTrue(
            File(repositoryRoot, shipped).isDirectory,
            "expected the shipped resources at $shipped, relative to $repositoryRoot"
        )

        val ignored = isIgnored(shipped)
        Assumptions.assumeTrue(ignored != null, "git unavailable — ignore rules cannot be checked here")
        Assertions.assertFalse(
            ignored!!,
            "git ignores $shipped, so any resource added there is invisible: it would need `git add -f` and would " +
                "otherwise ship missing from a release. Narrow the `server_files` rule instead of broadening it."
        )
    }

    /** And the flip side: the home directory a test run creates must stay ignored, or every run dirties the tree. */
    @Test
    fun theGeneratedTestHomesStayIgnored() {
        for (module in listOf("serverpackcreator-api", "serverpackcreator-clientside", "serverpackcreator-app")) {
            val generated = "$module/tests/server_files"
            val ignored = isIgnored(generated)
            Assumptions.assumeTrue(ignored != null, "git unavailable — ignore rules cannot be checked here")
            Assertions.assertTrue(
                ignored!!,
                "$generated must stay ignored: ApiWrapper.setup() writes the templates into each module's test " +
                    "home, so exposing it would leave every test run with a dirty working tree. This is why the " +
                    "fix re-includes the resource path rather than anchoring the rule to the repository root."
            )
        }
    }
}
