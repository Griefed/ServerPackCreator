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
package de.griefed.serverpackcreator.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins that the shipped manifest snapshot covers the Minecraft releases its own parent manifest advertises.
 *
 * SPC ships `manifests/` as resources and `ApiWrapper.setup()` seeds them into the home, which is what makes the
 * suite — and a first run on a fresh machine — work offline. That guarantee only holds per version: asking for one
 * whose `mcserver/<id>.json` is absent means a download, and if that download fails the answer is an empty
 * `Optional`, which consumers read as "this version declares no required Java". Downstream that became a
 * benign-looking `[N/A] SKIPPED` and quietly dropped the newest Minecraft versions from the template matrix.
 *
 * The set was internally inconsistent until 2026-07-31: `minecraft-manifest.json` listed **26.2** as the newest
 * release while `mcserver/` had neither it nor any 1.21.x — 16 releases advertised and not cached. This guard is
 * what stops that drifting back, because the symptom is invisible on a machine whose home already has the files.
 *
 * **Releases only, deliberately.** Snapshots are advertised in the same manifest and hundreds are absent; caching
 * them all would multiply the shipped resources for versions the grinder's release gate never selects. If snapshot
 * support ever needs the same guarantee, that is a separate, much larger decision.
 */
internal class ShippedManifestSnapshotTest {

    /** Module directory, from the home the build injects — not the working directory (see `TestPropertiesTest`). */
    private val moduleDirectory: File = System.getProperty("de.griefed.serverpackcreator.home")
        ?.let { File(it).parentFile }
        ?: File("").absoluteFile

    /** The resources as they are committed, which is what actually ships. */
    private val shippedManifests = File(moduleDirectory, "src/main/resources/de/griefed/resources/manifests")

    @Test
    fun everyAdvertisedReleaseHasAShippedServerManifest() {
        val parentManifest = File(shippedManifests, "minecraft-manifest.json")
        Assertions.assertTrue(parentManifest.isFile, "expected the shipped parent manifest at $parentManifest")

        val cached = File(shippedManifests, "mcserver").list()?.toSet() ?: emptySet()
        Assertions.assertTrue(cached.isNotEmpty(), "expected shipped per-version manifests beside $parentManifest")

        val versions = ObjectMapper().readTree(parentManifest).get("versions")
        val missing = versions
            .filter { it.get("type")?.asText() == "release" }
            .map { it.get("id").asText() }
            .filterNot { "$it.json" in cached }

        Assertions.assertTrue(
            missing.isEmpty(),
            "the shipped manifest advertises ${missing.size} release(s) whose per-version manifest is not shipped, " +
                "so a fresh machine must fetch them and answers \"required Java unknown\" when it cannot: " +
                "${missing.take(20)}. Refresh with the `updateManifests` task."
        )
    }
}
