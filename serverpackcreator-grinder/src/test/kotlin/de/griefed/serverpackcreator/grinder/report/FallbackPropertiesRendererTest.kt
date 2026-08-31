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

import de.griefed.serverpackcreator.clientside.BootDecision
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

    /**
     * A comma cannot survive the round trip: `UpdateConfig.updateFallback` splits the value on it, so one entry
     * containing a comma arrives at every client as *two* bogus `startsWith` matchers against real mod
     * filenames. Filenames may legally contain commas and `FilenameStemDeriver` derives stems straight from
     * them, so this is reachable without anything unusual happening — and silent at both ends.
     */
    @Test
    fun dropsAnEntryTheCommaSeparatedFormatCannotRepresent() {
        val document = FallbackPropertiesRenderer.render(
            clientsideMods = listOf("safe-", "danger,ous-"),
            whitelist = listOf("also,bad-"),
            verdicts = emptyList()
        )

        Assertions.assertEquals(listOf("safe-"), entriesOf(document, fallbackKey))
        Assertions.assertTrue(entriesOf(document, whitelistKey).isEmpty(), "the whitelist must be filtered too")
    }

    @Test
    fun saysSoInTheDocumentWhenItHadToDropSomething() {
        val document = FallbackPropertiesRenderer.render(listOf("safe-", "danger,ous-"), emptyList(), emptyList())

        Assertions.assertTrue(
            document.lineSequence().any { it.startsWith("#") && it.contains("comma") },
            "dropping an entry silently is how a list quietly goes wrong; the document must admit it"
        )
    }

}

/**
 * Pins the gate that decides what actually reaches an SPC instance: **HIGH is necessary but no longer
 * sufficient — the deciding console must have carried decisive client-only evidence.**
 *
 * Why HIGH alone was never enough: `CRASHED` is reachable both from the client-only-class marker, which no
 * environment failure can fabricate, and from the bare exit-code fallback, which means only "the process
 * exited non-zero and nothing recognised why". Sampled against the live grinder on 2026-08-31, four of five
 * published boot logs were the latter, and `created_ltab-` was already in the served list because of it.
 */
internal class FallbackPropertiesPublicationGateTest {

    private fun verdict(slug: String, decidedBy: String?) = grindVerdict(
        slug, "Fabric", confidence = Confidence.HIGH, suggestedEntry = "$slug-"
    ).copy(decidedBy = decidedBy)

    @Test
    fun onlyAVerdictDecidedByDecisiveEvidenceIsPublished() {
        val rendered = FallbackPropertiesRenderer.render(
            clientsideMods = emptyList(),
            whitelist = emptyList(),
            verdicts = listOf(
                verdict("modelfix", BootDecision.CLIENT_ONLY_CLASS.name),
                verdict("ruled", BootDecision.OPERATOR_RULE.name),
                verdict("create_ltab", BootDecision.EXIT_CODE.name),
                verdict("mixinbroken", BootDecision.MIXIN_APPLY_FAILURE.name)
            )
        )

        Assertions.assertTrue(rendered.contains("modelfix-"), "a client-only-class crash is what the list is for")
        Assertions.assertTrue(rendered.contains("ruled-"), "an operator rule stating CRASHED said so deliberately")
        Assertions.assertFalse(
            rendered.contains("create_ltab-"),
            "a bare non-zero exit nobody recognised must never publish — this is the live false positive"
        )
        Assertions.assertFalse(rendered.contains("mixinbroken-"), "a mixin that failed to apply is not sideness evidence")
    }

    /**
     * A verdict recorded before the rung was tracked cannot be shown to be decisive, so it does not publish.
     * That deliberately empties the grinder's contribution until a sweep re-grinds — an empty contribution is
     * better than a wrong one, and the alternative is grandfathering in exactly the entries this gate exists
     * to remove.
     */
    @Test
    fun aLegacyVerdictWithNoRecordedDecisionIsNotPublished() {
        val rendered = FallbackPropertiesRenderer.render(
            clientsideMods = emptyList(), whitelist = emptyList(), verdicts = listOf(verdict("legacy", null))
        )

        Assertions.assertFalse(rendered.contains("legacy-"))
    }

    /** The shipped list is untouched by the gate — it is not the grinder's to withhold. */
    @Test
    fun theShippedListIsPublishedRegardless() {
        val rendered = FallbackPropertiesRenderer.render(
            clientsideMods = listOf("shipped-entry-"),
            whitelist = listOf("whitelisted-"),
            verdicts = listOf(verdict("create_ltab", BootDecision.EXIT_CODE.name))
        )

        Assertions.assertTrue(rendered.contains("shipped-entry-"))
        Assertions.assertTrue(rendered.contains("whitelisted-"))
    }
}
