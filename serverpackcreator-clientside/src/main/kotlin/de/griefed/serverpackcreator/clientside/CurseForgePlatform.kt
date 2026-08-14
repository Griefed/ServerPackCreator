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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.kotlin.cachedLoggerOf

/**
 * Resolves `curseforge.com` mod-links via the CurseForge REST API (requires an `x-api-key`).
 * CurseForge exposes no sideness-field, so [ProjectFiles.clientSide]/[ProjectFiles.serverSide] are
 * always [DeclaredSupport.UNKNOWN] here and confidence must come from the jar-scan and the boot-test. Files
 * whose author forbade third-party distribution arrive with a `null` download-URL and are flagged
 * [ModFile.locked] for the browser-downloader.
 *
 * @param apiKey       The CurseForge API-key (from the `CURSEFORGE_API_KEY` environment-variable).
 * @param httpFetcher  HTTP boundary, swapped for canned JSON in tests.
 * @param objectMapper Jackson mapper for parsing the JSON responses.
 * @author Griefed
 */
class CurseForgePlatform(
    private val apiKey: String,
    private val httpFetcher: HttpFetcher = JdkHttpFetcher(),
    private val objectMapper: ObjectMapper = ObjectMapper()
) : ModPlatform {

    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val apiBase = "https://api.curseforge.com/v1"
    private val minecraftGameId = 432
    private val modsClassId = 6
    private val requiredRelationType = 3
    private val headers: Map<String, String>
        get() = mapOf("x-api-key" to apiKey, "Accept" to "application/json")

    /** A CurseForge `gameVersions` entry that looks like a Minecraft version (e.g. `1.20.1`). */
    private val minecraftVersion = Regex("^\\d+\\.\\d+(\\.\\d+)?$")

    override fun handles(projectUrl: String): Boolean =
        projectUrl.contains("curseforge.com", ignoreCase = true)

    override fun resolve(projectUrl: String): ProjectFiles {
        val slug = extractSlug(projectUrl)
        val search = objectMapper.readTree(
            httpFetcher.get("$apiBase/mods/search?gameId=$minecraftGameId&classId=$modsClassId&slug=$slug", headers)
        )
        val modNode = search.path("data").firstOrNull()
            ?: throw IllegalArgumentException("No CurseForge mod found for slug '$slug'.")
        val modId = modNode.path("id").asLong()
        val webBase = modNode.path("links").textOrNull("websiteUrl") ?: projectUrl

        val filesResponse = objectMapper.readTree(
            httpFetcher.get("$apiBase/mods/$modId/files?pageSize=50", headers)
        )
        val files = filesResponse.path("data").map { fileNode -> toModFile(fileNode, webBase) }
        return ProjectFiles(
            platform = "CurseForge",
            slug = slug,
            projectUrl = projectUrl,
            clientSide = DeclaredSupport.UNKNOWN,
            serverSide = DeclaredSupport.UNKNOWN,
            files = files
        )
    }

    override fun resolveDependency(nativeRef: String): ProjectFiles? = try {
        val modId = nativeRef.toLong()
        val modNode = objectMapper.readTree(httpFetcher.get("$apiBase/mods/$modId", headers)).path("data")
        val webBase = modNode.path("links").textOrNull("websiteUrl") ?: "https://www.curseforge.com"
        val files = objectMapper.readTree(httpFetcher.get("$apiBase/mods/$modId/files?pageSize=50", headers))
            .path("data").map { toModFile(it, webBase) }
        ProjectFiles("CurseForge", nativeRef, webBase, DeclaredSupport.UNKNOWN, DeclaredSupport.UNKNOWN, files)
    } catch (ex: Exception) {
        log.warn("Could not resolve CurseForge dependency '$nativeRef': ${ex.message}")
        null
    }

    /**
     * Map a single CurseForge file-node onto a [ModFile]. `gameVersions` mixes Minecraft versions and
     * loader-tags, so loaders and Minecraft versions are separated by shape; a `null` `downloadUrl`
     * marks a distribution-locked file.
     */
    private fun toModFile(fileNode: JsonNode, webBase: String): ModFile {
        val gameVersions = fileNode.path("gameVersions").map { it.asText() }
        val requiredDeps = fileNode.path("dependencies")
            .filter { it.path("relationType").asInt() == requiredRelationType }
            .map { it.path("modId").asText() }
        val fileId = fileNode.path("id").asLong()
        return ModFile(
            fileName = fileNode.path("fileName").asText(),
            loaders = LoaderNames.canonicalLoaders(gameVersions),
            minecraftVersions = gameVersions.filter { minecraftVersion.matches(it) }.toSortedSet(),
            downloadUrl = fileNode.textOrNull("downloadUrl"),
            pageUrl = "$webBase/files/$fileId",
            requiredDependencies = requiredDeps
        )
    }

    /**
     * Extract the mod-slug from a CurseForge link such as
     * `https://www.curseforge.com/minecraft/mc-mods/jei` or `.../mc-mods/jei/files/all` — the slug is
     * the path-segment following the category (`mc-mods`, `bukkit-plugins`, …).
     */
    private fun extractSlug(projectUrl: String): String {
        val segments = projectUrl.substringBefore('?').substringBefore('#')
            .substringAfter("curseforge.com/").split('/').filter { it.isNotBlank() }
        // segments are [game, category, slug, ...]; the slug sits at index 2.
        require(segments.size >= 3) { "Cannot extract a CurseForge mod-slug from '$projectUrl'." }
        return segments[2]
    }
}
