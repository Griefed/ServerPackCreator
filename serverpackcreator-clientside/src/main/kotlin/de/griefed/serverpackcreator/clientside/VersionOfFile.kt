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
package de.griefed.serverpackcreator.clientside

/**
 * The mod's own version, read out of the string a platform published the file under.
 *
 * **A published version routinely leads with the Minecraft version**, and comparing that against a
 * dependant's declared range compares the wrong number entirely. Create publishes both spellings within one
 * loader/Minecraft pair — `mc1.20.1-6.0.8` and `1.20.1-6.0.6` — so the set is judged inconsistently: the
 * `mc`-prefixed one is unreadable to [VersionConstraint] and therefore *accepts*, while the bare one reads
 * as `1.20.1` and compares as though Create were at version 1.20.
 *
 * **The file's own declared Minecraft versions are the evidence, so nothing here is a guess.** A prefix is
 * removed only when it is literally one of [ModFile.minecraftVersions], optionally spelled `mc<version>`.
 * That is why `0.92.2+1.20.1` is left alone — the Minecraft version is a *suffix* there, which no range
 * reads, and Fabric API publishes that shape by the thousand.
 *
 * @author Griefed
 */
object VersionOfFile {

    /**
     * The version [file] should be compared by: its published version with a leading Minecraft-version
     * component removed, or the published version verbatim when it carries none.
     *
     * Never returns an empty string. A file published under nothing but its Minecraft version has no mod
     * version to read, and `""` would compare as `0.0.0` — a refusal manufactured out of a naming
     * convention, which is the direction [VersionConstraint] exists to avoid.
     */
    fun of(file: ModFile): String? {
        val published = file.version?.trim()?.takeIf { it.isNotEmpty() } ?: return file.version
        for (minecraftVersion in file.minecraftVersions.sortedByDescending { it.length }) {
            for (prefix in listOf("mc$minecraftVersion", minecraftVersion)) {
                if (!published.startsWith(prefix, ignoreCase = true)) {
                    continue
                }
                // Only a *separated* prefix is one: without this, `1.21.11-…` would have its `1.21.1`
                // sheared off by the 1.21.1 a multi-version file also declares.
                val remainder = published.drop(prefix.length).trimStart('-', '_', '+', '.', ' ')
                if (remainder.isNotEmpty() && published[prefix.length] in "-_+. ") {
                    // `1.21.4-NeoForge-5.4.0`: the loader name sits between the two versions and is not part
                    // of either. Dropping leading non-numeric segments is what leaves `5.4.0`.
                    return remainder.dropLoaderSegments().takeIf { it.isNotEmpty() } ?: remainder
                }
            }
        }
        return published
    }

    /**
     * Drop leading `-`-separated segments that hold no digit, so `NeoForge-5.4.0` reads as `5.4.0`.
     *
     * Bounded to segments with **no digit at all**: a segment like `0.5.1.f` must survive, and so must one
     * like `1.21.4`, which is why the caller strips the Minecraft version by name first rather than relying
     * on this.
     */
    private fun String.dropLoaderSegments(): String =
        split("-").dropWhile { segment -> segment.isNotEmpty() && segment.none { it.isDigit() } }
            .joinToString("-")
}
