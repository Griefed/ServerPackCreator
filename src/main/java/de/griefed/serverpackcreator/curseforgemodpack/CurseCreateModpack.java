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
package de.griefed.serverpackcreator.curseforgemodpack;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
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

/**
 * <strong>Table of methods</strong><br>
 * {@link #CurseCreateModpack(LocalizationManager)}<br>
 * {@link #setModloaderCase(String)}<br>
 * {@link #curseForgeModpack(String, Integer, Integer)}<br>
 * {@link #initializeModpack(String, Integer, Integer)}<br>
 * {@link #downloadMods(String)}<br>
 * {@link #copyOverride(String)}<br>
 * {@link #checkCurseForgeDir(String)}<br>
 * {@link #unzipArchive(String, String)}<br>
 * {@link #newFile(File, ZipEntry)}<br>
 * {@link #cleanupEnvironment(String)}<p>
 * Download a modpack from CurseForge and create it by unzipping the ZIP-archive, copy all folders and files from the
 * override directory to the parent directory, download all mods in said modpack, and delete no longer needed files.
 * Modpacks are create in a ProjectName/FileDisplayName structure. Before a modpack is created, the FileDisplayName folder
 * is checked for existence and deleted should it already exist. The reason for this is the order in which ServerPackCreator
 * checks and uses the configuration file.<p>
 * First: the configuration is checked for a valid CurseForge projectID,fileID combination.<br>
 * Second: The folder structure is checked for an already existing folder with FileDisplayName and if it exists it is deleted.<br>
 * Third: The modpack is created and all mods are downloaded etc., files are copied, etc.<br>
 * Fourth: Information about the modpack is acquired from the modpack's manifest.json and written to a new configuration file
 * with said information. The configuration for modpackDir, which previously contained a projectID,fileID is replaced
 * with the path to the new modpack at ProjectName/FileDisplayName.<br>
 * If modpackDir holds a projectID,fileID combination we have to assume the modpack has yet to be created, thus
 * we need to make sure the target directory is empty in order to create a clean and fresh modpack.<br>
 * If modpackDir does not hold a projectID,fileID we have to assume it is a path pointing at a directory which already
 * contains a modpack we can work with and create a server pack from. Thus, we clean up the environment if modpackDir holds
 * a projectID,fileID.
 */
public class CurseCreateModpack {
    private static final Logger LOG = LogManager.getLogger(CurseCreateModpack.class);

    private final LocalizationManager LOCALIZATIONMANAGER;

