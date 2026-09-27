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
import java.io.OutputStream

/**
 * Writes a directory tree as a gzipped TAR, with the permissions a server pack needs rather than the
 * ones it happens to have.
 *
 * Writing the format here rather than with a library is a deliberate trade, and the reason is delivery:
 * a pf4j plugin jar carries no dependencies of its own, nothing in this repository builds a fat plugin
 * jar, and the host's runtime classpath has no TAR writer on it — so `commons-compress` could not reach
 * the machine this runs on. What is written is the small, closed subset TAR actually needs here: ustar
 * headers, GNU `L` headers for names past 100 characters, and octal fields. Every claim about it is
 * checked by `TarGzWriterTest` against the real `tar`, because a format written by hand and verified by
 * re-reading the spec is a format verified by its author's assumptions.
 *
 * @author Griefed
 */
internal object TarGzWriter {

    /**
     * Write everything under [pack] — not [pack] itself — into [destination] as a gzipped TAR.
     *
     * [destination] is closed by this call, because the gzip trailer only lands when the stream is.
     */
    fun write(pack: File, destination: OutputStream): Unit = TODO("the TAR writer has not been written yet")
}
