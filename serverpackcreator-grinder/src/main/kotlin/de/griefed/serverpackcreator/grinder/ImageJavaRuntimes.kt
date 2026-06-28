/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.grinder

import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftMeta

/**
 * Decides which bundled JDK boots a given Minecraft version, and whether the runtime image can run it
 * at all. The required Java major comes from SPC's own version metadata
 * ([MinecraftMeta.requiredJavaVersion]) — Mojang's *declared* server requirement — not a hand-rolled
 * heuristic, so it stays correct across Minecraft versioning-scheme changes. The image ships a fixed
 * JDK set ([bundledMajors], default 8/17/21 to match the runtime Dockerfile); a version whose required
 * Java isn't bundled is reported **unsupported** so the grinder skips it. Skipping (rather than booting
 * on the wrong JDK) is the whole point: a Java-version crash would otherwise be mis-scored as a
 * clientside crash (a false HIGH).
 *
 * @param requiredJavaMajor Maps a Minecraft version to its required Java major, or `null` when unknown.
 *                          Injected (not bound to [MinecraftMeta]) so the gate is unit-testable.
 * @param bundledMajors     The Java majors the runtime image actually ships — must match the Dockerfile.
 * @author Griefed
 */
class ImageJavaRuntimes(
    private val requiredJavaMajor: (String) -> Int?,
    private val bundledMajors: Set<Int> = setOf(8, 17, 21)
) {
    /** True when [minecraftVersion]'s required Java major is both known and bundled in the image. */
    fun supports(minecraftVersion: String): Boolean =
        requiredJavaMajor(minecraftVersion)?.let { it in bundledMajors } ?: false

    /** The in-image `java` binary path for [minecraftVersion], or `null` when its Java isn't bundled. */
    fun javaPath(minecraftVersion: String): String? =
        requiredJavaMajor(minecraftVersion)
            ?.takeIf { it in bundledMajors }
            ?.let { "/opt/java-$it/bin/java" }

    companion object {
        /**
         * Build from SPC's version metadata: the required Java comes straight from Mojang's server
         * manifest via [MinecraftMeta.requiredJavaVersion] (a stringified major), parsed to an Int.
         */
        fun from(minecraftMeta: MinecraftMeta, bundledMajors: Set<Int> = setOf(8, 17, 21)): ImageJavaRuntimes =
            ImageJavaRuntimes(
                requiredJavaMajor = { minecraftMeta.requiredJavaVersion(it).orElse(null)?.toIntOrNull() },
                bundledMajors = bundledMajors
            )
    }
}
