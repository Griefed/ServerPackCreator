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

import de.griefed.serverpackcreator.curseforgemodpack.CurseCreateModpack;
import de.griefed.serverpackcreator.gui.CreateGui;
import de.griefed.serverpackcreator.i18n.IncorrectLanguageException;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.system.ApplicationHome;

import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;

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
 */
public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    /**
     * Initializes all objects needed for running ServerPackCreator and ensures Dependency Injection.
     * Calls {@link DefaultFiles} so all default files are available.
     * Checks arguments to determine which mode to enter.
     * Lists a couple of environment variables important for reporting issues.
     * @param args Commandline arguments with which ServerPackCreator is run. Determines which mode ServerPackCreator
     * will enter and which locale is used.
     */
    public static void main(String[] args) {
        List<String> programArgs = Arrays.asList(args);

        LocalizationManager localizationManager = new LocalizationManager();
        if (Arrays.asList(args).contains("-lang")) {
            try {
                // Init the LocalizationManager with the locale passed by the cli arguments.
                localizationManager.init(programArgs.get(programArgs.indexOf("-lang") + 1));
            } catch (IncorrectLanguageException e) {
                LOG.info(programArgs.get(programArgs.indexOf("-lang") + 1));
                // We can not use localized string here, because the localization manager has not yet been initialized.
                LOG.error("Incorrect language specified, falling back to English (United States)...");

                // Init the LocalizationManager with the default locale en_US.
                localizationManager.init();
            }
        } else {
            // Check local lang.properties file for locale setting.
            localizationManager.checkLocaleFile();
        }

        // Prepare instances for dependency injection
        CurseCreateModpack curseCreateModpack = new CurseCreateModpack(localizationManager);
        Configuration configuration = new Configuration(localizationManager, curseCreateModpack);
        DefaultFiles defaultFiles = new DefaultFiles(localizationManager);
        CreateServerPack createServerPack = new CreateServerPack(localizationManager, configuration, curseCreateModpack);

        //noinspection UnusedAssignment
        String jarPath = null,
               jarName = null,
               javaVersion = null,
               osArch = null,
               osName = null,
               osVersion = null;

        LOG.warn(localizationManager.getLocalizedString("handler.log.warn.wip0"));
        LOG.warn(localizationManager.getLocalizedString("handler.log.warn.wip1"));
        LOG.warn(localizationManager.getLocalizedString("handler.log.warn.wip2"));
        LOG.warn(localizationManager.getLocalizedString("handler.log.warn.wip3"));
        LOG.warn(localizationManager.getLocalizedString("handler.log.warn.wip4"));
        LOG.warn(localizationManager.getLocalizedString("handler.log.warn.wip0"));

        // Print system information to console and logs.
        ApplicationHome home = new ApplicationHome(Main.class);
        jarPath = home.getSource().toString().replace("\\","/");
        jarName = jarPath.substring(jarPath.lastIndexOf("/") + 1);
        javaVersion = System.getProperty("java.version");
        osArch = System.getProperty("os.arch");
        osName = System.getProperty("os.name");
        osVersion = System.getProperty("os.version");
        LOG.info(localizationManager.getLocalizedString("handler.log.info.system.enter"));
        LOG.info(String.format(localizationManager.getLocalizedString("handler.log.info.system.jarpath"), jarPath));
        LOG.info(String.format(localizationManager.getLocalizedString("handler.log.info.system.jarname"), jarName));
        LOG.info(String.format(localizationManager.getLocalizedString("handler.log.info.system.java"), javaVersion));
        LOG.info(String.format(localizationManager.getLocalizedString("handler.log.info.system.osarchitecture"), osArch));
        LOG.info(String.format(localizationManager.getLocalizedString("handler.log.info.system.osname"), osName));
        LOG.info(String.format(localizationManager.getLocalizedString("handler.log.info.system.osversion"), osVersion));
        LOG.info(localizationManager.getLocalizedString("handler.log.info.system.include"));

        // Ensure default files are present.
        defaultFiles.filesSetup();

        // Start generation of a new configuration file with user input.
        if (Arrays.asList(args).contains("-cgen")) {

            configuration.createConfigurationFile();

            if (createServerPack.run()) {
                System.exit(0);
            } else {
                System.exit(1);
            }

        // Start ServerPackCreator in commandline mode.
        } else if (Arrays.asList(args).contains("-cli")) {

            // Start generation of a new configuration with user input if no configuration file is present.
            if (!new File("creator.conf").exists() && !new File("serverpackcreator.conf").exists()) {

                configuration.createConfigurationFile();
            }

            if (createServerPack.run()) {
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

                configuration.createConfigurationFile();
            }

            if (createServerPack.run()) {
                System.exit(0);
            } else {
                System.exit(1);
            }

        // If no mode is specified and we have a graphical environment, start in GUI mode.
        } else {

            CreateGui createGui = new CreateGui(localizationManager, configuration, curseCreateModpack, createServerPack);
            
            createGui.mainGUI();

        }
    }
}
