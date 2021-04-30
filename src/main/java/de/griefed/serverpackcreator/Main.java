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

import java.awt.*;
import java.io.File;
import java.net.URISyntaxException;
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
    private static final Logger appLogger = LogManager.getLogger(Main.class);

    /**
     * Initializes all objects needed for running ServerPackCreator and ensures Dependency Injection.
     * Calls {@link FilesSetup} so all default files are available.
     * Checks arguments to determine which mode to enter.
     * Lists a couple of environment variables important for reporting issues.
     *
     * @param args Commandline arguments with which ServerPackCreator is run. Determines which mode ServerPackCreator
     * will enter and which locale is used.
     */
    public static void main(String[] args) {
        List<String> programArgs = Arrays.asList(args);

        LocalizationManager localizationManager = new LocalizationManager();
        if (Arrays.asList(args).contains("-lang")) {
            try {
                localizationManager.init(programArgs.get(programArgs.indexOf("-lang") + 1));
            } catch (IncorrectLanguageException e) {
                appLogger.info(programArgs.get(programArgs.indexOf("-lang") + 1));
                appLogger.error("Incorrect language specified, falling back to English (United States)...");
                localizationManager.init();
            }
        } else {
            localizationManager.checkLocaleFile();
        }

        CurseCreateModpack curseCreateModpack = new CurseCreateModpack(localizationManager);
        Configuration configuration = new Configuration(localizationManager, curseCreateModpack);
        FilesSetup filesSetup = new FilesSetup(localizationManager);
        CreateServerPack createServerPack = new CreateServerPack(localizationManager, configuration, curseCreateModpack);
        CreateGui tabbedPane = new CreateGui(localizationManager, configuration, curseCreateModpack);

        String jarPath = null,
                jarName = null,
                javaVersion = null,
                osArch = null,
                osName = null,
                osVersion = null;

        appLogger.warn(localizationManager.getLocalizedString("handler.log.warn.wip0"));
        appLogger.warn(localizationManager.getLocalizedString("handler.log.warn.wip1"));
        appLogger.warn(localizationManager.getLocalizedString("handler.log.warn.wip2"));
        appLogger.warn(localizationManager.getLocalizedString("handler.log.warn.wip3"));
        appLogger.warn(localizationManager.getLocalizedString("handler.log.warn.wip4"));
        appLogger.warn(localizationManager.getLocalizedString("handler.log.warn.wip0"));

        try {
            jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            jarName = jarPath.substring(jarPath.lastIndexOf("/") + 1);
            javaVersion = System.getProperty("java.version");
            osArch = System.getProperty("os.arch");
            osName = System.getProperty("os.name");
            osVersion = System.getProperty("os.version");
            appLogger.info(localizationManager.getLocalizedString("handler.log.info.system.enter"));
            appLogger.info(String.format(localizationManager.getLocalizedString("handler.log.info.system.jarpath"), jarPath));
            appLogger.info(String.format(localizationManager.getLocalizedString("handler.log.info.system.jarname"), jarName));
            appLogger.info(String.format(localizationManager.getLocalizedString("handler.log.info.system.java"), javaVersion));
            appLogger.info(String.format(localizationManager.getLocalizedString("handler.log.info.system.osarchitecture"), osArch));
            appLogger.info(String.format(localizationManager.getLocalizedString("handler.log.info.system.osname"), osName));
            appLogger.info(String.format(localizationManager.getLocalizedString("handler.log.info.system.osversion"), osVersion));
            appLogger.info(localizationManager.getLocalizedString("handler.log.info.system.include"));

        } catch (URISyntaxException ex) {
            appLogger.error(localizationManager.getLocalizedString("handler.log.error.system.properties"), ex);
        }

        filesSetup.filesSetup();

        if (Arrays.asList(args).contains("-cgen")) {

            configuration.createConfigurationFile();

            if (createServerPack.run()) {
                System.exit(0);
            } else {
                System.exit(1);
            }

        } else if (Arrays.asList(args).contains("-cli")) {

            if (!new File("creator.conf").exists() && !new File("serverpackcreator.conf").exists()) {

                configuration.createConfigurationFile();
            }

            if (createServerPack.run()) {
                System.exit(0);
            } else {
                System.exit(1);
            }
        } else if (GraphicsEnvironment.isHeadless()) {

            if (!new File("creator.conf").exists() && !new File("serverpackcreator.conf").exists()) {

                configuration.createConfigurationFile();
            }

            if (createServerPack.run()) {
                System.exit(0);
            } else {
                System.exit(1);
            }

        } else {

            tabbedPane.mainGUI();

        }
    }
}