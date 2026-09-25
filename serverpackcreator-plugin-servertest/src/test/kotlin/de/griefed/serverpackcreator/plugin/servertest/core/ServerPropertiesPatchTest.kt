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
package de.griefed.serverpackcreator.plugin.servertest.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins that borrowing a pack's `server.properties` changes the port and nothing else, and that the pack gets
 * its file back.
 *
 * `server.properties` is on ServerPackCreator's protected-paths list because it is the user's: a world's
 * difficulty, its MOTD, its whitelist switch. Testing a pack must not silently rewrite any of that, so the
 * assertions here are on the **whole file** rather than on the port line — a guard that only checked the port
 * would stay green while the rest of the file was reformatted underneath it.
 */
internal class ServerPropertiesPatchTest {

    /** A realistic slice of ServerPackCreator's shipped properties: comments, blank line, unrelated keys. */
    private val original = """
        #Minecraft server properties
        #Thu Sep 25 12:00:00 CEST 2026
        enable-query=false
        enable-rcon=false
        motd=A ServerPackCreator server pack

        query.port=25565
        rcon.port=25575
        server-ip=
        server-port=25565
        difficulty=hard
    """.trimIndent() + "\n"

    private fun packWithProperties(directory: File, contents: String = original): File =
        File(directory, ServerPropertiesPatch.PROPERTIES_NAME).apply { writeText(contents) }

    /** The port changes; every other byte — comments, blank line, ordering, unrelated keys — survives. */
    @Test
    fun rewritesOnlyThePortLines(@TempDir packDir: File) {
        val properties = packWithProperties(packDir)

        ServerPropertiesPatch(packDir).borrow(30123)

        Assertions.assertEquals(
            original.replace("query.port=25565", "query.port=30123").replace("server-port=25565", "server-port=30123"),
            properties.readText()
        )
    }

    /** The backup holds the original bytes, so restoring cannot be approximate. */
    @Test
    fun keepsTheOriginalBytesInTheBackup(@TempDir packDir: File) {
        packWithProperties(packDir)
        val patch = ServerPropertiesPatch(packDir)

        patch.borrow(30123)

        Assertions.assertEquals(original, patch.backupFile.readText())
    }

    /** Giving the file back means byte-for-byte, and leaving no backup behind. */
    @Test
    fun restorePutsTheOriginalBackAndRemovesTheBackup(@TempDir packDir: File) {
        val properties = packWithProperties(packDir)
        val patch = ServerPropertiesPatch(packDir)
        patch.borrow(30123)

        patch.restore()

        Assertions.assertEquals(original, properties.readText())
        Assertions.assertFalse(patch.backupFile.exists(), "A returned borrow must leave no backup behind.")
    }

    /**
     * The crash path, and the reason the backup exists as a file rather than as a field.
     *
     * A ServerPackCreator killed mid-run leaves the patched file and its backup. The next borrow must restore
     * first, so what it backs up is the user's port — not the previous run's borrowed one, which would make
     * the borrowed port permanent the moment it was finally restored.
     */
    @Test
    fun restoresAnOutstandingBackupBeforeBorrowingAgain(@TempDir packDir: File) {
        packWithProperties(packDir)
        ServerPropertiesPatch(packDir).borrow(30123)

        val secondRun = ServerPropertiesPatch(packDir)
        secondRun.borrow(30456)

        Assertions.assertEquals(
            original,
            secondRun.backupFile.readText(),
            "The backup must hold the user's own file, not the previous run's patched one."
        )
        secondRun.restore()
        Assertions.assertEquals(original, File(packDir, ServerPropertiesPatch.PROPERTIES_NAME).readText())
    }

    /**
     * A pack generated without `server.properties` still needs a port, so one is created — and restoring puts
     * the pack back to having no such file, because that is what "as it was" means here.
     */
    @Test
    fun createsAndThenRemovesAPropertiesFileThatDidNotExist(@TempDir packDir: File) {
        val patch = ServerPropertiesPatch(packDir)

        patch.borrow(30123)

        Assertions.assertTrue(patch.propertiesFile.isFile, "A pack with no properties still needs its port set.")
        Assertions.assertTrue(patch.propertiesFile.readText().contains("server-port=30123"))

        patch.restore()

        Assertions.assertFalse(
            patch.propertiesFile.exists(),
            "The pack had no server.properties before the test, so it must have none after."
        )
    }

