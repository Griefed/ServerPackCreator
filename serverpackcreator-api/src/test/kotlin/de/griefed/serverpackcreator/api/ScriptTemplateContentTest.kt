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

/**
 * Guards the **shipped** start-script templates at the source level, without a container. The grinder's
 * `ScriptTemplateMatrixIT` actually boots them, but it is gated behind a live Docker daemon + a built
 * image, so it never runs in CI — these assertions do, and they pin the fixes that IT paid to discover.
 *
 * Kept deliberately narrow: it asserts *specific, load-bearing constructs*, not whole-file snapshots,
 * so ordinary template edits don't churn it.
 */
internal class ScriptTemplateContentTest {

    /** Read a shipped template from the classpath (`src/main/resources/.../server_files`). */
    private fun template(name: String): String =
        javaClass.getResourceAsStream("/de/griefed/resources/server_files/$name")
            ?.bufferedReader()?.use { it.readText() }
            ?: Assertions.fail("template resource '$name' is not on the classpath")

    /**
     * Regression guard for the fish word-splitting bug: `runJavaCommand` must split the assembled Java
     * command with `--no-empty`. Fish's `string split` keeps empty tokens where bash's word-splitting
     * drops them, so without this an empty `$JAVA_ARGS` (a double space) passes a stray "" argument that
     * Java reads as the main class → "Could not find or load main class". bash is unaffected. Found by
     * booting the templates in a container (bash reached the ready-line, fish did not).
     */
    @Test
    fun fishStartTemplateSplitsTheJavaCommandWithoutEmptyTokens() {
        val fish = template("default_template.fish")
        Assertions.assertTrue(
            fish.contains("string split --no-empty \" \" -- \$cmd"),
            "runJavaCommand must use `string split --no-empty` or an empty JAVA_ARGS breaks the boot"
        )
        Assertions.assertFalse(
            fish.contains("set -l args (string split \" \" -- \$cmd)"),
            "the empty-token-keeping split must not come back"
        )
    }

    /**
     * All three start-script templates must run modloader **installers** through the `JAVA_INSTALLER`
     * override rather than plain `JAVA`. The Quilt installer requires Java 17+ even when the server runs
     * on an older Java (Minecraft 1.16.1 → Java 8), so without this Quilt cannot be installed on older
     * Minecraft at all — reproduced in a container for bash *and* fish before the fix. The override falls
     * back to `JAVA` when unset, so packs that never set it are unaffected.
     */
    @Test
    fun allTemplatesRunTheQuiltInstallerWithTheInstallerJavaOverride() {
        val expectations = mapOf(
            "default_template.sh" to listOf("runInstallerJavaCommand()", "\${JAVA_INSTALLER:-\$JAVA}", "runInstallerJavaCommand \"-jar quilt-installer.jar"),
            "default_template.fish" to listOf("function runInstallerJavaCommand", "JAVA_INSTALLER", "runInstallerJavaCommand \"-jar quilt-installer.jar"),
            "default_template.ps1" to listOf("RunInstallerJavaCommand", "\$ExternalVariables['JAVA_INSTALLER']", "RunInstallerJavaCommand \"-jar quilt-installer.jar")
        )
        for ((name, needles) in expectations) {
            val body = template(name)
            for (needle in needles) {
                Assertions.assertTrue(body.contains(needle), "$name must contain `$needle`")
            }
            // The Quilt install must not silently regress to the server's Java.
            Assertions.assertFalse(
                body.contains("runJavaCommand \"-jar quilt-installer.jar") ||
                    body.contains("RunJavaCommand \"-jar quilt-installer.jar"),
                "$name must not run the Quilt installer with the server's JAVA"
            )
        }
    }

