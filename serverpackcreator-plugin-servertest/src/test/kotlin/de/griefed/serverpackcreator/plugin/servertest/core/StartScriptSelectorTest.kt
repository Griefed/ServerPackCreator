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

import de.griefed.serverpackcreator.api.settings.ScriptTemplatesConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins that the scripts on offer are **ServerPackCreator's own configured templates**, and that the user
 * picks among them.
 *
 * Two separate mistakes are being guarded against. Offering a fixed list would let the dropdown name a
 * script no generation produces — or hide one an operator added — because
 * `ApiProperties.startScriptTemplates` is what `ServerPackProvisioner` actually writes packs from. And
 * choosing *for* the user is the failure with no workaround: a guess landing on a script their machine
 * cannot run leaves them unable to start a pack with nowhere to say otherwise.
 */
internal class StartScriptSelectorTest {

    /** The template keys ServerPackCreator ships by default, taken from its own defaults rather than typed. */
    private val shippedKeys = listOf("sh", "ps1", "bat", "fish")

    private fun packAt(directory: File) = LaunchablePack(
        directory = directory,
        name = directory.name,
        minecraftVersion = "1.21",
        modloader = "NeoForge",
        modloaderVersion = "21.0.18",
        scriptKeysPresent = StartScriptSelector.scriptKeysIn(directory)
    )

    /**
     * A key becomes the file ServerPackCreator generates for it.
     *
     * `ServerPackProvisioner.startScriptName` is `"start.$key"`, and this plugin has to agree with it or
     * every row reports a missing script that is sitting right there.
     */
    @Test
    fun aTemplateKeyNamesTheGeneratedScript() {
        Assertions.assertEquals("start.sh", StartScripts.forKey("sh").fileName)
        Assertions.assertEquals("start.bat", StartScripts.forKey("bat").fileName)
        Assertions.assertEquals("start.zsh", StartScripts.forKey("zsh").fileName)
    }

