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
 * How strongly a hosting platform says a mod supports **one** side. Mirrors Modrinth's
 * `client_side`/`server_side` values verbatim; CurseForge exposes no such field and therefore always
 * reports [UNKNOWN].
 *
 * Read as a pair — a project carries one of these for the client and another for the server
 * ([ProjectFiles.clientSide] / [ProjectFiles.serverSide]) — so a single value answers "how much does
 * it want *this* side", never "which side does it belong on".
 *
 * **Not to be merged with `api.modscanning.Sideness`, despite the name this type used to carry.**
 * That enum is SPC's own *verdict* (`SERVER`/`CLIENT`, one value = the whole answer, defaulting to
 * `SERVER` so nothing is dropped). This one is a third party's *self-report* about one side, and the
 * confidence model is built on keeping the two apart: `ClientsideVerifier.aggregate` folds this,
 * `JarScan` (where SPC's own verdict arrives) and `BootResult` into a [Verdict] precisely because
 * the platform's claim is unreliable — which is the entire reason the expensive boot-test exists.
 * The translation between the two domains is deliberate and lives at that call-site, taking *two* of
 * these values to derive one client/server leaning.
 *
 * @author Griefed
 */
enum class DeclaredSupport {
    /** The mod needs this side to run. */
    REQUIRED,

    /** The mod runs with or without this side. */
    OPTIONAL,

    /** The mod does not work on this side — the one value that leans clientside when read of the server. */
    UNSUPPORTED,

    /** The platform said nothing. Always the case on CurseForge, which has no such field. */
    UNKNOWN;

    /** Parsing of the platform's own wording into this enum; see [fromString] for the unrecognised case. */
    companion object {
        /**
         * Parse a platform-provided support-string into a [DeclaredSupport], defaulting to [UNKNOWN] for
         * anything unrecognized or absent so callers never have to null-check.
         */
        fun fromString(value: String?): DeclaredSupport = when (value?.lowercase()) {
            "required" -> REQUIRED
            "optional" -> OPTIONAL
            "unsupported" -> UNSUPPORTED
            else -> UNKNOWN
        }
    }
}

/**
 * How stable a published file claims to be — Modrinth's `version_type`, CurseForge's `releaseType`.
 *
 * **Declaration order is the preference order**, and `BootCandidateSelector.pickBootableCandidate` walks it:
 * a release is what a user's pack installs, so it is what a verdict should be about.
 *
 * Measured on `hybrid-aquatic`, 2026-09-10: 16 stable Forge releases on Minecraft 1.20.1 beside 10
 * `[Sinytra]` betas on 1.20.1–1.20.4. Newest-Minecraft-first picked a beta, because authors publish their
 * experimental *newer*-Minecraft ports on that channel while the stable line sits on an older version — so
 * the ordering preferred a beta for exactly the projects that had a stable alternative.
 *
 * @author Griefed
 */
enum class ReleaseChannel {
    /** A normal release, and what an absent or unrecognised channel reads as. */
    RELEASE,

    /** Pre-release. Reached only when the project publishes no release for the loader being booted. */
    BETA,

    /** Earlier than beta, and the last resort — `faster-random` publishes one and no Forge release at all. */
    ALPHA;

    companion object {
        /**
         * The channel [value] names, defaulting to [RELEASE] for anything unrecognised, absent or `null`.
         *
         * Failing toward RELEASE keeps the preference a *no-op* where the platform says nothing, which is the
         * behaviour every caller had before the channel was read at all — a platform that renames the field
         * degrades to newest-Minecraft-first rather than to "everything is an alpha".
         */
        fun fromString(value: String?): ReleaseChannel =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: RELEASE

        /**
         * The channel CurseForge's numeric `releaseType` names — `1` release, `2` beta, `3` alpha — with the
         * same fail-toward-RELEASE rule for anything else.
         */
        fun fromCurseForge(releaseType: Int): ReleaseChannel = when (releaseType) {
            2 -> BETA
            3 -> ALPHA
            else -> RELEASE
        }
    }
}

/**
 * A single downloadable mod-file as listed by a hosting platform, normalized across Modrinth and
 * CurseForge. [downloadUrl] is `null` when the author forbade third-party distribution
 * (CurseForge `allowModDistribution=false`); such [locked] files can only be fetched through the
 * project's website download-flow (see the browser-downloader in the boot phase).
 *
 * @param fileName             The exact file-name as published, case preserved (the clientside-list
 *                             matches file-names with `startsWith`, so case matters).
 * @param loaders              Canonical SPC loader-names this file targets (Forge, NeoForge, Fabric,
 *                             Quilt, LegacyFabric).
 * @param minecraftVersions    Minecraft versions this file targets.
 * @param downloadUrl          Direct download-URL, or `null` when distribution is locked.
 * @param pageUrl              The project/file webpage, used by the browser-downloader for locked files.
 * @param requiredDependencies Platform-native references (ids/slugs) of mods required by this file.
 * @author Griefed
 */
data class ModFile(
    val fileName: String,
    val loaders: Set<String>,
    val minecraftVersions: Set<String>,
    val downloadUrl: String?,
    val pageUrl: String?,
    val requiredDependencies: List<String>,
    /**
     * The version the platform published this file under (Modrinth's `version_number`, CurseForge's
     * `displayName`), or `null` when it reported none. Carried so a dependant's declared constraint can
     * actually be *matched* rather than merely recorded — both platforms had this and both discarded it.
     */
    val version: String? = null,
    /**
     * Every project this file's page links, required **and** optional — the pool of things worth asking
     * what they are, never a list of things to stage.
     *
     * [requiredDependencies] is the staging list and stays as strict as it is. This one exists because a
     * mod id that resolves to nothing by spelling can still be sitting on the project page under a
     * different name: `do-a-barrel-roll` declares `yet_another_config_lib_v3` in its jar while Modrinth
     * lists YACL for it as *optional*, so the required list never mentions it.
     *
     * **Incompatible links are excluded.** Reading their id would be the right file for the wrong reason,
     * and a match would stage the one jar the author says must not be there.
     */
    val relatedDependencies: List<String> = emptyList(),
    /**
     * How stable the platform says this file is. Defaults to [ReleaseChannel.RELEASE], which is also what an
     * absent or unrecognised value reads as — see [ReleaseChannel.fromString].
     */
    val channel: ReleaseChannel = ReleaseChannel.RELEASE
) {
    /** Whether this file cannot be downloaded via the API and needs the browser download-flow. */
    val locked: Boolean
        get() = downloadUrl == null
}

/**
 * The resolved view of a hosting-platform project: its declared sideness (Modrinth only) and the
 * complete list of its [files] across every loader and Minecraft version.
 *
 * @param platform    Human-readable platform name ("Modrinth" or "CurseForge").
 * @param slug        The project-slug taken from the issue-link.
 * @param projectUrl  The original project-link from the issue.
 * @param clientSide  Platform-declared client support; [DeclaredSupport.UNKNOWN] for CurseForge.
 * @param serverSide  Platform-declared server support; [DeclaredSupport.UNKNOWN] for CurseForge.
 * @param files       Every published file of the project.
 * @author Griefed
 */
data class ProjectFiles(
    val platform: String,
    val slug: String,
    val projectUrl: String,
    val clientSide: DeclaredSupport,
    val serverSide: DeclaredSupport,
    val files: List<ModFile>
) {
    /** All distinct canonical loader-names this project ships for. */
    val loaders: Set<String>
        get() = files.flatMap { it.loaders }.toSortedSet()

    /** Every distinct file-name of the project. */
    val fileNames: List<String>
        get() = files.map { it.fileName }.distinct()
}
