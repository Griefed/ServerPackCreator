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
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission

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
     * multi-gigabyte pack is compressed once rather than twice. The temporary file is deleted even
     * when writing fails, because it is the size of the pack.
     */
    fun wrap(pack: File): List<File> {
        val name = safeName(pack.name)
        val payload = Files.createTempFile("spc-selfextract-", ".tar.gz").toFile()
        return try {
            payload.outputStream().use { TarGzWriter.write(pack, it) }
            val shell = write(File(pack.parentFile, "$name.bsx"), Stubs.shell(name), payload)
            val batch = write(File(pack.parentFile, "$name.cmd"), Stubs.batch(name), payload)
            makeExecutable(shell)
            listOf(shell, batch)
        } finally {
            payload.delete()
        }
    }

    /** Write stub-then-payload into [artifact], with the payload's own position settled into the stub. */
    private fun write(artifact: File, stub: String, payload: File): File {
        val header = settleOffset(stub.replace(Stubs.NAME_PLACEHOLDER, artifact.nameWithoutExtension))
        BufferedOutputStream(artifact.outputStream(), 1 shl 16).use { out ->
            out.write(header)
            payload.inputStream().use { it.copyTo(out, 1 shl 16) }
        }
        return artifact
    }

    /**
     * Substitute the payload's byte position into [stub], which is circular: the position is the stub's
     * own length plus one, and a longer number makes the stub longer.
     *
     * Settled by iterating rather than by padding the number to a fixed width. Padding looks tidier and
     * does not work — BSD tail, which is what macOS ships, rejects a zero-padded count outright with
     * `illegal offset -- +000000000639`. Each round can only add digits, never remove them, so this
     * settles in one or two.
     *
     * The position is 1-based because that is what `tail -c +N` counts from; the Windows stub subtracts
     * the one for its own 0-based `Seek`. One number serves both.
     */
    private fun settleOffset(stub: String): ByteArray {
        var offset = "1"
        repeat(8) {
            val candidate = stub.replace(Stubs.OFFSET_PLACEHOLDER, offset).toByteArray(StandardCharsets.UTF_8)
            val next = (candidate.size + 1).toString()
            if (next == offset) {
                return candidate
            }
            offset = next
        }
        throw IllegalStateException("the payload offset did not settle for a stub of ${stub.length} characters")
    }

    /**
     * Reduce a pack's name to what both a POSIX shell and cmd.exe can carry unquoted.
     *
     * Neither stub quotes it, deliberately: quoting it correctly for both at once is a rule nobody
     * reading the artifact would be able to check, where a name with nothing special in it is obviously
     * safe. Replacing rather than rejecting, because the name comes from a pack that already exists.
     */
    private fun safeName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._+-]"), "_").ifEmpty { "server-pack" }

    /**
     * Set the executable bit on the shell artifact, for everyone, where the filesystem has one.
     *
     * Windows has no POSIX view, so an artifact built there arrives without it and the recipient has to
     * `chmod +x` — which is what the chapter tells them. Silently doing nothing here is the honest
     * behaviour: the alternative is pretending a bit was set that the filesystem cannot hold.
     */
    private fun makeExecutable(artifact: File) {
        val view = Files.getFileAttributeView(artifact.toPath(), PosixFileAttributeView::class.java) ?: return
        view.setPermissions(
            view.readAttributes().permissions() + setOf(
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_EXECUTE
            )
        )
    }
}
