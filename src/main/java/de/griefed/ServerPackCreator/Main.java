package de.griefed.ServerPackCreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

public class Main {
    private static final Logger appLogger = LogManager.getLogger("Main");
    /** Warn user about WIP status. Get configuration from serverpackcreator.conf. Check configuration. Print configuration. Make calls according to configuration.
     * Basically, the main class makes the calls to every other class where the actual magic is happening. The main class of ServerPackCreator should never contain code which does work on the serverpack itself.
     * @param args Command Line Argument determines whether ServerPackCreator will start into normal operation mode or with a step-by-step generation of a configuration file.
     */
    public static void main(String[] args) {
        appLogger.warn("################################################################");
        appLogger.warn("#         WORK IN PROGRESS! CONSIDER THIS ALPHA-STATE!         #");
        appLogger.warn("#  USE AT YOUR OWN RISK! BE AWARE THAT DATA LOSS IS POSSIBLE!  #");
        appLogger.warn("#         I CAN NOT BE HELD RESPONSIBLE FOR DATA LOSS!         #");
        appLogger.warn("#                    YOU HAVE BEEN WARNED!                     #");
        appLogger.warn("################################################################");
        try {
            String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            appLogger.info("JAR Path: " + jarPath);
            String jarName = jarPath.substring(jarPath.lastIndexOf("/") + 1);
            appLogger.info("JAR Name: " + jarName);
        } catch (URISyntaxException ex) {
            appLogger.error("Error getting jar name.", ex);
        }
        if (Arrays.asList(args).contains(Reference.CONFIG_GEN_ARGUMENT) || (!FilesSetup.oldConfigFile.exists() && !FilesSetup.configFile.exists())){
            CLISetup.setup();
        } 
        FilesSetup.filesSetup();
        if (!ConfigCheck.checkConfig()) {
            CopyFiles.cleanupEnvironment(ConfigCheck.modpackDir);
            try {
                CopyFiles.copyFiles(ConfigCheck.modpackDir, ConfigCheck.copyDirs);
            } catch (IOException ex) {
                appLogger.error("There was an error calling the copyFiles method.", ex);
            }
            if (ConfigCheck.clientMods.toArray().length != 0) {
                ServerSetup.deleteClientMods(ConfigCheck.modpackDir, ConfigCheck.clientMods);
            }
            CopyFiles.copyStartScripts(ConfigCheck.modpackDir, ConfigCheck.modLoader, ConfigCheck.includeStartScripts);
            if (ConfigCheck.includeServerInstallation) {
                ServerSetup.installServer(ConfigCheck.modLoader, ConfigCheck.modpackDir, ConfigCheck.minecraftVersion, ConfigCheck.modLoaderVersion, ConfigCheck.javaPath);
            } else {
                appLogger.info("Not installing modded server.");
            }
            if (ConfigCheck.includeServerIcon) {
                CopyFiles.copyIcon(ConfigCheck.modpackDir);
            } else {
                appLogger.info("Not including servericon.");
            }
            if (ConfigCheck.includeServerProperties) {
                CopyFiles.copyProperties(ConfigCheck.modpackDir);
            } else {
                appLogger.info("Not including server.properties.");
            }
            if (ConfigCheck.includeZipCreation) {
                ServerSetup.zipBuilder(ConfigCheck.modpackDir, ConfigCheck.modLoader, ConfigCheck.includeServerInstallation);
            } else {
                appLogger.info("Not creating zip archive of serverpack.");
            }
            appLogger.info("Serverpack available at: " + ConfigCheck.modpackDir + "/serverpack");
            appLogger.info("Done!");
        } else {
            appLogger.error("Config file has errors. Consider editing serverpackcreator.conf file that is located in directory with SPC.");
            System.exit(1);
        }
    }
}
