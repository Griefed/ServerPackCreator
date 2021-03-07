package de.griefed.serverPackCreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

class FilesSetup {
    static final File oldConfigFile = new File("creator.conf");
    static final File configFile = new File("serverpackcreator.conf");
    static final File propertiesFile = new File("server.properties");
    static final File iconFile = new File("server-icon.png");
    static final File forgeWindowsFile = new File("start-forge.bat");
    static final File forgeLinuxFile = new File("start-forge.sh");
    static final File fabricWindowsFile = new File("start-fabric.bat");
    static final File fabricLinuxFile = new File("start-fabric.sh");

    private static final Logger appLogger = LogManager.getLogger("FilesSetup");
    private static final Logger errorLogger = LogManager.getLogger("FilesSetupError");

    // Copy all default files to base directory where jar resides.
    static void filesSetup() {
        appLogger.info("Setting up default files...");
        boolean firstRun = true;
        // Create directory for server files for customization
        try {
            Files.createDirectories(Paths.get("server_files"));
        } catch (IOException ex) {
            errorLogger.error("Could not create server_files directory.", ex);
        }
        // Migrate creator.conf to new name, create config file in case it does not exist yet
        if (oldConfigFile.exists()) {
            try {
                Files.copy(oldConfigFile.getAbsoluteFile().toPath(), configFile.getAbsoluteFile().toPath());
                boolean isOldConfigDeleted = oldConfigFile.delete();
                if (isOldConfigDeleted) {
                    appLogger.info("creator.conf migrated to serverpackcreator.conf");
                }
                firstRun = false;
            } catch (IOException ex) {
                errorLogger.error("Error renaming creator.conf to serverpackcreator.conf", ex);
            }
        } else if (!configFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/" + configFile.getName()));
                Files.copy(link, configFile.getAbsoluteFile().toPath());
                link.close();
                appLogger.info("Default config file generated. Please customize.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    errorLogger.error("Could not extract default config-file", ex);
                }
            }
        } else { firstRun = false; }
        // Copy Forge related start scripts
        if (!forgeWindowsFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + forgeWindowsFile.getName()));
                Files.copy(link, Paths.get("server_files/" + forgeWindowsFile));
                link.close();
                appLogger.info("Default Forge Windows start file generated.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    errorLogger.error("Could not extract default Forge Windows start file", ex);
                }
            }
        }
        if (!forgeLinuxFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + forgeLinuxFile.getName()));
                Files.copy(link, Paths.get("server_files/" + forgeLinuxFile));
                link.close();
                appLogger.info("Default Forge Linux start file generated.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    errorLogger.error("Could not extract default Forge Linux start file", ex);
                }
            }
        }
        // Copy Fabric related start scripts
        if (!fabricWindowsFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + fabricWindowsFile.getName()));
                Files.copy(link, Paths.get("server_files/" + fabricWindowsFile));
                link.close();
                appLogger.info("Default Fabric Windows start file generated.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    errorLogger.error("Could not extract default Fabric Windows start file", ex);
                }
            }
        }
        if (!fabricLinuxFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + fabricLinuxFile.getName()));
                Files.copy(link, Paths.get("server_files/" + fabricLinuxFile));
                link.close();
                appLogger.info("Default Fabric Linux start file generated.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    errorLogger.error("Could not extract default Fabric Linux start file", ex);
                }
            }
        }
        // Copy server.properties if it does not exist
        if (!propertiesFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + propertiesFile.getName()));
                Files.copy(link, Paths.get("server_files/" + propertiesFile));
                link.close();
                appLogger.info("Default server.properties file generated. Please customize if you intend on using it.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    errorLogger.error("Could not extract default server.properties file", ex);
                }
            }
        }
        // Copy server icon file if it does not exist
        if (!iconFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + iconFile.getName()));
                Files.copy(link, Paths.get("server_files/" + iconFile));
                link.close();
                appLogger.info("Default server-icon.png file generated. Please customize if you intend on using it.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    errorLogger.error("Could not extract default server-icon.png file", ex);
                }
            }
        }
        // If the config file was just generated because it did not exist yet, then exit and tell user to customize
        if (firstRun) {
            appLogger.info("################################################################");
            appLogger.info("#                                                              #");
            appLogger.info("#       FIRST RUN. CUSTOMIZE YOUR CREATOR.CONF FILE NOW.       #");
            appLogger.info("#        THE DEFAULTS ARE MEANT TO SHOW HOW IT'S DONE.         #");
            appLogger.info("#    THE DEFAULTS WILL MOST LIKELY NOT WORK ON YOUR SYSTEM.    #");
            appLogger.info("#                                                              #");
            appLogger.info("################################################################");
            appLogger.warn("First run! Default files generated. Please customize and run again.");
            System.exit(0);
        } else {
            appLogger.info("Setup completed.");
        }
    }

}
