/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.api.versionmeta.minecraft

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.Type
import de.griefed.serverpackcreator.api.versionmeta.forge.ForgeInstance
import de.griefed.serverpackcreator.api.versionmeta.forge.ForgeMeta
import java.net.URL
import java.util.*

/**
 * Relevant information about a given Minecraft client.
 *
 * @author Griefed
 */
class MinecraftClient {
    private val forgeMeta: ForgeMeta
    private val utilities: Utilities
    private val apiProperties: ApiProperties
    val version: String
    val type: Type
    val url: URL
    val minecraftServer: MinecraftServer

    /**
     * Constructor using version, type and url.
     *
     * @param version               The Minecraft version.
     * @param type                  Either [Type.RELEASE] or [Type.SNAPSHOT].
     * @param url                   Url to this versions manifest.
     * @param forgeMeta             To acquire Forge instances for this client version.
     * @param utilities             Commonly used utilities across ServerPackCreator.
     * @param apiProperties ServerPackCreator settings.
     * @author Griefed
     */
    internal constructor(
        version: String,
        type: Type,
        url: URL,
        forgeMeta: ForgeMeta,
        utilities: Utilities,
        apiProperties: ApiProperties
    ) {
        this.apiProperties = apiProperties
        this.utilities = utilities
        this.version = version
        this.type = type
        this.url = url
        this.forgeMeta = forgeMeta
        minecraftServer = MinecraftServer(
            version, type, url, this.utilities,
            this.apiProperties
        )
    }

    /**
     * Constructor using version, type, url and a [MinecraftServer].
     *
     * @param version               The Minecraft version.
     * @param type                  Either [Type.RELEASE] or [Type.SNAPSHOT].
     * @param url                   Url to this versions manifest.
     * @param server                Instance of [MinecraftServer]
     * @param forgeMeta             To acquire Forge instances for this client version.
     * @param utilities             Instance of commonly used utilities.
     * @param apiProperties ServerPackCreator settings.
     * @author Griefed
     */
    internal constructor(
        version: String,
        type: Type,
        url: URL,
        server: MinecraftServer,
        forgeMeta: ForgeMeta,
        utilities: Utilities,
        apiProperties: ApiProperties
    ) {
        this.apiProperties = apiProperties
        this.utilities = utilities
        this.version = version
        this.type = type
        this.url = url
        minecraftServer = server
        this.forgeMeta = forgeMeta
    }

    /**
     * Get the [ForgeInstance] for this client, wrapped in an [Optional].
     *
     * @return Forge instance for this client, wrapped in an [Optional].
     * @author Griefed
     */
    fun forge() = forgeMeta.getForgeInstances(version)
}