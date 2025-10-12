/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.api.plugins

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.toml.TomlFormat
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.apache.logging.log4j.kotlin.logger
import org.pf4j.Plugin
import org.pf4j.PluginManager
import org.pf4j.PluginRuntimeException
import java.net.URI

/**
 * A ServerPackCreator plugin provides additional functionality to ServerPackCreator via any of
 *
 *  * [de.griefed.serverpackcreator.api.plugins.configurationhandler.ConfigCheckExtension]
 *  * [de.griefed.serverpackcreator.api.plugins.serverpackhandler.PostGenExtension]
 *  * [de.griefed.serverpackcreator.api.plugins.serverpackhandler.PreGenExtension]
 *  * [de.griefed.serverpackcreator.api.plugins.serverpackhandler.PreZipExtension]
 *  * [de.griefed.serverpackcreator.api.plugins.swinggui.ConfigPanelExtension]
 *  * [de.griefed.serverpackcreator.api.plugins.swinggui.TabExtension]
 *
 * and
 *
 *  * [de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionConfigPanel]
 *  * [de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionTab]
 *
 * For details on `pf4j`, the library used to realize the plugin-functionality in ServerPackCreator, visit [pf4j.org](https://pf4j.org)
 *
 * @param context PluginWrapper provided by ServerPackCreator. Do not touch unless you know what you are doing.
 *
 * @author Griefed
 */
@Suppress("unused","MemberVisibilityCanBePrivate")
abstract class ServerPackCreatorPlugin(val context: PluginContext) : Plugin(), BaseInformation {
    private val logger by lazy { cachedLoggerOf(this.javaClass) }
    final override val name: String
    final override val description: String
    final override val author: String
    final override val version: String
    val id: String

    protected val pluginsLog = logger("PluginsLogger")

    init {
        val classPath = this.javaClass.getResource(this.javaClass.simpleName + ".class")!!.toString()
        val url = URI(classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/plugin.toml").toURL()
        val pluginToml: CommentedConfig
        url.openStream().use {
            pluginToml = TomlFormat.instance().createParser().parse(it)
        }
        id = pluginToml.get("id")
        name = pluginToml.get("name")
        description = pluginToml.get("description")
        author = pluginToml.get("author")
        version = pluginToml.get("version")
    }

    /**
     * This method is called by the application when the plugin is started. See [PluginManager.startPlugin].
     *
     * If you intend on overwriting this method, make sure to call `super.start()` first.
     *
     * @throws PluginRuntimeException if something goes wrong.
     * @author Griefed
     */
    @Suppress("DuplicatedCode")
    @Throws(PluginRuntimeException::class)
    override fun start() {
        super.start()
        logger.info("Plugin-ID:          $id")
        logger.info("Plugin-Name:        $name")
        logger.info("Plugin-Description: $description")
        logger.info("Plugin-Author:      $author")
        logger.info("Plugin-Version:     $version")
        logger.info("Started: $name ($id)")
        pluginsLog.info("Plugin-ID:          $id")
        pluginsLog.info("Plugin-Name:        $name")
        pluginsLog.info("Plugin-Description: $description")
        pluginsLog.info("Plugin-Author:      $author")
        pluginsLog.info("Plugin-Version:     $version")
        pluginsLog.info("Started: $name ($id)")
    }

    /**
     * This method is called by the application when the plugin is stopped. See [PluginManager.stopPlugin].
     *
     * If you intend on overwriting this method, make sure to call `super.start()` first.
     *
     * @throws PluginRuntimeException if something goes wrong.
     * @author Griefed
     */
    @Throws(PluginRuntimeException::class)
    override fun stop() {
        super.stop()
        logger.info("Stopped: $name ($id)")
        pluginsLog.info("Stopped: $name ($id)")
    }
}