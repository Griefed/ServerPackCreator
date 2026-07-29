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
import de.griefed.serverpackcreator.grinder.container.PACK_MOUNT
import de.griefed.serverpackcreator.grinder.loader.ApiVanillaPackGenerator
import de.griefed.serverpackcreator.grinder.loader.ImageJavaRuntimes
import de.griefed.serverpackcreator.grinder.loader.PackVariables
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
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
    private val shells = envList("SPC_GRINDER_TEMPLATE_SHELLS", "bash,fish")
    /**
     * Concurrent boots. **Defaults to 1 on purpose.** Each cell boots a real Minecraft server with a 3 GB
     * container limit, so running several at once starves the host: measured on a dev machine, 3 workers
     * produced 8 spurious failures (`start.sh: line 144: Killed "$JAVA"` — the JVM SIGKILLed mid
     * "Preparing level"), and every one of them passed when re-run serially. For a correctness harness a
     * false FAIL is far worse than a slow pass, so raise this only with headroom to match.
     */
    private val workers = env("SPC_GRINDER_TEMPLATE_WORKERS", "1").toInt()
    private val bootTimeout = Duration.ofMinutes(env("SPC_GRINDER_TEMPLATE_TIMEOUT_MINUTES", "20").toLong())

    /** The vanilla server's ready-line — the template did its job when this appears. */
    private val readyLine = Regex("""Done \([^)]*\)! For help""")

    /**
     * How each shell is invoked and which generated script it runs. **Boot cells are Linux shells only.**
     * `.ps1` is deliberately absent: the PowerShell template shells out to Windows `CMD /C` (Java-version
     * detection, the server launch itself and the bit check), so it cannot execute in a Linux container by
     * design — it is validated by [powerShellTemplatesParse] instead. Verified empirically: a `pwsh` boot
     * dies at `The term 'CMD' is not recognized`, then mis-detects Java and aborts at the Jabba prompt.
     */
    private fun scriptFor(shell: String): String = when (shell) {
        "bash" -> "start.sh"
        "fish" -> "start.fish"
        "pwsh" -> throw IllegalArgumentException(
            "`.ps1` cannot be booted in a Linux container (it invokes Windows `CMD`); " +
                "PowerShell is covered by the parse check, not the boot matrix"
        )
        else -> throw IllegalArgumentException("Unknown shell '$shell' (expected bash|fish)")
    }

    /** Launch a script with its shell. Both bash and fish take the script path directly. */
    private fun commandFor(shell: String, script: String): List<String> = listOf(shell, script)

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
        val imageJava = ImageJavaRuntimes.from(apiWrapper.versionMeta.minecraft)
        val resolver = LoaderVersionResolver(apiWrapper.versionMeta)
        val engine = DockerJavaContainerEngine()
        val genRoot = File(System.getProperty("java.io.tmpdir"), "spc-template-it").apply { mkdirs() }

        val cells = minecraftVersions.flatMap { mc ->
            loaders.flatMap { loader ->
                shells.map { shell -> Cell(loader, mc, shell, scriptFor(shell)) }
            }
        }
        // A cell is runnable only when the image has the Minecraft version's JDK *and* the loader really
        // has a build for it. Resolving the loader version up-front doubles as that filter and gives each
        // valid cell its version without a second (nullable) lookup later.
        val loaderVersions: Map<Cell, String> = cells.mapNotNull { cell ->
            val version = if (imageJava.supports(cell.minecraftVersion)) {
                resolver.latest(cell.loader, cell.minecraftVersion)
            } else {
                null
            }
            version?.let { cell to it }
        }.toMap()
        val valid = cells.filter { it in loaderVersions }
        val invalid = cells.filterNot { it in loaderVersions }

        // Generate a pack per valid cell (sequential), then boot them in bounded parallel. Forcing the
        // default sh/fish/ps1 templates is a mutation of process-wide ApiProperties, so it is scoped to
        // generation and restored afterwards — otherwise it would leak into any other test in this JVM.
        val previousStartTemplates = HashMap(apiWrapper.apiProperties.startScriptTemplates)
        val previousJavaTemplates = HashMap(apiWrapper.apiProperties.javaScriptTemplates)
        val packs: Map<Cell, File?> = try {
            apiWrapper.apiProperties.startScriptTemplates = apiWrapper.apiProperties.defaultStartScriptTemplates()
            apiWrapper.apiProperties.javaScriptTemplates = apiWrapper.apiProperties.defaultJavaScriptTemplates()
            valid.associateWith { cell ->
                val cellId = sanitize("${cell.loader}-${cell.minecraftVersion}-${cell.shell}")
                ApiVanillaPackGenerator(apiWrapper, File(genRoot, cellId))
                    .generate(cell.loader, loaderVersions.getValue(cell), cell.minecraftVersion)
            }
        } finally {
            apiWrapper.apiProperties.startScriptTemplates = previousStartTemplates
            apiWrapper.apiProperties.javaScriptTemplates = previousJavaTemplates
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

    /**
     * PowerShell's coverage: parse both shipped `.ps1` templates with PowerShell's **own** parser inside
     * the image. This is the honest ceiling on Linux — the templates invoke Windows `CMD`, so they cannot
     * be *booted* here (see [scriptFor]) — but a parse catches the syntax-level regressions that are the
     * whole reason these templates get tested at all (the `.fish` bug was found the same way, one rung up).
     * Templates are mounted read-only straight from the api resources, so this checks what actually ships.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "GRINDER_TEMPLATE_IT", matches = "1")
    fun powerShellTemplatesParse() {
        val templates = File("../serverpackcreator-api/src/main/resources/de/griefed/resources/server_files")
            .canonicalFile
        Assertions.assertTrue(templates.isDirectory, "template resources not found at $templates")

        // `HOME=/tmp` because pwsh writes $HOME/.cache on start-up and the rootfs is read-only (tmpfs /tmp).
        val script = """
            HOME=/tmp exec pwsh -NoProfile -Command '
              ${'$'}failed = 0
              foreach (${'$'}f in @("/templates/default_template.ps1","/templates/default_java_template.ps1")) {
                ${'$'}errors = ${'$'}null
                [System.Management.Automation.Language.Parser]::ParseFile(${'$'}f, [ref]${'$'}null, [ref]${'$'}errors) | Out-Null
                if (${'$'}errors) { Write-Output ("PARSE ERRORS in " + ${'$'}f); ${'$'}errors | ForEach-Object { Write-Output ${'$'}_.ToString() }; ${'$'}failed = 1 }
                else { Write-Output ("parse OK: " + ${'$'}f) }
              }
              exit ${'$'}failed'
        """.trimIndent()

        val output = DockerJavaContainerEngine().run(
            ContainerSpec(
                image = image,
                command = listOf("sh", "-c", script),
                workingDir = "/templates",
                mounts = listOf(BindMount(templates.absolutePath, "/templates", readOnly = true)),
                networkMode = "none"
            ),
            readyPattern = Regex("""PARSE ERRORS"""), // never expected; the run simply exits
            timeout = Duration.ofMinutes(3)
        )
        val rendered = output.lines.joinToString("\n")
        Assertions.assertEquals(0, output.exitCode, "PowerShell reported parse errors:\n$rendered")
        Assertions.assertTrue(
            rendered.contains("parse OK: /templates/default_template.ps1"),
            "expected a successful parse of default_template.ps1, got:\n$rendered"
        )
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
}
