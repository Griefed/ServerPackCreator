/*
 * MIT License
 *
 * Copyright (c) 2023 Griefed
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
package de.griefed.example.kotlin.configcheck

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.toml.TomlWriter
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.PackConfig
import de.griefed.serverpackcreator.api.plugins.configurationhandler.ConfigCheckExtension
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.apache.logging.log4j.LogManager
import org.pf4j.Extension
import java.io.StringWriter
import java.util.*

/**
 * Custom configuration check, which runs during a regular server pack configuration check in ServerPackCreator.
 * @author Griefed
 */
@Suppress("unused")
@Extension
class ConfigurationCheck : ConfigCheckExtension {
    private var tomlWriter: TomlWriter? = null
        get() {
            if (field == null) {
                field = TomlWriter()
            }
            return field
        }

    /**
     * @param versionMeta         Instance of [VersionMeta] so you can work with available
     * Minecraft, Forge, Fabric, LegacyFabric and Quilt versions.
     * @param apiProperties       Instance of [ApiProperties] The current configuration of
     * ServerPackCreator, like the default list of clientside-only mods,
     * the server pack directory etc.
     * @param utilities           Instance of [Utilities] commonly used across
     * ServerPackCreator.
     * @param packConfig  The configuration to check.
     * @param encounteredErrors   A list of encountered errors during any and all checks. The list is
     * displayed to the user if it contains any entries.
     * @param pluginConfig         Configuration for this addon, conveniently provided by
     * ServerPackCreator.
     * @param packSpecificConfigs Modpack and server pack specific configurations for this addon,
     * conveniently provided by ServerPackCreator.
     * @return `true` if an error was encountered. `false` if the checks were
     * successful.
     * @throws Exception if any unexpected error is encountered during the execution of this method.
     * @author Griefed
     */
    @Throws(Exception::class)
    override fun runCheck(
        versionMeta: VersionMeta,
        apiProperties: ApiProperties,
        utilities: Utilities,
        packConfig: PackConfig,
        encounteredErrors: MutableList<String>,
        pluginConfig: Optional<CommentedConfig>,
        packSpecificConfigs: ArrayList<CommentedConfig>
    ): Boolean {
        var bool = false
        LOG_ADDONS.info("I am: $name($version) by $author. $description")
        LOG_ADDONS.info("I would provide additional checks for a given server pack configuration!")
        LOG_ADDONS.info(
            "Maybe something based on this server packs Minecraft version ${packConfig.minecraftVersion}?"
        )
        if (pluginConfig.isPresent) {
            LOG_ADDONS.info("I got passed the following configuration:")
            val stringWriter = StringWriter()
            tomlWriter!!.write(pluginConfig.get(), stringWriter)
            LOG_ADDONS.info(stringWriter)
        }
        LOG_ADDONS.info("I got passed the following pack specific configuration(s):")
        for (i in packSpecificConfigs.indices) {
            LOG_ADDONS.info("Configuration $i of ${packSpecificConfigs.size}:")
            val stringWriter = StringWriter()
            tomlWriter!!.write(packSpecificConfigs[i], stringWriter)
            LOG_ADDONS.info(stringWriter)
            if (packSpecificConfigs[i].get<Any>("text").toString().isNotEmpty()) {
                bool = true
                encounteredErrors.add(
                    "Extension ${packSpecificConfigs[i].get<Any>("extension")} encountered an error."
                )
            }
        }
        return bool
    }

    /**
     * Get the if of this extension. Used by ServerPackCreator to determine which configuration, if
     * any, to provide to any given extension being run.
     *
     * @return The ID of this extension.
     * @author Griefed
     */
    override val extensionId: String
        get() = "configcheckexample"

    /**
     * Get the name of this addon.
     *
     * @return The name of this addon.
     * @author Griefed
     */
    override val name: String
        get() = "Example Configuration Check Extension"

    /**
     * Get the description of this addon.
     *
     * @return The description of this addon.
     * @author Griefed
     */
    override val description: String
        get() = "An example for an extension which executes additional configuration checks."

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

    companion object {
        private val LOG_ADDONS = LogManager.getLogger("AddonsLogger")
    }
}