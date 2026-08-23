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

import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Properties

/**
 * Pins the document served at `/as-properties`, which a ServerPackCreator instance polls through its
 * `de.griefed.serverpackcreator.configuration.fallback.updateurl`.
 *
 * The pins **parse** the rendered text with `java.util.Properties` rather than asserting on its shape:
 * the consumer is `UpdateConfig.updateFallback`, which does exactly that, so a document that merely
 * *looks* right is worth nothing. Note `Properties.load(InputStream)` decodes ISO-8859-1 — anything
 * beyond ASCII has to be `\uXXXX`-escaped or it arrives mangled at the far end.
 */
internal class FallbackPropertiesRendererTest {

    private val fallbackKey = "de.griefed.serverpackcreator.configuration.fallbackmodslist"
    private val whitelistKey = "de.griefed.serverpackcreator.configuration.modswhitelist"

    /** Parse the rendered document the way an SPC instance will, and split one key back into entries. */
    private fun entriesOf(document: String, key: String): List<String> {
        val parsed = Properties()
        parsed.load(document.byteInputStream(Charsets.ISO_8859_1))
        return parsed.getProperty(key).orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    @Test
    fun addsHighConfidenceFindingsToTheRepositoryList() {
        val document = FallbackPropertiesRenderer.render(
            clientsideMods = listOf("jei-", "journeymap-"),
            whitelist = emptyList(),
            verdicts = listOf(
                grindVerdict("entityculling", "Fabric", Confidence.HIGH, suggestedEntry = "entityculling-"),
                grindVerdict("skinlayers3d", "Forge", Confidence.HIGH, suggestedEntry = "skinlayers3d-")
            )
        )

        val entries = entriesOf(document, fallbackKey)
        Assertions.assertTrue(entries.containsAll(listOf("jei-", "journeymap-")), "the repository list must survive: $entries")
        Assertions.assertTrue(entries.containsAll(listOf("entityculling-", "skinlayers3d-")), "HIGH findings must be added: $entries")
    }

    @Test
    fun publishesOnlyHighConfidence() {
        val document = FallbackPropertiesRenderer.render(
            clientsideMods = listOf("jei-"),
            whitelist = emptyList(),
            verdicts = listOf(
                grindVerdict("maybe", "Fabric", Confidence.MEDIUM, suggestedEntry = "maybe-"),
                grindVerdict("unlikely", "Fabric", Confidence.LOW, suggestedEntry = "unlikely-"),
                grindVerdict("unknown", "Fabric", Confidence.INCONCLUSIVE, suggestedEntry = "unknown-"),
                grindVerdict("nothingsuggested", "Fabric", Confidence.HIGH, suggestedEntry = null)
            )
        )

        val entries = entriesOf(document, fallbackKey)
        Assertions.assertEquals(listOf("jei-"), entries, "only a crash-proven mod may be published, was: $entries")
    }

    @Test
    fun neverRepeatsAnEntryTheRepositoryListAlreadyCarries() {
        val document = FallbackPropertiesRenderer.render(
            clientsideMods = listOf("entityculling-"),
            whitelist = emptyList(),
            // The same mod, ground on three loaders: one entry, not three.
            verdicts = listOf("Fabric", "Forge", "NeoForge").map {
                grindVerdict("entityculling", it, Confidence.HIGH, suggestedEntry = "entityculling-")
            }
        )

        Assertions.assertEquals(listOf("entityculling-"), entriesOf(document, fallbackKey))
    }

    @Test
    fun passesTheWhitelistThroughSoTheEndpointReplacesTheRepositoryUrlWholesale() {
        val document = FallbackPropertiesRenderer.render(
            clientsideMods = listOf("jei-"),
            whitelist = listOf("Ping-Wheel-", "some-mod-"),
            verdicts = emptyList()
        )

        Assertions.assertEquals(listOf("Ping-Wheel-", "some-mod-"), entriesOf(document, whitelistKey))
    }

    @Test
    fun survivesEntriesThatWouldOtherwiseBreakThePropertiesFormat() {
        val document = FallbackPropertiesRenderer.render(
            clientsideMods = listOf("[1.8.9] Lunar Block Overlay v1", "Über-Mod-", """back\slash-"""),
            whitelist = emptyList(),
            verdicts = emptyList()
        )

        val entries = entriesOf(document, fallbackKey)
        Assertions.assertTrue(entries.contains("[1.8.9] Lunar Block Overlay v1"), "internal spaces must survive: $entries")
        Assertions.assertTrue(entries.contains("Über-Mod-"), "non-ASCII must round-trip as ISO-8859-1: $entries")
        Assertions.assertTrue(entries.contains("""back\slash-"""), "a backslash must not eat the next character: $entries")
    }

    @Test
    fun isStableAcrossRendersSoPollingDoesNotChurn() {
        val verdicts = listOf(grindVerdict("zed", "Fabric", Confidence.HIGH, suggestedEntry = "zed-"))
        val first = FallbackPropertiesRenderer.render(listOf("beta-", "alpha-"), emptyList(), verdicts)
        val second = FallbackPropertiesRenderer.render(listOf("alpha-", "beta-"), emptyList(), verdicts)

        Assertions.assertEquals(first, second, "input order must not change the document, or every poll looks like a change")
        Assertions.assertEquals(listOf("alpha-", "beta-", "zed-"), entriesOf(first, fallbackKey))
    }

    @Test
    fun rendersAUsableDocumentWithNothingToPublish() {
        val document = FallbackPropertiesRenderer.render(emptyList(), emptyList(), emptyList())

        val parsed = Properties()
        parsed.load(document.byteInputStream(Charsets.ISO_8859_1))
        Assertions.assertNotNull(parsed.getProperty(fallbackKey), "the key must exist even when empty, or clients see no update")
    }
}
