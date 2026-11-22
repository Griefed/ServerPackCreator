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
package de.griefed.serverpackcreator.api.serverpack

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.api.versionmeta.fabric.FabricMeta
import de.griefed.serverpackcreator.api.versionmeta.forge.ForgeInstance
import de.griefed.serverpackcreator.api.versionmeta.forge.ForgeMeta
import de.griefed.serverpackcreator.api.versionmeta.legacyfabric.LegacyFabricMeta
import de.griefed.serverpackcreator.api.versionmeta.neoforge.NeoForgeInstance
import de.griefed.serverpackcreator.api.versionmeta.neoforge.NeoForgeMeta
import de.griefed.serverpackcreator.api.versionmeta.quilt.QuiltMeta
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.io.File
import java.util.zip.ZipFile
import java.net.MalformedURLException
import java.net.URI
import java.util.*
import javax.imageio.ImageIO

/**
 * Branch coverage for [ServerPackProvisioner] that complements the existing characterization tests
 * (which cover the default/missing icon, the scaled-custom icon, the default/missing properties and
 * the basic Java/restart placeholder replacement). Added here: copying an existing custom
 * server.properties, the already-64x64 icon direct-copy (no scaling) path, the generic-key and
 * Windows-path-escaping branches of placeholder replacement, and — via MockK-stubbed [VersionMeta]
 * and [WebUtilities] — the per-loader reachability branches of `serverDownloadable` and both
 * launcher-present/absent branches of `getImprovedFabricLauncher`, all driven offline.
 */
internal class ServerPackProvisionerTest {
    private val api = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val apiProperties = api.apiProperties
    private val provisioner = api.serverPackHandler.provisioner

    /**
     * Builds a [ServerPackProvisioner] whose network-bound collaborators are mocked: the given
     * [versionMeta] supplies (stubbed) loader instances/URLs and [webUtilities] decides reachability,
     * so the download/reachability branches run without touching the network.
     */
    private fun mockedProvisioner(versionMeta: VersionMeta, webUtilities: WebUtilities): ServerPackProvisioner {
        val utilities = mockk<Utilities>()
        every { utilities.webUtilities } returns webUtilities
        return ServerPackProvisioner(apiProperties, versionMeta, utilities)
    }

    /**
     * An existing custom server.properties is copied verbatim into the server pack.
     */
    @Test
    fun copyPropertiesCopiesExistingCustomFile(@TempDir tempDir: File) {
        val custom = File(tempDir, "custom.properties")
        custom.writeText("motd=Hello from a custom properties file")
        val destination = File(tempDir, "pack").also { it.mkdirs() }

        provisioner.copyProperties(destination.absolutePath, custom.absolutePath)

        val copied = File(destination, apiProperties.defaultServerProperties.name)
        Assertions.assertTrue(copied.isFile, "Custom properties must be copied")
        Assertions.assertEquals(custom.readText(), copied.readText())
    }

    /**
     * A custom icon that is already 64x64 is copied directly, without going through the scaling
     * path.
     */
    @Test
    fun copyIconCopiesAlready64x64IconWithoutScaling(@TempDir tempDir: File) {
        val icon = File(tempDir, "icon64.png")
        ImageIO.write(BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), "png", icon)
        val destination = File(tempDir, "pack").also { it.mkdirs() }

        provisioner.copyIcon(destination.absolutePath, icon.absolutePath)

