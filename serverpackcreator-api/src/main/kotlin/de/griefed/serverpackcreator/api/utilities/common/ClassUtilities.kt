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

import net.lingala.zip4j.ZipFile
import java.io.File
import java.net.JarURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.Paths

private val nested = ".*[.]jar!.*[.]jar".toRegex()
private val tmpDir = System.getProperty("java.io.tmpdir")
private const val FILE = "file:"

/**
 * Acquire the JAR-file which contains this class. If JAR-file is a nested JAR-file, meaning the class is inside
 * a JAR-file inside another JAR-file, then said nested JAR-file will be extracted to the specified directory, or your
 * systems temp-directory, and that file will be returned by this method.
 *
 * In any case the returned file will give you access to the JAR-file which contains class from which this method was called,
 * unless the class itself is nested more than one layer deep.
 * @param tempDir The directory to which a nested JAR-file will be extracted to. If not specified, your systems default
 * temp-directory will be used.
 * @author Griefed
 */
@Throws(JarAccessException::class)
fun <T : Any> Class<T>.source(tempDir: File = File(tmpDir)): File {
    val clazz = "$simpleName.class"
    val classResource: URL = getResource(clazz) ?: throw JarAccessException("Class resource is null")
    val url = classResource.toString()

    val source: File?
    if (url.startsWith(FILE)) {
        try {
            val uri = URI(url)
            val file = Paths.get(uri).toFile()
            source = file.parentFile
        } catch (e: Exception) {
            throw JarAccessException("Invalid Jar File URL String at JAR: $url")
        }
    } else {
        val jar = (classResource.openConnection() as JarURLConnection).jarFile
        source = File(jar.name)
    }

    if (source == null) {
        throw JarAccessException("Invalid Jar File URL String: clazz: $clazz, classResource: $classResource, url: $url")
    }

    return if (source.path.matches(nested)) {
        val inner = source.path.toString().substring(source.path.toString().lastIndexOf("!"))
        val nestedJar = File(inner.substring(2))
        if (!tempDir.isDirectory) throw JarAccessException("Invalid temp-directory $tempDir.")
        ZipFile(source.path.toString().replace(inner, "")).use {
            it.extractFile(
                nestedJar.toString(),
                tempDir.path,
                nestedJar.name
            )
        }
        File(tempDir.path, nestedJar.name).absoluteFile
    } else {
        val tempSource = source.absolutePath
        val newSource = if (tempSource.contains("!")) {
            File(tempSource.substring(0, tempSource.indexOfFirst { letter -> letter == '!' }))
        } else {
            source.absoluteFile
        }
        newSource
    }
}