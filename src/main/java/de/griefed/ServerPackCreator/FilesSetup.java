package de.griefed.ServerPackCreator;

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
    /** Generate all default files, including serverpackcreator.conf if it does not exist. Said default files provide an example for the user as to what is possible and a template as well.
     * It is possible to run this application only with the serverpackcreator.conf, so long as the related config variables are set correctly.
     * If any file was generated during execution of this method, it is considered as first run and will exit after generation of said conf-file and tell the user to customize and run again.
     */
    static void filesSetup() {
        appLogger.info("Checking for default files...");
        boolean firstRun = true;
        try {
            Files.createDirectories(Paths.get("./server_files"));
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
                firstRun = true;
                appLogger.info("serverpackcreator.conf generated. Please customize.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default config-file", ex);
                }
            }
        } else { firstRun = false; }
        if (!forgeWindowsFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + forgeWindowsFile.getName()));
                Files.copy(link, Paths.get("./server_files/" + forgeWindowsFile));
                link.close();
                firstRun = true;
                appLogger.info("start-forge.bat generated. Please customize if you intend on using it.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default Forge Windows start file", ex);
                }
            }
        } else { firstRun = false; }
        if (!forgeLinuxFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + forgeLinuxFile.getName()));
                Files.copy(link, Paths.get("./server_files/" + forgeLinuxFile));
                link.close();
                firstRun = true;
                appLogger.info("start-forge.sh generated. Please customize if you intend on using it.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default Forge Linux start file", ex);
                }
            }
        } else { firstRun = false; }
        if (!fabricWindowsFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + fabricWindowsFile.getName()));
                Files.copy(link, Paths.get("./server_files/" + fabricWindowsFile));
                link.close();
                firstRun = true;
                appLogger.info("start-fabric.bat generated. Please customize if you intend on using it.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default Fabric Windows start file", ex);
                }
            }
        } else { firstRun = false; }
        if (!fabricLinuxFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + fabricLinuxFile.getName()));
                Files.copy(link, Paths.get("./server_files/" + fabricLinuxFile));
                link.close();
                firstRun = true;
                appLogger.info("start-fabric.sh generated. Please customize if you intend on using it.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default Fabric Linux start file", ex);
                }
            }
        } else { firstRun = false; }
        if (!propertiesFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + propertiesFile.getName()));
                Files.copy(link, Paths.get("./server_files/" + propertiesFile));
                link.close();
                firstRun = true;
                appLogger.info("server.properties generated. Please customize if you intend on using it.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default server.properties file", ex);
                }
            }
        } else { firstRun = false; }
        if (!iconFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + iconFile.getName()));
                Files.copy(link, Paths.get("./server_files/" + iconFile));
                link.close();
                firstRun = true;
                appLogger.info("server-icon.png generated. Please customize if you intend on using it.");
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default server-icon.png file", ex);
                }
            }
        } else { firstRun = false; }
        if (firstRun) {
            appLogger.warn("################################################################");
            appLogger.warn("#             ONE OR MORE DEFAULT FILE(S) GENERATED.           #");
            appLogger.warn("# CHECK THE LOGS TO FIND OUT WHICH FILE(S) WAS/WERE GENERATED. #");
            appLogger.warn("#         CUSTOMIZE, THEN RUN SERVERPACKCREATOR AGAIN!         #");
            appLogger.warn("################################################################");
            System.exit(0);
        } else {
            appLogger.info("Setup completed.");
        }
    }
}