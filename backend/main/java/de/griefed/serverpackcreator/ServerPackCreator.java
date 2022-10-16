/* Copyright (C) 2022  Griefed
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
package de.griefed.serverpackcreator;

import com.electronwill.nightconfig.toml.TomlParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.MigrationManager.MigrationMessage;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.i18n.IncorrectLanguageException;
import de.griefed.serverpackcreator.modscanning.AnnotationScanner;
import de.griefed.serverpackcreator.modscanning.FabricScanner;
import de.griefed.serverpackcreator.modscanning.ModScanner;
import de.griefed.serverpackcreator.modscanning.QuiltScanner;
import de.griefed.serverpackcreator.modscanning.TomlScanner;
import de.griefed.serverpackcreator.swing.ServerPackCreatorSplash;
import de.griefed.serverpackcreator.swing.ServerPackCreatorWindow;
import de.griefed.serverpackcreator.utilities.ConfigurationEditor;
import de.griefed.serverpackcreator.utilities.UpdateChecker;
import de.griefed.serverpackcreator.utilities.common.BooleanUtilities;
import de.griefed.serverpackcreator.utilities.common.FileUtilities;
import de.griefed.serverpackcreator.utilities.common.JarUtilities;
import de.griefed.serverpackcreator.utilities.common.JsonUtilities;
import de.griefed.serverpackcreator.utilities.common.ListUtilities;
import de.griefed.serverpackcreator.utilities.common.StringUtilities;
import de.griefed.serverpackcreator.utilities.common.SystemUtilities;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.utilities.common.WebUtilities;
import de.griefed.serverpackcreator.utilities.common.XmlUtilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import de.griefed.versionchecker.Update;
import java.awt.GraphicsEnvironment;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Executors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.xml.sax.SAXException;

/**
 * Main-class of ServerPackCreator. Run either via {@link #main(String[])}, or
 * {@link #getInstance(String[])}, or {@link #getInstance()} to work with ServerPackCreator. For
 * available arguments to initialize and run SPC with, check {@link Mode} and
 * {@link CommandlineParser} for how the initialization is prioritized. An instance of SPC will have
 * the base amount of class-instances available to it. If a given instance of a class is null, then
 * calling the appropriate getter will ensure a new instance is initialized first, hence the huge
 * amount of {@code synchronized}-methods.<br><br> When running as a web-service, Spring Boot will
 * read and parse the {@code serverpackcreator.properties}-file to set Spring Boot properties. So if
 * you want to change Spring Boot specific properties, that's the file to do that in.
 *
 * @author Griefed
 */
@SpringBootApplication
@EnableScheduling
public class ServerPackCreator {

