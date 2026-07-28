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

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.clientside.LoaderVersionResolver
import de.griefed.serverpackcreator.grinder.container.BindMount
import de.griefed.serverpackcreator.grinder.container.ContainerSpec
import de.griefed.serverpackcreator.grinder.container.DockerJavaContainerEngine
import de.griefed.serverpackcreator.grinder.loader.ApiVanillaPackGenerator
import de.griefed.serverpackcreator.grinder.loader.ImageJavaRuntimes
import de.griefed.serverpackcreator.grinder.loader.PackVariables
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Boots the generated start-scripts (`start.sh` / `start.fish` / `start.ps1`) across a matrix of
 * Minecraft versions, loaders and shells to prove the **templates themselves** install the loader +
 * Minecraft server and reach the ready-line — the reason being the newly-added `.fish` templates, with
 * bash as the control and PowerShell alongside. This is what nothing else covers: the rest of the suite
 * only asserts template *files* are written, never that they run.
 *
 * Each cell = `(loader, minecraftVersion, shell)`. A mod-less pack is generated per cell (with the
 * default sh/fish/ps1 templates forced on), the per-Minecraft JDK is pointed at, and the pack is booted
 * **with network** (`networkMode=bridge`, since the template does its own install) in the
 * `spc-grinder-templates` image; the cell passes when the console reaches `Done (…)! For help`. Cells
 * whose loader has no build for the Minecraft version (or whose JDK the image lacks) are reported as
 * skipped, not failed.
 *
 * Integration-only — needs a live Docker daemon, the built `spc-grinder-templates` image and a real
 * `ApiWrapper` — so it is **gated behind `GRINDER_TEMPLATE_IT=1`** and skipped on a normal run. It is
 * network-heavy and slow (each cell downloads a Minecraft server + loader). Run:
 * `GRINDER_TEMPLATE_IT=1 ./gradlew :serverpackcreator-grinder:test --tests "*ScriptTemplateMatrixIT"`.
 * The matrix dimensions and concurrency are env-overridable for a focused subset (see the fields).
 *
 * @author Griefed
 */
internal class ScriptTemplateMatrixIT {

    /** One matrix cell. [script] is the file the [shell] runs (e.g. bash → `start.sh`). */
    private data class Cell(val loader: String, val minecraftVersion: String, val shell: String, val script: String) {
        val label get() = "$loader $minecraftVersion [$shell]"
    }

    /** The outcome of booting one cell. */
    private data class Outcome(val passed: Boolean, val detail: String)

    private val image = env("SPC_GRINDER_IMAGE_TEMPLATES", "spc-grinder-templates:latest")
    private val minecraftVersions = envList("SPC_GRINDER_TEMPLATE_MC", "1.12.2,1.16.1,1.20.1")
    private val loaders = envList("SPC_GRINDER_TEMPLATE_LOADERS", "Forge,NeoForge,Fabric,Quilt")
    private val shells = envList("SPC_GRINDER_TEMPLATE_SHELLS", "bash,fish,pwsh")
    private val workers = env("SPC_GRINDER_TEMPLATE_WORKERS", "3").toInt()
    private val bootTimeout = Duration.ofMinutes(env("SPC_GRINDER_TEMPLATE_TIMEOUT_MINUTES", "20").toLong())

    /** The vanilla server's ready-line — the template did its job when this appears. */
    private val readyLine = Regex("""Done \([^)]*\)! For help""")

    /** How each shell is invoked and which generated script it runs. */
    private fun scriptFor(shell: String): String = when (shell) {
        "bash" -> "start.sh"
        "fish" -> "start.fish"
        "pwsh" -> "start.ps1"
        else -> throw IllegalArgumentException("Unknown shell '$shell' (expected bash|fish|pwsh)")
    }

    private fun commandFor(shell: String, script: String): List<String> =
        if (shell == "pwsh") listOf("pwsh", "-File", script) else listOf(shell, script)

