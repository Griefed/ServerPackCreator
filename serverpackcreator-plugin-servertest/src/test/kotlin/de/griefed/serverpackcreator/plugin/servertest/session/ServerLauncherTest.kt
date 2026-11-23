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
package de.griefed.serverpackcreator.plugin.servertest.session

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Collections
import java.util.Random
import de.griefed.serverpackcreator.plugin.servertest.pack.LaunchablePack
import de.griefed.serverpackcreator.plugin.servertest.pack.ServerPropertiesPatch
import de.griefed.serverpackcreator.plugin.servertest.pack.StartScripts

/**
 * Pins the order things are taken in, and that everything taken is given back exactly once.
 *
 * None of this is rendering: it decides whether a pack may run at all, how many ports it costs, and
 * whether a user's `server.properties` comes back. The give-back is reachable from two directions, which
 * is why it carries
 * an atomic guard, and nothing asserted that guard held.
 */
internal class ServerLauncherTest {

    /** The script every launch here uses unless a test says otherwise. */
    private val shellScript = StartScripts.forKey("sh")

    /** A pack that can be launched, rooted at a real directory so the properties borrow has somewhere to go. */
    private fun packAt(directory: File, name: String = directory.name) = LaunchablePack(
        directory = directory,
        name = name,
        minecraftVersion = "1.21",
        modloader = "NeoForge",
        modloaderVersion = "21.0.18",
        scriptKeysPresent = setOf("sh")
    )

    /** An allocator over a known range whose ports are all free, so counts are exact. */
    private fun allocator(first: Int = 31000, last: Int = 31009, free: (Int) -> Boolean = { true }) =
        PortAllocator(first, last, object : Random() {
            override fun nextInt(bound: Int): Int = 0
        }, free)

    private fun launcherOver(allocator: PortAllocator, registry: SessionRegistry = SessionRegistry()) =
        ServerLauncher(allocator, registry)

    /** The happy path: a port is taken, written into the pack, and the pack is registered but not started. */
    @Test
    fun takesAPortWritesItIntoThePackAndRegistersTheSession(@TempDir packDir: File) {
        File(packDir, ServerPropertiesPatch.PROPERTIES_NAME).writeText("motd=Test\nserver-port=25565\n")
        val registry = SessionRegistry()
        val outcome = launcherOver(allocator(), registry).launch(packAt(packDir), shellScript, {}, {}, {})

        val started = Assertions.assertInstanceOf(LaunchOutcome.Started::class.java, outcome)
        Assertions.assertEquals(31000, started.port)
        Assertions.assertTrue(
            File(packDir, ServerPropertiesPatch.PROPERTIES_NAME).readText().contains("server-port=31000"),
            "The pack must be pointed at the port the user is told to connect to."
        )
        Assertions.assertTrue(registry.isRunning(packDir), "Registered before it is started, so a second Start cannot race.")
        Assertions.assertEquals(SessionState.Starting, started.session.state, "launch() must not start the process.")
    }

    /** Two servers over one world directory corrupt it, so the second ask is refused and costs nothing. */
    @Test
    fun refusesAPackThatIsAlreadyRunningAndTakesNothing(@TempDir packDir: File) {
        val allocator = allocator()
        val launcher = launcherOver(allocator, SessionRegistry().also { it.register(packDir, unstarted(packDir)) })

        val outcome = launcher.launch(packAt(packDir, "Occupied"), shellScript, {}, {}, {})

        val refused = Assertions.assertInstanceOf(LaunchOutcome.Refused::class.java, outcome)
        Assertions.assertTrue(refused.reason.contains("Occupied"), "The refusal must name the pack: ${refused.reason}")
        Assertions.assertEquals(31000, allocator.allocate(), "No port may have been taken by a refused launch.")
        Assertions.assertFalse(
            File(packDir, ServerPropertiesPatch.BACKUP_NAME).exists(),
            "A refused launch must not have borrowed anything."
        )
    }

