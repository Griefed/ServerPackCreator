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
package de.griefed.serverpackcreator.clientside

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.griefed.serverpackcreator.clientside.platform.DeclaredSupport
import de.griefed.serverpackcreator.clientside.verdict.BootDecision
import de.griefed.serverpackcreator.clientside.verdict.BootResult
import de.griefed.serverpackcreator.clientside.verdict.ClientsideReport
import de.griefed.serverpackcreator.clientside.verdict.GrindTargetVerdict
import de.griefed.serverpackcreator.clientside.verdict.JarScan
import de.griefed.serverpackcreator.clientside.verdict.Verdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the issue comment [ClientsideReportRenderer] writes, which three consumers read without any
 * compiler between them: the reusable workflow finds the comment by [ClientsideReportRenderer.MARKER]
 * to update it in place, `clientside-accept.yml` cuts the hidden data block out with a regex, and
 * `-clientsideapply` reads `suggestedEntries` and `projectUrl` from that block.
 */
internal class ClientsideReportRendererTest {

    private val excerpt = "java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft"

    private val crashedOnNeoForge = GrindTargetVerdict(
        loader = "NeoForge",
        suggestedEntry = "examplemod-",
        declaredClientSide = DeclaredSupport.REQUIRED,
        declaredServerSide = DeclaredSupport.UNSUPPORTED,
        jarScan = JarScan.CLIENT,
        bootResult = BootResult.CRASHED,
        bootedLoader = "NeoForge",
        bootCrashExcerpt = excerpt,
        sampleFile = "examplemod-neoforge-2.0.0+1.21.1.jar",
        note = "Reached client-only code on a dedicated server.",
        decidedBy = BootDecision.CLIENT_ONLY_CLASS,
        verdict = Verdict.CONFIRMED,
        minecraftLine = "1.21",
        minecraftVersion = "1.21.1"
    )

    private val survivedViaAnotherLoader = GrindTargetVerdict(
        loader = "Fabric",
        suggestedEntry = "examplemod-",
        declaredClientSide = DeclaredSupport.REQUIRED,
        declaredServerSide = DeclaredSupport.UNSUPPORTED,
        jarScan = JarScan.CLIENT,
        bootResult = BootResult.SURVIVED,
        bootedLoader = "NeoForge",
        bootCrashExcerpt = null,
        sampleFile = "examplemod-fabric-1.0.0+1.20.1.jar",
        note = "Reached client-only code on a dedicated server.",
        decidedBy = BootDecision.READY_LINE,
        verdict = Verdict.CLEAR,
        minecraftLine = "1.20",
        minecraftVersion = "1.20.1"
    )

    private val neverBooted = GrindTargetVerdict(
        loader = "Fabric",
        suggestedEntry = null,
        declaredClientSide = DeclaredSupport.UNKNOWN,
        declaredServerSide = DeclaredSupport.UNKNOWN,
        jarScan = JarScan.DEFERRED,
        bootResult = null,
        bootedLoader = null,
        bootCrashExcerpt = null,
        sampleFile = null,
        note = null,
        verdict = Verdict.LOCKED,
        minecraftLine = "1.19",
        minecraftVersion = "1.19.2"
    )

    private val report = ClientsideReport(
        platform = "Modrinth",
        slug = "examplemod",
        projectUrl = "https://modrinth.com/mod/examplemod",
        phase = "metadata + server-boot",
        suggestedEntries = listOf("examplemod-"),
        perTarget = listOf(crashedOnNeoForge, survivedViaAnotherLoader, neverBooted),
        fileNames = listOf("examplemod-neoforge-2.0.0+1.21.1.jar", "examplemod-fabric-1.0.0+1.20.1.jar")
    )

    private val markdown = ClientsideReportRenderer.renderMarkdown(report)
    private val lines = markdown.lines()

    @Test
    fun theMarkerIsTheFirstLineSoTheWorkflowCanFindTheCommentAgain() {
        Assertions.assertEquals(ClientsideReportRenderer.MARKER, lines.first())
    }

