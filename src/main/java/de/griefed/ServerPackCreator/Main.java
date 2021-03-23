package de.griefed.ServerPackCreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

public class Main {
    private static final Logger appLogger = LogManager.getLogger("ApplicationLogger");
    /** Main class makes the calls to every other class where the actual magic is happening. The main class of ServerPackCreator should never contain code which does work on the server pack itself.
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
            appLogger.info(String.format("JAR Path: %s", jarPath));
            String jarName = jarPath.substring(jarPath.lastIndexOf("/") + 1);
            appLogger.info(String.format("JAR Name: %s", jarName));
        } catch (URISyntaxException ex) {
            appLogger.error("Error getting jar name.", ex);
        }
        if (Arrays.asList(args).contains(Reference.CONFIG_GEN_ARGUMENT) || (!Reference.oldConfigFile.exists() && !Reference.configFile.exists())){
            CLISetup.setup();
        } 
        FilesSetup.filesSetup();
        if (!ConfigCheck.checkConfig()) {
            CopyFiles.cleanupEnvironment(Reference.modpackDir);
            try {
                CopyFiles.copyFiles(Reference.modpackDir, Reference.copyDirs);
            } catch (IOException ex) {
                appLogger.error("There was an error calling the copyFiles method.", ex);
            }
            if (Reference.clientMods.toArray().length != 0) {
                ServerSetup.deleteClientMods(Reference.modpackDir, Reference.clientMods);
            }
            CopyFiles.copyStartScripts(Reference.modpackDir, Reference.modLoader, Reference.includeStartScripts);
            if (Reference.includeServerInstallation) {
                ServerSetup.installServer(Reference.modLoader, Reference.modpackDir, Reference.minecraftVersion, Reference.modLoaderVersion, Reference.javaPath);
            } else {
                appLogger.info("Not installing modded server.");
            }
            if (Reference.includeServerIcon) {
                CopyFiles.copyIcon(Reference.modpackDir);
            } else {
                appLogger.info("Not including servericon.");
            }
            if (Reference.includeServerProperties) {
                CopyFiles.copyProperties(Reference.modpackDir);
            } else {
                appLogger.info("Not including server.properties.");
            }
            if (Reference.includeZipCreation) {
                ServerSetup.zipBuilder(Reference.modpackDir, Reference.modLoader, Reference.includeServerInstallation);
            } else {
                appLogger.info("Not creating zip archive of serverpack.");
            }
            appLogger.info("Serverpack available at: " + Reference.modpackDir + "/serverpack");
            appLogger.info("Done!");
        } else {
            appLogger.error("Config file has errors. Consider editing serverpackcreator.conf file that is located in directory with SPC.");
            System.exit(1);
        }
    }
}