    /** With every port busy there is nothing to hand out, and the refusal says so rather than launching. */
    @Test
    fun refusesWhenNoPortIsFree(@TempDir packDir: File) {
        val outcome = launcherOver(allocator(free = { false })).launch(packAt(packDir), shellScript, {}, {}, {})

        val refused = Assertions.assertInstanceOf(LaunchOutcome.Refused::class.java, outcome)
        Assertions.assertTrue(refused.reason.contains("31000"), "The refusal must name the range it searched.")
        Assertions.assertFalse(File(packDir, ServerPropertiesPatch.BACKUP_NAME).exists())
    }

    /** RCON off costs one port. It is off in ServerPackCreator's shipped properties, so this is the norm. */
    @Test
    fun takesOnePortWhenRconIsOff(@TempDir packDir: File) {
        File(packDir, ServerPropertiesPatch.PROPERTIES_NAME).writeText("enable-rcon=false\nserver-port=25565\n")
        val allocator = allocator()

        launcherOver(allocator).launch(packAt(packDir), shellScript, {}, {}, {})

        Assertions.assertEquals(31001, allocator.allocate(), "Only one port should have been taken.")
    }

    /** RCON on is a second real listening socket, so it gets a port of its own rather than sharing. */
    @Test
    fun takesASecondPortWhenRconIsOn(@TempDir packDir: File) {
        File(packDir, ServerPropertiesPatch.PROPERTIES_NAME)
            .writeText("enable-rcon=true\nrcon.port=25575\nserver-port=25565\n")
        val allocator = allocator()

        launcherOver(allocator).launch(packAt(packDir), shellScript, {}, {}, {})

        Assertions.assertEquals(31002, allocator.allocate(), "Two ports should have been taken, not one.")
        Assertions.assertTrue(
            File(packDir, ServerPropertiesPatch.PROPERTIES_NAME).readText().contains("rcon.port=31001"),
            "The second port must actually reach the pack."
        )
    }

    /**
     * A borrow that fails must leave nothing taken. Otherwise a pack whose properties cannot be written
     * silently costs a port on every attempt until the range is exhausted.
     */
    @Test
    fun givesEverythingBackWhenTheBorrowFails(@TempDir packDir: File) {
        val allocator = allocator()
        // A real filesystem condition rather than a stub: something else already occupies the path the
        // borrow has to write, so `writeText` fails exactly as it would on a read-only mount.
        File(packDir, ServerPropertiesPatch.PROPERTIES_NAME).mkdirs()

        val outcome = launcherOver(allocator).launch(packAt(packDir), shellScript, {}, {}, {})

        Assertions.assertInstanceOf(LaunchOutcome.Refused::class.java, outcome)
        Assertions.assertEquals(31000, allocator.allocate(), "A failed borrow must give its port back.")
    }

    /**
     * The give-back runs once however often the session reports itself closed.
     *
     * Reachable twice for real: the session's own close callback, and the failure path that releases before
     * a session exists. A second release would hand a live server's port to the next pack.
     */
    @Test
    fun releasesExactlyOnceEvenIfTheSessionReportsClosedTwice(@TempDir packDir: File) {
        File(packDir, ServerPropertiesPatch.PROPERTIES_NAME).writeText("server-port=25565\n")
        val allocator = allocator()
        val registry = SessionRegistry()
        var callerNotified = 0
        var wiredClose: (() -> Unit)? = null
        val launcher = ServerLauncher(
            allocator,
            registry,
            { ServerPropertiesPatch(it) },
            { directory, command, onLine, onState, onClosed ->
                wiredClose = onClosed
                ServerSession(directory, command, onLine, onState, onClosed)
            }
        )

        launcher.launch(packAt(packDir), shellScript, {}, {}, { callerNotified++ })

        // Invoked twice on purpose: the give-back is reachable from the session's own close callback and
        // from the failure path, and a second release would hand a live server's port to the next pack.
        requireNotNull(wiredClose).invoke()
        requireNotNull(wiredClose).invoke()

        Assertions.assertEquals(1, callerNotified, "The caller must be told once.")
        Assertions.assertFalse(registry.isRunning(packDir), "A closed session must be unregistered.")
        Assertions.assertFalse(File(packDir, ServerPropertiesPatch.BACKUP_NAME).exists(), "The borrow must be returned.")
        Assertions.assertEquals(
            "server-port=25565\n",
            File(packDir, ServerPropertiesPatch.PROPERTIES_NAME).readText(),
            "The user's properties must come back exactly."
        )
        Assertions.assertEquals(31000, allocator.allocate(), "The port must be released once, and be reusable.")
    }

