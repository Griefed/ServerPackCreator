package de.griefed.ServerPackCreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class CopyFiles {
    private static final Logger appLogger = LogManager.getLogger(CopyFiles.class);

    /** Deletes files from previous runs of ServerPackCreator.
     * @param modpackDir String. The directory in where to check for files from previous runs.
     */
    static void cleanupEnvironment(String modpackDir) {
        if (new File(String.format("%s/server_pack", modpackDir)).exists()) {
            appLogger.info("Found old server_pack. Cleaning up...");
            Path serverPack = Paths.get(String.format("%s/server_pack", modpackDir));
            try {
                Files.walkFileTree(serverPack,
                        new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult postVisitDirectory(
                                    Path dir, IOException exc) throws IOException {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            }
                            @Override
                            public FileVisitResult visitFile(
                                    Path file, BasicFileAttributes attrs)
                                    throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }
                        });
            } catch (IOException ex) {
                appLogger.error(String.format("Error deleting file from %s/server_pack", modpackDir));
            } finally {
                appLogger.info("Cleanup of previous server_pack completed.");
            }
        }
        if (new File(String.format("%s/server_pack.zip", modpackDir)).exists()) {
            appLogger.info("Found old server_pack.zip. Cleaning up...");
            boolean isZipDeleted = new File(String.format("%s/server_pack.zip", modpackDir)).delete();
            if (isZipDeleted) {
                appLogger.info("Old server_pack.zip deleted.");
            } else {
                appLogger.error("Error deleting old zip archive.");
            }
        }
    }

    /** Copies start scripts for Forge modloader into the server_pack folder.
     * @param modpackDir String. Files will be copied into subfolder server_pack. Checks for valid modpackDir are in ConfigCheck.
     * @param modLoader String. Determines whether start scripts for Forge or Fabric are copied to modpackDir. Checks for valid modLoader are in ConfigCheck.
     * @param includeStartScripts Boolean. Whether to include start scripts in server_pack. Boolean.
     */
    static void copyStartScripts(String modpackDir, String modLoader, boolean includeStartScripts) {
        if (modLoader.equalsIgnoreCase("Forge") && includeStartScripts) {
            appLogger.info("Copying Forge start scripts...");
            try {
                Files.copy(
                        Paths.get(String.format("./server_files/%s", Reference.forgeWindowsFile)),
                        Paths.get(String.format("%s/server_pack/%s", modpackDir, Reference.forgeWindowsFile)),
                        REPLACE_EXISTING
                );
                Files.copy(
                        Paths.get(String.format("./server_files/%s", Reference.forgeLinuxFile)),
                        Paths.get(String.format("%s/server_pack/%s", modpackDir, Reference.forgeLinuxFile)),
                        REPLACE_EXISTING
                );
            } catch (IOException ex) {
                appLogger.error("An error occurred while copying files: ", ex);
            }
        } else if (modLoader.equalsIgnoreCase("Fabric") && includeStartScripts) {
            appLogger.info("Copying Fabric start scripts...");
            try {
                Files.copy(
                        Paths.get(String.format("./server_files/%s", Reference.fabricWindowsFile)),
                        Paths.get(String.format("%s/server_pack/%s", modpackDir, Reference.fabricWindowsFile)),
                        REPLACE_EXISTING
                );
                Files.copy(
                        Paths.get(String.format("./server_files/%s", Reference.fabricLinuxFile)),
                        Paths.get(String.format("%s/server_pack/%s", modpackDir, Reference.fabricLinuxFile)),
                        REPLACE_EXISTING
                );
            } catch (IOException ex) {
                appLogger.error("An error occurred while copying files: ", ex);
            }
        } else {
            appLogger.info("Specified invalid modloader. Must be either Forge or Fabric.");
        }
    }

    /** Copies all specified folders and their files to the modpackDir.
     * @param modpackDir String. /server_pack. Directory where all directories listed in copyDirs will be copied into.
     * @param copyDirs String List. The folders and files within to copy.
     * @param clientMods String List. List of clientside-only mods NOT to copy to server pack.
     * @throws IOException Only print stacktrace if it does not start with java.nio.file.DirectoryNotEmptyException.
     */
    static void copyFiles(String modpackDir, List<String> copyDirs, List<String> clientMods) throws IOException {
        String serverPath = String.format("%s/server_pack", modpackDir);
        Files.createDirectories(Paths.get(serverPath));
        for (int i = 0; i < copyDirs.toArray().length; i++) {
            String clientDir = String.format("%s/%s", modpackDir,copyDirs.get(i));
            String serverDir = String.format("%s/%s", serverPath,copyDirs.get(i));
            appLogger.info(String.format("Setting up %s files.", serverDir));
            if (copyDirs.get(i).startsWith("saves/")) {
                String savesDir = String.format("%s/%s", serverPath, copyDirs.get(i).substring(6));
                try {
                    Stream<Path> files = Files.walk(Paths.get(clientDir));
                    files.forEach(file -> {
                        try {
                            Files.copy(
                                    file,
                                    Paths.get(savesDir).resolve(Paths.get(clientDir).relativize(file)),
                                    REPLACE_EXISTING
                            );
                            appLogger.debug(String.format("Copying: %s", file.toAbsolutePath().toString()));
                        } catch (IOException ex) {
                            if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                                appLogger.error("An error occurred during copy operation.", ex);
                            }
                        }
                    });
                } catch (IOException ex) {
                    appLogger.error("An error occurred copying the specified world.", ex);
                }
            } else if (copyDirs.get(i).startsWith("mods") && clientMods.toArray().length > 0) {
                List<String> listOfFiles = excludeClientMods(clientDir, clientMods);
                Files.createDirectories(Paths.get(serverDir));
                for (int in = 0; in < listOfFiles.toArray().length; in++) {
                    try {
                        Files.copy(
                                Paths.get(listOfFiles.get(in)),
                                Paths.get(String.format("%s/%s",serverDir, new File(listOfFiles.get(in)).getName())),
                                REPLACE_EXISTING
                        );
                        appLogger.debug(String.format("Copying: %s", listOfFiles.get(in)));
                    } catch (IOException ex) {
                        if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                            appLogger.error("An error occurred copying files to the serverpack.", ex);
                        }
                    }
                }
            } else {
                try {
                    Stream<Path> files = Files.walk(Paths.get(clientDir));
                    files.forEach(file -> {
                        try {
                            Files.copy(
                                    file,
                                    Paths.get(serverDir).resolve(Paths.get(clientDir).relativize(file)),
                                    REPLACE_EXISTING
                            );
                            appLogger.debug(String.format("Copying: %s", file.toAbsolutePath().toString()));
                        } catch (IOException ex) {
                            if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                                appLogger.error("An error occurred copying files to the serverpack.", ex);
                            }
                        }
                    });
                    files.close();
                } catch (IOException ex) {
                    appLogger.error("An error occurred during the copy-procedure to the serverpack.", ex);
                }
            }
        }
    }

    /** Generate a list of all mods in a modpack EXCEPT clientside-only mods. This list is then used by copyFiles.
     * @param modsDir String. /mods The directory in which to generate a list of all available mods.
     * @param clientMods List String. A list of all clientside-only mods passed by copyFiles, which is then removed from the list generated in this method.
     * @return List String. A list of all mods inside the modpack excluding the specified clientside-only mods.
     */
    @SuppressWarnings("UnusedAssignment")
    private static List<String> excludeClientMods(String modsDir, List<String> clientMods) {
        appLogger.info("Preparing a list of mods to include in server pack...");
        String[] copyMods = new String[0];
        List<String> modpackModList = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Paths.get(modsDir))) {
            modpackModList = walk.filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toList());
            for (int in = 0; in < modpackModList.toArray().length; in++) {
                for (int i = 0; i < clientMods.toArray().length; i++) {
                    String modpackMod = modpackModList.get(in);
                    String clientMod = clientMods.get(i);
                    if (modpackMod.contains(clientMod)) {
                        modpackModList.remove(in);
                    }
                }
            }
            copyMods = modpackModList.toArray(new String[0]);
            return Arrays.asList(copyMods.clone());
        } catch (IOException ex) {
            appLogger.error("Error: There was an error during the acquisition of files in mods directory.", ex);
        }
        return Arrays.asList(copyMods.clone());
    }

    /** Copies the server-icon.png into server_pack.
     * @param modpackDir String. /server_pack. Directory where the server-icon.png will be copied to.
     */
    static void copyIcon(String modpackDir) {
        appLogger.info("Copying server-icon.png...");
        try {
            Files.copy(
                    Paths.get(String.format("./server_files/%s", Reference.iconFile)),
                    Paths.get(String.format("%s/server_pack/%s", modpackDir, Reference.iconFile)),
                    REPLACE_EXISTING
            );
        } catch (IOException ex) {
            appLogger.error("An error occurred trying to copy the server icon.", ex);
        }
    }

    /** Copies the server.properties into server_pack.
     * @param modpackDir String. /server_pack. Directory where the server.properties. will be copied to.
     */
    static void copyProperties(String modpackDir) {
        appLogger.info("Copying server.properties...");
        try {
            Files.copy(
                    Paths.get(String.format("./server_files/%s", Reference.propertiesFile)),
                    Paths.get(String.format("%s/server_pack/%s", modpackDir, Reference.propertiesFile)),
                    REPLACE_EXISTING
            );
        } catch (IOException ex) {
            appLogger.error("An error occurred trying to copy the server.properties-file.", ex);
        }
    }
}