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
 * Resolves `modrinth.com` project-links via Modrinth's public REST API (no API-key; a descriptive
 * `User-Agent` is required). Modrinth uniquely declares `client_side`/`server_side`, giving a strong
 * metadata signal CurseForge cannot, and all Modrinth files are freely downloadable (never locked).
 *
 * @param httpFetcher  HTTP boundary, swapped for canned JSON in tests.
 * @param objectMapper Jackson mapper for parsing the JSON responses.
 * @author Griefed
 */
class ModrinthPlatform(
    private val httpFetcher: HttpFetcher = JdkHttpFetcher(),
    private val objectMapper: ObjectMapper = ObjectMapper()
) : ModPlatform {

    /** @see ModPlatform.name */
    override val name = "Modrinth"


    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val apiBase = "https://api.modrinth.com/v2"
    private val userAgent = "Griefed/ServerPackCreator (clientside-verification; griefed@griefed.de)"
    private val headers = mapOf("User-Agent" to userAgent, "Accept" to "application/json")

    override fun handles(projectUrl: String): Boolean =
        projectUrl.contains("modrinth.com", ignoreCase = true)

    override fun resolve(projectUrl: String): ProjectFiles {
        val slug = extractSlug(projectUrl)
        val project = objectMapper.readTree(httpFetcher.get("$apiBase/project/$slug", headers))
        val versions = objectMapper.readTree(httpFetcher.get("$apiBase/project/$slug/version", headers))

        val files = ArrayList<ModFile>()
        for (version in versions) {
            files.addAll(filesOf(version, projectUrl))
        }
        return ProjectFiles(
            platform = name,
            slug = slug,
            projectUrl = projectUrl,
            clientSide = DeclaredSupport.fromString(project.textOrNull("client_side")),
            serverSide = DeclaredSupport.fromString(project.textOrNull("server_side")),
            files = files
        )
    }

    override fun resolveDependency(nativeRef: String): ProjectFiles? = try {
        val projectUrl = "https://modrinth.com/mod/$nativeRef"
        val versions = objectMapper.readTree(httpFetcher.get("$apiBase/project/$nativeRef/version", headers))
        val files = versions.flatMap { filesOf(it, projectUrl) }
        ProjectFiles(name, nativeRef, projectUrl, DeclaredSupport.UNKNOWN, DeclaredSupport.UNKNOWN, files)
    } catch (ex: Exception) {
        log.warn("Could not resolve Modrinth dependency '$nativeRef': ${ex.message}")
        null
    }

    /**
     * Map a single Modrinth version-node onto our [ModFile]s. A version's `loaders`/`game_versions`
     * and required `dependencies` apply to every file it lists, but only its [modFilesOf] entries are
     * mods at all.
     */
    private fun filesOf(version: JsonNode, projectUrl: String): List<ModFile> {
        val loaders = LoaderNames.canonicalLoaders(version.path("loaders").map { it.asText() })
        val mcVersions = version.path("game_versions").map { it.asText() }.toSortedSet()
        val requiredDeps = version.path("dependencies")
            .filter { it.path("dependency_type").asText() == "required" }
            .mapNotNull { it.path("project_id").asText(null) }
        return modFilesOf(version).map { file ->
            ModFile(
                fileName = file.path("filename").asText(),
                loaders = loaders,
                minecraftVersions = mcVersions,
                downloadUrl = file.textOrNull("url"),
                pageUrl = projectUrl,
                requiredDependencies = requiredDeps,
                // The version this file was published under, which is what a dependant's declared
                // constraint has to be matched against. Modrinth states it once per version, not per file.
                version = version.textOrNull("version_number")
            )
        }
    }

    /**
     * The entries of [version]'s `files` that are actually the mod: the ones Modrinth flags
     * `"primary": true`, or — for a version that flags none — all of them.
     *
     * A Modrinth version routinely carries attachments beside its mod jar, source jars above all, and none
     * of them is a mod: not bootable, not worth scanning, and poisonous to the file-name stem the verdict
     * is published under. Measured against the live API on 2026-08-23: `creativecore`'s single stray
     * `CreativeCore-sources.jar` dragged its Fabric list-entry from `CreativeCore_FABRIC_` down to
     * `CreativeCore-sources`, which matches nothing.
     *
     * The fallback is not defensive padding — 3 of that project's 300 versions genuinely flag no primary,
     * and dropping them would lose real builds. "No primary" means "Modrinth cannot tell us which one",
     * not "none of these is a mod".
     */
    private fun modFilesOf(version: JsonNode): List<JsonNode> {
        val files = version.path("files").toList()
        val primaries = files.filter { it.path("primary").asBoolean(false) }
        return primaries.ifEmpty { files }
    }

    /**
     * Extract the project-slug from a Modrinth link such as `https://modrinth.com/mod/jei` or
     * `.../mod/jei/versions` — the slug is the path-segment following the project-type segment.
     */
    private fun extractSlug(projectUrl: String): String {
        val segments = projectUrl.substringBefore('?').substringBefore('#')
            .substringAfter("modrinth.com/").split('/').filter { it.isNotBlank() }
        require(segments.size >= 2) { "Cannot extract a Modrinth project-slug from '$projectUrl'." }
        return segments[1]
    }
}
