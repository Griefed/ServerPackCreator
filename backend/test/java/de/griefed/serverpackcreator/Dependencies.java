package de.griefed.serverpackcreator;

import com.electronwill.nightconfig.toml.TomlParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.modscanning.AnnotationScanner;
import de.griefed.serverpackcreator.modscanning.FabricScanner;
import de.griefed.serverpackcreator.modscanning.ModScanner;
import de.griefed.serverpackcreator.modscanning.QuiltScanner;
import de.griefed.serverpackcreator.modscanning.TomlScanner;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

public final class Dependencies {

  private final ServerPackCreator SERVER_PACK_CREATOR;
  private final ObjectMapper OBJECT_MAPPER;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final I18n I18N;
  private final Utilities UTILITIES;
  private final ConfigUtilities CONFIGUTILITIES;
  private final VersionMeta VERSIONMETA;
  private final ConfigurationHandler CONFIGURATIONHANDLER;
  private final ServerPackHandler SERVERPACKHANDLER;
  private final ApplicationPlugins APPLICATION_PLUGINS;
  private final ModScanner MODSCANNER;

  private static final class InstanceHolder {

    static final Dependencies instance = new Dependencies();
  }

  public static Dependencies getInstance() {
    return InstanceHolder.instance;
  }

  private Dependencies() {
    try {
      FileUtils.copyDirectory(
          new File("backend/test/resources/testresources/addons"), new File("plugins"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      FileUtils.copyFile(
          new File("backend/test/resources/serverpackcreator.properties"),
          new File("serverpackcreator.properties"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      FileUtils.copyFile(
          new File("backend/test/resources/serverpackcreator.db"),
          new File("serverpackcreator.db"));
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      SERVER_PACK_CREATOR = new ServerPackCreator(new String[] {"--setup"});
      SERVER_PACK_CREATOR.run();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    OBJECT_MAPPER =
        new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    APPLICATIONPROPERTIES = new ApplicationProperties();
    I18N = new I18n(APPLICATIONPROPERTIES);
    UTILITIES = new Utilities(I18N, APPLICATIONPROPERTIES);
    CONFIGUTILITIES = new ConfigUtilities(UTILITIES, APPLICATIONPROPERTIES, OBJECT_MAPPER);
    MODSCANNER =
        new ModScanner(
            new AnnotationScanner(OBJECT_MAPPER),
            new FabricScanner(OBJECT_MAPPER),
            new QuiltScanner(OBJECT_MAPPER),
            new TomlScanner(new TomlParser()));
    APPLICATION_PLUGINS = new ApplicationPlugins();

    try {
      VERSIONMETA =
          new VersionMeta(
              APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST_LOCATION(),
              APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST_LOCATION(),
              APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST_LOCATION(),
              APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION(),
              APPLICATIONPROPERTIES.FABRIC_INTERMEDIARIES_MANIFEST_LOCATION(),
              APPLICATIONPROPERTIES.QUILT_VERSION_MANIFEST_LOCATION(),
              APPLICATIONPROPERTIES.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION(),
              OBJECT_MAPPER);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      CONFIGURATIONHANDLER =
          new ConfigurationHandler(
              I18N, VERSIONMETA, APPLICATIONPROPERTIES, UTILITIES, CONFIGUTILITIES);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      SERVERPACKHANDLER =
          new ServerPackHandler(
              APPLICATIONPROPERTIES, VERSIONMETA, UTILITIES, APPLICATION_PLUGINS, MODSCANNER);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ServerPackCreator SERVER_PACK_CREATOR() {
    return SERVER_PACK_CREATOR;
  }

  public ObjectMapper OBJECT_MAPPER() {
    return OBJECT_MAPPER;
  }

  public ApplicationProperties APPLICATIONPROPERTIES() {
    return APPLICATIONPROPERTIES;
  }

  public I18n I18N() {
    return I18N;
  }

  public Utilities UTILITIES() {
    return UTILITIES;
  }

  public ConfigUtilities CONFIGUTILITIES() {
    return CONFIGUTILITIES;
  }

  public VersionMeta VERSIONMETA() {
    return VERSIONMETA;
  }

  public ConfigurationHandler CONFIGURATIONHANDLER() {
    return CONFIGURATIONHANDLER;
  }

  public ServerPackHandler SERVERPACKHANDLER() {
    return SERVERPACKHANDLER;
  }

  public ApplicationPlugins APPLICATION_PLUGINS() {
    return APPLICATION_PLUGINS;
  }

  public ModScanner MODSCANNER() {
    return MODSCANNER;
  }
}
