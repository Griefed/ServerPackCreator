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

import org.jetbrains.annotations.Contract
import java.io.File

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
 * @param jarUtilities Common JAR-file-related utilities.
 *
 */
class JarInformation(clazz: Class<*>, jarUtilities: JarUtilities = JarUtilities()) {
    /**
     * The folder containing the ServerPackCreator.exe or JAR-file.
     *
     * @return Folder containing the ServerPackCreator.exe or JAR-file.
     * @author Griefed
     */
    @get:Contract(pure = true)
    val jarFolder: File

    /**
     * The .exe or JAR-file of ServerPackCreator.
     *
     * @return The .exe or JAR-file of ServerPackCreator.
     * @author Griefed
     */
    @get:Contract(pure = true)
    val jarFile: File

    /**
     * The name of the .exe or JAR-file.
     *
     * @return The name of the .exe or JAR-file.
     * @author Griefed
     */
    @get:Contract(pure = true)
    val jarFileName: String

    /**
     * The Java version used to run ServerPackCreator.
     *
     * @return Java version.
     * @author Griefed
     */
    @get:Contract(pure = true)
    val javaVersion: String

    /**
     * Architecture of the operating system on which ServerPackCreator is running on.
     *
     * @return Arch.
     * @author Griefed
     */
    @get:Contract(pure = true)
    val osArch: String

    /**
     * The name of the operating system on which ServerPackCreator is running on.
     *
     * @return OS name.
     * @author Griefed
     */
    @get:Contract(pure = true)
    val osName: String

    /**
     * The version of the OS on which ServerPackCreator is running on.
     *
     * @return Version of the OS.
     * @author Griefed
     */
    @get:Contract(pure = true)
    val osVersion: String

    /**
     * Whether a .exe or JAR-file was used for running ServerPackCreator.
     *
     * @return `true` if a .exe was/is used.
     * @author Griefed
     */
    @get:Contract(pure = true)
    val isExe: Boolean

    init {
        val sysInfo = HashMap<String, String>(10)
        sysInfo.putAll(jarUtilities.jarInformation(clazz))
        jarFile = sysInfo["jarPath"]?.let { File(it) }!!
        jarFolder = if (jarFile.isFile) {
            jarFile.parentFile
        } else {
            jarFile
        }
        jarFileName = sysInfo["jarName"].toString()
        javaVersion = sysInfo["javaVersion"].toString()
        osArch = sysInfo["osArch"].toString()
        osName = sysInfo["osName"].toString()
        osVersion = sysInfo["osVersion"].toString()
        isExe = jarFileName.endsWith(".exe")
    }
}