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
package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.api.PropertyStore
import de.griefed.serverpackcreator.api.settings.GenerationConfig
import de.griefed.serverpackcreator.api.settings.UpdateConfig
import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * Drives the **real consumer** against the **real endpoint**: SPC's own `UpdateConfig.updateFallback` pointed at
 * a running [ReportServer] over loopback.
 *
 * Everything else about `/as-properties` is pinned against `java.util.Properties`, which is a model of the
 * consumer rather than the consumer. This is the one that answers what only a runtime can: that
 * `UpdateConfig` accepts the document, splits it into the entries intended, and installs them into
 * `GenerationConfig.clientsideMods` — the list generation actually excludes mods with. It needs no Docker and
 * no internet: `PropertyStore` is no-arg constructible and the server binds an ephemeral loopback port.
 */
internal class FallbackPropertiesConsumerTest {

    /** A real UpdateConfig over an empty store, with persistence stubbed out. */
    private class Consumer {
        val store = PropertyStore()
        val generation = GenerationConfig(store)
        var saved = false
        val update = UpdateConfig(store, generation) { saved = true }
    }

    @Test
    fun spcsOwnUpdaterAcceptsTheDocumentAndInstallsTheEntries() {
        val verdicts = InMemoryVerdictStore().apply {
            record(grindVerdict("entityculling", "Fabric", verdict = Verdict.CONFIRMED, suggestedEntry = "entityculling-"))
            record(grindVerdict("notclientside", "Forge", suggestedEntry = "notclientside-"))
        }
        val server = ReportServer(
            verdicts,
            requestedPort = 0,
            fallbackLists = { FallbackLists(clientsideMods = listOf("jei-", "journeymap-"), whitelist = listOf("Ping-Wheel-")) }
        ).start()

        try {
            val consumer = Consumer()
            consumer.update.updateUrl = URI("http://127.0.0.1:${server.port}/as-properties").toURL()

            Assertions.assertTrue(consumer.update.updateFallback(), "the updater must report that it took the lists")

            Assertions.assertTrue(
                consumer.generation.clientsideMods.containsAll(listOf("jei-", "journeymap-", "entityculling-")),
                "SPC ended up with ${consumer.generation.clientsideMods}"
            )
            Assertions.assertFalse(
                consumer.generation.clientsideMods.contains("notclientside-"),
                "an unproven finding reached a real SPC instance's exclusion list"
            )
            Assertions.assertTrue(
                consumer.generation.modsWhitelist.contains("Ping-Wheel-"),
                "the whitelist did not survive the round trip: ${consumer.generation.modsWhitelist}"
            )
            Assertions.assertTrue(consumer.saved, "a changed list must be persisted by the consumer")
        } finally {
            server.stop()
        }
    }

    /**
     * The entries must arrive *clean*. The document spreads them over continuation lines with a four-space
     * indent, and `Properties` strips that leading whitespace — but only from continuation lines, so a stray
     * space anywhere else would arrive baked into a `startsWith` matcher and silently stop matching anything.
     */
    @Test
    fun entriesArriveWithoutTheLayoutTheyWereRenderedIn() {
        val server = ReportServer(
            InMemoryVerdictStore(),
            requestedPort = 0,
            fallbackLists = { FallbackLists(listOf("jei-", "[1.8.9] Lunar Block Overlay v1"), emptyList()) }
        ).start()

        try {
            val consumer = Consumer()
            consumer.update.updateUrl = URI("http://127.0.0.1:${server.port}/as-properties").toURL()
            consumer.update.updateFallback()

            val entries = consumer.generation.clientsideMods
            Assertions.assertTrue(entries.contains("jei-"), "got $entries")
            Assertions.assertTrue(entries.contains("[1.8.9] Lunar Block Overlay v1"), "internal spaces must survive: $entries")
            Assertions.assertTrue(entries.none { it != it.trim() }, "an entry arrived with padding: $entries")
        } finally {
            server.stop()
        }
    }
}
