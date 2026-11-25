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
package de.griefed.serverpackcreator.clientside.boot

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.nio.channels.SeekableByteChannel
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes

/**
 * Host-side access to a directory a booted mod could write into — a staged server pack, or the attempt
 * directory holding one. Whatever such a tree contains is attacker-controlled, so nothing here ever follows
 * a symbolic link: reads refuse links and anything resolving outside the tree, deletes and walks treat a
 * link as the link itself (`Files.walkFileTree` without `FileVisitOption.FOLLOW_LINKS` reports it to
 * `visitFile` and never descends). `java.io.File`'s `isFile`, `readText`, `walkTopDown` and Kotlin's
 * `File.deleteRecursively` all follow links, and must not be used on these trees.
 */
object ModWritableTree {

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    private val noFollow = arrayOf(LinkOption.NOFOLLOW_LINKS)

    /** The regular [files] directly inside one directory, and the symbolic links [refused] there. */
    data class Listing(
        /** Regular files, none of them a link, all resolving inside the tree. */
        val files: List<File>,
        /** Paths refused because they are, or resolve through, a symbolic link — evidence a mod tried something. */
        val refused: List<File>
    )

    /**
     * The regular files directly inside [root]`/`[directory]. A [directory] that is itself a link, or a file
     * that is one, is refused and logged at WARN rather than followed; so is anything whose real path leaves
     * [root]'s. Other non-regular entries (directories, FIFOs) are skipped silently, as they never were evidence.
     */
    fun regularFilesIn(root: File, directory: String): Listing {
        val dir = File(root, directory).toPath()
        if (!Files.exists(dir, *noFollow)) {
            return Listing(emptyList(), emptyList())
        }
        val rootReal = root.toPath().toRealPath()
        if (Files.isSymbolicLink(dir) || !Files.isDirectory(dir, *noFollow) || !dir.toRealPath().startsWith(rootReal)) {
            return Listing(emptyList(), listOf(refuse(dir.toFile())))
        }
        val entries = dir.toFile().listFiles().orEmpty().map { it.toPath() }
        val (links, others) = entries.partition { Files.isSymbolicLink(it) }
        val files = others.filter { Files.isRegularFile(it, *noFollow) }
        val (inside, outside) = files.partition { resolvesInside(it, rootReal) }
        return Listing(inside.map { it.toFile() }, (links + outside).map { refuse(it.toFile()) })
    }

    /** Whether [path]'s real path lies under [rootReal]; one that cannot be resolved, say because it vanished, counts as outside. */
    private fun resolvesInside(path: Path, rootReal: Path): Boolean = try {
        path.toRealPath().startsWith(rootReal)
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }

    /**
     * Open [file] for reading without following a link in its last component, so a link swapped in after
     * [regularFilesIn] listed it fails to open instead of being read.
     */
    fun openForReading(file: File): SeekableByteChannel =
        Files.newByteChannel(file.toPath(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)

    /**
     * Delete [tree] and everything beneath it, removing every symbolic link as a link. Keeps going past an
     * entry it cannot remove and returns `false` if anything survived; an absent [tree] counts as deleted.
     */
    fun deleteRecursively(tree: File): Boolean {
        val root = tree.toPath()
        if (!Files.exists(root, *noFollow)) {
            return true
        }
        var complete = true
        fun delete(path: Path) {
            try {
                Files.deleteIfExists(path)
            } catch (failure: IOException) {
                log.debug("Could not delete $path: ${failure.message}")
                complete = false
            }
        }
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult =
                FileVisitResult.CONTINUE.also { delete(file) }

            override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                FileVisitResult.CONTINUE.also { delete(file) }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult =
                FileVisitResult.CONTINUE.also { delete(dir) }
        })
        return complete
    }

    /** Total bytes of the regular files in [tree], or of [tree] itself if it is one; nothing behind a link counts. */
    fun sizeOf(tree: File): Long {
        var total = 0L
        Files.walkFileTree(tree.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (attrs.isRegularFile) {
                    total += attrs.size()
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE
        })
        return total
    }

    private fun refuse(path: File): File = path.also {
        log.warn("Refusing to follow ${it.absolutePath}: a symbolic link in a tree a booted mod could write to.")
    }
}
