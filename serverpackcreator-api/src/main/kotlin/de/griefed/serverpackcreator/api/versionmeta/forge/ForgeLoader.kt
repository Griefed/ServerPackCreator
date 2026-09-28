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
package de.griefed.serverpackcreator.api.versionmeta.forge

import com.fasterxml.jackson.databind.JsonNode
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftMeta
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.util.Collections

/**
 * Information about available Forge loader versions in correlation to Minecraft versions.
 *
 * @param loaderManifest Node containing information about available Forge versions.
 * @param utilities      Commonly used utilities across ServerPackCreator.
 * @param minecraftMeta  Meta for retroactively updating the previously passed meta.
 *
 * @author Griefed
 */
internal class ForgeLoader(
    private val loaderManifest: File,
    private val utilities: Utilities,
    private val minecraftMeta: MinecraftMeta
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    /**
     * Published as an **immutable snapshot behind `@Volatile`**, not as a collection [update] mutates in
     * place — the refresh runs on a background coroutine while callers read.
     */
    @Volatile
    var minecraftVersions: List<String> = emptyList()
        private set
    @Volatile
    var forgeVersions: List<String> = emptyList()
        private set

    /**
     * 1-n Minecraft version to Forge versions.
     * * `key`: Minecraft version.
     * * `value`: List of Forge versions for said Minecraft versions.
     */
    @Volatile
    var versionMeta: Map<String, List<String>> = emptyMap()
        private set

    /**
     * 1-1 Forge version to Minecraft version
     * * `key`: Forge version.
     * * `value`: Minecraft version for said Forge version.
     */
    @Volatile
    var forgeToMinecraftMeta: Map<String, String> = emptyMap()
        private set

    /**
     * 1-1 Minecraft + Forge version combination to [ForgeInstance]
     * * `key`: Minecraft version + Forge version.
     *
     * Example:
     * * `1.18.2-40.0.44`
     * + `value`: The [ForgeInstance] for said Minecraft and Forge version combination.
     */
    @Volatile
    var instanceMeta: Map<String, ForgeInstance> = emptyMap()
        private set

    /**
     * Update the available Forge loader information.
     *
     * @author Griefed
     */
    @Throws(IOException::class)
    fun update() {
        val nextMinecraftVersions = ArrayList<String>(100)
        val nextForgeVersions = ArrayList<String>(100)
        val nextVersionMeta = HashMap<String, List<String>>(200)
        val nextForgeToMinecraftMeta = HashMap<String, String>(200)
        val nextInstanceMeta = HashMap<String, ForgeInstance>(200)
        val forgeManifest: JsonNode = utilities.jsonUtilities.getJson(loaderManifest)
        for (field in forgeManifest.fieldNames()) {
            /*
             * A field, which represents a supported Minecraft version from the Forge manifest, does NOT necessarily exist
             * in Mojang's Minecraft manifest.
             * Examples:
             *   Forge Manifest Minecraft version: 1.7.10_pre4
             *   Minecraft Manifest version:       1.7.10-pre4
             * So, if we want to acquire a Forge instance for 1.7.10-pre4, it would fail.
             * When retrieving a Forge instance with a Minecraft version from the MinecraftMeta, we need to check for
             * 1.7.10_pre4 AND 1.7.10-pre4.
             */
            val mcVersion: String
            val client = field.replace("_", "-")
            if (minecraftMeta.getClient(client).isPresent) {
                mcVersion = client
                nextMinecraftVersions.add(client)
            } else {
                mcVersion = field
                nextMinecraftVersions.add(field)
            }
            val forgeVersionsForMCVer: MutableList<String> = ArrayList(100)

            for (forge in forgeManifest.get(field)) {
                /*
                 * substring of length of Minecraft version plus 1, so entries like "1.18.2-40.0.17" get their
                 * Minecraft version portion removed and result in "40.0.17". The +1 removes the "-", too. :)
                 */
                val forgeVersion = forgeVersionFrom(forge.asText(), mcVersion)
                if (forgeVersion == null) {
                    // One malformed entry costs one version, not the whole Forge load: `update()` catches only
                    // MalformedURLException and NoSuchElementException, so an uncaught slice error here would abort
                    // the parse for every remaining Minecraft version too.
                    log.warn("Skipping malformed Forge manifest entry '${forge.asText()}' under Minecraft $mcVersion.")
                    continue
                }
                nextForgeVersions.add(forgeVersion)
                forgeVersionsForMCVer.add(forgeVersion)
                try {
                    val forgeInstance = ForgeInstance(
                        mcVersion,
                        forgeVersion,
                        minecraftMeta
                    )
                    nextInstanceMeta[mcVersion + forge.asText().substring(mcVersion.length)] = forgeInstance
                    nextForgeToMinecraftMeta[forgeVersion] = mcVersion
                } catch (ex: MalformedURLException) {

                    // Well, in THEORY this should never be thrown, so we don't need to bother
                    // with a thorough error message
                    log.debug("Could not create Forge instance for Minecraft $mcVersion and Forge $forgeVersion.", ex)
                } catch (ex: NoSuchElementException) {
                    log.debug("Could not create Forge instance for Minecraft $mcVersion and Forge $forgeVersion.", ex)
                }
            }
            nextVersionMeta[mcVersion] = forgeVersionsForMCVer.asReversed()
        }
        // Published in one assignment each, as unmodifiable views: a `List`-typed field still holds an
        // ArrayList at runtime, so a caller could otherwise cast and mutate the metadata's own state.
        minecraftVersions = Collections.unmodifiableList(nextMinecraftVersions)
        forgeVersions = Collections.unmodifiableList(nextForgeVersions)
        versionMeta = Collections.unmodifiableMap(nextVersionMeta)
        forgeToMinecraftMeta = Collections.unmodifiableMap(nextForgeToMinecraftMeta)
        instanceMeta = Collections.unmodifiableMap(nextInstanceMeta)
    }

    internal companion object {
        /**
         * Strip the Minecraft portion off a Forge manifest entry, leaving the Forge version:
         * `1.18.2-40.0.17` with Minecraft `1.18.2` yields `40.0.17`. The `+ 1` also removes the `-` separator.
         *
         * **Load-bearing assumption:** the entry always begins with the manifest's own Minecraft key, and the key is
         * only ever reconciled by swapping `_` for `-` (Forge writes `1.7.10_pre4` where Mojang writes
         * `1.7.10-pre4`). That swap is length-preserving, which is the *only* reason cutting by
         * `minecraftVersion.length` stays correct for those versions — a reconciliation that changed the length would
         * silently slice the version in the wrong place instead of failing.
         *
         * Returns `null` for an entry with nothing after the key, which the caller logs and skips. A length check is
         * the only guard that fits: `startsWith("$minecraftVersion-")` would reject the legitimate `1.7.10_pre4`
         * entry, since entries carry the **raw** manifest key while the Minecraft version may be the reconciled form.
         */
        internal fun forgeVersionFrom(manifestEntry: String, minecraftVersion: String): String? =
            manifestEntry
                .takeIf { it.length > minecraftVersion.length + 1 }
                ?.substring(minecraftVersion.length + 1)
    }

}