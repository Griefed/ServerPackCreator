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
package de.griefed.serverpackcreator.api.utilities.common

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.*

/**
 * Utility-class revolving around the system we are running on.
 *
 * @author Griefed
 */
@Suppress("unused")
class SystemUtilities {

    companion object {
        private val log by lazy { cachedLoggerOf(SystemUtilities::class.java) }
        private val windowsDriveRegex = "^[a-zA-Z]:\\\\.*".toRegex()
        private val javaPathSuffix = "%s${File.separator}bin${File.separator}java"
        private val javaHome = System.getProperty("java.home")

        private val OS: String = System.getProperty("os.name").lowercase(Locale.getDefault())
        val IS_WINDOWS: Boolean = (OS.indexOf("win") >= 0)
        val IS_MAC: Boolean = (OS.indexOf("mac") >= 0)
        val IS_UNIX: Boolean = (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0)
        val IS_SOLARIS: Boolean = (OS.indexOf("sunos") >= 0)

        /**
         * Automatically acquire the path to the systems default Java installation.
         *
         * @return String. The path to the systems default Java installation.
         * @author Griefed
         */
        fun acquireJavaPathFromSystem(): String {
            log.debug("Acquiring path to Java installation from system properties...")
            var javaPath = "Couldn't acquire JavaPath"
            if (File(javaHome).exists()) {
                javaPath = javaPathSuffix.format(javaHome)
                if (javaPath.matches(windowsDriveRegex) || File("${javaPath}.exe").isFile) {
                    log.debug("We're running on Windows. Ensuring javaPath ends with .exe")
                    javaPath = "${javaPath}.exe"
                }
            }
            return javaPath
        }
    }
}