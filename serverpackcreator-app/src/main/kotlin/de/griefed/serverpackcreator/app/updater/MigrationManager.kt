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
package de.griefed.serverpackcreator.app.updater

import Translations
import com.electronwill.nightconfig.toml.TomlParser
import de.comahe.i18n4k.Locale
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.filteredWalk
import de.griefed.serverpackcreator.api.utilities.common.readText
import net.lingala.zip4j.ZipFile
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*

/**
 * The migration manager of ServerPackCreator is responsible for determining update-steps between a
 * given old version and a given new version. The determined steps between updates are then executed
 * in order to ensure a users environment is up-to-date, even when skipping multiple versions when
 * updating.
 *
 *
 * This does not guarantee a safe update when updating between major versions, as major versions
 * tend to contain breaking changes, and depending on those changes there will still be a need for
 * the user to ensure they can safely update. One example would be major version 3 to major version
 * 4, where the Java version required by ServerPackCreator will rise from Java 8 to Java 17 or
 * later.<br></br> Aas this is not something ServerPackCreator can take care of, it is a good example for
 * a migration which this manager can not take care of.
 *
 *
 * Other migrations, such as updating the `log4j2.xml` and the logs-directory inside said file
 * can and will be taken care of.
 *
 *
 * Some migrations will require the user to restart ServerPackCreator for the executed migrations to
 * take full effect. A given migration method which executes such migrations should register a
 * migration message informing the user about the need to restart SPC.
 *
 *
 * Migration messages are displayed in a dialog when using the GUI as well as printed to the
 * serverpackcreator.log.
 *
 * @author Griefed
 */
