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
package de.griefed.serverpackcreator.api.versionmeta

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.JsonUtilities
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.api.utilities.common.XmlUtilities
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pins that a manifest which cannot be parsed costs a repair, not the process.
 *
 * Reported from the grinder running as a systemd service: `forge-manifest.json` was truncated mid-write, and
 * from then on every start died with `JsonEOFException` out of `ForgeLoader.update`, through
 * `ApiWrapper.setup()`, straight out of `main`. Nothing in the run could fix it — the jar only seeds a
 * manifest that is **absent** — so the service sat in a restart loop until the file was deleted by hand.
 */
internal class VersionMetaManifestRepairTest {

    /** The mapper `ApiWrapper` builds — the metas deserialise upstream documents carrying fields they do not model. */
    private fun objectMapper() = ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())

    /** The manifests the jar ships, copied into [directory] exactly as `ApiWrapper.setup()` seeds them. */
    private fun seedManifests(directory: File): Map<String, File> =
        listOf(
            "minecraft-manifest.json", "forge-manifest.json", "neoforge-manifest.xml",
            "neoforge-manifest-new.xml", "fabric-manifest.xml", "fabric-installer-manifest.xml",
            "fabric-intermediaries-manifest.json", "quilt-manifest.xml", "quilt-installer-manifest.xml",
            "legacy-fabric-game-manifest.json", "legacy-fabric-loader-manifest.json",
            "legacy-fabric-installer-manifest.xml"
        ).associateWith { name ->
            val destination = File(directory, name)
            javaClass.getResourceAsStream("/de/griefed/resources/manifests/$name")!!.use { resource ->
                destination.outputStream().use { resource.transferTo(it) }
            }
            destination
        }

    /** A [VersionMeta] over the manifests in [directory], wired the way [ApiWrapper] wires the real one. */
    private fun versionMetaOver(manifests: Map<String, File>, cacheDirectory: File): VersionMeta {
        val apiProperties = mockk<ApiProperties>(relaxed = true)
        every { apiProperties.installerCacheDirectory } returns cacheDirectory
        every { apiProperties.minecraftServerManifestsDirectory } returns cacheDirectory
        every { apiProperties.networkConnectTimeout } returns 2_000
        every { apiProperties.networkReadTimeout } returns 2_000
        every { apiProperties.networkDownloadReadTimeout } returns 2_000
        return VersionMeta(
            manifests.getValue("minecraft-manifest.json"),
            manifests.getValue("forge-manifest.json"),
            manifests.getValue("neoforge-manifest.xml"),
            manifests.getValue("neoforge-manifest-new.xml"),
            manifests.getValue("fabric-manifest.xml"),
            manifests.getValue("fabric-installer-manifest.xml"),
            manifests.getValue("fabric-intermediaries-manifest.json"),
            manifests.getValue("quilt-manifest.xml"),
            manifests.getValue("quilt-installer-manifest.xml"),
            manifests.getValue("legacy-fabric-game-manifest.json"),
            manifests.getValue("legacy-fabric-loader-manifest.json"),
            manifests.getValue("legacy-fabric-installer-manifest.xml"),
            objectMapper(),
            Utilities(
                WebUtilities(apiProperties),
                JsonUtilities(objectMapper()),
                XmlUtilities(DocumentBuilderFactory.newInstance())
            ),
            apiProperties
        )
    }

    /**
     * Pins the reported crash: a truncated Forge manifest must be repaired from the jar and the versions it
     * carries must be usable, rather than taking the whole process down.
     */
    @Test
    fun aTruncatedForgeManifestIsRepairedInsteadOfKillingStartup(@TempDir tempDir: File) {
        val manifests = seedManifests(File(tempDir, "manifests").apply { mkdirs() })
        val forgeManifest = manifests.getValue("forge-manifest.json")
        forgeManifest.writeText(forgeManifest.readText().take(90_000))

        val versionMeta = Assertions.assertDoesNotThrow<VersionMeta>({
            versionMetaOver(manifests, File(tempDir, "cache").apply { mkdirs() })
        }, "A manifest that cannot be parsed must be repaired, not thrown out of the constructor")

        Assertions.assertTrue(
            versionMeta.forge.supportedMinecraftVersions().isNotEmpty(),
            "The repaired manifest must have been re-read, not merely rewritten"
        )
    }

    /**
     * Pins the same for the XML half. Forge is what the host reported, but every loader manifest is written
     * by the same refresh and is therefore truncatable the same way.
     */
    @Test
    fun aTruncatedNeoForgeManifestIsRepairedInsteadOfKillingStartup(@TempDir tempDir: File) {
        val manifests = seedManifests(File(tempDir, "manifests").apply { mkdirs() })
        val neoForgeManifest = manifests.getValue("neoforge-manifest-new.xml")
        neoForgeManifest.writeText(neoForgeManifest.readText().take(40_000))

        val versionMeta = Assertions.assertDoesNotThrow<VersionMeta>({
            versionMetaOver(manifests, File(tempDir, "cache").apply { mkdirs() })
        }, "A truncated maven-metadata must be repaired, not thrown out of the constructor")

        Assertions.assertTrue(
            versionMeta.neoForge.supportedMinecraftVersions().isNotEmpty(),
            "The repaired manifest must have been re-read, not merely rewritten"
        )
    }
}