    @Test
    fun theHeadingNamesTheProjectAndThePhase() {
        Assertions.assertTrue("## 🤖 Clientside-mod verification — `examplemod` (Modrinth)" in lines, markdown)
        Assertions.assertTrue(lines.any { "Automated assessment (**metadata + server-boot**)" in it }, markdown)
    }

    @Test
    fun suggestedEntriesAreListedInACodeBlock() {
        val heading = lines.indexOf("### Suggested clientside-list entries")
        Assertions.assertEquals(listOf("```", "examplemod-", "```"), lines.subList(heading + 1, heading + 4))
    }

    @Test
    fun noSuggestedEntryAsksTheMaintainerToAddOne() {
        val rendered = ClientsideReportRenderer.renderMarkdown(report.copy(suggestedEntries = emptyList())).lines()
        val heading = rendered.indexOf("### Suggested clientside-list entries")
        Assertions.assertEquals(
            "_None could be derived — see the file-names below and add an entry manually._",
            rendered[heading + 1]
        )
    }

    @Test
    fun everyTargetIsOneTableRow() {
        Assertions.assertTrue("### Confidence per Minecraft version" in lines, markdown)
        Assertions.assertTrue(
            "| Minecraft | Loader | Suggested entry | Declared (client/server) | Jar scan | Boot | Verdict |" in lines,
            markdown
        )
        Assertions.assertTrue(
            "| 1.21.1 | NeoForge | `examplemod-` | REQUIRED / UNSUPPORTED | CLIENT | CRASHED | 🟢 **CONFIRMED** |" in lines,
            markdown
        )
        Assertions.assertTrue("| 1.19.2 | Fabric | — | UNKNOWN / UNKNOWN | DEFERRED | — | 🔒 LOCKED |" in lines, markdown)
    }

    @Test
    fun aBootRunUnderAnotherLoaderSaysWhichOne() {
        Assertions.assertTrue(
            "| 1.20.1 | Fabric | `examplemod-` | REQUIRED / UNSUPPORTED | CLIENT | SURVIVED (via NeoForge) | 🔵 CLEAR |" in lines,
            markdown
        )
    }

    /**
     * Two rows of one project routinely share a loader and differ by Minecraft era, so a row that does not
     * name its version is indistinguishable from its neighbour — issue #1338 rendered two identical Fabric rows.
     */
    @Test
    fun rowsSharingALoaderAreToldApartByTheirMinecraftVersion() {
        val fabricRows = ClientsideReportRenderer.renderMarkdown(
            report.copy(perTarget = listOf(survivedViaAnotherLoader, survivedViaAnotherLoader.copy(minecraftVersion = "1.21.1")))
        ).lines().filter { "| Fabric |" in it }
        Assertions.assertEquals(listOf("1.20.1", "1.21.1"), fabricRows.map { it.removePrefix("| ").substringBefore(" |") })
    }

    @Test
    fun aRowWithoutAnExactVersionFallsBackToItsLine() {
        val rows = ClientsideReportRenderer.renderMarkdown(
            report.copy(
                perTarget = listOf(
                    neverBooted.copy(minecraftVersion = null),
                    neverBooted.copy(minecraftVersion = null, minecraftLine = null)
                )
            )
        ).lines().filter { "| Fabric |" in it }
        Assertions.assertEquals(listOf("1.19", "—"), rows.map { it.removePrefix("| ").substringBefore(" |") })
    }

    /**
     * A staging refusal carries `INCONCLUSIVE` as its boot result while no server ever ran; only a real boot
     * has a classifier rung to name ([GrindTargetVerdict.decidedBy]).
     */
    @Test
    fun aTargetRefusedBeforeAnyServerRanSaysItWasNotBooted() {
        val refused = neverBooted.copy(bootResult = BootResult.INCONCLUSIVE, decidedBy = null, verdict = Verdict.UNVERIFIABLE)
        val row = ClientsideReportRenderer.renderMarkdown(report.copy(perTarget = listOf(refused)))
            .lines().single { "| Fabric |" in it }
        Assertions.assertEquals("not booted", row.split(" | ")[5], row)
    }

