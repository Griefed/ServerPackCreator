package de.griefed.serverpackcreator.versionmeta;

import de.griefed.serverpackcreator.ApplicationProperties;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VersionMetaTest {

  private final VersionMeta VERSIONMETA;

  public VersionMetaTest() throws IOException {
    ApplicationProperties APPLICATIONPROPERTIES = new ApplicationProperties();
    this.VERSIONMETA =
        new VersionMeta(
            APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.QUILT_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION());
  }

  @Test
  void meta() throws IOException {
    Assertions.assertNotNull(VERSIONMETA.update());
  }

  @Test
  void minecraft() {
    Assertions.assertTrue(VERSIONMETA.minecraft().checkMinecraftVersion("1.16.5"));
    Assertions.assertFalse(VERSIONMETA.minecraft().checkMinecraftVersion("1.16.7"));
    Assertions.assertNotNull(VERSIONMETA.minecraft().latestRelease());
    Assertions.assertNotNull(VERSIONMETA.minecraft().latestRelease().version());
    Assertions.assertNotNull(VERSIONMETA.minecraft().latestRelease().server());
    Assertions.assertNotNull(VERSIONMETA.minecraft().latestRelease().url());
    Assertions.assertEquals(VERSIONMETA.minecraft().latestRelease().type(), Type.RELEASE);
    Assertions.assertNotNull(VERSIONMETA.minecraft().releaseVersionsDescending());
    Assertions.assertNotNull(VERSIONMETA.minecraft().releaseVersionsAscending());
    Assertions.assertNotNull(VERSIONMETA.minecraft().releaseVersionsArrayDescending());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().releaseVersionsArrayDescending().length,
        VERSIONMETA.minecraft().releaseVersionsDescending().size());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().releaseVersionsArrayDescending()[0],
        VERSIONMETA.minecraft()
            .releaseVersionsArrayAscending()[
            VERSIONMETA.minecraft().releaseVersionsArrayAscending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.minecraft().releaseVersionsArrayAscending());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().releaseVersionsArrayAscending().length,
        VERSIONMETA.minecraft().releaseVersionsAscending().size());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().releaseVersionsArrayAscending()[0],
        VERSIONMETA.minecraft()
            .releaseVersionsArrayDescending()[
            VERSIONMETA.minecraft().releaseVersionsArrayDescending().length - 1]);
    VERSIONMETA
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
    Assertions.assertNotNull(VERSIONMETA.minecraft().latestSnapshot());
    Assertions.assertNotNull(VERSIONMETA.minecraft().latestSnapshot().version());
    Assertions.assertNotNull(VERSIONMETA.minecraft().latestSnapshot().url());
    Assertions.assertEquals(VERSIONMETA.minecraft().latestSnapshot().type(), Type.SNAPSHOT);
    Assertions.assertNotNull(VERSIONMETA.minecraft().snapshotsVersionsDescending());
    Assertions.assertNotNull(VERSIONMETA.minecraft().snapshotVersionsAscending());
    Assertions.assertNotNull(VERSIONMETA.minecraft().snapshotVersionsArrayDescending());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().snapshotVersionsArrayDescending().length,
        VERSIONMETA.minecraft().snapshotsVersionsDescending().size());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().snapshotVersionsArrayDescending()[0],
        VERSIONMETA.minecraft()
            .snapshotVersionsArrayAscending()[
            VERSIONMETA.minecraft().snapshotVersionsArrayAscending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.minecraft().snapshotVersionsArrayAscending());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().snapshotVersionsArrayAscending().length,
        VERSIONMETA.minecraft().snapshotVersionsAscending().size());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().snapshotVersionsArrayAscending()[0],
        VERSIONMETA.minecraft()
            .snapshotVersionsArrayDescending()[
            VERSIONMETA.minecraft().snapshotVersionsArrayDescending().length - 1]);
    VERSIONMETA
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
    VERSIONMETA
        .minecraft()
        .releaseVersionsAscending()
        .forEach(
            mcVer -> Assertions.assertTrue(VERSIONMETA.minecraft().getClient(mcVer).isPresent()));
    Assertions.assertNotNull(VERSIONMETA.minecraft().releasesArrayAscending());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().releasesArrayAscending().length,
        VERSIONMETA.minecraft().releasesAscending().size());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().releasesArrayAscending()[0],
        VERSIONMETA.minecraft()
            .releasesArrayDescending()[
            VERSIONMETA.minecraft().releasesArrayDescending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.minecraft().snapshotsDescending());
    Assertions.assertNotNull(VERSIONMETA.minecraft().snapshotsAscending());
    Assertions.assertNotNull(VERSIONMETA.minecraft().snapshotsArrayDescending());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().snapshotsArrayDescending().length,
        VERSIONMETA.minecraft().snapshotsDescending().size());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().snapshotsArrayDescending()[0],
        VERSIONMETA.minecraft()
            .snapshotsArrayAscending()[
            VERSIONMETA.minecraft().snapshotsArrayAscending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.minecraft().snapshotsArrayAscending());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().snapshotsArrayAscending().length,
        VERSIONMETA.minecraft().snapshotsAscending().size());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().snapshotsArrayAscending()[0],
        VERSIONMETA.minecraft()
            .snapshotsArrayDescending()[
            VERSIONMETA.minecraft().snapshotsArrayDescending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.minecraft().getServer("1.16.5").get());
    Assertions.assertTrue(VERSIONMETA.minecraft().checkServerAvailability("1.16.5"));
    Assertions.assertFalse(VERSIONMETA.minecraft().checkServerAvailability("1.16.6"));
    Assertions.assertNotNull(VERSIONMETA.minecraft().latestReleaseServer());
    Assertions.assertNotNull(VERSIONMETA.minecraft().latestSnapshotServer());
    Assertions.assertNotNull(VERSIONMETA.minecraft().releasesServersDescending());
    Assertions.assertNotNull(VERSIONMETA.minecraft().releasesServersAscending());
    VERSIONMETA
        .minecraft()
        .snapshotsDescending()
        .forEach(
            snapshot -> {
              Assertions.assertNotNull(snapshot);
              Assertions.assertNotNull(snapshot.version());
              Assertions.assertNotNull(snapshot.url());
              Assertions.assertEquals(snapshot.type(), Type.SNAPSHOT);
            });
    Assertions.assertNotNull(VERSIONMETA.minecraft().releasesServersArrayDescending());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().releasesServersArrayDescending().length,
        VERSIONMETA.minecraft().releasesServersDescending().size());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().releasesServersArrayDescending()[0],
        VERSIONMETA.minecraft()
            .releasesServersArrayAscending()[
            VERSIONMETA.minecraft().releasesServersArrayAscending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.minecraft().releasesServersArrayAscending());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().releasesServersArrayAscending().length,
        VERSIONMETA.minecraft().releasesServersAscending().size());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().releasesServersArrayAscending()[0],
        VERSIONMETA.minecraft()
            .releasesServersArrayDescending()[
            VERSIONMETA.minecraft().releasesServersArrayDescending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.minecraft().snapshotsServersDescending());
    Assertions.assertNotNull(VERSIONMETA.minecraft().snapshotsServersAscending());
    Assertions.assertNotNull(VERSIONMETA.minecraft().snapshotsServersArrayDescending());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().snapshotsServersArrayDescending().length,
        VERSIONMETA.minecraft().snapshotsServersDescending().size());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().snapshotsServersArrayDescending()[0],
        VERSIONMETA.minecraft()
            .snapshotsServersArrayAscending()[
            VERSIONMETA.minecraft().snapshotsServersArrayAscending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.minecraft().snapshotsServersArrayAscending());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().snapshotsServersArrayAscending().length,
        VERSIONMETA.minecraft().snapshotsServersAscending().size());
    Assertions.assertEquals(
        VERSIONMETA.minecraft().snapshotsServersArrayAscending()[0],
        VERSIONMETA.minecraft()
            .snapshotsServersArrayDescending()[
            VERSIONMETA.minecraft().snapshotsServersArrayDescending().length - 1]);
  }

  @Test
  void forge() {
    Assertions.assertTrue(VERSIONMETA.forge().checkForgeVersion("40.0.45"));
    Assertions.assertFalse(VERSIONMETA.forge().checkForgeVersion("40.99.99"));
    Assertions.assertTrue(VERSIONMETA.forge().checkMinecraftVersion("1.16.5"));
    Assertions.assertFalse(VERSIONMETA.forge().checkMinecraftVersion("1.16.8"));
    Assertions.assertTrue(VERSIONMETA.forge().checkForgeAndMinecraftVersion("1.18.2", "40.0.45"));
    Assertions.assertFalse(VERSIONMETA.forge().checkForgeAndMinecraftVersion("1.18.21", "99.0.45"));
    Assertions.assertTrue(VERSIONMETA.forge().isForgeInstanceAvailable("1.18.2", "40.0.45"));
    Assertions.assertFalse(VERSIONMETA.forge().isForgeInstanceAvailable("1.182.2", "40.023.45"));
    Assertions.assertTrue(VERSIONMETA.forge().isForgeInstanceAvailable("40.0.4"));
    Assertions.assertFalse(VERSIONMETA.forge().isForgeInstanceAvailable("40.0123.4"));
    Assertions.assertNotNull(VERSIONMETA.forge().supportedMinecraftVersion("40.0.4").get());
    Assertions.assertTrue(VERSIONMETA.forge().supportedMinecraftVersion("40.0.4").isPresent());
    Assertions.assertFalse(VERSIONMETA.forge().supportedMinecraftVersion("40.0123.4").isPresent());
    Assertions.assertNotNull(VERSIONMETA.forge().getForgeInstance("1.18.2", "40.0.45").get());
    Assertions.assertTrue(VERSIONMETA.forge().getForgeInstance("1.18.2", "40.0.45").isPresent());
    Assertions.assertFalse(
        VERSIONMETA.forge().getForgeInstance("1.18.2", "40.0123.45").isPresent());
    Assertions.assertNotNull(VERSIONMETA.forge().getForgeInstance("40.0.45").get());
    Assertions.assertTrue(VERSIONMETA.forge().getForgeInstance("40.0.45").isPresent());
    Assertions.assertFalse(VERSIONMETA.forge().getForgeInstance("40.0.45123").isPresent());
    Assertions.assertNotNull(VERSIONMETA.forge().forgeVersions());
    Assertions.assertNotNull(VERSIONMETA.forge().forgeVersionsDescending());
    Assertions.assertNotNull(VERSIONMETA.forge().forgeVersionsArray());
    Assertions.assertNotNull(VERSIONMETA.forge().forgeVersionsArrayDescending());
    VERSIONMETA
        .forge()
        .forgeVersions()
        .forEach(
            forgeVersion -> {
              Assertions.assertTrue(VERSIONMETA.forge().getForgeInstance(forgeVersion).isPresent());
              Assertions.assertNotNull(VERSIONMETA.forge().getForgeInstance(forgeVersion).get());
              Assertions.assertNotNull(
                  VERSIONMETA.forge().getForgeInstance(forgeVersion).get().forgeVersion());
              Assertions.assertNotNull(
                  VERSIONMETA.forge().getForgeInstance(forgeVersion).get().minecraftVersion());
              Assertions.assertNotNull(
                  VERSIONMETA.forge().getForgeInstance(forgeVersion).get().installerUrl());
              Assertions.assertNotNull(
                  VERSIONMETA.forge().getForgeInstance(forgeVersion).get().minecraftClient());
            });
    VERSIONMETA
        .forge()
        .minecraftVersionsAscending()
        .forEach(
            minecraftVersion -> {
              Assertions.assertTrue(
                  VERSIONMETA.forge().getForgeInstances(minecraftVersion).isPresent());
              VERSIONMETA
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
    VERSIONMETA
        .forge()
        .minecraftVersionsDescending()
        .forEach(
            minecraftVersion -> {
              Assertions.assertTrue(
                  VERSIONMETA.forge().getForgeInstances(minecraftVersion).isPresent());
              if (VERSIONMETA.forge().getForgeInstances(minecraftVersion).isPresent()) {
                VERSIONMETA
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
    Assertions.assertNotNull(VERSIONMETA.forge().latestForgeVersion("1.18.2").get());
    Assertions.assertTrue(VERSIONMETA.forge().latestForgeVersion("1.18.2").isPresent());
    Assertions.assertFalse(VERSIONMETA.forge().latestForgeVersion("1.18.2123").isPresent());
    Assertions.assertNotNull(VERSIONMETA.forge().oldestForgeVersion("1.18.2").get());
    Assertions.assertTrue(VERSIONMETA.forge().oldestForgeVersion("1.18.2").isPresent());
    Assertions.assertFalse(VERSIONMETA.forge().oldestForgeVersion("1.18.2123").isPresent());
    Assertions.assertNotNull(VERSIONMETA.forge().minecraftVersionsArrayAscending());
    Assertions.assertEquals(
        VERSIONMETA.forge().minecraftVersionsArrayAscending().length,
        VERSIONMETA.forge().minecraftVersionsAscending().size());
    Assertions.assertEquals(
        VERSIONMETA.forge().minecraftVersionsArrayAscending()[0],
        VERSIONMETA.forge()
            .minecraftVersionsArrayDescending()[
            VERSIONMETA.forge().minecraftVersionsArrayDescending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.forge().minecraftVersionsArrayDescending());
    Assertions.assertEquals(
        VERSIONMETA.forge().minecraftVersionsArrayDescending().length,
        VERSIONMETA.forge().minecraftVersionsDescending().size());
    Assertions.assertEquals(
        VERSIONMETA.forge().minecraftVersionsArrayDescending()[0],
        VERSIONMETA.forge()
            .minecraftVersionsArrayAscending()[
            VERSIONMETA.forge().minecraftVersionsArrayAscending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.forge().availableForgeVersionsAscending("1.16.5").get());
    Assertions.assertTrue(
        VERSIONMETA.forge().availableForgeVersionsAscending("1.16.5").isPresent());
    Assertions.assertFalse(
        VERSIONMETA.forge().availableForgeVersionsAscending("1.16.5123").isPresent());
    Assertions.assertNotNull(VERSIONMETA.forge().availableForgeVersionsDescending("1.16.5").get());
    Assertions.assertTrue(
        VERSIONMETA.forge().availableForgeVersionsDescending("1.16.5").isPresent());
    Assertions.assertFalse(
        VERSIONMETA.forge().availableForgeVersionsDescending("1.16.5123").isPresent());
    Assertions.assertNotNull(
        VERSIONMETA.forge().availableForgeVersionsArrayAscending("1.16.5").get());
    Assertions.assertTrue(
        VERSIONMETA.forge().availableForgeVersionsArrayAscending("1.16.5").isPresent());
    Assertions.assertFalse(
        VERSIONMETA.forge().availableForgeVersionsArrayAscending("1.16.5123").isPresent());
    Assertions.assertEquals(
        VERSIONMETA.forge().availableForgeVersionsArrayAscending("1.16.5").get().length,
        VERSIONMETA.forge().availableForgeVersionsAscending("1.16.5").get().size());
    Assertions.assertEquals(
        VERSIONMETA.forge().availableForgeVersionsArrayAscending("1.16.5").get()[0],
        VERSIONMETA
            .forge()
            .availableForgeVersionsArrayDescending("1.16.5")
            .get()[
            VERSIONMETA.forge().availableForgeVersionsArrayDescending("1.16.5").get().length - 1]);
    Assertions.assertNotNull(
        VERSIONMETA.forge().availableForgeVersionsArrayDescending("1.16.5").get());
    Assertions.assertTrue(
        VERSIONMETA.forge().availableForgeVersionsArrayDescending("1.16.5").isPresent());
    Assertions.assertFalse(
        VERSIONMETA.forge().availableForgeVersionsArrayDescending("1.16.5123").isPresent());
    Assertions.assertEquals(
        VERSIONMETA.forge().availableForgeVersionsArrayDescending("1.16.5").get().length,
        VERSIONMETA.forge().availableForgeVersionsDescending("1.16.5").get().size());
    Assertions.assertEquals(
        VERSIONMETA.forge().availableForgeVersionsArrayDescending("1.16.5").get()[0],
        VERSIONMETA
            .forge()
            .availableForgeVersionsArrayAscending("1.16.5")
            .get()[
            VERSIONMETA.forge().availableForgeVersionsArrayAscending("1.16.5").get().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.forge().installerUrl("40.0.45").get());
    Assertions.assertTrue(VERSIONMETA.forge().installerUrl("40.0.45").isPresent());
    Assertions.assertFalse(VERSIONMETA.forge().installerUrl("40.0.41235").isPresent());
  }

  @Test
  void fabric() {
    Assertions.assertNotNull(VERSIONMETA.fabric().loaderVersionsAscending());
    Assertions.assertNotNull(VERSIONMETA.fabric().loaderVersionsDescending());
    Assertions.assertNotNull(VERSIONMETA.fabric().loaderVersionsArrayAscending());
    Assertions.assertNotNull(VERSIONMETA.fabric().loaderVersionsArrayDescending());
    Assertions.assertNotNull(VERSIONMETA.fabric().latestLoaderVersion());
    Assertions.assertNotNull(VERSIONMETA.fabric().releaseLoaderVersion());
    Assertions.assertNotNull(VERSIONMETA.fabric().latestInstallerVersion());
    Assertions.assertNotNull(VERSIONMETA.fabric().releaseInstallerVersion());
    Assertions.assertNotNull(VERSIONMETA.fabric().installerVersionsAscending());
    Assertions.assertNotNull(VERSIONMETA.fabric().installerVersionsDescending());
    Assertions.assertNotNull(VERSIONMETA.fabric().installerVersionsAscending());
    Assertions.assertNotNull(VERSIONMETA.fabric().installerVersionsArrayAscending());
    Assertions.assertEquals(
        VERSIONMETA.fabric().installerVersionsArrayAscending().length,
        VERSIONMETA.fabric().installerVersionsAscending().size());
    Assertions.assertEquals(
        VERSIONMETA.fabric().installerVersionsArrayAscending()[0],
        VERSIONMETA.fabric()
            .installerVersionsArrayDescending()[
            VERSIONMETA.fabric().installerVersionsArrayDescending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.fabric().installerVersionsArrayDescending());
    Assertions.assertEquals(
        VERSIONMETA.fabric().installerVersionsArrayDescending().length,
        VERSIONMETA.fabric().installerVersionsDescending().size());
    Assertions.assertEquals(
        VERSIONMETA.fabric().installerVersionsArrayDescending()[0],
        VERSIONMETA.fabric()
            .installerVersionsArrayAscending()[
            VERSIONMETA.fabric().installerVersionsArrayAscending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.fabric().latestInstallerUrl());
    Assertions.assertNotNull(VERSIONMETA.fabric().releaseInstallerUrl());
    VERSIONMETA
        .fabric()
        .installerVersionsAscending()
        .forEach(
            version -> {
              Assertions.assertTrue(VERSIONMETA.fabric().isInstallerUrlAvailable(version));
              Assertions.assertNotNull(VERSIONMETA.fabric().installerUrl(version).get());
            });
    Assertions.assertFalse(VERSIONMETA.fabric().isInstallerUrlAvailable("0.11233.3"));
    Assertions.assertFalse(VERSIONMETA.fabric().installerUrl("0.13123.3").isPresent());
    Assertions.assertTrue(VERSIONMETA.fabric().checkFabricVersion("0.13.3"));
    Assertions.assertFalse(VERSIONMETA.fabric().checkFabricVersion("0.12313.3"));
  }

  @Test
  void quilt() {
    Assertions.assertNotNull(VERSIONMETA.quilt().loaderVersionsAscending());
    Assertions.assertNotNull(VERSIONMETA.quilt().loaderVersionsDescending());
    Assertions.assertNotNull(VERSIONMETA.quilt().loaderVersionsArrayAscending());
    Assertions.assertNotNull(VERSIONMETA.quilt().loaderVersionsArrayDescending());
    Assertions.assertNotNull(VERSIONMETA.quilt().latestLoaderVersion());
    Assertions.assertNotNull(VERSIONMETA.quilt().releaseLoaderVersion());
    Assertions.assertNotNull(VERSIONMETA.quilt().latestInstallerVersion());
    Assertions.assertNotNull(VERSIONMETA.quilt().releaseInstallerVersion());
    Assertions.assertNotNull(VERSIONMETA.quilt().installerVersionsAscending());
    Assertions.assertNotNull(VERSIONMETA.quilt().installerVersionsDescending());
    Assertions.assertNotNull(VERSIONMETA.quilt().installerVersionsAscending());
    Assertions.assertNotNull(VERSIONMETA.quilt().installerVersionsArrayAscending());
    Assertions.assertEquals(
        VERSIONMETA.quilt().installerVersionsArrayAscending().length,
        VERSIONMETA.quilt().installerVersionsAscending().size());
    Assertions.assertEquals(
        VERSIONMETA.quilt().installerVersionsArrayAscending()[0],
        VERSIONMETA.quilt()
            .installerVersionsArrayDescending()[
            VERSIONMETA.quilt().installerVersionsArrayDescending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.quilt().installerVersionsArrayDescending());
    Assertions.assertEquals(
        VERSIONMETA.quilt().installerVersionsArrayDescending().length,
        VERSIONMETA.quilt().installerVersionsDescending().size());
    Assertions.assertEquals(
        VERSIONMETA.quilt().installerVersionsArrayDescending()[0],
        VERSIONMETA.quilt()
            .installerVersionsArrayAscending()[
            VERSIONMETA.quilt().installerVersionsArrayAscending().length - 1]);
    Assertions.assertNotNull(VERSIONMETA.quilt().latestInstallerUrl());
    Assertions.assertNotNull(VERSIONMETA.quilt().releaseInstallerUrl());
    VERSIONMETA
        .quilt()
        .installerVersionsAscending()
        .forEach(
            version -> {
              Assertions.assertTrue(VERSIONMETA.quilt().isInstallerUrlAvailable(version));
              Assertions.assertNotNull(VERSIONMETA.quilt().installerUrl(version).get());
            });
    Assertions.assertFalse(VERSIONMETA.quilt().isInstallerUrlAvailable("0.11233.3"));
    Assertions.assertFalse(VERSIONMETA.quilt().installerUrl("0.13123.3").isPresent());
    Assertions.assertTrue(VERSIONMETA.quilt().checkQuiltVersion("0.16.1"));
    Assertions.assertFalse(VERSIONMETA.quilt().checkQuiltVersion("0.12313.3"));
  }
}