  private static final Logger LOG = LogManager.getLogger(ServerPackCreator.class);
  private static final String[] SETUP = new String[]{"--setup"};
  private static volatile ServerPackCreator serverPackCreator = null;
  private final String[] ARGS;
  private final CommandlineParser COMMANDLINE_PARSER;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final I18n I18N;
  private final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
          .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());
  private BooleanUtilities booleanUtilities = null;
  private FileUtilities fileUtilities = null;
  private JarUtilities jarUtilities = null;
  private JsonUtilities jsonUtilities = null;
  private ListUtilities listUtilities = null;
  private StringUtilities stringUtilities = null;
  private SystemUtilities systemUtilities = null;
  private WebUtilities webUtilities = null;
  private DocumentBuilder documentBuilder = null;
  private XmlUtilities xmlUtilities = null;
  private Utilities utilities = null;
  private VersionMeta versionMeta = null;
  private ConfigurationHandler configurationHandler = null;
  private ApplicationAddons applicationAddons = null;
  private ServerPackHandler serverPackHandler = null;
  private ServerPackCreatorSplash serverPackCreatorSplash = null;
  private UpdateChecker updateChecker = null;
  private ModScanner modScanner = null;
  private AnnotationScanner annotationScanner = null;
  private FabricScanner fabricScanner = null;
  private QuiltScanner quiltScanner = null;
  private TomlParser tomlParser = null;
  private TomlScanner tomlScanner = null;
  private ConfigurationEditor configurationEditor = null;
  private ServerPackCreatorWindow serverPackCreatorGui = null;
  private MigrationManager migrationManager = null;
  private ConfigurableApplicationContext springBootApplicationContext = null;

  /**
   * Initialize ServerPackCreator and determine the {@link Mode} to run in from the passed entries
   * in the String-array. A new instance of SPC will initialize an instance of
   * {@link ApplicationProperties}, required for just about everything, as well as {@link I18n} for
   * localization purposes. {@link FileUtilities}, {@link SystemUtilities}, {@link ListUtilities}
   * and {@link JarUtilities} are also setup, so feel free to use them right away.
   *
   * @param args Commandline arguments with which ServerPackCreator is run. Determines which mode
   *             ServerPackCreator will enter and which locale is used. In order to see which
   *             argument results in which mode, see {@link Mode}.
   * @author Griefed
   */
  public ServerPackCreator(@NotNull String[] args) {
    ARGS = args;
    COMMANDLINE_PARSER = new CommandlineParser(args);

    if (COMMANDLINE_PARSER.propertiesFile().isPresent()) {

      APPLICATIONPROPERTIES = new ApplicationProperties(
          COMMANDLINE_PARSER.propertiesFile().get(),
          getFileUtilities(),
          getSystemUtilities(),
          getListUtilities(),
          getJarUtilities());

    } else {

      APPLICATIONPROPERTIES = new ApplicationProperties(
          getFileUtilities(),
          getSystemUtilities(),
          getListUtilities(),
          getJarUtilities());
    }

    if (COMMANDLINE_PARSER.getLanguageToUse().isPresent()) {

      APPLICATIONPROPERTIES.writeLocaleToFile(COMMANDLINE_PARSER.LANG);

      I18N = new I18n(
          APPLICATIONPROPERTIES,
          COMMANDLINE_PARSER.LANG);

    } else {

      I18N = new I18n(APPLICATIONPROPERTIES);
    }
  }

  /**
   * This instances common file utilities used across ServerPackCreator.
   *
   * @return Common file utilities used across ServerPackCreator.
   * @author Griefed
   */
  public synchronized @NotNull FileUtilities getFileUtilities() {
    if (fileUtilities == null) {
      fileUtilities = new FileUtilities();
    }
    return fileUtilities;
  }

  /**
   * This instances common system utilities used across ServerPackCreator.
   *
   * @return Common system utilities used across ServerPackCreator.
   * @author Griefed
   */
  public synchronized @NotNull SystemUtilities getSystemUtilities() {
    if (systemUtilities == null) {
      systemUtilities = new SystemUtilities();
    }
    return systemUtilities;
  }

  /**
   * This instances common list utilities used across ServerPackCreator.
   *
   * @return Common list utilities used across ServerPackCreator.
   * @author Griefed
   */
  public synchronized @NotNull ListUtilities getListUtilities() {
    if (listUtilities == null) {
      listUtilities = new ListUtilities();
    }
    return listUtilities;
  }

  /**
   * This instances common JAR-utilities used across ServerPackCreator.
   *
   * @return Common JAR-utilities used across ServerPackCreator.
   * @author Griefed
   */
  public synchronized @NotNull JarUtilities getJarUtilities() {
    if (jarUtilities == null) {
      jarUtilities = new JarUtilities();
    }
    return jarUtilities;
  }

  /**
   * Acquire an instance of ServerPackCreator using the {@code --setup}-argument so a prepared
   * environment is present after acquiring the instance. If a new instance of ServerPackCreator is
   * created as the result of calling this method, then the setup is run to ensure a properly
   * prepared environment, otherwise the already existing instance of ServerPackCreator is returned,
   * allowing you to do your operations.
   *
   * @return ServerPackCreator-instance using the {@code --setup}-argument, or the already existing
   * instance, if one was initialized already.
   * @author Griefed
   */
  public synchronized static @NotNull ServerPackCreator getInstance() {
    return getInstance(SETUP);
  }

  /**
   * Acquire an instance of ServerPackCreator using the specified argument. If a new instance of
   * ServerPackCreator is created as the result of calling this method, then the setup is run to
   * ensure a properly prepared environment. Afterwards, the instance of ServerPackCreator is
   * returned.
   *
   * @param args Arguments with which to instantiate ServerPackCreator. Possible arguments can be
   *             found at {@link Mode}.
   * @return ServerPackCreator-instance with the specified argument.
   * @author Griefed
   */
  public synchronized static @NotNull ServerPackCreator getInstance(String[] args) {
    if (serverPackCreator == null) {
      serverPackCreator = new ServerPackCreator(args);
      try {
        serverPackCreator.run(Mode.SETUP);
      } catch (IOException | ParserConfigurationException | SAXException ex) {
        LOG.error("Something went horribly wrong trying to run the ServerPackCreator setup.", ex);
      }
    }
    return serverPackCreator;
  }

  /**
   * Initialize ServerPackCreator with the passed commandline-arguments and run.
   *
   * <p>For a list of available commandline arguments, check out {@link
   * ServerPackCreator.Mode}
   *
   * @param args Commandline arguments with which ServerPackCreator is run. Determines which mode
   *             ServerPackCreator will enter and which locale is used.
   * @throws ParserConfigurationException indicates a serious configuration error.
   * @throws IOException                  if any IO errors occur.
   * @throws SAXException                 if any parse errors occur.
   * @author Griefed
   */
  public static void main(String[] args)
      throws IOException, ParserConfigurationException, SAXException {
    serverPackCreator = new ServerPackCreator(args);
    serverPackCreator.run();
  }

  /**
   * Run the ServerPackCreator webservice and provide Spring Boot with arguments.
   *
   * @param args Arguments passed from invocation in {@link #main(String[])}.
   * @author Griefed
   */
  public synchronized void web(String[] args) {

    String[] springArgs = new String[args.length + 1];
    System.arraycopy(args, 0, springArgs, 0, args.length);

    springArgs[springArgs.length - 1] = "--spring.config.location="
        + "classpath:application.properties,"
        + "classpath:serverpackcreator.properties,"
        + "file:" + APPLICATIONPROPERTIES.serverPackCreatorPropertiesFile().getAbsolutePath() + ","
        + "optional:file:./serverpackcreator.properties";

    LOG.debug("Running webservice with args:" + Arrays.toString(springArgs));

    LOG.debug(
        "Application name: " + getSpringBootApplicationContext(springArgs).getApplicationName());

    LOG.debug("Property sources:");
    springBootApplicationContext.getEnvironment().getPropertySources()
                                .forEach(property -> LOG.debug(
                                    "    " + property.getName() + ": " + property.getSource()));

    LOG.debug("System properties:");
    for (Entry<String, Object> entry : springBootApplicationContext.getEnvironment()
                                                                   .getSystemProperties()
                                                                   .entrySet()) {
      LOG.debug("    Key: " + entry.getKey() + " - Value: " + entry.getValue());
    }
    LOG.debug("System environment:");
    for (Map.Entry<String, Object> entry : springBootApplicationContext.getEnvironment()
                                                                       .getSystemEnvironment()
                                                                       .entrySet()) {
      LOG.debug("    Key: " + entry.getKey() + " - Value: " + entry.getValue());
    }
  }

  /**
   * This instances arguments with which ServerPackCreator was started.
   *
   * @return All arguments with which ServerPackCreator was started.
   * @author Griefed
   */
  public @NotNull String @NotNull [] getArgs() {
    return ARGS;
  }

  /**
   * This instances internationalization used in the GUI and error messages displayed in the very
   * same.
   *
   * @return Instance of ServerPackCreators Internationalization used in this instance.
   * @author Griefed
   */
  public @NotNull I18n getI18n() {
    return I18N;
  }

  /**
   * This instances JSON-ObjectMapper used across ServerPackCreator with which this instance was
   * initialized. By default, the ObjectMapper used across ServerPackCreator has the following
   * features set:
   * <ul>
   *   <li>disabled: {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}</li>
   *   <li>enabled: {@link DeserializationFeature#ACCEPT_SINGLE_VALUE_AS_ARRAY}</li>
   *   <li>enabled: {@link JsonReadFeature#ALLOW_UNESCAPED_CONTROL_CHARS}</li>
   * </ul>
   *
   * @return Json-ObjectMapper to parse and read JSON.
   * @author Griefed
   */
  public @NotNull ObjectMapper getObjectMapper() {
    return OBJECT_MAPPER;
  }

  /**
   * This instances settings used across ServerPackCreator, such as the working-directories, files
   * and other settings.
   *
   * @return ApplicationProperties used across this ServerPackCreator-instance.
   * @author Griefed
   */
  public @NotNull ApplicationProperties getApplicationProperties() {
    return APPLICATIONPROPERTIES;
  }

  /**
   * This instances common boolean utilities used across ServerPackCreator.
   *
   * @return Common boolean utilities used across ServerPackCreator.
   * @author Griefed
   */
  public synchronized @NotNull BooleanUtilities getBooleanUtilities() {
    if (booleanUtilities == null) {
      booleanUtilities = new BooleanUtilities();
    }
    return booleanUtilities;
  }

  /**
   * This instances common JSON utilities used across ServerPackCreator.
   *
   * @return Common JSON utilities used across ServerPackCreator.
   * @author Griefed
   */
  public synchronized @NotNull JsonUtilities getJsonUtilities() {
    if (jsonUtilities == null) {
      jsonUtilities = new JsonUtilities(OBJECT_MAPPER);
    }
    return jsonUtilities;
  }

  /**
   * This instances common XML utilities used across ServerPackCreator.
   *
   * @return Common XML utilities used across ServerPackCreator.
   * @author Griefed
   */
  public synchronized @NotNull XmlUtilities getXmlUtilities() {
    if (xmlUtilities == null) {
      xmlUtilities = new XmlUtilities(getDocumentBuilder());
    }
    return xmlUtilities;
  }

  /**
   * This instances DocumentBuilder for working with XML-data.
   *
   * @return DocumentBuilder for working with XML.
   * @author Griefed
   */
  public synchronized @Nullable DocumentBuilder getDocumentBuilder() {
    if (documentBuilder == null) {
      try {
        documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      } catch (ParserConfigurationException e) {
        throw new RuntimeException(e);
      }
    }
    return documentBuilder;
  }

  /**
   * This instances common String utilities used across ServerPackCreator.
   *
   * @return Common String utilities used across ServerPackCreator.
   * @author Griefed
   */
  public synchronized @NotNull StringUtilities getStringUtilities() {
    if (stringUtilities == null) {
      stringUtilities = new StringUtilities();
    }
    return stringUtilities;
  }

  /**
   * This instances common web utilities used across ServerPackCreator.
   *
   * @return Common web utilities used across ServerPackCreator.
   * @author Griefed
   */
  public synchronized @NotNull WebUtilities getWebUtilities() {
    if (webUtilities == null) {
      webUtilities = new WebUtilities(APPLICATIONPROPERTIES);
    }
    return webUtilities;
  }

  /**
   * This instances collection of common utilities used across ServerPackCreator.
   *
   * @return Collection of common utilities used across ServerPackCreator.
   * @author Griefed
   */
  public synchronized @NotNull Utilities getUtilities() {
    if (utilities == null) {
      utilities = new Utilities(
          getBooleanUtilities(),
          getFileUtilities(),
          getJarUtilities(),
          getListUtilities(),
          getStringUtilities(),
          getSystemUtilities(),
          getWebUtilities(),
          getJsonUtilities(),
          getXmlUtilities()
      );
    }
    return utilities;
  }

  /**
   * This instances MigrationManager responsible for checking and executing any required
   * migration-steps between version upgrades.
   *
   * @return MigrationManager responsible for checking and executing any required migration-steps.
   * @author Griefed
   */
  public synchronized @NotNull MigrationManager getMigrationManager() {
    if (migrationManager == null) {
      migrationManager = new MigrationManager(APPLICATIONPROPERTIES, I18N);
    }
    return migrationManager;
  }

  /**
   * This instances version meta used for checking version-correctness of Minecraft and supported
   * modloaders, as well as gathering information about Minecraft servers and modloader installers.
   *
   * @return Meta used for checking version-correctness of Minecraft and supported modloaders.
   * @throws IOException                  When manifests couldn't be parsed.
   * @throws ParserConfigurationException When xml-manifests couldn't be read.
   * @throws SAXException                 When xml-manifests couldn't be read.
   * @author Griefed
   */
  public synchronized @NotNull VersionMeta getVersionMeta()
      throws IOException, ParserConfigurationException, SAXException {
    if (versionMeta == null) {
      versionMeta =
          new VersionMeta(
              APPLICATIONPROPERTIES.minecraftVersionManifest(),
              APPLICATIONPROPERTIES.forgeVersionManifest(),
              APPLICATIONPROPERTIES.fabricVersionManifest(),
              APPLICATIONPROPERTIES.fabricInstallerManifest(),
              APPLICATIONPROPERTIES.fabricIntermediariesManifest(),
              APPLICATIONPROPERTIES.quiltVersionManifest(),
              APPLICATIONPROPERTIES.quiltInstallerManifest(),
              APPLICATIONPROPERTIES.legacyFabricGameManifest(),
              APPLICATIONPROPERTIES.legacyFabricLoaderManifest(),
              APPLICATIONPROPERTIES.legacyFabricInstallerManifest(),
              OBJECT_MAPPER,
              getUtilities(),
              APPLICATIONPROPERTIES);
    }
    return versionMeta;
  }

  /**
   * This instances ConfigurationHandler for checking a given {@link ConfigurationModel} for
   * validity, so a server pack can safely be created from it.
   *
   * @return Handler for config checking.
   * @throws IOException                  When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occured during the parsing of a manifest.
   * @throws ParserConfigurationException When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occured during the parsing of a manifest.
   * @throws SAXException                 When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occured during the parsing of a manifest.
   * @author Griefed
   */
  public synchronized @NotNull ConfigurationHandler getConfigurationHandler()
      throws IOException, ParserConfigurationException, SAXException {

    if (configurationHandler == null) {

      configurationHandler =
          new ConfigurationHandler(
              I18N,
              getVersionMeta(),
              APPLICATIONPROPERTIES,
              getUtilities(),
              getApplicationAddons());
    }
    return configurationHandler;
  }

  /**
   * This instances addon manager for ServerPackCreator-addons, if any are installed. This gives you
   * access to the available extensions, should any be available in your instance of
   * ServerPackCreator.
   *
   * @return Addon manager for ServerPackCreator-addons, if any are installed.
   * @throws IOException                  When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws ParserConfigurationException When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws SAXException                 When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @author Griefed
   */
  public synchronized @NotNull ApplicationAddons getApplicationAddons()
      throws IOException, ParserConfigurationException, SAXException {

    if (applicationAddons == null) {

      applicationAddons = new ApplicationAddons(
          getTomlParser(),
          APPLICATIONPROPERTIES,
          getVersionMeta(),
          getUtilities());
    }
    return applicationAddons;
  }

  /**
   * This instances ServerPackHandler used to turn a {@link ConfigurationModel} into a server pack.
   *
   * @return The ServerPackHandler with which config models can be used to create server packs.
   * @throws IOException                  When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws ParserConfigurationException When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws SAXException                 When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   */
  public synchronized @NotNull ServerPackHandler getServerPackHandler()
      throws IOException, ParserConfigurationException, SAXException {

    if (serverPackHandler == null) {

      serverPackHandler =
          new ServerPackHandler(
              APPLICATIONPROPERTIES,
              getVersionMeta(),
              getUtilities(),
              getApplicationAddons(),
              getModScanner());
    }
    return serverPackHandler;
  }

  /**
   * Splash screen displayed during the boot up of ServerPackCreator if a graphical environment is
   * supported and SPC is started in GUI-mode.
   *
   * @return Boot splash screen displayed during startup.
   * @author Griefed
   */
  public synchronized @NotNull ServerPackCreatorSplash getServerPackCreatorSplash() {
    if (GraphicsEnvironment.isHeadless()) {
      throw new RuntimeException("Graphical environment not supported!");
    }

    if (serverPackCreatorSplash == null) {

      serverPackCreatorSplash = new ServerPackCreatorSplash(
          APPLICATIONPROPERTIES.serverPackCreatorVersion());
    }
    return serverPackCreatorSplash;
  }

  /**
   * This instances update checker to inform the user about any potentially available update,
   * including links to said update, if any.
   *
   * @return This instances update checker to perform update checks and information acquirement.
   * @author Griefed
   */
  public synchronized @NotNull UpdateChecker getUpdateChecker() {
    if (updateChecker == null) {
      updateChecker = new UpdateChecker();
    }
    return updateChecker;
  }

  /**
   * This instances modscanner to determine the sideness of a given Forge, Fabric, LegacyFabric or
   * Quilt mod.
   *
   * @return Modscanner to determine the sideness of a given Forge, Fabric, LegacyFabric or Quilt
   * mod.
   * @author Griefed
   */
  public synchronized @NotNull ModScanner getModScanner() {
    if (modScanner == null) {

      modScanner = new ModScanner(
          getAnnotationScanner(),
          getFabricScanner(),
          getQuiltScanner(),
          getTomlScanner());
    }
    return modScanner;
  }

  /**
   * This instances annotation scanner used to determine the sideness of Forge mods for Minecraft
   * 1.12.2 and older.
   *
   * @return Annotation scanner used to determine the sideness of Forge mods for Minecraft 1.12.2
   * and older.
   * @author Griefed
   */
  public synchronized @NotNull AnnotationScanner getAnnotationScanner() {
    if (annotationScanner == null) {

      annotationScanner = new AnnotationScanner(
          OBJECT_MAPPER,
          getUtilities());
    }
    return annotationScanner;
  }

  /**
   * This instances scanner to determine the sideness of Fabric mods.
   *
   * @return Scanner to determine the sideness of Fabric mods.
   * @author Griefed
   */
  public synchronized @NotNull FabricScanner getFabricScanner() {
    if (fabricScanner == null) {

      fabricScanner = new FabricScanner(
          OBJECT_MAPPER,
          getUtilities());
    }
    return fabricScanner;
  }

  /**
   * This instances scanner to determine the sideness of Quilt mods.
   *
   * @return Scanner to determine the sideness of Quilt mods.
   * @author Griefed
   */
  public synchronized @NotNull QuiltScanner getQuiltScanner() {
    if (quiltScanner == null) {

      quiltScanner = new QuiltScanner(
          OBJECT_MAPPER,
          getUtilities());
    }
    return quiltScanner;
  }

  /**
   * This instances toml parser to read and parse various {@code .toml}-files during modscanning,
   * addon- and extension config loading and provisioning, serverpackcreator.conf reading and more.
   *
   * @return Toml parser to read and parse {@code .toml}-files.
   * @author Griefed
   */
  public synchronized @NotNull TomlParser getTomlParser() {
    if (tomlParser == null) {
      tomlParser = new TomlParser();
    }
    return tomlParser;
  }

  /**
   * This instances toml scanner to determine the sideness of Forge mods for Minecraft 1.13.x and
   * newer.
   *
   * @return Scanner to determine the sideness of Forge mods for Minecraft 1.13.x and newer.
   * @author Griefed
   */
  public synchronized @NotNull TomlScanner getTomlScanner() {
    if (tomlScanner == null) {
      tomlScanner = new TomlScanner(getTomlParser());
    }
    return tomlScanner;
  }

  /**
   * This instances configuration editor used when running in {@link Mode#CLI}. Bear in mind that
   * the CLI config editor only provides limited functionality.
   *
   * @return Config editor to load, edit and save serverpackcreator.conf-files from the CLI.
   * @throws IOException                  When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws ParserConfigurationException When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws SAXException                 When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @author Griefed
   */
  public synchronized @NotNull ConfigurationEditor getConfigurationEditor()
      throws IOException, ParserConfigurationException, SAXException {

    if (configurationEditor == null) {

      configurationEditor = new ConfigurationEditor(
          getConfigurationHandler(),
          APPLICATIONPROPERTIES,
          getUtilities(),
          getVersionMeta());
    }
    return configurationEditor;
  }

  /**
   * This instances frame holding the GUI allowing the user to run and configure their server
   * packs.
   *
   * @return Frame holding the GUI allowing the user to run and configure their server packs.
   * @throws IOException                  When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws ParserConfigurationException When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws SAXException                 When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @author Griefed
   */
  public synchronized @NotNull ServerPackCreatorWindow getServerPackCreatorGui()
      throws IOException, ParserConfigurationException, SAXException {

    if (GraphicsEnvironment.isHeadless()) {
      throw new RuntimeException("Graphical environment not supported!");
    }

    if (serverPackCreatorGui == null) {

      serverPackCreatorGui = new ServerPackCreatorWindow(
          I18N,
          getConfigurationHandler(),
          getServerPackHandler(),
          APPLICATIONPROPERTIES,
          getVersionMeta(),
          getUtilities(),
          getUpdateChecker(),
          getServerPackCreatorSplash(),
          getApplicationAddons(),
          getMigrationManager().getMigrationMessages());
    }
    return serverPackCreatorGui;
  }

  /**
   * This instances application context when running as a webservice. When no instance of the Spring
   * Boot application context is available yet, it will be created and the Spring Boot application
   * will be started with the given arguments.
   *
   * @param args CLI arguments to pass to Spring Boot when it has not yet been started.
   * @return Application context of Spring Boot.
   * @author Griefed
   */
  public synchronized @NotNull ConfigurableApplicationContext getSpringBootApplicationContext(
      @NotNull String @NotNull [] args) {
    if (springBootApplicationContext == null) {
      springBootApplicationContext = SpringApplication.run(ServerPackCreator.class, args);
    }
    return springBootApplicationContext;
  }

  /**
   * Run ServerPackCreator with the mode acquired from {@link CommandlineParser}.
   *
   * @throws IOException                  When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws ParserConfigurationException When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws SAXException                 When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @author Griefed
   */
  public synchronized void run() throws IOException, ParserConfigurationException, SAXException {
    run(COMMANDLINE_PARSER.getModeToRunIn());
  }

  /**
   * Run ServerPackCreator in a specific {@link Mode}.
   *
   * @param modeToRunIn Mode to run in.
   * @throws IOException                  When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws ParserConfigurationException When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws SAXException                 When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @author Griefed
   */
  public synchronized void run(@NotNull Mode modeToRunIn)
      throws IOException, ParserConfigurationException, SAXException {

    switch (modeToRunIn) {
      case HELP:
        printHelp();
        continuedRunOptions();
        break;

      case UPDATE:
        updateCheck(false);
        continuedRunOptions();
        break;

      case WEB:
        stageOne();
        stageFour();
        checkDatabase();
        web(ARGS);
        break;

      case CGEN:
        stageOne();
        stageTwo();
        runConfigurationEditor();
        continuedRunOptions();
        break;

      case CLI:
        stageOne();
        stageTwo();
        stageThree();
        runHeadless();
        break;

      case GUI:
        showSplashScreen();
        stageOne();

        getServerPackCreatorSplash().update(20);
        stageTwo();

        getServerPackCreatorSplash().update(40);
        stageThree();

        getServerPackCreatorSplash().update(60);
        Executors.newSingleThreadExecutor().execute(this::stageFour);

        getServerPackCreatorSplash().update(80);
        runGui();

        break;

      case SETUP:
        stageOne();
        stageTwo();
        stageThree();
        LOG.info("Setup completed.");

      case EXIT:
      default:
        LOG.debug("Exiting...");
    }
  }

  /**
   * Stage one of starting ServerPackCreator.
   * <p>
   * Creates and prepares the environment for ServerPackCreator to run by creating required
   * directories and copying required files from the JAR-file to the filesystem. Some of these files
   * can and should be edited by a given user, others however, not.
   *
   * <ul>
   *   <li>Checks the read- and write-permissions of ServerPackCreators base-directory. See {@link ApplicationProperties#homeDirectory()}.</li>
   *   <li>Copies the {@code README.md} from the JAR to the home-directory.</li>
   *   <li>Copies the {@code HELP.md} from the JAR to the home-directory.</li>
   *   <li>Copies the {@code CHANGELOG.md} from the JAR to the home-directory.</li>
   *   <li>Copies the {@code LICENSE} from the JAR to the home-directory.</li>
   *   <li>Copies the default localization properties to the {@link ApplicationProperties#langDirectory()}.</li>
   *   <li>Copies the fallback version-manifests to the {@link ApplicationProperties#manifestsDirectory()}.</li>
   *   <li>Creates default directories:</li>
   *   <ul>
   *     <li>{@link ApplicationProperties#serverFilesDirectory()}</li>
   *     <li>{@link ApplicationProperties#workDirectory()}</li>
   *     <li>{@link ApplicationProperties#tempDirectory()}</li>
   *     <li>{@link ApplicationProperties#modpacksDirectory()}</li>
   *     <li>{@link ApplicationProperties#serverPacksDirectory()}</li>
   *     <li>{@link ApplicationProperties#addonsDirectory()}</li>
   *     <li>{@link ApplicationProperties#addonConfigsDirectory()}</li>
   *   </ul>
   *   <li>Example {@code disabled.txt}-file in {@link ApplicationProperties#addonsDirectory()}.</li>
   *   <li>Creates an empty {@code serverpackcreator.conf}, if ServerPackCreators mode is not {@link Mode#CLI} or {@link Mode#CGEN}.</li>
   *   <li>Creates the default {@code server.properties} if it doesn't exist.</li>
   *   <li>Creates the default {@code server-icon.png} if it doesn't exist.</li>
   *   <li>Creates the default PowerShell and Shell script templates or overwrites them if they already exist. </li>
   *   <li>Determines whether this instance of ServerPackCreator was updated from a previous version.
   *   <br>If an update was detected, and migrations are available for any of the steps of the update,
   *   <br>they are executed, thus ensuring users are safe to update their instances.</li>
   *   <li>Writes ServerPackCreator and system information to the console and logs, important for error reporting and debugging.</li>
   * </ul>
   *
   * @author Griefed
   */
  private void stageOne() {

    System.setProperty("file.encoding", StandardCharsets.UTF_8.name());

    if (!getUtilities().FileUtils().checkPermissions(APPLICATIONPROPERTIES.getJarFolder())) {

      LOG.error("One or more file or directory has no read- or write-permission."
                    + " This may lead to corrupted server packs!"
                    + " Check the permissions of the ServerPackCreator base directory!");
    }

    getUtilities().JarUtils().copyFileFromJar(
        "README.md",
        true,
        ServerPackCreator.class,
        APPLICATIONPROPERTIES.homeDirectory().toString());

    getUtilities().JarUtils().copyFileFromJar(
        "HELP.md",
        true,
        ServerPackCreator.class,
        APPLICATIONPROPERTIES.homeDirectory().toString());

    getUtilities().JarUtils().copyFileFromJar(
        "CHANGELOG.md",
        true,
        ServerPackCreator.class,
        APPLICATIONPROPERTIES.homeDirectory().toString());

    getUtilities().JarUtils().copyFileFromJar(
        "LICENSE",
        true,
        ServerPackCreator.class,
        APPLICATIONPROPERTIES.homeDirectory().toString());

    String prefix;
    String source;
    try {

      prefix = "BOOT-INF/classes";
      source = "/de/griefed/resources/lang";

      if (APPLICATIONPROPERTIES.isExe()) {
        prefix = "";
        source = "de/griefed/resources/lang";
      }

      getUtilities().JarUtils().copyFolderFromJar(
          ServerPackCreator.class,
          source,
          APPLICATIONPROPERTIES.langDirectory().toString(),
          prefix,
          "properties");

    } catch (IOException ex) {
      LOG.error("Error copying \"/de/griefed/resources/lang\" from the JAR-file.");
    }

    try {

      prefix = "BOOT-INF/classes";
      source = "/de/griefed/resources/manifests";

      if (APPLICATIONPROPERTIES.isExe()) {
        prefix = "";
        source = "de/griefed/resources/manifests";
      }

      getUtilities().JarUtils().copyFolderFromJar(
          ServerPackCreator.class,
          source,
          APPLICATIONPROPERTIES.manifestsDirectory().toString(),
          prefix,
          "xml|json");

    } catch (IOException ex) {
      LOG.error("Error copying \"/de/griefed/resources/manifests\" from the JAR-file.");
    }

    getUtilities().FileUtils().createDirectories(
        Paths.get(APPLICATIONPROPERTIES.serverFilesDirectory().toString()));

    getUtilities().FileUtils().createDirectories(
        Paths.get(APPLICATIONPROPERTIES.workDirectory().toString()));

    getUtilities().FileUtils().createDirectories(
        Paths.get(APPLICATIONPROPERTIES.tempDirectory().toString()));

    getUtilities().FileUtils().createDirectories(
        Paths.get(APPLICATIONPROPERTIES.modpacksDirectory().toString()));

    getUtilities().FileUtils().createDirectories(
        Paths.get(APPLICATIONPROPERTIES.serverPacksDirectory().toString()));

    getUtilities().FileUtils().createDirectories(
        Paths.get(APPLICATIONPROPERTIES.addonsDirectory().toString()));

    getUtilities().FileUtils().createDirectories(
        Paths.get(APPLICATIONPROPERTIES.addonConfigsDirectory().toString()));

    if (!new File(APPLICATIONPROPERTIES.addonsDirectory(), "disabled.txt").isFile()) {
      try (BufferedWriter writer =
          new BufferedWriter(
              new FileWriter(
                  new File(
                      APPLICATIONPROPERTIES.addonsDirectory(),
                      "disabled.txt")))
      ) {

        writer.write("########################################\n");
        writer.write("#...Load all plugins except these......#\n");
        writer.write("#...Add one plugin-id per line.........#\n");
        writer.write("########################################\n");
        writer.write("#example-plugin\n");

      } catch (IOException ex) {
        LOG.error("Error generating disable.txt in the plugins directory.", ex);
      }
    }

    boolean config = checkForConfig();

    boolean serverProperties = checkServerFilesFile(
        APPLICATIONPROPERTIES.defaultServerProperties());

    boolean serverIcon = checkServerFilesFile(
        APPLICATIONPROPERTIES.defaultServerIcon());

    overwriteServerFilesFile(APPLICATIONPROPERTIES.defaultShellTemplate());
    overwriteServerFilesFile(APPLICATIONPROPERTIES.defaultPowershellTemplate());

    if (config || serverProperties || serverIcon) {

      LOG.warn("#################################################################");
      LOG.warn("#.............ONE OR MORE DEFAULT FILE(S) GENERATED.............#");
      LOG.warn("#..CHECK THE LOGS TO FIND OUT WHICH FILE(S) WAS/WERE GENERATED..#");
      LOG.warn("#...............CUSTOMIZE THEM BEFORE CONTINUING!...............#");
      LOG.warn("#################################################################");

    } else {
      LOG.info("Setup completed.");
    }

    getMigrationManager().migrate();

    for (MigrationMessage message : getMigrationManager().getMigrationMessages()) {
      for (String part : message.get().split("\n")) {
        LOG.info(part);
      }
      LOG.info("");
    }

    // Print system information to console and logs.
    LOG.debug("Gathering system information to include in log to make debugging easier.");

    if (APPLICATIONPROPERTIES.serverPackCreatorVersion().matches(".*(alpha|beta|dev).*")) {

      LOG.debug("Warning user about possible data loss.");
      LOG.warn("################################################################");
      LOG.warn("#.............ALPHA | BETA | DEV VERSION DETECTED..............#");
      LOG.warn("#.............THESE VERSIONS ARE WORK IN PROGRESS!.............#");
      LOG.warn("#..USE AT YOUR OWN RISK! BE AWARE THAT DATA LOSS IS POSSIBLE!..#");
      LOG.warn("#........I WILL NOT BE HELD RESPONSIBLE FOR DATA LOSS!.........#");
      LOG.warn("#....................YOU HAVE BEEN WARNED!.....................#");
      LOG.warn("################################################################");
    }
    LOG.info("SYSTEM AND SPC INFORMATION:");
    LOG.info("ServerPackCreator version: " + APPLICATIONPROPERTIES.serverPackCreatorVersion());
    LOG.info("ServerPackCreator home:    " + APPLICATIONPROPERTIES.homeDirectory());
    LOG.info("JAR Folder:                " + APPLICATIONPROPERTIES.getJarFolder());
    LOG.info("JAR Path:                  " + APPLICATIONPROPERTIES.getJarFile());
    LOG.info("JAR Name:                  " + APPLICATIONPROPERTIES.getJarName());
    LOG.info("Java version:              " + APPLICATIONPROPERTIES.getJavaVersion());
    LOG.info("OS architecture:           " + APPLICATIONPROPERTIES.getOSArch());
    LOG.info("OS name:                   " + APPLICATIONPROPERTIES.getOSName());
    LOG.info("OS version:                " + APPLICATIONPROPERTIES.getOSVersion());
    LOG.info("Include this information when reporting an issue on GitHub.");
  }

  /**
   * Initialize {@link VersionMeta}, {@link ConfigurationHandler}.
   *
   * @throws IOException                  When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws ParserConfigurationException When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws SAXException                 When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @author Griefed
   */
  private void stageTwo() throws IOException, ParserConfigurationException, SAXException {
    getVersionMeta();
    getConfigurationHandler();
  }

  /**
   * Initialize {@link ApplicationAddons}, {@link ModScanner} (consisting of {@link TomlParser},
   * {@link AnnotationScanner}, {@link FabricScanner},{@link TomlScanner},{@link QuiltScanner}),
   * {@link ServerPackHandler} and {@link UpdateChecker}.
   *
   * @throws IOException                  When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws ParserConfigurationException When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws SAXException                 When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @author Griefed
   */
  private void stageThree() throws IOException, ParserConfigurationException, SAXException {
    getApplicationAddons();
    getAnnotationScanner();
    getFabricScanner();
    getQuiltScanner();
    getTomlParser();
    getTomlScanner();
    getModScanner();
    getServerPackHandler();
    getUpdateChecker();
  }

  /**
   * Initialize our FileWatcher to ensure that vital files get restored, should they be deleted
   * whilst ServerPackCreator is running.
   * <p>
   * Files which will be restored are:
   * <ul>
   *   <li>serverpackcreator.properties</li>
   *   <li>Default server.properties</li>
   *   <li>Default server-icon.png</li>
   *   <li>Default PowerShell script template</li>
   *   <li>Default Shell script template</li>
   * </ul>
   *
   * @author Griefed
   */
  private void stageFour() {

    LOG.debug("Setting up FileWatcher...");

    FileAlterationObserver fileAlterationObserver = new FileAlterationObserver(
        APPLICATIONPROPERTIES.homeDirectory());

    FileAlterationListener fileAlterationListener =
        new FileAlterationListener() {
          @Override
          public void onStart(FileAlterationObserver observer) {
          }

          @Override
          public void onDirectoryCreate(File directory) {
          }

          @Override
          public void onDirectoryChange(File directory) {
          }

          @Override
          public void onDirectoryDelete(File directory) {
          }

          @Override
          public void onFileCreate(File file) {
          }

          @Override
          public void onFileChange(File file) {
          }

          @Override
          public void onFileDelete(File file) {
            if (!file.toString()
                     .contains(APPLICATIONPROPERTIES.serverPacksDirectory().toString())
                && !file.toString()
                        .contains(APPLICATIONPROPERTIES.modpacksDirectory().toString())) {

              if (check(file, APPLICATIONPROPERTIES.serverPackCreatorPropertiesFile())) {

                createFile(APPLICATIONPROPERTIES.serverPackCreatorPropertiesFile());
                APPLICATIONPROPERTIES.loadProperties();
                LOG.info("Restored serverpackcreator.properties and loaded defaults.");

              } else if (check(file, APPLICATIONPROPERTIES.defaultServerProperties())) {

                checkServerFilesFile(APPLICATIONPROPERTIES.defaultServerProperties());
                LOG.info("Restored default server.properties.");

              } else if (check(file, APPLICATIONPROPERTIES.defaultServerIcon())) {

                checkServerFilesFile(APPLICATIONPROPERTIES.defaultServerIcon());
                LOG.info("Restored default server-icon.png.");

              } else if (check(file, APPLICATIONPROPERTIES.defaultShellTemplate())) {

                checkServerFilesFile(APPLICATIONPROPERTIES.defaultShellTemplate());
                LOG.info("Restored default_template.sh.");

              } else if (check(file, APPLICATIONPROPERTIES.defaultPowershellTemplate())) {

                checkServerFilesFile(APPLICATIONPROPERTIES.defaultPowershellTemplate());
                LOG.info("Restored default_template.ps1.");
              }
            }
          }

          @Override
          public void onStop(FileAlterationObserver observer) {
          }

          private boolean check(File watched,
                                File toCreate) {
            return watched.getName().equals(toCreate.getName());
          }

          private void createFile(File toCreate) {

            getUtilities().JarUtils()
                          .copyFileFromJar(toCreate.getName(), ServerPackCreator.class,
                                           toCreate.getParent());
          }
        };

    fileAlterationObserver.addListener(fileAlterationListener);
    FileAlterationMonitor fileAlterationMonitor = new FileAlterationMonitor(1000);
    fileAlterationMonitor.addObserver(fileAlterationObserver);

    try {
      fileAlterationMonitor.start();
    } catch (Exception ex) {
      LOG.error("Error starting the FileWatcher Monitor.", ex);
    }

    LOG.debug("File-watcher started...");
  }

  /**
   * Show the splashscreen of ServerPackCreator, indicating that things are loading and
   * ServerPackCreator is starting.
   *
   * @author Griefed
   */
  private void showSplashScreen() {
    getServerPackCreatorSplash();
  }

  /**
   * Run ServerPackCreator with our GUI.
   *
   * @throws IOException if the {@link VersionMeta} could not be instantiated.
   * @author Griefed
   */
  private void runGui() throws IOException, ParserConfigurationException, SAXException {
    getServerPackCreatorGui().mainGUI();
  }

  /**
   * Offer the user to continue using ServerPackCreator when running in {@link Mode#HELP},
   * {@link Mode#UPDATE} or {@link Mode#CGEN}.
   *
   * @throws IOException                  When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws ParserConfigurationException When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws SAXException                 When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @author Griefed
   */
  private void continuedRunOptions()
      throws IOException, ParserConfigurationException, SAXException {

    printMenu();

    Scanner scanner = new Scanner(System.in);
    int selection;

    do {

      try {

        selection = scanner.nextInt();

        if (selection == 7 && GraphicsEnvironment.isHeadless()) {
          System.out.println("You environment does not support a GUI.");
          selection = 100;
        }

        switch (selection) {
          case 1:
            printHelp();
            printMenu();
            selection = 100;
            break;

          case 2:
            updateCheck(true);
            printMenu();
            selection = 100;
            break;

          case 3:
            changeLocale();
            printMenu();
            selection = 100;
            break;

          case 4:
            runConfigurationEditor();
            printMenu();
            selection = 100;
            break;

          default:
            if (selection > 7) {
              System.out.println("Not a valid number. Please pick a number from 0 to 7.");
              printMenu();
            }
        }

      } catch (InputMismatchException | ParserConfigurationException | SAXException ex) {
        System.out.println("Not a valid number. Please pick a number from 0 to 7.");
        selection = 100;
      }

    } while (selection > 7);

    scanner.close();

    switch (selection) {
      case 5:
        run(Mode.CLI);
        break;

      case 6:
        run(Mode.WEB);
        break;

      case 7:
        run(Mode.GUI);
        break;

      case 0:
      default:
        System.out.println("Exiting...");
    }
  }

  /**
   * Allow the user to change the locale used in localization.
   *
   * @author Griefed
   */
  private void changeLocale() {
    System.out.println("What locale would you like to use?");
    System.out.println("(Locale format is en_us, de_de, uk_ua etc.)");
    System.out.println("Note: Changing the locale only affects the GUI. CLI always uses en_US.");

    Scanner scanner = new Scanner(System.in);
    String regex = "^[a-zA-Z]+_[a-zA-Z]+$";
    String userLocale;

    // For a list of locales, see https://stackoverflow.com/a/3191729/12537638 or
    // https://stackoverflow.com/a/28357857/12537638
    do {

      userLocale = scanner.next();

      if (!userLocale.matches(regex)) {

        System.out.println(
            "Incorrect format. ServerPackCreator currently only supports locales in the format of en_us (Language, Country).");

      } else {

        try {

          I18N.initialize(userLocale);

        } catch (IncorrectLanguageException e) {
          System.out.println(
              "Incorrect format. ServerPackCreator currently only supports locales in the format of en_us (Language, Country).");
          userLocale = "";
        }
      }

    } while (!userLocale.matches(regex));

    scanner.close();
    System.out.println("Using language: " + I18N.getMessage("localeName"));
  }

  /**
   * Print the text-menu so the user may decide what they would like to do next.
   *
   * @author Griefed
   */
  private void printMenu() {
    System.out.println();
    System.out.println("What would you like to do next?");
    System.out.println("(1) : Print help");
    System.out.println("(2) : Check for updates");
    System.out.println("(3) : Change locale");
    System.out.println("(4) : Generate a new configuration");
    System.out.println("(5) : Run ServerPackCreator in CLI-mode");
    System.out.println("(6) : Run ServerPackCreator as a webservice");
    System.out.println("(7) : Run ServerPackCreator with a GUI");
    System.out.println("(0) : Exit");
    System.out.println("-------------------------------------------");
    System.out.print("Enter the number of your selection: ");
  }

  /**
   * Run ServerPackCreator in {@link Mode#CLI}. Requires a {@code serverpackcreator.conf}-file to be
   * present.
   *
   * @throws IOException                  When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws ParserConfigurationException When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws SAXException                 When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @author Griefed
   */
  private void runHeadless() throws IOException, ParserConfigurationException, SAXException {
    if (!APPLICATIONPROPERTIES.defaultConfig().exists()) {

      LOG.warn("No serverpackcreator.conf found...");
      LOG.info(
          "If you want to run ServerPackCreator in CLI-mode, a serverpackcreator.conf is required.");
      LOG.info(
          "Either copy an existing config, or run ServerPackCreator with the '-cgen'-argument to generate one via commandline.");
      System.exit(1);

    } else {
      ConfigurationModel configurationModel = new ConfigurationModel();

      if (getConfigurationHandler().checkConfiguration(
          APPLICATIONPROPERTIES.defaultConfig(), configurationModel, false)) {
        System.exit(1);
      }

      if (!getServerPackHandler().run(configurationModel)) {
        System.exit(1);
      }
    }
  }

  /**
   * Run in {@link Mode#CGEN} and allow the user to load, edit and create a
   * {@code serverpackcreator.conf}-file using the CLI.
   *
   * @throws IOException                  When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws ParserConfigurationException When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @throws SAXException                 When the {@link VersionMeta} had to be instantiated, but
   *                                      an error occurred during the parsing of a manifest.
   * @author Griefed
   */
  private void runConfigurationEditor()
      throws IOException, ParserConfigurationException, SAXException {
    getConfigurationEditor().continuedRunOptions();
  }

  /**
   * Check whether a {@code serverpackcreator.conf}-file exists. If it doesn't exist, and we are not
   * running in {@link Mode#CLI} or {@link Mode#CGEN}, create an unconfigured default one which can
   * then be loaded into the GUI.
   *
   * @return {@code true} if a {@code serverpackcreator.conf}-file was created.
   * @author Griefed
   */
  public boolean checkForConfig() {

    if (!APPLICATIONPROPERTIES.defaultConfig().exists()
        && COMMANDLINE_PARSER.getModeToRunIn() != Mode.CLI
        && COMMANDLINE_PARSER.getModeToRunIn() != Mode.CGEN) {

      return getUtilities().JarUtils().copyFileFromJar(
          "de/griefed/resources/" + APPLICATIONPROPERTIES.defaultConfig().getName(),
          APPLICATIONPROPERTIES.defaultConfig(), ServerPackCreator.class);

    }
    return false;
  }

  /**
   * Check whether the specified server-files file exists and create it if it doesn't.
   *
   * @param fileToCheckFor The file which is to be checked for whether it exists and if it doesn't,
   *                       should be created.
   * @return {@code true} if the file was generated.
   * @author Griefed
   */
  public boolean checkServerFilesFile(@NotNull File fileToCheckFor) {
    return getUtilities().JarUtils().copyFileFromJar(
        "de/griefed/resources/server_files/" + fileToCheckFor.getName(),
        new File(
            APPLICATIONPROPERTIES.serverFilesDirectory(),
            fileToCheckFor.getName()),
        ServerPackCreator.class);
  }

  /**
   * Overwrite the specified server-files file, even when it exists. Used to ensure files like the
   * default script templates are always up to date.
   *
   * @param fileToOverwrite The file which is to be overwritten. If it exists. it is first deleted,
   *                        then extracted from our JAR-file.
   * @author Griefed
   */
  public void overwriteServerFilesFile(@NotNull File fileToOverwrite) {
    FileUtils.deleteQuietly(new File(
        APPLICATIONPROPERTIES.serverFilesDirectory(),
        fileToOverwrite.getName()));

    checkServerFilesFile(fileToOverwrite);
  }

  /**
   * Ensures serverpackcreator.db exists. If the database does not exist, it is created.
   *
   * @author Griefed
   */
  public void checkDatabase() {
    Connection connection = null;
    try {
      connection = DriverManager.getConnection(
          "jdbc:sqlite:" + APPLICATIONPROPERTIES.serverPackCreatorDatabase());

      DatabaseMetaData databaseMetaData = connection.getMetaData();
      LOG.debug("Database driver name: " + databaseMetaData.getDriverName());
      LOG.debug("Database driver version: " + databaseMetaData.getDriverVersion());
      LOG.debug("Database product name: " + databaseMetaData.getDatabaseProductName());
      LOG.debug("Database product version: " + databaseMetaData.getDatabaseProductVersion());

    } catch (SQLException ignored) {

    } finally {

      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException ex) {
          LOG.error("Couldn't close SQL connection", ex);
        }
      }
    }
  }

  /**
   * Check for update-availability. If an update is present, information about said update is
   * printed to the console.
   *
   * @param logToConsole Whether to log update information to console or to logs.
   * @author Griefed
   */
  public void updateCheck(boolean logToConsole) {
    getUpdateChecker().refresh();

    Optional<Update> update = getUpdateChecker().checkForUpdate(
        APPLICATIONPROPERTIES.serverPackCreatorVersion(),
        APPLICATIONPROPERTIES.checkForAvailablePreReleases());

    if (logToConsole) {
      System.out.println();
      if (update.isPresent()) {
        System.out.println("Update available!");
        System.out.println("    " + update.get().version());
        System.out.println("    " + update.get().url());
      } else {
        System.out.println("No updates available.");
      }
    } else {
      if (update.isPresent()) {
        LOG.info("Update available!");
        LOG.info("    " + update.get().version());
        LOG.info("    " + update.get().url());
      } else {
        LOG.info("No updates available.");
      }
    }

  }

  /**
   * Prints the help-text to the console. The help text contains information about:
   * <ul>
   *   <li>running ServerPackCreator in different modes:</li>
   *   <ul>
   *     <li>{@link Mode#CGEN}</li>
   *     <li>{@link Mode#UPDATE}</li>
   *     <li>{@link Mode#CLI}</li>
   *     <li>{@link Mode#WEB}</li>
   *     <li>{@link Mode#GUI}</li>
   *     <li>{@link Mode#SETUP}</li>
   *   </ul>
   *   <li>available languages</li>
   *   <li>where to report issues</li>
   *   <li>where to get support</li>
   *   <li>where to find the wiki</li>
   *   <li>how to support me</li>
   * </ul>
   *
   * @author Griefed
   */
  private void printHelp() {
    // TODO move to file in resources. Read and print text from file
    System.out.println(
        "# How to use ServerPackCreator:\n"
            + "#   java -jar ServerPackCreator.jar\n"
            + "#     Simply running the JAR without extra arguments runs ServerPackCreator in GUI mode unless\n"
            + "#     you are running in a headless environment. In the case of a headless environment, the CLI\n"
            + "#     mode will automatically be used.\n"
            + "#");
    System.out.println("#   Extra arguments to use ServerPackCreator with:\n" + "#");
    System.out.println(
        "#     -lang : Allows you to use one of the available languages for ServerPackCreator. I can not\n"
            + "#             guarantee that each of the following available languages is 100% translated.\n"
            + "#             You best choice is en_us, or not specifying any as that is the default, because\n"
            + "#             I write ServerPackCreator with english in mind. Available usages:\n"
            + "#             -lang en_us\n"
            + "#             -lang uk_ua\n"
            + "#             -lang de_de\n"
            + "#");
    System.out.println(
        "#     -cgen : Only available for the commandline interface. This will start the generation of\n"
            + "#             a new configuration file. You will be asked to enter information about your modpack\n"
            + "#             step-by-step. Each setting you enter will be checked for errors before it is saved.\n"
            + "#             If everything you enter is valid and without errors, it will be written to a new\n"
            + "#             serverpackcreator.conf and ServerPackCreator will immediately start a run with said\n"
            + "#             configuration file, generating a server pack for you.\n"
            + "#");
    System.out.println(
        "#   -update : Check whether a new version of ServerPackCreator is available for download.\n"
            + "#             If an update is available, the version and link to the release of said update are\n"
            + "#             written to the console so you can from work with it from there.\n"
            + "#             Note: Automatic updates are currently not planned nor supported, and neither are\n"
            + "#             downloads of any available updates to your system. You need to update manually.\n"
            + "#");
    System.out.println(
        "#     -cli  : Run ServerPackCreator in Command-line interface mode. Checks the serverpackcreator.conf\n"
            + "#             for errors and if none are found, starts the generation of a server pack with the configuration\n"
            + "#             provided by your serverpackcreator.conf.\n"
            + "#");
    System.out.println(
        "#     -web  : Run ServerPackCreator as a webservice available at http://localhost:8080. The webservice\n"
            + "#             provides the same functionality as running ServerPackCreator in GUI mode (so no Commandline\n"
            + "#             arguments and a non-headless environment) as well as a REST API which can be used in different ways.\n"
            + "#             For more information about the REST API, please see the Java documentation:\n"
            + "#              - GitHub Pages: https://griefed.github.io/ServerPackCreator/\n"
            + "#              - GitLab Pages: https://griefed.pages.griefed.de/ServerPackCreator/\n"
            + "#");
    System.out.println(
        "#      -gui : Run ServerPackCreator using the graphical user interface. If your environment supports\n"
            + "#             graphics, i.e. is not headless, then this is the default mode in which ServerPackCreator\n"
            + "#             started as when no arguments are used.\n"
            + "#");
    System.out.println(
        "#   --setup : Set up and prepare the environment for subsequent runs of ServerPackCreator.\n"
            + "#             This will create/copy all files needed for ServerPackCreator to function\n"
            + "#             properly from inside its JAR-file and setup everything else, too.\n"
            + "#");
    System.out.println("# Support:\n" + "#");
    System.out.println(
        "#   Issues:  Encountered a bug, or want some part of the documentation to be improved on? Got a suggestion?\n"
            + "#            Open an issue on GitHub at: https://github.com/Griefed/ServerPackCreator/issues\n"
            + "#");
    System.out.println(
        "#   Discord: If you would like to chat with me, ask me questions, or see when there's something new\n"
            + "#            regarding ServerPackCreator coming up, you can join my Discord server to stay up-to-date.\n"
            + "#             - Discord link: https://discord.griefed.de\n"
            + "#");
    System.out.println(
        "# Help/Wiki: If you want additional help on how to use ServerPackCreator, take a look at my wiki articles\n"
            + "#            regarding ServerPackCreator and some of the more advanced things it can do.\n"
            + "#             - Help:  https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-Help\n"
            + "#             - HowTo: https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-HowTo\n"
            + "#");
    System.out.println(
        "# Buy Me A Coffee:\n"
            + "#   You like ServerPackCreator and would like to support me? By all means, every bit is very much\n"
            + "#   appreciated and helps me pay for servers and food. Food is most important. And coffee. Food and Coffee.\n"
            + "#   Those two get converted into code. Thank you very much!\n"
            + "#     - Github Sponsors: https://github.com/sponsors/Griefed");
  }

  /**
   * Available mods of ServerPackCreator and their respective CLI-arguments required to be
   * activated/used.
   *
   * @author Griefed
   */
  public enum Mode {

    /**
     * <b>Priority 0</b>
     * <p>
     * Print ServerPackCreators help to commandline.
     */
    HELP("-help"),

    /**
     * <b>Priority 1</b>
     * <p>
     * Check whether a newer version of ServerPackCreator is available.
     */
    UPDATE("-update"),

    /**
     * <b>Priority 2</b>
     * <p>
     * Run ServerPackCreators configuration generation.
     */
    CGEN("-cgen"),

    /**
     * <b>Priority 3</b>
     * <p>
     * Run ServerPackCreator in commandline-mode. If no graphical environment is supported, this is
     * the default ServerPackCreator will enter, even when starting ServerPackCreator with no extra
     * arguments at all.
     */
    CLI("-cli"),

    /**
     * <b>Priority 4</b>
     * <p>
     * Run ServerPackCreator as a webservice.
     */
    WEB("-web"),

    /**
     * <b>Priority 5</b>
     * <p>
     * Run ServerPackCreator with our GUI. If a graphical environment is supported, this is the
     * default ServerPackCreator will enter, even when starting ServerPackCreator with no extra
     * arguments at all.
     */
    GUI("-gui"),

    /**
     * <b>Priority 6</b>
     * <p>
     * Set up and prepare the environment for subsequent runs of ServerPackCreator. This will
     * create/copy all files needed for ServerPackCreator to function properly from inside its
     * JAR-file and setup everything else, too.
     * <p>
     * The {@code --setup}-argument also allows a user to specify a {@code properties}-file to load
     * into {@link ApplicationProperties}. Values are loaded from the specified file and
     * subsequently stored in the local {@code serverpackcreator.properties}-file inside
     * ServerPackCreators home-directory.
     */
    SETUP("--setup"),

    /**
     * <b>Priority 7</b>
     * <p>
     * Exit ServerPackCreator.
     */
    EXIT("exit"),

    /**
     * Used when the user wants to change the language of ServerPackCreator.
     */
    LANG("-lang");

    private final String ARGUMENT;

    Mode(@NotNull String cliArg) {
      ARGUMENT = cliArg;
    }

    /**
     * Textual representation of this mode.
     *
     * @return Textual representation of this mode.
     * @author Griefed
     */
    public @NotNull String argument() {
      return ARGUMENT;
    }
  }

  /**
   * The Commandline Parser checks the passed commandline arguments to determine the mode to run
   * in.
   * <p>
   * CLI arguments are checked in order of their priority, which you can find in {@link Mode}.
   * <p>
   * After the mode has been determined, you can acquire it with
   * {@link CommandlineParser#getModeToRunIn()}.
   * <p>
   * If a specific language was passed using the {@code -lang}-argument, you can get it with
   * {@link CommandlineParser#getLanguageToUse()}.
   * <p>
   * If ServerPackCreator was run with the {@code --setup}-argument specifying a
   * {@code properties}-file to load into {@link ApplicationProperties}, you can get said file via
   * {@link CommandlineParser#propertiesFile()}. Values are loaded from the specified file and
   * subsequently stored in the local {@code serverpackcreator.properties}-file inside
   * ServerPackCreators home-directory.
   *
   * @author Griefed
   */
  @SuppressWarnings("InnerClassMayBeStatic")
  public class CommandlineParser {

    /**
     * The mode in which ServerPackCreator will run in after the commandline arguments have been
     * parsed and checked.
     */
    private final Mode MODE;
    /**
     * The language ServerPackCreator should use if any was specified. Null if none was specified,
     * so we can use the default language {@code en_us}.
     */
    private final String LANG;

    private File propertiesFile = null;

    /**
     * Create a new CommandlineParser from the passed commandline-arguments with which
     * ServerPackCreator was started. The mode and language in which ServerPackCreator should run
     * will thus be determined and available to you via {@link #getModeToRunIn()} and
     * {@link #getLanguageToUse()}.<br> {@link #getLanguageToUse()} is wrapped in an
     * {@link Optional} to quickly determine whether a language was specified.
     *
     * @param args Array of commandline-arguments with which ServerPackCreator was started.
     *             Typically passed from {@link ServerPackCreator}.
     * @author Griefed
     */
    public CommandlineParser(@NotNull String[] args) {

      final List<String> argsList = new ArrayList<>(Arrays.asList(args));

      /*
       * Check whether a language locale was specified by the user.
       * If none was specified, set LANG to null so the Optional returns false for isPresent(),
       * thus allowing us to use the locale set in the ApplicationProperties later on.
       */
      if (argsList.contains(Mode.LANG.argument())
          && argsList.size() >= argsList.indexOf(Mode.LANG.argument()) + 1) {

        LANG = argsList.get(argsList.indexOf(Mode.LANG.argument()) + 1);
      } else {
        LANG = "en_us";
      }

      /*
       * Check whether the user wanted us to print the help-text.
       */
      if (argsList.contains(Mode.HELP.argument())) {

        MODE = Mode.HELP;
        return;
      }

      /*
       * Check whether the user wants to check for update availability.
       */
      if (argsList.contains(Mode.UPDATE.argument())) {

        MODE = Mode.UPDATE;
        return;
      }

      /*
       * Check whether the user wants to generate a new serverpackcreator.conf from the commandline.
       */
      if (argsList.contains(Mode.CGEN.argument())) {

        MODE = Mode.CGEN;
        return;
      }

      /*
       * Check whether the user wants to run in commandline-mode or whether a GUI would not be supported.
       */
      if (argsList.contains(Mode.CLI.argument())) {

        MODE = Mode.CLI;
        return;

      } else if (GraphicsEnvironment.isHeadless()) {

        MODE = Mode.CLI;
        return;
      }

      /*
       * Check whether the user wants ServerPackCreator to run as a webservice.
       */
      if (argsList.contains(Mode.WEB.argument())) {

        MODE = Mode.WEB;
        return;
      }

      /*
       * Check whether the user wants to use ServerPackCreators GUI.
       */
      if (argsList.contains(Mode.GUI.argument())) {

        MODE = Mode.GUI;
        return;
      }

      /*
       * Check whether the user wants to set up and prepare the environment for subsequent runs.
       */
      if (argsList.contains(Mode.SETUP.argument())) {

        if (argsList.size() > 1
            && new File(argsList.get(argsList.indexOf(Mode.SETUP.argument()) + 1)).isFile()) {

          propertiesFile = new File(argsList.get(argsList.indexOf(Mode.SETUP.argument()) + 1));

        }

        MODE = Mode.SETUP;
        return;
      }

      /*
       * Last but not least, failsafe-check whether a GUI would be supported.
       */
      if (!GraphicsEnvironment.isHeadless()) {
        MODE = Mode.GUI;
        return;
      }

      /*
       * If all else fails, exit ServerPackCreator.
       */
      MODE = Mode.EXIT;
    }

    /**
     * Get the mode in which ServerPackCreator should be run in.
     *
     * @return Mode in which ServerPackCreator should be run in.
     * @author Griefed
     */
    protected @NotNull Mode getModeToRunIn() {
      return MODE;
    }

    /**
     * Get the locale in which ServerPackCreator should be run in, wrapped in an {@link Optional}.
     *
     * @return The locale in which ServerPackCreator should be run in, wrapped in an
     * {@link Optional}.
     * @author Griefed
     */
    protected @NotNull Optional<String> getLanguageToUse() {
      return Optional.ofNullable(LANG);
    }

    /**
     * If ServerPackCreator was executed with the {@code --setup}-argument as well as a
     * properties-file, then this method will return the specified properties file, wrapped in an
     * {@link Optional}, so you can check whether it is present or not.
     *
     * @return The specified properties-file, wrapped in an Optional.
     * @author Griefed
     */
    public @NotNull Optional<File> propertiesFile() {
      return Optional.ofNullable(propertiesFile);
    }
  }
}