    @Test
    fun eachVerdictHasItsOwnBadge() {
        val badges = Verdict.entries.associateWith { verdict ->
            ClientsideReportRenderer.renderMarkdown(report.copy(perTarget = listOf(neverBooted.copy(verdict = verdict))))
                .lines().single { "| Fabric |" in it }.split(" | ").last().removeSuffix(" |")
        }
        Assertions.assertEquals(
            mapOf(
                Verdict.CONFIRMED to "🟢 **CONFIRMED**",
                Verdict.CLEAR to "🔵 CLEAR",
                Verdict.ERROR to "🛠 ERROR",
                Verdict.INCONCLUSIVE to "⚪ INCONCLUSIVE",
                Verdict.LOCKED to "🔒 LOCKED",
                Verdict.UNVERIFIABLE to "🚫 UNVERIFIABLE"
            ),
            badges
        )
    }

    @Test
    fun aCrashShowsItsExcerptInACollapsedBlock() {
        val summary = lines.indexOf("<details><summary>💥 NeoForge · Minecraft 1.21.1 server crash output</summary>")
        Assertions.assertTrue(summary >= 0, markdown)
        Assertions.assertEquals(listOf("", "```", excerpt, "```", "</details>"), lines.subList(summary + 1, summary + 6))
        Assertions.assertEquals(1, lines.count { it.startsWith("<details><summary>💥") }, "only the crashed target has one")
    }

    @Test
    fun aNoteSharedByTwoTargetsIsShownOnce() {
        val note = lines.indexOf("> [!NOTE]")
        Assertions.assertEquals("> - Reached client-only code on a dedicated server.", lines[note + 1])
        Assertions.assertFalse(lines[note + 2].startsWith("> -"), markdown)
    }

    @Test
    fun everyPublishedFileNameIsListed() {
        val summary = lines.indexOf("<details><summary>All 2 published file-names</summary>")
        Assertions.assertTrue(summary >= 0, markdown)
        Assertions.assertEquals(
            listOf("", "```", "examplemod-neoforge-2.0.0+1.21.1.jar", "examplemod-fabric-1.0.0+1.20.1.jar", "```"),
            lines.subList(summary + 1, summary + 6)
        )
    }

    private fun console(lines: Int, prefix: String = "line") = (1..lines).map { "[Server thread/INFO]: $prefix $it" }

    @Test
    fun eachBootedTargetShowsTheEndOfItsConsole() {
        val rendered = ClientsideReportRenderer.renderMarkdown(report) { target ->
            if (target === crashedOnNeoForge) console(500) else null
        }
        val lines = rendered.lines()

        val summary = lines.indexOf("<details><summary>📜 NeoForge · Minecraft 1.21.1 — CRASHED · last 200 of 500 console lines</summary>")
        Assertions.assertTrue(summary >= 0, rendered)
        val block = lines.subList(summary, summary + lines.drop(summary).indexOf("</details>"))
        Assertions.assertTrue("[Server thread/INFO]: line 500" in block, "the end of the console is what decided the verdict")
        Assertions.assertTrue("[Server thread/INFO]: line 301" in block)
        Assertions.assertFalse("[Server thread/INFO]: line 300" in block)
        Assertions.assertTrue(lines.indexOf("### 🪵 Boot logs") in 0 until summary, rendered)
    }

    @Test
    fun aTargetWithoutAConsoleSaysSo() {
        val rendered = ClientsideReportRenderer.renderMarkdown(report) { target ->
            if (target === crashedOnNeoForge) console(3) else null
        }
        Assertions.assertTrue("- Fabric · Minecraft 1.19.2 — no server console was recorded" in rendered.lines(), rendered)
    }

    @Test
    fun aReportWithoutABootHasNoBootLogs() {
        val metadataOnly = report.copy(perTarget = listOf(neverBooted))
        Assertions.assertFalse("### 🪵 Boot logs" in ClientsideReportRenderer.renderMarkdown(metadataOnly) { console(3) })
    }

