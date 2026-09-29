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

import de.griefed.serverpackcreator.plugin.servertest.core.LaunchablePack
import de.griefed.serverpackcreator.plugin.servertest.core.SessionState
import de.griefed.serverpackcreator.plugin.servertest.core.StartScript
import de.griefed.serverpackcreator.plugin.servertest.core.StartScripts
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins what the pack list says, and when Start is offered.
 *
 * The pane's *rendering* is untested by design — this project's standing stance on tables — but the model is
 * not: which phrase a row shows and whether its Start button lights up is the logic a user reads, and it is
 * decided here rather than by Swing.
 */
internal class PackTableModelTest {

    private fun pack(
        name: String = "NeoForge-1.21",
        scripts: Set<String> = setOf("sh")
    ) = LaunchablePack(File("/packs/$name"), name, "1.21", "NeoForge", "21.0.18", scripts)

    /** A row for [pack] under the script the user has chosen; SH unless a test says otherwise. */
    private fun row(
        pack: LaunchablePack,
        state: SessionState? = null,
        running: Boolean = false,
        script: StartScript? = StartScripts.forKey("sh")
    ) = PackRow(pack, state, running, script)

    /** The columns carry the manifest's facts, in the documented order. */
    @Test
    fun eachColumnShowsItsOwnFact() {
        val model = PackTableModel().apply { rows = listOf(row(pack())) }

        Assertions.assertEquals("NeoForge-1.21", model.getValueAt(0, PackTableModel.NAME_COLUMN))
        Assertions.assertEquals("1.21", model.getValueAt(0, PackTableModel.MINECRAFT_COLUMN))
        Assertions.assertEquals("NeoForge", model.getValueAt(0, PackTableModel.MODLOADER_COLUMN))
        Assertions.assertEquals("21.0.18", model.getValueAt(0, PackTableModel.MODLOADER_VERSION_COLUMN))
        Assertions.assertEquals("Ready to launch", model.getValueAt(0, PackTableModel.STATUS_COLUMN))
        Assertions.assertEquals(PackTableModel.COLUMNS.size, model.columnCount)
    }

    /**
     * A blocked pack shows its reason in the Status column, which is the answer to the only question such a
     * row raises: why is its Start button grey?
     */
    @Test
    fun aBlockedPackShowsItsReasonAndCannotBeStarted() {
        val reason = "start.sh"
        val model = PackTableModel().apply {
            rows = listOf(row(pack(scripts = emptySet())))
        }

        Assertions.assertTrue(
            (model.getValueAt(0, PackTableModel.STATUS_COLUMN) as String).contains(reason),
            "A blocked row must name the script the user chose and the pack lacks."
        )
        Assertions.assertFalse(model.startableAt(0))
    }

    /** A running pack cannot be started again — two servers over one world directory corrupt it. */
    @Test
    fun aRunningPackCannotBeStartedAgain() {
        val model = PackTableModel().apply {
            rows = listOf(row(pack(), SessionState.Ready, running = true))
        }

        Assertions.assertEquals("Running", model.getValueAt(0, PackTableModel.STATUS_COLUMN))
        Assertions.assertFalse(model.startableAt(0))
    }

    /**
     * A pack that has already been tested can be tested again, while still showing how the last run ended.
     *
     * The distinction the row is built on: a lingering `Exited` state is not the same as a live session, and
     * keying Start on the state rather than on liveness would let a pack be tested exactly once.
     */
    @Test
    fun aStoppedPackKeepsItsExitStatusAndBecomesStartableAgain() {
        val model = PackTableModel().apply {
            rows = listOf(row(pack(), SessionState.Exited(0), running = false))
        }

        Assertions.assertEquals("Stopped (exit 0)", model.getValueAt(0, PackTableModel.STATUS_COLUMN))
        Assertions.assertTrue(model.startableAt(0), "A pack that has stopped must be testable again.")
    }

    /** A force-killed session has no exit status, and says so rather than showing a misleading number. */
    @Test
    fun aForceKilledSessionReportsAnUnknownStatus() {
        val model = PackTableModel().apply {
            rows = listOf(row(pack(), SessionState.Exited(null), running = false))
        }

        Assertions.assertEquals("Stopped (exit unknown)", model.getValueAt(0, PackTableModel.STATUS_COLUMN))
    }

    /** The in-between states are named, so a long modloader install does not look like nothing happening. */
    @Test
    fun theTransientStatesAreNamed() {
        val model = PackTableModel().apply {
            rows = listOf(
                row(pack("starting"), SessionState.Starting, running = true),
                row(pack("stopping"), SessionState.Stopping, running = true)
            )
        }

        Assertions.assertEquals("Starting…", model.getValueAt(0, PackTableModel.STATUS_COLUMN))
        Assertions.assertEquals("Stopping…", model.getValueAt(1, PackTableModel.STATUS_COLUMN))
    }

    /**
     * With no start-script templates configured there is nothing to run, and the row says so.
     *
     * Reachable: the templates are a user-editable setting and can be emptied, which makes generations
     * produce no start scripts at all.
     */
    @Test
    fun aRowWithNoConfiguredScriptSaysSoAndCannotBeStarted() {
        val model = PackTableModel().apply { rows = listOf(row(pack(), script = null)) }

        Assertions.assertEquals(
            StartScripts.NO_SCRIPTS_CONFIGURED,
            model.getValueAt(0, PackTableModel.STATUS_COLUMN)
        )
        Assertions.assertFalse(model.startableAt(0))
    }

    /** The pane acts on the pack behind a row, so the mapping must survive a rows assignment. */
    @Test
    fun rowsMapBackToTheirPacks() {
        val model = PackTableModel().apply {
            rows = listOf(
                row(pack("first")),
                row(pack("second"))
            )
        }

        Assertions.assertEquals("second", model.packAt(1).name)
        Assertions.assertEquals(2, model.rowCount)
    }
}
