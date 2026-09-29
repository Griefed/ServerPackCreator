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
package de.griefed.serverpackcreator.plugin.servertest.gui

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionTab
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.plugin.servertest.core.LaunchablePack
import de.griefed.serverpackcreator.plugin.servertest.core.PackVariables
import de.griefed.serverpackcreator.plugin.servertest.core.PortAllocator
import de.griefed.serverpackcreator.plugin.servertest.core.ServerPackCatalog
import de.griefed.serverpackcreator.plugin.servertest.core.ServerPropertiesPatch
import de.griefed.serverpackcreator.plugin.servertest.core.ServerSession
import de.griefed.serverpackcreator.plugin.servertest.core.ServerTestSettings
import de.griefed.serverpackcreator.plugin.servertest.core.SessionRegistry
import de.griefed.serverpackcreator.plugin.servertest.core.SessionState
import de.griefed.serverpackcreator.plugin.servertest.core.StartScriptSelection
import java.awt.BorderLayout
import java.io.File
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JOptionPane
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities

/**
 * The plugin's single tab: the pack list, plus one console per running server.
 *
 * A `TabExtension` yields exactly one tab, so these are a nested [JTabbedPane] — the list is always index 0
 * and starting a pack appends a console beside it.
 *
 * **This constructor must not throw.** `ApiPlugins.addTabExtensionTabs` calls `getTab` *outside* the
 * try-block guarding the rest of tab registration, so an exception here escapes into GUI assembly instead of
 * being logged as a plugin error. Everything that can fail is reported in the pane.
 *
 * Threading: no coroutines. A plugin cannot reach ServerPackCreator's lifecycle-cancelled
 * `ComponentCoroutineScope`, and an unowned `GlobalScope` is the anti-pattern this project spent a sprint
 * removing from its own GUI. A session's console arrives on its reader thread and every pane marshals to the
 * event dispatch thread itself.
 *
 * @author Griefed
 */
class ServerTestTab(
    versionMeta: VersionMeta,
    apiProperties: ApiProperties,
    utilities: Utilities,
    pluginConfig: Optional<CommentedConfig>,
    configFile: Optional<File>
) : ExtensionTab(versionMeta, apiProperties, utilities, pluginConfig, configFile) {

    private val settings = ServerTestSettings(pluginConfig.orElseGet { CommentedConfig.inMemory() })
    private val catalog = ServerPackCatalog(utilities.jsonUtilities.objectMapper)
    private val allocator = PortAllocator(settings.portRangeStart, settings.portRangeEnd)
    private val registry = SessionRegistry()

    /** The pack list plus one console per running server; the list is always the first tab. */
    private val panes = JTabbedPane()

    private val packList = PackListPane(onStart = ::launch, onRefresh = ::refreshPackList)

    /** Consoles by pack directory, so a state change can find the pane it belongs to. */
    private val consoles = mutableMapOf<File, ConsolePane>()

    /**
     * The last state reported per pack, kept after the session ends so the list can still say how a run
     * finished. Whether a pack is *running* is asked of the registry, never inferred from this.
     */
    private val lastStates = mutableMapOf<File, SessionState>()

    init {
        layout = BorderLayout()
        panes.addTab("Server Packs", packList)
        add(panes, BorderLayout.CENTER)

        registry.installShutdownHook()
        refreshPackList()
    }

    /** Re-read the server-packs directory and redraw the list with each pack's current state. */
    private fun refreshPackList() {
        val packs = catalog.packsIn(apiProperties.serverPacksDirectory)
        packList.show(packs.map { PackRow(it, lastStates[it.directory], registry.isRunning(it.directory)) })
    }

    /**
     * Launch [pack]: take a port, borrow its `server.properties`, start the script and open its console.
     *
     * The order matters on the way out as much as on the way in, which is why releasing the port and giving
     * the properties file back are one callback handed to the session rather than steps at each exit.
     */
    private fun launch(pack: LaunchablePack) {
        val selection = pack.selection as? StartScriptSelection.Available ?: return

        if (registry.isRunning(pack.directory)) {
            warn("${pack.name} is already running. Two servers over one world directory would corrupt it.")
            return
        }

        val patch = ServerPropertiesPatch(pack.directory)
        val serverPort = allocator.allocate()
        if (serverPort == null) {
            warn(
                "No free port between ${allocator.range.first} and ${allocator.range.last}. Stop a running " +
                        "server, or widen the range in this plugin's config.toml."
            )
            return
        }
        // Only when the pack has RCON switched on: it is a second real listening socket, and two test
        // servers sharing one collide exactly as their game ports would.
        val rconPort = if (patch.rconEnabled()) allocator.allocate() else null

        val givenBack = AtomicBoolean(false)
        val giveBack = {
            // Guarded because it is reachable from two places: the session's close callback, and the failure
            // path below when the borrow itself fails before any session exists.
            if (givenBack.compareAndSet(false, true)) {
                runCatching { patch.restore() }
                    .onFailure { log.error("Could not restore ${patch.propertiesFile.absolutePath}.", it) }
                allocator.release(serverPort)
                rconPort?.let(allocator::release)
            }
        }

        try {
            patch.borrow(serverPort, rconPort)
        } catch (ex: Exception) {
            giveBack()
            warn("Could not set the port in ${pack.name}'s server.properties: ${ex.message}")
            return
        }

        val session = ServerSession(
            workingDirectory = pack.directory,
            command = selection.command,
            onLine = { line -> consoles[pack.directory]?.appendLine(line) },
            onState = { state -> onSessionState(pack, state) },
            onClosed = {
                giveBack()
                registry.unregister(pack.directory)
                SwingUtilities.invokeLater { refreshPackList() }
            }
        )

        // Registered before it is started, so a second Start pressed in the same instant is refused rather
        // than racing into a second server over the same world.
        if (!registry.register(pack.directory, session)) {
            giveBack()
            warn("${pack.name} is already running.")
            return
        }

        val console = ConsolePane(
            session = session,
            port = serverPort,
            variables = PackVariables.read(pack.directory),
            scrollback = settings.consoleScrollback
        )
        consoles[pack.directory] = console
        lastStates[pack.directory] = SessionState.Starting

        panes.addTab(pack.name, console)
        panes.selectedComponent = console
        refreshPackList()

        session.start()
    }

    /** Push a state change to the pack's console and to the list, on the event dispatch thread. */
    private fun onSessionState(pack: LaunchablePack, state: SessionState) {
        consoles[pack.directory]?.showState(state)
        SwingUtilities.invokeLater {
            lastStates[pack.directory] = state
            refreshPackList()
        }
    }

    /** Say something the user needs to act on, rendered as literal text. */
    private fun warn(message: String) {
        log.warn(message)
        JOptionPane.showMessageDialog(this, message, "Server Test", JOptionPane.WARNING_MESSAGE)
    }
}
