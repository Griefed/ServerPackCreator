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

import de.griefed.serverpackcreator.i18n.IncorrectLanguageException;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.plugins.ApplicationPlugins;
import de.griefed.serverpackcreator.swing.ServerPackCreatorGui;
import de.griefed.serverpackcreator.swing.ServerPackCreatorSplash;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.ConfigurationCreator;
import de.griefed.serverpackcreator.utilities.UpdateChecker;
import de.griefed.serverpackcreator.utilities.common.Utilities;
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
public class ServerPackCreator {

  private static final Logger LOG = LogManager.getLogger(ServerPackCreator.class);

  private final CommandlineParser COMMANDLINE_PARSER;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final LocalizationManager LOCALIZATIONMANAGER;

  private final String[] ARGS;

  private final File LOG4J2XML = new File("log4j2.xml");
  private final File SERVERPACKCREATOR_PROPERTIES = new File("serverpackcreator.properties");
  private Utilities utilities = null;
  private VersionMeta versionMeta = null;
  private ConfigUtilities configUtilities = null;
  private ConfigurationHandler configurationHandler = null;
  private ApplicationPlugins applicationPlugins = null;
  private ServerPackHandler serverPackHandler = null;
  private ServerPackCreatorSplash serverPackCreatorSplash = null;
  private UpdateChecker updateChecker = null;
  private HashMap<String, String> systemInformation;

  /**
   * Initialize ServerPackCreator and determine the {@link CommandlineParser.Mode} to run in.
   *
   * @param args Commandline arguments with which ServerPackCreator is run. Determines which mode
   *     ServerPackCreator will enter and which locale is used.
   * @author Griefed
   */
  public ServerPackCreator(String[] args) {
    this.ARGS = args;
    this.COMMANDLINE_PARSER = new CommandlineParser(ARGS);
    this.APPLICATIONPROPERTIES = new ApplicationProperties();

    if (COMMANDLINE_PARSER.getLanguageToUse().isPresent()) {
      this.LOCALIZATIONMANAGER =
          new LocalizationManager(
              APPLICATIONPROPERTIES, COMMANDLINE_PARSER.getLanguageToUse().get());
    } else {
      this.LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
    }
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
        runWebservice();
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

    this.utilities = new Utilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);

    this.systemInformation =
        utilities.JarUtils()
            .systemInformation(
                utilities.JarUtils().getApplicationHomeForClass(ServerPackCreator.class));

    // Uncomment if you want to work on encodings....
    // Nightmare fuel...
    // utilities.SystemUtils().println(LOCALIZATIONMANAGER.getLocalizedString("encoding.check"));

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
        writer.write("# - Load all plugins except these.   - #\n");
        writer.write("# - Add one plugin-id per line.      - #\n");
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

