package de.griefed.serverPackCreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ServerSetup {
    private static final Logger appLogger = LogManager.getLogger("ServerSetup");
    private static final Logger errorLogger = LogManager.getLogger("ServerSetupError");

    // Delete clientside mods from serverpack
    static void deleteClientMods(String modpackDir, List<String> clientMods) throws IOException {
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

    static void installServer(String modLoader, String modpackDir, String minecraftVersion, String modLoaderVersion, String javaPath) {
        File fabricInstaller = new File(modpackDir + "/server_pack/fabric-installer.jar");
        File forgeInstaller = new File(modpackDir + "/server_pack/forge-installer.jar");
        if (modLoader.equals("Fabric")) {
            try {
                appLogger.info("################################################################");
                appLogger.info("#                Starting Fabric installation                  #");
                appLogger.info("################################################################");
                // Download newest release version of fabric-installer.jar
                ServerUtilities.downloadFabricJar(modpackDir);
                if (fabricInstaller.exists()) {
                    ProcessBuilder processBuilder = new ProcessBuilder(javaPath, "-jar", "fabric-installer.jar", "server", "-mcversion " + minecraftVersion, "-loader " + modLoaderVersion, "-downloadMinecraft").directory(new File(modpackDir + "/server_pack"));
                    processBuilder.redirectErrorStream(true);
                    Process p = processBuilder.start();
                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    while (true) {
                        line = r.readLine();
                        if (line == null) { break; }
                        appLogger.info(line);
                    }
                }
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
                // Download Forge installer as specified in config
                appLogger.info("Downloading https://files.minecraftforge.net/maven/net/minecraftforge/forge/" + minecraftVersion + "-" + modLoaderVersion + "/forge-" + minecraftVersion + "-" + modLoaderVersion + "-installer.jar");
                ServerUtilities.downloadForgeJar(minecraftVersion, modLoaderVersion, modpackDir);
                // Install Forge server
                if (forgeInstaller.exists()) {
                    ProcessBuilder processBuilder = new ProcessBuilder(javaPath, "-jar", "forge-installer.jar", "--installServer").directory(new File(modpackDir + "/server_pack"));
                    processBuilder.redirectErrorStream(true);
                    Process p = processBuilder.start();
                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    while (true) {
                        line = r.readLine();
                        if (line == null) { break; }
                        appLogger.info(line);
                    }
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
        // Cleanup
        ServerUtilities.cleanUpServerPack(fabricInstaller, forgeInstaller, modLoader, modpackDir, minecraftVersion, modLoaderVersion);
    }

    // Create zip archive of serverpack.
    static void zipBuilder(String modpackDir, String modLoader, String minecraftVersion) {
        // With help from https://stackoverflow.com/questions/1091788/how-to-create-a-zip-file-in-java
        appLogger.info("!!!       NOTE: The minecraft_server.jar will not be included in the zip-archive.       !!!");
        appLogger.info("!!! Mojang strictly prohibits the distribution of their software through third parties. !!!");
        appLogger.info("!!!   Tell your users to execute the download scripts to get the Minecraft server jar.  !!!");
        final Path sourceDir = Paths.get(modpackDir + "/server_pack");
        ServerUtilities.generateDownloadScripts(modLoader, modpackDir, minecraftVersion);
        String zipFileName = sourceDir.toString().concat(".zip");
        appLogger.info("Creating zip archive of serverpack...");
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
        ServerUtilities.deleteMinecraftJar(modLoader, modpackDir);
        appLogger.info("Finished creation of zip archive.");
    }
}
