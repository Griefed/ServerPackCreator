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
 * Names the per-attempt scratch directory a verification stages into, and reads that name back to the
 * candidate owning it.
 *
 * **Both halves live here so they cannot drift.** Two callers build the name — `ClientsideVerifier` for the
 * jar-scan download, `BootVerifier` for the staged server pack — and the grinder's `BootWorkspaceReaper`
 * decides what to delete from the name alone. Before this object they agreed only by separate string
 * literals happening to match.
 *
 * **The name is qualified by platform because a slug is not an identity.** The same slug on Modrinth and on
 * CurseForge is two projects and, in the grinder, two candidates ground by parallel workers. Staging *wipes*
 * the directory before using it and the reaper deletes it afterwards, so an unqualified name lets one
 * candidate pull the server pack out from under a container the other is still booting.
 *
 * @author Griefed
 */
object AttemptDirectory {

    /** Separator between the name's parts; no platform, slug or loader in use contains one. */
    private const val SEPARATOR = "-"

    /**
     * The directory name for one `(platform, slug, loader)` attempt, e.g. `Modrinth-creativecore-Fabric`.
     * [platform] is the value the resolving `ModPlatform` reports (`Modrinth`, `CurseForge`).
     */
    fun nameFor(platform: String, slug: String, loader: String): String =
        "$platform$SEPARATOR$slug$SEPARATOR$loader"

    /**
     * The candidate a directory built by [nameFor] belongs to, comparable against [ownerKey].
     *
     * Only the *loader* suffix is cut, never matched by prefix: slugs nest (`jei` vs `jei-extras`,
     * `creativecore` vs `creativecore-extras`), and a prefix match would claim ownership of a different —
     * possibly in-flight — project and delete its pack mid-boot.
     */
    fun ownerOf(directoryName: String): String = directoryName.substringBeforeLast(SEPARATOR)

    /** The owning candidate's key, to compare an [ownerOf] result against. */
    fun ownerKey(platform: String, slug: String): String = "$platform$SEPARATOR$slug"
}
