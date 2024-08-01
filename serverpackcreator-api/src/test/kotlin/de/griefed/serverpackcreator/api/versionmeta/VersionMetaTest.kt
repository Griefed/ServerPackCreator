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
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).versionMeta

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
        Assertions.assertNotNull(versionMeta.minecraft.allVersions())
        Assertions.assertNotNull(versionMeta.minecraft.clientReleases())
        Assertions.assertNotNull(versionMeta.minecraft.clientSnapshots())
        Assertions.assertNotNull(versionMeta.minecraft.serverReleases())
        for (release in versionMeta.minecraft.clientReleases()) {
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
        for (release in versionMeta.minecraft.clientReleases()) {
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
        for (releaseClient in versionMeta.minecraft.clientReleases()) {
            Assertions.assertTrue(versionMeta.minecraft.getClient(releaseClient.version).isPresent)
        }
        Assertions.assertNotNull(versionMeta.minecraft.getServer("1.16.5").get())
        Assertions.assertTrue(versionMeta.minecraft.isServerAvailable("1.16.5"))
        Assertions.assertFalse(versionMeta.minecraft.isServerAvailable("1.16.6"))
        Assertions.assertNotNull(versionMeta.minecraft.latestReleaseServer())
        Assertions.assertNotNull(versionMeta.minecraft.latestSnapshotServer())
        for (snapshot in versionMeta.minecraft.clientSnapshots()) {
            Assertions.assertNotNull(snapshot)
            Assertions.assertNotNull(snapshot.version)
            Assertions.assertNotNull(snapshot.url)
            Assertions.assertEquals(snapshot.type, Type.SNAPSHOT)
        }
        Assertions.assertNotNull(versionMeta.minecraft.serverSnapshots())
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
        Assertions.assertNotNull(versionMeta.forge.forgeVersions())
        for (forgeVersion in versionMeta.forge.forgeVersions()) {
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
        for (minecraftVersion in versionMeta.forge.supportedMinecraftVersions()) {
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
        for (minecraftVersion in versionMeta.forge.supportedMinecraftVersions()) {
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
        Assertions.assertNotNull(versionMeta.forge.supportedMinecraftVersions())
        Assertions.assertNotNull(versionMeta.forge.supportedForgeVersions("1.16.5").get())
        Assertions.assertTrue(
            versionMeta.forge.supportedForgeVersions("1.16.5").isPresent
        )
        Assertions.assertFalse(
            versionMeta.forge.supportedForgeVersions("1.16.5123").isPresent
        )
        Assertions.assertNotNull(versionMeta.forge.supportedForgeVersions("1.16.5").get())
        Assertions.assertNotNull(
            versionMeta.forge.supportedForgeVersions("1.16.5").get()
        )
        Assertions.assertNotNull(versionMeta.forge.installerUrl("40.0.45").get())
        Assertions.assertTrue(versionMeta.forge.installerUrl("40.0.45").isPresent)
        Assertions.assertFalse(versionMeta.forge.installerUrl("40.0.41235").isPresent)
    }

    @Test
    fun fabric() {
        Assertions.assertNotNull(versionMeta.fabric.loaderVersions())
        Assertions.assertNotNull(versionMeta.fabric.latestLoader())
        Assertions.assertNotNull(versionMeta.fabric.releaseLoader())
        Assertions.assertNotNull(versionMeta.fabric.latestInstaller())
        Assertions.assertNotNull(versionMeta.fabric.releaseInstaller())
        Assertions.assertNotNull(versionMeta.fabric.installerVersions())
        Assertions.assertNotNull(versionMeta.fabric.latestInstallerUrl())
        Assertions.assertNotNull(versionMeta.fabric.releaseInstallerUrl())
        for (version in versionMeta.fabric.installerVersions()) {
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
        Assertions.assertNotNull(versionMeta.quilt.loaderVersions())
        Assertions.assertNotNull(versionMeta.quilt.latestLoader())
        Assertions.assertNotNull(versionMeta.quilt.releaseLoader())
        Assertions.assertNotNull(versionMeta.quilt.latestInstaller())
        Assertions.assertNotNull(versionMeta.quilt.releaseInstaller())
        Assertions.assertNotNull(versionMeta.quilt.installerVersions())
        Assertions.assertNotNull(versionMeta.quilt.latestInstallerUrl())
        Assertions.assertNotNull(versionMeta.quilt.releaseInstallerUrl())
        for (version in versionMeta.quilt.installerVersions()) {
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
        Assertions.assertNotNull(versionMeta.legacyFabric.loaderVersions())
        Assertions.assertNotNull(versionMeta.legacyFabric.latestLoader())
        Assertions.assertNotNull(versionMeta.legacyFabric.releaseLoader())
        Assertions.assertNotNull(versionMeta.legacyFabric.latestInstaller())
        Assertions.assertNotNull(versionMeta.legacyFabric.releaseInstaller())
        Assertions.assertNotNull(versionMeta.legacyFabric.installerVersions())
        Assertions.assertNotNull(versionMeta.legacyFabric.latestInstallerUrl())
        Assertions.assertNotNull(versionMeta.legacyFabric.releaseInstallerUrl())
        for (version in versionMeta.legacyFabric.installerVersions()) {
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