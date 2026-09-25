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

import java.io.File

/**
 * Every server this plugin has running, and the one place that guarantees none outlives ServerPackCreator.
 *
 * Two jobs, both of which a per-tab collection would get wrong.
 *
 * **One session per pack.** The key is the pack's own directory. Two servers over one `world/` directory
 * corrupt it, so a pack that is already running cannot be started again — and because the key is the
 * directory rather than the displayed name, two packs that merely look alike are still distinct.
 *
 * **Nothing survives the application.** `ApiPlugins.addTabExtensionTabs` gives a plugin no close callback and
 * `MainPanel.closeAndExit` is not reachable from `-api`, so a JVM shutdown hook is the only join point a
 * plugin has. It covers a normal quit and a Ctrl+C; it cannot cover `SIGKILL`, which is stated here rather
 * than pretended away — a server surviving that is reachable, and the next launch's bind test will simply
 * hand out a different port.
 *
 * @author Griefed
 */
class SessionRegistry {

    /** Live sessions by pack directory. Synchronized: sessions end on their own reader threads. */
    private val sessions = mutableMapOf<String, ServerSession>()

    /** Whether the shutdown hook has been installed, so repeated tab construction installs only one. */
    private var hookInstalled = false

    /**
     * Take ownership of [session] for [packDirectory], or refuse because that pack is already running.
     *
     * Returns false rather than throwing: the caller is a button handler, and "this pack is already running"
     * is an answer to show, not an error.
     */
    @Synchronized
    fun register(packDirectory: File, session: ServerSession): Boolean {
        val key = keyFor(packDirectory)
        if (sessions.containsKey(key)) {
            return false
        }
        sessions[key] = session
        return true
    }

    /** Forget the session for [packDirectory]; called when its process has gone. */
    @Synchronized
    fun unregister(packDirectory: File) {
        sessions.remove(keyFor(packDirectory))
    }

    /** Whether a server is running out of [packDirectory] right now. */
    @Synchronized
    fun isRunning(packDirectory: File): Boolean = sessions.containsKey(keyFor(packDirectory))

    /** How many servers this plugin currently has running. */
    @Synchronized
    fun runningCount(): Int = sessions.size

    /**
     * Kill every running server.
     *
     * Kills rather than asks: this runs from the shutdown hook, where the JVM is already going and a
     * `stop` typed onto a pipe would need a reply nobody is left to read. Iterates a snapshot, because
     * each kill ends a session that unregisters itself from the same map.
     */
    @Synchronized
    fun killAll() {
        for (session in sessions.values.toList()) {
            runCatching { session.kill() }
        }
        sessions.clear()
    }

    /**
     * Install the JVM shutdown hook that calls [killAll], once.
     *
     * Idempotent because a tab may be constructed more than once in a JVM — a theme change rebuilds the
     * component tree — and a second hook would kill an already-empty registry for no reason.
     */
    @Synchronized
    fun installShutdownHook() {
        if (hookInstalled) {
            return
        }
        hookInstalled = true
        Runtime.getRuntime().addShutdownHook(Thread(::killAll, "servertest-shutdown"))
    }

    /** The identity a pack is tracked under: its canonical path, so two spellings of one directory agree. */
    private fun keyFor(packDirectory: File): String =
        runCatching { packDirectory.canonicalPath }.getOrElse { packDirectory.absolutePath }
}
