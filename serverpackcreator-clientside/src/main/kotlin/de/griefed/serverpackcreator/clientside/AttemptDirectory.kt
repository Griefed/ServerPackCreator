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
 * literals happening to match. The grinder's `BootLogStore` addresses its kept consoles by the same name,
 * so a change here orphans every log already on disk.
 *
 * **The name is qualified by platform because a slug is not an identity.** The same slug on Modrinth and on
 * CurseForge is two projects and, in the grinder, two candidates ground by parallel workers. Staging *wipes*
 * the directory before using it and the reaper deletes it afterwards, so an unqualified name lets one
 * candidate pull the server pack out from under a container the other is still booting.
 *
 * @author Griefed
 */
object AttemptDirectory {

    /**
     * Separator between the name's parts.
     *
     * A **slug** may contain one (`jei-extras`), which is why [ownerOf] cuts a fixed number of parts off the
     * end rather than matching a prefix. A loader and a Minecraft version-line never do — a line is
     * dot-separated (`1.20`, `26.2`) — so the suffix is unambiguous.
     */
    private const val SEPARATOR = "-"

    /** How many trailing parts [nameFor] appends after the owner: the loader and the Minecraft line. */
    private const val SUFFIX_PARTS = 2

    /**
     * The directory name for one `(platform, slug, loader, Minecraft line)` attempt, e.g.
     * `Modrinth-creativecore-Fabric-26.2`. [platform] is the value the resolving `ModPlatform` reports
     * (`Modrinth`, `CurseForge`).
     *
     * **The line is part of the name because one loader is no longer one attempt.** A project is ground once
     * per Minecraft version-line and the same loader routinely wins two of them — NeoForge on 1.21 and on
     * 1.20 — so a `(platform, slug, loader)` name would have the second stage wipe the first's pack and
     * console. That is the `creativecore` failure exactly: two runs sharing a directory produced SURVIVED
     * and CRASHED for the identical build, and a crash is the one outcome that reaches a published
     * exclusion.
     */
    fun nameFor(platform: String, slug: String, loader: String, minecraftLine: String): String =
        "$platform$SEPARATOR$slug$SEPARATOR$loader$SEPARATOR$minecraftLine"

    /**
     * The candidate a directory built by [nameFor] belongs to, comparable against [ownerKey].
     *
     * The [SUFFIX_PARTS] trailing parts are cut, never matched by prefix: slugs nest (`jei` vs `jei-extras`,
     * `creativecore` vs `creativecore-extras`), and a prefix match would claim ownership of a different —
     * possibly in-flight — project and delete its pack mid-boot. **Keep this in lockstep with [nameFor]'s
     * part count**: cutting one too few silently scopes the reaper to a loader instead of a project, and one
     * too many hands it a prefix of the slug.
     */
    fun ownerOf(directoryName: String): String =
        (1..SUFFIX_PARTS).fold(directoryName) { name, _ -> name.substringBeforeLast(SEPARATOR) }

    /** The owning candidate's key, to compare an [ownerOf] result against. */
    fun ownerKey(platform: String, slug: String): String = "$platform$SEPARATOR$slug"
}