@Suppress("unused")
class MigrationManager(
    private val apiProperties: ApiProperties, private val tomlParser: TomlParser
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val migrationMethods = MigrationMethods()
    private val previous: String = apiProperties.oldVersion()
    private val current: String = apiProperties.apiVersion
    private val alphaBetaDev = ".*(alpha|beta|dev).*".toRegex()
    private val release = "[0-9]+.[0-9]+.[0-9]+".toRegex()
    val migrationMessages: MutableList<MigrationMessage> = ArrayList(10)

    /**
     * Perform necessary migrations from the old version to the current version being used.
     *
     * @author Griefed
     */
    fun migrate() {
        if (previous.isEmpty()) {
            log.info("No old version received. Assuming first time run.")
            apiProperties.setOldVersion(current)
            return
        }
        if (current.matches(alphaBetaDev)) {
            log.info("No migrations to execute. Running alpha, beta or dev version.")
            return
        }
        if (previous.matches(alphaBetaDev)) {
            log.info("No migrations to execute. Upgrading from alpha, beta or dev version.")
            return
        }
        if (!current.matches(release)) {
            log.info("No migrations to execute. Running development branch.")
            return
        }
        if (isOlder(previous, current)) {
            log.info("No migrations to execute. User went back a version. From $previous to $current.")
            return
        }
        if (previous == current) {
            log.info("No migrations to execute. User has not updated.")
        }
        val migrationMethods = getMigrationMethods(previous, current)
        if (migrationMethods.isNotEmpty()) {
            for (method in migrationMethods) {
                log.info("Resolving migrations for: ${toSemantic(method!!.name)}")
                try {
                    method.isAccessible = true
                    method.invoke(this.migrationMethods)
                } catch (ex: IllegalAccessException) {
                    log.error(
                        "Could not access migration-method: ${method.name}. Please report this on GitHub and include the logs!",
                        ex
                    )
                } catch (ex: InvocationTargetException) {
                    log.error(
                        "Could not invoke migration-method: ${method.name}. Please report this on GitHub and include the logs!",
                        ex
                    )
                }
            }
        } else {
            log.info("No migrations to execute.")
        }
        apiProperties.setOldVersion(current)
    }

    /**
     * Check if the first version is older than the second one.
     *
     * @param old          The old version used.
     * @param checkAgainst The current version being used.
     * @return `true` if the current version is older than the previously used version.
     * @author Griefed
     */
    private fun isOlder(old: String, checkAgainst: String): Boolean {
        return older(
            semantics(old), semantics(checkAgainst)
        )
    }

    /**
     * Acquire the list of migration-methods to execute based on the current version being used. All
     * methods which match a version newer than the passed one are executed.
     *
     * @param oldVersion The current version being used.
     * @return Methods to execute to ensure proper migration between version updates.
     * @author Griefed
     */
    private fun getMigrationMethods(oldVersion: String, currentVersion: String): List<Method?> {
        val run: MutableList<Method?> = ArrayList(100)
        val methods = migrationMethods.javaClass.declaredMethods
        val methodMap = HashMap<String, Method>(100)
        val methodVersions = TreeSet<String>()
        for (method in methods) {
            val methodVersion = toSemantic(method.name.replace("\\\$[0-9]*lambda\\\$[0-9]*".toRegex(),""))
            methodMap[methodVersion] = method
            methodVersions.add(methodVersion)
        }
        for (methodVersion in methodVersions) {
            if (isNewer(
                    oldVersion, methodVersion
                ) && isOlderOrSame(
                    currentVersion, methodVersion
                )
            ) {
                run.add(methodMap[methodVersion])
            }
        }
        return run
    }

    /**
     * Convert a text-based version to a semantic representation, usable in version comparisons.
     *
     * @param textVersion The text-based version to convert to the semantic format.
     * @return Semantic representation of the text-based version.
     * @author Griefed
     */
    private fun toSemantic(textVersion: String): String {
        return textVersion.replace(
            "Zero", "0"
        ).replace(
            "One", "1"
        ).replace(
            "Two", "2"
        ).replace(
            "Three", "3"
        ).replace(
            "Four", "4"
        ).replace(
            "Five", "5"
        ).replace(
            "Six", "6"
        ).replace(
            "Seven", "7"
        ).replace(
            "Eight", "8"
        ).replace(
            "Nine", "9"
        ).replace(
            "Point", "."
        )
    }

    /**
     * Compare two integer arrays of semantic version against each other and determine whether we have
     * an old version at hand.
     *
     * @param old          The old version numbers.
     * @param checkAgainst The new version numbers to check whether they represent an older version.
     * @return `true` if the version numbers checked against represent an older version.
     * @author Griefed
     */
    private fun older(old: IntArray, checkAgainst: IntArray): Boolean {
        // Current MAJOR version smaller?
        if (checkAgainst[0] < old[0]) {
            return true
        }

        // Current MAJOR version equal and current MINOR version smaller?
        return if (checkAgainst[0] == old[0] && checkAgainst[1] < old[1]) {
            true

            // Current MAJOR version equal, current MINOR equal, current PATCH version smaller?
        } else checkAgainst[0] == old[0] && checkAgainst[1] == old[1] && checkAgainst[2] < old[2]
    }

    /**
     * Get the major, minor and patch numbers of a version.
     *
     * @param version The version of which to get the major, minor and patch number array.
     * @return Array containing the major, minor and patch numbers.
     * @author Griefed
     */
    private fun semantics(version: String): IntArray {
        return version.replace("\\\$[0-9]*lambda\\\$[0-9]*".toRegex(),"").split(Regex("\\.")).map { it.toInt() }.toIntArray()
    }

    /**
     * Check if the first version is newer than the second one.
     *
     * @param old          The current version being used.
     * @param checkAgainst The version the migration-method represents.
     * @return `true` if the migration-method version is newer than the current version.
     * @author Griefed
     */
    private fun isNewer(old: String, checkAgainst: String): Boolean {
        return newer(
            semantics(old), semantics(checkAgainst)
        )
    }

    /**
     * Check if the first version is older than or the same as the second one.
     *
     * @param old          The old version used.
     * @param checkAgainst The current version being used.
     * @return `true` if the current version is older than or the same as the previously used
     * version.
     * @author Griefed
     */
    private fun isOlderOrSame(old: String, checkAgainst: String): Boolean {
        return oldOrSame(
            semantics(old), semantics(checkAgainst)
        )
    }

    /**
     * Compare two integer arrays of semantic version against each other and determine whether we have
     * a new version at hand.
     *
     * @param old          The old version numbers.
     * @param checkAgainst The new version numbers to check whether they represent a newer version.
     * @return `true` if the version numbers checked against represent a newer version.
     * @author Griefed
     */
    private fun newer(old: IntArray, checkAgainst: IntArray): Boolean {
        // Method MAJOR bigger?
        if (checkAgainst[0] > old[0]) {
            return true
        }

        // Method MAJOR version equal and method MINOR bigger?
        return if (checkAgainst[0] == old[0] && checkAgainst[1] > old[1]) {
            true
        } else {
            // Method MAJOR equal, method MINOR equal, method PATCH bigger?
            checkAgainst[0] == old[0] && checkAgainst[1] == old[1] && checkAgainst[2] > old[2]
        }
    }

    /**
     * Compare two integer arrays of semantic version against each other and determine whether we have
     * an old version or the same one at hand.
     *
     * @param old          The old version numbers.
     * @param checkAgainst The new version numbers to check whether they represent an older version.
     * @return `true` if the version numbers checked against represent an older version or the
     * same.
     * @author Griefed
     */
    private fun oldOrSame(old: IntArray, checkAgainst: IntArray): Boolean {
        return checkAgainst[0] <= old[0] && checkAgainst[1] <= old[1] && checkAgainst[2] <= old[2]
    }

    /**
     * Check if the first version is newer than or the same as the second one.
     *
     * @param current      The old version used.
     * @param checkAgainst The current version being used.
     * @return `true` if the current version is newer than or the same as the previously used
     * version.
     * @author Griefed
     */
    private fun isNewerOrSame(current: String, checkAgainst: String): Boolean {
        return newOrSame(semantics(current), semantics(checkAgainst))
    }

    /**
     * Compare two integer arrays of semantic version against each other and determine whether we have
     * a new version or the same one at hand.
     *
     * @param current      The old version numbers.
     * @param checkAgainst The new version numbers to check whether they represent an older version.
     * @return `true` if the version numbers checked against represent a newer version or the
     * same.
     * @author Griefed
     */
    private fun newOrSame(current: IntArray, checkAgainst: IntArray): Boolean {
        return checkAgainst[0] >= current[0] && checkAgainst[1] >= current[1] && checkAgainst[2] >= current[2]
    }

    /**
     * Convert a semantic version to text-based representation.
     *
     * @param version The version to convert.
     * @return The semantic version converted to text-based representation.
     * @author Griefed
     */
    private fun toText(version: String): String {
        val textVersion = StringBuilder()
        for (character in version.toCharArray()) {
            when (character.toString()) {
                "0" -> textVersion.append("Zero")
                "1" -> textVersion.append("One")
                "2" -> textVersion.append("Two")
                "3" -> textVersion.append("Three")
                "4" -> textVersion.append("Four")
                "5" -> textVersion.append("Five")
                "6" -> textVersion.append("Six")
                "7" -> textVersion.append("Seven")
                "8" -> textVersion.append("Eight")
                "9" -> textVersion.append("Nine")
                "." -> textVersion.append("Point")
            }
        }
        return textVersion.toString()
    }

    /**
     * A migration message include any and all information for a particular migration which has taken
     * place during the startup of ServerPackCreator. It contains the
     *
     * @author Griefed
     */
    inner class MigrationMessage(
        private val fromVersion: String, private val toVersion: String, private val changes: MutableList<String> = ArrayList(20)
    ) {

        fun fromVersion(): String {
            return fromVersion
        }

        fun toVersion(): String {
            return toVersion
        }

        fun changes(): List<String> {
            return changes
        }

        fun count(): Int {
            return changes.size
        }

        fun get(): String {
            return toString()
        }

        override fun toString(): String {
            val header = "From $fromVersion to $toVersion the following changes were made:\n"
            val content = StringBuilder()
            content.append(header).append("\n")
            for (i in changes.indices) {
                content.append("  (").append(i + 1).append("): ").append(changes[i]).append("\n")
            }
            return content.toString()
        }
    }

    /**
     * Inner class which holds all methods responsible for performing migration-steps.
     * **ONLY** methods which perform migration steps are allowed here!
     *
     * @author Griefed
     */
    @Suppress("FunctionName")
    private inner class MigrationMethods {
        /**
         * Migrate the log4j2.xml to the new settings due to home-directory preparations.
         *
         * @author Griefed
         */
        private fun ThreePointOneFivePointZero() {
            try {
                val changes: MutableList<String> = ArrayList(10)
                val log4J2Xml = File(
                    apiProperties.homeDirectory, "log4j2.xml"
                )
                val oldLogs = "<Property name=\"log-path\">logs</Property>"
                val newLogs = ("<Property name=\"log-path\">${apiProperties.logsDirectory}</Property>")
                var log4j: String = log4J2Xml.readText(Charsets.UTF_8)
                var changed = false
                if (log4j.contains(oldLogs)) {
                    changed = true
                    log4j = log4j.replace(
                        oldLogs, newLogs
                    )
                    val message: String =
                        Translations.migrationmanager_migration_threepointonefilepointzero_directory(apiProperties.logsDirectory)
                    changes.add(message)
                }
                if (log4j.contains("<Configuration status=\"WARN\">")) {
                    changed = true
                    log4j = log4j.replace(
                        "<Configuration status=\"WARN\">", "<Configuration monitorInterval=\"30\">"
                    )
                    changes.add(
                        Translations.migrationmanager_migration_threepointonefilepointzero_interval.toString()
                    )
                }
                if (changed) {
                    log4J2Xml.writeText(log4j)
                    changes.add(
                        Translations.migrationmanager_migration_threepointonefilepointzero_restart.toString()
                    )
                }
                if (changes.isNotEmpty()) {
                    migrationMessages.add(
                        MigrationMessage(
                            previous, current, changes
                        )
                    )
                }
            } catch (ex: IOException) {
                log.error(
                    "Error reading/writing log4j2.xml.", ex
                )
            }
        }

        private fun FourPointZeroPointZero() {
            val changes: MutableList<String> = ArrayList<String>(10)
            if (File(apiProperties.homeDirectory, "plugins").isDirectory && File(
                    apiProperties.homeDirectory, "addons"
                ).renameTo(apiProperties.pluginsDirectory)
            ) {
                changes.add(Translations.migrationmanager_migration_fourpointzeropointzero_addons.toString())
                val disabled = File(apiProperties.pluginsDirectory, "disabled.txt")
                val contents = disabled.readText()
                for (file in apiProperties.pluginsDirectory.filteredWalk(listOf(".jar"))) {
                    val id = ZipFile(file).use {
                        tomlParser.parse(
                            it.getInputStream(it.getFileHeader("addon.toml")).readText()
                        ).get<String>("id")
                    }
                    if (!contents.contains(id)) {
                        disabled.appendText(id)
                        changes.add(Translations.migrationmanager_migration_fourpointzeropointzero_addons_disabled(id))
                    }
                }
            }

            if (apiProperties.language.language.lowercase() == "en_us") {
                val old = apiProperties.language.language.split("_")
                apiProperties.changeLocale(Locale("en_GB"))
                changes.add(Translations.migrationmanager_migration_fourpointzeropointzero_locale(old))
            }

            if (changes.isNotEmpty()) {
                migrationMessages.add(
                    MigrationMessage(
                        previous, current, changes
                    )
                )
            }
        }

        private fun FivePointZeroPointZero() {
            val changes: MutableList<String> = ArrayList<String>(10)
            val previousSetting = apiProperties.scriptTemplates.joinToString(",")
            val currentFiles = apiProperties.serverFilesDirectory.walk().maxDepth(1).filter {
                it.name.endsWith("sh",ignoreCase = true) ||
                        it.name.endsWith("ps1",ignoreCase = true) ||
                        it.name.endsWith("bat",ignoreCase = true)
            }.filter { !it.name.contains("default_template",ignoreCase = true)}.toList()
            val templates = TreeSet<File>()

            if (previousSetting == "default_template.ps1,default_template.sh") {
                changes.add(Translations.migrationmanager_migration_fivepointzeropointzero_scripts_default.toString())
                apiProperties.scriptTemplates = TreeSet(apiProperties.defaultScriptTemplates())
            } else if (currentFiles.isNotEmpty()) {
                changes.add(Translations.migrationmanager_migration_fivepointzeropointzero_scripts_custom.toString())
                for (file in currentFiles) {
                    templates.add(file.absoluteFile)
                }
                apiProperties.scriptTemplates = templates
            }

            if (changes.isNotEmpty()) {
                migrationMessages.add(
                    MigrationMessage(
                        previous, current, changes
                    )
                )
            }
        }

        private fun SixPointZeroPointZero() {
            val changes: MutableList<String> = ArrayList<String>(10)
            val previousSetting = apiProperties.scriptTemplates
            val newSetting = hashMapOf<String, String>()

            var type: String
            for (template in previousSetting) {
                type = template.name.substring(template.name.lastIndexOf("."))
                newSetting[type] = template.absolutePath
                changes.add(Translations.migrationmanager_migration_sixpointzeropointzero_scripts_template("$type = ${template.absolutePath}"))
            }
        }
    }
}