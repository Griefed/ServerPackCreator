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
package de.griefed.serverpackcreator.plugin.selfextract

import com.electronwill.nightconfig.core.CommentedConfig
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.util.Optional

/**
 * Pins the hook itself: a finished generation produces both artifacts, through the real extension-point
 * signature.
 *
 * The signature is half the point. `PostGenExtension.run` takes seven parameters this extension ignores
 * six of, and driving it through the interface means a change to the extension point is a compile error
 * here rather than a hook that quietly stops being called.
 *
 * The other half is what happens when the pack is not what the extension expected. Generation has
 * already finished by the time this runs, and the server pack is already on disk and already zipped —
 * so a plugin that throws would turn a finished, correct generation into a reported failure.
 * `ApiPlugins.runPostGenExtensions` does wrap the call, but relying on somebody else's `catch` for
 * something this plugin can see coming is not a design, it is a hope.
 *
 * @author Griefed
 */
internal class SelfExtractPostGenExtensionTest {

    /** A pack directory with the one file that makes it a server pack. */
    private fun pack(): File {
        val parent = Files.createTempDirectory("spc-selfextract-extension").toFile().apply { deleteOnExit() }
        val pack = File(parent, "All_the_Mods_9").apply { mkdirs() }
        File(pack, "start.sh").writeText("#!/bin/sh\necho started\n")
        File(pack, "server.properties").writeText("motd=x\n")
        return pack
    }

    /** Drive the extension the way `ServerPackHandler` drives it. */
    private fun generate(destination: String) = SelfExtractPostGenExtension().run(
        versionMeta = mockk(relaxed = true),
        utilities = mockk(relaxed = true),
        apiProperties = mockk(relaxed = true),
        packConfig = mockk(relaxed = true),
        destination = destination,
        pluginConfig = Optional.empty<CommentedConfig>(),
        packSpecificConfigs = ArrayList()
    )

    /** The extension identifies itself wherever ServerPackCreator lists it. */
    @Test
    fun carriesItsOwnIdentity() {
        val extension = SelfExtractPostGenExtension()

        Assertions.assertTrue(extension.extensionId.isNotBlank())
        Assertions.assertTrue(extension.name.isNotBlank())
        Assertions.assertTrue(extension.description.isNotBlank())
        Assertions.assertTrue(extension.author.isNotBlank())
        Assertions.assertTrue(extension.version.isNotBlank())
    }

    /** A finished generation leaves both artifacts beside the pack it just wrote. */
    @Test
    fun aFinishedGenerationLeavesBothArtifactsBesideThePack() {
        val pack = pack()

        generate(pack.absolutePath)

        Assertions.assertTrue(File(pack.parentFile, "All_the_Mods_9.bsx").isFile, "no .bsx was written")
        Assertions.assertTrue(File(pack.parentFile, "All_the_Mods_9.cmd").isFile, "no .cmd was written")
    }

    /**
     * A destination that is not there must not throw.
     *
     * The generation it would abort has already succeeded, and the pack is already on disk — so the
     * only thing throwing could achieve here is turning a good generation into a reported failure.
     */
    @Test
    fun aMissingPackIsSurvived() {
        val absent = File(Files.createTempDirectory("spc-selfextract-absent").toFile(), "never-generated")

        Assertions.assertDoesNotThrow { generate(absent.absolutePath) }
        Assertions.assertFalse(File(absent.parentFile, "never-generated.bsx").exists(), "nothing should be written")
    }

    /**
     * A pack containing a symbolic link must be left alone, loudly, rather than followed.
     *
     * Windows cannot recreate one without Developer Mode, and a link pointing out of the pack would let
     * extraction write outside the destination directory — which is a vulnerability, not a quirk.
     */
    @Test
    fun aPackContainingASymbolicLinkIsRefused() {
        val pack = pack()
        val target = File(pack.parentFile, "outside.txt").apply { writeText("not part of the pack") }
        runCatching { Files.createSymbolicLink(File(pack, "link-to-outside.txt").toPath(), target.toPath()) }
            .onFailure { return }   // no symlink permission here; nothing to check

        Assertions.assertDoesNotThrow { generate(pack.absolutePath) }
        Assertions.assertFalse(
            File(pack.parentFile, "All_the_Mods_9.bsx").exists(),
            "a pack with a symbolic link in it must not be wrapped"
        )
    }
}
