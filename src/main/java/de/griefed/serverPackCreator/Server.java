package de.griefed.serverPackCreator;

import net.fabricmc.installer.server.ServerInstaller;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.LauncherMeta;
import net.fabricmc.installer.util.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static net.fabricmc.installer.util.InstallerProgress.CONSOLE;

public class Server {
    private static final Logger appLogger = LogManager.getLogger("Server");
    private static final Logger errorLogger = LogManager.getLogger("ServerError");

    // Delete clientside mods from serverpack
    public static void deleteClientMods(String modpackDir, List<String> clientMods) throws IOException {
        appLogger.info("Deleting client-side mods from serverpack: ");
        File serverMods = new File(modpackDir + "/server_pack/mods");
            for (File f : Objects.requireNonNull(serverMods.listFiles())) {
                for (int i = 0; i < clientMods.toArray().length; i++) {
                    if (f.getName().startsWith(clientMods.get(i))) {
                        boolean isDeleted = f.delete();
                        if (isDeleted) {
                            appLogger.info("    " + f.getName());
                        } else {
                            errorLogger.error("Could not delete: " + f.getName());
                        }
                    }
                }
            }
    }
    // Create zip archive of serverpack.
    public static void zipBuilder(String modpackDir) {
        appLogger.info("Creating zip archive of serverpack...");
        final Path sourceDir = Paths.get(modpackDir + "/server_pack");
        String zipFileName = sourceDir.toString().concat(".zip");
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
                        errorLogger.error(ex);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            outputStream.close();
        } catch (IOException ex) {
            errorLogger.error(ex);
        }
    }

    public static void installServer(String modLoader, String modpackDir, String minecraftVersion, String modLoaderVersion) {
        File serverDir = new File(modpackDir + "/server_pack");
        if (modLoader.equals("Fabric")) {
            try {
                appLogger.info("################################################################");
                appLogger.info("#                Starting Fabric installation                  #");
                appLogger.info("################################################################");
                // Feels like a dirty hack, but it works...
                ServerInstaller.install(serverDir, modLoaderVersion, minecraftVersion, CONSOLE);
                File serverJar = new File(serverDir, "server.jar");
                File serverJarTmp = new File(serverDir, "server.jar.tmp");
                Files.deleteIfExists((serverJar.toPath()));
                InstallerProgress.CONSOLE.updateProgress((Utils.BUNDLE.getString("progress.download.minecraft")));
                Utils.downloadFile(new URL(LauncherMeta.getLauncherMeta().getVersion(minecraftVersion).getVersionMeta().downloads.get("server").url), serverJarTmp.toPath());
                Files.move(serverJarTmp.toPath(), serverJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                InstallerProgress.CONSOLE.updateProgress(Utils.BUNDLE.getString("progress.done"));
                appLogger.info("################################################################");
                appLogger.info("#         Fabric installation complete. Returning to SPC.      #");
                appLogger.info("################################################################");
            } catch (IOException ex) {
                errorLogger.error("An error occurred during Fabric installation.", ex);
            }
        } else if (modLoader.equals("Forge")) {
            appLogger.info("Forge installation not yet implemented.");
            /*try {
                appLogger.info("################################################################");
                appLogger.info("#                Starting Forge installation                   #");
                appLogger.info("################################################################");


                appLogger.info("################################################################");
                appLogger.info("#        Forge installation complete. Returning to SPC.        #");
                appLogger.info("################################################################");
            } catch (IOException ex) {
                errorLogger.error("An error occurred during Forge installation.", ex);
            }
            */
        } else {
            errorLogger.error("Specified invalid modloader: " + modLoader);
        }
    }
}