    /** The scripts ServerPackCreator ships templates for run through the interpreter each one needs. */
    @Test
    fun theShippedScriptTypesRunThroughTheirInterpreter() {
        Assertions.assertEquals(listOf("bash", "start.sh"), StartScripts.forKey("sh").command)
        Assertions.assertEquals(listOf("cmd", "/c", "start.bat"), StartScripts.forKey("bat").command)
        Assertions.assertEquals(
            listOf("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "start.ps1"),
            StartScripts.forKey("ps1").command
        )
        Assertions.assertEquals(listOf("fish", "start.fish"), StartScripts.forKey("fish").command)
    }

    /**
     * A template an operator added is offered and executed **directly**, not guessed at.
     *
     * ServerPackCreator marks every generated start script executable, so a custom template carrying a
     * shebang runs on its own. Inventing an interpreter for a key nobody documented would repeat the very
     * mistake the fixed platform fallback made.
     */
    @Test
    fun anOperatorsOwnTemplateIsOfferedAndRunDirectly() {
        val custom = StartScripts.forKey("zsh")

        Assertions.assertEquals(listOf("./start.zsh"), custom.command)
        Assertions.assertTrue(custom.label.contains("start.zsh"), "The label must name the file: ${custom.label}")
    }

    /** The dropdown's entries come from the configured templates, one per key, in a stable order. */
    @Test
    fun theChoicesAreBuiltFromTheConfiguredTemplateKeys() {
        val offered = StartScripts.forTemplateKeys(shippedKeys.shuffled())

        Assertions.assertEquals(shippedKeys.toSet(), offered.map { it.key }.toSet())
        Assertions.assertEquals(
            StartScripts.forTemplateKeys(shippedKeys.shuffled()).map { it.key },
            offered.map { it.key },
            "A HashMap's keys have no order, so the dropdown must impose one or it reshuffles between reads."
        )
    }

    /** An operator who removes a template stops it being offered — the whole point of reading the setting. */
    @Test
    fun aRemovedTemplateIsNotOffered() {
        val offered = StartScripts.forTemplateKeys(listOf("sh", "bat"))

        Assertions.assertEquals(listOf("sh", "bat"), offered.map { it.key })
        Assertions.assertTrue(offered.none { it.key == "fish" }, "fish is not configured, so it must not be offered.")
    }

    /** The pre-selection follows the host, but only among what is actually configured. */
    @Test
    fun theDefaultFollowsThePlatformAmongTheConfiguredScripts() {
        val all = StartScripts.forTemplateKeys(shippedKeys)

        Assertions.assertEquals("bat", StartScripts.defaultFor(all, Platform.WINDOWS)?.key)
        Assertions.assertEquals("sh", StartScripts.defaultFor(all, Platform.POSIX)?.key)

        val noBat = StartScripts.forTemplateKeys(listOf("sh", "ps1"))
        Assertions.assertNotNull(
            StartScripts.defaultFor(noBat, Platform.WINDOWS),
            "With no batch template configured, Windows must still start somewhere."
        )
    }

    /** No templates configured means nothing to pre-select, and the caller has to cope rather than crash. */
    @Test
    fun noConfiguredTemplatesMeansNoDefault() {
        Assertions.assertTrue(StartScripts.forTemplateKeys(emptyList()).isEmpty())
        Assertions.assertNull(StartScripts.defaultFor(emptyList(), Platform.POSIX))
    }

    /** The keys this plugin knows interpreters for are the ones ServerPackCreator actually ships. */
    @Test
    fun theShippedTemplateKeysAreTheOnesThisPluginKnows() {
        val shipped = ScriptTemplatesConfig::class.java.declaredMethods
            .any { it.name == "defaultStartScriptTemplates" }
        Assertions.assertTrue(shipped, "ScriptTemplatesConfig must still be where the defaults live.")

        for (key in shippedKeys) {
            Assertions.assertFalse(
                StartScripts.forKey(key).command == listOf("./start.$key"),
                "'$key' is a script type ServerPackCreator ships, so this plugin must know how to run it " +
                        "rather than falling back to executing it directly."
            )
        }
    }

    /** A pack carrying the chosen script can be launched with it, and the argv is that script's. */
    @Test
    fun aPackCarryingTheChosenScriptCanBeLaunchedWithIt(@TempDir packDir: File) {
        for (key in shippedKeys) {
            File(packDir, "start.$key").writeText("#placeholder\n")
        }
        val pack = packAt(packDir)

        for (script in StartScripts.forTemplateKeys(shippedKeys)) {
            val available = Assertions.assertInstanceOf(
                StartScriptSelection.Available::class.java,
                StartScriptSelector.selectFor(pack, script),
                "${script.fileName} is in the pack, so it must be launchable."
            )
            Assertions.assertEquals(script.command, available.command)
            Assertions.assertEquals(File(packDir, script.fileName), available.script)
        }
    }

    /**
     * A pack missing the chosen script is refused **by name** — never silently launched with another.
     *
     * Substituting is what the old platform fallback did, and it hid that the user asked for `start.bat`
     * and got `start.ps1`.
     */
    @Test
    fun aMissingScriptIsRefusedByNameRatherThanSubstituted(@TempDir packDir: File) {
        File(packDir, "start.sh").writeText("#placeholder\n")
        val pack = packAt(packDir)

        val missing = Assertions.assertInstanceOf(
            StartScriptSelection.Missing::class.java,
            StartScriptSelector.selectFor(pack, StartScripts.forKey("bat"))
        )
        Assertions.assertTrue(
            missing.reason.contains("start.bat"),
            "The reason must name the file the user asked for, but was: ${missing.reason}"
        )
        Assertions.assertInstanceOf(
            StartScriptSelection.Available::class.java,
            StartScriptSelector.selectFor(pack, StartScripts.forKey("sh"))
        )
    }

    /**
     * What a pack carries is every `start.*` it has, independent of what is configured now.
     *
     * Deliberate: a pack generated under a different template set still reports itself honestly, and the
     * dropdown — not the catalog — is where the current configuration is applied.
     */
    @Test
    fun readsEveryStartScriptAPackCarries(@TempDir packDir: File) {
        File(packDir, "start.sh").writeText("#placeholder\n")
        File(packDir, "start.zsh").writeText("#placeholder\n")
        File(packDir, "start.ps1").mkdirs()
        File(packDir, "install_java.sh").writeText("#not a start script\n")
        File(packDir, "variables.txt").writeText("#not a start script\n")

        Assertions.assertEquals(
            setOf("sh", "zsh"),
            StartScriptSelector.scriptKeysIn(packDir),
            "A directory named start.ps1 is not a script, and install_java.sh is not a start script."
        )
    }

    /** An empty directory carries nothing, and every choice is refused rather than throwing. */
    @Test
    fun aPackWithNoScriptsCarriesNoneAndRefusesAll(@TempDir packDir: File) {
        Assertions.assertTrue(StartScriptSelector.scriptKeysIn(packDir).isEmpty())

        val pack = packAt(packDir)
        for (script in StartScripts.forTemplateKeys(shippedKeys)) {
            Assertions.assertInstanceOf(
                StartScriptSelection.Missing::class.java,
                StartScriptSelector.selectFor(pack, script)
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
