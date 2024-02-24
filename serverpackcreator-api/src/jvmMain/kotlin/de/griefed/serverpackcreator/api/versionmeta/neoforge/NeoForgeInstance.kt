package de.griefed.serverpackcreator.api.versionmeta.neoforge

import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftClient
import java.net.URL
import java.util.*

interface NeoForgeInstance {
    val minecraftVersion: String
    val neoForgeVersion: String
    val installerUrl: URL

    /**
     * Get this Forge instances corresponding Minecraft client instance, wrapped in an
     * [Optional]
     *
     * @return Client wrapped in an [Optional].
     * @author Griefed
     */
    fun minecraftClient(): Optional<MinecraftClient>
}