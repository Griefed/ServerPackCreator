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

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Stores values gathered by [JarUtilities.jarInformation] for easy access. Values
 * stored and provided by this class are:
 *
 *  * The directory in which the exe or JAR reside in
 *  * The file used to start ServerPackCreator. Either an exe or a JAR
 *  * The filename of the exe or JAR
 *  * The Java version with which ServerPackCreator is being used
 *  * The operating systems' architecture
 *  * The operating systems' name
 *  * The operating systems' version
 *  * Whether an exe is being used for running ServerPackCreator
 *
 * @param clazz The class from which to acquire information about the containing JAR-file and general location.
 *
 */
class JarInformation(clazz: Class<*>) {
    /**
     * The folder containing the ServerPackCreator.exe or JAR-file.
     *
     * @author Griefed
     */
    var jarFolder: File
        private set

    /**
     * The .exe or JAR-file of ServerPackCreator.
     *
     * @author Griefed
     */
    val jarFile: File

    /**
     * The path to the JAR-file or .exe of ServerPackCreator
     */
    val jarPath: Path

    /**
     * The name of the .exe or JAR-file.
     *
     * @author Griefed
     */
    val jarFileName: String

    /**
     * The Java version used to run ServerPackCreator.
     *
     * @author Griefed
     */
    val javaVersion: String

    /**
     * Architecture of the operating system on which ServerPackCreator is running on.
     *
     * @author Griefed
     */
    val osArch: String

    /**
     * The name of the operating system on which ServerPackCreator is running on.
     *
     * @author Griefed
     */
    val osName: String

    /**
     * The version of the OS on which ServerPackCreator is running on.
     *
     * @author Griefed
     */
    val osVersion: String

    /**
     * Whether a .exe or JAR-file was used for running ServerPackCreator.
     *
     * @author Griefed
     */
    val isExe: Boolean

    init {
        val sysInfo = HashMap<String, String>(10)
        sysInfo.putAll(JarUtilities.jarInformation(clazz))
        jarPath = Paths.get(sysInfo["jarPath"]!!)
        jarFile = jarPath.toFile()
        jarFolder = if (jarPath.toString().contains("!")) {
            val temp = File(jarPath.toString().substringBefore('!'))
            if (temp.isDirectory) {
                temp
            } else {
                temp.parentFile
            }
        } else {
            if (jarFile.isDirectory) {
                jarFile
            } else {
                jarFile.parentFile
            }
        }
        jarFileName = sysInfo["jarName"].toString()
        javaVersion = sysInfo["javaVersion"].toString()
        osArch = sysInfo["osArch"].toString()
        osName = sysInfo["osName"].toString()
        osVersion = sysInfo["osVersion"].toString()
        isExe = jarFileName.endsWith(".exe")
    }
}