    /**
     * A borrow that cannot be returned leaves the user's `server.properties` on the borrowed port, with the
     * original sitting in a backup only this plugin knows the name of. The user has to be told where it is, in
     * the console they are looking at and in the log, or their real port is silently gone.
     */
    @Test
    fun aRestoreThatFailsTellsTheUserWhereTheirOriginalIs(@TempDir packDir: File) {
        val properties = File(packDir, ServerPropertiesPatch.PROPERTIES_NAME).apply { writeText("server-port=25565\n") }
        val backup = File(packDir, ServerPropertiesPatch.BACKUP_NAME)
        val allocator = allocator()
        val consoleLines = mutableListOf<String>()
        var wiredClose: (() -> Unit)? = null
        val launcher = ServerLauncher(
            allocator,
            SessionRegistry(),
            { ServerPropertiesPatch(it) },
            { directory, command, onLine, onState, onClosed ->
                wiredClose = onClosed
                ServerSession(directory, command, onLine, onState, onClosed)
            }
        )
        launcher.launch(packAt(packDir), shellScript, { consoleLines.add(it) }, {}, {})
        // A real filesystem condition: something now occupies the path the restore has to write back to, so the
        // copy cannot replace it -- as a file locked by another program or a permissions change would.
        properties.delete()
        File(properties, "occupied").apply { parentFile.mkdirs() }.writeText("")

        val errors = capturingErrorsOf("AddonsLogger") { requireNotNull(wiredClose).invoke() }

        Assertions.assertTrue(backup.isFile, "the original must still be there to be restored by hand")
        Assertions.assertTrue(
            consoleLines.any { it.contains(backup.absolutePath) && it.contains(properties.absolutePath) },
            "the console must name the backup and the file it belongs over: $consoleLines"
        )
        Assertions.assertTrue(
            errors.any { it.message.formattedMessage.contains(backup.absolutePath) && it.thrown != null },
            "the log must carry the backup's location and the cause at ERROR"
        )
        Assertions.assertEquals(31000, allocator.allocate(), "the port is still given back")
    }

    /** The ERROR events [logger] records while [block] runs. */
    private fun capturingErrorsOf(logger: String, block: () -> Unit): List<LogEvent> {
        val target = LogManager.getLogger(logger) as Logger
        val events = Collections.synchronizedList(mutableListOf<LogEvent>())
        val appender = object : AbstractAppender("capture", null, null, true, Property.EMPTY_ARRAY) {
            override fun append(event: LogEvent) {
                if (event.level == Level.ERROR) {
                    events.add(event.toImmutable())
                }
            }
        }.apply { start() }
        target.addAppender(appender)
        try {
            block()
        } finally {
            target.removeAppender(appender)
            appender.stop()
        }
        return events.toList()
    }

    /**
     * The script the user picked is the script that runs — and a pack without it is refused by name.
     *
     * The launcher must not fall back to a script the pack happens to have: the user asked for one, and
     * quietly running another hides what actually happened.
     */
    @Test
    fun launchesWithTheScriptTheUserChoseAndRefusesAPackWithoutIt(@TempDir packDir: File) {
        File(packDir, ServerPropertiesPatch.PROPERTIES_NAME).writeText("server-port=25565\n")
        val pack = LaunchablePack(
            directory = packDir,
            name = packDir.name,
            minecraftVersion = "1.21",
            modloader = "NeoForge",
            modloaderVersion = "21.0.18",
            scriptKeysPresent = setOf("sh", "fish")
        )

        val chosen = launcherOver(allocator()).launch(pack, StartScripts.forKey("fish"), {}, {}, {})
        Assertions.assertInstanceOf(LaunchOutcome.Started::class.java, chosen)

        val refused = launcherOver(allocator(31100, 31109))
            .launch(pack, StartScripts.forKey("bat"), {}, {}, {})
        Assertions.assertTrue(
            (refused as LaunchOutcome.Refused).reason.contains("start.bat"),
            "A pack without the chosen script must be refused by name: ${refused.reason}"
        )
    }

    private fun unstarted(packDir: File) = ServerSession(packDir, listOf("bash", "start.sh"), {}, {}, {})
}