    if (config || serverProperties || serverIcon) {

      LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning0"));
      LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning1"));
      LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning2"));
      LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning3"));
      LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.warn.filessetup.warning0"));

    } else {
      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.filessetup.finish"));
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

      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("main.log.debug.warning"));
      LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.wip0"));
      LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.wip1"));
      LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.wip2"));
      LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.wip3"));
      LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.wip4"));
      LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.wip0"));
    }

    /* This log is meant to be read by the user, therefore we allow translation. */
    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.enter"));
    LOG.info("ServerPackCreator version: " + APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION());
    LOG.info(
        String.format(
            LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.jarpath"),
            systemInformation.get("jarPath")));
    LOG.info(
        String.format(
            LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.jarname"),
            systemInformation.get("jarName")));
    LOG.info(
        String.format(
            LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.java"),
            systemInformation.get("javaVersion")));
    LOG.info(
        String.format(
            LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.osarchitecture"),
            systemInformation.get("osArch")));
    LOG.info(
        String.format(
            LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.osname"),
            systemInformation.get("osName")));
    LOG.info(
        String.format(
            LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.osversion"),
            systemInformation.get("osVersion")));
    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.include"));
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
            APPLICATIONPROPERTIES.QUILT_VERSION_MANIFEST_LOCATION(),
            APPLICATIONPROPERTIES.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION());

    this.configUtilities =
        new ConfigUtilities(LOCALIZATIONMANAGER, utilities, APPLICATIONPROPERTIES, versionMeta);

    this.configurationHandler =
        new ConfigurationHandler(
            LOCALIZATIONMANAGER, versionMeta, APPLICATIONPROPERTIES, utilities, configUtilities);
  }

  /**
   * Initialize {@link ApplicationPlugins}, {@link ServerPackHandler}.
   *
   * @throws IOException if the {@link VersionMeta} could not be instantiated.
   * @author Griefed
   */
  private void stageThree() throws IOException {
    this.applicationPlugins = new ApplicationPlugins();

    this.serverPackHandler =
        new ServerPackHandler(
            LOCALIZATIONMANAGER, APPLICATIONPROPERTIES, versionMeta, utilities, applicationPlugins);

    if (this.updateChecker == null) {
      this.updateChecker = new UpdateChecker(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
    } else {
      this.updateChecker.refresh();
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
          public void onStart(FileAlterationObserver observer) {}

          @Override
          public void onDirectoryCreate(File directory) {}

          @Override
          public void onDirectoryChange(File directory) {}

          @Override
          public void onDirectoryDelete(File directory) {}

          @Override
          public void onFileCreate(File file) {}

          @Override
          public void onFileChange(File file) {}

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
              }
            }
          }

          @Override
          public void onStop(FileAlterationObserver observer) {}

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
                  Objects.requireNonNull(Main.class.getResourceAsStream("/" + toCreate.getName())),
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
    new ServerPackCreatorGui(
            LOCALIZATIONMANAGER,
            configurationHandler,
            serverPackHandler,
            APPLICATIONPROPERTIES,
            versionMeta,
            utilities,
            new UpdateChecker(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES),
            applicationPlugins,
            configUtilities,
            serverPackCreatorSplash)
        .mainGUI();
  }

  /**
   * Offer the user to continue using ServerPackCreator.
   *
   * @throws IOException if an error occurs trying to run ServerPackCreator in {@link
   *     CommandlineParser.Mode#GUI}, {@link CommandlineParser.Mode#CLI} or {@link
   *     CommandlineParser.Mode#WEB}
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
          System.out.println(LOCALIZATIONMANAGER.getLocalizedString("run.menu.error.gui"));
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
              System.out.println(
                  LOCALIZATIONMANAGER.getLocalizedString("run.menu.error.selection"));
              printMenu();
            }
        }

      } catch (InputMismatchException ex) {
        System.out.println(LOCALIZATIONMANAGER.getLocalizedString("run.menu.error.selection"));
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
        System.out.println(LOCALIZATIONMANAGER.getLocalizedString("run.menu.exit"));
        System.exit(0);
    }
  }

  private void changeLocale() {
    System.out.println(LOCALIZATIONMANAGER.getLocalizedString("run.menu.change.locale"));
    System.out.println(LOCALIZATIONMANAGER.getLocalizedString("run.menu.change.locale.format"));

    Scanner scanner = new Scanner(System.in);
    String regex = "^[a-zA-Z]+_[a-zA-Z]+$";
    String userLocale;

    // For a list of locales, see https://stackoverflow.com/a/3191729/12537638 or
    // https://stackoverflow.com/a/28357857/12537638
    do {

      userLocale = scanner.next();

      if (!userLocale.matches(regex)) {

        System.out.println(LOCALIZATIONMANAGER.getLocalizedString("run.menu.change.locale.error"));

      } else {

        try {

          LOCALIZATIONMANAGER.initialize(userLocale);

        } catch (IncorrectLanguageException e) {
          System.out.println(
              LOCALIZATIONMANAGER.getLocalizedString("run.menu.change.locale.error"));
          userLocale = "";
        }
      }

    } while (!userLocale.matches(regex));

    scanner.close();
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("cli.usingLanguage")
            + " "
            + LOCALIZATIONMANAGER.getLocalizedString("localeName"));
  }

  /**
   * Print the text-menu so the user may decide what they would like to do next.
   *
   * @author Griefed
   */
  private void printMenu() {
    System.out.println();
    System.out.println(LOCALIZATIONMANAGER.getLocalizedString("run.menu.options"));
    System.out.println("(1) : " + LOCALIZATIONMANAGER.getLocalizedString("run.menu.options1"));
    System.out.println("(2) : " + LOCALIZATIONMANAGER.getLocalizedString("run.menu.options2"));
    System.out.println("(3) : " + LOCALIZATIONMANAGER.getLocalizedString("run.menu.options3"));
    System.out.println("(4) : " + LOCALIZATIONMANAGER.getLocalizedString("run.menu.options4"));
    System.out.println("(5) : " + LOCALIZATIONMANAGER.getLocalizedString("run.menu.options5"));
    System.out.println("(6) : " + LOCALIZATIONMANAGER.getLocalizedString("run.menu.options6"));
    System.out.println("(7) : " + LOCALIZATIONMANAGER.getLocalizedString("run.menu.options7"));
    System.out.println("(0) : " + LOCALIZATIONMANAGER.getLocalizedString("run.menu.options0"));

    // Determine the length of the longest menu entry.
    int lengthOfText = 0;
    for (int i = 0; i < 8; i++) {
      if (LOCALIZATIONMANAGER.getLocalizedString("run.menu.options" + i).length() + 6
          > lengthOfText) {
        lengthOfText = LOCALIZATIONMANAGER.getLocalizedString("run.menu.options" + i).length() + 6;
      }
    }

    // Print "-" n-times. Where n is the length of the longest menu entry.
    for (int i = 0; i < lengthOfText; i++) {
      System.out.print("-");
    }
    System.out.println();
    System.out.print(LOCALIZATIONMANAGER.getLocalizedString("run.menu.options.select") + ": ");
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
   * @throws IOException if the {@link ConfigurationCreator} could not be instantiated.
   * @author Griefed
   */
  private void createConfig() throws IOException {

    new ConfigurationCreator(
            LOCALIZATIONMANAGER,
            configurationHandler,
            APPLICATIONPROPERTIES,
            utilities,
            versionMeta,
            configUtilities)
        .createConfigurationFile();
  }

  /**
   * Run ServerPackCreator as a webservice.
   *
   * @author Griefed
   */
  private void runWebservice() {

    if (systemInformation.get("osName").contains("windows")
        || systemInformation.get("osName").contains("Windows")) {

      Scanner reader = new Scanner(System.in);

      LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.windows"));
      System.out.print(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.windows.input") + " ");

      //noinspection UnusedAssignment
      String answer = "foobar";

      do {
        answer = reader.nextLine();

        if (answer.equals("No")) {

          LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.windows.no"));
          reader.close();
          System.exit(0);

        } else if (answer.equals("Yes")) {

          LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.windows.yes"));
          reader.close();
          WebService.main(ARGS);
        }

      } while (!answer.matches("^(Yes|No)$"));

    } else {

      WebService.main(ARGS);
    }
  }

  /**
   * Check for old config file, if found rename to new name. If neither old nor new config file can
   * be found, a new config file is generated.
   *
   * @return Boolean. Returns true if the file was generated, so we can inform the user about said
   *     newly generated file.
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
          /* This log is meant to be read by the user, therefore we allow translation. */
          LOG.info(
              LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforconfig.old"));
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

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(
            LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforconfig.config"));
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
   *     should be created.
   * @return Boolean. Returns true if the file was generated, so we can inform the user about said
   *     newly generated file.
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

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(
            String.format(
                LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.checkforfile"),
                fileToCheckFor));

        firstRun = true;

      } catch (IOException ex) {

        if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {

          LOG.error(String.format("Could not extract default %s file.", fileToCheckFor), ex);
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
      this.updateChecker = new UpdateChecker(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
    } else {
      this.updateChecker.refresh();
    }

    Optional<Update> update =
        updateChecker.checkForUpdate(
            APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION(),
            APPLICATIONPROPERTIES.checkForAvailablePreReleases());

    System.out.println();
    if (update.isPresent()) {
      System.out.println(LOCALIZATIONMANAGER.getLocalizedString("update.dialog.available"));
      System.out.println("    " + update.get().version());
      System.out.println("    " + update.get().url());
    } else {
      System.out.println(LOCALIZATIONMANAGER.getLocalizedString("updates.log.info.none"));
    }
  }

  /**
   * Print the help to console and exit with status code 0.
   *
   * @author Griefed
   */
  private void printHelp() {
    System.out.println(LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.intro"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.arguments.intro"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.arguments.lang"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.arguments.help"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.arguments.update"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.arguments.cgen"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.arguments.cli"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.arguments.web"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.arguments.gui"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.arguments.setup"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.support.intro"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.support.issues"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.support.discord"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.support.wiki"));
    System.out.println(
        LOCALIZATIONMANAGER.getLocalizedString("serverpackcreator.help.support.someluv"));
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
     * will thus be determined and available to you via {@link #getModeToRunIn()} and {@link
     * #getLanguageToUse()}.<br>
     * {@link #getLanguageToUse()} is wrapped in an {@link Optional} to quickly determine whether a
     * language was specified.
     *
     * @param args {@link String}-array of commandline-arguments with which ServerPackCreator was
     *     started. Typically passed from {@link Main}.
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
     *     {@link Optional}.
     * @author Griefed
     */
    protected Optional<String> getLanguageToUse() {
      return Optional.ofNullable(LANG);
    }

    /** Mode-priorities. Highest to lowest. */
    public enum Mode {

      /** Priority 0. Print ServerPackCreators help to commandline. */
      HELP("-help"),

      /** Priority 1. Check whether a newer version of ServerPackCreator is available. */
      UPDATE("-update"),

      /** Priority 2. Run ServerPackCreators configuration generation. */
      CGEN("-cgen"),

      /**
       * Priority 3. Run ServerPackCreator in commandline-mode. If no graphical environment is
       * supported, this is the default ServerPackCreator will enter, even when starting
       * ServerPackCreator with no extra arguments at all.
       */
      CLI("-cli"),

      /** Priority 4. Run ServerPackCreator as a webservice. */
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

      /** Priority 7. Exit ServerPackCreator. */
      EXIT("exit"),

      /** Used when the user wants to change the language of ServerPackCreator. */
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

  /**
   * @author Griefed
   */
  @SpringBootApplication
  @EnableScheduling
  @PropertySources({
    @PropertySource("classpath:application.properties"),
    @PropertySource("classpath:serverpackcreator.properties")
  })
  public static class WebService {

    /**
     * Start Spring Boot app, providing our Apache Tomcat and serving our frontend.
     *
     * @param args Arguments passed from invocation in {@link Main#main(String[])}.
     * @author Griefed
     */
    public static void main(String[] args) {
      SpringApplication.run(WebService.class, args);
    }
  }
}
