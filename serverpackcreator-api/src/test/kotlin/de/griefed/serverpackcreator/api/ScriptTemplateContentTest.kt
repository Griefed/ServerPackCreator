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
 * Guards the **shipped** start-script templates at the source level, without a container. The grinder's
 * `ScriptTemplateMatrixIT` actually boots them, but it is gated behind a live Docker daemon + a built
 * image, so it never runs in CI — these assertions do, and they pin the fixes that IT paid to discover.
 *
 * Whether the fish and PowerShell templates *parse at all* lives in [ShellTemplateSyntaxTest], which asks
 * the real interpreters rather than reading the source.
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
     * asserts the function still produces a runnable command. Ordering alone is not enough: an offline
     * short-circuit that `return`s as soon as it finds the jar jumps over the `SERVER_RUN_COMMAND=...`
     * assignment at the end of the function, and the pack then launches
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
     * The loop must propagate the server's exit status. An unconditional `exit 0` at the end throws it away, and a
     * modded server that crashed on startup then looks, to anything reading the script's exit code, exactly like a
     * clean shutdown. For the grinder that erases the single decisive signal in its whole confidence model:
     * `BootLogClassifier` maps a `0` exit without a ready-line to INCONCLUSIVE, so **CRASHED can never be observed
     * and no verdict can reach HIGH**. It matters for ordinary users too — `systemd`, Docker restart policies and
     * CI all read the exit code to decide whether the server failed.
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
     * Reading it in isolation sends every Forge boot on current Minecraft down the legacy launcher path, where the
     * server never starts at all.
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
     * **Executes** the bash template's `setupForge` across Minecraft versions and asserts which of the two modern
     * launch paths it picks — the ServerStarterJar, or Forge's own argfile.
     *
     * Forge's installer produces one of two argfiles, and only one of them the ServerStarterJar can start:
     *
     * | Minecraft | argfile | ServerStarterJar |
     * |---|---|---|
     * | 1.17 – 1.20.1 | `-p <module path>`, cpw `securejarhandler` | works — cpw's loader falls back to the platform classloader |
     * | **1.20.2** | `-p <module path>`, Forge `securemodules` | **dies** — `Could not find parent layer for module` |
     * | 1.20.3 onwards | `-jar forge-<ver>-shim.jar` | works — the starter jar takes its own "jar mode" |
     *
     * Measured against real installs: Forge `1.20.2-48.1.0` on Temurin 17 dies at
     * `SecureModuleClassLoader.<init>` under the starter jar and reaches `Done (5.183s)! For help` from its
     * argfile, while `1.21.1-52.1.0` reaches `Done (6.593s)!` *through* the starter jar. 1.20.2's install carries
     * no shim jar and its argfile opens `-p … --add-modules ALL-MODULE-PATH`; 1.20.3's and 1.21.1's do carry one.
     *
     * **The versions below are the point, for the same reason as the launcher-era test above.** The rule may not
     * read minor+patch in isolation: `26.20.2` has minor `20` and patch `2` and is not 1.20.2. Getting that wrong
     * costs the hosting-company compatibility the starter jar exists to provide, on every modern pack.
     */
    @Test
    fun theBashTemplateBypassesTheStarterJarOnlyWhereForgeCannotBeLaunchedWithIt() {
        val bash = which("bash") ?: Assumptions.abort("bash not installed — Forge launch-path check skipped")

        // Minecraft version to whether the ServerStarterJar may be used. Java is fixed at 17 throughout, so the
        // Security-Manager branch is never what decides here.
        val expectations = mapOf(
            "1.17.1" to true,
            "1.19.2" to true,
            "1.20" to true,
            "1.20.1" to true,
            "1.20.2" to false,
            "1.20.3" to false,
            "1.20.4" to true,
            "1.21.1" to true,
            // The YY.x scheme. `26.2`'s minor is 2 and `26.20.2` matches 1.20.2 component for component below
            // the major — neither is the affected era.
            "26.2" to true,
            "26.20.2" to true,
            // A component that is not a number at all (a snapshot-shaped version). It must take the bypass: the
            // argfile path works for every Forge from 1.17 on, while the starter jar does not, so a version we
            // cannot read must not be sent down the route that has a known failure.
            "26w05a" to false
        )

        for ((minecraftVersion, starterJarExpected) in expectations) {
            val packDir = File.createTempFile("spc-forge-ssj-", "-pack").apply { delete(); mkdirs() }
            val harness = File(packDir, "harness.sh")
            harness.writeText(
                """
                downloadIfNotExist() { echo "false"; }
                runJavaCommand() { :; }
                refreshServerJar() { :; }
                crashServer() { echo "CRASHED: ${'$'}1"; exit 3; }
                JAVA_ARGS="-Xmx4G"
                JAVA_VERSION="17"
                USE_SSJ="true"
                SSJ_FORGE_ARGS="-Djava.security.manager=allow"
                MINECRAFT_VERSION="$minecraftVersion"
                MODLOADER_VERSION="48.1.0"
                LAUNCHER_JAR_LOCATION="do_not_manually_edit"
                SERVER_RUN_COMMAND="do_not_manually_edit"
                IFS="." read -ra SEMANTICS <<<"${'$'}{MINECRAFT_VERSION}"
                ${extractShellFunction("default_template.sh", "forgeNeedsItsOwnArgfile")}
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
            val choseStarterJar = runCommand.contains("-jar server.jar")

            Assertions.assertEquals(
                starterJarExpected,
                choseStarterJar,
                "Minecraft $minecraftVersion was launched via ${if (choseStarterJar) "the ServerStarterJar" else "Forge's argfile"}; " +
                    "expected ${if (starterJarExpected) "the ServerStarterJar" else "Forge's argfile"}. Run command: $runCommand"
            )
            // Reading an unparseable version must stay silent, not shout bash arithmetic at the operator.
            Assertions.assertFalse(
                output.contains("value too great for base") || output.contains("integer expression expected"),
                "Minecraft $minecraftVersion made the shell complain about arithmetic:\n$output"
            )
            // The argfile path must name the argfile it launches from, not merely avoid the starter jar.
            if (!starterJarExpected) {
                Assertions.assertTrue(
                    runCommand.contains("@libraries/net/minecraftforge/forge/$minecraftVersion-48.1.0/unix_args.txt"),
                    "the bypass must launch from Forge's own argfile: $runCommand"
                )
            }
        }
    }

    /**
     * All three templates must carry the same Minecraft-1.20.2/1.20.3 bypass, and each must test the **major**
     * component.
     *
     * Only bash can be executed on every machine, so fish and PowerShell get the same source-level treatment
     * [allTemplatesResolveJavaAfterTheChecksAndFailSafeWhenItIsUnknown] gives their Java guard. A template that
     * silently lacks the bypass produces a server pack that cannot start on Minecraft 1.20.2 — the failure this
     * exists to prevent — and one that omits the major test bypasses the starter jar for `26.20.2` as well,
     * quietly dropping the hosting compatibility it provides.
     *
     * Each must also **screen a component before comparing it**, and fail towards the bypass when it cannot be
     * read. Comparing an unreadable component is not harmless: bash prints `value too great for base` at the
     * operator, and PowerShell's `[int]` cast *throws*. Failing towards the bypass is the safe polarity for the
     * same reason the Java guard's is — the argfile path works for every Forge from 1.17 on, so a version nobody
     * can parse must not be handed to the one route with a known failure.
     */
    @Test
    fun allTemplatesBypassTheStarterJarForTheAffectedForgeVersionsAndTestTheMajor() {
        val expectations = mapOf(
            "default_template.sh" to listOf(
                """[[ "${'$'}{SEMANTICS[0]}" =~ ^[0-9]+${'$'} ]] || return 0""",
                """[[ ${'$'}{SEMANTICS[0]} -eq 1 ]] || return 1""",
                """[[ ${'$'}{SEMANTICS[1]} -eq 20 ]] || return 1""",
                """[[ ${'$'}{SEMANTICS[2]} -eq 2 || ${'$'}{SEMANTICS[2]} -eq 3 ]]"""
            ),
            "default_template.fish" to listOf(
                """if not string match -qr '^[0-9]+${'$'}' -- "${'$'}SEMANTICS[1]"""",
                """test "${'$'}SEMANTICS[1]" -eq 1""",
                """test "${'$'}SEMANTICS[2]" -eq 20""",
                """test "${'$'}SEMANTICS[3]" -eq 2; or test "${'$'}SEMANTICS[3]" -eq 3"""
            ),
            "default_template.ps1" to listOf(
                """if (-Not ([string]${'$'}Semantics[0] -match '^\d+${'$'}'))""",
                """[int]${'$'}Semantics[0] -ne 1""",
                """[int]${'$'}Semantics[1] -ne 20""",
                """[int]${'$'}Semantics[2] -eq 2) -Or ([int]${'$'}Semantics[2] -eq 3"""
            )
        )

        expectations.forEach { (name, required) ->
            val text = template(name)
            required.forEach { fragment ->
                Assertions.assertTrue(
                    text.contains(fragment),
                    "$name is missing part of the Forge 1.20.2/1.20.3 starter-jar bypass. Expected to find:\n  $fragment"
                )
            }
        }
    }

    /**
     * Every template must resolve the Java version **after** the Java-check block, and must fail safe when it
     * cannot.
     *
     * The executing tests below cover bash only, because fish and PowerShell cannot be run on every machine — so
     * these two properties are asserted at source level for all three, which is the same compromise
     * [allTemplatesUseAnAlreadyInstalledFabricLauncherBeforeCheckingTheNetwork] makes.
     *
     * Both halves matter and they fail differently:
     *
     *  - **Ordering.** `JAVA_VERSION` starts as the literal `do_not_manually_edit` and only `getJavaVersion` fills
     *    it in. No `installJava` call-site re-reads it and `install_java.sh` never sets it, so a pack that installs
     *    its own Java reached `setupForge` with no version — which is exactly how
     *    `-Djava.security.manager=allow` reached a Java 25 VM and stopped it from starting.
     *  - **Fail-safe polarity.** The guard must be "not numeric **OR** >= 24", never "numeric **AND** >= 24". An
     *    unresolved version cannot rule out Java 24+, so it must take the self-install path rather than the one
     *    that passes a flag which is fatal there. A guard inverted the wrong way still parses, still runs, and
     *    silently reinstates the crash — no syntax check can catch it, which is why it is pinned here.
     */
    @Test
    fun allTemplatesResolveJavaAfterTheChecksAndFailSafeWhenItIsUnknown() {
        val expectations = mapOf(
            "default_template.sh" to Triple(
                "# Check and warn the user if a 32bit Java-installation is used",
                "getJavaVersion",
                """if [[ ! "${'$'}{JAVA_VERSION}" =~ ^[0-9]+${'$'} ]] || [[ ${'$'}{JAVA_VERSION} -ge 24 ]]; then"""
            ),
            "default_template.fish" to Triple(
                "# Check and warn the user if a 32bit Java-installation is used",
                "getJavaVersion",
                """if not string match -qr '^[0-9]+${'$'}' -- "${'$'}JAVA_VERSION"; or test "${'$'}JAVA_VERSION" -ge 24"""
            ),
            "default_template.ps1" to Triple(
                "# Check and warn the user if a 32bit Java-installation is used",
                "GetJavaVersion",
                """if ((-Not ("${'$'}{JavaVersion}" -match '^\d+${'$'}')) -Or ([int]${'$'}{JavaVersion} -ge 24))"""
            )
        )

        expectations.forEach { (name, markers) ->
            val (afterChecksMarker, resolveCall, failSafeGuard) = markers
            val installCall = if (name.endsWith(".ps1")) "InstallJava" else "installJava"
            val text = template(name)

            // The resolve call must sit AFTER the last installJava — every one of which is inside the
            // Java-check block — and before the 32-bit warning that follows the block. Anchoring on the last
            // install is what makes this bite: searching backwards from the 32-bit marker alone would happily
            // match one of the calls *inside* the block and pass with the post-block call deleted.
            val checksEndAt = text.indexOf(afterChecksMarker)
            Assertions.assertTrue(checksEndAt >= 0, "$name: the end-of-Java-checks marker is stale, update this test")
            val lastInstallAt = text.lastIndexOf(installCall, checksEndAt)
            Assertions.assertTrue(lastInstallAt >= 0, "$name: the installJava marker is stale, update this test")
            val resolveAt = text.indexOf(resolveCall, lastInstallAt + installCall.length)
            Assertions.assertTrue(
                resolveAt in 0..<checksEndAt,
                "$name never calls $resolveCall after the Java-check block, so a pack that installs its own Java " +
                    "reaches setupForge with JAVA_VERSION still unresolved"
            )

            Assertions.assertTrue(
                text.contains(failSafeGuard),
                "$name's Forge/SSJ guard is not the fail-safe form. It must treat an unreadable JAVA_VERSION as " +
                    "'cannot rule out Java 24+' and take the self-install path. Expected to find:\n  $failSafeGuard"
            )
        }
    }

    /**
     * **Executes** `setupForge`'s ServerStarterJar path and asserts that `SSJ_FORGE_ARGS` is dropped on a Java that
     * cannot accept it.
     *
     * SPC's default `SSJ_FORGE_ARGS` is `-Djava.security.manager=allow` (`PackConfig.spcSSJArgsKeyDefaultValue`),
     * which Forge's ServerStarterJar needed on older Java. **JEP 486 removed Security Manager support in Java 24**, so
     * from that release the flag does not merely do nothing — the VM refuses to start:
     *
     * ```
     * Error occurred during initialization of VM
     * java.lang.Error: A command line option has attempted to allow or enable the Security Manager.
     * ```
     *
     * Minecraft 26.x requires Java 25, so passing the flag there means **every modern Forge pack SPC generates dies
     * before Forge loads**. NeoForge, Fabric and Quilt never pass it, which is exactly why they boot.
     */
    @Test
    fun theBashTemplateDropsTheSecurityManagerFlagOnJavaThatRejectsIt() {
        val bash = which("bash") ?: Assumptions.abort("bash not installed — SSJ args check skipped")

        // Java major to whether -Djava.security.manager=allow may still be passed.
        val expectations = mapOf(17 to true, 21 to true, 24 to false, 25 to false)

        for ((javaVersion, flagAllowed) in expectations) {
            val packDir = File.createTempFile("spc-ssj-args-", "-pack").apply { delete(); mkdirs() }
            val harness = File(packDir, "harness.sh")
            harness.writeText(
                """
                downloadIfNotExist() { echo "false"; }
                runJavaCommand() { :; }
                refreshServerJar() { :; }
                crashServer() { echo "CRASHED: ${'$'}1"; exit 3; }
                JAVA_ARGS="-Xmx4G"
                USE_SSJ="true"
                SSJ_FORGE_ARGS="-Djava.security.manager=allow"
                JAVA_VERSION="$javaVersion"
                MINECRAFT_VERSION="26.1.2"
                MODLOADER_VERSION="64.1.0"
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

            Assertions.assertEquals(0, exit, "setupForge failed on Java $javaVersion:\n$output")
            val runCommand = output.lines().firstOrNull { it.startsWith("RESULT=") }
                ?: Assertions.fail("no run command produced on Java $javaVersion:\n$output")
            val passesFlag = runCommand.contains("-Djava.security.manager=allow")

            Assertions.assertEquals(
                flagAllowed,
                passesFlag,
                "on Java $javaVersion the security-manager flag was ${if (passesFlag) "passed" else "dropped"}; " +
                    "expected it to be ${if (flagAllowed) "passed" else "dropped"}. Run command: $runCommand"
            )
        }
    }

    /**
     * **Executes** `setupForge` with `JAVA_VERSION` still at its placeholder, and asserts the flag is *not* passed.
     *
     * This is the case a real user hit, and it is the one the version-keyed guard misses. `JAVA_VERSION` starts life
     * as the literal `do_not_manually_edit` and is only filled in by `getJavaVersion`. None of the three
     * `installJava` call-sites re-read it afterwards, and `install_java.sh` never sets it either — so a pack that
     * installs its own Java reaches `setupForge` with the placeholder still in place, the numeric guard does not
     * match, and the fatal flag is passed anyway:
     *
     * ```
     * Downloading and using Java temurin@25
     * Run Command:  java ... -Djava.security.manager=allow -jar server.jar --installer-force ...
     * Error occurred during initialization of VM
     * java.lang.Error: A command line option has attempted to allow or enable the Security Manager.
     * ```
     *
     * The guard therefore only ever protected users who *already had* the right Java — which is why neither the
     * grinder (it pre-bakes Java and never takes the install path) nor `ScriptTemplateMatrixIT` (same) caught it.
     *
     * Pinned as **fail-safe**, not merely as "re-read the version": an unknown Java version must never take the
     * branch that passes a flag which is fatal on the JVMs it cannot rule out.
     */
    @Test
    fun theBashTemplateDropsTheSecurityManagerFlagWhenTheJavaVersionIsUnknown() {
        val bash = which("bash") ?: Assumptions.abort("bash not installed — SSJ args check skipped")

        // Every shape JAVA_VERSION can carry when nothing has resolved it.
        for (unknownVersion in listOf("do_not_manually_edit", "", "unknown")) {
            val packDir = File.createTempFile("spc-ssj-unknown-", "-pack").apply { delete(); mkdirs() }
            val harness = File(packDir, "harness.sh")
            harness.writeText(
                """
                downloadIfNotExist() { echo "false"; }
                runJavaCommand() { :; }
                refreshServerJar() { :; }
                crashServer() { echo "CRASHED: ${'$'}1"; exit 3; }
                JAVA_ARGS="-Xmx4G"
                USE_SSJ="true"
                SSJ_FORGE_ARGS="-Djava.security.manager=allow"
                JAVA_VERSION="$unknownVersion"
                MINECRAFT_VERSION="1.20.1"
                MODLOADER_VERSION="47.4.22"
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

            Assertions.assertEquals(0, exit, "setupForge failed for JAVA_VERSION='$unknownVersion':\n$output")
            val runCommand = output.lines().firstOrNull { it.startsWith("RESULT=") }
                ?: Assertions.fail("no run command produced for JAVA_VERSION='$unknownVersion':\n$output")

            Assertions.assertFalse(
                runCommand.contains("-Djava.security.manager=allow"),
                "with JAVA_VERSION='$unknownVersion' the security-manager flag was passed. An unresolved Java " +
                    "version must fail safe — it cannot rule out Java 24+, where the flag stops the VM starting. " +
                    "Run command: $runCommand"
            )
        }
    }

    /**
     * **Executes** the bash template's `setupForge` and asserts *who* installs Forge, by Java version.
     *
     * ServerStarterJar runs the Forge installer **in its own JVM** and relies on a `SecurityManager`
     * (`SecurityAccess.wrapNoForceExit`) to swallow the `System.exit(0)` the installer calls when it finishes.
     * JEP 486 removed Security Manager support in Java 24, and SSJ catches the resulting
     * `UnsupportedOperationException` *silently* — so from Java 24 on, the installer's exit terminates the whole
     * process. Measured on Minecraft 26.2 (Java 25): the pack installs, prints "The server installed
     * successfully", exits **0**, and never launches the server — no world, no ready-line. Booting the same pack a
     * second time works, because the install is then already there and SSJ only has to launch.
     *
     * Exit code 0 is what makes this dangerous: nothing downstream can tell it from a clean shutdown, and the
     * grinder never sees it at all because it pre-bakes the install and boots from cache. Only a user starting a
     * fresh Forge pack hits it, and to them the server simply does nothing.
     *
     * So on Java that cannot trap the exit, the template must install Forge itself and launch through the
     * argfile the installer produces, never handing the install to SSJ. Below Java 24 the SSJ path is left exactly
     * as it was — that combination demonstrably works.
     */
    @Test
    fun theBashTemplateInstallsForgeItselfWhenSSJCannotTrapTheInstallersExit() {
        val bash = which("bash") ?: Assumptions.abort("bash not installed — Forge install-ownership check skipped")

        // Java major to whether SSJ may be trusted with the install (it can trap System.exit below 24).
        val expectations = mapOf(17 to true, 21 to true, 24 to false, 25 to false)

        for ((javaVersion, ssjMayInstall) in expectations) {
            val packDir = File.createTempFile("spc-forge-install-", "-pack").apply { delete(); mkdirs() }
            val harness = File(packDir, "harness.sh")
            harness.writeText(
                """
                # "true" == the artifact was absent and has been downloaded, which is what triggers an install.
                downloadIfNotExist() { echo "true"; }
                runJavaCommand() { echo "JAVACMD=${'$'}1"; }
                refreshServerJar() { echo "REFRESH_SSJ"; }
                crashServer() { echo "CRASHED: ${'$'}1"; exit 3; }
                JAVA_ARGS="-Xmx4G"
                USE_SSJ="true"
                SSJ_FORGE_ARGS="-Djava.security.manager=allow"
                JAVA_VERSION="$javaVersion"
                MINECRAFT_VERSION="26.2"
                MODLOADER_VERSION="65.1.0"
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

            Assertions.assertEquals(0, exit, "setupForge failed on Java $javaVersion:\n$output")
            val runCommand = output.lines().firstOrNull { it.startsWith("RESULT=") }
                ?: Assertions.fail("no run command produced on Java $javaVersion:\n$output")
            val installedUpFront = output.lines().any { it.contains("JAVACMD=") && it.contains("--installServer") }
            val handsInstallToSsj = runCommand.contains("--installer")

            if (ssjMayInstall) {
                Assertions.assertTrue(
                    handsInstallToSsj,
                    "on Java $javaVersion SSJ can trap the installer's exit, so the install must stay with it — " +
                        "this path works today and must not change. Run command: $runCommand"
                )
            } else {
                Assertions.assertFalse(
                    handsInstallToSsj,
                    "on Java $javaVersion SSJ cannot trap the Forge installer's System.exit, so handing it the " +
                        "install means the pack installs and then exits 0 without ever launching. " +
                        "Run command: $runCommand"
                )
                Assertions.assertTrue(
                    installedUpFront,
                    "on Java $javaVersion the template must run the Forge installer itself before launching; " +
                        "nothing invoked --installServer:\n$output"
                )
                Assertions.assertTrue(
                    runCommand.contains("unix_args.txt"),
                    "on Java $javaVersion the launch must go through the argfile the installer produced. " +
                        "Run command: $runCommand"
                )
            }
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
     * **Executes** the bash template's Java-check block with `SKIP_JAVA_CHECK=true`, and asserts the version is
     * still *resolved*.
     *
     * Skipping the checks must not mean flying blind. `variables.txt` documents the setting as disabling "the
     * compatibility check of your Minecraft version and the provided Java version, as well as the automatic
     * installation" — it says nothing about *reading* the version, and reading it is what decides whether the
     * Forge/SSJ path may pass `-Djava.security.manager=allow`.
     *
     * This matters most for the exact user the setting is aimed at. `variables.txt` tells anyone pointing `JAVA`
     * at a custom path to set `SKIP_JAVA_CHECK=true`, so that user has a deliberately chosen, working Java —
     * and resolving it is what lets them keep the ServerStarterJar path on Java 17 or 21 instead of being
     * pushed onto the self-install path with everybody else.
     *
     * The unresolvable case is asserted too: it must yield a non-numeric version, which the fail-safe guard then
     * routes away from the fatal flag.
     */
    @Test
    fun theBashTemplateResolvesTheJavaVersionEvenWhenChecksAreSkipped() {
        val bash = which("bash") ?: Assumptions.abort("bash not installed — Java-resolve check skipped")

        val text = template("default_template.sh")
        val blockStart = text.indexOf("# If Java checks are desired")
        val blockEnd = text.indexOf("# Check and warn the user if a 32bit Java-installation is used")
        Assertions.assertTrue(blockStart in 0..<blockEnd, "the Java-check block markers are stale, update this test")
        val javaCheckBlock = text.substring(blockStart, blockEnd)

        // A readable Java must be read; an unreadable one must come back non-numeric so the guard fails safe.
        val cases = mapOf("17.0.1" to "17", null to "")

        for ((fakeVersion, expected) in cases) {
            val packDir = File.createTempFile("spc-skipcheck-", "-pack").apply { delete(); mkdirs() }
            val fakeJava = File(packDir, "fake-java")
            if (fakeVersion != null) {
                fakeJava.writeText("#!/bin/sh\necho 'openjdk version \"$fakeVersion\" 2021-10-19' 1>&2\n")
                fakeJava.setExecutable(true)
            }

            val harness = File(packDir, "harness.sh")
            harness.writeText(
                """
                installJava() { echo "INSTALL CALLED"; }
                crashServer() { echo "CRASHED: ${'$'}1"; exit 3; }
                commandAvailable() { command -v "${'$'}1" > /dev/null 2>&1; }
                ${extractShellFunction("default_template.sh", "getJavaVersion")}
                JAVA="${fakeJava.absolutePath}"
                SKIP_JAVA_CHECK="true"
                RECOMMENDED_JAVA_VERSION="21"
                MINECRAFT_VERSION="1.20.1"
                JAVA_VERSION="do_not_manually_edit"
                IFS="." read -ra SEMANTICS <<<"${'$'}{MINECRAFT_VERSION}"
                $javaCheckBlock
                echo "RESOLVED=${'$'}{JAVA_VERSION}"
                """.trimIndent()
            )

            val process = ProcessBuilder(bash.absolutePath, harness.absolutePath)
                .directory(packDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exit = process.waitFor()
            packDir.deleteRecursively()

            Assertions.assertEquals(0, exit, "the Java-check block failed for version '$fakeVersion':\n$output")
            Assertions.assertFalse(
                output.contains("INSTALL CALLED"),
                "SKIP_JAVA_CHECK=true must still skip the automatic installation, which is what it promises:\n$output"
            )
            val resolved = output.lines().firstOrNull { it.startsWith("RESOLVED=") }?.removePrefix("RESOLVED=")
                ?: Assertions.fail("the block produced no JAVA_VERSION for '$fakeVersion':\n$output")

            Assertions.assertEquals(
                expected, resolved,
                "with SKIP_JAVA_CHECK=true and JAVA reporting '${fakeVersion ?: "nothing"}', JAVA_VERSION resolved " +
                    "to '$resolved'. Skipping the compatibility check must not leave the version unread — it is " +
                    "what decides whether the security-manager flag may be passed."
            )
        }
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
    /**
     * A faithful stand-in for `downloadIfNotExist`: reports the file as present when it is, and makes any
     * actual download fatal. The real function echoes "false" for a file that is already there, which is
     * what steers `setupQuilt` past its install branch — a stub that always downloaded would never
     * reproduce the cached-launcher case at all.
     */
    private val offlineDownloadStub = """
        downloadIfNotExist() {
          if [[ -s "${'$'}{1}" ]]; then echo "${'$'}{1} present." >&2; echo "false"; return 0; fi
          echo "NETWORK: download attempted for ${'$'}{2}" >&2; exit 4
        }
    """.trimIndent()

    /**
     * **Executes** `setupQuilt` on a pack that kept its launcher but lost the Minecraft server jar, and
     * asserts it does not quietly carry on.
     *
     * Observed live: `Modrinth/architectury-api` at Minecraft 1.20.4 died with "Missing game jar
     * at /srv/pack/server.jar" and was scored against the mod. The vanilla jar is fetched only as a side
     * effect of installing the launcher — `--download-server` lives inside the branch that runs when
     * `quilt-server-launch.jar` is absent — so a pack that already has the launcher and not the jar never
     * gets one. That is a restored backup, a half-cleaned directory, or the grinder's cached loader
     * install, and Quilt's launcher refuses to start without it.
     *
     * Asserted as "must not exit 0 having done nothing", not as a fixed message: what matters is that the
     * gap is noticed. The stubs make any network use fatal, so noticing shows up as a non-zero exit.
     */
    @Test
    fun theBashTemplateNoticesAQuiltPackWithNoMinecraftServerJar() {
        val bash = which("bash") ?: Assumptions.abort("bash not installed — offline Quilt execution check skipped")
        val packDir = File.createTempFile("spc-quilt-nojar-", "-pack").apply { delete(); mkdirs() }
        // The launcher is there; the vanilla server jar is not. That is the whole scenario.
        File(packDir, "quilt-server-launch.jar").writeBytes(ByteArray(64))

        val harness = File(packDir, "harness.sh")
        harness.writeText(
            """
            commandAvailable() { return 1; }
            crashServer() { echo "CRASHED: ${'$'}1"; exit 3; }
            $offlineDownloadStub
            runInstallerJavaCommand() { echo "NETWORK: installer run"; exit 5; }
            JAVA_ARGS="-Xmx4G"
            MINECRAFT_VERSION="1.20.4"
            MODLOADER_VERSION="0.31.0-beta.1"
            QUILT_INSTALLER_VERSION="0.15.1"
            LAUNCHER_JAR_LOCATION="do_not_manually_edit"
            SERVER_RUN_COMMAND="do_not_manually_edit"
            ${extractShellFunction("default_template.sh", "setupQuilt")}
            setupQuilt
            echo "RESULT=${'$'}{SERVER_RUN_COMMAND}"
            """.trimIndent()
        )

        val process = ProcessBuilder(bash.absolutePath, harness.absolutePath)
            .directory(packDir).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exit = process.waitFor()
        packDir.deleteRecursively()

        Assertions.assertNotEquals(
            0, exit,
            "setupQuilt finished happily on a pack with no server.jar; the server then dies with " +
                "\"Missing game jar\" and the mod under test gets the blame. Output:\n$output"
        )
    }

    /** The ordinary cached path must keep working offline: launcher and server jar both present. */
    @Test
    fun theBashTemplateStillBuildsARunCommandWhenTheQuiltPackIsComplete() {
        val bash = which("bash") ?: Assumptions.abort("bash not installed — offline Quilt execution check skipped")
        val packDir = File.createTempFile("spc-quilt-offline-", "-pack").apply { delete(); mkdirs() }
        File(packDir, "quilt-server-launch.jar").writeBytes(ByteArray(64))
        File(packDir, "server.jar").writeBytes(ByteArray(64))

        val harness = File(packDir, "harness.sh")
        harness.writeText(
            """
            commandAvailable() { return 1; }
            crashServer() { echo "CRASHED: ${'$'}1"; exit 3; }
            $offlineDownloadStub
            runInstallerJavaCommand() { echo "NETWORK: installer run"; exit 5; }
            JAVA_ARGS="-Xmx4G"
            MINECRAFT_VERSION="1.20.4"
            MODLOADER_VERSION="0.31.0-beta.1"
            QUILT_INSTALLER_VERSION="0.15.1"
            LAUNCHER_JAR_LOCATION="do_not_manually_edit"
            SERVER_RUN_COMMAND="do_not_manually_edit"
            ${extractShellFunction("default_template.sh", "setupQuilt")}
            setupQuilt
            echo "RESULT=${'$'}{SERVER_RUN_COMMAND}"
            """.trimIndent()
        )

        val process = ProcessBuilder(bash.absolutePath, harness.absolutePath)
            .directory(packDir).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exit = process.waitFor()
        packDir.deleteRecursively()

        Assertions.assertEquals(0, exit, "setupQuilt failed on a complete offline pack:\n$output")
        Assertions.assertTrue(
            output.contains("RESULT=-Xmx4G -jar quilt-server-launch.jar nogui"),
            "the offline path must still assemble the run command, was:\n$output"
        )
    }

    /**
     * **Executes** the bash template's signal handling: a `SIGTERM` to the script must reach the *server*, and
     * the server console must keep working while it can.
     *
     * A pack's PID 1 under docker or systemd is this script, and the kernel **discards** a default-action signal
     * aimed at PID 1 — so a script that neither traps `TERM` nor `exec`s the server swallows every stop request
     * and the server is SIGKILLed when the window expires, losing the world save. Measured with `docker stop -t 5`
     * against the two shapes: no trap, 5096 ms and exit 137; trap plus `wait`, 72 ms and exit 0.
     *
     * The stdin half is the trap this fix sets for itself, and it fails silently: a shell with job control off
     * redirects an asynchronous command's stdin from `/dev/null` unless a redirection says otherwise, so
     * backgrounding the server to make it signallable also disconnects the console every operator types `stop`
     * into. Verified under `bash:latest`: `cat & wait` reads nothing, `cat 0<&0 & wait` reads its input.
     */
    @Test
    fun theBashTemplateForwardsATerminationSignalToTheServerAndKeepsItsConsole() {
        val bash = which("bash") ?: Assumptions.abort("bash not installed — signal-forwarding execution check skipped")

        // Stands in for the server: announces what it read from stdin, then waits to be signalled. `sleep & wait`
        // rather than a foreground sleep, so the handler is entered at once instead of after the sleep returns.
        val fakeServer = File.createTempFile("spc-fake-server-", ".sh").apply {
            writeText(
                """
                #!/bin/sh
                trap 'echo CHILD-GOT-TERM; exit 0' TERM
                if read -r line; then echo "CHILD-STDIN:${'$'}line"; else echo "CHILD-STDIN-CLOSED"; fi
                sleep 30 & wait
                """.trimIndent()
            )
            setExecutable(true)
            deleteOnExit()
        }

        val harness = File.createTempFile("spc-signal-", ".sh").apply { deleteOnExit() }
        harness.writeText(
            """
            JAVA="${fakeServer.absolutePath}"
            ${extractShellFunction("default_template.sh", "stopServer")}
            ${extractShellLine("default_template.sh", "trap stopServer")}
            ${extractShellFunction("default_template.sh", "runJavaCommand")}
            runJavaCommand ""
            """.trimIndent()
        )

        // The console goes to a file rather than a pipe. A pipe loses the very lines this test is about: the JDK
        // closes it as the process exits, so the reader throws `Stream closed` on output the handler had already
        // written. The file keeps everything and can be read after the exit.
        val console = File.createTempFile("spc-signal-console-", ".log").apply { deleteOnExit() }
        val process = ProcessBuilder(bash.absolutePath, harness.absolutePath)
            .redirectOutput(console).redirectErrorStream(true).start()
        process.outputStream.use { it.write("say hello\n".toByteArray()) }

        // Wait for the stand-in to have read stdin before signalling, so this asserts the handler and never the
        // race of a signal arriving before the child exists.
        val until = System.currentTimeMillis() + FIXTURE_BUDGET_MILLIS
        while (!console.readText().contains("CHILD-STDIN") && System.currentTimeMillis() < until) {
            Thread.sleep(50)
        }
        Assertions.assertTrue(
            console.readText().contains("CHILD-STDIN:say hello"),
            "the server must still receive its console on stdin; backgrounding it without re-attaching fd 0 " +
                "sends the console to /dev/null. Saw:\n${console.readText()}"
        )

        process.destroy() // SIGTERM on Unix, which is what `docker stop` and `systemctl stop` send.
        val finished = process.waitFor(FIXTURE_BUDGET_MILLIS, TimeUnit.MILLISECONDS)

        Assertions.assertTrue(finished, "the script must act on SIGTERM rather than ignore it. Saw:\n${console.readText()}")
        Assertions.assertTrue(
            console.readText().contains("CHILD-GOT-TERM"),
            "SIGTERM to the script must be forwarded to the server so it can save and exit. " +
                "Saw:\n${console.readText()}"
        )
    }

    /** How long the signal check waits on a child process before calling the host, not the template, the problem. */
    private val FIXTURE_BUDGET_MILLIS = 30_000L

    /**
     * Cut one top-level line out of a shell template, so a harness can execute the real registration rather than
     * a copy of it. Pins the line's existence as a side effect: a template that stops containing it fails here.
     */
    private fun extractShellLine(template: String, startsWith: String): String =
        template(template).lines().firstOrNull { it.trimStart().startsWith(startsWith) }
            ?: Assertions.fail("template $template has no line starting `$startsWith` — update this test")
}