    /** A key the file does not carry is appended rather than silently dropped. */
    @Test
    fun appendsAPortKeyTheFileDoesNotCarry(@TempDir packDir: File) {
        val properties = packWithProperties(packDir, "motd=Barebones\n")

        ServerPropertiesPatch(packDir).borrow(30123)

        Assertions.assertTrue(properties.readText().contains("motd=Barebones"), "The existing key must survive.")
        Assertions.assertTrue(properties.readText().contains("server-port=30123"), "The missing key must be added.")
    }

    /** RCON is read from the file as it stands, because it decides whether a second port must be allocated. */
    @Test
    fun readsWhetherRconIsEnabled(@TempDir packDir: File) {
        packWithProperties(packDir)
        Assertions.assertFalse(ServerPropertiesPatch(packDir).rconEnabled(), "The shipped default is off.")

        packWithProperties(packDir, original.replace("enable-rcon=false", "enable-rcon=true"))
        Assertions.assertTrue(ServerPropertiesPatch(packDir).rconEnabled())
    }

    /** RCON's port is left alone unless a port was supplied for it; when one is, it is used verbatim. */
    @Test
    fun movesRconOnlyWhenGivenAPortForIt(@TempDir packDir: File) {
        val properties = packWithProperties(packDir)

        ServerPropertiesPatch(packDir).borrow(30123)
        Assertions.assertTrue(properties.readText().contains("rcon.port=25575"), "No rcon port given, none set.")

        ServerPropertiesPatch(packDir).borrow(30123, rconPort = 30124)
        Assertions.assertTrue(properties.readText().contains("rcon.port=30124"))
    }

    /**
     * A properties file written on Windows keeps its CRLF endings. Rewriting them to LF would make every line
     * of the user's file show as changed in any diff they ran against it.
     */
    @Test
    fun preservesWindowsLineEndings(@TempDir packDir: File) {
        // Carries both port keys, so what this asserts is the line endings and nothing else. A fixture
        // missing one of them would also exercise the append path, whose own separator choice is a
        // different question -- pinned by appendsAPortKeyTheFileDoesNotCarry.
        val crlf = "motd=Windows\r\nquery.port=25565\r\nserver-port=25565\r\ndifficulty=hard\r\n"
        val properties = packWithProperties(packDir, crlf)

        ServerPropertiesPatch(packDir).borrow(30123)

        Assertions.assertEquals(
            "motd=Windows\r\nquery.port=30123\r\nserver-port=30123\r\ndifficulty=hard\r\n",
            properties.readText()
        )
    }

    /**
     * A key carried twice is rewritten in both places.
     *
     * Legal in a `.properties` file and Minecraft takes the last, so leaving an earlier stale line behind
     * would be harmless today and a trap the moment anything reads the file top-down. Pinned because the
     * implementation replaces every occurrence and nothing said so.
     */
    @Test
    fun rewritesEveryOccurrenceOfAPortKey(@TempDir packDir: File) {
        val properties = packWithProperties(packDir, "server-port=25565\nmotd=Twice\nserver-port=25566\n")

        ServerPropertiesPatch(packDir).borrow(30123)

        Assertions.assertEquals(
            "server-port=30123\nmotd=Twice\nserver-port=30123\nquery.port=30123\n",
            properties.readText()
        )
    }

    /**
     * Borrowing twice through the *same* instance still backs up the user's file, not the first borrow's.
     *
     * The crash-recovery guard covers two instances, which is the across-processes case. This is the one a
     * double-click on Start produces, and it goes through the same restore-before-borrow path.
     */
    @Test
    fun borrowingTwiceThroughOneInstanceKeepsTheUsersFile(@TempDir packDir: File) {
        packWithProperties(packDir)
        val patch = ServerPropertiesPatch(packDir)

        patch.borrow(30123)
        patch.borrow(30456)

        Assertions.assertEquals(original, patch.backupFile.readText())
        patch.restore()
        Assertions.assertEquals(original, patch.propertiesFile.readText())
    }

    /** A pack with no properties file at all reports RCON off rather than throwing. */
    @Test
    fun rconIsOffWhenThereIsNoPropertiesFile(@TempDir packDir: File) {
        Assertions.assertFalse(ServerPropertiesPatch(packDir).rconEnabled())
    }

    /** Restoring without an outstanding borrow does nothing, so a shutdown hook may always call it. */
    @Test
    fun restoreWithoutABorrowIsHarmless(@TempDir packDir: File) {
        val properties = packWithProperties(packDir)

        ServerPropertiesPatch(packDir).restore()

        Assertions.assertEquals(original, properties.readText())
    }
}
