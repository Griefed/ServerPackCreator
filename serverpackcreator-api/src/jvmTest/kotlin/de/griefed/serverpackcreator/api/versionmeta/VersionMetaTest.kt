package de.griefed.serverpackcreator.api.versionmeta

import de.griefed.serverpackcreator.api.ApiWrapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import javax.xml.parsers.ParserConfigurationException

class VersionMetaTest {
    private val versionMeta: VersionMeta =
        ApiWrapper.api(File("src/jvmTest/resources/serverpackcreator.properties")).versionMeta!!

    @Test
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    fun meta() {
        Assertions.assertNotNull(versionMeta.update())
    }

    @Test
    fun minecraft() {
        Assertions.assertTrue(versionMeta.minecraft.isMinecraftVersionAvailable("1.16.5"))
        Assertions.assertFalse(versionMeta.minecraft.isMinecraftVersionAvailable("1.16.7"))
        Assertions.assertNotNull(versionMeta.minecraft.latestRelease())
        Assertions.assertNotNull(versionMeta.minecraft.latestRelease().version)
        Assertions.assertNotNull(versionMeta.minecraft.latestRelease().minecraftServer)
        Assertions.assertNotNull(versionMeta.minecraft.latestRelease().url)
        Assertions.assertEquals(versionMeta.minecraft.latestRelease().type, Type.RELEASE)
        Assertions.assertNotNull(versionMeta.minecraft.releaseVersionsDescending())
        Assertions.assertNotNull(versionMeta.minecraft.releaseVersionsAscending())
        Assertions.assertNotNull(versionMeta.minecraft.releaseVersionsArrayDescending())
        Assertions.assertEquals(
            versionMeta.minecraft.releaseVersionsArrayDescending().size,
            versionMeta.minecraft.releaseVersionsDescending().size
        )
        Assertions.assertEquals(
            versionMeta.minecraft.releaseVersionsArrayDescending()[0],
            versionMeta.minecraft.releaseVersionsArrayAscending().last()
        )
        Assertions.assertNotNull(versionMeta.minecraft.releaseVersionsArrayAscending())
        Assertions.assertEquals(
            versionMeta.minecraft.releaseVersionsArrayAscending().size,
            versionMeta.minecraft.releaseVersionsAscending().size
        )
        Assertions.assertEquals(
            versionMeta.minecraft.releaseVersionsArrayAscending()[0],
            versionMeta.minecraft.releaseVersionsArrayDescending().last()
        )
        for (release in versionMeta.minecraft.releasesDescending()) {
            Assertions.assertNotNull(release)
            Assertions.assertNotNull(release.version)
            Assertions.assertNotNull(release.minecraftServer)
            Assertions.assertNotNull(release.minecraftServer.minecraftVersion)
            Assertions.assertSame(Type.RELEASE, release.minecraftServer.releaseType)
            if (release.minecraftServer.url().isPresent) {
                Assertions.assertNotNull(release.minecraftServer.url().get())
            }
            if (release.minecraftServer.javaVersion().isPresent) {
                Assertions.assertTrue(release.minecraftServer.javaVersion().get() > 0)
            }
            Assertions.assertNotNull(release.url)
            Assertions.assertEquals(release.type, Type.RELEASE)
        }
        Assertions.assertNotNull(versionMeta.minecraft.latestSnapshot())
        Assertions.assertNotNull(versionMeta.minecraft.latestSnapshot().version)
        Assertions.assertNotNull(versionMeta.minecraft.latestSnapshot().url)
        Assertions.assertEquals(versionMeta.minecraft.latestSnapshot().type, Type.SNAPSHOT)
        Assertions.assertNotNull(versionMeta.minecraft.snapshotVersionsDescending())
        Assertions.assertNotNull(versionMeta.minecraft.snapshotVersionsAscending())
        Assertions.assertNotNull(versionMeta.minecraft.snapshotVersionsArrayDescending())
        Assertions.assertEquals(
            versionMeta.minecraft.snapshotVersionsArrayDescending().size,
            versionMeta.minecraft.snapshotVersionsDescending().size
        )
        Assertions.assertEquals(
            versionMeta.minecraft.snapshotVersionsArrayDescending()[0],
            versionMeta.minecraft.snapshotVersionsArrayAscending().last()
        )
        Assertions.assertNotNull(versionMeta.minecraft.snapshotVersionsArrayAscending())
        Assertions.assertEquals(
            versionMeta.minecraft.snapshotVersionsArrayAscending().size,
            versionMeta.minecraft.snapshotVersionsAscending().size
        )
        Assertions.assertEquals(
            versionMeta.minecraft.snapshotVersionsArrayAscending()[0],
            versionMeta.minecraft.snapshotVersionsArrayDescending().last()
        )
        for (release in versionMeta.minecraft.releasesDescending()) {
            Assertions.assertNotNull(release)
            Assertions.assertNotNull(release.version)
            Assertions.assertNotNull(release.url)
            Assertions.assertEquals(release.type, Type.RELEASE)
            if (release.forge().isPresent) {
                for (forgeInstance in release.forge().get()) {
                    Assertions.assertNotNull(forgeInstance)
                    Assertions.assertNotNull(forgeInstance.minecraftVersion)
                    Assertions.assertNotNull(forgeInstance.minecraftClient())
                    Assertions.assertNotNull(forgeInstance.installerUrl)
                    Assertions.assertNotNull(forgeInstance.forgeVersion)
                }
            }
        }
        for (mcVer in versionMeta.minecraft.releaseVersionsAscending()) {
            Assertions.assertTrue(versionMeta.minecraft.getClient(mcVer).isPresent)
        }
        Assertions.assertNotNull(versionMeta.minecraft.releasesArrayAscending())
        Assertions.assertEquals(
            versionMeta.minecraft.releasesArrayAscending().size,
            versionMeta.minecraft.releasesAscending().size
        )
        Assertions.assertEquals(
            versionMeta.minecraft.releasesArrayAscending()[0],
            versionMeta.minecraft.releasesArrayDescending().last()
        )
        Assertions.assertNotNull(versionMeta.minecraft.snapshotsDescending())
        Assertions.assertNotNull(versionMeta.minecraft.snapshotsAscending())
        Assertions.assertNotNull(versionMeta.minecraft.snapshotsArrayDescending())
        Assertions.assertEquals(
            versionMeta.minecraft.snapshotsArrayDescending().size,
            versionMeta.minecraft.snapshotsDescending().size
        )
        Assertions.assertEquals(
            versionMeta.minecraft.snapshotsArrayDescending()[0],
            versionMeta.minecraft.snapshotsArrayAscending().last()
        )
        Assertions.assertNotNull(versionMeta.minecraft.snapshotsArrayAscending())
        Assertions.assertEquals(
            versionMeta.minecraft.snapshotsArrayAscending().size,
            versionMeta.minecraft.snapshotsAscending().size
        )
        Assertions.assertEquals(
            versionMeta.minecraft.snapshotsArrayAscending()[0],
            versionMeta.minecraft.snapshotsArrayDescending().last()
        )
        Assertions.assertNotNull(versionMeta.minecraft.getServer("1.16.5").get())
        Assertions.assertTrue(versionMeta.minecraft.isServerAvailable("1.16.5"))
        Assertions.assertFalse(versionMeta.minecraft.isServerAvailable("1.16.6"))
        Assertions.assertNotNull(versionMeta.minecraft.latestReleaseServer())
        Assertions.assertNotNull(versionMeta.minecraft.latestSnapshotServer())
        Assertions.assertNotNull(versionMeta.minecraft.releasesServersDescending())
        Assertions.assertNotNull(versionMeta.minecraft.releasesServersAscending())
        for (snapshot in versionMeta.minecraft.snapshotsDescending()) {
            Assertions.assertNotNull(snapshot)
            Assertions.assertNotNull(snapshot.version)
            Assertions.assertNotNull(snapshot.url)
            Assertions.assertEquals(snapshot.type, Type.SNAPSHOT)
        }
        Assertions.assertNotNull(versionMeta.minecraft.releasesServersArrayDescending())
        Assertions.assertEquals(
            versionMeta.minecraft.releasesServersArrayDescending().size,
            versionMeta.minecraft.releasesServersDescending().size
        )
        Assertions.assertEquals(
            versionMeta.minecraft.releasesServersArrayDescending()[0],
            versionMeta.minecraft.releasesServersArrayAscending().last()
        )
        Assertions.assertNotNull(versionMeta.minecraft.releasesServersArrayAscending())
        Assertions.assertEquals(
            versionMeta.minecraft.releasesServersArrayAscending().size,
            versionMeta.minecraft.releasesServersAscending().size
        )
        Assertions.assertEquals(
            versionMeta.minecraft.releasesServersArrayAscending()[0],
            versionMeta.minecraft.releasesServersArrayDescending().last()
        )
        Assertions.assertNotNull(versionMeta.minecraft.snapshotsServersDescending())
        Assertions.assertNotNull(versionMeta.minecraft.snapshotsServersAscending())
        Assertions.assertNotNull(versionMeta.minecraft.snapshotsServersArrayDescending())
        Assertions.assertEquals(
            versionMeta.minecraft.snapshotsServersArrayDescending().size,
            versionMeta.minecraft.snapshotsServersDescending().size
        )
        Assertions.assertEquals(
            versionMeta.minecraft.snapshotsServersArrayDescending()[0],
            versionMeta.minecraft.snapshotsServersArrayAscending().last()
        )
        Assertions.assertNotNull(versionMeta.minecraft.snapshotsServersArrayAscending())
        Assertions.assertEquals(
            versionMeta.minecraft.snapshotsServersArrayAscending().size,
            versionMeta.minecraft.snapshotsServersAscending().size
        )
        Assertions.assertEquals(
            versionMeta.minecraft.snapshotsServersArrayAscending()[0],
            versionMeta.minecraft.snapshotsServersArrayDescending().last()
        )
        for (version in versionMeta.minecraft.snapshotsAscending()) {
            version.minecraftServer.url().isPresent
        }
    }

