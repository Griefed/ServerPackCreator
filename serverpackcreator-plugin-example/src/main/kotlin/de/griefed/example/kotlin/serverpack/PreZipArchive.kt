/*
 * MIT License
 *
 * Copyright (C) 2024 Griefed
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package de.griefed.example.kotlin.serverpack

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.toml.TomlWriter
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.plugins.serverpackhandler.PreZipExtension
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.apache.logging.log4j.LogManager
import org.pf4j.Extension
import java.io.StringWriter
import java.util.*

/**
 * Custom pre-server pack ZIP-creation tasks.
 * @author Griefed
 */
@Suppress("unused")
@Extension
class PreZipArchive : PreZipExtension {
    private val pluginsLog = LogManager.getLogger("AddonsLogger")
    private var tomlWriter: TomlWriter = TomlWriter()

    /**
     * @param versionMeta         Instance of [VersionMeta] so you can work with available
     * Minecraft, Forge, Fabric, LegacyFabric and Quilt versions.
     * @param utilities           Instance of [Utilities] commonly used across
     * ServerPackCreator.
     * @param apiProperties       Instance of [ApiProperties] as ServerPackCreator itself uses
     * it.
     * @param packConfig  Instance of [PackConfig] for a given server pack.
     * @param destination         String. The destination of the server pack.
     * @param pluginConfig         Configuration for this addon, conveniently provided by
     * ServerPackCreator.
     * @param packSpecificConfigs Modpack and server pack specific configurations for this addon,
     * conveniently provided by ServerPackCreator.
     * @throws Exception [Exception] when an uncaught error occurs in the addon.
     * @author Griefed
     */
    @Throws(Exception::class)
    override fun run(
        versionMeta: VersionMeta,
        utilities: Utilities,
        apiProperties: ApiProperties,
        packConfig: PackConfig,
        destination: String,
        pluginConfig: Optional<CommentedConfig>,
        packSpecificConfigs: ArrayList<CommentedConfig>
    ) {
        pluginsLog.info("I am: $name($version) by $author. $description")
        pluginsLog.info("Running with ServerPackCreator ${apiProperties.apiVersion}")
        pluginsLog.info("I would do stuff right before a ZIP-archive of the server pack is created!")
        pluginsLog.info("I could make some magic happen in the $destination-directory :-)")
        pluginsLog.info("Maybe something based on this server packs Minecraft version ${packConfig.minecraftVersion}?")
        if (pluginConfig.isPresent) {
            pluginsLog.info("I got passed the following configuration:")
            val stringWriter = StringWriter()
            tomlWriter.write(pluginConfig.get(), stringWriter)
            pluginsLog.info(stringWriter)
        }
        pluginsLog.info("I got passed the following pack specific configuration(s):")
        for (i in packSpecificConfigs.indices) {
            pluginsLog.info("Configuration $i of ${packSpecificConfigs.size}:")
            val stringWriter = StringWriter()
            tomlWriter.write(packSpecificConfigs[i], stringWriter)
            pluginsLog.info(stringWriter)
        }
    }

    /**
     * Get the ID of this extension. Used by ServerPackCreator to determine which configuration, if
     * any, to provide to any given extension being run.
     *
     * @return The ID of this extension.
     * @author Griefed
     */
    override val extensionId: String
        get() = "prezipexample"

    /**
     * Get the name of this addon.
     *
     * @return The name of this addon.
     * @author Griefed
     */
    override val name: String
        get() = "Example Pre ZIP-Archive Creation Extension"

    /**
     * Get the description of this addon.
     *
     * @return The description of this addon.
     * @author Griefed
     */
    override val description: String
        get() = "An example for an extension which executes right before the creation of a ZIP-archive of a server pack is started."

    /**
     * Get the author of this addon.
     *
     * @return The author of this addon.
     * @author Griefed
     */
    override val author: String
        get() = "Griefed"

    /**
     * Get the version of this addon.
     *
     * @return The version of this addon.
     * @author Griefed
     */
    override val version: String
        get() = "0.0.1-SNAPSHOT"
}