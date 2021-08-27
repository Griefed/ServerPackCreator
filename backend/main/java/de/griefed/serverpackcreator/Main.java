/* Copyright (C) 2021  Griefed
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

import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.spring.ServerPackCreatorApplication;
import de.griefed.serverpackcreator.swing.SwingGuiInitializer;
import de.griefed.serverpackcreator.i18n.IncorrectLanguageException;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.system.ApplicationHome;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * <strong>Table of methods</strong>
 * <p>
 * {@link #main(String[])}
 * <p>
 * Depending on the passed commandline arguments and whether ServerPackCreator is run in a headless environment,
 * one of the following modes will be entered:<p>
 * <strong>-cgen</strong><p>
 * When ServerPackCreator is run with the <code>-cgen</code>-argument, you will be guided through a step-by-step
 * generation of a new configuration file.<p>
 * This mode is also entered if:<p>
 * ServerPackCreator is run with the <code>-cli</code>-argument <strong>AND</strong> if no configuration-file exists.<p>
 * ServerPackCreator can not find any configuration-file when run in cli-mode, for whatever reason.
 * <p>
 * <strong>-cli</strong><p>
 * When ServerPackCreator is run with the <code>-cli</code>-argument, it will be executed in commandline-mode only.
 * Use this argument if you explicitly want to execute ServerPackCreator without GUI. *
 * <p>
 * <strong>-lang</strong><p>
 * Specifies the locale with which ServerPackCreator is run. If no locale is specified or if this argument is not
 * used, ServerPackCreator will use the default locale <code>en_us</code><p>
 * Correct usage is:<p>
 * <code>-lang your_locale</code><p>
 * Examples:<p>
 * <code>-lang de_de</code><p>
 * <code>-lang en_us</code><p>
 * <strong>Headless environments</strong><p>
 * If ServerPackCreator is run in a headless environment, without any graphical environment, it should automatically
 * enter <code>-cli</code>-mode.
 * @author Griefed
 */
public class Main {

    private static final Logger LOG = LogManager.getLogger(Main.class);

    private static final File log4j2xml = new File("log4j2.xml");
    private static final File properties = new File("serverpackcreator.properties");
    


