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

/** How one platform addresses a project: a Modrinth slug, and CurseForge's numeric id. */
data class PlatformRef(
    /** The Modrinth slug or project id, or `null` when the project is not on Modrinth. */
    val modrinth: String?,
    /** CurseForge's numeric project id as text, or `null` when the project is not on CurseForge. */
    val curseForge: String?
)

/**
 * Bridges the two vocabularies a dependency is spelled in: a jar manifest names a **mod id** (`fabric`),
 * while a platform wants its own **project ref** — a Modrinth slug (`fabric-api`) or a CurseForge numeric
 * id (`306612`).
 *
 * **Deliberately tiny, and it must stay that way.** The platform-declared dependency path already resolves
 * everything the platform itself knows about; this exists only for the ids that path never sees, because
 * the author declared them in the jar and nowhere else. A large hand-written table of guesses would be
 * un-pinned data that goes stale in silence — exactly the failure class this repository has paid for
 * before. Grow it only for an id that has actually been observed going unresolved.
 *
 * @author Griefed
 */
object KnownModIds {

    /** Modrinth's project name, as the platform classes report it. */
    private const val MODRINTH = "Modrinth"

    /** CurseForge's project name, as the platform classes report it. */
    private const val CURSEFORGE = "CurseForge"

    /**
     * Mod ids whose platform ref cannot be derived from the id itself.
     *
     * Both spellings of Fabric API are here because descriptors use both: `fabric` is what
     * `fabric.mod.json` declares, `fabric-api` is what some Quilt and Fabric descriptors write instead.
     */
    private val aliases: Map<String, PlatformRef> = mapOf(
        "fabric" to PlatformRef("fabric-api", "306612"),
        "fabric-api" to PlatformRef("fabric-api", "306612"),
        // QFAPI is the reason a registry is needed at all rather than a slug guess: neither platform
        // addresses it by anything resembling its mod id. Verified against both live APIs on 2026-08-29 —
        // Modrinth `qsl` (qvIfYCYJ) and CurseForge `634179`, both titled "Quilted Fabric API (QFAPI) /
        // Quilt Standard Libraries (QSL)".
        "quilted_fabric_api" to PlatformRef("qsl", "634179"),
        "qsl" to PlatformRef("qsl", "634179")
    )

    /**
     * The ref [platform] can resolve [modId] by, or `null` when there is none.
     *
     * **The two platforms are treated asymmetrically on purpose.** An unknown id is handed to Modrinth
     * as-is, because Modrinth resolves a project by slug *or* id and most mod ids are their own slug — a
     * guess costs one lookup that may simply miss, which is far cheaper than never resolving the
     * dependency. CurseForge gets no guess at all: it addresses projects by numeric id, so a mod id is
     * never a valid ref, and searching for one would spend the API key's quota on a match nothing could
     * verify. An id that maps nowhere is *reported*, never fabricated.
     */
    fun refFor(modId: String, platform: String): String? {
        val id = modId.trim().lowercase()
        if (id.isEmpty()) {
            return null
        }
        aliases[id]?.let { alias ->
            return when (platform) {
                MODRINTH -> alias.modrinth
                CURSEFORGE -> alias.curseForge
                else -> null
            }
        }
        return if (platform == MODRINTH) id else null
    }
}
