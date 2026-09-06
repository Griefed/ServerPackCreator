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
package de.griefed.serverpackcreator.plugin.grinder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the tab extension's own identity. `GrinderPreGenExtension` had this guard and its sibling did
 * not, which is the sort of asymmetry that stays until somebody ships a blank tab title: ServerPackCreator
 * logs extensions by these fields and renders [GrinderTabExtension.title] straight into the tab strip, so
 * an empty one is a nameless tab rather than an error.
 *
 * The factory itself is not exercised here — it needs a `VersionMeta`, an `ApiProperties` and a live
 * `Utilities`, and what it returns is a Swing panel whose construction starts a poll timer. That is the
 * real runtime's job, and it was verified there.
 */
internal class GrinderTabExtensionTest {

    @Test
    fun identifiesItself() {
        val extension = GrinderTabExtension()

        Assertions.assertTrue(extension.extensionId.isNotBlank())
        Assertions.assertTrue(extension.name.isNotBlank())
        Assertions.assertTrue(extension.description.isNotBlank())
        Assertions.assertTrue(extension.author.isNotBlank())
        Assertions.assertTrue(extension.version.isNotBlank())
        Assertions.assertTrue(extension.title.isNotBlank(), "the title is what the tab strip shows")
        Assertions.assertTrue(extension.tooltip.isNotBlank())
    }

    /** The two extensions must not share an id — ServerPackCreator keys configuration off it. */
    @Test
    fun doesNotShareItsIdWithTheGenerationExtension() {
        Assertions.assertNotEquals(GrinderTabExtension().extensionId, GrinderPreGenExtension().extensionId)
    }
}
