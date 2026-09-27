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
package de.griefed.serverpackcreator.plugin.selfextract.core

import java.io.File

/**
 * Turns a generated server pack into the two artifacts that carry it: a `.bsx` for Linux and macOS,
 * a `.cmd` for Windows.
 *
 * Both are the same shape — a text stub, then a gzipped TAR of the pack — and both state, in their own
 * text, the byte at which that archive begins. That is the whole design, and it is circular: the number
 * is the stub's own length, so writing it changes what it measures. See [settleOffset].
 *
 * @author Griefed
 */
internal object SelfExtractingArchive {

    /**
     * Write `<pack>.bsx` and `<pack>.cmd` beside [pack] and return them.
     *
     * The archive is built once into a temporary file and copied into both artifacts, so a
     * multi-gigabyte pack is compressed once rather than twice.
     */
    fun wrap(pack: File): List<File> = TODO("the artifacts are not written yet")
}
