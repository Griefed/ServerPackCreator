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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.i18n.IncorrectLanguageException;
import de.griefed.serverpackcreator.modscanning.AnnotationScanner;
import de.griefed.serverpackcreator.modscanning.FabricScanner;
import de.griefed.serverpackcreator.modscanning.ModScanner;
import de.griefed.serverpackcreator.modscanning.QuiltScanner;
import de.griefed.serverpackcreator.modscanning.TomlScanner;
import de.griefed.serverpackcreator.swing.ServerPackCreatorGui;
import de.griefed.serverpackcreator.swing.ServerPackCreatorSplash;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.ConfigurationCreator;
import de.griefed.serverpackcreator.utilities.UpdateChecker;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.utilities.misc.Generated;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import de.griefed.versionchecker.Update;
import java.awt.GraphicsEnvironment;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Executors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Launch-class of ServerPackCreator which determines the mode to run in, takes care of
 * initialization and dependency injection and, finally, running ServerPackCreator.
 *
 * @author Griefed
 */
@Generated
@SpringBootApplication
@EnableScheduling
@PropertySources({
    @PropertySource("classpath:application.properties"),
    @PropertySource("classpath:serverpackcreator.properties")
})
public class ServerPackCreator {

  private static final Logger LOG = LogManager.getLogger(ServerPackCreator.class);
  private final CommandlineParser COMMANDLINE_PARSER;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final I18n I18N;
  private final String[] ARGS;
  private final File LOG4J2XML = new File("log4j2.xml");
  private final File SERVERPACKCREATOR_PROPERTIES = new File("serverpackcreator.properties");
  private final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
  private Utilities utilities = null;
  private VersionMeta versionMeta = null;
  private ConfigUtilities configUtilities = null;
  private ConfigurationHandler configurationHandler = null;
  private ApplicationPlugins applicationPlugins = null;
  private ServerPackHandler serverPackHandler = null;
  private ServerPackCreatorSplash serverPackCreatorSplash = null;
  private UpdateChecker updateChecker = null;

  /**
   * Initialize ServerPackCreator and determine the {@link CommandlineParser.Mode} to run in.
   *
   * @param args Commandline arguments with which ServerPackCreator is run. Determines which mode
   *             ServerPackCreator will enter and which locale is used.
   * @author Griefed
   */
  public ServerPackCreator(String[] args) {
    this.ARGS = args;
    this.COMMANDLINE_PARSER = new CommandlineParser(ARGS);
    this.APPLICATIONPROPERTIES = new ApplicationProperties();

    if (COMMANDLINE_PARSER.getLanguageToUse().isPresent()) {
      this.I18N = new I18n(APPLICATIONPROPERTIES, COMMANDLINE_PARSER.getLanguageToUse().get());
    } else {
      this.I18N = new I18n(APPLICATIONPROPERTIES);
    }
  }

  /**
   * Initialize ServerPackCreator with the passed commandline-arguments and run.
   *
   * <p>For a list of available commandline arguments, check out {@link
   * ServerPackCreator.CommandlineParser.Mode}
   *
   * @param args Commandline arguments with which ServerPackCreator is run. Determines which mode
   *             ServerPackCreator will enter and which locale is used.
   * @throws IOException if the {@link VersionMeta} could not be instantiated.
   * @author Griefed
   */
  public static void main(String[] args) throws IOException {

    ServerPackCreator serverPackCreator = new ServerPackCreator(args);

    serverPackCreator.run();
  }

  /**
   * Start Spring Boot app, providing our Apache Tomcat and serving our frontend.
   *
   * @param args Arguments passed from invocation in {@link #main(String[])}.
   * @author Griefed
   */
  public static void web(String[] args) {
    SpringApplication.run(ServerPackCreator.class, args);
  }

  /**
   * Run ServerPackCreator with the mode acquired from {@link CommandlineParser}.
   *
   * @throws IOException if the run fails.
   * @author Griefed
   */
  public void run() throws IOException {
    run(COMMANDLINE_PARSER.getModeToRunIn());
  }

