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
package de.griefed.serverpackcreator.api.utilities.common

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * Utility-class revolving around the system we are running on.
 *
 * @author Griefed
 */
@Suppress("unused")
actual class SystemUtilities {
    private val log = cachedLoggerOf(this.javaClass)
    private val windowsDrives: List<String> = listOf(
        "A:", "B:", "C:", "D:", "E:", "F:", "G:", "H:", "I:", "J:", "K:", "L:", "M:", "N:",
        "O:", "P:", "Q:", "R:", "S:", "T:", "U:", "V:", "W:", "X:", "Y:", "Z:"
    )
    private val javaPathSuffix = "%s/bin/java"
    private val javaHome = System.getProperty("java.home")
    private val pathPrefix = "/"
    private val exeSuffix = "%s.exe"

    /**
     * Automatically acquire the path to the systems default Java installation.
     *
     * @return String. The path to the systems default Java installation.
     * @author Griefed
     */
    actual fun acquireJavaPathFromSystem(): String {
        log.debug("Acquiring path to Java installation from system properties...")
        var javaPath = "Couldn't acquire JavaPath"
        if (File(javaHome).exists()) {
            javaPath = javaPathSuffix.format(javaHome)
            if (!javaPath.startsWith(pathPrefix)) {
                for (letter in windowsDrives) {
                    if (javaPath.startsWith(letter)) {
                        log.debug("We're running on Windows. Ensuring javaPath ends with .exe")
                        javaPath = exeSuffix.format(javaPath)
                    }
                }
            }
        }
        return javaPath
    }
}