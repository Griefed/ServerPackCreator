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
