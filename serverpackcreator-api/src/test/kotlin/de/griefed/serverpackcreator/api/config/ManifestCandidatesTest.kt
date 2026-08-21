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
package de.griefed.serverpackcreator.api.config

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Guards [ModpackManifestParser.manifestCandidates] — the list of files that define a modpack.
 *
 * It exists so a caller can ask whether a modpack's manifests changed without duplicating the paths;
 * the GUI's config-editor does exactly that, to avoid re-parsing a multi-megabyte
 * `minecraftinstance.json` on every keystroke-pause. A list that drifts from the one
 * [ModpackManifestParser.checkManifests] actually consults would make that memo miss real edits, which
 * is the failure this pins against. Same rule as `FacadeConstantDelegationTest`: a second copy of a
 * value is a bug waiting to happen.
 */
internal class ManifestCandidatesTest {

    private val parser = ModpackManifestParser(mockk<ApiProperties>(relaxed = true), mockk<Utilities>(relaxed = true))

    /**
     * Pins the exact set and order of candidates. Order is meaningful: `checkManifests` takes the first
     * that exists, so moving an entry changes which launcher wins for a pack carrying two manifests.
     */
    @Test
    fun everyLauncherManifestIsACandidateInConsultOrder() {
        val modpack = File("/packs/MyPack").absoluteFile
        val expected = listOf(
            File(modpack, "minecraftinstance.json"),
            File(modpack, "manifest.json"),
            File(modpack, "instance.json"),
            File(modpack.parentFile, "instance.json"),
            File(modpack.parentFile, "mmc-pack.json"),
            File(modpack.parentFile, "instance.cfg")
        )
        Assertions.assertEquals(expected, parser.manifestCandidates(modpack.absolutePath))
    }

    /**
     * Pins that the candidates are reported whether or not they exist. A memo keyed on their state has
     * to know about a manifest that is about to be *created*, so filtering to existing files here would
     * make an appearing manifest invisible.
     */
    @Test
    fun candidatesAreReportedEvenWhenAbsent() {
        val candidates = parser.manifestCandidates(File("/nonexistent/pack").absolutePath)
        Assertions.assertEquals(6, candidates.size)
        Assertions.assertTrue(candidates.none { it.exists() }, "precondition: none of these exist")
    }

    /**
     * Pins that the published facade reads the parser rather than holding its own copy of the paths.
     * Compared by value because both sides construct fresh `File` objects; the drift this catches is a
     * re-introduced literal list, which would differ in content.
     */
    @Test
    fun theConfigurationHandlerFacadeReportsTheSameCandidates() {
        val modpack = File("/packs/MyPack").absoluteFile
        val handler = ConfigurationHandler(
            mockk(relaxed = true),
            mockk<ApiProperties>(relaxed = true),
            mockk<Utilities>(relaxed = true),
            mockk(relaxed = true)
        )
        Assertions.assertEquals(
            parser.manifestCandidates(modpack.absolutePath),
            handler.manifestCandidates(modpack.absolutePath)
        )
    }
}
