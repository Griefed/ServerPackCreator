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
@file:Suppress("unused")

package de.griefed.serverpackcreator.api.utilities.common

import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Read this input stream to a string.
 * @param charset Charset to use for reading. Default is [Charsets.UTF_8]
 * @return The text contained in this input stream.
 * @author Griefed
 */
fun InputStream.readText(charset: Charset = Charsets.UTF_8) = this.bufferedReader(charset).use { it.readText() }

/**
 * Write this input stream to a text-file.
 * @param file The file to write to
 * @param charset Charset to use for reading. Default is [Charsets.UTF_8]
 * @author Griefed
 */
fun InputStream.writeTextToFile(file: File, charset: Charset = Charsets.UTF_8) =
    file.writeText(this.readText(charset), charset)