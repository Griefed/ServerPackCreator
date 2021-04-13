package de.griefed.serverpackcreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

public class Handler {
    private static final Logger appLogger = LogManager.getLogger(Handler.class);

    void main(String[] args) {
        String jarPath = null,
                jarName = null,
                javaVersion = null,
                osArch = null,
                osName = null,
                osVersion = null;

        appLogger.warn("################################################################");
        appLogger.warn("#                      WORK IN PROGRESS!                       #");
        appLogger.warn("#  USE AT YOUR OWN RISK! BE AWARE THAT DATA LOSS IS POSSIBLE!  #");
        appLogger.warn("#        I WILL NOT BE HELD RESPONSIBLE FOR DATA LOSS!         #");
        appLogger.warn("#                    YOU HAVE BEEN WARNED!                     #");
        appLogger.warn("################################################################");

        try {
            jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            jarName = jarPath.substring(jarPath.lastIndexOf("/") + 1);
            javaVersion = System.getProperty("java.version");
            osArch = System.getProperty("os.arch");
            osName = System.getProperty("os.name");
            osVersion = System.getProperty("os.version");
            appLogger.info("SYSTEM INFORMATION:");
            appLogger.info(String.format("JAR Path: %s", jarPath));
            appLogger.info(String.format("JAR Name: %s", jarName));
            appLogger.info(String.format("Java version: %s", javaVersion));
            appLogger.info(String.format("OS architecture: %s", osArch));
            appLogger.info(String.format("OS name: %s", osName));
            appLogger.info(String.format("OS version: %s", osVersion));
            appLogger.info("Include this information when reporting an issue on GitHub.");

        } catch (URISyntaxException ex) {
            appLogger.error("Error getting system properties.", ex);
        }

        if (Arrays.asList(args).contains(Reference.CONFIG_GEN_ARGUMENT)){

            Reference.cliSetup.setup();
            Reference.filesSetup.filesSetup();
            runInCli();

            System.exit(0);

        } else if (Arrays.asList(args).contains(Reference.RUN_CLI_ARGUMENT)) {
            if (!Reference.oldConfigFile.exists() && !Reference.configFile.exists()) {

                Reference.cliSetup.setup();
                Reference.filesSetup.filesSetup();
            }
            runInCli();

            System.exit(0);
        } else {
            Reference.mainGUI.main();
        }
    }

    private void runInCli() {
        if (!Reference.configCheck.checkConfig()) {
            Reference.copyFiles.cleanupEnvironment(Reference.modpackDir);
            try {
                Reference.copyFiles.copyFiles(Reference.modpackDir, Reference.copyDirs, Reference.clientMods);
            } catch (IOException ex) {
                appLogger.error("There was an error calling the copyFiles method.", ex);
            }
            Reference.copyFiles.copyStartScripts(Reference.modpackDir, Reference.modLoader, Reference.includeStartScripts);
            if (Reference.includeServerInstallation) {
                Reference.serverSetup.installServer(Reference.modLoader, Reference.modpackDir, Reference.minecraftVersion, Reference.modLoaderVersion, Reference.javaPath);
            } else {
                appLogger.info("Not installing modded server.");
            }
            if (Reference.includeServerIcon) {
                Reference.copyFiles.copyIcon(Reference.modpackDir);
            } else {
                appLogger.info("Not including servericon.");
            }
            if (Reference.includeServerProperties) {
                Reference.copyFiles.copyProperties(Reference.modpackDir);
            } else {
                appLogger.info("Not including server.properties.");
            }
            if (Reference.includeZipCreation) {
                Reference.serverSetup.zipBuilder(Reference.modpackDir, Reference.modLoader, Reference.includeServerInstallation);
            } else {
                appLogger.info("Not creating zip archive of serverpack.");
            }
            appLogger.info(String.format("Server pack available at: %s/server_pack", Reference.modpackDir));
            appLogger.info(String.format("Server pack archive available at : %s/server_pack.zip", Reference.modpackDir));
            appLogger.info("Done!");
            System.exit(0);
        } else {
            appLogger.error("ERROR: Please check your serverpackcreator.conf for any incorrect settings. This message is also displayed if serverpackcreator downloaded and setup a modpack from a projectID,fileID for modpackDir.");
            System.exit(1);
        }
    }
}
