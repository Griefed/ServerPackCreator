/* Copyright (C) 2023  Griefed
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
import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftMeta
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.w3c.dom.Document
import java.io.File
import java.io.IOException
import java.net.MalformedURLException

/**
 * Information about available NeoForge loader versions in correlation to Minecraft versions.
 *
 * @param loaderManifest Node containing information about available NeoForge versions.
 * @param utilities      Commonly used utilities across ServerPackCreator.
 * @param minecraftMeta  Meta for retroactively updating the previously passed meta.
 *
 * @author Griefed
 */
internal class NeoForgeLoader(
    private val loaderManifest: File,
    private val utilities: Utilities,
    private val minecraftMeta: MinecraftMeta
) {
    private val log = cachedLoggerOf(this.javaClass)
    val minecraftVersions: MutableList<String> = ArrayList(100)
    val neoForgeVersions: MutableList<String> = ArrayList(100)
    private val version = "version"

    /**
     * 1-n Minecraft version to NeoForge versions.
     * * `key`: Minecraft version.
     * * `value`: List of NeoForge versions for said Minecraft versions.
     */
    val versionMeta: HashMap<String, List<String>> = HashMap(200)

    /**
     * 1-1 NeoForge version to Minecraft version
     * * `key`: NeoForge version.
     * * `value`: Minecraft version for said NeoForge version.
     */
    val neoForgeToMinecraftMeta: HashMap<String, String> = HashMap(200)

    /**
     * 1-1 Minecraft + NeoForge version combination to [NeoForgeInstance]
     * * `key`: Minecraft version + NeoForge version.
     *
     * Example:
     * * `1.18.2-40.0.44`
     * + `value`: The [NeoForgeInstance] for said Minecraft and NeoForge version combination.
     */
    val instanceMeta: HashMap<String, NeoForgeInstance> = HashMap(200)

    /**
     * Update the available NeoForge loader information.
     *
     * @author Griefed
     */
    @Throws(IOException::class)
    fun update() {
        minecraftVersions.clear()
        neoForgeVersions.clear()
        versionMeta.clear()
        neoForgeToMinecraftMeta.clear()
        instanceMeta.clear()

        val document: Document = utilities.xmlUtilities.getXml(loaderManifest)
        val elements = document.getElementsByTagName(version)
        val neoForgeVersionsForMCVer: MutableList<String> = ArrayList(100)
        for (i in 0 until elements.length) {
            val node = elements.item(i)
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
                minecraftVersions.add(mcVersion)
            }
            neoForgeVersions.add(neoForgeVersion)
            neoForgeVersionsForMCVer.add(neoForgeVersion)
            try {
                val neoForgeInstance = NeoForgeInstance(
                    mcVersion,
                    neoForgeVersion,
                    minecraftMeta
                )
                instanceMeta["$mcVersion-$neoForgeVersion"] = neoForgeInstance
                neoForgeToMinecraftMeta[neoForgeVersion] = mcVersion
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
            versionMeta[mcVersion] = neoForgeVersionsForMCVer
        }
    }
}