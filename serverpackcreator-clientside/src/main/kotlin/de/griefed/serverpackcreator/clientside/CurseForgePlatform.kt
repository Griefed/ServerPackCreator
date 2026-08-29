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

        val files = allFilesOf(modId, webBase, slug)
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
        // Deliberately one page, unlike [resolve]: a dependency only needs *a* usable file for the loader and
        // Minecraft version being booted, and paging every dependency of every candidate would multiply what a
        // catalog sweep spends of the API key's quota for evidence nobody reads.
        val files = objectMapper.readTree(httpFetcher.get(filesUrl(modId, index = 0), headers))
            .path("data").map { toModFile(it, webBase) }
        ProjectFiles("CurseForge", nativeRef, webBase, DeclaredSupport.UNKNOWN, DeclaredSupport.UNKNOWN, files)
    } catch (ex: Exception) {
        log.warn("Could not resolve CurseForge dependency '$nativeRef': ${ex.message}")
        null
    }

    /** One page of a project's files, `index` being the offset CurseForge pages on. */
    private fun filesUrl(modId: Long, index: Int) = "$apiBase/mods/$modId/files?index=$index&pageSize=$FILE_PAGE_SIZE"

    /**
     * Every published file of [modId], walked page by page rather than taking the newest [FILE_PAGE_SIZE].
     *
     * **Why the whole list:** a project that migrated Forge → NeoForge keeps publishing NeoForge builds, so its
     * last Forge build sinks toward the far end of a single page and the builds before it drop out of view
     * entirely — leaving the boot-phase's crash re-check with no other version of *that* loader to try,
     * precisely for the projects that produce a false clientside verdict (measured on `iron-chests`,
     * 2026-08-23). Most projects still cost one call, because `totalCount` says when to stop.
     *
     * Bounded by [MAX_FILE_PAGES]: a `totalCount` that never arrives — an odd answer, a changed response
     * shape — must cost a bounded number of calls against the key's quota rather than spin. A truncated view
     * is logged rather than passed off as the whole list.
     */
    private fun allFilesOf(modId: Long, webBase: String, slug: String): List<ModFile> {
        val files = ArrayList<ModFile>()
        for (page in 0 until MAX_FILE_PAGES) {
            val response = objectMapper.readTree(httpFetcher.get(filesUrl(modId, files.size), headers))
            val data = response.path("data")
            if (data.isEmpty) {
                break
            }
            data.forEach { fileNode -> files.add(toModFile(fileNode, webBase)) }
            // An absent pagination block reads as "this was all of it", which keeps a fetcher that does not
            // emulate paging to exactly one call instead of walking until it repeats itself.
            if (files.size >= response.path("pagination").path("totalCount").asInt(files.size)) {
                return files
            }
        }
        log.warn("CurseForge project '$slug' has more files than ${MAX_FILE_PAGES * FILE_PAGE_SIZE}; older builds were not read.")
        return files
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
            requiredDependencies = requiredDeps,
            // CurseForge has no version field; `displayName` is what an author types as the release name
            // and is the closest thing to one. It is often decorated ("JEI 15.2.0.27 for 1.20.1"), which is
            // fine: VersionConstraint reads what it can and accepts what it cannot.
            version = fileNode.textOrNull("displayName")
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

    /**
     * CurseForge's own API limits, quoted rather than chosen: its page size and how far this client is willing
     * to page. See [FILE_PAGE_SIZE] and the paging landmine on `resolve`.
     */
    companion object {
        /** CurseForge's maximum `pageSize` for the files endpoint; asking for more is not honoured. */
        const val FILE_PAGE_SIZE = 50

        /**
         * How many file-pages one project may cost. 500 files is far past any real mod's history, so the cap
         * is a runaway guard rather than a budget — it exists so a response that never reports a reachable
         * `totalCount` cannot spend the API key's quota in a loop.
         */
        const val MAX_FILE_PAGES = 10
    }
}
