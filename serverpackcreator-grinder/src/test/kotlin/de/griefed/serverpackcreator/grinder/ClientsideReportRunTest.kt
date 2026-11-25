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
package de.griefed.serverpackcreator.grinder

import de.griefed.serverpackcreator.clientside.ClientsideReportRenderer
import de.griefed.serverpackcreator.clientside.verdict.BootResult
import de.griefed.serverpackcreator.clientside.boot.BootVerifier
import de.griefed.serverpackcreator.clientside.boot.ImageJavaRuntimes
import de.griefed.serverpackcreator.clientside.verdict.Verdict
import de.griefed.serverpackcreator.grinder.container.ContainerEngine
import de.griefed.serverpackcreator.grinder.container.ContainerRunOutput
import de.griefed.serverpackcreator.grinder.container.ContainerSpec
import de.griefed.serverpackcreator.grinder.container.RuntimeImagePreflight
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Duration

/**
 * Pins `--clientside-report`: the report the GitHub clientside workflow posts, produced by booting in the grinder's
 * container. The verification itself is faked; what is asserted is what the workflow and the accept-workflow read —
 * the marker, the hidden data block last, each boot's console — and that every failure still leaves a comment to
 * post while exiting non-zero.
 */
internal class ClientsideReportRunTest {

    private val url = "https://modrinth.com/mod/examplemod"

    /** Answers the image question; a boot reaching it means the run bypassed its verifier. */
    private class ImageEngine(private val present: Boolean) : ContainerEngine {
        override fun run(spec: ContainerSpec, readyPattern: Regex, timeout: Duration, onLine: (String) -> Unit): ContainerRunOutput =
            Assertions.fail("the report run boots only through its verifier")

        override fun hasImage(image: String): Boolean = present
    }

    private fun runWith(
        output: File,
        imagePresent: Boolean = true,
        projectUrl: String = url,
        verify: (GrindCandidate) -> ConsoledReport
    ): Int = ClientsideReportRun(ImageEngine(imagePresent), "spc-grinder-runtime:latest") { verify }.run(projectUrl, output)

    @Test
    fun aVerifiedProjectWritesTheReportTheWorkflowPostsAndExitsZero(@TempDir work: File) {
        val output = File(work, "out/clientside-report.md")
        var asked: GrindCandidate? = null
        val target = targetVerdict("Fabric", "examplemod-", verdict = Verdict.CLEAR, minecraftLine = "1.21", minecraftVersion = "1.21.1")
            .copy(bootResult = BootResult.SURVIVED)

        val exit = runWith(output) { candidate ->
            asked = candidate
            ConsoledReport(
                clientsideReport("examplemod", listOf(target)),
                mapOf(("Fabric" to "1.21") to listOf("[Server thread/INFO]: Done (4.2s)! For help, type \"help\""))
            )
        }

        Assertions.assertEquals(0, exit)
        Assertions.assertEquals(url, asked?.projectUrl)
        Assertions.assertEquals("examplemod", asked?.slug)
        val markdown = output.readText()
        Assertions.assertTrue(markdown.startsWith(ClientsideReportRenderer.MARKER), "the sticky-comment marker comes first")
        Assertions.assertTrue("Done (4.2s)!" in markdown, "each boot's console is part of the report")
        Assertions.assertTrue(ClientsideReportRun.EVIDENCE_NOTE in markdown, "and says where the rest of the evidence is")
        Assertions.assertTrue(
            "**All logs**" in ClientsideReportRun.EVIDENCE_NOTE,
            "an issue reader is pointed at the evidence block's archive link, not at a grinder setting"
        )
        val dataBlock = markdown.lastIndexOf("<!-- clientside-report-data")
        Assertions.assertTrue(dataBlock > 0, "the accept-workflow reads the hidden data block")
        Assertions.assertTrue(
            markdown.substring(dataBlock).trimEnd().endsWith("-->"),
            "the data block is the last thing in the report"
        )
    }

    @Test
    fun aMissingImageWritesThePreflightsRefusalAndExitsNonZeroWithoutVerifying(@TempDir work: File) {
        val output = File(work, "clientside-report.md")

        val exit = runWith(output, imagePresent = false) { Assertions.fail("nothing may be verified without the image") }

        Assertions.assertEquals(1, exit)
        val markdown = output.readText()
        Assertions.assertTrue(markdown.startsWith(ClientsideReportRenderer.MARKER))
        val refusal = RuntimeImagePreflight.refusalFor(ImageEngine(present = false), "spc-grinder-runtime:latest")
        Assertions.assertTrue(refusal != null && refusal in markdown, "the comment carries the preflight's own words: $markdown")
    }