    @Test
    fun forge() {
        Assertions.assertTrue(versionMeta.forge.isForgeVersionValid("40.0.45"))
        Assertions.assertFalse(versionMeta.forge.isForgeVersionValid("40.99.99"))
        Assertions.assertTrue(versionMeta.forge.isMinecraftVersionSupported("1.16.5"))
        Assertions.assertFalse(versionMeta.forge.isMinecraftVersionSupported("1.16.8"))
        Assertions.assertTrue(versionMeta.forge.isForgeAndMinecraftCombinationValid("1.18.2", "40.0.45"))
        Assertions.assertFalse(versionMeta.forge.isForgeAndMinecraftCombinationValid("1.18.21", "99.0.45"))
        Assertions.assertTrue(versionMeta.forge.isForgeInstanceAvailable("1.18.2", "40.0.45"))
        Assertions.assertFalse(versionMeta.forge.isForgeInstanceAvailable("1.182.2", "40.023.45"))
        Assertions.assertTrue(versionMeta.forge.isForgeInstanceAvailable("40.0.4"))
        Assertions.assertFalse(versionMeta.forge.isForgeInstanceAvailable("40.0123.4"))
        Assertions.assertNotNull(versionMeta.forge.minecraftVersion("40.0.4").get())
        Assertions.assertTrue(versionMeta.forge.minecraftVersion("40.0.4").isPresent)
        Assertions.assertFalse(versionMeta.forge.minecraftVersion("40.0123.4").isPresent)
        Assertions.assertNotNull(versionMeta.forge.getForgeInstance("1.18.2", "40.0.45").get())
        Assertions.assertTrue(versionMeta.forge.getForgeInstance("1.18.2", "40.0.45").isPresent)
        Assertions.assertFalse(
            versionMeta.forge.getForgeInstance("1.18.2", "40.0123.45").isPresent
        )
        Assertions.assertNotNull(versionMeta.forge.getForgeInstance("40.0.45").get())
        Assertions.assertTrue(versionMeta.forge.getForgeInstance("40.0.45").isPresent)
        Assertions.assertFalse(versionMeta.forge.getForgeInstance("40.0.45123").isPresent)
        Assertions.assertNotNull(versionMeta.forge.forgeVersionsAscending())
        Assertions.assertNotNull(versionMeta.forge.forgeVersionsDescending())
        Assertions.assertNotNull(versionMeta.forge.forgeVersionsAscendingArray())
        Assertions.assertNotNull(versionMeta.forge.forgeVersionsDescendingArray())
        for (forgeVersion in versionMeta.forge.forgeVersionsAscending()) {
            Assertions.assertTrue(versionMeta.forge.getForgeInstance(forgeVersion).isPresent)
            Assertions.assertNotNull(versionMeta.forge.getForgeInstance(forgeVersion).get())
            Assertions.assertNotNull(
                versionMeta.forge.getForgeInstance(forgeVersion).get().forgeVersion
            )
            Assertions.assertNotNull(
                versionMeta.forge.getForgeInstance(forgeVersion).get().minecraftVersion
            )
            Assertions.assertNotNull(
                versionMeta.forge.getForgeInstance(forgeVersion).get().installerUrl
            )
            Assertions.assertNotNull(
                versionMeta.forge.getForgeInstance(forgeVersion).get().minecraftClient()
            )
        }
        for (minecraftVersion in versionMeta.forge.supportedMinecraftVersionsAscending()) {
            Assertions.assertTrue(
                versionMeta.forge.getForgeInstances(minecraftVersion).isPresent
            )
            for (instance in versionMeta.forge.getForgeInstances(minecraftVersion).get()) {
                Assertions.assertNotNull(instance)
                Assertions.assertNotNull(instance.installerUrl)
                Assertions.assertNotNull(instance.forgeVersion)
                Assertions.assertNotNull(instance.minecraftVersion)
                Assertions.assertNotNull(instance.minecraftClient())
            }
        }
        for (minecraftVersion in versionMeta.forge.supportedMinecraftVersionsDescending()) {
            Assertions.assertTrue(
                versionMeta.forge.getForgeInstances(minecraftVersion).isPresent
            )
            if (versionMeta.forge.getForgeInstances(minecraftVersion).isPresent) {
                for (instance in versionMeta.forge.getForgeInstances(minecraftVersion).get()) {
                    Assertions.assertNotNull(instance)
                    Assertions.assertNotNull(instance.installerUrl)
                    Assertions.assertNotNull(instance.forgeVersion)
                    Assertions.assertNotNull(instance.minecraftVersion)
                    Assertions.assertNotNull(instance.minecraftClient())
                }
            }
        }
        Assertions.assertNotNull(versionMeta.forge.newestForgeVersion("1.18.2").get())
        Assertions.assertTrue(versionMeta.forge.newestForgeVersion("1.18.2").isPresent)
        Assertions.assertFalse(versionMeta.forge.newestForgeVersion("1.18.2123").isPresent)
        Assertions.assertNotNull(versionMeta.forge.oldestForgeVersion("1.18.2").get())
        Assertions.assertTrue(versionMeta.forge.oldestForgeVersion("1.18.2").isPresent)
        Assertions.assertFalse(versionMeta.forge.oldestForgeVersion("1.18.2123").isPresent)
        Assertions.assertNotNull(versionMeta.forge.supportedMinecraftVersionsAscendingArray())
        Assertions.assertEquals(
            versionMeta.forge.supportedMinecraftVersionsAscendingArray().size,
            versionMeta.forge.supportedMinecraftVersionsAscending().size
        )
        Assertions.assertEquals(
            versionMeta.forge.supportedMinecraftVersionsAscendingArray()[0],
            versionMeta.forge.supportedMinecraftVersionsDescendingArray().last()
        )
        Assertions.assertNotNull(versionMeta.forge.supportedMinecraftVersionsDescendingArray())
        Assertions.assertEquals(
            versionMeta.forge.supportedMinecraftVersionsDescendingArray().size,
            versionMeta.forge.supportedMinecraftVersionsDescending().size
        )
        Assertions.assertEquals(
            versionMeta.forge.supportedMinecraftVersionsDescendingArray()[0],
            versionMeta.forge.supportedMinecraftVersionsAscendingArray().last()
        )
        Assertions.assertNotNull(versionMeta.forge.supportedForgeVersionsAscending("1.16.5").get())
        Assertions.assertTrue(
            versionMeta.forge.supportedForgeVersionsAscending("1.16.5").isPresent
        )
        Assertions.assertFalse(
            versionMeta.forge.supportedForgeVersionsAscending("1.16.5123").isPresent
        )
        Assertions.assertNotNull(versionMeta.forge.supportedForgeVersionsDescending("1.16.5").get())
        Assertions.assertTrue(
            versionMeta.forge.supportedForgeVersionsDescending("1.16.5").isPresent
        )
        Assertions.assertFalse(
            versionMeta.forge.supportedForgeVersionsDescending("1.16.5123").isPresent
        )
        Assertions.assertNotNull(
            versionMeta.forge.supportedForgeVersionsAscendingArray("1.16.5").get()
        )
        Assertions.assertTrue(
            versionMeta.forge.supportedForgeVersionsAscendingArray("1.16.5").isPresent
        )
        Assertions.assertFalse(
            versionMeta.forge.supportedForgeVersionsAscendingArray("1.16.5123").isPresent
        )
        Assertions.assertEquals(
            versionMeta.forge.supportedForgeVersionsAscendingArray("1.16.5").get().size,
            versionMeta.forge.supportedForgeVersionsAscending("1.16.5").get().size
        )
        Assertions.assertEquals(
            versionMeta.forge.supportedForgeVersionsAscendingArray("1.16.5").get()[0],
            versionMeta.forge.supportedForgeVersionsDescendingArray("1.16.5").get().last()
        )
        Assertions.assertNotNull(
            versionMeta.forge.supportedForgeVersionsDescendingArray("1.16.5").get()
        )
        Assertions.assertTrue(
            versionMeta.forge.supportedForgeVersionsDescendingArray("1.16.5").isPresent
        )
        Assertions.assertFalse(
            versionMeta.forge.supportedForgeVersionsDescendingArray("1.16.5123").isPresent
        )
        Assertions.assertEquals(
            versionMeta.forge.supportedForgeVersionsDescendingArray("1.16.5").get().size,
            versionMeta.forge.supportedForgeVersionsDescending("1.16.5").get().size
        )
        Assertions.assertEquals(
            versionMeta.forge.supportedForgeVersionsDescendingArray("1.16.5").get()[0],
            versionMeta.forge.supportedForgeVersionsAscendingArray("1.16.5").get().last()
        )
        Assertions.assertNotNull(versionMeta.forge.installerUrl("40.0.45").get())
        Assertions.assertTrue(versionMeta.forge.installerUrl("40.0.45").isPresent)
        Assertions.assertFalse(versionMeta.forge.installerUrl("40.0.41235").isPresent)
    }

