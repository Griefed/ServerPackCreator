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
package de.griefed.serverpackcreator.api.versionmeta.minecraft

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Characterizes [MinecraftMeta.requiredJavaVersion] — the published query the grinder leans on to bound
 * Minecraft selection to a bundled JDK — against **real** version metadata (offline, using the bundled
 * per-version server manifests). The existing `ConfigEditorViewModelTest` only exercises the app-side
 * facade with a mocked `VersionMeta`, so this pins the actual delegation to `MinecraftServer.javaVersion`.
 */
internal class MinecraftMetaTest {
    private val minecraft =
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).versionMeta.minecraft

    /**
     * Pins Mojang's declared `javaVersion.majorVersion` (stringified) for a spread of known releases.
     * 1.17 is included deliberately: its real requirement is **16**, not 17 — a guard against any
     * hand-rolled "era → JDK" remap creeping back in.
     */
    @Test
    fun reportsTheDeclaredServerJavaMajorForKnownReleases() {
        Assertions.assertEquals("8", minecraft.requiredJavaVersion("1.16.5").orElse(null))
        Assertions.assertEquals("16", minecraft.requiredJavaVersion("1.17").orElse(null))
        Assertions.assertEquals("17", minecraft.requiredJavaVersion("1.20.1").orElse(null))
        Assertions.assertEquals("21", minecraft.requiredJavaVersion("1.20.6").orElse(null))
    }

    /** Pins the absent-path contract: an unknown version yields an empty Optional (no server → no Java). */
    @Test
    fun isEmptyForAnUnknownMinecraftVersion() {
        Assertions.assertTrue(minecraft.requiredJavaVersion("0.0.0-not-a-real-version").isEmpty)
    }
}
