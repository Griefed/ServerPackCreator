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

import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.GZIPOutputStream

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

    /** TAR's fixed block size. Every header is one block and every file is padded up to a multiple. */
    private const val BLOCK = 512

    /** How much of a header the `name` field holds; anything longer needs a GNU long-name entry. */
    private const val NAME_LENGTH = 100

    /** Typeflag for an ordinary file. */
    private const val TYPE_FILE = '0'.code.toByte()

    /** Typeflag for a directory. */
    private const val TYPE_DIRECTORY = '5'.code.toByte()

    /** Typeflag for GNU's long-name entry, whose *data* is the name of the entry that follows it. */
    private const val TYPE_LONG_NAME = 'L'.code.toByte()

    /** The name GNU gives a long-name entry. Readers key on the typeflag, but they print this. */
    private const val LONG_NAME_MARKER = "././@LongLink"

    /** Permissions for a directory and for the scripts that have to be runnable after extraction. */
    private const val MODE_EXECUTABLE = 493 // 0755

    /** Permissions for everything else in a server pack. */
    private const val MODE_REGULAR = 420 // 0644

    /**
     * Write everything under [pack] — not [pack] itself — into [destination] as a gzipped TAR.
     *
     * [destination] is closed by this call, because the gzip trailer only lands when the stream is.
     * Entries are sorted so two runs over the same pack produce the same bytes, and everything is
     * streamed: a server pack is routinely gigabytes and none of it is held in memory.
     */
    fun write(pack: File, destination: OutputStream) {
        GZIPOutputStream(BufferedOutputStream(destination, 1 shl 16)).use { gzip ->
            pack.walkTopDown()
                .filter { it != pack }
                .sortedBy { it.absolutePath }
                .forEach { entry -> writeEntry(gzip, pack, entry) }
            // Two zero blocks are what tells a reader the archive ended rather than was truncated.
            gzip.write(ByteArray(BLOCK * 2))
        }
    }

    /** Write one file or directory: its header, its bytes, and the padding up to the next block. */
    private fun writeEntry(out: OutputStream, pack: File, entry: File) {
        val directory = entry.isDirectory
        val name = relativeName(pack, entry, directory)
        val size = if (directory) 0L else entry.length()

        if (name.toByteArray(StandardCharsets.UTF_8).size > NAME_LENGTH) {
            writeLongName(out, name, entry.lastModified())
        }
        out.write(header(name, size, entry.lastModified(), modeFor(entry, directory), if (directory) TYPE_DIRECTORY else TYPE_FILE))
        if (!directory) {
            entry.inputStream().use { it.copyTo(out, 1 shl 16) }
            out.write(ByteArray(padding(size)))
        }
    }

    /**
     * Write the GNU `L` entry that carries a name too long for a header's own field.
     *
     * The name goes in as the entry's *data*; the header that follows repeats as much of it as fits,
     * which is what a reader that does not understand `L` falls back to.
     */
    private fun writeLongName(out: OutputStream, name: String, modified: Long) {
        val encoded = name.toByteArray(StandardCharsets.UTF_8)
        // +1 for the NUL GNU includes in the length, and which readers expect to find.
        val size = encoded.size + 1L
        out.write(header(LONG_NAME_MARKER, size, modified, MODE_REGULAR, TYPE_LONG_NAME))
        out.write(encoded)
        out.write(0)
        out.write(ByteArray(padding(size)))
    }

    /**
     * One 512-byte ustar header.
     *
     * The checksum is the sum of every byte of the header with its own field read as spaces — so it is
     * computed last, over a header that is otherwise finished.
     */
    private fun header(name: String, size: Long, modified: Long, mode: Int, type: Byte): ByteArray {
        val header = ByteArray(BLOCK)
        putString(header, 0, NAME_LENGTH, name)
        putOctal(header, 100, 8, mode.toLong())
        putOctal(header, 108, 8, 0L)                       // uid: nobody's, deliberately
        putOctal(header, 116, 8, 0L)                       // gid: likewise
        putOctal(header, 124, 12, size)
        putOctal(header, 136, 12, modified / 1000L)        // TAR counts seconds, File counts millis
        header.fill(' '.code.toByte(), 148, 156)           // checksum field, as spaces, while summing
        header[156] = type
        putString(header, 257, 6, "ustar")
        header[263] = '0'.code.toByte()
        header[264] = '0'.code.toByte()

        val checksum = header.sumOf { it.toInt() and 0xFF }
        // Six octal digits, a NUL and a space: the one field whose format is not "octal then NUL".
        putString(header, 148, 7, String.format(Locale.ROOT, "%06o", checksum) + "\u0000")
        header[155] = ' '.code.toByte()
        return header
    }

    /**
     * The entry's permissions, decided rather than copied.
     *
     * Nothing readable off disk is trustworthy here: a pack generated by ServerPackCreator carries 0544
     * on its scripts, because `File.setExecutable(true)` is owner-only, and a pack built on Windows
     * carries no POSIX bits at all. Both would extract into a server nobody but the builder can start.
     */
    private fun modeFor(entry: File, directory: Boolean): Int = when {
        directory -> MODE_EXECUTABLE
        entry.name.startsWith("start.") || entry.name.startsWith("install_java.") -> MODE_EXECUTABLE
        else -> MODE_REGULAR
    }

    /**
     * The entry's name inside the archive, built element by element with a `/` for a directory.
     *
     * Not `relativeTo(...).path`: on Windows that yields backslashes, which are legal filename
     * characters in TAR and would arrive on Linux as one file with slashes in its name.
     */
    private fun relativeName(pack: File, entry: File, directory: Boolean): String {
        val relative = entry.toPath().let { pack.toPath().relativize(it) }
        val name = relative.joinToString("/") { it.toString() }
        return if (directory) "$name/" else name
    }

    /** How many bytes of padding take [size] up to the next block boundary. */
    private fun padding(size: Long): Int = ((BLOCK - (size % BLOCK)) % BLOCK).toInt()

    /** Write [value] into [length] bytes at [offset], NUL-padded, truncating what does not fit. */
    private fun putString(target: ByteArray, offset: Int, length: Int, value: String) {
        val encoded = value.toByteArray(StandardCharsets.UTF_8)
        val written = minOf(encoded.size, length)
        encoded.copyInto(target, offset, 0, written)
    }

    /** Write [value] as TAR's zero-padded octal: [length] - 1 digits and a trailing NUL. */
    private fun putOctal(target: ByteArray, offset: Int, length: Int, value: Long) {
        putString(target, offset, length, java.lang.Long.toOctalString(value).padStart(length - 1, '0'))
    }
}