    /**
     * Syntax-checks the fish templates with `fish -n` when a fish interpreter is available, so a broken
     * edit is caught without needing the container matrix. Skipped (not failed) where fish is absent,
     * which is the normal case on CI and most dev machines.
     */
    @Test
    fun fishTemplatesAreSyntacticallyValidWhenFishIsAvailable() {
        // `abort` returns Nothing-like, so `fish` is non-null below without a !! assertion.
        val fish = which("fish") ?: Assumptions.abort("fish not installed — syntax check skipped")

        for (name in listOf("default_template.fish", "default_java_template.fish")) {
            val temp = File.createTempFile("spc-template-", ".fish").apply {
                writeText(template(name))
                deleteOnExit()
            }
            val process = ProcessBuilder(fish.absolutePath, "-n", temp.absolutePath)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exit = process.waitFor()
            Assertions.assertEquals(0, exit, "`fish -n` rejected $name:\n$output")
        }
    }

    /**
     * Every template must settle the Fabric launcher from **what is on disk** before it asks the network.
     *
     * The checks that follow ask `meta.fabricmc.net` whether Fabric supports this Minecraft version, and Fabric's
     * branch crashes on the *negative* form (`!= 200`) — so a request that simply could not be made reads as
     * "Fabric does not support this version". A complete, ready-to-run pack therefore refused to start with no
     * internet, and in the grinder (whose mod-boots run `--network none` by design) *every* Fabric boot aborted
     * before the mod was ever loaded: 103 wasted boots in one morning, and 22 Minecraft versions wrongly
     * concluded to be unsupported. Quilt and LegacyFabric were never affected because they crash on the positive
     * form (`== "[]"`), which an unreachable network cannot produce.
     */
    @Test
    fun allTemplatesUseAnAlreadyInstalledFabricLauncherBeforeCheckingTheNetwork() {
        val expectations = mapOf(
            "default_template.sh" to listOf("""if [[ -s "fabric-server-launcher.jar" ]]; then""", "FABRIC_AVAILABLE=\"\$(curl"),
            "default_template.fish" to listOf("""if test -s "fabric-server-launcher.jar"""", "set -g FABRIC_AVAILABLE (curl"),
            "default_template.ps1" to listOf("""if (Test-Path -Path 'fabric-server-launcher.jar' -PathType Leaf)""", "ImprovedFabricLauncherAvailable = [int]")
        )
        expectations.forEach { (name, markers) ->
            val text = template(name)
            val (presentCheck, networkProbe) = markers
            val presentAt = text.indexOf(presentCheck)
            val probeAt = text.indexOf(networkProbe)
            Assertions.assertTrue(presentAt >= 0, "$name does not check for an existing fabric-server-launcher.jar")
            Assertions.assertTrue(probeAt >= 0, "$name: the network probe marker is stale, update this test")
            Assertions.assertTrue(
                presentAt < probeAt,
                "$name asks the network before looking at the launcher jar it already has — an offline pack cannot boot"
            )
        }
    }

    /**
     * **Executes** the bash template's `setupFabric` on an offline pack that already has its launcher jar, and
     * asserts the function still produces a runnable command. Ordering alone is not enough: the first version of
     * the offline short-circuit above `return`ed as soon as it found the jar — jumping over the
     * `SERVER_RUN_COMMAND=...` assignment at the end of the function. The pack then launched
     * `java -Dlog4j2... do_not_manually_edit` (the placeholder) and died with "Could not find or load main class",
     * *past* every ordering assertion. Only running the function catches that, so this test runs it.
     *
     * The stubs make network use fatal rather than merely unnecessary: `commandAvailable` denies curl/wget and any
     * download or install call exits non-zero, so an offline pack that reaches for the network fails loudly here.
     */
    @Test
    fun theBashTemplateStillBuildsARunCommandWhenTheFabricLauncherIsAlreadyInstalled() {
        val bash = which("bash") ?: Assumptions.abort("bash not installed — offline Fabric execution check skipped")
        val packDir = File.createTempFile("spc-fabric-offline-", "-pack").apply { delete(); mkdirs() }
        // A non-empty jar: the template's `-s` test requires size, not mere existence.
        File(packDir, "fabric-server-launcher.jar").writeBytes(ByteArray(64))

        val harness = File(packDir, "harness.sh")
        harness.writeText(
            """
            commandAvailable() { return 1; }
            crashServer() { echo "CRASHED: ${'$'}1"; exit 3; }
            downloadIfNotExist() { echo "NETWORK: download attempted"; exit 4; }
            runJavaCommand() { echo "NETWORK: installer run"; exit 5; }
            JAVA_ARGS="-Xmx4G"
            MINECRAFT_VERSION="1.20.1"
            MODLOADER_VERSION="0.16.9"
            FABRIC_INSTALLER_VERSION="1.0.1"
            LAUNCHER_JAR_LOCATION="do_not_manually_edit"
            SERVER_RUN_COMMAND="do_not_manually_edit"
            ${extractShellFunction("default_template.sh", "setupFabric")}
            setupFabric
            echo "RESULT=${'$'}{SERVER_RUN_COMMAND}"
            """.trimIndent()
        )

        val process = ProcessBuilder(bash.absolutePath, harness.absolutePath)
            .directory(packDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exit = process.waitFor()

        Assertions.assertEquals(0, exit, "setupFabric failed on an offline pack that has its launcher:\n$output")
        Assertions.assertTrue(
            output.contains("RESULT=-Xmx4G -jar fabric-server-launcher.jar nogui"),
            "the offline path must still assemble the run command, was:\n$output"
        )
        Assertions.assertFalse(
            output.contains("do_not_manually_edit"),
            "an unset placeholder reached the run command — the offline branch skipped the assignment:\n$output"
        )
        packDir.deleteRecursively()
    }

    /**
     * **Executes** the bash template's run-loop and asserts it exits with the *server's* status.
     *
     * The loop used to end in an unconditional `exit 0`, throwing the server's exit status away — so a modded server
     * that crashed on startup looked, to anything reading the script's exit code, exactly like a clean shutdown.
     * For the grinder that erased the single decisive signal in its whole confidence model: `BootLogClassifier` maps
     * a `0` exit without a ready-line to INCONCLUSIVE, so **CRASHED could never be observed and no verdict could
     * ever reach HIGH**. Measured 2026-07-30: 517 verdicts over 3.5 h, zero HIGH, while a boot log sat there with
     * `NoClassDefFoundError: net/minecraft/client/Minecraft` in it. It matters for ordinary users too — `systemd`,
     * Docker restart policies and CI all read the exit code to decide whether the server failed.
     */
    @Test
    fun theBashTemplatesRunLoopExitsWithTheServersStatus() {
        val bash = which("bash") ?: Assumptions.abort("bash not installed — run-loop execution check skipped")

        for (serverStatus in listOf(0, 1, 137)) {
            val harness = File.createTempFile("spc-runloop-", ".sh").apply { deleteOnExit() }
            harness.writeText(
                """
                runJavaCommand() { return $serverStatus; }
                pause() { :; }
                SKIP_JAVA_CHECK="false"
                RESTART="false"
                WAIT_FOR_USER_INPUT="false"
                ADDITIONAL_ARGS=""
                SERVER_RUN_COMMAND="-jar server.jar nogui"
                ${extractShellBlock("default_template.sh", "while true", "done")}
                """.trimIndent()
            )

            val process = ProcessBuilder(bash.absolutePath, harness.absolutePath).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exit = process.waitFor()

            Assertions.assertEquals(
                serverStatus,
                exit,
                "the run loop must exit with the server's status ($serverStatus), not swallow it. Output:\n$output"
            )
        }
    }

    /**
     * **Executes** the bash template's `setupForge` across both Minecraft versioning schemes and asserts each one
     * picks the right launcher era.
     *
     * Forge changed how a server is launched: up to Minecraft 1.16 the installer produced a runnable `forge.jar`,
     * from 1.17 onwards it produces `libraries/…/unix_args.txt` consumed via `@user_jvm_args.txt`. The template
     * chooses between them, and choosing wrong is not a subtle failure — the legacy path on a modern Minecraft dies
     * with `Error: Unable to access jarfile forge.jar` before the server starts, so the mod under test is never
     * exercised at all.
     *
     * **The versions below are the point.** The era test may not read the Minecraft *minor* component in isolation,
     * because that is only meaningful under the `1.x` scheme: `26.2` has minor `2`, which looks like the 1.2 era.
     * Measured 2026-07-30 in the grinder: **24 boot logs, every one of them Forge**, never started the server for
     * exactly this reason, making Forge coverage on current Minecraft effectively zero.
     */
    @Test
    fun theBashTemplateChoosesTheForgeLauncherEraForBothVersioningSchemes() {
        val bash = which("bash") ?: Assumptions.abort("bash not installed — Forge era execution check skipped")

        // Minecraft version to whether the *legacy* runnable forge.jar is the correct launcher.
        val expectations = mapOf(
            "1.12.2" to true,
            "1.16.5" to true,
            "1.17.1" to false,
            "1.20.1" to false,
            "1.21.1" to false,
            // The newer YY.x scheme: these are modern Forge, whatever their minor component looks like.
            "26.1.2" to false,
            "26.2" to false
        )

        for ((minecraftVersion, legacyExpected) in expectations) {
            val packDir = File.createTempFile("spc-forge-era-", "-pack").apply { delete(); mkdirs() }
            val harness = File(packDir, "harness.sh")
            harness.writeText(
                """
                downloadIfNotExist() { echo "false"; }
                runJavaCommand() { :; }
                refreshServerJar() { :; }
                crashServer() { echo "CRASHED: ${'$'}1"; exit 3; }
                JAVA_ARGS="-Xmx4G"
                USE_SSJ="true"
                SSJ_FORGE_ARGS=""
                MINECRAFT_VERSION="$minecraftVersion"
                MODLOADER_VERSION="47.4.22"
                LAUNCHER_JAR_LOCATION="do_not_manually_edit"
                SERVER_RUN_COMMAND="do_not_manually_edit"
                IFS="." read -ra SEMANTICS <<<"${'$'}{MINECRAFT_VERSION}"
                ${extractShellFunction("default_template.sh", "setupForge")}
                setupForge
                echo "RESULT=${'$'}{SERVER_RUN_COMMAND}"
                """.trimIndent()
            )

            val process = ProcessBuilder(bash.absolutePath, harness.absolutePath)
                .directory(packDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exit = process.waitFor()
            packDir.deleteRecursively()

            Assertions.assertEquals(0, exit, "setupForge failed for Minecraft $minecraftVersion:\n$output")
            val runCommand = output.lines().firstOrNull { it.startsWith("RESULT=") }
                ?: Assertions.fail("no run command produced for Minecraft $minecraftVersion:\n$output")
            val choseLegacy = runCommand.contains("-jar forge.jar")

            Assertions.assertEquals(
                legacyExpected,
                choseLegacy,
                "Minecraft $minecraftVersion picked the ${if (choseLegacy) "legacy forge.jar" else "modern @user_jvm_args"} " +
                    "launcher; expected the ${if (legacyExpected) "legacy" else "modern"} one. Run command: $runCommand"
            )
        }
    }

    /**
     * **Executes** the bash template's `setupNeoForge` and asserts which installer coordinate it passes, across both
     * Minecraft versioning schemes.
     *
     * NeoForge's first releases — for Minecraft 1.20 and 1.20.1 only — were published under the legacy
     * `net/neoforged/forge/` artifact group and have to be installed by URL; everything later is installed by bare
     * version. The template distinguishes them, and like the Forge era test above it must not read the *minor*
     * component in isolation: under the `YY.x` scheme a future Minecraft `26.20` would match "minor is 20" and be
     * sent at a 1.20-era URL that does not exist for it.
     *
     * This case is **latent** — no such Minecraft has shipped — so the test documents the rule rather than a
     * live failure. It is here because this is the second instance of the same assumption found in one sweep, and an
     * unreachable bug is still cheaper to close now than to rediscover when Mojang makes it reachable.
     */
    @Test
    fun theBashTemplateChoosesTheNeoForgeInstallerCoordinateForBothVersioningSchemes() {
        val bash = which("bash") ?: Assumptions.abort("bash not installed — NeoForge coordinate check skipped")

        // Minecraft version to whether the legacy net/neoforged/forge/ URL is the correct installer coordinate.
        val expectations = mapOf(
            "1.20" to true,
            "1.20.1" to true,
            "1.20.2" to false,
            "1.21.1" to false,
            "26.2" to false,
            // The latent cases: a YY.20 Minecraft must not be mistaken for the 1.20 era.
            "26.20" to false,
            "26.20.1" to false
        )

        for ((minecraftVersion, legacyUrlExpected) in expectations) {
            val packDir = File.createTempFile("spc-neoforge-coord-", "-pack").apply { delete(); mkdirs() }
            val harness = File(packDir, "harness.sh")
            harness.writeText(
                """
                refreshServerJar() { :; }
                JAVA_ARGS="-Xmx4G"
                MINECRAFT_VERSION="$minecraftVersion"
                MODLOADER_VERSION="21.1.247"
                SERVER_RUN_COMMAND="do_not_manually_edit"
                IFS="." read -ra SEMANTICS <<<"${'$'}{MINECRAFT_VERSION}"
                ${extractShellFunction("default_template.sh", "setupNeoForge")}
                setupNeoForge
                echo "RESULT=${'$'}{SERVER_RUN_COMMAND}"
                """.trimIndent()
            )

            val process = ProcessBuilder(bash.absolutePath, harness.absolutePath)
                .directory(packDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exit = process.waitFor()
            packDir.deleteRecursively()

            Assertions.assertEquals(0, exit, "setupNeoForge failed for Minecraft $minecraftVersion:\n$output")
            val runCommand = output.lines().firstOrNull { it.startsWith("RESULT=") }
                ?: Assertions.fail("no run command produced for Minecraft $minecraftVersion:\n$output")
            val choseLegacyUrl = runCommand.contains("net/neoforged/forge/")

            Assertions.assertEquals(
                legacyUrlExpected,
                choseLegacyUrl,
                "Minecraft $minecraftVersion was sent at the ${if (choseLegacyUrl) "legacy 1.20-era URL" else "bare version"}; " +
                    "expected the ${if (legacyUrlExpected) "legacy URL" else "bare version"}. Run command: $runCommand"
            )
        }
    }

    /**
     * Cut a block out of a shell template, from the line equal to [startsWith] to the next line equal to [endsWith]
     * at column 0. Used for the run-loop, which is top-level script text rather than a function.
     */
    private fun extractShellBlock(template: String, startsWith: String, endsWith: String): String {
        val lines = template(template).lines()
        val start = lines.indexOfFirst { it == startsWith }
        Assertions.assertTrue(start >= 0, "template $template has no line `$startsWith` — update this test")
        val end = lines.drop(start + 1).indexOfFirst { it == endsWith }
        Assertions.assertTrue(end >= 0, "block starting at `$startsWith` in $template is not closed by `$endsWith`")
        return lines.subList(start, start + end + 2).joinToString("\n")
    }

    /**
     * Cut one `name() { ... }` function out of a shell template so it can be sourced in isolation. Matches the
     * closing brace in column 0, which is how the shipped templates format their function bodies.
     */
    private fun extractShellFunction(template: String, name: String): String {
        val lines = template(template).lines()
        val start = lines.indexOfFirst { it.startsWith("$name()") }
        Assertions.assertTrue(start >= 0, "template $template has no function `$name` — update this test")
        val end = lines.drop(start + 1).indexOfFirst { it == "}" }
        Assertions.assertTrue(end >= 0, "function `$name` in $template is not closed by a brace in column 0")
        return lines.subList(start, start + end + 2).joinToString("\n")
    }

    /** Locate an executable on `PATH`, or `null` when it is not installed. */
    private fun which(executable: String): File? =
        System.getenv("PATH")?.split(File.pathSeparator)
            ?.map { File(it, executable) }
            ?.firstOrNull { it.canExecute() }
}
