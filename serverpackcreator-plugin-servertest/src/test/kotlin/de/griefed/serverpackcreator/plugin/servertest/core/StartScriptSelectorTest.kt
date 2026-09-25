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
package de.griefed.serverpackcreator.plugin.servertest.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins which script a pack is launched with, and with what argv.
 *
 * The answers come from the pack's own `HOW-TO-RUN.md` rather than from taste: `bash start.sh` on Linux and
 * macOS, `start.bat` on Windows — the shipped shim whose entire job is to run `start.ps1` without the user
 * changing their ExecutionPolicy. Getting this wrong is not a crash but a hang or a cryptic shell error, so
 * it is worth pinning rather than eyeballing.
 *
 * Every case is exercised for **both** platforms regardless of the host, because a selector that silently
 * agrees with whatever machine ran the suite is the one defect this cannot afford.
 */
internal class StartScriptSelectorTest {

    /** A generated pack always carries every template's script, so the realistic fixture carries them all. */
    private fun packWithAllScripts(directory: File): File = directory.apply {
        for (script in listOf("start.sh", "start.bat", "start.ps1", "start.fish")) {
            File(this, script).writeText("#placeholder\n")
        }
    }

    /** Linux and macOS run the pack through bash, with the script named relatively so the cwd decides. */
    @Test
    fun posixRunsStartShThroughBash(@TempDir packDir: File) {
        val selection = StartScriptSelector.selectFor(packWithAllScripts(packDir), Platform.POSIX)
        Assertions.assertTrue(selection is StartScriptSelection.Available, "A pack with start.sh must be launchable.")
        val available = selection as StartScriptSelection.Available
        Assertions.assertEquals(listOf("bash", "start.sh"), available.command)
        Assertions.assertEquals(File(packDir, "start.sh"), available.script)
    }

    /**
     * Windows runs the batch shim, not PowerShell directly. `start.bat` exists precisely so a user does not
     * have to change their system's ExecutionPolicy, and the pack's own HOW-TO-RUN.md says to prefer it.
     */
    @Test
    fun windowsRunsStartBatThroughCmd(@TempDir packDir: File) {
        val selection = StartScriptSelector.selectFor(packWithAllScripts(packDir), Platform.WINDOWS)
        val available = Assertions.assertInstanceOf(StartScriptSelection.Available::class.java, selection)
        Assertions.assertEquals(listOf("cmd", "/c", "start.bat"), available.command)
    }

    /**
     * A pack generated with `bat` removed from the start-script templates still has `start.ps1`, so Windows
     * falls back to invoking PowerShell the way the shim would have. `-NoProfile` because a user profile that
     * writes to the console would land in the server log, `-ExecutionPolicy Bypass` because that is the whole
     * reason the shim exists.
     */
    @Test
    fun windowsFallsBackToPowerShellWhenTheShimIsAbsent(@TempDir packDir: File) {
        packWithAllScripts(packDir)
        File(packDir, "start.bat").delete()
        val selection = StartScriptSelector.selectFor(packDir, Platform.WINDOWS)
        val available = Assertions.assertInstanceOf(StartScriptSelection.Available::class.java, selection)
        Assertions.assertEquals(
            listOf("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "start.ps1"),
            available.command
        )
    }

    /**
     * POSIX does **not** fall back to `start.fish`. bash is on every Linux and macOS install; fish is a
     * deliberate user choice, so invoking it would swap a clear "no script" message for `fish: command not
     * found` at launch time — a failure that reads as the pack being broken rather than the shell missing.
     */
    @Test
    fun posixDoesNotFallBackToFish(@TempDir packDir: File) {
        packWithAllScripts(packDir)
        File(packDir, "start.sh").delete()
        val selection = StartScriptSelector.selectFor(packDir, Platform.POSIX)
        val missing = Assertions.assertInstanceOf(StartScriptSelection.Missing::class.java, selection)
        Assertions.assertTrue(
            missing.reason.contains("start.sh"),
            "The reason must name the file that is missing, but was: ${missing.reason}"
        )
    }

    /** An empty directory is not launchable on either platform, and says which script it wanted. */
    @Test
    fun aPackWithNoScriptsIsNotLaunchable(@TempDir packDir: File) {
        for (platform in Platform.entries) {
            val missing = Assertions.assertInstanceOf(
                StartScriptSelection.Missing::class.java,
                StartScriptSelector.selectFor(packDir, platform),
                "A pack with no start scripts must not be launchable on $platform."
            )
            Assertions.assertTrue(missing.reason.isNotBlank(), "A blocked pack must say why.")
        }
    }

    /** A directory entry with the right name but the wrong kind is not a script. */
    @Test
    fun aDirectoryNamedLikeAScriptIsNotAScript(@TempDir packDir: File) {
        File(packDir, "start.sh").mkdirs()
        Assertions.assertInstanceOf(
            StartScriptSelection.Missing::class.java,
            StartScriptSelector.selectFor(packDir, Platform.POSIX)
        )
    }

    /** The host's own family is read from `os.name`, the only thing that distinguishes the two branches. */
    @Test
    fun platformIsDetectedFromOsName() {
        Assertions.assertEquals(Platform.WINDOWS, Platform.of("Windows 11"))
        Assertions.assertEquals(Platform.WINDOWS, Platform.of("windows server 2022"))
        Assertions.assertEquals(Platform.POSIX, Platform.of("Mac OS X"))
        Assertions.assertEquals(Platform.POSIX, Platform.of("Linux"))
    }
}
