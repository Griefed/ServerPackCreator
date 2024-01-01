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
package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.utilities.common.JarInformation
import de.griefed.serverpackcreator.api.utilities.common.JarUtilities
import de.griefed.serverpackcreator.api.utilities.common.createDirectories
import de.griefed.serverpackcreator.api.utilities.common.readText
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.ConfigurationFactory
import org.apache.logging.log4j.core.config.ConfigurationSource
import org.apache.logging.log4j.core.config.Order
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.xml.XmlConfiguration
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Custom logging configuration setup to prevent incorrect log-directories when executing
 * ServerPackCreator from CLI from a completely other directory. Or possibly when using symlinks,
 * too. This class prevents the logs being written to the `logs`-directory inside the
 * directory from which SPC is being run from.
 *
 * @author Griefed
 */
@Suppress("unused")
@Plugin(name = "ServerPackCreatorConfigFactory", category = "ConfigurationFactory")
@Order(50)
class LoggingConfig : ConfigurationFactory() {
    private val suffixes = arrayOf(".xml")
    private val log4jXml: File

    /**
     * Check possible home-directories for a viable `serverpackcreator.properties` and check
     * whether the `de.griefed.serverpackcreator.home`-property is available. If it is, then use
     * said directory to create the log4j config if it does not already exist, with the path to the
     * logs-directory being set within the aforementioned home-directory.
     *
     * @author Griefed
     */
    init {
        System.setProperty("log4j2.formatMsgNoLookups", "true")
        val serverPackCreatorProperties = "serverpackcreator.properties"
        val jarInformation = JarInformation(this.javaClass, JarUtilities())
        var isDevVersion = false
        val logDirPath: String
        val props = Properties()
        val userHome = System.getProperty("user.home")
        var log4j: String

        val jarFolderFile = File(jarInformation.jarFolder.absoluteFile, serverPackCreatorProperties).absoluteFile
        val serverPackCreatorHomeDir = File(userHome, "ServerPackCreator").absoluteFile
        val homeDirFile = File(serverPackCreatorHomeDir,serverPackCreatorProperties).absoluteFile
        val relativeDirFile = File(serverPackCreatorProperties).absoluteFile
        val overrideProperties = File(jarInformation.jarFolder.absoluteFile, "overrides.properties")

        // Load the properties file from the classpath, providing default values.
        try {
            this.javaClass.getResourceAsStream("/$serverPackCreatorProperties").use {
                props.load(it)
            }
            println("Loaded properties from classpath.")
        } catch (ex: Exception) {
            println("Couldn't read properties from classpath.")
            ex.printStackTrace()
        }

        // If our properties-file exists in SPCs home directory, load it.
        loadFile(jarFolderFile, props)
        // If our properties-file exists in the users home dir ServerPackCreator-dir, load it.
        loadFile(homeDirFile, props)
        // If our properties-file in the directory from which the user is executing SPC exists, load it.
        loadFile(relativeDirFile, props)
        // If an overrides-file exists, load it
        loadFile(overrideProperties,props)

        val home = if (props.containsKey("de.griefed.serverpackcreator.home")) {
            File(props.getProperty("de.griefed.serverpackcreator.home"))
        } else {
            if (jarInformation.jarPath.toFile().isDirectory) {
                // Dev environment
                isDevVersion = true
                File("").absoluteFile
            } else {
                File(userHome, "ServerPackCreator")
            }
        }
        home.createDirectories(create = true, directory = true)

        logDirPath = File(home, "logs").absolutePath
        log4jXml = File(home, "log4j2.xml")

        val oldLogs = "<Property name=\"log-path\">logs</Property>"
        val newLogs = "<Property name=\"log-path\">$logDirPath</Property>"
        if (!log4jXml.isFile) {
            try {
                this.javaClass.getResourceAsStream("/log4j2.xml").use {
                    log4j = it?.readText().toString()
                    log4j = log4j.replace(oldLogs, newLogs)
                    if (isDevVersion) {
                        log4j = log4j.replace(
                            "<Property name=\"log-level-spc\">INFO</Property>",
                            "<Property name=\"log-level-spc\">DEBUG</Property>"
                        )
                    }
                    log4jXml.writeText(log4j)
                }
            } catch (ex: IOException) {
                println("Error reading/writing log4j2.xml.")
                ex.printStackTrace()
            }
        }
    }

