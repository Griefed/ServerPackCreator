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
package de.griefed.serverpackcreator.api.versionmeta

/**
 * Central registry of the manifest URLs, installer/launcher URL templates and manifest tag-names
 * used across the versionmeta package. Centralizing these previously-inline literals keeps the
 * per-loader parsers free of magic strings and gives a single place to update an upstream URL or a
 * manifest schema tag. Values are intentionally compile-time constants — version resolution must
 * stay deterministic and offline-testable, so these are not runtime-configurable settings.
 */
internal object VersionMetaConfig {

    // --- Manifest URLs (one per loader manifest VersionMeta tracks) ---

    /** Mojang's Minecraft version manifest (JSON). */
    const val MINECRAFT_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json"

    /** MinecraftForge's maven-metadata (JSON). */
    const val FORGE_MANIFEST = "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json"

    /** NeoForged's legacy `forge` maven-metadata (XML), used for the older NeoForge instances. */
    const val NEOFORGE_OLD_MANIFEST = "https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml"

    /** NeoForged's `neoforge` maven-metadata (XML), used for the newer NeoForge instances. */
    const val NEOFORGE_NEW_MANIFEST = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"

    /** FabricMC's fabric-loader maven-metadata (XML). */
    const val FABRIC_LOADER_MANIFEST = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml"

    /** FabricMC's intermediary mappings manifest (JSON). */
    const val FABRIC_INTERMEDIARIES_MANIFEST = "https://meta.fabricmc.net/v2/versions/intermediary"

    /** FabricMC's fabric-installer maven-metadata (XML). */
    const val FABRIC_INSTALLER_MANIFEST = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml"

    /** QuiltMC's quilt-loader maven-metadata (XML). */
    const val QUILT_LOADER_MANIFEST = "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-loader/maven-metadata.xml"

    /** QuiltMC's quilt-installer maven-metadata (XML). */
    const val QUILT_INSTALLER_MANIFEST = "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/maven-metadata.xml"

    /** Base URL of the LegacyFabric meta service. */
    const val LEGACYFABRIC_BASE = "https://meta.legacyfabric.net"

    /** LegacyFabric game-versions manifest (JSON). */
    const val LEGACYFABRIC_GAME_MANIFEST = "$LEGACYFABRIC_BASE/v2/versions/game"

    /** LegacyFabric loader-versions manifest (JSON). */
    const val LEGACYFABRIC_LOADER_MANIFEST = "$LEGACYFABRIC_BASE/v2/versions/loader"

    /** LegacyFabric's fabric-installer maven-metadata (XML). */
    const val LEGACYFABRIC_INSTALLER_MANIFEST = "https://maven.legacyfabric.net/net/legacyfabric/fabric-installer/maven-metadata.xml"

    // --- Installer / launcher URL templates (String.format placeholders) ---

    /** Fabric installer jar; format args: installer-version, installer-version. */
    const val FABRIC_INSTALLER_TEMPLATE = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar"

    /** Improved Fabric server launcher; format args: minecraft, loader, installer. */
    const val FABRIC_IMPROVED_LAUNCHER_TEMPLATE = "https://meta.fabricmc.net/v2/versions/loader/%s/%s/%s/server/jar"

    /** Prefix for the per-loader server-JSON lookup (Fabric loader details). */
    const val FABRIC_LOADER_DETAILS_URL_PREFIX = "https://meta.fabricmc.net/v2/versions/loader/"

    /** Suffix for the per-loader server-JSON lookup (Fabric loader details). */
    const val FABRIC_LOADER_DETAILS_JSON_SUFFIX = "/server/json"

    /** LegacyFabric installer jar; format args: installer-version, installer-version. */
    const val LEGACYFABRIC_INSTALLER_TEMPLATE = "https://maven.legacyfabric.net/net/legacyfabric/fabric-installer/%s/fabric-installer-%s.jar"

    /** Quilt installer jar; format args: installer-version, installer-version. */
    const val QUILT_INSTALLER_TEMPLATE = "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/%s/quilt-installer-%s.jar"

    // --- Interpolated installer URLs (built per resolved instance) ---

    /** The Forge installer-jar URL for the given Minecraft/Forge combination. */
    fun forgeInstallerUrl(minecraftVersion: String, forgeVersion: String) =
        "https://files.minecraftforge.net/maven/net/minecraftforge/forge/$minecraftVersion-$forgeVersion/forge-$minecraftVersion-$forgeVersion-installer.jar"

    /** The newer NeoForge installer-jar URL for the given NeoForge version. */
    fun newNeoForgeInstallerUrl(neoForgeVersion: String) =
        "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neoForgeVersion/neoforge-$neoForgeVersion-installer.jar"

    /** The older NeoForge installer-jar URL for the given Minecraft/NeoForge combination. */
    fun oldNeoForgeInstallerUrl(minecraftVersion: String, neoForgeVersion: String) =
        "https://maven.neoforged.net/releases/net/neoforged/forge/$minecraftVersion-$neoForgeVersion/forge-$minecraftVersion-$neoForgeVersion-installer.jar"

    // --- Manifest tag-/field-names (XML elements & JSON fields the parsers read) ---

    /** `version` element/field — appears in every loader's maven-metadata and version lists. */
    const val TAG_VERSION = "version"

    /** `versions` array — Minecraft manifest and NeoForge refresh-count. */
    const val TAG_VERSIONS = "versions"

    /** `latest` element/field. */
    const val TAG_LATEST = "latest"

    /** `release` element/field. */
    const val TAG_RELEASE = "release"

    /** `snapshot` type marker (Minecraft client meta). */
    const val TAG_SNAPSHOT = "snapshot"

    /** `type` field (Minecraft client meta). */
    const val TAG_TYPE = "type"

    /** `id` field (Minecraft client meta). */
    const val TAG_ID = "id"

    /** `url` field (Minecraft client/server meta). */
    const val TAG_URL = "url"

    /** `stable` flag (LegacyFabric loader versioning). */
    const val TAG_STABLE = "stable"

    /** `downloads` object (Minecraft server meta). */
    const val TAG_DOWNLOADS = "downloads"

    /** `server` object (Minecraft server meta). */
    const val TAG_SERVER = "server"

    /** `javaVersion` object (Minecraft server meta). */
    const val TAG_JAVA_VERSION = "javaVersion"

    /** `majorVersion` field (Minecraft server meta). */
    const val TAG_MAJOR_VERSION = "majorVersion"
}
