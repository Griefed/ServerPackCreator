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
    /** Mandatory. Generate all default files, including serverpackcreator.conf if it does not exist. Said default files provide an example for the user as to what is possible and a template as well. It is possible to run this application only with the serverpackcreator.conf, so long as the related config variables are set correctly. If the serverpackcreator.conf file was generated during execution of this method, it is considered as first run and will exit after generation of said conf-file.
     *
     */
    static void filesSetup() {
        appLogger.info("Setting up default files...");
        boolean firstRun = true;
        try {
            Files.createDirectories(Paths.get("server_files"));
        } catch (IOException ex) {
            appLogger.error("Could not create server_files directory.", ex);
        }
        if (oldConfigFile.exists()) {
            try {
                Files.copy(oldConfigFile.getAbsoluteFile().toPath(), configFile.getAbsoluteFile().toPath());
                boolean isOldConfigDeleted = oldConfigFile.delete();
                if (isOldConfigDeleted) {
                    appLogger.info("creator.conf migrated to serverpackcreator.conf");
                }
                firstRun = false;
            } catch (IOException ex) {
                appLogger.error("Error renaming creator.conf to serverpackcreator.conf", ex);
            }
        } else if (!configFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/" + configFile.getName()));
                Files.copy(link, configFile.getAbsoluteFile().toPath());
                link.close();
                appLogger.info("Default config file generated. Please customize.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default config-file", ex);
                }
            }
        } else { firstRun = false; }
        if (!forgeWindowsFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + forgeWindowsFile.getName()));
                Files.copy(link, Paths.get("server_files/" + forgeWindowsFile));
                link.close();
                appLogger.info("Default Forge Windows start file generated.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default Forge Windows start file", ex);
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
                    appLogger.error("Could not extract default Forge Linux start file", ex);
                }
            }
        }
        if (!fabricWindowsFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + fabricWindowsFile.getName()));
                Files.copy(link, Paths.get("server_files/" + fabricWindowsFile));
                link.close();
                appLogger.info("Default Fabric Windows start file generated.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default Fabric Windows start file", ex);
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
                    appLogger.error("Could not extract default Fabric Linux start file", ex);
                }
            }
        }
        if (!propertiesFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + propertiesFile.getName()));
                Files.copy(link, Paths.get("server_files/" + propertiesFile));
                link.close();
                appLogger.info("Default server.properties file generated. Please customize if you intend on using it.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default server.properties file", ex);
                }
            }
        }
        if (!iconFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + iconFile.getName()));
                Files.copy(link, Paths.get("server_files/" + iconFile));
                link.close();
                appLogger.info("Default server-icon.png file generated. Please customize if you intend on using it.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default server-icon.png file", ex);
                }
            }
        }
        if (firstRun) {
            appLogger.warn("################################################################");
            appLogger.warn("#  FIRST RUN. CUSTOMIZE YOUR SERVERPACKCREATOR.CONF FILE NOW.  #");
            appLogger.warn("#        THE DEFAULTS ARE MEANT TO SHOW HOW IT'S DONE.         #");
            appLogger.warn("#    THE DEFAULTS WILL MOST LIKELY NOT WORK ON YOUR SYSTEM.    #");
            appLogger.warn("################################################################");
            appLogger.warn("First run! Default files generated. Please customize and run again.");
            System.exit(0);
        } else {
            appLogger.info("Setup completed.");
        }
    }
}