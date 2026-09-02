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

    /** Fabric API itself, the project every one of its modules resolves to. */
    private val fabricApi = PlatformRef("fabric-api", "306612")

    /**
     * Fabric API is one project shipped as ~45 nested modules, and a descriptor depends on the **modules**
     * (`fabric-resource-loader-v0`), never on the project. Neither platform publishes them separately, so
     * every one of those ids was unresolvable: Modrinth's slug guess 404s and CurseForge refuses to guess.
     *
     * **A rule, not a table, and the difference is load-bearing.** The API-version suffix moves between
     * releases — the current tree ships `fabric-resource-loader-v1` while most of the corpus still declares
     * `-v0`, an id that exists in no source tree today — so a list snapshotted from the repository would be
     * wrong for exactly the older mods this is meant to fix. The shape (`fabric-<something>-v<digits>`) is
     * what is stable, and it is what the modules are named by convention.
     */
    private val fabricApiModulePattern = Regex("""^fabric-[a-z0-9_-]+-v\d+$""")

    /** The Fabric API modules that carry no version suffix, and so cannot be matched by shape. */
    private val unversionedFabricApiModules = setOf("fabric-api-base", "fabric-renderer-indigo")

    /**
     * Ids that look exactly like a Fabric API module and are not one.
     *
     * lucko's permissions library declares `fabric-permissions-api-v0` — plural, where Fabric API's own
     * module is the singular `fabric-permission-api-v1`. One character apart, two different projects.
     * Claiming it would stage Fabric API in place of the library the mod actually asked for and report a
     * dependency it never declared. Verified against lucko's own `fabric.mod.json`, 2026-09-01.
     *
     * Keep this list to ids **observed** colliding; guessing at more would re-create the un-pinned table
     * this class exists to avoid.
     */
    private val notFabricApi = setOf("fabric-permissions-api-v0")

    /** Whether [id] names a module of Fabric API, and therefore resolves to Fabric API itself. */
    private fun isFabricApiModule(id: String): Boolean = id !in notFabricApi &&
        (id in unversionedFabricApiModules || fabricApiModulePattern.matches(id))

    /** Quilt Standard Libraries / QFAPI, the project every QSL module resolves to. */
    private val quiltStandardLibraries = PlatformRef("qsl", "634179")

    /**
     * QSL is the Quilt mirror of the Fabric API case: one project shipped as ~33 modules, with descriptors
     * depending on the **modules** (`quilt_resource_loader`, `quilt_networking`) rather than the project.
     *
     * **The shape is deliberately not Fabric's.** QSL ids are underscored and carry no API-version suffix,
     * so `fabric-<x>-v<digits>` matches none of them — which is why the Fabric rule left this open instead
     * of closing it by coincidence. Verified 2026-09-01 across all 47 `quilt.mod.json` files in
     * `QuiltMC/quilt-standard-libraries` (branch 1.21.5): every module id they declare or depend on is
     * `quilt_` followed by lowercase words and underscores.
     */
    private val qslModulePattern = Regex("""^quilt_[a-z0-9_]+$""")

    /**
     * Ids matching the QSL shape that are not QSL modules.
     *
     * `quilt_loader` is Quilt Loader itself — supplied by the runtime, and already dropped before staging
     * by `BootVerifier.environmentProvidedIds`. Nothing downstream would notice the difference today, which
     * is exactly why it is listed: a lookup table other code is entitled to trust must not record a false
     * fact merely because the falsehood is currently unreachable.
     *
     * Like [notFabricApi], keep this to ids **observed** colliding rather than imagined ones.
     */
    private val notQsl = setOf("quilt_loader")

    /** Whether [id] names a module of QSL, and therefore resolves to QSL itself. */
    private fun isQslModule(id: String): Boolean = id !in notQsl && qslModulePattern.matches(id)

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
        val alias = aliases[id]
            ?: fabricApi.takeIf { isFabricApiModule(id) }
            ?: quiltStandardLibraries.takeIf { isQslModule(id) }
        alias?.let {
            return when (platform) {
                MODRINTH -> it.modrinth
                CURSEFORGE -> it.curseForge
                else -> null
            }
        }
        return if (platform == MODRINTH) id else null
    }
}