    @Test
    fun fabric() {
        Assertions.assertNotNull(versionMeta.fabric.loaderVersionsListAscending())
        Assertions.assertNotNull(versionMeta.fabric.loaderVersionsListDescending())
        Assertions.assertNotNull(versionMeta.fabric.loaderVersionsArrayAscending())
        Assertions.assertNotNull(versionMeta.fabric.loaderVersionsArrayDescending())
        Assertions.assertNotNull(versionMeta.fabric.latestLoader())
        Assertions.assertNotNull(versionMeta.fabric.releaseLoader())
        Assertions.assertNotNull(versionMeta.fabric.latestInstaller())
        Assertions.assertNotNull(versionMeta.fabric.releaseInstaller())
        Assertions.assertNotNull(versionMeta.fabric.installerVersionsListAscending())
        Assertions.assertNotNull(versionMeta.fabric.installerVersionsListDescending())
        Assertions.assertNotNull(versionMeta.fabric.installerVersionsListAscending())
        Assertions.assertNotNull(versionMeta.fabric.installerVersionsArrayAscending())
        Assertions.assertEquals(
            versionMeta.fabric.installerVersionsArrayAscending().size,
            versionMeta.fabric.installerVersionsListAscending().size
        )
        Assertions.assertEquals(
            versionMeta.fabric.installerVersionsArrayAscending()[0],
            versionMeta.fabric.installerVersionsArrayDescending().last()
        )
        Assertions.assertNotNull(versionMeta.fabric.installerVersionsArrayDescending())
        Assertions.assertEquals(
            versionMeta.fabric.installerVersionsArrayDescending().size,
            versionMeta.fabric.installerVersionsListDescending().size
        )
        Assertions.assertEquals(
            versionMeta.fabric.installerVersionsArrayDescending()[0],
            versionMeta.fabric.installerVersionsArrayAscending().last()
        )
        Assertions.assertNotNull(versionMeta.fabric.latestInstallerUrl())
        Assertions.assertNotNull(versionMeta.fabric.releaseInstallerUrl())
        for (version in versionMeta.fabric.installerVersionsListAscending()) {
            Assertions.assertTrue(versionMeta.fabric.isInstallerUrlAvailable(version))
            Assertions.assertNotNull(versionMeta.fabric.getInstallerUrl(version).get())
        }
        Assertions.assertFalse(versionMeta.fabric.isInstallerUrlAvailable("0.11233.3"))
        Assertions.assertFalse(versionMeta.fabric.getInstallerUrl("0.13123.3").isPresent)
        Assertions.assertTrue(versionMeta.fabric.isVersionValid("0.13.3"))
        Assertions.assertFalse(versionMeta.fabric.isVersionValid("0.12313.3"))
    }

