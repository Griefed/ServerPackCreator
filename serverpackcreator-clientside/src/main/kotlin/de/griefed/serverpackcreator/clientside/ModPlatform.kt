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
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Read [field] as a String, returning Kotlin `null` for both an absent field and an explicit JSON
 * `null` — unlike [JsonNode.asText] with a default, which yields the literal string `"null"` for a
 * JSON-null node and would defeat null-checks such as [ModFile.locked].
 *
 * @author Griefed
 */
internal fun JsonNode.textOrNull(field: String): String? {
    val node = this.path(field)
    return if (node.isMissingNode || node.isNull) null else node.asText()
}

/**
 * A hosting platform (Modrinth, CurseForge) able to resolve a project-link into the normalized list
 * of its files and declared sideness. Implementations encapsulate the platform-specific REST calls
 * and JSON-shapes; everything downstream works against the platform-agnostic [ProjectFiles].
 *
 * @author Griefed
 */
interface ModPlatform {
    /**
     * This platform's name, as it appears on every `ProjectFiles` it produces and as the grinder's
     * `ModPlatforms` constants spell it. A property rather than four scattered string literals, because a
     * candidate's platform and a recorded verdict's platform must agree exactly or the store re-grinds the
     * same project forever.
     */
    val name: String

    /** Whether this platform recognizes [projectUrl] as one of its own project-links. */
    fun handles(projectUrl: String): Boolean

    /**
     * Resolve a project-link into its files and declared sideness.
     *
     * @throws IllegalArgumentException if the link is malformed or the project cannot be found.
     */
    fun resolve(projectUrl: String): ProjectFiles

    /**
     * Resolve a required-dependency reference (a Modrinth `project_id` or a CurseForge `modId`) into
     * its files, or `null` when it cannot be resolved. DeclaredSupport is irrelevant for a dependency and is
     * left [DeclaredSupport.UNKNOWN]; the caller picks a file matching the dependent's loader and Minecraft
     * version.
     */
    fun resolveDependency(nativeRef: String, minecraftVersion: String? = null): ProjectFiles?
}

/**
 * Build the supported hosting platforms. Modrinth is always available (no key); CurseForge is added
 * only when an API-key is supplied, since its API mandates one.
 *
 * @author Griefed
 */
fun supportedPlatforms(curseForgeApiKey: String? = System.getenv("CURSEFORGE_API_KEY")): List<ModPlatform> {
    val platforms = mutableListOf<ModPlatform>(ModrinthPlatform())
    if (!curseForgeApiKey.isNullOrBlank()) {
        platforms.add(CurseForgePlatform(curseForgeApiKey))
    }
    return platforms
}

/**
 * Tiny HTTP-GET boundary so platform clients can be unit-tested against canned JSON without touching
 * the network. The production implementation is [JdkHttpFetcher]; tests supply a fake.
 *
 * @author Griefed
 */
fun interface HttpFetcher {
    /**
     * Perform a GET against [url] with the given [headers] and return the response body as a String.
     *
     * @throws java.io.IOException on transport failure or a non-2xx response.
     */
    fun get(url: String, headers: Map<String, String>): String
}

/**
 * Default [HttpFetcher] backed by the JDK [HttpClient]. Follows redirects (the CurseForge/Modrinth
 * CDNs answer file-requests with a 302) and fails on non-2xx responses so callers can surface a
 * meaningful error instead of parsing an error-page as JSON.
 *
 * @author Griefed
 */
class JdkHttpFetcher(
    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(30))
        .build()
) : HttpFetcher {
    override fun get(url: String, headers: Map<String, String>): String {
        val builder = HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(60))
        for ((key, value) in headers) {
            builder.header(key, value)
        }
        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw java.io.IOException("GET $url returned HTTP ${response.statusCode()}")
        }
        return response.body()
    }
}

/**
 * Normalizes the loader-identifiers used by the hosting platforms onto ServerPackCreator's canonical
 * names ([de.griefed.serverpackcreator.api.config.SupportedModloaders.names]), so the rest of the
 * pipeline can reuse the existing per-loader scanner dispatch verbatim.
 *
 * @author Griefed
 */
object LoaderNames {
    /**
     * Map a platform loader-token (e.g. Modrinth's `neoforge`, a CurseForge `gameVersions` tag) to
     * its canonical SPC name, or `null` when the token is not a loader we support (e.g. a Minecraft
     * version, "Client", a resourcepack tag).
     */
    fun canonical(token: String): String? = when (token.lowercase().replace("-", "").replace("_", "")) {
        "forge" -> "Forge"
        "neoforge" -> "NeoForge"
        "fabric" -> "Fabric"
        "quilt" -> "Quilt"
        "legacyfabric" -> "LegacyFabric"
        else -> null
    }

    /** Canonical loader-names extracted from a mixed list of platform tokens, dropping non-loaders. */
    fun canonicalLoaders(tokens: Iterable<String>): Set<String> =
        tokens.mapNotNull { canonical(it) }.toSortedSet()
}
