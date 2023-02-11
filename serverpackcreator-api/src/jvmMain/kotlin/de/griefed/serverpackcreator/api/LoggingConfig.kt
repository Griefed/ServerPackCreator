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
        val sysInfo: HashMap<String, String> = JarUtilities().jarInformation(LoggingConfig::class.java)
        var dev = false
        val path: String
        val properties = Properties()
        val userHome = System.getProperty("user.home")

        this.javaClass.getResourceAsStream("/serverpackcreator.properties").use {
            properties.load(it)
        }

        if (File(userHome, "ServerPackCreator${File.separator}serverpackcreator.properties").isFile
            && File(sysInfo["jarPath"]!!).isFile
        ) {
            File(
                System.getProperty("user.home"),
                "ServerPackCreator${File.separator}serverpackcreator.properties"
            ).inputStream().use {
                properties.load(it)
            }
        } else if (File(sysInfo["jarPath"], "serverpackcreator.properties").isFile
            && File(sysInfo["jarPath"]!!).isFile
        ) {
            File(sysInfo["jarPath"], "serverpackcreator.properties").inputStream().use {
                properties.load(it)
            }
        } else if (File("serverpackcreator.properties").isFile) {
            File("serverpackcreator.properties").inputStream().use {
                properties.load(it)
            }
        }

        val home = if (properties.containsKey("de.griefed.serverpackcreator.home")) {
            File(properties.getProperty("de.griefed.serverpackcreator.home"))
        } else {
            if (File(sysInfo["jarPath"] as String).isDirectory) {
                // Dev environment
                dev = true
                File(File("tests").absolutePath)
            } else {
                File(System.getProperty("user.home"), "ServerPackCreator")
            }
        }
        home.createDirectories(create = true, directory = true)

        if (dev) {
            path = File(home, "tests/logs").absolutePath
            log4jXml = File(home, "tests/log4j2.xml")
        } else {
            path = File(home, "logs").absolutePath
            log4jXml = File(home, "log4j2.xml")
        }
        val oldLogs = "<Property name=\"log-path\">logs</Property>"
        val newLogs = "<Property name=\"log-path\">$path</Property>"
        if (!log4jXml.isFile) {
            try {
                this.javaClass.getResourceAsStream("/log4j2.xml").use {
                    var log4j = it?.readText()
                    if (log4j != null) {
                        log4j = log4j.replace(oldLogs, newLogs)
                    }
                    if (dev) {
                        if (log4j != null) {
                            log4j = log4j.replace(
                                "<Property name=\"log-level-spc\">INFO</Property>",
                                "<Property name=\"log-level-spc\">DEBUG</Property>"
                            )
                        }
                    }
                    if (log4j != null) {
                        log4jXml.writeText(log4j)
                    }
                }
            } catch (ex: IOException) {
                println("Error reading/writing log4j2.xml. $ex")
            }
        }
        System.setProperty("log4j2.formatMsgNoLookups", "true")
    }

    override fun getSupportedTypes() = suffixes


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
        if (log4jXml.isFile) {
            try {
                val custom: CustomXmlConfiguration
                log4jXml.inputStream().use {
                    custom = CustomXmlConfiguration(
                        loggerContext,
                        ConfigurationSource(
                            it,
                            log4jXml
                        )
                    )
                }
                return custom
            } catch (ex: IOException) {
                println("Couldn't parse log4j2.xml. $ex")
            }
        } else if (File(File("").absolutePath, "log4j2.xml").isFile) {
            try {
                val config = File(File("").absolutePath, "log4j2.xml")
                val custom: CustomXmlConfiguration
                config.inputStream().use {
                    custom = CustomXmlConfiguration(
                        loggerContext,
                        ConfigurationSource(
                            it,
                            config
                        )
                    )
                }
                return custom

            } catch (ex: IOException) {
                println("Couldn't parse log4j2.xml. $ex")
            }
        }
        try {
            return CustomXmlConfiguration(
                loggerContext,
                ConfigurationSource(this.javaClass.getResourceAsStream("/log4j2.xml")!!)
            )
        } catch (ex: IOException) {
            println("Couldn't parse log4j2.xml. $ex")
        }
        return CustomXmlConfiguration(loggerContext, source)
    }

    /**
     * Custom XmlConfiguration to pass our custom log4j2.xml config to log4j.
     *
     * @author Griefed
     */
    @Suppress("RedundantOverride")
    inner class CustomXmlConfiguration
    /**
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
        (loggerContext: LoggerContext?, configSource: ConfigurationSource?) :
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