        val copied = File(destination, apiProperties.defaultServerIcon.name)
        Assertions.assertTrue(copied.isFile, "64x64 icon must be copied")
        val image = ImageIO.read(copied)
        Assertions.assertEquals(64, image.width)
        Assertions.assertEquals(64, image.height)
    }

    /**
     * For a local server pack, a Windows-style Java path is escaped (colons and backslashes), and
     * generic placeholders are replaced with their configured values.
     */
    @Test
    fun replacePlaceholdersEscapesWindowsPathAndReplacesGenericKey() {
        val scriptSettings = hashMapOf(
            "SPC_JAVA_SPC" to "C:\\Program Files\\Java\\bin\\java.exe",
            "SPC_FOO_SPC" to "bar"
        )
        val result = provisioner.replacePlaceholders(true, "JAVA=SPC_JAVA_SPC FOO=SPC_FOO_SPC", scriptSettings)

        Assertions.assertTrue(result.contains("C\\:"), "Local Java path must have its colon escaped; got $result")
        Assertions.assertTrue(result.contains("FOO=bar"), "Generic placeholder must be replaced; got $result")
    }

    /**
     * For a zipped server pack, the Java-placeholder collapses to plain "java" regardless of the
     * configured path.
     */
    @Test
    fun replacePlaceholdersUsesPlainJavaForZippedPack() {
        val scriptSettings = hashMapOf("SPC_JAVA_SPC" to "C:\\Program Files\\Java\\bin\\java.exe")
        val result = provisioner.replacePlaceholders(false, "JAVA=SPC_JAVA_SPC", scriptSettings)

        Assertions.assertEquals("JAVA=java", result)
    }

    /**
     * Fabric: the release installer URL being reachable makes the installer downloadable.
     */
    @Test
    fun serverDownloadableFabricReachable() {
        val webUtilities = mockk<WebUtilities>()
        val versionMeta = mockk<VersionMeta>()
        val fabricMeta = mockk<FabricMeta>()
        val url = URI("https://example.com/fabric-installer").toURL()
        every { versionMeta.fabric } returns fabricMeta
        every { fabricMeta.releaseInstallerUrl() } returns url
        every { webUtilities.isReachable(url) } returns true

        Assertions.assertTrue(
            mockedProvisioner(versionMeta, webUtilities).serverDownloadable("1.20.1", "Fabric", "0.15.0")
        )
    }

    /**
     * Forge: a present instance whose installer URL is reachable is downloadable.
     */
    @Test
    fun serverDownloadableForgePresentAndReachable() {
        val webUtilities = mockk<WebUtilities>()
        val versionMeta = mockk<VersionMeta>()
        val forgeMeta = mockk<ForgeMeta>()
        val forgeInstance = mockk<ForgeInstance>()
        val url = URI("https://example.com/forge-installer").toURL()
        every { versionMeta.forge } returns forgeMeta
        every { forgeMeta.getForgeInstance("1.20.1", "47.2.0") } returns Optional.of(forgeInstance)
        every { forgeInstance.installerUrl } returns url
        every { webUtilities.isReachable(url) } returns true

        Assertions.assertTrue(
            mockedProvisioner(versionMeta, webUtilities).serverDownloadable("1.20.1", "Forge", "47.2.0")
        )
    }

    /**
     * Forge: an absent instance is not downloadable, and reachability is never consulted
     * (the `isPresent &&` short-circuits).
     */
    @Test
    fun serverDownloadableForgeAbsentIsNotDownloadable() {
        val webUtilities = mockk<WebUtilities>()
        val versionMeta = mockk<VersionMeta>()
        val forgeMeta = mockk<ForgeMeta>()
        every { versionMeta.forge } returns forgeMeta
        every { forgeMeta.getForgeInstance("1.20.1", "0.0.0") } returns Optional.empty()

        Assertions.assertFalse(
            mockedProvisioner(versionMeta, webUtilities).serverDownloadable("1.20.1", "Forge", "0.0.0")
        )
    }

    /**
     * Quilt: an unreachable release installer URL makes the installer not downloadable.
     */
    @Test
    fun serverDownloadableQuiltUnreachable() {
        val webUtilities = mockk<WebUtilities>()
        val versionMeta = mockk<VersionMeta>()
        val quiltMeta = mockk<QuiltMeta>()
        val url = URI("https://example.com/quilt-installer").toURL()
        every { versionMeta.quilt } returns quiltMeta
        every { quiltMeta.releaseInstallerUrl() } returns url
        every { webUtilities.isReachable(url) } returns false

        Assertions.assertFalse(
            mockedProvisioner(versionMeta, webUtilities).serverDownloadable("1.20.1", "Quilt", "0.26.0")
        )
    }

    /**
     * LegacyFabric: a malformed installer URL is caught and reported as not downloadable rather
     * than propagating.
     */
    @Test
    fun serverDownloadableLegacyFabricMalformedUrlIsNotDownloadable() {
        val webUtilities = mockk<WebUtilities>()
        val versionMeta = mockk<VersionMeta>()
        val legacyFabricMeta = mockk<LegacyFabricMeta>()
        every { versionMeta.legacyFabric } returns legacyFabricMeta
        every { legacyFabricMeta.releaseInstallerUrl() } throws MalformedURLException("malformed")

        Assertions.assertFalse(
            mockedProvisioner(versionMeta, webUtilities).serverDownloadable("1.20.1", "LegacyFabric", "1.0.0")
        )
    }

    /**
     * NeoForge: a present instance whose installer URL is reachable is downloadable.
     */
    @Test
    fun serverDownloadableNeoForgePresentAndReachable() {
        val webUtilities = mockk<WebUtilities>()
        val versionMeta = mockk<VersionMeta>()
        val neoForgeMeta = mockk<NeoForgeMeta>()
        val neoForgeInstance = mockk<NeoForgeInstance>()
        val url = URI("https://example.com/neoforge-installer").toURL()
        every { versionMeta.neoForge } returns neoForgeMeta
        every { neoForgeMeta.getNeoForgeInstance("1.20.1", "20.4.80") } returns Optional.of(neoForgeInstance)
        every { neoForgeInstance.installerUrl } returns url
        every { webUtilities.isReachable(url) } returns true

        Assertions.assertTrue(
            mockedProvisioner(versionMeta, webUtilities).serverDownloadable("1.20.1", "NeoForge", "20.4.80")
        )
    }

    /**
     * An unrecognized modloader is never downloadable (the `when`'s else branch).
     */
    @Test
    fun serverDownloadableUnknownModloaderIsNotDownloadable() {
        val webUtilities = mockk<WebUtilities>()
        val versionMeta = mockk<VersionMeta>()

        Assertions.assertFalse(
            mockedProvisioner(versionMeta, webUtilities).serverDownloadable("1.20.1", "NotALoader", "1.0.0")
        )
    }

    /**
     * When an improved Fabric launcher is available, it is copied into the destination as
     * `fabric-server-launcher.jar` and the accompanying `SERVER_PACK_INFO.txt` is written.
     */
    @Test
    fun getImprovedFabricLauncherCopiesWhenAvailable(@TempDir tempDir: File) {
        val webUtilities = mockk<WebUtilities>()
        val versionMeta = mockk<VersionMeta>()
        val fabricMeta = mockk<FabricMeta>()
        val sourceLauncher = File(tempDir, "source-launcher.jar").apply { writeText("launcher-bytes") }
        every { versionMeta.fabric } returns fabricMeta
        every { fabricMeta.launcherFor("1.20.1", "0.15.0") } returns Optional.of(sourceLauncher)
        val destination = File(tempDir, "pack").also { it.mkdirs() }

        mockedProvisioner(versionMeta, webUtilities)
            .getImprovedFabricLauncher("1.20.1", "0.15.0", destination.absolutePath)

        val launcher = File(destination, "fabric-server-launcher.jar")
        val info = File(destination, "SERVER_PACK_INFO.txt")
        Assertions.assertTrue(launcher.isFile, "Improved launcher must be copied")
        Assertions.assertEquals("launcher-bytes", launcher.readText())
        Assertions.assertTrue(info.isFile, "Accompanying info file must be written")
        Assertions.assertTrue(info.readText().contains("improved Fabric Server Launcher"))
    }

    /**
     * When no improved Fabric launcher is available, neither the launcher nor the info file is
     * written.
     */
    @Test
    fun getImprovedFabricLauncherWritesNothingWhenUnavailable(@TempDir tempDir: File) {
        val webUtilities = mockk<WebUtilities>()
        val versionMeta = mockk<VersionMeta>()
        val fabricMeta = mockk<FabricMeta>()
        every { versionMeta.fabric } returns fabricMeta
        every { fabricMeta.launcherFor("1.20.1", "0.15.0") } returns Optional.empty()
        val destination = File(tempDir, "pack").also { it.mkdirs() }

        mockedProvisioner(versionMeta, webUtilities)
            .getImprovedFabricLauncher("1.20.1", "0.15.0", destination.absolutePath)

        Assertions.assertFalse(File(destination, "fabric-server-launcher.jar").exists())
        Assertions.assertFalse(File(destination, "SERVER_PACK_INFO.txt").exists())
    }

    /**
     * The ZIP-exclusion *preference* and the caller-supplied exclusion are two different things, and
     * only one of them is optional.
     *
     * `zipBuilder` used to install its `ExcludeFileFilter` only when
     * `de.griefed.serverpackcreator.serverpack.zip.exclude.enabled` was set; it now installs one
     * unconditionally, because an update has to keep a running server's world and player-data out of
     * an archive meant to be handed to other people whatever that preference says. With the
     * preference off, the configured list must still be ignored -- that is what the preference means.
     */
    @Test
    fun theCallersExclusionAppliesEvenWithTheZipExclusionPreferenceOff(@TempDir tempDir: File) {
        val originalEnabled = apiProperties.isZipFileExclusionEnabled
        try {
            apiProperties.isZipFileExclusionEnabled = false
            apiProperties.zipArchiveExclusions = TreeSet(listOf("server.jar"))
            val destination = File(tempDir, "pack").also { it.mkdirs() }
            File(destination, "server.jar").writeText("the configured exclusion")
            File(destination, "ops.json").writeText("the caller's exclusion")
            File(destination, "mods").mkdirs()
            File(destination, "mods/alpha.jar").writeText("a mod")

            provisioner.zipBuilder("1.20.1", destination.absolutePath, "Forge", "47.4.0") { candidate ->
                candidate.name == "ops.json"
            }

            val entries = ZipFile(File(destination.absolutePath + "_server_pack.zip")).use { zip ->
                zip.entries().toList().map { it.name }
            }
            Assertions.assertTrue(
                entries.any { it == "server.jar" },
                "With the preference off the configured exclusions must be ignored, got $entries"
            )
            Assertions.assertTrue(
                entries.none { it == "ops.json" },
                "but the caller's exclusion still applies, got $entries"
            )
            Assertions.assertTrue(entries.any { it == "mods/alpha.jar" }, "and everything else is archived")
        } finally {
            apiProperties.isZipFileExclusionEnabled = originalEnabled
        }
    }

    /**
     * A provisioner whose pre- and post-install cleanup lists are exactly [entries], so a cleanup test
     * never edits the shared test home's settings.
     */
    private fun provisionerCleaning(vararg entries: String): ServerPackProvisioner {
        val properties = mockk<de.griefed.serverpackcreator.api.ApiProperties>()
        every { properties.preInstallCleanupFiles } returns TreeSet(entries.toList())
        every { properties.postInstallCleanupFiles } returns TreeSet(entries.toList())
        return ServerPackProvisioner(properties, mockk(), mockk())
    }

    /** A server pack holding one file, beside a sibling directory the cleanup must never reach. */
    private fun packBesideASibling(tempDir: File): Pair<File, File> {
        val destination = File(tempDir, "pack").also { it.mkdirs() }
        File(destination, "world.dat").writeText("a world")
        val sibling = File(tempDir, "sibling").also { it.mkdirs() }
        File(sibling, "keep.txt").writeText("not ours")
        return destination to sibling
    }

    /**
     * `File(destination, "")` is the destination itself, and cleanup deletes directories recursively, so a
     * blank entry -- a doubled comma in the setting -- must not delete the whole server pack.
     */
    @Test
    fun aBlankCleanupEntryLeavesTheServerPackInPlace(@TempDir tempDir: File) {
        val (destination, _) = packBesideASibling(tempDir)
        val provisioner = provisionerCleaning("", " ")

        provisioner.preInstallationCleanup(destination.absolutePath)
        provisioner.postInstallCleanup(destination.absolutePath)

        Assertions.assertTrue(File(destination, "world.dat").isFile, "the server pack must survive a blank entry")
    }

    /** A cleanup entry is a path inside the server pack; one that leaves it is refused, whatever it names. */
    @Test
    fun aCleanupEntryLeavingTheServerPackTouchesNothingOutsideIt(@TempDir tempDir: File) {
        val (destination, sibling) = packBesideASibling(tempDir)
        val provisioner = provisionerCleaning("..", "../sibling", "../sibling/keep.txt", ".")

        provisioner.preInstallationCleanup(destination.absolutePath)
        provisioner.postInstallCleanup(destination.absolutePath)

        Assertions.assertTrue(File(sibling, "keep.txt").isFile, "nothing outside the server pack may be deleted")
        Assertions.assertTrue(File(destination, "world.dat").isFile, "nor the server pack itself")
    }

    /** The refusal is narrow: an entry naming something inside the server pack is still deleted. */
    @Test
    fun aCleanupEntryInsideTheServerPackIsStillDeleted(@TempDir tempDir: File) {
        val (destination, _) = packBesideASibling(tempDir)
        File(destination, "libraries/nested").mkdirs()
        File(destination, "installer.log").writeText("leftover")
        val provisioner = provisionerCleaning("libraries", "installer.log")

        provisioner.postInstallCleanup(destination.absolutePath)

        Assertions.assertFalse(File(destination, "libraries").exists())
        Assertions.assertFalse(File(destination, "installer.log").exists())
        Assertions.assertTrue(File(destination, "world.dat").isFile)
    }

    /** A blank ZIP exclusion names the archive's own root; it must exclude nothing, not everything. */
    @Test
    fun aBlankZipExclusionExcludesNothing(@TempDir tempDir: File) {
        val originalEnabled = apiProperties.isZipFileExclusionEnabled
        val originalExclusions = apiProperties.zipArchiveExclusions
        try {
            apiProperties.isZipFileExclusionEnabled = true
            apiProperties.zipArchiveExclusions = TreeSet(listOf("", "server.jar"))
            val destination = File(tempDir, "pack").also { it.mkdirs() }
            File(destination, "server.jar").writeText("excluded")
            File(destination, "mods").mkdirs()
            File(destination, "mods/alpha.jar").writeText("a mod")

            provisioner.zipBuilder("1.20.1", destination.absolutePath, "Forge", "47.4.0")

            val entries = ZipFile(File(destination.absolutePath + "_server_pack.zip")).use { zip ->
                zip.entries().toList().map { it.name }
            }
            Assertions.assertTrue(entries.any { it == "mods/alpha.jar" }, "the pack must still be archived, got $entries")
            Assertions.assertTrue(entries.none { it == "server.jar" }, "the real exclusion still applies, got $entries")
        } finally {
            apiProperties.isZipFileExclusionEnabled = originalEnabled
            apiProperties.zipArchiveExclusions = originalExclusions
        }
    }
}