    @Test
    fun quilt() {
        Assertions.assertNotNull(versionMeta.quilt.loaderVersionsListAscending())
        Assertions.assertNotNull(versionMeta.quilt.loaderVersionsListDescending())
        Assertions.assertNotNull(versionMeta.quilt.loaderVersionsArrayAscending())
        Assertions.assertNotNull(versionMeta.quilt.loaderVersionsArrayDescending())
        Assertions.assertNotNull(versionMeta.quilt.latestLoader())
        Assertions.assertNotNull(versionMeta.quilt.releaseLoader())
        Assertions.assertNotNull(versionMeta.quilt.latestInstaller())
        Assertions.assertNotNull(versionMeta.quilt.releaseInstaller())
        Assertions.assertNotNull(versionMeta.quilt.installerVersionsListAscending())
        Assertions.assertNotNull(versionMeta.quilt.installerVersionsListDescending())
        Assertions.assertNotNull(versionMeta.quilt.installerVersionsListAscending())
        Assertions.assertNotNull(versionMeta.quilt.installerVersionsArrayAscending())
        Assertions.assertEquals(
            versionMeta.quilt.installerVersionsArrayAscending().size,
            versionMeta.quilt.installerVersionsListAscending().size
        )
        Assertions.assertEquals(
            versionMeta.quilt.installerVersionsArrayAscending()[0],
            versionMeta.quilt.installerVersionsArrayDescending().last()
        )
        Assertions.assertNotNull(versionMeta.quilt.installerVersionsArrayDescending())
        Assertions.assertEquals(
            versionMeta.quilt.installerVersionsArrayDescending().size,
            versionMeta.quilt.installerVersionsListDescending().size
        )
        Assertions.assertEquals(
            versionMeta.quilt.installerVersionsArrayDescending()[0],
            versionMeta.quilt.installerVersionsArrayAscending().last()
        )
        Assertions.assertNotNull(versionMeta.quilt.latestInstallerUrl())
        Assertions.assertNotNull(versionMeta.quilt.releaseInstallerUrl())
        for (version in versionMeta.quilt.installerVersionsListAscending()) {
            Assertions.assertTrue(versionMeta.quilt.isInstallerUrlAvailable(version))
            Assertions.assertNotNull(versionMeta.quilt.getInstallerUrl(version).get())
        }
        Assertions.assertFalse(versionMeta.quilt.isInstallerUrlAvailable("0.11233.3"))
        Assertions.assertFalse(versionMeta.quilt.getInstallerUrl("0.13123.3").isPresent)
        Assertions.assertTrue(versionMeta.quilt.isVersionValid("0.16.1"))
        Assertions.assertFalse(versionMeta.quilt.isVersionValid("0.12313.3"))
    }

