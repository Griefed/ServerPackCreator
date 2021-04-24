package de.griefed.ServerPackCreator;

import de.griefed.ServerPackCreator.i18n.IncorrectLanguageException;
import de.griefed.ServerPackCreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class Main {
    private static final Logger appLogger = LogManager.getLogger(Main.class);
    /** Main class makes the calls to every other class where the actual magic is happening. The main class of ServerPackCreator should never contain code which does work on the server pack itself.
     * @param args Command Line Argument determines whether ServerPackCreator will start into normal operation mode or with a step-by-step generation of a configuration file.
     */
    public static void main(String[] args) {
        List<String> programArgs = Arrays.asList(args);
        if (Arrays.asList(args).contains(Reference.LANG_ARGUMENT)) {
            try {
                LocalizationManager.init(programArgs.get(programArgs.indexOf(Reference.LANG_ARGUMENT) + 1));
            } catch (IncorrectLanguageException e) {
                appLogger.info(programArgs.get(programArgs.indexOf(Reference.LANG_ARGUMENT) + 1));
                appLogger.error("Incorrect language specified, falling back to English (United States)...");
                LocalizationManager.init();
            }
        } else {
            FilesSetup.checkLocaleFile();
        }
        
        String jarPath = null,
               jarName = null,
               javaVersion = null,
               osArch = null,
               osName = null,
               osVersion = null;

        appLogger.warn(LocalizationManager.getLocalizedString("handler.log.warn.wip0"));
        appLogger.warn(LocalizationManager.getLocalizedString("handler.log.warn.wip1"));
        appLogger.warn(LocalizationManager.getLocalizedString("handler.log.warn.wip2"));
        appLogger.warn(LocalizationManager.getLocalizedString("handler.log.warn.wip3"));
        appLogger.warn(LocalizationManager.getLocalizedString("handler.log.warn.wip4"));
        appLogger.warn(LocalizationManager.getLocalizedString("handler.log.warn.wip0"));

        try {
            jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            jarName = jarPath.substring(jarPath.lastIndexOf("/") + 1);
            javaVersion = System.getProperty("java.version");
            osArch = System.getProperty("os.arch");
            osName = System.getProperty("os.name");
            osVersion = System.getProperty("os.version");
            appLogger.info(LocalizationManager.getLocalizedString("handler.log.info.system.enter"));
            appLogger.info(String.format(LocalizationManager.getLocalizedString("handler.log.info.system.jarpath"), jarPath));
            appLogger.info(String.format(LocalizationManager.getLocalizedString("handler.log.info.system.jarname"), jarName));
            appLogger.info(String.format(LocalizationManager.getLocalizedString("handler.log.info.system.java"), javaVersion));
            appLogger.info(String.format(LocalizationManager.getLocalizedString("handler.log.info.system.osarchitecture"), osArch));
            appLogger.info(String.format(LocalizationManager.getLocalizedString("handler.log.info.system.osname"), osName));
            appLogger.info(String.format(LocalizationManager.getLocalizedString("handler.log.info.system.osversion"), osVersion));
            appLogger.info(LocalizationManager.getLocalizedString("handler.log.info.system.include"));

        } catch (URISyntaxException ex) {
            appLogger.error(LocalizationManager.getLocalizedString("handler.log.error.system.properties"), ex);
        }
        if (programArgs.contains(Reference.CONFIG_GEN_ARGUMENT) || (!Reference.oldConfigFile.exists() && !Reference.configFile.exists() && !jarPath.endsWith(".exe"))){
            CLISetup.setup();
        }
        if (!FilesSetup.filesSetup()) {
            appLogger.warn(LocalizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning0"));
            appLogger.warn(LocalizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning1"));
            appLogger.warn(LocalizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning2"));
            appLogger.warn(LocalizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning3"));
            appLogger.warn(LocalizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning0"));
            System.exit(0);
        }
        if (!ConfigCheck.checkConfig()) {
            CopyFiles.cleanupEnvironment(Reference.modpackDir);
            try {
                CopyFiles.copyFiles(Reference.modpackDir, Reference.copyDirs, Reference.clientMods);
            } catch (IOException ex) {
                appLogger.error(LocalizationManager.getLocalizedString("handler.log.error.runincli.copyfiles"), ex);
            }
            CopyFiles.copyStartScripts(Reference.modpackDir, Reference.modLoader, Reference.includeStartScripts);
            if (Reference.includeServerInstallation) {
                ServerSetup.installServer(Reference.modLoader, Reference.modpackDir, Reference.minecraftVersion, Reference.modLoaderVersion, Reference.javaPath);
            } else {
                appLogger.info(LocalizationManager.getLocalizedString("handler.log.info.runincli.server"));
            }
            if (Reference.includeServerIcon) {
                CopyFiles.copyIcon(Reference.modpackDir);
            } else {
                appLogger.info(LocalizationManager.getLocalizedString("handler.log.info.runincli.icon"));
            }
            if (Reference.includeServerProperties) {
                CopyFiles.copyProperties(Reference.modpackDir);
            } else {
                appLogger.info(LocalizationManager.getLocalizedString("handler.log.info.runincli.properties"));
            }
            if (Reference.includeZipCreation) {
                ServerSetup.zipBuilder(Reference.modpackDir, Reference.modLoader, Reference.includeServerInstallation);
            } else {
                appLogger.info(LocalizationManager.getLocalizedString("handler.log.info.runincli.zip"));
            }
            appLogger.info(String.format(LocalizationManager.getLocalizedString("handler.log.info.runincli.serverpack"), Reference.modpackDir));
            appLogger.info(String.format(LocalizationManager.getLocalizedString("handler.log.info.runincli.archive"), Reference.modpackDir));
            appLogger.info(LocalizationManager.getLocalizedString("handler.log.info.runincli.finish"));
            System.exit(0);
        } else {
            appLogger.error(LocalizationManager.getLocalizedString("handler.log.error.runincli"));
            System.exit(1);
        }
    }
}