    private String projectName;
    private String fileName;
    private String fileDiskName;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     */
    public CurseCreateModpack(LocalizationManager injectedLocalizationManager) {
        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager();
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }
    }

    /**
     * Getter for the CurseForge project name.
     * @return String. Returns the name of the CurseForge project name.
     */
    String getProjectName() {
        return projectName;
    }

    /**
     * Setter for the name of the CurseForge project name.
     * @param newProjectID The ID of the new CurseForge project.
     */
    void setProjectName(int newProjectID) {
        String newProjectName;
        try {
            if (CurseAPI.project(newProjectID).isPresent()) {
                newProjectName = CurseAPI.project(newProjectID).get().name();
            } else {
                newProjectName = String.valueOf(newProjectID);
            }
        } catch (CurseException cex) {
            LOG.error(cex);
            newProjectName = String.valueOf(newProjectID);
        }
        this.projectName = newProjectName;
    }

    /**
     * Setter for the CurseForge file name and file disk name.
     * @param newProjectID The ID of the CurseForge project.
     * @param newFileID The ID of the CurseForge file.
     */
    void setFileNameAndDiskName(int newProjectID, int newFileID) {
        String newFileName;
        String newFileDiskName;

        try {
            if (CurseAPI.project(newProjectID).isPresent()) {
                try {
                    newFileName = Objects.requireNonNull(CurseAPI.project(newProjectID).get().files().fileWithID(newFileID)).displayName();
                }
                catch (NullPointerException npe) {
                    newFileName = Objects.requireNonNull(CurseAPI.project(newProjectID).get().files().fileWithID(newFileID)).nameOnDisk();
                }

                newFileDiskName = Objects.requireNonNull(CurseAPI.project(newProjectID).get().files().fileWithID(newFileID)).nameOnDisk();

            } else {
                newFileName = String.valueOf(newFileID);
                newFileDiskName = String.valueOf(newFileID);
            }

        } catch (CurseException cex) {
            LOG.error(cex);
            newFileName = String.valueOf(newFileID);
            newFileDiskName = String.valueOf(newFileID);
        }

        this.fileDiskName = newFileDiskName;
        this.fileName = newFileName;
    }

    /**
     * Getter for the CurseForge file name.
     * @return String. Returns the file name of the CurseForge project.
     */
    String getFileName() {
        return fileName;
    }

    /**
     * Getter for the CurseForge file disk name.
     * @return String. Returns the file disk name of the CurseForge file.
     */
    String getFileDiskName() {
        return fileDiskName;
    }

    /**
     * Ensures the modloader is normalized to first letter upper case and rest lower case. Basically allows the user to
     * input Forge or Fabric in any combination of upper- and lowercase and ServerPackCreator will still be able to
     * work with the users input.
     * @param modloader String. The String to check for case-insensitive cases of either Forge or Fabric.
     * @return String. Returns a normalized String of the specified modloader.
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

    /**
     * Acquires the names of the CurseForge project and file. Should no filename exist, we will use the fileDiskName as
     * fallback to ensure we always have a folder-structure of projectName/FileDisplayName at hand in which the modpack
     * will be created. Calls<br>
     * {@link CurseAPI} and various methods of it in order to acquire information about the modpack.<br>
     * {@link #checkCurseForgeDir(String)}<br>
     * {@link #initializeModpack(String, Integer, Integer)}
     * @param modpackDir String. Combination of project name and file name. Created during download procedure
     *                  and later replaces the modpackDir variable in the configuration file.
     * @param projectID Integer. The ID of the project. Used to gather information about the CurseForge project and to
     *                 download the modpack.
     * @param fileID Integer. The ID of the file. Used to gather information about the CurseForge file and to download
     *              the modpack.
     * @return Boolean. Returns true if the modpack was successfully created.
     */
    public boolean curseForgeModpack(String modpackDir, Integer projectID, Integer fileID) {
        boolean modpackCreated = false;

        try {
            if (CurseAPI.project(projectID).isPresent()) {
                setProjectName(projectID);
                setFileNameAndDiskName(projectID, fileID);
            }
        } catch (CurseException cex) { LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.curseforgemodpack"), projectID, fileID), cex); }

        if (!checkCurseForgeDir(modpackDir) &&
                !getProjectName().equals(String.valueOf(projectID)) &&
                !getFileDiskName().equals(String.valueOf(fileID))) {


            initializeModpack(modpackDir, projectID, fileID);
            modpackCreated = true;
        }

        return modpackCreated;
    }

    /**
     * Downloads the specified file of the specified project to a directory which is the combination of the project
     * name and file display name. Unzips the downloaded modpack ZIP-archive, gathers and displays information about the
     * specified project/file and makes calls to methods which further setup the modpack. Calls<br>
     * {@link CurseAPI} and various methods of it to create the modpack.<br>
     * {@link #unzipArchive(String, String)}<br>
     * {@link #copyOverride(String)}<br>
     * {@link #downloadMods(String)}<br>
     * @param modpackDir String. Combination of project name and file name. Created during download procedure and later
     *                  replaces the modpackDir variable in the configuration file.
     * @param projectID Integer. The ID of the project. Used to gather information and to download the modpack.
     * @param fileID Integer. The ID of the file. Used to gather information and to download the modpack.
     */
    void initializeModpack(String modpackDir, Integer projectID, Integer fileID) {
        try {
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.initializemodpack.download"), getProjectName(), getFileName()));

            CurseAPI.downloadFileToDirectory(projectID, fileID, Paths.get(modpackDir));
        } catch (CurseException cex) {
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.initializemodpack.download"), getFileName(), getProjectName(), modpackDir));
        }

        unzipArchive(String.format("%s/%s", modpackDir, getFileDiskName()), modpackDir);
        boolean isFileDeleted = new File(String.format("%s/%s", modpackDir, getFileDiskName())).delete();
        if (isFileDeleted) { LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.initializemodpack.deletezip")); }

        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(String.format("%s/manifest.json", modpackDir)));
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            CurseModpack modpack = objectMapper.readValue(jsonData, CurseModpack.class);

            String[] minecraftLoaderVersions = modpack.getMinecraft().toString().split(",");
            String[] modLoaderVersion = minecraftLoaderVersions[1].replace("[", "").replace("]", "").split("-");

            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.initializemodpack.infoheader"));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.initializemodpack.modpackname"), modpack.getName()));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.initializemodpack.modpackversion"), modpack.getVersion()));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.initializemodpack.modpackauthor"), modpack.getAuthor()));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.initializemodpack.modpackminecraftversion"), minecraftLoaderVersions[0].replace("[", "")));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.initializemodpack.modloader"), setModloaderCase(modLoaderVersion[0])));
            LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.initializemodpack.modloaderversion"), modLoaderVersion[1]));

        } catch (IOException ex) { LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.initializemodpack.readmodpack"), ex); }

        copyOverride(modpackDir);
        if (new File(String.format("%s/overrides", modpackDir)).isDirectory()) {
            try {
                Path pathToBeDeleted = Paths.get(String.format("%s/overrides", modpackDir));
                //noinspection ResultOfMethodCallIgnored
                Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException ex) {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.initializemodpack.overrides"));
            }
        }

        downloadMods(modpackDir);
    }

    /**
     * Downloads all mods specified in the modpack's manifest.json file. If a download of a mod fails, it will be
     * retried once before treating it as "currently unavailable" and adding the URL to the failed download
     * to a list which will be printed to the console and logs after the method has finished. The user will need to
     * download these failed mods themself as ServerPackCreator couldn't, for whatever reason, successfully download them.
     * If the acquisition of the download URL fails as well....well we're out of luck, then. The user will have to figure
     * this out on their own. Possible reasons for a failed download and failed URL acquisition might be that the file
     * was taken down, no longer exists, CurseForge is unavailable etc. etc. There's nothing we can do about that.
     * @param modpackDir String. All mods are downloaded to the child-directory "mods" inside the modpack directory.
     */
    void downloadMods(String modpackDir) {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.downloadmods.info"));
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
                    LOG.info(curseSplines.reticulate());
                }
                String[] mods = curseModpack.getFiles().get(i).toString().split(",");

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
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.downloadmods.curseforgeinfo"), cex);
                }

                try {

                    LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.downloadmods.specificmod"), i+1, curseModpack.getFiles().size(), modName, modFileName));
                    //Download mod
                    CurseAPI.downloadFileToDirectory(modID, fileID, Paths.get(String.format("%s/mods", modpackDir)));

                    try { Thread.sleep(1000); }
                    catch (InterruptedException iex) { LOG.debug(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.debug.downloadmods.sleep"), iex); }

                } catch (CurseException cex) {
                    LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.downloadmods.errordownload"), modName, modID, modFileName, fileID));

                    try {

                        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.downloadmods.tryagain"), modName, modID, modFileName, fileID));
                        //Retry download if previous attempt failed
                        CurseAPI.downloadFileToDirectory(modID, fileID, Paths.get(String.format("%s/mods", modpackDir)));

                    } catch (CurseException cex2) {

                        LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.downloadmods.retryfail"), modName, modID, modFileName, fileID));

                        try {
                            //Add URL of failed download to list
                            failedDownloads.add(String.format("Mod: %s, ID: %d. File: %s, ID: %d, URL: %s", modName, modID, modFileName, fileID, CurseAPI.fileDownloadURL(modID, fileID)));

                        } catch (CurseException cex3) {

                            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.downloadmods.urlfail"));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.downloadmods.fail"));
        }
        if (failedDownloads.size() != 0) {
            //Print the URLs of failed downloads, if there are any
            for (int i = 0; i <= failedDownloads.size(); i++) {
                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.downloadmods.urllist"), failedDownloads.get(i)));
            }
        }
    }

    /**
     * Recursively copy all folders and files from the override directory to the parent directory, our modpack directory.
     * @param modpackDir String. The overrides directory resides in this directory. All folders and files within overrides
     *                  are copied to the parent directory, the modpack directory.
     */
    void copyOverride(String modpackDir) {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.copyoverrides.info"));
        try {
            Stream<Path> files = Files.walk(Paths.get(String.format("%s/overrides", modpackDir)));
            files.forEach(file -> {
                try {
                    Files.copy(file, Paths.get(modpackDir).resolve(Paths.get(String.format("%s/overrides", modpackDir)).relativize(file)), REPLACE_EXISTING);
                    LOG.debug(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.debug.copyoverrides.status"), file.toAbsolutePath()));
                } catch (IOException ex) {
                    if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                        LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.copyoverrides.copy"), ex);
                    }
                }
            });
            files.close();
        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.copyoverrides.copy"), ex);
        }
    }

    /**
     * Check whether the folder for the specified CurseForge projectID/fileID exists and if it does exist, delete it
     * recursively to ensure we are working with a clean environment when creating a modpack from CurseForge.<br>
     * Calls {@link #cleanupEnvironment(String)} to ensure a clean environment when we create a new modpack from CurseForge.
     * @param modpackDir String. The path to the modpack directory, a combination of project name and file display name.
     * @return Boolean. Returns true if something went wrong during the cleanup of the modpack directory. If the cleanup
     * procedure finished successfully and we have a clean environment, false is returned. Returns false if the modpack
     * directory could not be found, indicating a clean environment.
     */
    boolean checkCurseForgeDir(String modpackDir) {
        boolean isModpackPresent = false;
        if (!(new File(modpackDir).isDirectory()) && !(new File(String.format("%s/manifest.json", modpackDir)).exists())) {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.checkcurseforgedir.create"));
        } else {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.checkcurseforgedir"));
            isModpackPresent = cleanupEnvironment(modpackDir);
        }
        return isModpackPresent;
    }

    /**
     * With help from <a href=https://www.baeldung.com/java-compress-and-uncompress>Baeldung Java Tutorials</a>.<br>
     * Unzips the downloaded modpack ZIP-archive to the specified directory. Calls {@link #newFile(File, ZipEntry)} as
     * a helper method.
     * @param zipFile String. The path to the ZIP-archive which we want to unzip.
     * @param modpackDir The directory into which the ZIP-archive will be unzipped into.
     */
    void unzipArchive(String zipFile, String modpackDir) {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.unziparchive"));
        File destDir = new File(modpackDir);
        byte[] buffer = new byte[1024];
        try {
            ZipInputStream input = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry = input.getNextEntry();
            while (zipEntry != null) {
                final File newFile = newFile(destDir, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.unziparchive.createdir"), newFile));
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.unziparchive.createdir"), parent));
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
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.unziparchive.extract"), zipFile), ex);
        }
    }

    /**
     * With help from <a href=https://www.baeldung.com/java-compress-and-uncompress>Baeldung Java Tutorials</a>.<br>
     * Helper-Method for unzipArchive. This method guards us against writing any file to the file system <em>outside</em>
     * of the target folder, thus ensuring all files are successfully unzipped into the desired target directory.
     * @param destinationDir The directory into which a file from the ZIP-archive is to be unzipped.
     * @param zipEntry File in the ZIP-archive which is to be unzipped from the ZIP-archive into the desired target
     *                 directory.
     * @return File. Returns a file from the ZIP-archive with the path pointing to the desired directory, ensuring proper
     * unzipping of the ZIP-archive.
     */
    private File newFile(File destinationDir, ZipEntry zipEntry) {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = null;
        String destFilePath = null;

        try {
            destDirPath = destinationDir.getCanonicalPath();

        } catch (IOException ex) {
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.newfile.path"), destinationDir), ex);
        }
        try {
            destFilePath = destFile.getCanonicalPath();
        } catch (IOException ex) {
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.newfile.path"), destFile), ex);
        }
        if (destFilePath != null && !destFilePath.startsWith(destDirPath + File.separator)) {
            LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.newfile.outside"), zipEntry.getName()));
        }
        return destFile;
    }

    /**
     * Deletes any and all folder and files, recursively, inside the target directory, thus ensuring we are working in a
     * clean environment when creating a new modpack from CurseForge.
     * @param modpackDir String. The directory we want to delete.
     * @return Boolean. Returns false if every file and folder was, recursively and successfully, deleted.
     */
    boolean cleanupEnvironment(String modpackDir) {
        boolean cleanedUp = false;
        if (new File(modpackDir).exists()) {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.cleanupenvironment.enter"));
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
                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.error.cleanupenvironment"), modpackDir));
            } finally {
                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createmodpack.log.info.cleanupenvironment.complete"));
            }
        }
        return cleanedUp;
    }
}