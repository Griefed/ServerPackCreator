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
import de.griefed.serverpackcreator.grinder.loader.ImageSupport
import de.griefed.serverpackcreator.grinder.loader.PackVariables
import org.junit.jupiter.api.*
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
 * The PowerShell checks live in `-api`, where they run on every push against a stock
 * `mcr.microsoft.com/powershell` image: `ShellTemplateSyntaxTest` parses the shipped templates and
 * `PowerShellInstallerJavaTest` executes `RunInstallerJavaCommand` for the `JAVA_INSTALLER` override and
 * its fallback. Neither needed this image or this matrix, and behind this gate neither ever ran. What is
 * left here is the one thing that genuinely does need them: booting a cell.
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
    /**
     * The Minecraft axis. **Both versioning schemes must stay represented**: the defaults carried only `1.x`
     * versions while three separate template bugs lived in `YY.x` handling (the Forge launcher era, the NeoForge
     * installer coordinate, and Forge's install ownership on Java 24+), so the branch the fixes were written for
     * was never exercised unless someone passed `SPC_GRINDER_TEMPLATE_MC` by hand. `26.2` is here to prevent that.
     */
    private val minecraftVersions = envList("SPC_GRINDER_TEMPLATE_MC", "1.12.2,1.16.1,1.20.1,1.21.11,26.2")
    private val loaders = envList("SPC_GRINDER_TEMPLATE_LOADERS", "Forge,NeoForge,Fabric,Quilt,LegacyFabric")
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
        //
        // Cells whose Minecraft version has *no known Java requirement* are held apart from both. They are not
        // "not applicable": SPC could not determine what the version needs, which is also what a failed manifest
        // download looks like, and reporting that as a skip is how all four Minecraft 26.2 cells vanished from a
        // green run -- Fabric included, which the live sweep boots fine. They are failed loudly below instead.
        val unknownJava = cells.filter { imageJava.supportFor(it.minecraftVersion) == ImageSupport.REQUIREMENT_UNKNOWN }
        val loaderVersions: Map<Cell, String> = cells.mapNotNull { cell ->
            val version = if (imageJava.supports(cell.minecraftVersion)) {
                resolver.latest(cell.loader, cell.minecraftVersion)
            } else {
                null
            }
            version?.let { cell to it }
        }.toMap()
        val valid = cells.filter { it in loaderVersions }
        val invalid = cells.filterNot { it in loaderVersions || it in unknownJava }

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
                val reason = when (imageJava.supportFor(cell.minecraftVersion)) {
                    ImageSupport.JDK_NOT_BUNDLED ->
                        "the image does not bundle Minecraft ${cell.minecraftVersion}'s required Java"
                    else -> "${cell.loader} has no build for Minecraft ${cell.minecraftVersion}"
                }
                Assumptions.assumeTrue(false, "N/A: $reason")
            })
        }
        // Loud on purpose: a metadata gap removes coverage, and it removes it from the newest Minecraft versions
        // first -- precisely the ones worth testing. Failing beats a skip that reads like a deliberate exclusion.
        unknownJava.forEach { cell ->
            tests.add(DynamicTest.dynamicTest("${cell.label} [metadata]") {
                Assertions.fail<Unit>(
                    "Minecraft ${cell.minecraftVersion} has no known required Java version, so this cell could not " +
                        "be run and its coverage is missing. This is a metadata failure, not an exclusion: SPC's " +
                        "per-version server manifest for ${cell.minecraftVersion} is absent and could not be " +
                        "downloaded (see the MinecraftServer warning in the log). Fix the metadata, or drop " +
                        "${cell.minecraftVersion} from SPC_GRINDER_TEMPLATE_MC deliberately."
                )
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
        // The installer JDK matters here: Quilt's installer needs Java 17+ even when the server runs on 8.
        PackVariables.prepareUnattended(
            pack,
            javaPath,
            offline = false,
            installerJavaPath = imageJava.installerJavaPathFor(cell.minecraftVersion)
        )

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
