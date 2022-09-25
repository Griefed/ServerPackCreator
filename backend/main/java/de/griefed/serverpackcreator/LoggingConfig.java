package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.utilities.common.JarUtilities;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.springframework.util.StreamUtils;

/**
 * Custom logging configuration setup to prevent incorrect log-directories when executing
 * ServerPackCreator from CLI from a completely other directory. Or possibly when using symlinks,
 * too. This class prevents the logs being written to the {@code logs}-directory inside the
 * directory from which SPC is being run from.
 *
 * @author Griefed
 */
@Plugin(name = "ServerPackCreatorConfigFactory", category = "ConfigurationFactory")
@Order(50)
public class LoggingConfig extends ConfigurationFactory {

  private final String[] SUFFIXES = new String[]{".xml"};
  private final JarUtilities JAR_UTILS = new JarUtilities();
  private final HashMap<String, String> SYSINFO = JAR_UTILS.jarInformation(LoggingConfig.class);
  private final File HOME;
  private final File LOG4J2XML;

  /**
   * Check possible home-directories for a viable {@code serverpackcreator.properties} and ceck
   * whether the {@code de.griefed.serverpackcreator.home}-property is available. If it is, then use
   * said directory to create the log4j config if it does not already exist, with the path to the
   * logs-directory being set within the aforementioned home-directory.
   *
   * @author Griefed
   */
  public LoggingConfig() {
    super();

    File props = null;
    if (new File("serverpackcreator.properties").isFile()) {

      props = new File("serverpackcreator.properties");

    } else if (new File(SYSINFO.get("jarPath"), "serverpackcreator.properties").isFile()) {

      props = new File(SYSINFO.get("jarPath"), "serverpackcreator.properties");

    } else if (new File(new File("").getAbsolutePath(), "serverpackcreator.properties").isFile()) {

      props = new File(new File("").getAbsolutePath(), "serverpackcreator.properties");

    }

    if (props != null) {

      Properties properties = new Properties();

      try (InputStream inputStream = Files.newInputStream(props.toPath())) {

        properties.load(inputStream);

        if (properties.containsKey("de.griefed.serverpackcreator.home")) {

          HOME = new File(properties.getProperty("de.griefed.serverpackcreator.home"));

        } else {
          if (new File(SYSINFO.get("jarPath")).isDirectory()) {
            HOME = new File(new File("").getAbsolutePath());
          } else {
            HOME = new File(SYSINFO.get("jarPath")).getParentFile();
          }
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    } else {
      if (new File(SYSINFO.get("jarPath")).isDirectory()) {
        HOME = new File("").getAbsoluteFile();
      } else {
        HOME = new File(SYSINFO.get("jarPath")).getParentFile();
      }
    }

    LOG4J2XML = new File(HOME, "log4j2.xml");

    if (!LOG4J2XML.isFile()) {
      try (InputStream stream = ServerPackCreator.class.getResourceAsStream("/log4j2.xml")) {

        String log4j = StreamUtils.copyToString(stream, StandardCharsets.UTF_8);

        log4j = log4j.replace(
            "<Property name=\"log-path\">logs</Property>",
            "<Property name=\"log-path\">" + new File(HOME, "logs").getAbsolutePath()
                + "</Property>");

        FileUtils.writeStringToFile(LOG4J2XML, log4j, StandardCharsets.UTF_8);

      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }

    System.setProperty("log4j2.formatMsgNoLookups", "true");
  }

  @Override
  protected String[] getSupportedTypes() {
    return SUFFIXES;
  }

  /**
   * Depending on whether this is the first run of ServerPackCreator on a users machine, the default
   * log4j2 configuration may be present at different locations. The default one is the config
   * inside the home-directory of SPC, of which we will try to setup our logging with. If said file
   * fails for whatever reason, we will try to use a config inside the directory from which SPC was
   * executed. Should that fail, too, the config from the classpath is used, to ensure we always
   * have default configs available. Should that fail, too, though, log4j is setup with its own
   * default settings.
   *
   * @param loggerContext logger context passed from log4j itself
   * @param source        configuration source passed from log4j itself. Attempts to overwrite it
   *                      are made, but if all else fails it is used to setup logging with log4js
   *                      default config.
   * @return Custom configuration with proper logs-directory set.
   * @author Griefed
   */
  @Override
  public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {

    if (LOG4J2XML.isFile()) {
      try {
        return new CustomXmlConfiguration(
            loggerContext,
            new ConfigurationSource(
                Files.newInputStream(LOG4J2XML.toPath()),
                LOG4J2XML)
        );
      } catch (IOException ex) {
        ex.printStackTrace();
      }

    } else if (new File(new File("").getAbsolutePath(), "log4j2.xml").isFile()) {
      try {
        File config = new File(new File("").getAbsolutePath(), "log4j2.xml");

        return new CustomXmlConfiguration(
            loggerContext,
            new ConfigurationSource(
                Files.newInputStream(config.toPath()),
                config)
        );
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }

    try {
      return new CustomXmlConfiguration(
          loggerContext,
          new ConfigurationSource(
              Objects.requireNonNull(LoggingConfig.class.getResourceAsStream("/log4j2.xml"))));
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    return new CustomXmlConfiguration(loggerContext, source);
  }

  /**
   * Custom XmlConfiguration to pass our custom log4j2.xml config to log4j.
   *
   * @author Griefed
   */
  @SuppressWarnings("InnerClassMayBeStatic")
  public class CustomXmlConfiguration extends XmlConfiguration {

    /**
     * Setup the XML configuration with the passed context and config source. For the config source
     * being used, {@link LoggingConfig#getConfiguration(LoggerContext, ConfigurationSource)} where
     * multiple attempts at creating a new CustomXmlConfiguration using our own log4j2.xml are made
     * before the default log4j setup is used.
     *
     * @param loggerContext logger context passed from log4j itself
     * @param configSource  configuration source passed from
     *                      {@link LoggingConfig#getConfiguration(LoggerContext,
     *                      ConfigurationSource)}.
     * @author Griefed
     */
    public CustomXmlConfiguration(LoggerContext loggerContext, ConfigurationSource configSource) {
      super(loggerContext, configSource);
    }

    /**
     * For now, all this does is call the {@link XmlConfiguration#doConfigure()}-method to setup the
     * configuration with the passed source from the constructor. Custom values and settings can be
     * set here in the future, should a need arise to do so.
     *
     * @author Griefed
     */
    @Override
    protected void doConfigure() {
      super.doConfigure();
      /*
       * Should need be we can add custom configurations here in the future. Not needed for now,
       * so all this does is ensure we use a custom log4j2.xml to setup our logging config.
       */
    }
  }
}