    @Test
    fun theVerifierIsNotBuiltWhenTheImageIsMissing(@TempDir work: File) {
        var built = false
        val exit = ClientsideReportRun(ImageEngine(present = false), "spc-grinder-runtime:latest") {
            built = true
            { Assertions.fail("not reached") }
        }.run(url, File(work, "clientside-report.md"))

        Assertions.assertEquals(1, exit)
        Assertions.assertFalse(built, "building the verifier boots an ApiWrapper, which a refused run must not pay for")
    }

    @Test
    fun aLineTheImageCannotBootIsAnErrorRowNamingTheMissingJava(@TempDir work: File) {
        val output = File(work, "clientside-report.md")
        val imageJava = ImageJavaRuntimes(requiredJavaMajor = { 26 }, bundledMajors = setOf(21))
        val refusal = runCatching { ContainerCandidateVerifier.bundledJavaPath(imageJava, "26.3") }.exceptionOrNull()?.message
        val unbootable = targetVerdict("Fabric", "examplemod-", verdict = Verdict.ERROR, note = refusal,
            minecraftLine = "26.3", minecraftVersion = "26.3")

        val exit = runWith(output) { ConsoledReport(clientsideReport("examplemod", listOf(unbootable)), emptyMap()) }

        Assertions.assertEquals(0, exit, "a report with an ERROR row is still a produced report")
        val markdown = output.readText()
        Assertions.assertTrue("ERROR" in markdown)
        Assertions.assertTrue(
            "Minecraft 26.3 requires Java 26" in markdown,
            "the reader must see which Java the line needed: $markdown"
        )
    }

    @Test
    fun aThrownVerificationStillLeavesACommentAndExitsNonZero(@TempDir work: File) {
        val output = File(work, "clientside-report.md")

        val exit = runWith(output) { throw IllegalStateException("Modrinth answered 503") }

        Assertions.assertEquals(1, exit)
        Assertions.assertEquals(ClientsideReportRenderer.renderFailure(url, "Modrinth answered 503"), output.readText())
    }

    @Test
    fun aLinkNoPlatformRecognisesIsRefusedBeforeAnythingRuns(@TempDir work: File) {
        val output = File(work, "clientside-report.md")

        val exit = runWith(output, projectUrl = "https://example.com/not-a-mod") { Assertions.fail("not reached") }

        Assertions.assertEquals(1, exit)
        Assertions.assertTrue(output.readText().startsWith(ClientsideReportRenderer.MARKER))
    }

    @Test
    fun theCommandLineIsExactlyAFlagALinkAndAnOutputFile() {
        Assertions.assertEquals(
            url to File("report.md"),
            ClientsideReportRun.parse(arrayOf("--clientside-report", url, "--output", "report.md"))
        )
        Assertions.assertNull(ClientsideReportRun.parse(arrayOf("--clientside-report", url)), "the output file is required")
        Assertions.assertNull(ClientsideReportRun.parse(arrayOf("--clientside-report", url, "-o", "report.md")))
        Assertions.assertNull(ClientsideReportRun.parse(arrayOf("--clientside-report", url, "--output", "report.md", url)))
        Assertions.assertNull(ClientsideReportRun.parse(arrayOf(url, "--output", "report.md")))
    }

    @Test
    fun theDecisiveConsolesAreReadFromStagingBeforeTheReap(@TempDir boot: File) {
        val report = clientsideReport(
            "examplemod",
            listOf(
                targetVerdict("NeoForge", "examplemod-", minecraftLine = "1.21", minecraftVersion = "1.21.1"),
                targetVerdict("Forge", "examplemod-", minecraftLine = "1.20", minecraftVersion = "1.20.1")
            )
        )
        BootVerifier.decisiveConsole(boot, "Modrinth", "examplemod", "NeoForge", "1.21")
            .apply { parentFile.mkdirs() }
            .writeText("first\nNoClassDefFoundError: net/minecraft/client/Minecraft\n")

        val consoles = ContainerCandidateVerifier.decisiveConsoles(boot, report)

        Assertions.assertEquals(
            mapOf(("NeoForge" to "1.21") to listOf("first", "NoClassDefFoundError: net/minecraft/client/Minecraft")),
            consoles,
            "a target with a console on disk is read; one without is absent rather than empty"
        )
    }
}
