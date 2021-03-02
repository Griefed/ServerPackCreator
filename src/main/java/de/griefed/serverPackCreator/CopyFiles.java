package de.griefed.serverPackCreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class CopyFiles {
    private static final Logger appLogger = LogManager.getLogger("CopyFiles");
    private static final Logger errorLogger = LogManager.getLogger("CopyFilesError");

    // Copy forge/fabric start scripts to serverpack, depending on modLoader config.
    static void copyStartScripts(String modpackDir, String modLoader, boolean includeStartScripts) {
        if (modLoader.equals("Forge") && includeStartScripts) {
            appLogger.info("Copying Forge start scripts...");
            try {
                Files.copy(Paths.get("server_files/" + FilesSetup.forgeWindowsFile), Paths.get(modpackDir + "/server_pack/" + FilesSetup.forgeWindowsFile), REPLACE_EXISTING);
                Files.copy(Paths.get("server_files/" + FilesSetup.forgeLinuxFile), Paths.get(modpackDir + "/server_pack/" + FilesSetup.forgeLinuxFile), REPLACE_EXISTING);
            } catch (IOException ex) {
                errorLogger.error("An error occurred while copying files: ", ex);
            }
        } else if (modLoader.equals("Fabric") && includeStartScripts) {
            appLogger.info("Copying Fabric start scripts...");
            try {
                Files.copy(Paths.get("server_files/" + FilesSetup.fabricWindowsFile), Paths.get(modpackDir + "/server_pack/" + FilesSetup.fabricWindowsFile), REPLACE_EXISTING);
                Files.copy(Paths.get("server_files/" + FilesSetup.fabricLinuxFile), Paths.get(modpackDir + "/server_pack/" + FilesSetup.fabricLinuxFile), REPLACE_EXISTING);
            } catch (IOException ex) {
                errorLogger.error("An error occurred while copying files: ", ex);
            }
        } else {
            appLogger.info("Specified invalid modloader. Must be either Forge or Fabric.");
        }
    }

    // Copy all specified directories from modpack to serverpack.
    static void copyFiles(String modpackDir, List<String> copyDirs) throws IOException {
        String serverPath = modpackDir + "/server_pack";
        Files.createDirectories(Paths.get(serverPath));
        for (int i = 0; i<copyDirs.toArray().length; i++) {
            String clientDir = modpackDir + "/" + copyDirs.get(i);
            String serverDir = serverPath + "/" + copyDirs.get(i);
            appLogger.info("Setting up " + serverDir + " files.");
            if (copyDirs.get(i).startsWith("saves/")) {
                String savesDir = serverPath + "/" + copyDirs.get(i).substring(6);
                try {
                    Stream<Path> files = Files.walk(Paths.get(clientDir));
                    files.forEach(file -> {
                        try {
                            Files.copy(file, Paths.get(savesDir).resolve(Paths.get(clientDir).relativize(file)), REPLACE_EXISTING);
                            appLogger.info("Copying: " + file.toAbsolutePath().toString());
                        } catch (IOException ex) {
                            if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                                errorLogger.error(ex);
                            }
                        }
                    });
                } catch (IOException ex) {
                    errorLogger.error("An error occurred copying the specified world.", ex);
                }
            } else {
                try {
                    Stream<Path> files = Files.walk(Paths.get(clientDir));
                    files.forEach(file -> {
                        try {
                            Files.copy(file, Paths.get(serverDir).resolve(Paths.get(clientDir).relativize(file)), REPLACE_EXISTING);
                            appLogger.info("Copying: " + file.toAbsolutePath().toString());
                        } catch (IOException ex) {
                            if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                                errorLogger.error("An error occurred copying files to the serverpack.", ex);
                            }
                        }
                    });
                    files.close();
                } catch (IOException ex) {
                    errorLogger.error("An error occurred during the copy-procedure to the serverpack.", ex);
                }
            }
        }
    }

    // If true, copy server-icon to serverpack.
    static void copyIcon(String modpackDir) {
        appLogger.info("Copying server-icon.png...");
        try {
            Files.copy(Paths.get("server_files/" + FilesSetup.iconFile), Paths.get(modpackDir + "/server_pack/" + FilesSetup.iconFile), REPLACE_EXISTING);
        } catch (IOException ex) {
            errorLogger.error("An error occurred trying to copy the server icon.", ex);
        }
    }

    // If true, copy server.properties to serverpack.
    static void copyProperties(String modpackDir) {
        appLogger.info("Copying server.properties...");
        try {
            Files.copy(Paths.get("server_files/" + FilesSetup.propertiesFile), Paths.get(modpackDir + "/server_pack/" + FilesSetup.propertiesFile), REPLACE_EXISTING);
        } catch (IOException ex) {
            errorLogger.error("An error occurred trying to copy the server.properties-file.", ex);
        }
    }
}
