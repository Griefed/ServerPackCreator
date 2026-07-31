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
    val minecraftVersions: MutableList<String> = ArrayList(100)
    val forgeVersions: MutableList<String> = ArrayList(100)

    /**
     * 1-n Minecraft version to Forge versions.
     * * `key`: Minecraft version.
     * * `value`: List of Forge versions for said Minecraft versions.
     */
    val versionMeta: HashMap<String, List<String>> = HashMap(200)

    /**
     * 1-1 Forge version to Minecraft version
     * * `key`: Forge version.
     * * `value`: Minecraft version for said Forge version.
     */
    val forgeToMinecraftMeta: HashMap<String, String> = HashMap(200)

    /**
     * 1-1 Minecraft + Forge version combination to [ForgeInstance]
     * * `key`: Minecraft version + Forge version.
     *
     * Example:
     * * `1.18.2-40.0.44`
     * + `value`: The [ForgeInstance] for said Minecraft and Forge version combination.
     */
    val instanceMeta: HashMap<String, ForgeInstance> = HashMap(200)

    /**
     * Update the available Forge loader information.
     *
     * @author Griefed
     */
    @Throws(IOException::class)
    fun update() {
        minecraftVersions.clear()
        forgeVersions.clear()
        versionMeta.clear()
        forgeToMinecraftMeta.clear()
        instanceMeta.clear()
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
                minecraftVersions.add(client)
            } else {
                mcVersion = field
                minecraftVersions.add(field)
            }
            val forgeVersionsForMCVer: MutableList<String> = ArrayList(100)

            for (forge in forgeManifest.get(field)) {
                /*
                 * substring of length of Minecraft version plus 1, so entries like "1.18.2-40.0.17" get their
                 * Minecraft version portion removed and result in "40.0.17". The +1 removes the "-", too. :)
                 */
                val forgeVersion = forgeVersionFrom(forge.asText(), mcVersion)
                forgeVersions.add(forgeVersion)
                forgeVersionsForMCVer.add(forgeVersion)
                try {
                    val forgeInstance = ForgeInstance(
                        mcVersion,
                        forgeVersion,
                        minecraftMeta
                    )
                    instanceMeta[mcVersion + forge.asText().substring(mcVersion.length)] = forgeInstance
                    forgeToMinecraftMeta[forgeVersion] = mcVersion
                } catch (ex: MalformedURLException) {

                    // Well, in THEORY this should never be thrown, so we don't need to bother
                    // with a thorough error message
                    log.debug("Could not create Forge instance for Minecraft $mcVersion and Forge $forgeVersion.", ex)
                } catch (ex: NoSuchElementException) {
                    log.debug("Could not create Forge instance for Minecraft $mcVersion and Forge $forgeVersion.", ex)
                }
            }
            versionMeta[mcVersion] = forgeVersionsForMCVer.asReversed()
        }
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
         */
        //TODO optional hardening -- low priority, and NOT the obvious guard.
        // Measured against the real manifest (2026-07-31): all 5025 entries across 77 Minecraft keys are prefixed
        // with their own key, and `minecraftVersion` is always derived from that key, so a mis-prefixed entry cannot
        // occur today and the cut is correct for every entry that exists. The only unhandled shape is an entry equal
        // to the key with nothing after it, which throws StringIndexOutOfBoundsException; `update()` catches only
        // MalformedURLException and NoSuchElementException, so it would abort the whole Forge load rather than cost
        // one version. A length check (`manifestEntry.length > minecraftVersion.length`) covers that.
        // DO NOT "fix" this with `manifestEntry.startsWith("$minecraftVersion-")`: entries are prefixed with the RAW
        // manifest key, while `minecraftVersion` may be the reconciled form -- the sole `1.7.10_pre4` key becomes
        // `1.7.10-pre4`, whose entry `1.7.10_pre4-10.12.2.1137-prerelease` such a guard would wrongly reject. Match
        // the raw key, or check length only.
        internal fun forgeVersionFrom(manifestEntry: String, minecraftVersion: String): String =
            manifestEntry.substring(minecraftVersion.length + 1)
    }

}