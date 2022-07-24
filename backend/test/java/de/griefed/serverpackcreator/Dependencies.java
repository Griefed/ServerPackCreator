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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Dependencies {

  private static final Logger LOG = LogManager.getLogger(Dependencies.class);

  private ServerPackCreator SERVER_PACK_CREATOR;
  private ObjectMapper OBJECT_MAPPER;
  private ApplicationProperties APPLICATIONPROPERTIES;
  private I18n I18N;
  private Utilities UTILITIES;
  private ConfigUtilities CONFIGUTILITIES;
  private VersionMeta VERSIONMETA = null;
  private ConfigurationHandler CONFIGURATIONHANDLER;
  private ServerPackHandler SERVERPACKHANDLER;
  private ApplicationPlugins APPLICATION_PLUGINS;
  private TomlParser TOMLPARSER;
  private TomlScanner TOMLSCANNER;
  private AnnotationScanner ANNOTATIONSCANNER;
  private FabricScanner FABRICSCANNER;
  private QuiltScanner QUILTSCANNER;
  private ModScanner MODSCANNER;

  private Dependencies() {

    try {
      FileUtils.copyDirectory(
          new File("backend/test/resources/testresources/addons"), new File("plugins"));
    } catch (IOException e) {
      LOG.error("Error initializing dependency.", e);
    }
    try {
      FileUtils.copyFile(
          new File("backend/test/resources/serverpackcreator.properties"),
          new File("serverpackcreator.properties"));
    } catch (IOException e) {
      LOG.error("Error initializing dependency.", e);
    }
    try {
      FileUtils.copyFile(
          new File("backend/test/resources/serverpackcreator.db"),
          new File("serverpackcreator.db"));
    } catch (IOException e) {
      LOG.error("Error initializing dependency.", e);
    }

    SERVER_PACK_CREATOR = new ServerPackCreator(new String[] {"--setup"});
    try {
      SERVER_PACK_CREATOR.run();
    } catch (IOException e) {
      LOG.error("Error initializing dependency.", e);
    }

    OBJECT_MAPPER =
        new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    APPLICATIONPROPERTIES = new ApplicationProperties();

    I18N = new I18n(APPLICATIONPROPERTIES);

    UTILITIES = new Utilities(I18N, APPLICATIONPROPERTIES);

    CONFIGUTILITIES = new ConfigUtilities(UTILITIES, APPLICATIONPROPERTIES, OBJECT_MAPPER);

    TOMLPARSER = new TomlParser();
    TOMLSCANNER = new TomlScanner(TOMLPARSER);
    ANNOTATIONSCANNER = new AnnotationScanner(OBJECT_MAPPER);
    QUILTSCANNER = new QuiltScanner(OBJECT_MAPPER);
    FABRICSCANNER = new FabricScanner(OBJECT_MAPPER);

    MODSCANNER = new ModScanner(ANNOTATIONSCANNER, FABRICSCANNER, QUILTSCANNER, TOMLSCANNER);

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
      LOG.error("Error initializing dependency.", e);
    }

    try {
      CONFIGURATIONHANDLER =
          new ConfigurationHandler(
              I18N, VERSIONMETA, APPLICATIONPROPERTIES, UTILITIES, CONFIGUTILITIES);
    } catch (IOException e) {
      LOG.error("Error initializing dependency.", e);
    }

    try {
      SERVERPACKHANDLER =
          new ServerPackHandler(
              APPLICATIONPROPERTIES, VERSIONMETA, UTILITIES, APPLICATION_PLUGINS, MODSCANNER);
    } catch (IOException e) {
      LOG.error("Error initializing dependency.", e);
    }
  }

  public static synchronized Dependencies getInstance() {
    return InstanceHolder.instance;
  }

  public synchronized ServerPackCreator SERVER_PACK_CREATOR() {
    if (SERVER_PACK_CREATOR == null) {
      this.SERVER_PACK_CREATOR = new ServerPackCreator(new String[] {"--setup"});
    }
    return SERVER_PACK_CREATOR;
  }

  public synchronized ObjectMapper OBJECT_MAPPER() {
    if (OBJECT_MAPPER == null) {
      OBJECT_MAPPER =
          new ObjectMapper()
              .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }
    return OBJECT_MAPPER;
  }

  public synchronized ApplicationProperties APPLICATIONPROPERTIES() {
    if (APPLICATIONPROPERTIES == null) {
      APPLICATIONPROPERTIES = new ApplicationProperties();
    }
    return APPLICATIONPROPERTIES;
  }

  public synchronized I18n I18N() {
    if (I18N == null) {
      I18N = new I18n(getInstance().APPLICATIONPROPERTIES());
    }
    return I18N;
  }

  public synchronized Utilities UTILITIES() {
    if (UTILITIES == null) {
      UTILITIES = new Utilities(getInstance().I18N(), getInstance().APPLICATIONPROPERTIES());
    }
    return UTILITIES;
  }

  public synchronized ConfigUtilities CONFIGUTILITIES() {
    if (CONFIGUTILITIES == null) {
      CONFIGUTILITIES =
          new ConfigUtilities(
              getInstance().UTILITIES(),
              getInstance().APPLICATIONPROPERTIES(),
              getInstance().OBJECT_MAPPER());
    }
    return CONFIGUTILITIES;
  }

  public synchronized VersionMeta VERSIONMETA() {
    if (VERSIONMETA == null) {
      try {
        VERSIONMETA =
            new VersionMeta(
                getInstance().APPLICATIONPROPERTIES().MINECRAFT_VERSION_MANIFEST_LOCATION(),
                getInstance().APPLICATIONPROPERTIES().FORGE_VERSION_MANIFEST_LOCATION(),
                getInstance().APPLICATIONPROPERTIES().FABRIC_VERSION_MANIFEST_LOCATION(),
                getInstance().APPLICATIONPROPERTIES().FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION(),
                getInstance().APPLICATIONPROPERTIES().FABRIC_INTERMEDIARIES_MANIFEST_LOCATION(),
                getInstance().APPLICATIONPROPERTIES().QUILT_VERSION_MANIFEST_LOCATION(),
                getInstance().APPLICATIONPROPERTIES().QUILT_INSTALLER_VERSION_MANIFEST_LOCATION(),
                getInstance().OBJECT_MAPPER());
      } catch (IOException e) {
        LOG.error("Error initializing dependency.", e);
      }
    }
    return VERSIONMETA;
  }

  public synchronized ConfigurationHandler CONFIGURATIONHANDLER() {
    if (CONFIGURATIONHANDLER == null) {
      try {
        CONFIGURATIONHANDLER =
            new ConfigurationHandler(
                getInstance().I18N(),
                getInstance().VERSIONMETA(),
                getInstance().APPLICATIONPROPERTIES(),
                getInstance().UTILITIES(),
                getInstance().CONFIGUTILITIES());
      } catch (IOException e) {
        LOG.error("Error initializing dependency.", e);
      }
    }
    return CONFIGURATIONHANDLER;
  }

  public synchronized ServerPackHandler SERVERPACKHANDLER() {
    if (SERVERPACKHANDLER == null) {
      try {
        SERVERPACKHANDLER =
            new ServerPackHandler(
                getInstance().APPLICATIONPROPERTIES(),
                getInstance().VERSIONMETA(),
                getInstance().UTILITIES(),
                getInstance().APPLICATION_PLUGINS(),
                getInstance().MODSCANNER());
      } catch (IOException e) {
        LOG.error("Error initializing dependency.", e);
      }
    }
    return SERVERPACKHANDLER;
  }

  public synchronized ApplicationPlugins APPLICATION_PLUGINS() {
    if (APPLICATION_PLUGINS == null) {
      APPLICATION_PLUGINS = new ApplicationPlugins();
    }
    return APPLICATION_PLUGINS;
  }

  public synchronized ModScanner MODSCANNER() {
    if (MODSCANNER == null) {
      MODSCANNER =
          new ModScanner(
              getInstance().ANNOTATIONSCANNER(),
              getInstance().FABRICSCANNER(),
              getInstance().QUILTSCANNER(),
              getInstance().TOMLSCANNER());
    }
    return MODSCANNER;
  }

  public synchronized TomlParser TOMLPARSER() {
    if (TOMLPARSER == null) {
      TOMLPARSER = new TomlParser();
    }
    return TOMLPARSER;
  }

  public synchronized TomlScanner TOMLSCANNER() {
    if (TOMLSCANNER == null) {
      TOMLSCANNER = new TomlScanner(getInstance().TOMLPARSER());
    }
    return TOMLSCANNER;
  }

  public synchronized AnnotationScanner ANNOTATIONSCANNER() {
    if (ANNOTATIONSCANNER == null) {
      ANNOTATIONSCANNER = new AnnotationScanner(getInstance().OBJECT_MAPPER());
    }
    return ANNOTATIONSCANNER;
  }

  public synchronized FabricScanner FABRICSCANNER() {
    if (FABRICSCANNER == null) {
      FABRICSCANNER = new FabricScanner(getInstance().OBJECT_MAPPER());
    }
    return FABRICSCANNER;
  }

  public synchronized QuiltScanner QUILTSCANNER() {
    if (QUILTSCANNER == null) {
      QUILTSCANNER = new QuiltScanner(getInstance().OBJECT_MAPPER());
    }
    return QUILTSCANNER;
  }

  private static final class InstanceHolder {
    static final Dependencies instance = new Dependencies();
  }
}
