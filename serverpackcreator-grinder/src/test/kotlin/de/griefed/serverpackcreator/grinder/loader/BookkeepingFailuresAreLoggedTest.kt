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
package de.griefed.serverpackcreator.grinder.loader

import de.griefed.serverpackcreator.grinder.report.capturingLogsOf
import org.apache.logging.log4j.Level
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.io.Writer

/**
 * Pins that the loader cache's bookkeeping, which never fails an install, still says when it lost something: an
 * unreadable template, marker or previous console, and an install console that could not be written, each leave
 * a WARN naming the file. What each call returns is unchanged.
 */
internal class BookkeepingFailuresAreLoggedTest {

    /** Make [file] unreadable for this test, or skip it where the filesystem or a root user ignores that. */
    private fun unreadable(file: File) {
        Assumptions.assumeTrue(file.setReadable(false), "This filesystem must honour setReadable(false)")
        Assumptions.assumeFalse(file.canRead(), "and the process must not be able to read it anyway (not root)")
    }

    private fun warningsOf(owner: Class<*>, block: () -> Unit): List<String> =
        capturingLogsOf(owner) { captured -> block(); captured.messagesAt(Level.WARN) }

    @Test
    fun anUnreadableTemplateIsNamedWhenTheDigestLeavesItOut(@TempDir root: File) {
        val readable = File(root, "start.sh").apply { writeText("a") }
        val skipped = File(root, "start.ps1").apply { writeText("b") }
        unreadable(skipped)
        try {
            val warnings = warningsOf(TemplateProvenance::class.java) {
                Assertions.assertNotNull(TemplateProvenance.digestOf(listOf(readable, skipped)))
            }
            Assertions.assertTrue(warnings.any { "start.ps1" in it }, "WARN names the skipped template: $warnings")
        } finally {
            skipped.setReadable(true)
        }
    }

    @Test
    fun anUnreadablePreviousConsoleIsNamedWhenItIsLost(@TempDir tuple: File) {
        val console = File(tuple, INSTALL_LOG).apply { writeText("console") }
        unreadable(console)
        try {
            val warnings = warningsOf(InstallLogRetention::class.java) {
                Assertions.assertNull(InstallLogRetention.preserve(tuple))
            }
            Assertions.assertTrue(warnings.any { INSTALL_LOG in it }, "WARN names the lost console: $warnings")
        } finally {
            console.setReadable(true)
        }
    }

    @Test
    fun aPreviousConsoleThatCannotBeWrittenBackIsNamed(@TempDir root: File) {
        val missingTuple = File(root, "not-recreated")
        val warnings = warningsOf(InstallLogRetention::class.java) {
            InstallLogRetention.writePrevious(missingTuple, "console")
        }
        Assertions.assertTrue(warnings.any { "$INSTALL_LOG.previous" in it }, "WARN names the lost console: $warnings")
    }

    @Test
    fun anUnreadableMarkerIsNamedRatherThanReadAsALegacyInstall(@TempDir root: File) {
        val cache = LoaderCache(root, { _, _, _, _ -> true }, templateProvenance = { "current" })
        val marker = File(cache.baseDirFor("Fabric", "0.19.5", "1.21.11").apply { mkdirs() }, LoaderCache.MARKER)
        marker.writeText("loaderVersion=0.19.5\n${LoaderCache.TEMPLATES_KEY}=current\n")
        unreadable(marker)
        try {
            val warnings = warningsOf(LoaderCache::class.java) {
                Assertions.assertTrue(cache.isInstalled("Fabric", "0.19.5", "1.21.11"))
                Assertions.assertEquals(emptyList<String>(), cache.installedVersions("Fabric", "1.21.11"))
            }
            Assertions.assertEquals(
                2, warnings.count { marker.absolutePath in it },
                "both the provenance and the version read name the unreadable marker: $warnings"
            )
        } finally {
            marker.setReadable(true)
        }
    }

    @Test
    fun anInstallConsoleThatCannotBeOpenedIsNamed(@TempDir root: File) {
        val occupied = File(root, INSTALL_LOG).apply { mkdirs() }
        val warnings = warningsOf(LiveInstallLog::class.java) {
            LiveInstallLog(occupied).use { it.append("line") }
        }
        Assertions.assertTrue(warnings.any { occupied.absolutePath in it }, "WARN names the console file: $warnings")
    }

    @Test
    fun anInstallConsoleThatStopsAcceptingLinesIsNamedOnce(@TempDir root: File) {
        val failing = object : Writer() {
            override fun write(cbuf: CharArray, off: Int, len: Int) = throw IOException("disk full")
            override fun flush() {}
            override fun close() = throw IOException("disk full")
        }
        val file = File(root, INSTALL_LOG)
        val warnings = warningsOf(LiveInstallLog::class.java) {
            LiveInstallLog(file) { failing }.use { log -> repeat(3) { log.append("line $it") } }
        }
        Assertions.assertEquals(
            1, warnings.count { file.absolutePath in it && "line" in it },
            "a failing console is reported once, not once per line: $warnings"
        )
        Assertions.assertTrue(
            warnings.any { file.absolutePath in it && "clos" in it },
            "and so is a failed close: $warnings"
        )
    }
}
