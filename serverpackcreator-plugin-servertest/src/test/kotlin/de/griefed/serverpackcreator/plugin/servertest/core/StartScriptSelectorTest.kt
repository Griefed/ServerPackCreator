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
 * Pins which script a launch uses, now that the **user** picks it.
 *
 * The plugin used to choose, falling back across candidates per platform, and the failure mode had no
 * workaround: a guess landing on a script the host could not run left somebody unable to start a pack with
 * nowhere to say otherwise. So all four are offerable, the platform only supplies the pre-selection, and a
 * pack that does not carry the chosen script is refused by name rather than quietly launched with another.
 */
internal class StartScriptSelectorTest {

    /** A generated pack always carries all four scripts, so the realistic fixture carries them all. */
    private fun packWithAllScripts(directory: File): File = directory.apply {
        for (kind in StartScriptKind.entries) {
            File(this, kind.fileName).writeText("#placeholder\n")
        }
    }

    private fun packAt(directory: File) = LaunchablePack(
        directory = directory,
        name = directory.name,
        minecraftVersion = "1.21",
        modloader = "NeoForge",
        modloaderVersion = "21.0.18",
        scriptsPresent = StartScriptSelector.scriptsIn(directory)
    )

    /** Every kind knows its file and how to run it, and the argv names the script relatively. */
    @Test
    fun eachKindCarriesItsFileAndArgv() {
        Assertions.assertEquals(listOf("bash", "start.sh"), StartScriptKind.SH.command)
        Assertions.assertEquals(listOf("cmd", "/c", "start.bat"), StartScriptKind.BAT.command)
        Assertions.assertEquals(
            listOf("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "start.ps1"),
            StartScriptKind.PS1.command
        )
        Assertions.assertEquals(listOf("fish", "start.fish"), StartScriptKind.FISH.command)

        for (kind in StartScriptKind.entries) {
            Assertions.assertTrue(
                kind.command.contains(kind.fileName),
                "${kind.name}'s argv must name ${kind.fileName} relatively, so the working directory picks the pack."
            )
            Assertions.assertTrue(kind.label.contains(kind.fileName), "${kind.name}'s label must name its file.")
        }
    }

    /** The pre-selection follows the host: the batch shim on Windows, bash everywhere else. */
    @Test
    fun theDefaultFollowsThePlatform() {
        Assertions.assertEquals(StartScriptKind.BAT, StartScriptKind.defaultFor(Platform.WINDOWS))
        Assertions.assertEquals(StartScriptKind.SH, StartScriptKind.defaultFor(Platform.POSIX))
    }

    /** All four are offerable, so a user is never stuck with a script their machine cannot run. */
    @Test
    fun everyScriptKindCanBeChosen(@TempDir packDir: File) {
        packWithAllScripts(packDir)
        val pack = packAt(packDir)

        for (kind in StartScriptKind.entries) {
            val selection = StartScriptSelector.selectFor(pack, kind)
            val available = Assertions.assertInstanceOf(
                StartScriptSelection.Available::class.java,
                selection,
                "${kind.name} must be launchable when the pack carries ${kind.fileName}."
            )
            Assertions.assertEquals(kind.command, available.command)
            Assertions.assertEquals(File(packDir, kind.fileName), available.script)
        }
    }

    /**
     * A pack missing the chosen script is refused by name — **never** silently launched with another.
     *
     * That substitution is precisely what the old platform-fallback did, and it is why this is pinned: the
     * user asked for one script, and running a different one hides what actually happened.
     */
    @Test
    fun aMissingScriptIsRefusedByNameRatherThanSubstituted(@TempDir packDir: File) {
        packWithAllScripts(packDir)
        File(packDir, StartScriptKind.BAT.fileName).delete()
        val pack = packAt(packDir)

        val missing = Assertions.assertInstanceOf(
            StartScriptSelection.Missing::class.java,
            StartScriptSelector.selectFor(pack, StartScriptKind.BAT)
        )
        Assertions.assertTrue(
            missing.reason.contains(StartScriptKind.BAT.fileName),
            "The reason must name the file the user asked for, but was: ${missing.reason}"
        )
        Assertions.assertInstanceOf(
            StartScriptSelection.Available::class.java,
            StartScriptSelector.selectFor(pack, StartScriptKind.SH),
            "The other scripts must be unaffected by one being absent."
        )
    }

    /** What a pack carries is read once, from disk, and a directory named like a script is not one. */
    @Test
    fun readsWhichScriptsAPackCarries(@TempDir packDir: File) {
        packWithAllScripts(packDir)
        File(packDir, StartScriptKind.FISH.fileName).delete()
        File(packDir, StartScriptKind.PS1.fileName).delete()
        File(packDir, StartScriptKind.PS1.fileName).mkdirs()

        Assertions.assertEquals(
            setOf(StartScriptKind.SH, StartScriptKind.BAT),
            StartScriptSelector.scriptsIn(packDir),
            "A directory named start.ps1 is not a script, and start.fish is gone."
        )
    }

    /** An empty directory carries nothing, and every kind is refused rather than throwing. */
    @Test
    fun aPackWithNoScriptsCarriesNoneAndRefusesAll(@TempDir packDir: File) {
        Assertions.assertTrue(StartScriptSelector.scriptsIn(packDir).isEmpty())

        val pack = packAt(packDir)
        for (kind in StartScriptKind.entries) {
            Assertions.assertInstanceOf(
                StartScriptSelection.Missing::class.java,
                StartScriptSelector.selectFor(pack, kind),
                "${kind.name} must be refused when the pack carries nothing."
            )
        }
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
