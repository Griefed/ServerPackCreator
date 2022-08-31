package de.griefed.serverpackcreator.versionmeta;

import de.griefed.serverpackcreator.ServerPackCreator;
import java.io.IOException;
import java.net.MalformedURLException;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public class VersionMetaTest {

  private final VersionMeta versionMeta;

  public VersionMetaTest() throws IOException, ParserConfigurationException, SAXException {
    versionMeta = ServerPackCreator.getInstance().getVersionMeta();
  }

  @Test
  void meta() throws IOException, ParserConfigurationException, SAXException {
    Assertions.assertNotNull(versionMeta.update());
  }

  @Test
  void minecraft() {
    Assertions.assertTrue(versionMeta.minecraft().checkMinecraftVersion("1.16.5"));
    Assertions.assertFalse(versionMeta.minecraft().checkMinecraftVersion("1.16.7"));
    Assertions.assertNotNull(versionMeta.minecraft().latestRelease());
    Assertions.assertNotNull(versionMeta.minecraft().latestRelease().version());
    Assertions.assertNotNull(versionMeta.minecraft().latestRelease().server());
    Assertions.assertNotNull(versionMeta.minecraft().latestRelease().url());
    Assertions.assertEquals(versionMeta.minecraft().latestRelease().type(), Type.RELEASE);
    Assertions.assertNotNull(versionMeta.minecraft().releaseVersionsDescending());
    Assertions.assertNotNull(versionMeta.minecraft().releaseVersionsAscending());
    Assertions.assertNotNull(versionMeta.minecraft().releaseVersionsArrayDescending());
    Assertions.assertEquals(
        versionMeta.minecraft().releaseVersionsArrayDescending().length,
        versionMeta.minecraft().releaseVersionsDescending().size());
    Assertions.assertEquals(
        versionMeta.minecraft().releaseVersionsArrayDescending()[0],
        versionMeta.minecraft()
            .releaseVersionsArrayAscending()[
            versionMeta.minecraft().releaseVersionsArrayAscending().length - 1]);
    Assertions.assertNotNull(versionMeta.minecraft().releaseVersionsArrayAscending());
    Assertions.assertEquals(
        versionMeta.minecraft().releaseVersionsArrayAscending().length,
        versionMeta.minecraft().releaseVersionsAscending().size());
    Assertions.assertEquals(
        versionMeta.minecraft().releaseVersionsArrayAscending()[0],
        versionMeta.minecraft()
            .releaseVersionsArrayDescending()[
            versionMeta.minecraft().releaseVersionsArrayDescending().length - 1]);
    versionMeta
        .minecraft()
        .releasesDescending()
        .forEach(
            release -> {
              Assertions.assertNotNull(release);
              Assertions.assertNotNull(release.version());
              Assertions.assertNotNull(release.server());
              Assertions.assertNotNull(release.server().version());
              Assertions.assertSame(Type.RELEASE, release.server().type());
              if (release.server().url().isPresent()) {
                Assertions.assertNotNull(release.server().url().get());
              }
              if (release.server().javaVersion().isPresent()) {
                Assertions.assertTrue(release.server().javaVersion().get() > 0);
              }
              Assertions.assertNotNull(release.url());
              Assertions.assertEquals(release.type(), Type.RELEASE);
            });
    Assertions.assertNotNull(versionMeta.minecraft().latestSnapshot());
    Assertions.assertNotNull(versionMeta.minecraft().latestSnapshot().version());
    Assertions.assertNotNull(versionMeta.minecraft().latestSnapshot().url());
    Assertions.assertEquals(versionMeta.minecraft().latestSnapshot().type(), Type.SNAPSHOT);
    Assertions.assertNotNull(versionMeta.minecraft().snapshotVersionsDescending());
    Assertions.assertNotNull(versionMeta.minecraft().snapshotVersionsAscending());
    Assertions.assertNotNull(versionMeta.minecraft().snapshotVersionsArrayDescending());
    Assertions.assertEquals(
        versionMeta.minecraft().snapshotVersionsArrayDescending().length,
        versionMeta.minecraft().snapshotVersionsDescending().size());
    Assertions.assertEquals(
        versionMeta.minecraft().snapshotVersionsArrayDescending()[0],
        versionMeta.minecraft()
            .snapshotVersionsArrayAscending()[
            versionMeta.minecraft().snapshotVersionsArrayAscending().length - 1]);
    Assertions.assertNotNull(versionMeta.minecraft().snapshotVersionsArrayAscending());
    Assertions.assertEquals(
        versionMeta.minecraft().snapshotVersionsArrayAscending().length,
        versionMeta.minecraft().snapshotVersionsAscending().size());
    Assertions.assertEquals(
        versionMeta.minecraft().snapshotVersionsArrayAscending()[0],
        versionMeta.minecraft()
            .snapshotVersionsArrayDescending()[
            versionMeta.minecraft().snapshotVersionsArrayDescending().length - 1]);
    versionMeta
        .minecraft()
        .releasesDescending()
        .forEach(
            release -> {
              Assertions.assertNotNull(release);
              Assertions.assertNotNull(release.version());
              Assertions.assertNotNull(release.url());
              Assertions.assertEquals(release.type(), Type.RELEASE);
              if (release.forge().isPresent()) {
                release
                    .forge()
                    .get()
                    .forEach(
                        forgeInstance -> {
                          Assertions.assertNotNull(forgeInstance);
                          Assertions.assertNotNull(forgeInstance.minecraftVersion());
                          Assertions.assertNotNull(forgeInstance.minecraftClient());
                          Assertions.assertNotNull(forgeInstance.installerUrl());
                          Assertions.assertNotNull(forgeInstance.forgeVersion());
                        });
              }
            });
    versionMeta
        .minecraft()
        .releaseVersionsAscending()
        .forEach(
            mcVer -> Assertions.assertTrue(versionMeta.minecraft().getClient(mcVer).isPresent()));
    Assertions.assertNotNull(versionMeta.minecraft().releasesArrayAscending());
    Assertions.assertEquals(
        versionMeta.minecraft().releasesArrayAscending().length,
        versionMeta.minecraft().releasesAscending().size());
    Assertions.assertEquals(
        versionMeta.minecraft().releasesArrayAscending()[0],
        versionMeta.minecraft()
            .releasesArrayDescending()[
            versionMeta.minecraft().releasesArrayDescending().length - 1]);
    Assertions.assertNotNull(versionMeta.minecraft().snapshotsDescending());
    Assertions.assertNotNull(versionMeta.minecraft().snapshotsAscending());
    Assertions.assertNotNull(versionMeta.minecraft().snapshotsArrayDescending());
    Assertions.assertEquals(
        versionMeta.minecraft().snapshotsArrayDescending().length,
        versionMeta.minecraft().snapshotsDescending().size());
    Assertions.assertEquals(
        versionMeta.minecraft().snapshotsArrayDescending()[0],
        versionMeta.minecraft()
            .snapshotsArrayAscending()[
            versionMeta.minecraft().snapshotsArrayAscending().length - 1]);
    Assertions.assertNotNull(versionMeta.minecraft().snapshotsArrayAscending());
    Assertions.assertEquals(
        versionMeta.minecraft().snapshotsArrayAscending().length,
        versionMeta.minecraft().snapshotsAscending().size());
    Assertions.assertEquals(
        versionMeta.minecraft().snapshotsArrayAscending()[0],
        versionMeta.minecraft()
            .snapshotsArrayDescending()[
            versionMeta.minecraft().snapshotsArrayDescending().length - 1]);
    Assertions.assertNotNull(versionMeta.minecraft().getServer("1.16.5").get());
    Assertions.assertTrue(versionMeta.minecraft().checkServerAvailability("1.16.5"));
    Assertions.assertFalse(versionMeta.minecraft().checkServerAvailability("1.16.6"));
    Assertions.assertNotNull(versionMeta.minecraft().latestReleaseServer());
    Assertions.assertNotNull(versionMeta.minecraft().latestSnapshotServer());
    Assertions.assertNotNull(versionMeta.minecraft().releasesServersDescending());
    Assertions.assertNotNull(versionMeta.minecraft().releasesServersAscending());
    versionMeta
        .minecraft()
        .snapshotsDescending()
        .forEach(
            snapshot -> {
              Assertions.assertNotNull(snapshot);
              Assertions.assertNotNull(snapshot.version());
              Assertions.assertNotNull(snapshot.url());
              Assertions.assertEquals(snapshot.type(), Type.SNAPSHOT);
            });
    Assertions.assertNotNull(versionMeta.minecraft().releasesServersArrayDescending());
    Assertions.assertEquals(
        versionMeta.minecraft().releasesServersArrayDescending().length,
        versionMeta.minecraft().releasesServersDescending().size());
    Assertions.assertEquals(
        versionMeta.minecraft().releasesServersArrayDescending()[0],
        versionMeta.minecraft()
            .releasesServersArrayAscending()[
            versionMeta.minecraft().releasesServersArrayAscending().length - 1]);
    Assertions.assertNotNull(versionMeta.minecraft().releasesServersArrayAscending());
    Assertions.assertEquals(
        versionMeta.minecraft().releasesServersArrayAscending().length,
        versionMeta.minecraft().releasesServersAscending().size());
    Assertions.assertEquals(
        versionMeta.minecraft().releasesServersArrayAscending()[0],
        versionMeta.minecraft()
            .releasesServersArrayDescending()[
            versionMeta.minecraft().releasesServersArrayDescending().length - 1]);
    Assertions.assertNotNull(versionMeta.minecraft().snapshotsServersDescending());
    Assertions.assertNotNull(versionMeta.minecraft().snapshotsServersAscending());
    Assertions.assertNotNull(versionMeta.minecraft().snapshotsServersArrayDescending());
    Assertions.assertEquals(
        versionMeta.minecraft().snapshotsServersArrayDescending().length,
        versionMeta.minecraft().snapshotsServersDescending().size());
    Assertions.assertEquals(
        versionMeta.minecraft().snapshotsServersArrayDescending()[0],
        versionMeta.minecraft()
            .snapshotsServersArrayAscending()[
            versionMeta.minecraft().snapshotsServersArrayAscending().length - 1]);
    Assertions.assertNotNull(versionMeta.minecraft().snapshotsServersArrayAscending());
    Assertions.assertEquals(
        versionMeta.minecraft().snapshotsServersArrayAscending().length,
        versionMeta.minecraft().snapshotsServersAscending().size());
    Assertions.assertEquals(
        versionMeta.minecraft().snapshotsServersArrayAscending()[0],
        versionMeta.minecraft()
            .snapshotsServersArrayDescending()[
            versionMeta.minecraft().snapshotsServersArrayDescending().length - 1]);
  }

  @Test
  void forge() {
    Assertions.assertTrue(versionMeta.forge().checkForgeVersion("40.0.45"));
    Assertions.assertFalse(versionMeta.forge().checkForgeVersion("40.99.99"));
    Assertions.assertTrue(versionMeta.forge().checkMinecraftVersion("1.16.5"));
    Assertions.assertFalse(versionMeta.forge().checkMinecraftVersion("1.16.8"));
    Assertions.assertTrue(versionMeta.forge().checkForgeAndMinecraftVersion("1.18.2", "40.0.45"));
    Assertions.assertFalse(versionMeta.forge().checkForgeAndMinecraftVersion("1.18.21", "99.0.45"));
    Assertions.assertTrue(versionMeta.forge().isForgeInstanceAvailable("1.18.2", "40.0.45"));
    Assertions.assertFalse(versionMeta.forge().isForgeInstanceAvailable("1.182.2", "40.023.45"));
    Assertions.assertTrue(versionMeta.forge().isForgeInstanceAvailable("40.0.4"));
    Assertions.assertFalse(versionMeta.forge().isForgeInstanceAvailable("40.0123.4"));
    Assertions.assertNotNull(versionMeta.forge().supportedMinecraftVersion("40.0.4").get());
    Assertions.assertTrue(versionMeta.forge().supportedMinecraftVersion("40.0.4").isPresent());
    Assertions.assertFalse(versionMeta.forge().supportedMinecraftVersion("40.0123.4").isPresent());
    Assertions.assertNotNull(versionMeta.forge().getForgeInstance("1.18.2", "40.0.45").get());
    Assertions.assertTrue(versionMeta.forge().getForgeInstance("1.18.2", "40.0.45").isPresent());
    Assertions.assertFalse(
        versionMeta.forge().getForgeInstance("1.18.2", "40.0123.45").isPresent());
    Assertions.assertNotNull(versionMeta.forge().getForgeInstance("40.0.45").get());
    Assertions.assertTrue(versionMeta.forge().getForgeInstance("40.0.45").isPresent());
    Assertions.assertFalse(versionMeta.forge().getForgeInstance("40.0.45123").isPresent());
    Assertions.assertNotNull(versionMeta.forge().forgeVersions());
    Assertions.assertNotNull(versionMeta.forge().forgeVersionsDescending());
    Assertions.assertNotNull(versionMeta.forge().forgeVersionsArray());
    Assertions.assertNotNull(versionMeta.forge().forgeVersionsArrayDescending());
    versionMeta
        .forge()
        .forgeVersions()
        .forEach(
            forgeVersion -> {
              Assertions.assertTrue(versionMeta.forge().getForgeInstance(forgeVersion).isPresent());
              Assertions.assertNotNull(versionMeta.forge().getForgeInstance(forgeVersion).get());
              Assertions.assertNotNull(
                  versionMeta.forge().getForgeInstance(forgeVersion).get().forgeVersion());
              Assertions.assertNotNull(
                  versionMeta.forge().getForgeInstance(forgeVersion).get().minecraftVersion());
              Assertions.assertNotNull(
                  versionMeta.forge().getForgeInstance(forgeVersion).get().installerUrl());
              Assertions.assertNotNull(
                  versionMeta.forge().getForgeInstance(forgeVersion).get().minecraftClient());
            });
    versionMeta
        .forge()
        .minecraftVersionsAscending()
        .forEach(
            minecraftVersion -> {
              Assertions.assertTrue(
                  versionMeta.forge().getForgeInstances(minecraftVersion).isPresent());
              versionMeta
                  .forge()
                  .getForgeInstances(minecraftVersion)
                  .get()
                  .forEach(
                      instance -> {
                        Assertions.assertNotNull(instance);
                        Assertions.assertNotNull(instance.installerUrl());
                        Assertions.assertNotNull(instance.forgeVersion());
                        Assertions.assertNotNull(instance.minecraftVersion());
                        Assertions.assertNotNull(instance.minecraftClient());
                      });
            });
    versionMeta
        .forge()
        .minecraftVersionsDescending()
        .forEach(
            minecraftVersion -> {
              Assertions.assertTrue(
                  versionMeta.forge().getForgeInstances(minecraftVersion).isPresent());
              if (versionMeta.forge().getForgeInstances(minecraftVersion).isPresent()) {
                versionMeta
                    .forge()
                    .getForgeInstances(minecraftVersion)
                    .get()
                    .forEach(
                        instance -> {
                          Assertions.assertNotNull(instance);
                          Assertions.assertNotNull(instance.installerUrl());
                          Assertions.assertNotNull(instance.forgeVersion());
                          Assertions.assertNotNull(instance.minecraftVersion());
                          Assertions.assertNotNull(instance.minecraftClient());
                        });
              }
            });
    Assertions.assertNotNull(versionMeta.forge().latestForgeVersion("1.18.2").get());
    Assertions.assertTrue(versionMeta.forge().latestForgeVersion("1.18.2").isPresent());
    Assertions.assertFalse(versionMeta.forge().latestForgeVersion("1.18.2123").isPresent());
    Assertions.assertNotNull(versionMeta.forge().oldestForgeVersion("1.18.2").get());
    Assertions.assertTrue(versionMeta.forge().oldestForgeVersion("1.18.2").isPresent());
    Assertions.assertFalse(versionMeta.forge().oldestForgeVersion("1.18.2123").isPresent());
    Assertions.assertNotNull(versionMeta.forge().minecraftVersionsArrayAscending());
    Assertions.assertEquals(
        versionMeta.forge().minecraftVersionsArrayAscending().length,
        versionMeta.forge().minecraftVersionsAscending().size());
    Assertions.assertEquals(
        versionMeta.forge().minecraftVersionsArrayAscending()[0],
        versionMeta.forge()
            .minecraftVersionsArrayDescending()[
            versionMeta.forge().minecraftVersionsArrayDescending().length - 1]);
    Assertions.assertNotNull(versionMeta.forge().minecraftVersionsArrayDescending());
    Assertions.assertEquals(
        versionMeta.forge().minecraftVersionsArrayDescending().length,
        versionMeta.forge().minecraftVersionsDescending().size());
    Assertions.assertEquals(
        versionMeta.forge().minecraftVersionsArrayDescending()[0],
        versionMeta.forge()
            .minecraftVersionsArrayAscending()[
            versionMeta.forge().minecraftVersionsArrayAscending().length - 1]);
    Assertions.assertNotNull(versionMeta.forge().availableForgeVersionsAscending("1.16.5").get());
    Assertions.assertTrue(
        versionMeta.forge().availableForgeVersionsAscending("1.16.5").isPresent());
    Assertions.assertFalse(
        versionMeta.forge().availableForgeVersionsAscending("1.16.5123").isPresent());
    Assertions.assertNotNull(versionMeta.forge().availableForgeVersionsDescending("1.16.5").get());
    Assertions.assertTrue(
        versionMeta.forge().availableForgeVersionsDescending("1.16.5").isPresent());
    Assertions.assertFalse(
        versionMeta.forge().availableForgeVersionsDescending("1.16.5123").isPresent());
    Assertions.assertNotNull(
        versionMeta.forge().availableForgeVersionsArrayAscending("1.16.5").get());
    Assertions.assertTrue(
        versionMeta.forge().availableForgeVersionsArrayAscending("1.16.5").isPresent());
    Assertions.assertFalse(
        versionMeta.forge().availableForgeVersionsArrayAscending("1.16.5123").isPresent());
    Assertions.assertEquals(
        versionMeta.forge().availableForgeVersionsArrayAscending("1.16.5").get().length,
        versionMeta.forge().availableForgeVersionsAscending("1.16.5").get().size());
    Assertions.assertEquals(
        versionMeta.forge().availableForgeVersionsArrayAscending("1.16.5").get()[0],
        versionMeta
            .forge()
            .availableForgeVersionsArrayDescending("1.16.5")
            .get()[
            versionMeta.forge().availableForgeVersionsArrayDescending("1.16.5").get().length - 1]);
    Assertions.assertNotNull(
        versionMeta.forge().availableForgeVersionsArrayDescending("1.16.5").get());
    Assertions.assertTrue(
        versionMeta.forge().availableForgeVersionsArrayDescending("1.16.5").isPresent());
    Assertions.assertFalse(
        versionMeta.forge().availableForgeVersionsArrayDescending("1.16.5123").isPresent());
    Assertions.assertEquals(
        versionMeta.forge().availableForgeVersionsArrayDescending("1.16.5").get().length,
        versionMeta.forge().availableForgeVersionsDescending("1.16.5").get().size());
    Assertions.assertEquals(
        versionMeta.forge().availableForgeVersionsArrayDescending("1.16.5").get()[0],
        versionMeta
            .forge()
            .availableForgeVersionsArrayAscending("1.16.5")
            .get()[
            versionMeta.forge().availableForgeVersionsArrayAscending("1.16.5").get().length - 1]);
    Assertions.assertNotNull(versionMeta.forge().installerUrl("40.0.45").get());
    Assertions.assertTrue(versionMeta.forge().installerUrl("40.0.45").isPresent());
    Assertions.assertFalse(versionMeta.forge().installerUrl("40.0.41235").isPresent());
  }

  @Test
  void fabric() {
    Assertions.assertNotNull(versionMeta.fabric().loaderVersionsListAscending());
    Assertions.assertNotNull(versionMeta.fabric().loaderVersionsListDescending());
    Assertions.assertNotNull(versionMeta.fabric().loaderVersionsArrayAscending());
    Assertions.assertNotNull(versionMeta.fabric().loaderVersionsArrayDescending());
    Assertions.assertNotNull(versionMeta.fabric().latestLoader());
    Assertions.assertNotNull(versionMeta.fabric().releaseLoader());
    Assertions.assertNotNull(versionMeta.fabric().latestInstaller());
    Assertions.assertNotNull(versionMeta.fabric().releaseInstaller());
    Assertions.assertNotNull(versionMeta.fabric().installerVersionsListAscending());
    Assertions.assertNotNull(versionMeta.fabric().installerVersionsListDescending());
    Assertions.assertNotNull(versionMeta.fabric().installerVersionsListAscending());
    Assertions.assertNotNull(versionMeta.fabric().installerVersionsArrayAscending());
    Assertions.assertEquals(
        versionMeta.fabric().installerVersionsArrayAscending().length,
        versionMeta.fabric().installerVersionsListAscending().size());
    Assertions.assertEquals(
        versionMeta.fabric().installerVersionsArrayAscending()[0],
        versionMeta.fabric()
            .installerVersionsArrayDescending()[
            versionMeta.fabric().installerVersionsArrayDescending().length - 1]);
    Assertions.assertNotNull(versionMeta.fabric().installerVersionsArrayDescending());
    Assertions.assertEquals(
        versionMeta.fabric().installerVersionsArrayDescending().length,
        versionMeta.fabric().installerVersionsListDescending().size());
    Assertions.assertEquals(
        versionMeta.fabric().installerVersionsArrayDescending()[0],
        versionMeta.fabric()
            .installerVersionsArrayAscending()[
            versionMeta.fabric().installerVersionsArrayAscending().length - 1]);
    Assertions.assertNotNull(versionMeta.fabric().latestInstallerUrl());
    Assertions.assertNotNull(versionMeta.fabric().releaseInstallerUrl());
    versionMeta
        .fabric()
        .installerVersionsListAscending()
        .forEach(
            version -> {
              Assertions.assertTrue(versionMeta.fabric().isInstallerUrlAvailable(version));
              Assertions.assertNotNull(versionMeta.fabric().getInstallerUrl(version).get());
            });
    Assertions.assertFalse(versionMeta.fabric().isInstallerUrlAvailable("0.11233.3"));
    Assertions.assertFalse(versionMeta.fabric().getInstallerUrl("0.13123.3").isPresent());
    Assertions.assertTrue(versionMeta.fabric().isVersionValid("0.13.3"));
    Assertions.assertFalse(versionMeta.fabric().isVersionValid("0.12313.3"));
  }

  @Test
  void quilt() {
    Assertions.assertNotNull(versionMeta.quilt().loaderVersionsListAscending());
    Assertions.assertNotNull(versionMeta.quilt().loaderVersionsListDescending());
    Assertions.assertNotNull(versionMeta.quilt().loaderVersionsArrayAscending());
    Assertions.assertNotNull(versionMeta.quilt().loaderVersionsArrayDescending());
    Assertions.assertNotNull(versionMeta.quilt().latestLoader());
    Assertions.assertNotNull(versionMeta.quilt().releaseLoader());
    Assertions.assertNotNull(versionMeta.quilt().latestInstaller());
    Assertions.assertNotNull(versionMeta.quilt().releaseInstaller());
    Assertions.assertNotNull(versionMeta.quilt().installerVersionsListAscending());
    Assertions.assertNotNull(versionMeta.quilt().installerVersionsListDescending());
    Assertions.assertNotNull(versionMeta.quilt().installerVersionsListAscending());
    Assertions.assertNotNull(versionMeta.quilt().installerVersionsArrayAscending());
    Assertions.assertEquals(
        versionMeta.quilt().installerVersionsArrayAscending().length,
        versionMeta.quilt().installerVersionsListAscending().size());
    Assertions.assertEquals(
        versionMeta.quilt().installerVersionsArrayAscending()[0],
        versionMeta.quilt()
            .installerVersionsArrayDescending()[
            versionMeta.quilt().installerVersionsArrayDescending().length - 1]);
    Assertions.assertNotNull(versionMeta.quilt().installerVersionsArrayDescending());
    Assertions.assertEquals(
        versionMeta.quilt().installerVersionsArrayDescending().length,
        versionMeta.quilt().installerVersionsListDescending().size());
    Assertions.assertEquals(
        versionMeta.quilt().installerVersionsArrayDescending()[0],
        versionMeta.quilt()
            .installerVersionsArrayAscending()[
            versionMeta.quilt().installerVersionsArrayAscending().length - 1]);
    Assertions.assertNotNull(versionMeta.quilt().latestInstallerUrl());
    Assertions.assertNotNull(versionMeta.quilt().releaseInstallerUrl());
    versionMeta
        .quilt()
        .installerVersionsListAscending()
        .forEach(
            version -> {
              Assertions.assertTrue(versionMeta.quilt().isInstallerUrlAvailable(version));
              Assertions.assertNotNull(versionMeta.quilt().getInstallerUrl(version).get());
            });
    Assertions.assertFalse(versionMeta.quilt().isInstallerUrlAvailable("0.11233.3"));
    Assertions.assertFalse(versionMeta.quilt().getInstallerUrl("0.13123.3").isPresent());
    Assertions.assertTrue(versionMeta.quilt().isVersionValid("0.16.1"));
    Assertions.assertFalse(versionMeta.quilt().isVersionValid("0.12313.3"));
  }

  @Test
  void legacyFabric() throws MalformedURLException {
    Assertions.assertNotNull(versionMeta.legacyFabric().loaderVersionsListAscending());
    Assertions.assertNotNull(versionMeta.legacyFabric().loaderVersionsListDescending());
    Assertions.assertNotNull(versionMeta.legacyFabric().loaderVersionsArrayAscending());
    Assertions.assertNotNull(versionMeta.legacyFabric().loaderVersionsArrayDescending());
    Assertions.assertNotNull(versionMeta.legacyFabric().latestLoader());
    Assertions.assertNotNull(versionMeta.legacyFabric().releaseLoader());
    Assertions.assertNotNull(versionMeta.legacyFabric().latestInstaller());
    Assertions.assertNotNull(versionMeta.legacyFabric().releaseInstaller());
    Assertions.assertNotNull(versionMeta.legacyFabric().installerVersionsListAscending());
    Assertions.assertNotNull(versionMeta.legacyFabric().installerVersionsListDescending());
    Assertions.assertNotNull(versionMeta.legacyFabric().installerVersionsListAscending());
    Assertions.assertNotNull(versionMeta.legacyFabric().installerVersionsArrayAscending());
    Assertions.assertEquals(
        versionMeta.legacyFabric().installerVersionsArrayAscending().length,
        versionMeta.legacyFabric().installerVersionsListAscending().size());
    Assertions.assertEquals(
        versionMeta.legacyFabric().installerVersionsArrayAscending()[0],
        versionMeta.legacyFabric()
            .installerVersionsArrayDescending()[
            versionMeta.legacyFabric().installerVersionsArrayDescending().length - 1]);
    Assertions.assertNotNull(versionMeta.legacyFabric().installerVersionsArrayDescending());
    Assertions.assertEquals(
        versionMeta.legacyFabric().installerVersionsArrayDescending().length,
        versionMeta.legacyFabric().installerVersionsListDescending().size());
    Assertions.assertEquals(
        versionMeta.legacyFabric().installerVersionsArrayDescending()[0],
        versionMeta.legacyFabric()
            .installerVersionsArrayAscending()[
            versionMeta.legacyFabric().installerVersionsArrayAscending().length - 1]);
    Assertions.assertNotNull(versionMeta.legacyFabric().latestInstallerUrl());
    Assertions.assertNotNull(versionMeta.legacyFabric().releaseInstallerUrl());
    versionMeta
        .legacyFabric()
        .installerVersionsListAscending()
        .forEach(
            version -> {
              Assertions.assertTrue(versionMeta.legacyFabric().isInstallerUrlAvailable(version));
              try {
                Assertions.assertNotNull(versionMeta.legacyFabric().getInstallerUrl(version).get());
              } catch (MalformedURLException e) {
                throw new RuntimeException(e);
              }
            });
    Assertions.assertFalse(versionMeta.legacyFabric().isInstallerUrlAvailable("0.11233.3"));
    Assertions.assertFalse(versionMeta.legacyFabric().getInstallerUrl("0.13123.3").isPresent());
    Assertions.assertTrue(versionMeta.legacyFabric().isVersionValid("0.13.3"));
    Assertions.assertFalse(versionMeta.legacyFabric().isVersionValid("0.12313.3"));
  }
}
