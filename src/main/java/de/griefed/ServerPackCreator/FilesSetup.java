package de.griefed.ServerPackCreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

class FilesSetup {
    private static final Logger appLogger = LogManager.getLogger("ApplicationLogger");
    /** Calls individual methods which check for existence of default files. If any of these methods return true, ServerPackCreator will exit, giving the user the chance to customize it before the program runs in production.
     */
    static void filesSetup() {
        appLogger.info("Checking for default files...");
        try {
            Files.createDirectories(Paths.get("./server_files"));
        } catch (IOException ex) {
            appLogger.error("Could not create server_files directory.", ex);
        }
        boolean doesConfigExist = checkForConfig();
        boolean doesFabricLinuxExist = checkForFabricLinux();
        boolean doesFabricWindowsExist = checkForFabricWindows();
        boolean doesForgeLinuxExist = checkForForgeLinux();
        boolean doesForgeWindowsExist = checkForForgeWindows();
        boolean doesPropertiesExist = checkForProperties();
        boolean doesIconExist = checkForIcon();
        if (doesConfigExist || doesFabricLinuxExist || doesFabricWindowsExist || doesForgeLinuxExist || doesForgeWindowsExist || doesPropertiesExist || doesIconExist) {
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
    /** Check for old config file, if found rename to new name. If neither old nor new config file can be found, a new config file is generated.
     * @return Boolean. Returns true if new config file was generated.
     */
    private static boolean checkForConfig() {
        boolean firstRun = false;
        if (Reference.oldConfigFile.exists()) {
            try {
                Files.copy(Reference.oldConfigFile.getAbsoluteFile().toPath(), Reference.configFile.getAbsoluteFile().toPath());
                boolean isOldConfigDeleted = Reference.oldConfigFile.delete();
                if (isOldConfigDeleted) {
                    appLogger.info("creator.conf migrated to serverpackcreator.conf");
                }
            } catch (IOException ex) {
                appLogger.error("Error renaming creator.conf to serverpackcreator.conf", ex);
            }
        } else if (!Reference.configFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/" + Reference.configFile.getName()));
                Files.copy(link, Reference.configFile.getAbsoluteFile().toPath());
                link.close();
                appLogger.info("serverpackcreator.conf generated. Please customize.");
                firstRun = true;
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default config-file", ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }
    /** Checks for existence of Fabric start script for Linux. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated.
     */
    private static boolean checkForFabricLinux() {
        boolean firstRun = false;
        if (!Reference.fabricLinuxFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + Reference.fabricLinuxFile.getName()));
                Files.copy(link, Paths.get("./server_files/" + Reference.fabricLinuxFile));
                link.close();
                appLogger.info("start-fabric.sh generated. Please customize if you intend on using it.");
                firstRun = true;
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default Fabric Linux start file", ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }
    /** Checks for existence of Fabric start script for Windows. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated.
     */
    private static boolean checkForFabricWindows() {
        boolean firstRun = false;
        if (!Reference.fabricWindowsFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + Reference.fabricWindowsFile.getName()));
                Files.copy(link, Paths.get("./server_files/" + Reference.fabricWindowsFile));
                link.close();
                appLogger.info("start-fabric.bat generated. Please customize if you intend on using it.");
                firstRun = true;
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default Fabric Windows start file", ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }
    /** Checks for existence of Forge start script for Linux. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated.
     */
    private static boolean checkForForgeLinux() {
        boolean firstRun = false;
        if (!Reference.forgeLinuxFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + Reference.forgeLinuxFile.getName()));
                Files.copy(link, Paths.get("./server_files/" + Reference.forgeLinuxFile));
                link.close();
                appLogger.info("start-forge.sh generated. Please customize if you intend on using it.");
                firstRun = true;
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default Forge Linux start file", ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }
    /** Checks for existence of Forge start script for Windows. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated.
     */
    private static boolean checkForForgeWindows() {
        boolean firstRun = false;
        if (!Reference.forgeWindowsFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + Reference.forgeWindowsFile.getName()));
                Files.copy(link, Paths.get("./server_files/" + Reference.forgeWindowsFile));
                link.close();
                appLogger.info("start-forge.bat generated. Please customize if you intend on using it.");
                firstRun = true;
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default Forge Windows start file", ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }
    /** Checks for existence of server.properties file. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated.
     */
    private static boolean checkForProperties() {
        boolean firstRun = false;
        if (!Reference.propertiesFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + Reference.propertiesFile.getName()));
                Files.copy(link, Paths.get("./server_files/" + Reference.propertiesFile));
                link.close();
                appLogger.info("server.properties generated. Please customize if you intend on using it.");
                firstRun = true;
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default server.properties file", ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }
    /** Checks for existence of server-icon.png file. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated.
     */
    private static boolean checkForIcon() {
        boolean firstRun = false;
        if (!Reference.iconFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream("/server_files/" + Reference.iconFile.getName()));
                Files.copy(link, Paths.get("./server_files/" + Reference.iconFile));
                link.close();
                appLogger.info("server-icon.png generated. Please customize if you intend on using it.");
                firstRun = true;
            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error("Could not extract default server-icon.png file", ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }
}