    @Test
    @Throws(MalformedURLException::class)
    fun legacyFabric() {
        Assertions.assertNotNull(versionMeta.legacyFabric.loaderVersionsListAscending())
        Assertions.assertNotNull(versionMeta.legacyFabric.loaderVersionsListDescending())
        Assertions.assertNotNull(versionMeta.legacyFabric.loaderVersionsArrayAscending())
        Assertions.assertNotNull(versionMeta.legacyFabric.loaderVersionsArrayDescending())
        Assertions.assertNotNull(versionMeta.legacyFabric.latestLoader())
        Assertions.assertNotNull(versionMeta.legacyFabric.releaseLoader())
        Assertions.assertNotNull(versionMeta.legacyFabric.latestInstaller())
        Assertions.assertNotNull(versionMeta.legacyFabric.releaseInstaller())
        Assertions.assertNotNull(versionMeta.legacyFabric.installerVersionsListAscending())
        Assertions.assertNotNull(versionMeta.legacyFabric.installerVersionsListDescending())
        Assertions.assertNotNull(versionMeta.legacyFabric.installerVersionsListAscending())
        Assertions.assertNotNull(versionMeta.legacyFabric.installerVersionsArrayAscending())
        Assertions.assertEquals(
            versionMeta.legacyFabric.installerVersionsArrayAscending().size,
            versionMeta.legacyFabric.installerVersionsListAscending().size
        )
        Assertions.assertEquals(
            versionMeta.legacyFabric.installerVersionsArrayAscending()[0],
            versionMeta.legacyFabric.installerVersionsArrayDescending().last()
        )
        Assertions.assertNotNull(versionMeta.legacyFabric.installerVersionsArrayDescending())
        Assertions.assertEquals(
            versionMeta.legacyFabric.installerVersionsArrayDescending().size,
            versionMeta.legacyFabric.installerVersionsListDescending().size
        )
        Assertions.assertEquals(
            versionMeta.legacyFabric.installerVersionsArrayDescending()[0],
            versionMeta.legacyFabric.installerVersionsArrayAscending().last()
        )
        Assertions.assertNotNull(versionMeta.legacyFabric.latestInstallerUrl())
        Assertions.assertNotNull(versionMeta.legacyFabric.releaseInstallerUrl())
        for (version in versionMeta.legacyFabric.installerVersionsListAscending()) {
            Assertions.assertTrue(versionMeta.legacyFabric.isInstallerUrlAvailable(version))
            try {
                Assertions.assertNotNull(versionMeta.legacyFabric.getInstallerUrl(version).get())
            } catch (e: MalformedURLException) {
                throw RuntimeException(e)
            }
        }
        Assertions.assertFalse(versionMeta.legacyFabric.isInstallerUrlAvailable("0.11233.3"))
        Assertions.assertFalse(versionMeta.legacyFabric.getInstallerUrl("0.13123.3").isPresent)
        Assertions.assertTrue(versionMeta.legacyFabric.isVersionValid("0.13.3"))
        Assertions.assertFalse(versionMeta.legacyFabric.isVersionValid("0.12313.3"))
    }
}