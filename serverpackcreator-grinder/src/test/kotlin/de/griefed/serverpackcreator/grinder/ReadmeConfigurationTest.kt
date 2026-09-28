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

import de.griefed.serverpackcreator.grinder.container.ContainerResources
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Keeps the README's configuration table in step with [GrinderApplication].
 *
 * The table is the service's operator contract: an admin sets `SPC_GRINDER_*` from it and expects the
 * documented default. Prose cannot be compiled, so this compares the two sources directly — every variable
 * the entry point reads must be documented with its real default, and the README must not invent variables
 * the service ignores. Without this, adding a knob and forgetting the table (or renaming one) is silent.
 *
 * It reads the README as text — documentation drift is the point — but takes the *variables* from
 * [GrinderConfiguration.KNOBS], the same list the service configures itself from. That list replaced a
 * regex over `GrinderApplication`'s source, which only worked while every `env(...)` call stayed inside one
 * `main`, and which would have silently stopped covering anything that moved out of the file it scanned.
 */
internal class ReadmeConfigurationTest {

    private val readme = File("README.md")

    @Test
    fun everyEnvironmentVariableTheServiceReadsIsDocumentedWithItsDefault() {
        Assertions.assertTrue(readme.isFile, "README.md not found at ${readme.absolutePath}")
        val readmeText = readme.readText()

        for (knob in GrinderConfiguration.KNOBS) {
            Assertions.assertTrue(readmeText.contains("`${knob.name}`"), "README does not document ${knob.name}")
            knob.literalDefault?.let { default ->
                Assertions.assertTrue(
                    readmeText.contains("`$default`"),
                    "README does not state ${knob.name}'s real default ($default)"
                )
            }
        }
    }

    /** The one variable that is not `SPC_GRINDER_`-prefixed still has to be in the table. */
    @Test
    fun theCurseForgeKeyIsDocumentedToo() {
        Assertions.assertTrue(readme.readText().contains("`CURSEFORGE_API_KEY`"))
    }

    @Test
    fun theReadmeDoesNotDocumentVariablesTheServiceIgnores() {
        val known = buildSet {
            addAll(GrinderConfiguration.KNOBS.map { it.name })
            // Read by the gated integration tests rather than the service itself.
            addAll(listOf("SPC_GRINDER_IMAGE_TEMPLATES", "SPC_GRINDER_TEMPLATE_MC", "SPC_GRINDER_TEMPLATE_LOADERS",
                "SPC_GRINDER_TEMPLATE_SHELLS", "SPC_GRINDER_TEMPLATE_WORKERS", "SPC_GRINDER_TEMPLATE_TIMEOUT_MINUTES"))
        }

        val documented = Regex("""`(SPC_GRINDER_[A-Z_]+)`""").findAll(readme.readText())
            .map { it.groupValues[1] }
            .toSet()

        val phantom = documented - known
        Assertions.assertTrue(phantom.isEmpty(), "README documents variables nothing reads: $phantom")
    }

    /**
     * Keeps the README's worker-sizing arithmetic in step with the per-boot memory cap it divides by.
     *
     * §5 tells an operator to size `SPC_GRINDER_WORKERS` as roughly *(memory available to Docker − overhead) / 3 GiB*,
     * where 3 GiB is [ContainerResources.memoryBytes]. Change the cap and that advice silently starts
     * over-subscribing a host — and an over-subscribed host OOM-kills boots, which cost their full budget and teach
     * nothing. Prose cannot be compiled, so the figure is compared against the real default instead.
     */
    @Test
    fun theWorkerSizingAdviceQuotesTheRealPerBootMemoryCap() {
        val capGiB = ContainerResources().memoryBytes / (1024L * 1024L * 1024L)
        val text = readme.readText()

        Assertions.assertTrue(
            text.contains("### Sizing the worker count"),
            "the worker-sizing section is gone — SPC_GRINDER_WORKERS is the biggest lever on sweep duration"
        )
        Assertions.assertTrue(
            text.contains("$capGiB GiB"),
            "README's sizing advice must quote the real per-boot cap of $capGiB GiB (ContainerResources.memoryBytes)"
        )
        Assertions.assertTrue(
            text.contains("/ $capGiB GiB"),
            "the sizing formula must divide by the real per-boot cap of $capGiB GiB, or it over-subscribes the host"
        )
    }
}
