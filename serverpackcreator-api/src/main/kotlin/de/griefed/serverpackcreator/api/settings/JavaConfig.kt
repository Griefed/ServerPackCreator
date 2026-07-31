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
package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import de.griefed.serverpackcreator.api.utilities.common.FileType
import de.griefed.serverpackcreator.api.utilities.common.FileUtilities
import de.griefed.serverpackcreator.api.utilities.common.InvalidFileTypeException
import de.griefed.serverpackcreator.api.utilities.common.SystemUtilities
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * Settings-group for Java-installations used by ServerPackCreator: the Java for
 * modloader-server installs with validation and system-fallback, the per-version java-paths
 * for script-variables, and the script-autoupdate flag. Extracted from ApiProperties (refactor
 * Phase 1b); ApiProperties remains the facade through which consumers access these values.
 */
class JavaConfig(private val store: PropertyStore) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val checkedJavas = hashMapOf<String, Boolean>()

    /** Property keys for the Java paths and the auto-update toggle. */

    companion object {
        /**
         * Property-key holding the Java used for modloader-server installations.
         */
        const val JAVA_FOR_SERVER_INSTALL_KEY = "de.griefed.serverpackcreator.java"

        /**
         * Property-key prefix under which per-version java-paths are stored, suffixed with the
         * Java version-number.
         */
        const val SCRIPT_JAVA_PATHS_PREFIX = "de.griefed.serverpackcreator.script.java"

        /**
         * Property-key toggling automatic updates of the SPC_JAVA_SPC script-placeholder.
         */
        const val SCRIPT_JAVA_AUTOUPDATE_KEY = "de.griefed.serverpackcreator.script.java.autoupdate"
    }

    /**
     * Fallback-value for automatic updates of the SPC_JAVA_SPC script-placeholder.
     */
    val fallbackJavaScriptAutoupdateEnabled = true

    /**
     * Paths to Java installations available to SPC for automatically updating the script variables of a given server pack
     * configuration.
     * * key: Java version
     * * value: Path to the Java .exe or binary
     *
     * If you plan on overwriting this property, make sure to format they key-value-pairs as follows:
     * * key: `de.griefed.serverpackcreator.script.java` followed by the number representing the Java version
     * * value: Valid path to a Java installation corresponding to the number used in the key
     */
    var javaPaths = HashMap<String, String>(256)
        get() {
            val paths = HashMap<String, String>(256)
            var path: String
            var position: String
            for (i in 8..255) {
                position = SCRIPT_JAVA_PATHS_PREFIX + i
                path = store.properties.getProperty(position, "")
                if (checkJavaPath(path)) {
                    paths[i.toString()] = path
                    store.properties.setProperty(position, path)
                }
            }
            field = paths
            return paths
        }
        set(values) {
            var position: Int?
            var newKey: String
            val paths = HashMap<String, String>(256)
            for (i in 8..255) {
                store.properties.remove(SCRIPT_JAVA_PATHS_PREFIX + i)
            }
            for ((key, value) in values) {
                if (!checkJavaPath(value)) {
                    continue
                }
                position = key.replace(SCRIPT_JAVA_PATHS_PREFIX, "").toIntOrNull()
                newKey = SCRIPT_JAVA_PATHS_PREFIX + position
                if (position != null && 8 <= position && position < 256) {
                    store.properties.setProperty(newKey, value)
                    paths[newKey] = value
                }
            }
            field = paths
            log.info("Available Java paths for scripts:")
            for ((key, value) in field) {
                log.info("Java $key path: $value")
            }
        }

    /**
     * Whether to automatically update the `SPC_JAVA_SPC`-placeholder in the script variables
     * table with a Java path matching the required Java version for the Minecraft server.
     */
    var isJavaScriptAutoupdateEnabled = fallbackJavaScriptAutoupdateEnabled
        get() {
            field = store.getBool(SCRIPT_JAVA_AUTOUPDATE_KEY, fallbackJavaScriptAutoupdateEnabled)
            return field
        }
        set(value) {
            store.setBool(SCRIPT_JAVA_AUTOUPDATE_KEY, value)
            field = value
            log.info("Automatically update SPC_JAVA_SPC-placeholder in script variables table set to: $field")
        }

    /**
     * Java installation used for installing the modloader server during server pack creation.
     */
    var javaPath = "java"
        get() {
            val prop = store.properties.getProperty(JAVA_FOR_SERVER_INSTALL_KEY, null)
            field = if (checkJavaPath(prop)) {
                prop
            } else {
                val acquired = acquireJavaPath()
                store.properties.setProperty(JAVA_FOR_SERVER_INSTALL_KEY, acquired)
                acquired
            }
            return field
        }
        set(value) {
            if (checkJavaPath(value)) {
                store.properties.setProperty(JAVA_FOR_SERVER_INSTALL_KEY, value)
                field = value
                log.info("Java path set to: $field")
            } else {
                log.error("Invalid Java path specified: $value")
            }
        }

    /**
     * Check the given path to a Java installation for validity and return it, if it is valid. If the
     * passed path is a UNIX symlink or Windows lnk, it is resolved, then returned. If the passed path
     * is considered invalid, the system default is acquired and returned.
     *
     * @param pathToJava The path to check for whether it is a valid Java installation.
     * @return Returns the path to the Java installation. If user input was incorrect, SPC will try to
     * acquire the path automatically.
     * @author Griefed
     */
    fun acquireJavaPath(pathToJava: String? = null): String {
        var checkedJavaPath: String
        try {
            if (!pathToJava.isNullOrBlank()) {
                if (checkJavaPath(pathToJava)) {
                    return pathToJava
                }
                if (checkJavaPath("$pathToJava.exe")) {
                    return "$pathToJava.exe"
                }
                if (checkJavaPath("$pathToJava.lnk")) {
                    return FileUtilities.resolveLink(File("$pathToJava.lnk"))
                }
            }
            checkedJavaPath = SystemUtilities.acquireJavaPathFromSystem()
            log.debug("Acquired path to Java installation: $checkedJavaPath")
        } catch (ex: NullPointerException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = SystemUtilities.acquireJavaPathFromSystem()
            log.debug("Automatically acquired path to Java installation: $checkedJavaPath", ex)
        } catch (ex: InvalidFileTypeException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = SystemUtilities.acquireJavaPathFromSystem()
            log.debug("Automatically acquired path to Java installation: $checkedJavaPath", ex)
        } catch (ex: IOException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = SystemUtilities.acquireJavaPathFromSystem()
            log.debug("Automatically acquired path to Java installation: $checkedJavaPath", ex)
        }
        return checkedJavaPath
    }

    /**
     * Check whether the given path is a valid Java specification.
     *
     * @param pathToJava Path to the Java executable
     * @return `true` if the path is valid.
     * @author Griefed
     */
    private fun checkJavaPath(pathToJava: String?): Boolean {
        if (pathToJava.isNullOrBlank()) {
            return false
        }
        if (checkedJavas.containsKey(pathToJava)) {
            return checkedJavas[pathToJava]!!
        }
        val result: Boolean
        when (FileUtilities.checkFileType(pathToJava)) {
            FileType.FILE -> {
                result = testJava(pathToJava)
            }

            FileType.LINK, FileType.SYMLINK -> {
                result = try {
                    testJava(FileUtilities.resolveLink(File(pathToJava)))
                } catch (ex: InvalidFileTypeException) {
                    log.error("Could not read Java link/symlink.", ex)
                    false
                } catch (ex: IOException) {
                    log.error("Could not read Java link/symlink.", ex)
                    false
                }
            }

            FileType.DIRECTORY -> {
                log.error("Directory specified. Path to Java must lead to a lnk, symlink or file.")
                result = false
            }

            FileType.INVALID -> result = false
        }
        checkedJavas[pathToJava] = result
        return result
    }

    /**
     * Test for a valid Java specification by trying to run `java -version`. If the command goes
     * through without errors, it is considered a correct specification.
     *
     * @param pathToJava Path to the java executable/binary.
     * @return `true` if the specified file is a valid Java executable/binary.
     * @author Griefed
     */
    private fun testJava(pathToJava: String): Boolean {
        val testSuccessful: Boolean = try {
            val processBuilder = ProcessBuilder(listOf(pathToJava, "-version"))
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            while (bufferedReader.readLine() != null && bufferedReader.readLine() != "null") {
                println(bufferedReader.readLine())
            }
            bufferedReader.close()
            process.destroyForcibly()
            true
        } catch (e: IOException) {
            log.error("Invalid Java specified.")
            false
        }
        return testSuccessful
    }

    /**
     * Whether a viable path to a Java executable or binary has been configured for
     * ServerPackCreator.
     *
     * @return `true` if a viable path has been set.
     * @author Griefed
     */
    fun javaAvailable() = checkJavaPath(javaPath)

    /**
     * Get the path to the specified Java executable/binary, wrapped in an [Optional] for your
     * convenience.
     *
     * @param javaVersion The Java version to acquire the path for.
     * @return The path to the Java executable/binary, if available.
     * @author Griefed
     */
    fun javaPath(javaVersion: Int) =
        if (javaPaths.containsKey(javaVersion.toString())
            && javaPaths[javaVersion.toString()]?.let { File(it).isFile } == true
        ) {
            Optional.ofNullable(javaPaths[javaVersion.toString()])
        } else {
            Optional.empty()
        }

    /**
     * Get the path to the specified Java executable/binary, wrapped in an [Optional] for your
     * convenience.
     *
     * @param javaVersion The Java version to acquire the path for.
     * @return The path to the Java executable/binary, if available.
     * @author Griefed
     */
    fun javaPath(javaVersion: String) = javaPath(javaVersion.toInt())
}
