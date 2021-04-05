package de.griefed.ServerPackCreator.CurseForgeModpack;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class CreateModpack {
    private static final Logger appLogger = LogManager.getLogger(CreateModpack.class);
    private static String projectName;
    private static String fileName;
    private static String fileDiskName;
    /** Gets the names of the specified project and file and makes calls to methods which create the modpack so we can then create a server pack from it.
     * @param modpackDir String. Combination of project name and file name. Created during download procedure and later added to config file.
     * @param projectID Integer. The ID of the project. Used to gather information and to download the modpack.
     * @param fileID Integer. The ID of the file. Used to gather information and to download the modpack.
     * @return Boolean. Returns true if the modpack was freshly created.
     */
    public static boolean curseForgeModpack(String modpackDir, Integer projectID, Integer fileID) {
        boolean modpackCreated = false;
        try {
            if (CurseAPI.project(projectID).isPresent()) {
                Optional<CurseProject> curseProject = CurseAPI.project(projectID);
                //noinspection OptionalGetWithoutIsPresent
                projectName = curseProject.get().name();
                try {
                    //noinspection ConstantConditions
                    fileName = curseProject.get().files().fileWithID(fileID).displayName();
                } catch (NullPointerException npe) {
                    //noinspection ConstantConditions
                    fileName = curseProject.get().files().fileWithID(fileID).nameOnDisk();
                }
                //noinspection ConstantConditions
                fileDiskName = curseProject.get().files().fileWithID(fileID).nameOnDisk();
            } else {
                projectName = projectID.toString();
                fileName = fileID.toString();
                fileDiskName = fileID.toString();
            }
        } catch (CurseException cex) {
        appLogger.error(String.format("Error: Could not retrieve either projectID %s or fileID %s. Please verify that they are correct.", projectID, fileID), cex);
        }
        if (!checkCurseForgeDir(modpackDir)) {
            initializeModpack(modpackDir, projectID, fileID);
            modpackCreated = true;
        }
        return modpackCreated;
    }
    /** Downloads the specified file of the specified project to a directory which is the combination of the project name and file display name. Unzips the downloaded modpack archive, gathers and displays information about the specified project/file and makes calls to methods which further setup the modpack.
     * @param modpackDir String. Combination of project name and file name. Created during download procedure and later added to config file.
     * @param projectID Integer. The ID of the project. Used to gather information and to download the modpack.
     * @param fileID Integer. The ID of the file. Used to gather information and to download the modpack.
     */
    private static void initializeModpack(String modpackDir, Integer projectID, Integer fileID) {
        try {
            appLogger.info(String.format("Downloading %s/%s.", projectName, fileName));
            CurseAPI.downloadFileToDirectory(projectID, fileID, Paths.get(modpackDir));
        } catch (CurseException cex) {
            appLogger.error(String.format("Error: Could not download file %s for project %s to directory %s.", fileName, projectName, modpackDir));
        }
        unzipArchive(String.format("%s/%s", modpackDir, fileDiskName), modpackDir);
        boolean isFileDeleted = new File(String.format("%s/%s", modpackDir, fileDiskName)).delete();
        if (isFileDeleted) {
            appLogger.info("Downloaded ZIP-file no longer needed. Deleted.");
        }
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(String.format("%s/manifest.json", modpackDir)));
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            Modpack modpack = objectMapper.readValue(jsonData, Modpack.class);
            String[] minecraftLoaderVersions = modpack.getMinecraft().toString().split(",");
            String[] modLoaderVersion = minecraftLoaderVersions[1].replace("[", "").replace("]", "").split("-");
            appLogger.info("CurseForge modpack manifest.json parsed.");
            appLogger.info(String.format("CurseForge modpack name: %s", modpack.getName()));
            appLogger.info(String.format("CurseForge modpack version: %s", modpack.getVersion()));
            appLogger.info(String.format("CurseForge modpack author: %s", modpack.getAuthor()));
            appLogger.info(String.format("CurseForge modpack Minecraft version: %s", minecraftLoaderVersions[0].replace("[", "")));
            appLogger.info(String.format("CurseForge modpack modloader: %s", setModloader(modLoaderVersion[0])));
            appLogger.info(String.format("CurseForge modpack modloader version: %s", modLoaderVersion[1]));
        } catch (IOException ex) {
            appLogger.error("Error: There was a fault during json parsing.", ex);
        }
        copyOverride(modpackDir);
        if (new File(String.format("%s/overrides", modpackDir)).isDirectory()) {
            try {
                Path pathToBeDeleted = Paths.get(String.format("%s/overrides", modpackDir));
                //noinspection ResultOfMethodCallIgnored
                Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException ex) {
                appLogger.info("Directory \"overrides\" not found. Skipping delete action...");
            }
        }
        downloadMods(modpackDir);
    }
    /** Downloads all mods specified in the modpack's manifest.json file. If a download fails, one retry will be made, if said retry fails, too, then the download url will be sent to the log.
     * @param modpackDir String. All mods are downloaded to a child directory 'mods'
     */
    private static void downloadMods(String modpackDir) {
        appLogger.info("Downloading mods...");
        List<String> failedDownloads = new ArrayList<>();
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(String.format("%s/manifest.json", modpackDir)));
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            Modpack modpack = objectMapper.readValue(jsonData, Modpack.class);
            Random randInt = new Random();
            for (int i = 0; i < modpack.getFiles().toArray().length; i++) {
                if (randInt.nextInt(modpack.getFiles().toArray().length)==i) {
                    appLogger.info(String.format("%s", Splines.getSplines()));
                }
                String[] mods = modpack.getFiles().get(i).toString().split(",");
                String modName, modFileName;
                modName = modFileName = "";
                int modID, fileID;
                modID = Integer.parseInt(mods[0]);
                fileID = Integer.parseInt(mods[1]);
                try {
                    //noinspection OptionalGetWithoutIsPresent
                    modName = CurseAPI.project(modID).get().name();
                    //noinspection OptionalGetWithoutIsPresent
                    modFileName = Objects.requireNonNull(CurseAPI.project(modID).get().files().fileWithID(fileID)).nameOnDisk();
                } catch (CurseException cex) {
                    appLogger.error("Error: Couldn't retrieve CurseForge project name and file name.", cex);
                }
                try {
                    appLogger.info(String.format("Downloading mod %d of %d: %s | %s.", i+1, modpack.getFiles().toArray().length, modName, modFileName));
                    CurseAPI.downloadFileToDirectory(modID, fileID, Paths.get(String.format("%s/mods", modpackDir)));
                    try {
                        //noinspection BusyWait
                        Thread.sleep(1000);
                    } catch (InterruptedException iex) {
                        appLogger.debug("Error during interruption event.", iex);
                    }
                } catch (CurseException cex) {
                    appLogger.error(String.format("Error: Could not download mod %s (ID %s) | %s (ID %s).", modName, modID, modFileName, fileID));
                    try {
                        appLogger.info(String.format("Trying again for mod %s (ID %s) | %s (ID %s).", modName, modID, modFileName, fileID));
                        CurseAPI.downloadFileToDirectory(modID, fileID, Paths.get(String.format("%s/mods", modpackDir)));
                    } catch (CurseException cex2) {
                        appLogger.error(String.format("Error: Retry of download for  %s (ID %s) | %s (ID %s).", modName, modID, modFileName, fileID));
                        try {
                            failedDownloads.add(String.format("Mod: %s, ID: %d. File: %s, ID: %d, URL: %s", modName, modID, modFileName, fileID, CurseAPI.fileDownloadURL(modID, fileID)));
                        } catch (CurseException cex3) {
                            appLogger.error("Error: An error occurred during URL retrieval.");
                        }
                    }
                }
            }
        } catch (IOException ex) {
            appLogger.error("Error: An error was encountered in the downloadMods method.");
        }
        if (failedDownloads.toArray().length != 0) {
            for (int i = 0; i <= failedDownloads.toArray().length; i++) {
                appLogger.error(String.format("Failed downloads detected. Try manually downloading them: %s", failedDownloads.get(i)));
            }
        }
    }
    /** Deletes all directories in the modpack directory as specified in an internal Array. Currently not used anywhere.
     * @param modpackDir String. The directory in which to deletes should be made.
     */
    @SuppressWarnings("unused")
    private static void deleteDirs(String modpackDir) {
        appLogger.info("Deleting directories not needed in server pack from modpack...");
        String[] dirsToBeDeleted = {"overrides", "packmenu", "resourcepacks"};
        for (String s : dirsToBeDeleted) {
            String deleteMe = (String.format("%s/%s", modpackDir, s));
            if (new File(deleteMe).isDirectory()) {
                try {
                    Path pathToBeDeleted = Paths.get(deleteMe);
                    //noinspection ResultOfMethodCallIgnored
                    Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                } catch (IOException ex) {
                    appLogger.info(String.format("Directory %s not found. Skipping delete action...", deleteMe));
                }
            }
        }
    }
    /** Copies all folders and the files therein to the parent modpack directory.
     * @param modpackDir String. The overrides directory resides in this directory. All folders and files within overrides are copied here.
     */
    private static void copyOverride(String modpackDir) {
        appLogger.info("Copying folders from overrides to modpack...");
        try {
            Stream<Path> files = java.nio.file.Files.walk(Paths.get(String.format("%s/overrides", modpackDir)));
            files.forEach(file -> {
                try {
                    Files.copy(file, Paths.get(modpackDir).resolve(Paths.get(String.format("%s/overrides", modpackDir)).relativize(file)), REPLACE_EXISTING);
                    appLogger.debug(String.format("Copying: %s", file.toAbsolutePath().toString()));
                } catch (IOException ex) {
                    if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                        appLogger.error("An error occurred copying files from overrides to parent directory.", ex);
                    }
                }
            });
            files.close();
        } catch (IOException ex) {
            appLogger.error("An error occurred copying files from overrides to parent directory.", ex);
        }
    }
    /** Check whether the folder for the specified CurseForge projectID/fileID exists.
     * @param modpackDir String. The path to the modpack directory, a combination of project name and file display name.
     * @return Boolean. Returns true if the directory exists. False if not.
     */
    private static boolean checkCurseForgeDir(String modpackDir) {
        boolean isModpackPresent = false;
        if (!(new File(modpackDir).isDirectory()) && !(new File(String.format("%s/manifest.json", modpackDir)).exists())) {
            appLogger.info("CurseForge directory doesn't exist. We will download your file and create the modpack.");
        } else {
            appLogger.info("CurseForge directory found.");
            isModpackPresent = true;
        }
        return isModpackPresent;
    }
    /** With help from: https://www.baeldung.com/java-compress-and-uncompress
     * Unzips the downloaded modpack archive to a directory.
     * @param zipFile String. The name of the zipfile to extract.
     * @param modpackDir The directory where the archive resides in and will be extracted to.
     */
    private static void unzipArchive(String zipFile, String modpackDir) {
        appLogger.info("Extracting modpack ZIP-file.");
        File destDir = new File(modpackDir);
        byte[] buffer = new byte[1024];
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                final File newFile = newFile(destDir, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        appLogger.error(String.format("Failed to create directory %s", newFile));
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        appLogger.error(String.format("Failed to create directory %s", parent));
                    }

                    final FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (IOException ex) {
            appLogger.error(String.format("Error: There was an error extracting the archive %s", zipFile), ex);
        }
    }
    /** Helper-Method for unzipArchive. With help from: https://www.baeldung.com/java-compress-and-uncompress
     * @param destinationDir Check whether the file is outside of the directory it is supposed to be in.
     * @param zipEntry Zip entry with which to check for location.
     * @return Returns the correct destination for the new file.
     */
    private static File newFile(File destinationDir, ZipEntry zipEntry) {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = null;
        String destFilePath = null;
        try {
            destDirPath = destinationDir.getCanonicalPath();

        } catch (IOException ex) {
            appLogger.error(String.format("Error: There was an error getting the path for %s", destinationDir), ex);
        }
        try {
            destFilePath = destFile.getCanonicalPath();
        } catch (IOException ex) {
            appLogger.error(String.format("Error: There was an error getting the path for %s", destFile.toString()), ex);
        }
        if (destFilePath != null && !destFilePath.startsWith(destDirPath + File.separator)) {
            appLogger.error(String.format("Entry is outside of the target dir: %s", zipEntry.getName()));
        }
        return destFile;
    }
    /** Standardize the specified modloader.
     * @param modloader String. If any case of Forge or Fabric was specified, return "Forge" or "Fabric", so users can enter "forge" or "fabric" or any combination of upper- and lowercase letters..
     * @return String. Returns a standardized String of the specified modloader.
     */
    static String setModloader(String modloader) {
        String returnLoader = null;
        if (modloader.equalsIgnoreCase("Forge")) {
            returnLoader = "Forge";
        } else if (modloader.equalsIgnoreCase("Fabric")) {
            returnLoader = "Fabric";
        }
        return returnLoader;
    }
}
