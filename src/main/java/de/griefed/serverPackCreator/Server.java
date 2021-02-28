package de.griefed.serverPackCreator;

import net.fabricmc.installer.server.ServerInstaller;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.LauncherMeta;
import net.fabricmc.installer.util.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
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
        // With help from https://stackoverflow.com/questions/1091788/how-to-create-a-zip-file-in-java
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
                        errorLogger.error("There was an error during zip creation", ex);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            outputStream.close();
        } catch (IOException ex) {
            errorLogger.error("There was an error during zip creation", ex);
        }
    }

    public static void installServer(String modLoader, String modpackDir, String minecraftVersion, String modLoaderVersion, String javaPath) {
        File serverDir = new File(modpackDir + "/server_pack");
        if (modLoader.equals("Fabric")) {
            try {
                appLogger.info("################################################################");
                appLogger.info("#                Starting Fabric installation                  #");
                appLogger.info("################################################################");
                // Feels like a dirty hack, but it works...
                // Code from https://github.com/FabricMC/fabric-installer/blob/master/src/main/java/net/fabricmc/installer/server/ServerHandler.java
                // With permission https://github.com/FabricMC/fabric-installer/issues/63#issuecomment-787103410
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
            try {
                appLogger.info("################################################################");
                appLogger.info("#                Starting Forge installation                   #");
                appLogger.info("################################################################");
                appLogger.info("Downloading https://files.minecraftforge.net/maven/net/minecraftforge/forge/" + minecraftVersion + "-" + modLoaderVersion + "/forge-" + minecraftVersion + "-" + modLoaderVersion + "-installer.jar");
                URL downloadForge = new URL("https://files.minecraftforge.net/maven/net/minecraftforge/forge/" + minecraftVersion + "-" + modLoaderVersion + "/forge-" + minecraftVersion + "-" + modLoaderVersion + "-installer.jar");
                ReadableByteChannel readableByteChannel = Channels.newChannel(downloadForge.openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(modpackDir + "/server_pack/forge-installer.jar");
                FileChannel fileChannel = fileOutputStream.getChannel();
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                fileOutputStream.flush();
                fileOutputStream.close();
                File forgeInstaller = new File(modpackDir + "/server_pack/forge-installer.jar");
                if (forgeInstaller.exists()) {
                    ProcessBuilder builder = new ProcessBuilder("\"" + javaPath + "/bin/java\"", "-jar", "forge-installer.jar", "--installServer").directory(new File(modpackDir + "/server_pack"));
                    builder.redirectErrorStream(true);
                    Process p = builder.start();
                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    while (true) {
                        line = r.readLine();
                        if (line == null) { break; }
                        appLogger.info(line);
                    }
                }
                Files.copy(Paths.get(modpackDir + "/server_pack/forge-" + minecraftVersion + "-" + modLoaderVersion + ".jar"), Paths.get(modpackDir + "/server_pack/forge.jar"), REPLACE_EXISTING);
                boolean isOldJarDeleted = (new File(modpackDir + "/server_pack/forge-" + minecraftVersion + "-" + modLoaderVersion + ".jar")).delete();
                if ((isOldJarDeleted) && (new File(modpackDir + "/server_pack/forge.jar").exists())) {
                    appLogger.info("Renamed forge.jar and deleted old one.");
                } else {
                    errorLogger.error("There was an error during renaming or deletion of the forge server jar.");
                }
                boolean isInstallerDeleted = (new File(modpackDir + "/server_pack/forge-installer.jar")).delete();
                if (isInstallerDeleted) {
                    appLogger.info("Deleted " + forgeInstaller.getName());
                } else {
                    errorLogger.error("Could not delete " + forgeInstaller.getName());
                }
                appLogger.info("################################################################");
                appLogger.info("#        Forge installation complete. Returning to SPC.        #");
                appLogger.info("################################################################");
            } catch (IOException ex) {
                errorLogger.error("An error occurred during Forge installation.", ex);
            }

        } else {
            errorLogger.error("Specified invalid modloader: " + modLoader);
        }
    }
}
