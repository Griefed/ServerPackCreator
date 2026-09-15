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
package de.griefed.serverpackcreator.api.versionmeta.neoforge

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.toDotEscapedRegex
import de.griefed.serverpackcreator.api.versionmeta.VersionMetaConfig
import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftMeta
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.w3c.dom.Document
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.util.Collections

/**
 * Information about available NeoForge loader versions in correlation to Minecraft versions.
 *
 * @param oldNeoForgeManifest Node containing information about available NeoForge versions.
 * @param utilities      Commonly used utilities across ServerPackCreator.
 * @param minecraftMeta  Meta for retroactively updating the previously passed meta.
 *
 * @author Griefed
 */
internal class NeoForgeLoader(
    private val oldNeoForgeManifest: File,
    private val newNeoForgeManifest: File,
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
    var neoForgeVersions: List<String> = emptyList()
        private set
    private val version = VersionMetaConfig.TAG_VERSION

    /**
     * 1-n Minecraft version to NeoForge versions.
     * * `key`: Minecraft version.
     * * `value`: List of NeoForge versions for said Minecraft versions.
     */
    @Volatile
    var versionMeta: Map<String, List<String>> = emptyMap()
        private set

    /**
     * 1-1 NeoForge version to Minecraft version
     * * `key`: NeoForge version.
     * * `value`: Minecraft version for said NeoForge version.
     */
    @Volatile
    var neoForgeToMinecraftMeta: Map<String, String> = emptyMap()
        private set

    /**
     * 1-1 Minecraft + NeoForge version combination to [NeoForgeInstance]
     * * `key`: Minecraft version + NeoForge version.
     *
     * Example:
     * * `1.18.2-40.0.44`
     * + `value`: The [NeoForgeInstance] for said Minecraft and NeoForge version combination.
     */
    @Volatile
    var instanceMeta: Map<String, NeoForgeInstance> = emptyMap()
        private set

    /**
     * Update the available NeoForge loader information.
     *
     * @author Griefed
     */
    @Throws(IOException::class)
    fun update() {
        val nextMinecraftVersions = ArrayList<String>(100)
        val nextNeoForgeVersions = ArrayList<String>(100)
        val nextVersionMeta = HashMap<String, List<String>>(200)
        val nextNeoForgeToMinecraftMeta = HashMap<String, String>(200)
        val nextInstanceMeta = HashMap<String, NeoForgeInstance>(200)

        val oldNeoDocument: Document = utilities.xmlUtilities.getXml(oldNeoForgeManifest)
        val oldNeoElements = oldNeoDocument.getElementsByTagName(version)
        val oldNeoForgeVersionsForMCVer: MutableList<String> = ArrayList(100)
        for (i in 0 until oldNeoElements.length) {
            val node = oldNeoElements.item(i)
            val children = node.childNodes
            val item = children.item(0)
            val combination = item.nodeValue.split("-")
            if (combination.size == 1) {
                // What the hell, NeoForge
                // https://github.com/Griefed/ServerPackCreator/issues/489
                continue
            }
            val mcVersion = combination[0]
            val neoForgeVersion = combination[1]

            if (!minecraftVersions.contains(mcVersion)) {
                nextMinecraftVersions.add(mcVersion)
            }
            nextNeoForgeVersions.add(neoForgeVersion)
            oldNeoForgeVersionsForMCVer.add(neoForgeVersion)
            try {
                val neoForgeInstance = OldNeoForgeInstance(
                    mcVersion,
                    neoForgeVersion,
                    minecraftMeta
                )
                nextInstanceMeta["$mcVersion-$neoForgeVersion"] = neoForgeInstance
                nextNeoForgeToMinecraftMeta[neoForgeVersion] = mcVersion
            } catch (ex: MalformedURLException) {

                // Well, in THEORY this should never be thrown, so we don't need to bother
                // with a thorough error message
                log.debug(
                    "Could not create NeoForge instance for Minecraft $mcVersion and NeoForge $neoForgeVersion.",
                    ex
                )
            } catch (ex: NoSuchElementException) {
                log.debug(
                    "Could not create NeoForge instance for Minecraft $mcVersion and NeoForge $neoForgeVersion.",
                    ex
                )
            }
            nextVersionMeta[mcVersion] = oldNeoForgeVersionsForMCVer
        }

        val newNeoDocument: Document = utilities.xmlUtilities.getXml(newNeoForgeManifest)
        val newNeoElements = newNeoDocument.getElementsByTagName(version)
        for (mcVersion in minecraftMeta.allVersions().map { it.version }) {
            val mcVersionRegex = neoForgeVersionPatternFor(mcVersion) ?: continue
            val newNeoForgeVersionsForMCVer: MutableList<String> = ArrayList(100)

            for (i in 0 until newNeoElements.length) {
                val node = newNeoElements.item(i)
                val children = node.childNodes
                val item = children.item(0)
                val neoForgeVersion = item.nodeValue.toString()

                if (neoForgeVersion.matches(mcVersionRegex)) {
                    if (!minecraftVersions.contains(mcVersion)) {
                        nextMinecraftVersions.add(mcVersion)
                    }
                    if (!neoForgeVersions.contains(neoForgeVersion)) {
                        nextNeoForgeVersions.add(neoForgeVersion)
                    }
                    if (!newNeoForgeVersionsForMCVer.contains(neoForgeVersion)) {
                        newNeoForgeVersionsForMCVer.add(neoForgeVersion)
                    }

                    try {
                        val neoForgeInstance = NewNeoForgeInstance(
                            mcVersion,
                            neoForgeVersion,
                            minecraftMeta
                        )
                        nextInstanceMeta["$mcVersion-$neoForgeVersion"] = neoForgeInstance
                        nextNeoForgeToMinecraftMeta[neoForgeVersion] = mcVersion
                    } catch (ex: MalformedURLException) {

                        // Well, in THEORY this should never be thrown, so we don't need to bother
                        // with a thorough error message
                        log.debug(
                            "Could not create NeoForge instance for Minecraft $mcVersion and NeoForge $neoForgeVersion.",
                            ex
                        )
                    } catch (ex: NoSuchElementException) {
                        log.debug(
                            "Could not create NeoForge instance for Minecraft $mcVersion and NeoForge $neoForgeVersion.",
                            ex
                        )
                    }
                }
            }
            if (newNeoForgeVersionsForMCVer.isNotEmpty()) {
                nextVersionMeta[mcVersion] = newNeoForgeVersionsForMCVer
            }
        }

        // Reversed on the builder, before publication -- the old code walked the published map's entries
        // while writing back into it, which is a mutation a concurrent reader could observe half-applied.
        for (key in nextVersionMeta.keys.toList()) {
            nextVersionMeta[key] = nextVersionMeta.getValue(key).reversed()
        }

        // Published in one assignment each, as unmodifiable views.
        minecraftVersions = Collections.unmodifiableList(nextMinecraftVersions)
        neoForgeVersions = Collections.unmodifiableList(nextNeoForgeVersions)
        versionMeta = Collections.unmodifiableMap(nextVersionMeta)
        neoForgeToMinecraftMeta = Collections.unmodifiableMap(nextNeoForgeToMinecraftMeta)
        instanceMeta = Collections.unmodifiableMap(nextInstanceMeta)
}

    internal companion object {
        /**
         * Translate a Minecraft version into the pattern that NeoForge versions for it must **fully** match, or
         * `null` when the version can carry no NeoForge builds at all and must be skipped.
         *
         * NeoForge encodes the Minecraft version it targets into its own version, in two different schemes:
         * for Minecraft `1.x[.y]` it drops the leading `1.` (Minecraft `1.21.1` → NeoForge `21.1.247`, and
         * Minecraft `1.21` → NeoForge `21.0.167`, where the omitted patch becomes an explicit `0`), while for the
         * newer `YY.x[.y]` Minecraft scheme it keeps the version as-is (Minecraft `26.1.2` → `26.1.2.93`, and
         * Minecraft `26.2` → `26.2.0.40-beta`, again with the omitted patch as `0`). Anything that is not a plain
         * release — snapshots (`24w14a`), pre-releases, release candidates — never has NeoForge builds and yields
         * `null`.
         *
         * Extracted from the loader's parse loop so it can be unit-tested: a wrong mapping here does not fail
         * loudly, it silently produces empty or mis-assigned NeoForge version lists.
         */
        internal fun neoForgeVersionPatternFor(minecraftVersion: String): Regex? = when {
            minecraftVersion.matches("1\\.\\d+".toRegex()) ->
                "^${minecraftVersion.substring(2).toDotEscapedRegex()}\\.0(?:\\.\\d+)+(?:-.*)?".toRegex()

            minecraftVersion.matches("1\\.\\d+(.\\d+)?".toRegex()) ->
                "^${minecraftVersion.substring(2).toDotEscapedRegex()}(?:\\.\\d+)+(?:-.*)?".toRegex()

            minecraftVersion.matches("\\d{2}.\\d+".toRegex()) ->
                "^${minecraftVersion.toDotEscapedRegex()}\\.0(?:\\.\\d+)+(?:-.*)?".toRegex()

            minecraftVersion.matches("\\d{2}.\\d+.\\d+".toRegex()) ->
                "^${minecraftVersion.toDotEscapedRegex()}(?:\\.\\d+)+(?:-.*)?".toRegex()

            else -> null
        }
    }
}