    @Test
    fun aConsoleCannotCloseItsOwnCodeBlock() {
        val rendered = ClientsideReportRenderer.renderMarkdown(report) { target ->
            if (target === crashedOnNeoForge) listOf("before", "````", "after") else null
        }
        val lines = rendered.lines()
        val opener = lines.first { it.endsWith("text") && it.startsWith("`") }
        val fence = opener.removeSuffix("text")
        Assertions.assertTrue(fence.length > 4, "a fence of $fence closes on the console's own ````")
        Assertions.assertEquals(listOf("before", "````", "after", fence), lines.subList(lines.indexOf(opener) + 1, lines.indexOf(opener) + 5))
    }

    @Test
    fun terminalColourCodesAndRunawayLinesAreTamed() {
        val rendered = ClientsideReportRenderer.renderMarkdown(report) { target ->
            if (target === crashedOnNeoForge) listOf("\u001B[32m[Server thread/INFO]: Done\u001B[0m\r", "x".repeat(5_000)) else null
        }
        Assertions.assertTrue("[Server thread/INFO]: Done" in rendered.lines(), rendered)
        Assertions.assertFalse('\u001B' in rendered)
        Assertions.assertTrue(rendered.lines().none { it.length > ClientsideReportRenderer.MAX_CONSOLE_LINE_CHARS + 1 })
    }

    /**
     * GitHub refuses a comment over 65,536 characters, and a refused report is no report at all — including the
     * data block the `accepted` label needs. However much the boots printed, the comment must fit.
     */
    @Test
    fun hugeConsolesStillFitIntoOneComment() {
        val rendered = ClientsideReportRenderer.renderMarkdown(report) { console(20_000, "x".repeat(150)) }

        Assertions.assertTrue(rendered.length <= ClientsideReportRenderer.MAX_COMMENT_CHARS, "length ${rendered.length}")
        Assertions.assertEquals(3, rendered.lines().count { it.startsWith("<details><summary>📜") }, "every booted target keeps a share")
        val json = Regex("<!-- clientside-report-data\\n([\\s\\S]*?)\\n-->").find(rendered)?.groupValues?.get(1)
        Assertions.assertEquals(listOf("examplemod-"), jacksonObjectMapper().readTree(json).path("suggestedEntries").map { it.asText() })
    }

    /**
     * A project with a long history can exceed the limit on its own: its file names are listed in the Markdown and
     * again in the data block. They go before anything the acceptance workflow needs.
     */
    @Test
    fun aReportTooLargeOnItsOwnDropsTheFileNamesNotTheEntries() {
        val longHistory = report.copy(fileNames = (1..3_000).map { "examplemod-neoforge-2.0.$it+1.21.1-some-long-suffix.jar" })
        val rendered = ClientsideReportRenderer.renderMarkdown(longHistory)

        Assertions.assertTrue(rendered.length <= ClientsideReportRenderer.MAX_COMMENT_CHARS, "length ${rendered.length}")
        Assertions.assertTrue(rendered.lines().any { it.startsWith("_The 3000 published file-names are left out") }, rendered.take(3_000))
        val json = Regex("<!-- clientside-report-data\\n([\\s\\S]*?)\\n-->").find(rendered)?.groupValues?.get(1)
        val data = jacksonObjectMapper().readTree(json)
        Assertions.assertEquals(listOf("examplemod-"), data.path("suggestedEntries").map { it.asText() })
        Assertions.assertEquals("https://modrinth.com/mod/examplemod", data.path("projectUrl").asText())
    }

    @Test
    fun theDataBlockIsLastAndReadsBackTheWayTheAcceptanceWorkflowReadsIt() {
        // The same expression `clientside-accept.yml` applies to the comment body.
        val json = Regex("<!-- clientside-report-data\\n([\\s\\S]*?)\\n-->").find(markdown)?.groupValues?.get(1)
        Assertions.assertNotNull(json, markdown)
        val data = jacksonObjectMapper().readTree(json)
        Assertions.assertEquals(listOf("examplemod-"), data.path("suggestedEntries").map { it.asText() })
        Assertions.assertEquals("https://modrinth.com/mod/examplemod", data.path("projectUrl").asText())
        Assertions.assertTrue(markdown.trimEnd().endsWith("-->"), "nothing may follow the data block")
    }
}
