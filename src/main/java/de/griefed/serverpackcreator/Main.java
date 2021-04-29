package de.griefed.serverpackcreator;

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
        LocalizationManager localizationManager = new LocalizationManager();
        Configuration configuration = new Configuration(localizationManager);
        FilesSetup filesSetup = new FilesSetup(localizationManager);
        CreateServerPack createServerPack = new CreateServerPack(localizationManager, configuration);


        List<String> programArgs = Arrays.asList(args);

        String jarPath = null,
                jarName = null,
                javaVersion = null,
                osArch = null,
                osName = null,
                osVersion = null;

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

            CreateGui tabbedPane = new CreateGui(localizationManager, configuration);
            tabbedPane.mainGUI();

        }
    }
}