    /**
     * Build the matrix, generate a pack per valid cell (sequentially — generation is cheap and mutates
     * shared `ApiProperties`), boot them in bounded parallel, and emit a per-cell [DynamicTest]. Invalid
     * cells (loader has no build for the Minecraft version, or the image lacks its JDK) are reported
     * skipped via a failed assumption.
     */
    @TestFactory
    @EnabledIfEnvironmentVariable(named = "GRINDER_TEMPLATE_IT", matches = "1")
    fun scriptTemplateMatrix(): List<DynamicTest> {
        val apiWrapper = System.getenv("SPC_GRINDER_SPC_PROPERTIES")?.takeIf { it.isNotBlank() }
            ?.let { ApiWrapper.api(File(it)) }
            ?: ApiWrapper.api()
        // Force the *default* sh/fish/ps1 templates so every generated pack carries all three scripts,
        // regardless of any custom template override in the SPC properties.
        apiWrapper.apiProperties.startScriptTemplates = apiWrapper.apiProperties.defaultStartScriptTemplates()
        apiWrapper.apiProperties.javaScriptTemplates = apiWrapper.apiProperties.defaultJavaScriptTemplates()

        val imageJava = ImageJavaRuntimes.from(apiWrapper.versionMeta.minecraft)
        val resolver = LoaderVersionResolver(apiWrapper.versionMeta)
        val engine = DockerJavaContainerEngine()
        val genRoot = File(System.getProperty("java.io.tmpdir"), "spc-template-it").apply { mkdirs() }

        val cells = minecraftVersions.flatMap { mc ->
            loaders.flatMap { loader ->
                shells.map { shell -> Cell(loader, mc, shell, scriptFor(shell)) }
            }
        }
        // Valid = the image has the JDK and the loader actually has a build for this Minecraft version.
        val (valid, invalid) = cells.partition {
            imageJava.supports(it.minecraftVersion) && resolver.latest(it.loader, it.minecraftVersion) != null
        }

        // Generate a pack per valid cell (sequential), then boot them in bounded parallel.
        val packs: Map<Cell, File?> = valid.associateWith { cell ->
            val loaderVersion = resolver.latest(cell.loader, cell.minecraftVersion)!!
            val cellId = sanitize("${cell.loader}-${cell.minecraftVersion}-${cell.shell}")
            ApiVanillaPackGenerator(apiWrapper, File(genRoot, cellId)).generate(cell.loader, loaderVersion, cell.minecraftVersion)
        }
        val executor = Executors.newFixedThreadPool(workers.coerceAtLeast(1))
        val futures: Map<Cell, Future<Outcome>> = valid.associateWith { cell ->
            executor.submit(Callable { boot(cell, packs[cell], imageJava, engine) })
        }
        executor.shutdown() // no new tasks; submitted boots run to completion

        val tests = ArrayList<DynamicTest>(cells.size)
        invalid.forEach { cell ->
            tests.add(DynamicTest.dynamicTest("${cell.label} [N/A]") {
                Assumptions.assumeTrue(false, "N/A: ${cell.loader} has no build for Minecraft ${cell.minecraftVersion} (or its JDK is not bundled)")
            })
        }
        valid.forEach { cell ->
            tests.add(DynamicTest.dynamicTest(cell.label) {
                val outcome = futures.getValue(cell).get()
                Assertions.assertTrue(outcome.passed, "${cell.label} did not reach the ready-line:\n${outcome.detail}")
            })
        }
        return tests
    }

    /** Boot one cell in a container and classify by the ready-line. */
    private fun boot(cell: Cell, pack: File?, imageJava: ImageJavaRuntimes, engine: DockerJavaContainerEngine): Outcome {
        if (pack == null) {
            return Outcome(false, "pack generation failed")
        }
        val javaPath = imageJava.javaPath(cell.minecraftVersion)
            ?: return Outcome(false, "no bundled JDK for ${cell.minecraftVersion}")
        // offline=false: the template must do its own network install (loader + Minecraft server).
        PackVariables.prepareUnattended(pack, javaPath, offline = false)

        val spec = ContainerSpec(
            image = image,
            command = commandFor(cell.shell, cell.script),
            workingDir = PACK_MOUNT,
            mounts = listOf(BindMount(pack.absolutePath, PACK_MOUNT, readOnly = false)),
            networkMode = "bridge"
        )
        val output = engine.run(spec, readyLine, bootTimeout)
        val reached = output.lines.any { readyLine.containsMatchIn(it) }
        return if (reached) {
            Outcome(true, "reached ready-line")
        } else {
            Outcome(false, "exitCode=${output.exitCode} timedOut=${output.timedOut}\n" + output.lines.takeLast(25).joinToString("\n"))
        }
    }

    private fun env(key: String, default: String): String = System.getenv(key)?.takeIf { it.isNotBlank() } ?: default
    private fun envList(key: String, default: String): List<String> = env(key, default).split(',').map { it.trim() }.filter { it.isNotEmpty() }
    private fun sanitize(token: String): String = token.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        /** Where the pack is bind-mounted inside the container (and its working directory). */
        const val PACK_MOUNT = "/srv/pack"
    }
}