    override fun getSupportedTypes() = suffixes

    /**
     * Load the [propertiesFile] into the provided [props]
     *
     * @author Griefed
     */
    private fun loadFile(propertiesFile: File, props: Properties) {
        if (!propertiesFile.isFile) {
            println("Properties-file does not exist: ${propertiesFile.absolutePath}.")
            return
        }
        try {
            propertiesFile.inputStream().use {
                props.load(it)
            }
            println("Loaded properties from $propertiesFile.")
        } catch (ex: Exception) {
            println("Couldn't read properties from ${propertiesFile.absolutePath}.")
            ex.printStackTrace()
        }
    }


    /**
     * Depending on whether this is the first run of ServerPackCreator on a users machine, the default
     * log4j2 configuration may be present at different locations. The default one is the config
     * inside the home-directory of SPC, of which we will try to set up our logging with. If said file
     * fails for whatever reason, we will try to use a config inside the directory from which SPC was
     * executed. Should that fail, too, the config from the classpath is used, to ensure we always
     * have default configs available. Should that fail, too, though, log4j is set up with its own
     * default settings.
     *
     * @param loggerContext logger context passed from log4j itself
     * @param source        configuration source passed from log4j itself. Attempts to overwrite it
     * are made, but if all else fails it is used to set up logging with log4j's
     * default config.
     * @return Custom configuration with proper logs-directory set.
     * @author Griefed
     */
    override fun getConfiguration(loggerContext: LoggerContext, source: ConfigurationSource): Configuration {
        val config = File(File("").absolutePath, "log4j2.xml")
        val configSource: ConfigurationSource
        if (log4jXml.isFile) {
            try {
                return getXmlConfig(log4jXml, loggerContext)
            } catch (ex: IOException) {
                println("Couldn't parse $log4jXml.")
                ex.printStackTrace()
            }
        } else if (config.isFile) {
            try {
                return getXmlConfig(config, loggerContext)
            } catch (ex: IOException) {
                println("Couldn't parse $config.")
                ex.printStackTrace()
            }
        }
        try {
            configSource = ConfigurationSource(this.javaClass.getResourceAsStream("/log4j2.xml")!!)
            return CustomXmlConfiguration(loggerContext, configSource)
        } catch (ex: IOException) {
            println("Couldn't parse resource log4j2.xml.")
            ex.printStackTrace()
        }
        return CustomXmlConfiguration(loggerContext, source)
    }

    /**
     * @author Griefed
     */
    private fun getXmlConfig(sourceFile: File, loggerContext: LoggerContext): CustomXmlConfiguration {
        val configSource: ConfigurationSource
        val stream = sourceFile.inputStream()
        configSource = ConfigurationSource(stream, sourceFile)
        val custom = CustomXmlConfiguration(loggerContext, configSource)
        stream.close()
        return custom
    }

    /**
     * Custom XmlConfiguration to pass our custom log4j2.xml config to log4j.
     *
     * Set up the XML configuration with the passed context and config source. For the config source
     * being used, [LoggingConfig.getConfiguration] where
     * multiple attempts at creating a new CustomXmlConfiguration using our own log4j2.xml are made
     * before the default log4j setup is used.
     *
     * @param loggerContext logger context passed from log4j itself
     * @param configSource  configuration source passed from
     * [LoggingConfig.getConfiguration].
     * @author Griefed
     */
    @Suppress("RedundantOverride")
    inner class CustomXmlConfiguration(loggerContext: LoggerContext?, configSource: ConfigurationSource?) :
        XmlConfiguration(loggerContext, configSource) {

        /**
         * For now, all this does is call the [XmlConfiguration.doConfigure]-method to set up the
         * configuration with the passed source from the constructor. Custom values and settings can be
         * set here in the future, should a need arise to do so.
         *
         * @author Griefed
         */
        override fun doConfigure() = super.doConfigure()
    }
}