    /**
     * Initializes all objects needed for running ServerPackCreator and ensures Dependency Injection.<p>
     * Calls {@link DefaultFiles#filesSetup()} so all default files are available. Calls {@link AddonsHandler#initializeAddons()}
     * to gather the list of addons to execute after server pack generation.<br>
     * Checks arguments to determine which mode to enter. Valid arguments are "-lang", "-cli", "-cgen", "-weh" and "-help".<br>
     * Lists a couple of environment variables important for reporting issues.
     * @author Griefed
     * @param args Commandline arguments with which ServerPackCreator is run. Determines which mode ServerPackCreator
     * will enter and which locale is used.
     */
    public static void main(String[] args) {

        //Create our log4j2.xml and serverpackcreator.properties
        createFile(log4j2xml);
        createFile(properties);

        Properties serverpackcreatorproperties = null;

        try (InputStream inputStream = new FileInputStream("serverpackcreator.properties")) {
            serverpackcreatorproperties = new Properties();
            serverpackcreatorproperties.load(inputStream);
        } catch (IOException ex) {
            LOG.error("Couldn't read properties file.", ex);
        }

        List<String> programArgs = Arrays.asList(args);

        LocalizationManager LOCALIZATIONMANAGER = new LocalizationManager();
        if (Arrays.asList(args).contains("-lang")) {
            try {
                // Init the LocalizationManager with the locale passed by the cli arguments.
                LOCALIZATIONMANAGER.init(programArgs.get(programArgs.indexOf("-lang") + 1));
            } catch (IncorrectLanguageException e) {
                LOG.info(programArgs.get(programArgs.indexOf("-lang") + 1));
                // We can not use localized string here, because the localization manager has not yet been initialized.
                LOG.error("Incorrect language specified, falling back to English (United States)...");

                // Init the LocalizationManager with the default locale en_US.
                LOCALIZATIONMANAGER.init();
            }
        } else {
            // Check local lang.properties file for locale setting.
            LOCALIZATIONMANAGER.checkLocaleFile();
        }

        // Print help and information about ServerPackCreator which could help the user figure out what to do.
        if (Arrays.asList(args).contains("-help")) {
            LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("main.log.debug.help.issued"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help01"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help02"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help03"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help04"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help05"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help06"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help07"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help08"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help09"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help10"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help11"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help12"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help13"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help14"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help15"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help16"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help17"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help18"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help19"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help20"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help21"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help22"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help23"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help24"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help25"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help26"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help27"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help28"));
            System.out.println("#");
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help29"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help30"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help31"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help32"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help33"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help34"));
            System.out.println("#");
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help35"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help36"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help37"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help38"));
            System.out.println(LOCALIZATIONMANAGER.getLocalizedString("main.console.help39"));

            LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("main.log.debug.help.exit"));
            System.exit(0);
        }

        // Prepare instances for dependency injection
        DefaultFiles DEFAULTFILES = new DefaultFiles(LOCALIZATIONMANAGER);
        AddonsHandler ADDONSHANDLER = new AddonsHandler(LOCALIZATIONMANAGER);
        CurseCreateModpack CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER);
        ConfigurationHandler CONFIGURATIONHANDLER = new ConfigurationHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, serverpackcreatorproperties);
        ServerPackHandler SERVERPACKHANDLER = new ServerPackHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, ADDONSHANDLER, CONFIGURATIONHANDLER, serverpackcreatorproperties);

        //noinspection UnusedAssignment
        String jarPath = null,
               jarName = null,
               javaVersion = null,
               osArch = null,
               osName = null,
               osVersion = null;

        LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("main.log.debug.warning"));
        LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.wip0"));
        LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.wip1"));
        LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.wip2"));
        LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.wip3"));
        LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.wip4"));
        LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("main.log.warn.wip0"));

        //Print system information to console and logs.
        LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("main.log.debug.gathering"));
        ApplicationHome home = new ApplicationHome(de.griefed.serverpackcreator.Main.class);
        jarPath = home.getSource().toString().replace("\\","/");
        jarName = jarPath.substring(jarPath.lastIndexOf("/") + 1);
        javaVersion = System.getProperty("java.version");
        osArch = System.getProperty("os.arch");
        osName = System.getProperty("os.name");
        osVersion = System.getProperty("os.version");
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.enter"));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.jarpath"), jarPath));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.jarname"), jarName));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.java"), javaVersion));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.osarchitecture"), osArch));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.osname"), osName));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.osversion"), osVersion));
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("main.log.info.system.include"));

        // Ensure default files are present.
        DEFAULTFILES.filesSetup();

        // Check addons and generate addon-lists.
        ADDONSHANDLER.initializeAddons();

        // Start generation of a new configuration file with user input.
        if (Arrays.asList(args).contains("-cgen")) {

            CONFIGURATIONHANDLER.createConfigurationFile();

            ConfigurationModel configurationModel = new ConfigurationModel();

            if (SERVERPACKHANDLER.run(CONFIGURATIONHANDLER.getConfigFile(), configurationModel)) {
                System.exit(0);
            } else {
                System.exit(1);
            }

        // Start ServerPackCreator in commandline mode.
        } else if (Arrays.asList(args).contains("-cli")) {

            // Start generation of a new configuration with user input if no configuration file is present.
            if (!new File("creator.conf").exists() && !new File("serverpackcreator.conf").exists()) {

                CONFIGURATIONHANDLER.createConfigurationFile();
            }

            ConfigurationModel configurationModel = new ConfigurationModel();

            if (SERVERPACKHANDLER.run(CONFIGURATIONHANDLER.getConfigFile(), configurationModel)) {
                System.exit(0);
            } else {
                System.exit(1);
            }

        // Start ServerPackCreator as webservice.
        } else if (Arrays.asList(args).contains("-web")) {

            ServerPackCreatorApplication.main(args);

        // If the environment is headless, so no possibility for GUI, start in commandline-mode.
        } else if (GraphicsEnvironment.isHeadless()) {

            // Start generation of a new configuration with user input if no configuration file is present.
            if (!new File("creator.conf").exists() && !new File("serverpackcreator.conf").exists()) {

                CONFIGURATIONHANDLER.createConfigurationFile();
            }

            ConfigurationModel configurationModel = new ConfigurationModel();

            if (SERVERPACKHANDLER.run(CONFIGURATIONHANDLER.getConfigFile(), configurationModel)) {
                System.exit(0);
            } else {
                System.exit(1);
            }

        // If no mode is specified, and we have a graphical environment, start in GUI mode.
        } else {

            SwingGuiInitializer swingGuiInitializer = new SwingGuiInitializer(LOCALIZATIONMANAGER, CONFIGURATIONHANDLER, CURSECREATEMODPACK, SERVERPACKHANDLER, ADDONSHANDLER, serverpackcreatorproperties);
            
            swingGuiInitializer.mainGUI();
        }
    }

    private static void createFile(File fileToCreate) {
        if (!fileToCreate.exists()) {
            try {
                InputStream link = (Main.class.getResourceAsStream(String.format("/%s", fileToCreate.getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("%s", fileToCreate)));
                    link.close();
                }} catch (IOException ignored) {}
        }
    }
}
