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

public class Dependencies {

  public static final ServerPackCreator SERVER_PACK_CREATOR =
      new ServerPackCreator(new String[] {"--setup"});
  public static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
  public static final ApplicationProperties APPLICATIONPROPERTIES = new ApplicationProperties();
  public static final I18n I18N = new I18n(APPLICATIONPROPERTIES);
  public static final Utilities UTILITIES = new Utilities(I18N, APPLICATIONPROPERTIES);
  public static final ConfigUtilities CONFIGUTILITIES =
      new ConfigUtilities(UTILITIES, APPLICATIONPROPERTIES, OBJECT_MAPPER);
  public static final VersionMeta VERSIONMETA;
  public static final ConfigurationHandler CONFIGURATIONHANDLER;
  public static final ServerPackHandler SERVERPACKHANDLER;
  public static final ApplicationPlugins APPLICATION_PLUGINS;
  public static final ModScanner MODSCANNER =
      new ModScanner(
          new AnnotationScanner(OBJECT_MAPPER),
          new FabricScanner(OBJECT_MAPPER),
          new QuiltScanner(OBJECT_MAPPER),
          new TomlScanner(new TomlParser()));

  static {
    try {
      FileUtils.copyDirectory(
          new File("backend/test/resources/testresources/addons"), new File("plugins"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    APPLICATION_PLUGINS = new ApplicationPlugins();

    try {
      SERVER_PACK_CREATOR.run();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

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
}
