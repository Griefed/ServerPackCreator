package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ServerSetup {
    private static final Logger appLogger = LogManager.getLogger(FilesSetup.class);
    private static final Logger installerLogger = LogManager.getLogger("InstallerLogger");
    /** Installs the files for a Forge/Fabric server.
     * @param modLoader String. The modloader for which to install the server.
     * @param modpackDir String. /server_pack The directory where the modloader server will be installed in.
     * @param minecraftVersion String. The Minecraft version for which to install the modloader and Minecraft server.
     * @param modLoaderVersion String. The modloader version for which to install the modloader and Minecraft server.
     * @param javaPath String. Path to Java installation needed to execute the Fabric and Forge installers.
     */
    void installServer(String modLoader, String modpackDir, String minecraftVersion, String modLoaderVersion, String javaPath) {
        File fabricInstaller = new File(String.format("%s/server_pack/fabric-installer.jar", modpackDir));
        File forgeInstaller = new File(String.format("%s/server_pack/forge-installer.jar", modpackDir));
        if (modLoader.equalsIgnoreCase("Fabric")) {
            try {
                appLogger.info(LocalizationManager.getLocalizedString("serversetup.log.info.installserver.fabric.enter"));
                if (Reference.serverUtilities.downloadFabricJar(modpackDir)) {
                    appLogger.info(LocalizationManager.getLocalizedString("serversetup.log.info.installserver.fabric.download"));
                    ProcessBuilder processBuilder = new ProcessBuilder(
                            javaPath,
                            "-jar",
                            "fabric-installer.jar",
                            "server",
                            String.format("-mcversion %s", minecraftVersion),
                            String.format("-loader %s", modLoaderVersion),
                            "-downloadMinecraft").directory(new File(String.format("%s/server_pack", modpackDir)));
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while (true) {
                        line = reader.readLine();
                        if (line == null) { break; }
                        installerLogger.info(line);
                    }
                    reader.close();
                    appLogger.info(LocalizationManager.getLocalizedString("serversetup.log.info.installserver.fabric.details"));
                    appLogger.info(LocalizationManager.getLocalizedString("serversetup.log.info.installserver"));
                } else {
                    appLogger.error(LocalizationManager.getLocalizedString("serversetup.log.error.installserver.fabric"));
                }
            } catch (IOException ex) {
                appLogger.error(LocalizationManager.getLocalizedString("serversetup.log.error.installserver.fabricfail"), ex);
            }
        } else if (modLoader.equalsIgnoreCase("Forge")) {
            try {
                appLogger.info(LocalizationManager.getLocalizedString("serversetup.log.info.installserver.forge.enter"));
                if (Reference.serverUtilities.downloadForgeJar(minecraftVersion, modLoaderVersion, modpackDir)) {
                    appLogger.info(LocalizationManager.getLocalizedString("serversetup.log.info.installserver.forge.download"));
                    ProcessBuilder processBuilder = new ProcessBuilder(
                            javaPath,
                            "-jar",
                            "forge-installer.jar",
                            "--installServer")
                            .directory(new File(String.format("%s/server_pack", modpackDir)));
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while (true) {
                        line = reader.readLine();
                        if (line == null) { break; }
                        installerLogger.info(line);
                    }
                    reader.close();
                    appLogger.info(LocalizationManager.getLocalizedString("serversetup.log.info.installserver.forge.details"));
                    appLogger.info(LocalizationManager.getLocalizedString("serversetup.log.info.installserver"));
                } else {
                    appLogger.error(LocalizationManager.getLocalizedString("serversetup.log.error.installserver.forge"));
                }
            } catch (IOException ex) {
                appLogger.error(LocalizationManager.getLocalizedString("serversetup.log.error.installserver.forgefail"), ex);
            }
        } else {
            appLogger.error(String.format(LocalizationManager.getLocalizedString("serversetup.log.error.installserver"), modLoader));
        }
        Reference.serverUtilities.generateDownloadScripts(modLoader, modpackDir, minecraftVersion);
        Reference.serverUtilities.cleanUpServerPack(
                fabricInstaller,
                forgeInstaller,
                modLoader,
                modpackDir,
                minecraftVersion, modLoaderVersion);
    }
    /** Create a zip-archive of the serverpack, excluding Mojang's minecraft_server.jar.
     * With help from https://stackoverflow.com/questions/1091788/how-to-create-a-zip-file-in-java
     * @param modpackDir String. The directory where the zip-archive will be created and saved in.
     * @param modLoader String. Determines the name of Minecraft#s server jar which will be deleted from the zip-archive.
     * @param includeServerInstallation Boolean. Determines whether the Minecraft server jar needs to be deleted from the zip-archive.
     */
    void zipBuilder(String modpackDir, String modLoader, Boolean includeServerInstallation) {
        final Path sourceDir = Paths.get(String.format("%s/server_pack", modpackDir));
        String zipFileName = sourceDir.toString().concat(".zip");
        appLogger.info(LocalizationManager.getLocalizedString("serversetup.log.info.zipbuilder.enter"));
        try {
            final ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    try {
                        Path targetFile = sourceDir.relativize(file);
                        outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                        byte[] bytes = Files.readAllBytes(file);
                        outputStream.write(bytes, 0, bytes.length);
                        outputStream.closeEntry();
                    } catch (IOException ex) {
                        appLogger.error(LocalizationManager.getLocalizedString("serversetup.log.error.zipbuilder.create"), ex);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            outputStream.close();
        } catch (IOException ex) {
            appLogger.error(LocalizationManager.getLocalizedString("serversetup.log.error.zipbuilder.create"), ex);
        }
        if (includeServerInstallation) {
            Reference.serverUtilities.deleteMinecraftJar(modLoader, modpackDir);
            appLogger.warn(LocalizationManager.getLocalizedString("serversetup.log.warn.zipbuilder.minecraftjar1"));
            appLogger.warn(LocalizationManager.getLocalizedString("serversetup.log.warn.zipbuilder.minecraftjar2"));
            appLogger.warn(LocalizationManager.getLocalizedString("serversetup.log.warn.zipbuilder.minecraftjar3"));
        }
        appLogger.info(LocalizationManager.getLocalizedString("serversetup.log.info.zipbuilder.finish"));
    }
}