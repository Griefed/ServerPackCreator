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
package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionTab
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins that the logger name plugins actually log through reaches `plugins.log`.
 *
 * [ExtensionTab] and `ExtensionConfigPanel` hand every plugin a logger named `AddonsLogger`, and the example
 * plugin's KDoc tells third parties that ServerPackCreator "configures that appender for plugins specifically".
 * It did not: `log4j2.xml` declared only `PluginsLogger`, so `AddonsLogger` fell through to Root and every
 * plugin's output landed in `serverpackcreator.log` instead — the one file a user is told *not* to read when a
 * plugin misbehaves. Nothing failed, which is why it survived: a logger with no configuration is a working
 * logger pointed at the wrong file.
 *
 * Two assertions rather than one, because the runtime check alone would pin the *test* configuration.
 * `src/test/resources/log4j2.xml` shadows the shipped file on the test classpath, so a green runtime assertion
 * says nothing about what users get. The shipped resource is therefore read from disk as well, and both copies
 * are asserted so they cannot drift apart.
 */
internal class PluginLoggingRoutingTest {

    /** Repository root, found by walking up from this module rather than assuming a working directory. */
    private val repositoryRoot: File = generateSequence(File("").absoluteFile) { it.parentFile }
        .firstOrNull { File(it, ".gitignore").isFile && File(it, ".git").exists() }
        ?: File("..").absoluteFile

    /** The logger name `ExtensionTab` hands every plugin. Changing it is a published-API behaviour change. */
    private val pluginLoggerName = "AddonsLogger"

    /** The appender that writes `plugins.log`, and the only appender a plugin's output belongs in. */
    private val pluginsAppenderName = "PluginsLogger"

    /**
     * The name plugins log through resolves to a logger that writes the plugins appender — not to Root.
     *
     * Asserts on the appenders log4j actually resolved, rather than on the logger's name, because a name with no
     * configuration behind it still produces a perfectly functional logger inheriting Root's appenders.
     */
    @Test
    fun pluginLoggerNameWritesThePluginsAppender() {
        val logger = LogManager.getLogger(pluginLoggerName) as Logger
        val appenders = logger.appenders.keys
        Assertions.assertTrue(
            appenders.contains(pluginsAppenderName),
            "Logger '$pluginLoggerName' must write '$pluginsAppenderName', but resolved to $appenders. " +
                    "A plugin logging through ExtensionTab.log lands in serverpackcreator.log instead of plugins.log."
        )
    }

    /**
     * Both the shipped and the test log4j configuration declare the logger, routed to the plugins appender.
     *
     * The shipped file is the one users get and the test file is the only one this JVM reads, so a fix applied
     * to one and not the other is green here and broken in the artifact — or the reverse.
     */
    @Test
    fun bothLog4jConfigurationsDeclareThePluginLogger() {
        for (relativePath in listOf(
            "serverpackcreator-api/src/main/resources/log4j2.xml",
            "serverpackcreator-api/src/test/resources/log4j2.xml"
        )) {
            val configuration = File(repositoryRoot, relativePath)
            Assertions.assertTrue(configuration.isFile, "$relativePath must exist.")
            val declaration = Regex(
                """<Logger\s+name="$pluginLoggerName".*?</Logger>""",
                setOf(RegexOption.DOT_MATCHES_ALL)
            ).find(configuration.readText())
            Assertions.assertNotNull(
                declaration,
                "$relativePath must declare a <Logger name=\"$pluginLoggerName\">; without it plugin output " +
                        "falls through to Root and lands in serverpackcreator.log."
            )
            Assertions.assertTrue(
                declaration!!.value.contains("""ref="$pluginsAppenderName""""),
                "$relativePath declares '$pluginLoggerName' but does not route it to '$pluginsAppenderName'."
            )
        }
    }
}
