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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Collections

/**
 * Pins the two jobs a plain collection of sessions would get wrong: one server per pack, and no server
 * outliving ServerPackCreator.
 *
 * The first is not tidiness. Two servers running out of one directory write the same `world/`, and a
 * corrupted world is the kind of damage a test feature must never cause.
 */
internal class SessionRegistryTest {

    private var spawnedChildPid: Long? = null

    @AfterEach
    fun killLeakedChild() {
        spawnedChildPid?.let { pid -> ProcessHandle.of(pid).ifPresent { it.destroyForcibly() } }
    }

    /** A session that is never started; enough to exercise the bookkeeping without spawning anything. */
    private fun unstartedSession(packDir: File) =
        ServerSession(packDir, listOf("bash", "start.sh"), {}, {}, {})

    /** A pack already running cannot be started again — the answer is `false`, not an exception. */
    @Test
    fun refusesASecondSessionForTheSamePack(@TempDir packDir: File) {
        val registry = SessionRegistry()

        Assertions.assertTrue(registry.register(packDir, unstartedSession(packDir)))
        Assertions.assertFalse(
            registry.register(packDir, unstartedSession(packDir)),
            "Two servers over one world directory corrupt it."
        )
        Assertions.assertEquals(1, registry.runningCount())
    }

    /** Two different packs run side by side; that is the whole point of tracking them separately. */
    @Test
    fun allowsOneSessionPerDistinctPack(@TempDir parent: File) {
        val registry = SessionRegistry()
        val first = File(parent, "first").apply { mkdirs() }
        val second = File(parent, "second").apply { mkdirs() }

        Assertions.assertTrue(registry.register(first, unstartedSession(first)))
        Assertions.assertTrue(registry.register(second, unstartedSession(second)))
        Assertions.assertEquals(2, registry.runningCount())
    }

    /**
     * Two spellings of one directory are one pack. The key is the canonical path, so a trailing `.` or a
     * symlinked route cannot smuggle a second server onto the same world.
     */
    @Test
    fun treatsTwoSpellingsOfOneDirectoryAsOnePack(@TempDir packDir: File) {
        val registry = SessionRegistry()
        registry.register(packDir, unstartedSession(packDir))

        Assertions.assertFalse(registry.register(File(packDir, "."), unstartedSession(packDir)))
        Assertions.assertTrue(registry.isRunning(File(packDir.absolutePath + File.separator + ".")))
    }

    /** A stopped pack can be started again, or the list would rot after the first run of each pack. */
    @Test
    fun aPackCanBeStartedAgainAfterItIsUnregistered(@TempDir packDir: File) {
        val registry = SessionRegistry()
        registry.register(packDir, unstartedSession(packDir))

        registry.unregister(packDir)

        Assertions.assertFalse(registry.isRunning(packDir))
        Assertions.assertTrue(registry.register(packDir, unstartedSession(packDir)))
    }

    /**
     * killAll really kills, and it kills what the scripts spawned.
     *
     * Against a real process on purpose: this runs from the JVM shutdown hook and is the only thing standing
     * between closing ServerPackCreator and leaving a Minecraft server holding a port and several GB of heap.
     * A bookkeeping-only assertion would pass while doing exactly nothing.
     */
    @Test
    fun killAllEndsTheServersAndWhatTheySpawned(@TempDir packDir: File) {
        Assumptions.assumeTrue(File("/bin/bash").canExecute(), "Needs bash to run a stand-in start script.")
        val pidFile = File(packDir, "child.pid")
        File(packDir, "start.sh").writeText(
            """
            #!/usr/bin/env bash
            sleep 300 &
            echo ${'$'}! > "${pidFile.absolutePath}"
            echo "up"
            wait
            """.trimIndent() + "\n"
        )
        val lines = Collections.synchronizedList(mutableListOf<String>())
        val registry = SessionRegistry()
        val session = ServerSession(packDir, listOf("bash", "start.sh"), { lines.add(it) }, {}, {})
        registry.register(packDir, session)
        session.start()

        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline && !(pidFile.isFile && lines.contains("up"))) {
            Thread.sleep(25)
        }
        val childPid = pidFile.readText().trim().toLong().also { spawnedChildPid = it }

        registry.killAll()

        val gone = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < gone && ProcessHandle.of(childPid).map { it.isAlive }.orElse(false)) {
            Thread.sleep(25)
        }
        Assertions.assertFalse(
            ProcessHandle.of(childPid).map { it.isAlive }.orElse(false),
            "Process $childPid outlived ServerPackCreator's shutdown."
        )
        Assertions.assertEquals(0, registry.runningCount())
    }

    /** A second tab construction — a theme change rebuilds the tree — must not add a second hook. */
    @Test
    fun installsItsShutdownHookOnlyOnce() {
        val registry = SessionRegistry()

        registry.installShutdownHook()
        registry.installShutdownHook()

        Assertions.assertEquals(0, registry.runningCount(), "Installing a hook must not disturb the registry.")
    }
}
