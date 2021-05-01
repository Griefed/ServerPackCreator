/* Copyright (C) 2021  Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
//TODO: Write table of contents
package de.griefed.serverpackcreator.curseforgemodpack;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
//TODO: Write docs for class
public class CurseCreateModpack {
    private static final Logger appLogger = LogManager.getLogger(CurseCreateModpack.class);
    private String projectName;
    private String fileName;
    private String fileDiskName;

    private LocalizationManager localizationManager;

    public CurseCreateModpack(LocalizationManager injectedLocalizationManager) {
        if (injectedLocalizationManager == null) {
            this.localizationManager = new LocalizationManager();
        } else {
            this.localizationManager = injectedLocalizationManager;
        }
    }

    /** Standardize the specified modloader.
     * @param modloader String. If any case of Forge or Fabric was specified, return "Forge" or "Fabric", so users can enter "forge" or "fabric" or any combination of upper- and lowercase letters..
     * @return String. Returns a standardized String of the specified modloader.
     */
    String setModloaderCase(String modloader) {
        String returnLoader = null;
        if (modloader.equalsIgnoreCase("Forge")) {
            returnLoader = "Forge";
        } else if (modloader.equalsIgnoreCase("Fabric")) {
            returnLoader = "Fabric";
        }
        return returnLoader;
    }

    /** Gets the names of the specified project and file and makes calls to methods which create the modpack so we can then create a server pack from it.
     * @param modpackDir String. Combination of project name and file name. Created during download procedure and later added to config file.
     * @param projectID Integer. The ID of the project. Used to gather information and to download the modpack.
     * @param fileID Integer. The ID of the file. Used to gather information and to download the modpack.
     * @return Boolean. Returns true if the modpack was freshly created.
     */
    @SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
    public boolean curseForgeModpack(String modpackDir, Integer projectID, Integer fileID) {
        boolean modpackCreated = false;

        try {
            if (CurseAPI.project(projectID).isPresent()) {
                Optional<CurseProject> curseProject = CurseAPI.project(projectID);

                this.projectName = curseProject.get().name();
                try { this.fileName = curseProject.get().files().fileWithID(fileID).displayName(); }
                catch (NullPointerException npe) { this.fileName = curseProject.get().files().fileWithID(fileID).nameOnDisk(); }
                this.fileDiskName = curseProject.get().files().fileWithID(fileID).nameOnDisk();

            } else {
                this.projectName = projectID.toString();
                this.fileName = fileID.toString();
                this.fileDiskName = fileID.toString();
            }

        } catch (CurseException cex) { appLogger.error(String.format(localizationManager.getLocalizedString("createmodpack.log.error.curseforgemodpack"), projectID, fileID), cex); }

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
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void initializeModpack(String modpackDir, Integer projectID, Integer fileID) {
        try {
            appLogger.info(String.format(localizationManager.getLocalizedString("createmodpack.log.info.initializemodpack.download"), projectName, fileName));

            CurseAPI.downloadFileToDirectory(projectID, fileID, Paths.get(modpackDir));
        } catch (CurseException cex) {
            appLogger.error(String.format(localizationManager.getLocalizedString("createmodpack.log.error.initializemodpack.download"), fileName, projectName, modpackDir));
        }

        unzipArchive(String.format("%s/%s", modpackDir, fileDiskName), modpackDir);
        boolean isFileDeleted = new File(String.format("%s/%s", modpackDir, fileDiskName)).delete();
        if (isFileDeleted) { appLogger.info(localizationManager.getLocalizedString("createmodpack.log.info.initializemodpack.deletezip")); }

        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(String.format("%s/manifest.json", modpackDir)));
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            CurseModpack modpack = objectMapper.readValue(jsonData, CurseModpack.class);

            String[] minecraftLoaderVersions = modpack.getMinecraft().toString().split(",");
            String[] modLoaderVersion = minecraftLoaderVersions[1].replace("[", "").replace("]", "").split("-");

            appLogger.info(localizationManager.getLocalizedString("createmodpack.log.info.initializemodpack.infoheader"));
            appLogger.info(String.format(localizationManager.getLocalizedString("createmodpack.log.info.initializemodpack.modpackname"), modpack.getName()));
            appLogger.info(String.format(localizationManager.getLocalizedString("createmodpack.log.info.initializemodpack.modpackversion"), modpack.getVersion()));
            appLogger.info(String.format(localizationManager.getLocalizedString("createmodpack.log.info.initializemodpack.modpackauthor"), modpack.getAuthor()));
            appLogger.info(String.format(localizationManager.getLocalizedString("createmodpack.log.info.initializemodpack.modpackminecraftversion"), minecraftLoaderVersions[0].replace("[", "")));
            appLogger.info(String.format(localizationManager.getLocalizedString("createmodpack.log.info.initializemodpack.modloader"), setModloaderCase(modLoaderVersion[0])));
            appLogger.info(String.format(localizationManager.getLocalizedString("createmodpack.log.info.initializemodpack.modloaderversion"), modLoaderVersion[1]));

        } catch (IOException ex) { appLogger.error(localizationManager.getLocalizedString("createmodpack.log.error.initializemodpack.readmodpack"), ex); }

        copyOverride(modpackDir);
        if (new File(String.format("%s/overrides", modpackDir)).isDirectory()) {
            try {
                Path pathToBeDeleted = Paths.get(String.format("%s/overrides", modpackDir));
                Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException ex) {
                appLogger.info(localizationManager.getLocalizedString("createmodpack.log.info.initializemodpack.overrides"));
            }
        }

        downloadMods(modpackDir);
    }

    /** Downloads all mods specified in the modpack's manifest.json file. If a download fails, one retry will be made, if said retry fails, too, then the download url will be sent to the log.
     * @param modpackDir String. All mods are downloaded to a child directory 'mods'
     */
    @SuppressWarnings({"OptionalGetWithoutIsPresent", "BusyWait"})
    private void downloadMods(String modpackDir) {
        appLogger.info(localizationManager.getLocalizedString("createmodpack.log.info.downloadmods.info"));
        List<String> failedDownloads = new ArrayList<>();

        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(String.format("%s/manifest.json", modpackDir)));

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

            CurseModpack curseModpack = objectMapper.readValue(jsonData, CurseModpack.class);
            Random randInt = new Random();
            CurseSplines curseSplines = new CurseSplines();
            for (int i = 0; i < curseModpack.getFiles().size(); i++) {
                if (randInt.nextInt(curseModpack.getFiles().size())==i) {
                    appLogger.info(curseSplines.reticulate());
                }
                String[] mods = curseModpack.getFiles().get(i).toString().split(",");

                String modName, modFileName;
                modName = modFileName = "";

                int modID, fileID;
                modID = Integer.parseInt(mods[0]);
                fileID = Integer.parseInt(mods[1]);

                try {
                    modName = CurseAPI.project(modID).get().name();
                    modFileName = Objects.requireNonNull(CurseAPI.project(modID).get().files().fileWithID(fileID)).nameOnDisk();
                } catch (CurseException cex) {
                    appLogger.error(localizationManager.getLocalizedString("createmodpack.log.error.downloadmods.curseforgeinfo"), cex);
                }
                try {
                    appLogger.info(String.format(localizationManager.getLocalizedString("createmodpack.log.info.downloadmods.specificmod"), i+1, curseModpack.getFiles().size(), modName, modFileName));
                    CurseAPI.downloadFileToDirectory(modID, fileID, Paths.get(String.format("%s/mods", modpackDir)));
                    try { Thread.sleep(1000); }
                    catch (InterruptedException iex) { appLogger.debug(localizationManager.getLocalizedString("createmodpack.log.debug.downloadmods.sleep"), iex); }
                } catch (CurseException cex) { appLogger.error(String.format(localizationManager.getLocalizedString("createmodpack.log.error.downloadmods.errordownload"), modName, modID, modFileName, fileID));
                    try {
                        appLogger.info(String.format(localizationManager.getLocalizedString("createmodpack.log.info.downloadmods.tryagain"), modName, modID, modFileName, fileID));
                        CurseAPI.downloadFileToDirectory(modID, fileID, Paths.get(String.format("%s/mods", modpackDir)));
                    } catch (CurseException cex2) {
                        appLogger.error(String.format(localizationManager.getLocalizedString("createmodpack.log.error.downloadmods.retryfail"), modName, modID, modFileName, fileID));
                        try {
                            failedDownloads.add(String.format("Mod: %s, ID: %d. File: %s, ID: %d, URL: %s", modName, modID, modFileName, fileID, CurseAPI.fileDownloadURL(modID, fileID)));
                        } catch (CurseException cex3) {
                            appLogger.error(localizationManager.getLocalizedString("createmodpack.log.error.downloadmods.urlfail"));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            appLogger.error(localizationManager.getLocalizedString("createmodpack.log.error.downloadmods.fail"));
        }
        if (failedDownloads.size() != 0) {
            for (int i = 0; i <= failedDownloads.size(); i++) {
                appLogger.error(String.format(localizationManager.getLocalizedString("createmodpack.log.error.downloadmods.urllist"), failedDownloads.get(i)));
            }
        }
    }

    /** Copies all folders and the files therein to the parent modpack directory.
     * @param modpackDir String. The overrides directory resides in this directory. All folders and files within overrides are copied here.
     */
    private void copyOverride(String modpackDir) {
        appLogger.info(localizationManager.getLocalizedString("createmodpack.log.info.copyoverrides.info"));
        try {
            Stream<Path> files = java.nio.file.Files.walk(Paths.get(String.format("%s/overrides", modpackDir)));
            files.forEach(file -> {
                try {
                    Files.copy(file, Paths.get(modpackDir).resolve(Paths.get(String.format("%s/overrides", modpackDir)).relativize(file)), REPLACE_EXISTING);
                    appLogger.debug(String.format(localizationManager.getLocalizedString("createmodpack.log.debug.copyoverrides.status"), file.toAbsolutePath().toString()));
                } catch (IOException ex) {
                    if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                        appLogger.error(localizationManager.getLocalizedString("createmodpack.log.error.copyoverrides.copy"), ex);
                    }
                }
            });
            files.close();
        } catch (IOException ex) {
            appLogger.error(localizationManager.getLocalizedString("createmodpack.log.error.copyoverrides.copy"), ex);
        }
    }

    /** Check whether the folder for the specified CurseForge projectID/fileID exists.
     * @param modpackDir String. The path to the modpack directory, a combination of project name and file display name.
     * @return Boolean. Returns true if the directory exists. False if not.
     */
    private boolean checkCurseForgeDir(String modpackDir) {
        boolean isModpackPresent = false;
        if (!(new File(modpackDir).isDirectory()) && !(new File(String.format("%s/manifest.json", modpackDir)).exists())) {
            appLogger.info(localizationManager.getLocalizedString("createmodpack.log.info.checkcurseforgedir.create"));
        } else {
            appLogger.info(localizationManager.getLocalizedString("createmodpack.log.info.checkcurseforgedir"));
            isModpackPresent = cleanupEnvironment(modpackDir);
        }
        return isModpackPresent;
    }

    /** With help from: https://www.baeldung.com/java-compress-and-uncompress
     * Unzips the downloaded modpack archive to a directory.
     * @param zipFile String. The name of the zipfile to extract.
     * @param modpackDir The directory where the archive resides in and will be extracted to.
     */
    private void unzipArchive(String zipFile, String modpackDir) {
        appLogger.info(localizationManager.getLocalizedString("createmodpack.log.info.unziparchive"));
        File destDir = new File(modpackDir);
        byte[] buffer = new byte[1024];
        try {
            ZipInputStream input = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry = input.getNextEntry();
            while (zipEntry != null) {
                final File newFile = newFile(destDir, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        appLogger.error(String.format(localizationManager.getLocalizedString("createmodpack.log.error.unziparchive.createdir"), newFile));
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        appLogger.error(String.format(localizationManager.getLocalizedString("createmodpack.log.error.unziparchive.createdir"), parent));
                    }
                    final FileOutputStream output = new FileOutputStream(newFile);
                    int length;
                    while ((length = input.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }
                    output.close();
                }
                zipEntry = input.getNextEntry();
            }
            input.closeEntry();
            input.close();
        } catch (IOException ex) {
            appLogger.error(String.format(localizationManager.getLocalizedString("createmodpack.log.error.unziparchive.extract"), zipFile), ex);
        }
    }

    /** Helper-Method for unzipArchive. With help from: https://www.baeldung.com/java-compress-and-uncompress
     * @param destinationDir Check whether the file is outside of the directory it is supposed to be in.
     * @param zipEntry Zip entry with which to check for location.
     * @return Returns the correct destination for the new file.
     */
    private File newFile(File destinationDir, ZipEntry zipEntry) {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = null;
        String destFilePath = null;

        try {
            destDirPath = destinationDir.getCanonicalPath();

        } catch (IOException ex) {
            appLogger.error(String.format(localizationManager.getLocalizedString("createmodpack.log.error.newfile.path"), destinationDir), ex);
        }
        try {
            destFilePath = destFile.getCanonicalPath();
        } catch (IOException ex) {
            appLogger.error(String.format(localizationManager.getLocalizedString("createmodpack.log.error.newfile.path"), destFile.toString()), ex);
        }
        if (destFilePath != null && !destFilePath.startsWith(destDirPath + File.separator)) {
            appLogger.error(String.format(localizationManager.getLocalizedString("createmodpack.log.error.newfile.outside"), zipEntry.getName()));
        }
        return destFile;
    }

    /** Deletes all files, directories and ZIP-archives of previously generated server packs to ensure newly generated
     * server pack is as clean as possible.
     * @param modpackDir String. The server_pack directory and ZIP-archive will be deleted inside the modpack directory.
     * @return Boolean. Returns false if every file and folder was successfully deleted.
     */
    private boolean cleanupEnvironment(String modpackDir) {
        boolean cleanedUp = false;
        if (new File(modpackDir).exists()) {
            appLogger.info(localizationManager.getLocalizedString("createmodpack.log.info.cleanupenvironment.enter"));
            Path modpackPath = Paths.get(modpackDir);
            try {
                Files.walkFileTree(modpackPath,
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
                cleanedUp = true;
                appLogger.error(String.format(localizationManager.getLocalizedString("createmodpack.log.error.cleanupenvironment"), modpackDir));
            } finally {
                appLogger.info(localizationManager.getLocalizedString("createmodpack.log.info.cleanupenvironment.complete"));
            }
        }
        return cleanedUp;
    }
}