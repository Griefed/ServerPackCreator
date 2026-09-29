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
package de.griefed.serverpackcreator.plugin.servertest

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.plugin.servertest.core.GenerationNotifier
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Optional

/**
 * Pins that a finished generation reaches the notifier, through the real extension-point signature.
 *
 * The signature is the point: `PostGenExtension.run` takes seven parameters this extension mostly ignores,
 * and pinning it through the interface means a change to the extension point is a compile error here rather
 * than a hook that silently stops being called.
 */
internal class ServerTestPostGenExtensionTest {

    /** The extension identifies itself, and does not collide with the tab extension's id. */
    @Test
    fun carriesItsOwnIdentity() {
        val extension = ServerTestPostGenExtension()

        Assertions.assertTrue(extension.extensionId.isNotBlank())
        Assertions.assertNotEquals(ServerTestTabExtension().extensionId, extension.extensionId)
        Assertions.assertTrue(extension.name.isNotBlank())
        Assertions.assertTrue(extension.description.isNotBlank())
    }

    /** A finished generation publishes the pack's directory, taken from the destination it was handed. */
    @Test
    fun publishesTheGeneratedPack() {
        val seen = mutableListOf<File>()
        val handle = GenerationNotifier.subscribe { seen.add(it) }
        try {
            ServerTestPostGenExtension().run(
                versionMeta = mockk(relaxed = true),
                utilities = mockk(relaxed = true),
                apiProperties = mockk(relaxed = true),
                packConfig = mockk(relaxed = true),
                destination = File("/packs/Generated").absolutePath,
                pluginConfig = Optional.empty<CommentedConfig>(),
                packSpecificConfigs = ArrayList()
            )
        } finally {
            handle.cancel()
        }

        Assertions.assertEquals(listOf(File("/packs/Generated").absoluteFile), seen)
    }
}
