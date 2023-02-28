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

import net.lingala.zip4j.ZipFile
import java.io.File
import java.net.URL
import java.nio.file.Paths

private val jar = "^jar:(file:.*[.]jar)!/.*".toRegex()
private val jarJar = "^(file:.*[.]jar)!.*[.]jar".toRegex()
private val nested = ".*[.]jar!.*[.]jar".toRegex()

/**
 * Acquire the JAR-file which contains this class. If JAR-file is a nested JAR-file, meaning the class is inside
 * a JAR-file inside another JAR-file, then said nested JAR-file will be extracted to the specified directory, or your
 * systems temp-directory, and that file will be returned by this method.
 *
 * In any case the returned file will give you access to the JAR-file which contains class from which this method was called,
 * unless the class itself is nested more than one layer deep.
 * @param rootOnly Whether only the root of the parent JAR-file should be considered, ignoring potential nesting of the class.
 * @param tempDir The directory to which a nested JAR-file will be extracted to. If not specified, your systems default
 * temp-directory will be used.
 * @author Griefed
 */
@Throws(RuntimeException::class)
fun <T : Any> Class<T>.source(
    rootOnly: Boolean = false,
    tempDir: File = File(System.getProperty("java.io.tmpdir"))
): File {
    val classResource: URL = this.getResource(this.simpleName + ".class")
        ?: throw RuntimeException("Class resource is null")
    val url = classResource.toString()
    var source: File? = null
    if (url.startsWith("jar:file:")) {

        var path = url.replace(jar, "$1")
        if (rootOnly && path.matches(jarJar)) {
            path = path.replace(jarJar, "$1")
        }

        try {
            source = Paths.get(URL(path).toURI()).toFile()
        } catch (e: Exception) {
            throw RuntimeException("Invalid Jar File URL String")
        }

    } else if (url.startsWith("file:")) {
        try {
            source = Paths.get(URL(url).toURI()).toFile().parentFile
        } catch (e: Exception) {
            throw RuntimeException("Invalid Jar File URL String")
        }
    }

    if (source != null) {
        return if (source.path.matches(nested)) {
            val inner = source.path.toString().substring(source.path.toString().lastIndexOf("!"))
            val nestedJar = File(inner.substring(2))
            if (!tempDir.isDirectory) throw RuntimeException("Invalid temp-directory $tempDir.")
            ZipFile(source.path.toString().replace(inner, "")).use {
                it.extractFile(
                    nestedJar.toString(),
                    tempDir.path,
                    nestedJar.name
                )
            }
            File(tempDir.path, nestedJar.name).absoluteFile
        } else {
            source
        }
    }

    throw RuntimeException("Invalid Jar File URL String")
}