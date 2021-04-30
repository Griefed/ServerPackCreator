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

public class Main {
    private static final Logger appLogger = LogManager.getLogger(Main.class);

    /**
     * Init and "main" has been moved to Handler-class. Main now only inits the LocalizationManager and passes the cli args, if any,  to Handler, which then runs the usual operations as they used to be in pre-2.x.x
     * @param args Commandline arguments with which ServerPackCreator is run. Passed to Handler-class which then decides what to do corresponding to input.
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