  /**
   * Run ServerPackCreator in a specific {@link CommandlineParser.Mode}.
   *
   * @param modeToRunIn {@link CommandlineParser.Mode} to run in.
   * @throws IOException if the run fails.
   * @author Griefed
   */
  public void run(CommandlineParser.Mode modeToRunIn) throws IOException {

    switch (modeToRunIn) {
      case HELP:
        printHelp();
        continuedRunOptions();
        break;

      case UPDATE:
        updateCheck();
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
        createConfig();
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
        serverPackCreatorSplash.update(20);
        stageTwo();
        serverPackCreatorSplash.update(40);
        stageThree();
        serverPackCreatorSplash.update(60);
        Executors.newSingleThreadExecutor().execute(this::stageFour);
        serverPackCreatorSplash.update(80);
        runGui();
        break;

      case SETUP:
        stageOne();

      case EXIT:
      default:
        LOG.debug("Exit specified or invalid mode chosen. Exiting...");
    }
  }

  /**
   * Stage one of starting ServerPackCreator. Initialize {@link Utilities} and create/copy the
   * default-files required by ServerPackCreator.
   *
   * @author Griefed
   */
  private void stageOne() {

    System.setProperty("log4j2.formatMsgNoLookups", "true");
    System.setProperty("file.encoding", StandardCharsets.UTF_8.name());

    this.utilities = new Utilities(APPLICATIONPROPERTIES);

    HashMap<String, String> systemInformation =
        utilities.JarUtils()
            .systemInformation(
                utilities.JarUtils().getApplicationHomeForClass(ServerPackCreator.class));

    LOG.debug("System information jarPath: " + systemInformation.get("jarPath"));
    LOG.debug("System information jarName: " + systemInformation.get("jarName"));

    if (!utilities.FileUtils()
        .checkPermissions(new File(systemInformation.get("jarPath")).getParentFile())) {

      LOG.error(
          "One or more file or directory has no read- or write-permission. This may lead to corrupted server packs! Check the permissions of the ServerPackCreator base directory!");
    }

    utilities.JarUtils().copyFileFromJar(LOG4J2XML, ServerPackCreator.class);

    if (!SERVERPACKCREATOR_PROPERTIES.exists()) {
      try {
        //noinspection ResultOfMethodCallIgnored
        SERVERPACKCREATOR_PROPERTIES.createNewFile();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    try {

      String prefix = "BOOT-INF/classes";
      String langSource = "/de/griefed/resources/lang";

      if (systemInformation.get("jarName").endsWith(".exe")) {
        prefix = "";
        langSource = "de/griefed/resources/lang";
      }

      utilities.JarUtils()
          .copyFolderFromJar(ServerPackCreator.class, langSource, "lang", prefix, ".properties");

    } catch (IOException ex) {
      LOG.error("Error copying \"/de/griefed/resources/lang\" from the JAR-file.");
    }

    try {
      Files.createDirectories(Paths.get("./server_files"));
    } catch (IOException ex) {
      LOG.error("Could not create server_files directory.", ex);
    }

    try {
      Files.createDirectories(Paths.get("./work"));
    } catch (IOException ex) {
      LOG.error("Could not create work directory.", ex);
    }

    try {
      Files.createDirectories(Paths.get("./work/temp"));
    } catch (IOException ex) {
      LOG.error("Could not create work/temp directory.", ex);
    }

    try {
      Files.createDirectories(Paths.get("./work/modpacks"));
    } catch (IOException ex) {
      LOG.error("Could not create work/modpacks directory.", ex);
    }

    try {
      Files.createDirectories(Paths.get("./server-packs"));
    } catch (IOException ex) {
      LOG.error("Could not create server-packs directory.", ex);
    }

    try {
      Files.createDirectories(Paths.get(System.getProperty("pf4j.pluginsDir", "./plugins")));
    } catch (IOException ex) {
      LOG.error("Could not create plugins directory.", ex);
    }

    if (!new File(System.getProperty("pf4j.pluginsDir", "./plugins") + "/disabled.txt").isFile()) {
      try (BufferedWriter writer =
          new BufferedWriter(
              new FileWriter(
                  Paths.get(System.getProperty("pf4j.pluginsDir", "./plugins"))
                      + "/disabled.txt"))) {

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
    boolean serverProperties =
        checkServerFilesFile(APPLICATIONPROPERTIES.DEFAULT_SERVER_PROPERTIES());
    boolean serverIcon = checkServerFilesFile(APPLICATIONPROPERTIES.DEFAULT_SERVER_ICON());
    boolean shellTemplate = checkServerFilesFile(APPLICATIONPROPERTIES.DEFAULT_SHELL_TEMPLATE());
    boolean powershellTemplate =
        checkServerFilesFile(APPLICATIONPROPERTIES.DEFAULT_POWERSHELL_TEMPLATE());

    if (config || serverProperties || serverIcon || shellTemplate || powershellTemplate) {

      LOG.warn("#################################################################");
      LOG.warn("#.............ONE OR MORE DEFAULT FILE(S) GENERATED.............#");
      LOG.warn("#..CHECK THE LOGS TO FIND OUT WHICH FILE(S) WAS/WERE GENERATED..#");
      LOG.warn("#...............CUSTOMIZE THEM BEFORE CONTINUING!...............#");
      LOG.warn("#################################################################");

    } else {
      LOG.info("Setup completed.");
    }

    // Print system information to console and logs.
    LOG.debug("Gathering system information to include in log to make debugging easier.");
    APPLICATIONPROPERTIES.setProperty(
        "homeDir",
        systemInformation
            .get("jarPath")
            .substring(0, systemInformation.get("jarPath").replace("\\", "/").lastIndexOf("/"))
            .replace("\\", "/"));

    if (APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION().contains("dev")
        || APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION().contains("alpha")
        || APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION().contains("beta")) {

      LOG.debug("Warning user about possible data loss.");
      LOG.warn("################################################################");
      LOG.warn("#.............ALPHA | BETA | DEV VERSION DETECTED..............#");
      LOG.warn("#.............THESE VERSIONS ARE WORK IN PROGRESS!.............#");
      LOG.warn("#..USE AT YOUR OWN RISK! BE AWARE THAT DATA LOSS IS POSSIBLE!..#");
      LOG.warn("#........I WILL NOT BE HELD RESPONSIBLE FOR DATA LOSS!.........#");
      LOG.warn("#....................YOU HAVE BEEN WARNED!.....................#");
      LOG.warn("################################################################");
    }

    LOG.info("SYSTEM INFORMATION:");
    LOG.info("ServerPackCreator version: " + APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION());
    LOG.info("JAR Path:        " + systemInformation.get("jarPath"));
    LOG.info("JAR Name:        " + systemInformation.get("jarName"));
    LOG.info("Java version:    " + systemInformation.get("javaVersion"));
    LOG.info("OS architecture: " + systemInformation.get("osArch"));
    LOG.info("OS name:         " + systemInformation.get("osName"));
    LOG.info("OS version:      " + systemInformation.get("osVersion"));
    LOG.info("Include this information when reporting an issue on GitHub.");
  }

  /**
   * Initialize {@link VersionMeta}, {@link ConfigUtilities}, {@link ConfigurationHandler}.
   *
   * @throws IOException if the {@link VersionMeta} could not be instantiated.
   * @author Griefed
   */
  private void stageTwo() throws IOException {
    this.versionMeta =
        new VersionMeta(
            APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.FABRIC_INTERMEDIARIES_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.QUILT_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION(),
            OBJECT_MAPPER);

    this.configUtilities = new ConfigUtilities(utilities, APPLICATIONPROPERTIES, OBJECT_MAPPER);

    this.configurationHandler =
        new ConfigurationHandler(
            I18N, versionMeta, APPLICATIONPROPERTIES, utilities, configUtilities);
  }

  /**
   * Initialize {@link ApplicationPlugins}, {@link ServerPackHandler} and our {@link UpdateChecker}
   * if it is found to be <code>null</code>.
   *
   * @throws IOException if the {@link VersionMeta} could not be instantiated.
   * @author Griefed
   */
  private void stageThree() throws IOException {
    this.applicationPlugins = new ApplicationPlugins();
    // TODO: store all objects in fields in this class
    this.serverPackHandler =
        new ServerPackHandler(
            APPLICATIONPROPERTIES,
            versionMeta,
            utilities,
            applicationPlugins,
            new ModScanner(
                new AnnotationScanner(OBJECT_MAPPER, utilities),
                new FabricScanner(OBJECT_MAPPER, utilities),
                new QuiltScanner(OBJECT_MAPPER, utilities),
                new TomlScanner(new TomlParser())));

    if (this.updateChecker == null) {
      this.updateChecker = new UpdateChecker();
    }
  }

  /**
   * Initialize our FileWatcher
   *
   * @author Griefed
   */
  private void stageFour() {

    LOG.debug("Setting up FileWatcher...");

    FileAlterationObserver fileAlterationObserver = new FileAlterationObserver(new File("."));
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
            if (!file.toString().replace("\\", "/").startsWith("./server-packs")
                && !file.toString().replace("\\", "/").startsWith("./work/modpacks")) {

              if (check(file, APPLICATIONPROPERTIES.SERVERPACKCREATOR_PROPERTIES())) {

                createFile(APPLICATIONPROPERTIES.SERVERPACKCREATOR_PROPERTIES());
                APPLICATIONPROPERTIES.reload();
                LOG.info("Restored serverpackcreator.properties and loaded defaults.");

              } else if (check(file, APPLICATIONPROPERTIES.DEFAULT_SERVER_PROPERTIES())) {

                checkServerFilesFile(APPLICATIONPROPERTIES.DEFAULT_SERVER_PROPERTIES());
                LOG.info("Restored default server.properties.");

              } else if (check(file, APPLICATIONPROPERTIES.DEFAULT_SERVER_ICON())) {

                checkServerFilesFile(APPLICATIONPROPERTIES.DEFAULT_SERVER_ICON());
                LOG.info("Restored default server-icon.png.");

              } else if (check(file, APPLICATIONPROPERTIES.DEFAULT_SHELL_TEMPLATE())) {

                checkServerFilesFile(APPLICATIONPROPERTIES.DEFAULT_SHELL_TEMPLATE());
                LOG.info("Restored default_template.sh.");

              } else if (check(file, APPLICATIONPROPERTIES.DEFAULT_POWERSHELL_TEMPLATE())) {

                checkServerFilesFile(APPLICATIONPROPERTIES.DEFAULT_POWERSHELL_TEMPLATE());
                LOG.info("Restored default_template.ps1.");
              }
            }
          }

          @Override
          public void onStop(FileAlterationObserver observer) {
          }

          private boolean check(File watched, File toCreate) {
            return watched
                .toString()
                .replace("\\", "/")
                .substring(watched.toString().replace("\\", "/").lastIndexOf("/") + 1)
                .equals(toCreate.toString());
          }

          private void createFile(File toCreate) {
            try {
              FileUtils.copyInputStreamToFile(
                  Objects.requireNonNull(
                      ServerPackCreator.class.getResourceAsStream("/" + toCreate.getName())),
                  toCreate);

            } catch (IOException ex) {
              LOG.error("Error creating file: " + toCreate, ex);
            }
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
   * Show the splashscreen of ServerPackCreator, indicating that things are loading.
   *
   * @author Griefed
   */
  private void showSplashScreen() {
    this.serverPackCreatorSplash =
        new ServerPackCreatorSplash(APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION());
  }

  /**
   * Run ServerPackCreator with our GUI.
   *
   * @throws IOException if the {@link VersionMeta} could not be instantiated.
   * @author Griefed
   */
  private void runGui() throws IOException {
    if (updateChecker == null) {
      updateChecker = new UpdateChecker();
    }

    new ServerPackCreatorGui(
        I18N,
        configurationHandler,
        serverPackHandler,
        APPLICATIONPROPERTIES,
        versionMeta,
        utilities,
        updateChecker,
        applicationPlugins,
        configUtilities,
        serverPackCreatorSplash)
        .mainGUI();
  }

  /**
   * Offer the user to continue using ServerPackCreator.
   *
   * @throws IOException if an error occurs trying to run ServerPackCreator in
   *                     {@link CommandlineParser.Mode#GUI}, {@link CommandlineParser.Mode#CLI} or
   *                     {@link CommandlineParser.Mode#WEB}
   * @author Griefed
   */
  private void continuedRunOptions() throws IOException {

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
            updateCheck();
            printMenu();
            selection = 100;
            break;

          case 3:
            changeLocale();
            printMenu();
            selection = 100;
            break;

          case 4:
            createConfig();
            printMenu();
            selection = 100;
            break;

          default:
            if (selection > 7) {
              System.out.println("Not a valid number. Please pick a number from 0 to 7.");
              printMenu();
            }
        }

      } catch (InputMismatchException ex) {
        System.out.println("Not a valid number. Please pick a number from 0 to 7.");
        selection = 100;
      }

    } while (selection > 7);

    scanner.close();

    switch (selection) {
      case 5:
        run(CommandlineParser.Mode.CLI);
        break;

      case 6:
        run(CommandlineParser.Mode.WEB);
        break;

      case 7:
        run(CommandlineParser.Mode.GUI);
        break;

      case 0:
      default:
        System.out.println("Exiting...");
        System.exit(0);
    }
  }

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
    System.out.println("(7) : Run ServerPackCraator with a GUI");
    System.out.println("(0) : Exit");
    System.out.println("-------------------------------------------");
    System.out.print("Enter the number of your selection: ");
  }

  /**
   * Run ServerPackCreator in headless, CLI, mode. If no serverpackcreator.conf-file exists, it is
   * created through {@link ConfigurationCreator#createConfigurationFile()} and subsequently used by
   * a ServerPackCreator headless-run.
   *
   * @throws IOException if the {@link ConfigurationCreator} could not be instantiated.
   * @author Griefed
   */
  private void runHeadless() throws IOException {
    if (!new File("serverpackcreator.conf").exists()) {
      createConfig();
    }

    ConfigurationModel configurationModel = new ConfigurationModel();

    if (configurationHandler.checkConfiguration(
        APPLICATIONPROPERTIES.DEFAULT_CONFIG(), configurationModel, false)
        && serverPackHandler.run(configurationModel)) {
      System.exit(0);
    } else {
      System.exit(1);
    }
  }

  /**
   * Create a new serverpackcreator.conf-file.
   *
   * @author Griefed
   */
  private void createConfig() {

    new ConfigurationCreator(
        configurationHandler,
        APPLICATIONPROPERTIES,
        utilities,
        versionMeta,
        configUtilities)
        .createConfigurationFile();
  }

  /**
   * Check for old config file, if found rename to new name. If neither old nor new config file can
   * be found, a new config file is generated.
   *
   * @return Boolean. Returns true if the file was generated, so we can inform the user about said
   * newly generated file.
   * @author Griefed
   */
  public boolean checkForConfig() {
    boolean firstRun = false;
    if (APPLICATIONPROPERTIES.OLD_CONFIG().exists()) {
      try {
        Files.copy(
            APPLICATIONPROPERTIES.OLD_CONFIG().getAbsoluteFile().toPath(),
            APPLICATIONPROPERTIES.DEFAULT_CONFIG().getAbsoluteFile().toPath());

        if (APPLICATIONPROPERTIES.OLD_CONFIG().delete()) {

          LOG.info("creator.conf migrated to serverpackcreator.conf.");
        }

      } catch (IOException ex) {
        if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
          LOG.error("Error renaming creator.conf to serverpackcreator.conf.", ex);
        }
      }
    } else if (!APPLICATIONPROPERTIES.DEFAULT_CONFIG().exists()) {
      try {

        FileUtils.copyInputStreamToFile(
            Objects.requireNonNull(
                ServerPackCreator.class.getResourceAsStream(
                    String.format(
                        "/de/griefed/resources/%s",
                        APPLICATIONPROPERTIES.DEFAULT_CONFIG().getName()))),
            APPLICATIONPROPERTIES.DEFAULT_CONFIG());

        LOG.info("serverpackcreator.conf generated. Please customize.");
        firstRun = true;

      } catch (IOException ex) {
        if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
          LOG.error("Could not extract default config-file.", ex);
          firstRun = true;
        }
      }
    }
    return firstRun;
  }

  /**
   * Checks for existence of defaults files. If it is not found, it is generated.
   *
   * @param fileToCheckFor The file which is to be checked for whether it exists and if it doesn't,
   *                       should be created.
   * @return Boolean. Returns true if the file was generated, so we can inform the user about said
   * newly generated file.
   * @author Griefed
   */
  public boolean checkServerFilesFile(File fileToCheckFor) {
    boolean firstRun = false;

    if (!new File(String.format("server_files/%s", fileToCheckFor)).exists()) {
      try {

        FileUtils.copyInputStreamToFile(
            Objects.requireNonNull(
                ServerPackCreator.class.getResourceAsStream(
                    String.format("/de/griefed/resources/server_files/%s", fileToCheckFor))),
            new File(String.format("./server_files/%s", fileToCheckFor)));

        LOG.info(fileToCheckFor + " generated. Please customize if you intend on using it.");

        firstRun = true;

      } catch (IOException ex) {

        if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {

          LOG.error("Could not extract default " + fileToCheckFor + " file.", ex);
          firstRun = true;
        }
      }
    }
    return firstRun;
  }

  /**
   * Ensures serverpackcreator.db exists. If the database does not exist, it is created.
   *
   * @author Griefed
   */
  public void checkDatabase() {
    Connection connection = null;
    try {
      connection =
          DriverManager.getConnection(
              "jdbc:sqlite:" + APPLICATIONPROPERTIES.SERVERPACKCREATOR_DATABASE());

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
   * Check for update-availability and exit with status code 0.
   *
   * @author Griefed
   */
  public void updateCheck() {
    if (this.updateChecker == null) {
      this.updateChecker = new UpdateChecker();
    } else {
      this.updateChecker.refresh();
    }

    Optional<Update> update =
        updateChecker.checkForUpdate(
            APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION(),
            APPLICATIONPROPERTIES.checkForAvailablePreReleases());

    System.out.println();
    if (update.isPresent()) {
      System.out.println("Update available!");
      System.out.println("    " + update.get().version());
      System.out.println("    " + update.get().url());
    } else {
      System.out.println("No updates available.");
    }
  }

  /**
   * Print the help to console and exit with status code 0.
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
        "#     -cgen : Only available for the commandline interface. This will start the generation of\n"
            + "#             a new configuration file. You will be asked to enter information about your modpack\n"
            + "#             step-by-step. Each setting you enter will be checked for errors before it is saved.\n"
            + "#             If everything you enter is valid and without errors, it will be written to a new\n"
            + "#             serverpackcreator.conf and ServerPackCreator will immediately start a run with said\n"
            + "#             configuration file, generating a server pack for you.\n"
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
   * Check the passed commandline-arguments with which ServerPackCreator was started and return the
   * mode in which to run.
   *
   * @author Griefed
   */
  public static class CommandlineParser {

    /**
     * The mode in which ServerPackCreator will run in after the commandline arguments have been
     * parsed and checked.
     */
    private final Mode MODE;
    /**
     * The language ServerPackCreator should use if any was specified. Null if none was specified,
     * so we can use the default language <code>en_us</code>.
     */
    private final String LANG;

    /**
     * Create a new CommandlineParser from the passed commandline-arguments with which
     * ServerPackCreator was started. The mode and language in which ServerPackCreator should run
     * will thus be determined and available to you via {@link #getModeToRunIn()} and
     * {@link #getLanguageToUse()}.<br> {@link #getLanguageToUse()} is wrapped in an
     * {@link Optional} to quickly determine whether a language was specified.
     *
     * @param args {@link String}-array of commandline-arguments with which ServerPackCreator was
     *             started. Typically passed from {@link ServerPackCreator}.
     * @author Griefed
     */
    public CommandlineParser(String[] args) {

      List<String> argsList = Arrays.asList(args);

      /*
       * Check whether a language locale was specified by the user.
       * If none was specified, set LANG to null so the Optional returns false for isPresent(),
       * thus allowing us to use the locale set in the ApplicationProperties later on.
       */
      if (argsList.contains(Mode.LANG.argument())) {
        this.LANG = argsList.get(argsList.indexOf(Mode.LANG.argument()) + 1);
      } else {
        this.LANG = null;
      }

      /*
       * Check whether the user wanted us to print the help-text.
       */
      if (argsList.contains(Mode.HELP.argument())) {
        this.MODE = Mode.HELP;
        return;
      }

      /*
       * Check whether the user wants to check for update availability.
       */
      if (argsList.contains(Mode.UPDATE.argument())) {
        this.MODE = Mode.UPDATE;
        return;
      }

      /*
       * Check whether the user wants to generate a new serverpackcreator.conf from the commandline.
       */
      if (argsList.contains(Mode.CGEN.argument())) {
        this.MODE = Mode.CGEN;
        return;
      }

      /*
       * Check whether the user wants to run in commandline-mode or whether a GUI would not be supported.
       */
      if (argsList.contains(Mode.CLI.argument())) {
        this.MODE = Mode.CLI;
        return;
      } else if (GraphicsEnvironment.isHeadless()) {
        this.MODE = Mode.CLI;
        return;
      }

      /*
       * Check whether the user wants ServerPackCreator to run as a webservice.
       */
      if (argsList.contains(Mode.WEB.argument())) {
        this.MODE = Mode.WEB;
        return;
      }

      /*
       * Check whether the user wants to use ServerPackCreators GUI.
       */
      if (argsList.contains(Mode.GUI.argument())) {
        this.MODE = Mode.GUI;
        return;
      }

      /*
       * Check whether the user wants to set up and prepare the environment for subsequent runs.
       */
      if (argsList.contains(Mode.SETUP.argument())) {
        this.MODE = Mode.SETUP;
        return;
      }

      /*
       * Last but not least, failsafe-check whether a GUI would be supported.
       */
      if (!GraphicsEnvironment.isHeadless()) {
        this.MODE = Mode.GUI;
        return;
      }

      /*
       * If all else fails, exit ServerPackCreator.
       */
      this.MODE = Mode.EXIT;
    }

    /**
     * Get the mode in which ServerPackCreator should be run in.
     *
     * @return {@link Mode} in which ServerPackCreator should be run in.
     * @author Griefed
     */
    protected Mode getModeToRunIn() {
      return MODE;
    }

    /**
     * Get the locale in which ServerPackCreator should be run in, wrapped in an {@link Optional}.
     *
     * @return {@link String} The locale in which ServerPackCreator should be run in, wrapped in an
     * {@link Optional}.
     * @author Griefed
     */
    protected Optional<String> getLanguageToUse() {
      return Optional.ofNullable(LANG);
    }

    /**
     * Mode-priorities. Highest to lowest.
     */
    public enum Mode {

      /**
       * Priority 0. Print ServerPackCreators help to commandline.
       */
      HELP("-help"),

      /**
       * Priority 1. Check whether a newer version of ServerPackCreator is available.
       */
      UPDATE("-update"),

      /**
       * Priority 2. Run ServerPackCreators configuration generation.
       */
      CGEN("-cgen"),

      /**
       * Priority 3. Run ServerPackCreator in commandline-mode. If no graphical environment is
       * supported, this is the default ServerPackCreator will enter, even when starting
       * ServerPackCreator with no extra arguments at all.
       */
      CLI("-cli"),

      /**
       * Priority 4. Run ServerPackCreator as a webservice.
       */
      WEB("-web"),

      /**
       * Priority 5. Run ServerPackCreator with our GUI. If a graphical environment is supported,
       * this is the default ServerPackCreator will enter, even when starting ServerPackCreator with
       * no extra arguments at all.
       */
      GUI("-gui"),

      /**
       * Priority 6 Set up and prepare the environment for subsequent runs of ServerPackCreator.
       * This will create/copy all files needed for ServerPackCreator to function properly from
       * inside its JAR-file and setup everything else, too.
       */
      SETUP("--setup"),

      /**
       * Priority 7. Exit ServerPackCreator.
       */
      EXIT("exit"),

      /**
       * Used when the user wants to change the language of ServerPackCreator.
       */
      LANG("-lang");

      private final String ARGUMENT;

      Mode(String cliArg) {
        this.ARGUMENT = cliArg;
      }

      /**
       * Textual representation of this mode.
       *
       * @return {@link String} Textual representation of this mode.
       * @author Griefed
       */
      public String argument() {
        return ARGUMENT;
      }
    }
  }
}
