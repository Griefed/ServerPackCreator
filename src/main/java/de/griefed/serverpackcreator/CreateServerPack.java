package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import net.fabricmc.installer.util.LauncherMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class CreateServerPack {
    private static final Logger appLogger = LogManager.getLogger(FilesSetup.class);
    private static final Logger installerLogger = LogManager.getLogger("InstallerLogger");

    private Configuration configuration;
    private LocalizationManager localizationManager;
    private final File propertiesFile    = new File("server.properties");
    private final File iconFile          = new File("server-icon.png");
    private final File forgeWindowsFile  = new File("start-forge.bat");
    private final File forgeLinuxFile    = new File("start-forge.sh");
    private final File fabricWindowsFile = new File("start-fabric.bat");
    private final File fabricLinuxFile   = new File("start-fabric.sh");


    public CreateServerPack(LocalizationManager injectedLocalizationManager, Configuration injectedConfiguration) {
        if (injectedLocalizationManager == null) {
            this.localizationManager = new LocalizationManager();
        } else {
            this.localizationManager = injectedLocalizationManager;
        }

        if (injectedConfiguration == null) {
            this.configuration = new Configuration(localizationManager);
        } else {
            this.configuration = injectedConfiguration;
        }
    }

    public File getPropertiesFile() {
        return propertiesFile;
    }

    public File getIconFile() {
        return iconFile;
    }

    public File getForgeWindowsFile() {
        return forgeWindowsFile;
    }

    public File getForgeLinuxFile() {
        return forgeLinuxFile;
    }

    public File getFabricWindowsFile() {
        return fabricWindowsFile;
    }

    public File getFabricLinuxFile() {
        return fabricLinuxFile;
    }

    /**
     * Run when serverpackcreator is run in either -cli or -cgen mode. Runs what used to be the main content in Main in pre-1.x.x. times. Inits config checks and, if config checks are successfull, calls methods to create the server pack.
     * @return Return true if the serverpack was successfully generated, false if not.
     */
    public boolean run() {
        if (!configuration.checkConfigFile(configuration.getConfigFile())) {
            cleanupEnvironment(configuration.getModpackDir());
            try {
                copyFiles(configuration.getModpackDir(), configuration.getCopyDirs(), configuration.getClientMods());
            } catch (IOException ex) {
                appLogger.error(localizationManager.getLocalizedString("handler.log.error.runincli.copyfiles"), ex);
            }
            copyStartScripts(configuration.getModpackDir(), configuration.getModLoader(), configuration.getIncludeStartScripts());
            if (configuration.getIncludeServerInstallation()) {
                installServer(configuration.getModLoader(), configuration.getModpackDir(), configuration.getMinecraftVersion(), configuration.getModLoaderVersion(), configuration.getJavaPath());
            } else {
                appLogger.info(localizationManager.getLocalizedString("handler.log.info.runincli.server"));
            }
            if (configuration.getIncludeServerIcon()) {
                copyIcon(configuration.getModpackDir());
            } else {
                appLogger.info(localizationManager.getLocalizedString("handler.log.info.runincli.icon"));
            }
            if (configuration.getIncludeServerProperties()) {
                copyProperties(configuration.getModpackDir());
            } else {
                appLogger.info(localizationManager.getLocalizedString("handler.log.info.runincli.properties"));
            }
            if (configuration.getIncludeZipCreation()) {
                zipBuilder(configuration.getModpackDir(), configuration.getModLoader(), configuration.getIncludeServerInstallation(), configuration.getMinecraftVersion());
            } else {
                appLogger.info(localizationManager.getLocalizedString("handler.log.info.runincli.zip"));
            }
            appLogger.info(String.format(localizationManager.getLocalizedString("handler.log.info.runincli.serverpack"), configuration.getModpackDir()));
            appLogger.info(String.format(localizationManager.getLocalizedString("handler.log.info.runincli.archive"), configuration.getModpackDir()));
            appLogger.info(localizationManager.getLocalizedString("handler.log.info.runincli.finish"));
            return true;
        } else {
            appLogger.error(localizationManager.getLocalizedString("handler.log.error.runincli"));
            return false;
        }
    }

    /** Deletes files from previous runs of serverpackcreator.
     * @param modpackDir String. The directory in where to check for files from previous runs.
     */
    void cleanupEnvironment(String modpackDir) {
        if (new File(String.format("%s/server_pack", modpackDir)).exists()) {
            appLogger.info(localizationManager.getLocalizedString("copyfiles.log.info.cleanupenvironment.folder.enter"));
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
                appLogger.error(String.format(localizationManager.getLocalizedString("copyfiles.log.error.cleanupenvironment.folder.delete"), modpackDir));
            } finally {
                appLogger.info(localizationManager.getLocalizedString("copyfiles.log.info.cleanupenvironment.folder.complete"));
            }
        }
        if (new File(String.format("%s/server_pack.zip", modpackDir)).exists()) {
            appLogger.info(localizationManager.getLocalizedString("copyfiles.log.info.cleanupenvironment.zip.enter"));
            boolean isZipDeleted = new File(String.format("%s/server_pack.zip", modpackDir)).delete();
            if (isZipDeleted) {
                appLogger.info(localizationManager.getLocalizedString("copyfiles.log.info.cleanupenvironment.zip.complete"));
            } else {
                appLogger.error(localizationManager.getLocalizedString("copyfiles.log.error.cleanupenvironment.zip.delete"));
            }
        }
    }

    /** Copies start scripts for Forge modloader into the server_pack folder.
     * @param modpackDir String. Files will be copied into subfolder server_pack. Checks for valid modpackDir are in ConfigCheck.
     * @param modLoader String. Determines whether start scripts for Forge or Fabric are copied to modpackDir. Checks for valid modLoader are in ConfigCheck.
     * @param includeStartScripts Boolean. Whether to include start scripts in server_pack. Boolean.
     */
    void copyStartScripts(String modpackDir, String modLoader, boolean includeStartScripts) {
        if (modLoader.equalsIgnoreCase("Forge") && includeStartScripts) {
            appLogger.info(localizationManager.getLocalizedString("copyfiles.log.info.copystartscripts.forge"));
            try {
                Files.copy(
                        Paths.get(String.format("./server_files/%s", getForgeWindowsFile())),
                        Paths.get(String.format("%s/server_pack/%s", modpackDir, getForgeWindowsFile())),
                        REPLACE_EXISTING
                );
                Files.copy(
                        Paths.get(String.format("./server_files/%s", getForgeLinuxFile())),
                        Paths.get(String.format("%s/server_pack/%s", modpackDir, getForgeLinuxFile())),
                        REPLACE_EXISTING
                );
            } catch (IOException ex) {
                appLogger.error(localizationManager.getLocalizedString("copyfiles.log.error.copystartscripts"), ex);
            }
        } else if (modLoader.equalsIgnoreCase("Fabric") && includeStartScripts) {
            appLogger.info(localizationManager.getLocalizedString("copyfiles.log.info.copystartscripts.fabric"));
            try {
                Files.copy(
                        Paths.get(String.format("./server_files/%s", getFabricWindowsFile())),
                        Paths.get(String.format("%s/server_pack/%s", modpackDir, getFabricWindowsFile())),
                        REPLACE_EXISTING
                );
                Files.copy(
                        Paths.get(String.format("./server_files/%s", getFabricLinuxFile())),
                        Paths.get(String.format("%s/server_pack/%s", modpackDir, getFabricLinuxFile())),
                        REPLACE_EXISTING
                );
            } catch (IOException ex) {
                appLogger.error(localizationManager.getLocalizedString("copyfiles.log.error.copystartscripts"), ex);
            }
        } else {
            appLogger.error(localizationManager.getLocalizedString("configcheck.log.error.checkmodloader"));
        }
    }

    /** Copies all specified folders and their files to the modpackDir.
     * @param modpackDir String. /server_pack. Directory where all directories listed in copyDirs will be copied into.
     * @param copyDirs String List. The folders and files within to copy.
     * @param clientMods String List. List of clientside-only mods NOT to copy to server pack.
     * @throws IOException Only print stacktrace if it does not start with java.nio.file.DirectoryNotEmptyException.
     */
    void copyFiles(String modpackDir, List<String> copyDirs, List<String> clientMods) throws IOException {
        String serverPath = String.format("%s/server_pack", modpackDir);
        Files.createDirectories(Paths.get(serverPath));
        for (int i = 0; i < copyDirs.size(); i++) {
            String clientDir = String.format("%s/%s", modpackDir,copyDirs.get(i));
            String serverDir = String.format("%s/%s", serverPath,copyDirs.get(i));
            appLogger.info(String.format(localizationManager.getLocalizedString("copyfiles.log.info.copyfiles.setup"), serverDir));
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

                            appLogger.debug(String.format(localizationManager.getLocalizedString("copyfiles.log.debug.copyfiles"), file.toAbsolutePath().toString()));

                        } catch (IOException ex) {
                            if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                                appLogger.error(localizationManager.getLocalizedString("copyfiles.log.error.copyfiles.saves"), ex);
                            }
                        }
                    });
                } catch (IOException ex) {
                    appLogger.error(localizationManager.getLocalizedString("copyfiles.log.error.copyfiles.saves.world"), ex);
                }
            } else if (copyDirs.get(i).startsWith("mods") && clientMods.size() > 0) {
                List<String> listOfFiles = excludeClientMods(clientDir, clientMods);
                Files.createDirectories(Paths.get(serverDir));
                for (int in = 0; in < listOfFiles.size(); in++) {
                    try {

                        Files.copy(
                                Paths.get(listOfFiles.get(in)),
                                Paths.get(String.format("%s/%s",serverDir, new File(listOfFiles.get(in)).getName())),
                                REPLACE_EXISTING
                        );

                        appLogger.debug(String.format(localizationManager.getLocalizedString("copyfiles.log.debug.copyfiles"), listOfFiles.get(in)));

                    } catch (IOException ex) {
                        if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                            appLogger.error(localizationManager.getLocalizedString("copyfiles.log.error.copyfiles.mods"), ex);
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

                            appLogger.debug(String.format(localizationManager.getLocalizedString("copyfiles.log.debug.copyfiles"), file.toAbsolutePath().toString()));
                        } catch (IOException ex) {
                            if (!ex.toString().startsWith("java.nio.file.DirectoryNotEmptyException")) {
                                appLogger.error(localizationManager.getLocalizedString("copyfiles.log.error.copyfiles.mods"), ex);
                            }
                        }
                    });
                    files.close();
                } catch (IOException ex) {
                    appLogger.error(localizationManager.getLocalizedString("copyfiles.log.error.copyfiles"), ex);
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
    List<String> excludeClientMods(String modsDir, List<String> clientMods) {
        appLogger.info(localizationManager.getLocalizedString("copyfiles.log.info.excludeclientmods"));
        String[] copyMods = new String[0];
        List<String> modpackModList = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(Paths.get(modsDir))) {

            modpackModList = walk.filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toList());

            for (int in = 0; in < modpackModList.size(); in++) {

                for (int i = 0; i < clientMods.size(); i++) {

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
            appLogger.error(localizationManager.getLocalizedString("copyfiles.log.error.excludeclientmods"), ex);
        }
        return Arrays.asList(copyMods.clone());
    }

    /** Copies the server-icon.png into server_pack.
     * @param modpackDir String. /server_pack. Directory where the server-icon.png will be copied to.
     */
    void copyIcon(String modpackDir) {
        appLogger.info(localizationManager.getLocalizedString("copyfiles.log.info.copyicon"));
        try {

            Files.copy(
                    Paths.get(String.format("./server_files/%s", getIconFile())),
                    Paths.get(String.format("%s/server_pack/%s", modpackDir, getIconFile())),
                    REPLACE_EXISTING
            );

        } catch (IOException ex) {
            appLogger.error(localizationManager.getLocalizedString("copyfiles.log.error.copyicon"), ex);
        }
    }

    /** Copies the server.properties into server_pack.
     * @param modpackDir String. /server_pack. Directory where the server.properties. will be copied to.
     */
    void copyProperties(String modpackDir) {
        appLogger.info(localizationManager.getLocalizedString("copyfiles.log.info.copyproperties"));
        try {

            Files.copy(
                    Paths.get(String.format("./server_files/%s", getPropertiesFile())),
                    Paths.get(String.format("%s/server_pack/%s", modpackDir, getPropertiesFile())),
                    REPLACE_EXISTING
            );

        } catch (IOException ex) {
            appLogger.error(localizationManager.getLocalizedString("copyfiles.log.error.copyproperties"), ex);
        }
    }

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
                appLogger.info(localizationManager.getLocalizedString("serversetup.log.info.installserver.fabric.enter"));
                installerLogger.info(localizationManager.getLocalizedString("serversetup.log.info.installserver.fabric.enter"));
                if (downloadFabricJar(modpackDir)) {
                    appLogger.info(localizationManager.getLocalizedString("serversetup.log.info.installserver.fabric.download"));
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
                    installerLogger.info(localizationManager.getLocalizedString("serversetup.log.info.installserver"));
                    reader.close();
                    appLogger.info(localizationManager.getLocalizedString("serversetup.log.info.installserver.fabric.details"));
                    appLogger.info(localizationManager.getLocalizedString("serversetup.log.info.installserver"));
                } else {
                    appLogger.error(localizationManager.getLocalizedString("serversetup.log.error.installserver.fabric"));
                }
            } catch (IOException ex) {
                appLogger.error(localizationManager.getLocalizedString("serversetup.log.error.installserver.fabricfail"), ex);
            }
        } else if (modLoader.equalsIgnoreCase("Forge")) {
            try {
                appLogger.info(localizationManager.getLocalizedString("serversetup.log.info.installserver.forge.enter"));
                installerLogger.info(localizationManager.getLocalizedString("serversetup.log.info.installserver.forge.enter"));
                if (downloadForgeJar(minecraftVersion, modLoaderVersion, modpackDir)) {
                    appLogger.info(localizationManager.getLocalizedString("serversetup.log.info.installserver.forge.download"));
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
                    installerLogger.info(localizationManager.getLocalizedString("serversetup.log.info.installserver"));
                    reader.close();
                    appLogger.info(localizationManager.getLocalizedString("serversetup.log.info.installserver.forge.details"));
                    appLogger.info(localizationManager.getLocalizedString("serversetup.log.info.installserver"));
                    process.destroy();
                } else {
                    appLogger.error(localizationManager.getLocalizedString("serversetup.log.error.installserver.forge"));
                }
            } catch (IOException ex) {
                appLogger.error(localizationManager.getLocalizedString("serversetup.log.error.installserver.forgefail"), ex);
            }
        } else {
            appLogger.error(String.format(localizationManager.getLocalizedString("configcheck.log.error.checkmodloader"), modLoader));
        }

        generateDownloadScripts(modLoader, modpackDir, minecraftVersion);
        cleanUpServerPack(
                fabricInstaller,
                forgeInstaller,
                modLoader,
                modpackDir,
                minecraftVersion,
                modLoaderVersion);

    }
    /** Create a zip-archive of the serverpack, excluding Mojang's minecraft_server.jar.
     * With help from https://stackoverflow.com/questions/1091788/how-to-create-a-zip-file-in-java
     * @param modpackDir String. The directory where the zip-archive will be created and saved in.
     * @param modLoader String. Determines the name of Minecraft#s server jar which will be deleted from the zip-archive.
     * @param includeServerInstallation Boolean. Determines whether the Minecraft server jar needs to be deleted from the zip-archive.
     * @param minecraftVersion String. The Minecraft version of which to delete the server jar. Used if modloader is Forge.
     */
    void zipBuilder(String modpackDir, String modLoader, Boolean includeServerInstallation, String minecraftVersion) {
        final Path sourceDir = Paths.get(String.format("%s/server_pack", modpackDir));
        String zipFileName = sourceDir.toString().concat(".zip");
        appLogger.info(localizationManager.getLocalizedString("serversetup.log.info.zipbuilder.enter"));
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
                        appLogger.error(localizationManager.getLocalizedString("serversetup.log.error.zipbuilder.create"), ex);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            outputStream.close();
        } catch (IOException ex) {
            appLogger.error(localizationManager.getLocalizedString("serversetup.log.error.zipbuilder.create"), ex);
        }
        if (includeServerInstallation) {
            deleteMinecraftJar(modLoader, modpackDir, minecraftVersion);
            appLogger.warn(localizationManager.getLocalizedString("serversetup.log.warn.zipbuilder.minecraftjar1"));
            appLogger.warn(localizationManager.getLocalizedString("serversetup.log.warn.zipbuilder.minecraftjar2"));
            appLogger.warn(localizationManager.getLocalizedString("serversetup.log.warn.zipbuilder.minecraftjar3"));
        }
        appLogger.info(localizationManager.getLocalizedString("serversetup.log.info.zipbuilder.finish"));
    }

    /** Calls methods for generating download scripts for Mojang's Minecraft server depending on the specified versions and modloader.
     * @param modLoader String. The specified modloader determines the name under which Mojang's server jar will be downloaded as.
     * @param modpackDir String. /server_pack The directory where the scripts will be placed in.
     * @param minecraftVersion String. The version of the Minecraft server jar to download.
     */
    void generateDownloadScripts(String modLoader, String modpackDir, String minecraftVersion) {
        if (modLoader.equalsIgnoreCase("Fabric")) {
            fabricShell(modpackDir, minecraftVersion);
            fabricBatch(modpackDir, minecraftVersion);
        } else if (modLoader.equalsIgnoreCase("Forge")) {
            forgeShell(modpackDir, minecraftVersion);
            forgeBatch(modpackDir, minecraftVersion);
        } else {
            appLogger.error(String.format(localizationManager.getLocalizedString("configcheck.log.error.checkmodloader"), modLoader));
        }
    }

    /** Generates download scripts for Mojang's Minecraft server for Fabric,Linux.
     * @param modpackDir String. /server_pack The directory where the scripts will be placed in.
     * @param minecraftVersion String. The version of the Minecraft server jar to download.
     */
    void fabricShell(String modpackDir, String minecraftVersion) {
        try {
            String downloadMinecraftServer = (new URL(
                    LauncherMeta
                            .getLauncherMeta()
                            .getVersion(minecraftVersion)
                            .getVersionMeta()
                            .downloads
                            .get("server")
                            .url))
                    .toString();
            String shFabric = String.format("#!/bin/bash\n#Download the Minecraft_server.jar for your modpack\n\nwget -O server.jar %s", downloadMinecraftServer);
            Path pathSh = Paths.get(String.format("%s/server_pack/download_minecraft-server.jar_fabric.sh", modpackDir));
            byte[] strToBytesSh = shFabric.getBytes();
            Files.write(pathSh, strToBytesSh);
            String readSh = Files.readAllLines(pathSh).get(0);
            appLogger.debug(String.format(localizationManager.getLocalizedString("serverutilities.log.debug.fabricshell"), readSh));
        } catch (IOException ex) {
            appLogger.error(localizationManager.getLocalizedString("serverutilities.log.error.fabricshell"), ex);
        }
        appLogger.info(localizationManager.getLocalizedString("serverutilities.log.info.fabricshell"));
    }

    /** Generates download scripts for Mojang's Minecraft server for Fabric,Windows.
     * @param modpackDir String. /server_pack The directory where the scripts will be placed in.
     * @param minecraftVersion String. The version of the Minecraft server jar to download.
     */
    void fabricBatch(String modpackDir, String minecraftVersion) {
        try {
            String downloadMinecraftServer = (new URL(
                    LauncherMeta
                            .getLauncherMeta()
                            .getVersion(minecraftVersion)
                            .getVersionMeta()
                            .downloads
                            .get("server")
                            .url))
                    .toString();
            String batFabric = String.format("powershell -Command \"(New-Object Net.WebClient).DownloadFile('%s', 'server.jar')\"", downloadMinecraftServer);
            Path pathBat = Paths.get(String.format("%s/server_pack/download_minecraft-server.jar_fabric.bat", modpackDir));
            byte[] strToBytesBat = batFabric.getBytes();
            Files.write(pathBat, strToBytesBat);
            String readBat = Files.readAllLines(pathBat).get(0);
            appLogger.debug(String.format(localizationManager.getLocalizedString("serverutilities.log.debug.fabricbatch"), readBat));
        } catch (IOException ex) {
            appLogger.error(localizationManager.getLocalizedString("serverutilities.log.error.fabricbatch"), ex);
        }
        appLogger.info(localizationManager.getLocalizedString("serverutilities.log.info.fabricbatch"));
    }

    /** Generates download scripts for Mojang's Minecraft server for Forge,Linux.
     * @param modpackDir String. /server_pack The directory where the scripts will be placed in.
     * @param minecraftVersion String. The version of the Minecraft server jar to download.
     */
    void forgeShell(String modpackDir, String minecraftVersion) {
        try {
            String downloadMinecraftServer = (new URL(
                    LauncherMeta
                            .getLauncherMeta()
                            .getVersion(minecraftVersion)
                            .getVersionMeta()
                            .downloads
                            .get("server")
                            .url))
                    .toString();
            String shForge = String.format("#!/bin/bash\n# Download the Minecraft_server.jar for your modpack\n\nwget -O minecraft_server.%s.jar %s", minecraftVersion, downloadMinecraftServer);
            Path pathSh = Paths.get(String.format("%s/server_pack/download_minecraft-server.jar_forge.sh", modpackDir));
            byte[] strToBytesSh = shForge.getBytes();
            Files.write(pathSh, strToBytesSh);
            String readSh = Files.readAllLines(pathSh).get(0);
            appLogger.debug(String.format(localizationManager.getLocalizedString("serverutilities.log.debug.forgeshell"), readSh));
        } catch (IOException ex) {
            appLogger.error(localizationManager.getLocalizedString("serverutilities.log.error.forgeshell"), ex);
        }
        appLogger.info(localizationManager.getLocalizedString("serverutilities.log.info.forgeshell"));
    }

    /** Generates download scripts for Mojang's Minecraft server for Forge,Windows.
     * @param modpackDir String. /server_pack The directory where the scripts will be placed in.
     * @param minecraftVersion String. The version of the Minecraft server jar to download.
     */
    void forgeBatch(String modpackDir, String minecraftVersion) {
        try {
            String downloadMinecraftServer = (new URL(
                    LauncherMeta
                            .getLauncherMeta()
                            .getVersion(minecraftVersion)
                            .getVersionMeta()
                            .downloads
                            .get("server")
                            .url))
                    .toString();
            String batForge = String.format("powershell -Command \"(New-Object Net.WebClient).DownloadFile('%s', 'minecraft_server.%s.jar')\"", downloadMinecraftServer, minecraftVersion);
            Path pathBat = Paths.get(String.format("%s/server_pack/download_minecraft-server.jar_forge.bat", modpackDir));
            byte[] strToBytesBat = batForge.getBytes();
            Files.write(pathBat, strToBytesBat);
            String readBat = Files.readAllLines(pathBat).get(0);
            appLogger.debug(String.format(localizationManager.getLocalizedString("serverutilities.log.debug.forgebatch"), readBat));
        } catch (IOException ex) {
            appLogger.error(localizationManager.getLocalizedString("serverutilities.log.error.forgebatch"), ex);
        }
        appLogger.info(localizationManager.getLocalizedString("serverutilities.log.info.forgebatch"));
    }

    /** Downloads the specified version of Fabric.
     * @param modpackDir String. /server_pack The directory where the Fabric installer will be placed in.
     * @return Boolean. Returns true if the download was successful. False if not.
     */
    boolean downloadFabricJar(String modpackDir) {
        boolean downloaded = false;
        try {
            appLogger.info(localizationManager.getLocalizedString("serverutilities.log.info.downloadfabricjar.enter"));
            String latestFabricInstaller = latestFabricInstaller(modpackDir);
            URL downloadFabric = new URL(String.format("https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar", latestFabricInstaller, latestFabricInstaller));

            ReadableByteChannel readableByteChannel = Channels.newChannel(downloadFabric.openStream());
            FileOutputStream downloadFabricFileOutputStream = new FileOutputStream(String.format("%s/server_pack/fabric-installer.jar", modpackDir));
            FileChannel downloadFabricFileChannel = downloadFabricFileOutputStream.getChannel();
            downloadFabricFileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            downloadFabricFileOutputStream.flush();
            downloadFabricFileOutputStream.close();
            readableByteChannel.close();
            downloadFabricFileChannel.close();

        } catch (IOException e) {
            appLogger.error(localizationManager.getLocalizedString("serverutilities.log.error.downloadfabricjar.download"), e);
            if (new File(String.format("%s/server_pack/fabric-installer.jar", modpackDir)).exists()) {
                try {
                    Files.delete(Paths.get(String.format("%s/server_pack/fabric-installer.jar", modpackDir)));
                } catch (IOException ex) {
                    appLogger.error(localizationManager.getLocalizedString("serverutilities.log.error.downloadfabricjar.delete"), ex);
                }
            }
        }
        if (new File(String.format("%s/server_pack/fabric-installer.jar", modpackDir)).exists()) {
            downloaded = true;
        }
        return downloaded;
    }

    /** Returns the latest installer version for the Fabric installer to be used in ServerSetup.installServer.
     * @param modpackDir String. /server_pack The directory where the Fabric installer will be placed in.
     * @return Boolean. Returns true if the download was successful. False if not.
     */
    String latestFabricInstaller(String modpackDir) {
        String result;
        try {
            URL downloadFabricXml = new URL("https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml");

            ReadableByteChannel downloadFabricXmlReadableByteChannel = Channels.newChannel(downloadFabricXml.openStream());
            FileOutputStream downloadFabricXmlFileOutputStream = new FileOutputStream(String.format("%s/server_pack/fabric-installer.xml", modpackDir));
            FileChannel downloadFabricXmlFileChannel = downloadFabricXmlFileOutputStream.getChannel();
            downloadFabricXmlFileOutputStream.getChannel().transferFrom(downloadFabricXmlReadableByteChannel, 0, Long.MAX_VALUE);

            downloadFabricXmlFileOutputStream.flush();
            downloadFabricXmlFileOutputStream.close();
            downloadFabricXmlReadableByteChannel.close();
            downloadFabricXmlFileChannel.close();

            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document fabricXml = builder.parse(new File(String.format("%s/server_pack/fabric-installer.xml",modpackDir)));
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            result = (String) xpath.evaluate("/metadata/versioning/release", fabricXml, XPathConstants.STRING);
            appLogger.info(localizationManager.getLocalizedString("serverutilities.log.info.latestfabricinstaller"));
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException ex) {
            appLogger.error(localizationManager.getLocalizedString("serverutilities.log.error.latestfabricinstaller"), ex);
            result = "0.7.2";
        }
        return result;
    }

    /** Downloads the specified version of the Forge installer to be used in ServerSetup.installServer.
     * @param minecraftVersion String. The Minecraft version corresponding to the Forge version. Minecraft version and Forge version build a pair.
     * @param modLoaderVersion String. The Forge version corresponding to the Minecraft version. Minecraft version and Forge version build a pair.
     * @param modpackDir String. /server_pack The directory where the Forge installer will be placed in.
     * @return Boolean. Returns true if the download was successful. False if not.
     */
    boolean downloadForgeJar(String minecraftVersion, String modLoaderVersion, String modpackDir) {
        boolean downloaded = false;
        try {
            appLogger.info(localizationManager.getLocalizedString("serverutilities.log.info.downloadforgejar.enter"));
            URL downloadForge = new URL(String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s-%s/forge-%s-%s-installer.jar", minecraftVersion, modLoaderVersion, minecraftVersion, modLoaderVersion));

            ReadableByteChannel readableByteChannel = Channels.newChannel(downloadForge.openStream());
            FileOutputStream downloadForgeFileOutputStream = new FileOutputStream(String.format("%s/server_pack/forge-installer.jar", modpackDir));
            FileChannel downloadForgeFileChannel = downloadForgeFileOutputStream.getChannel();
            downloadForgeFileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            downloadForgeFileOutputStream.flush();
            downloadForgeFileOutputStream.close();
            readableByteChannel.close();
            downloadForgeFileChannel.close();

        } catch (IOException e) {
            appLogger.error(localizationManager.getLocalizedString("serverutilities.log.error.downloadforgejar.download"), e);
            if (new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).exists()) {
                if (new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).delete()) {
                    appLogger.error(localizationManager.getLocalizedString("serverutilities.log.debug.downloadforgejar"));
                }
            }
        }
        if (new File(String.format("%s/server_pack/forge-installer.jar", modpackDir)).exists()) {
            downloaded = true;
        }
        return downloaded;
    }

    /** Deletes Mojang's minecraft_server.jar from the zip-archive so users do not accidentally upload a file containing software from Mojang.
     * With help from https://stackoverflow.com/questions/5244963/delete-files-from-a-zip-archive-without-decompressing-in-java-or-maybe-python and https://bugs.openjdk.java.net/browse/JDK-8186227
     * @param modLoader String. Determines the name of the file to delete.
     * @param modpackDir String. /server_pack The directory in which the file will be deleted.
     * @param minecraftVersion String. The Minecraft version of which to delete the server jar. Used if modloader is Forge.
     */
    void deleteMinecraftJar(String modLoader, String modpackDir, String minecraftVersion) {
        if (modLoader.equalsIgnoreCase("Forge")) {
            appLogger.info(localizationManager.getLocalizedString("serverutilities.log.info.deleteminecraftjar.enter"));

            Map<String, String> zip_properties = new HashMap<>();
            zip_properties.put("create", "false");
            zip_properties.put("encoding", "UTF-8");

            Path serverpackZip = Paths.get(String.format("%s/server_pack.zip", modpackDir));
            URI zipUri = URI.create("jar:" + serverpackZip.toUri());

            try (FileSystem zipfs = FileSystems.newFileSystem(zipUri, zip_properties)) {
                Path pathInZipfile = zipfs.getPath(String.format("minecraft_server.%s.jar", minecraftVersion));
                Files.delete(pathInZipfile);
                appLogger.info(localizationManager.getLocalizedString("serverutilities.log.info.deleteminecraftjar.success"));
            } catch (IOException ex) {
                appLogger.error(localizationManager.getLocalizedString("serverutilities.log.error.deleteminecraftjar.delete"), ex);
            }
        } else if (modLoader.equalsIgnoreCase("Fabric")) {
            appLogger.info(localizationManager.getLocalizedString("serverutilities.log.info.deleteminecraftjar.enter"));

            Map<String, String> zip_properties = new HashMap<>();
            zip_properties.put("create", "false");
            zip_properties.put("encoding", "UTF-8");

            Path serverpackZip = Paths.get(String.format("%s/server_pack.zip", modpackDir));
            URI zipUri = URI.create(String.format("jar:%s", serverpackZip.toUri()));

            try (FileSystem zipfs = FileSystems.newFileSystem(zipUri, zip_properties)) {
                Path pathInZipfile = zipfs.getPath("server.jar");
                Files.delete(pathInZipfile);
                appLogger.info(localizationManager.getLocalizedString("serverutilities.log.info.deleteminecraftjar.success"));
            } catch (IOException ex) {
                appLogger.error(localizationManager.getLocalizedString("serverutilities.log.error.deleteminecraftjar.delete"), ex);
            }
        } else {
            appLogger.error(String.format(localizationManager.getLocalizedString("configcheck.log.error.checkmodloader"), modLoader));
        }
    }

    /** Deletes remnant files from Fabric/Forge installation no longer needed.
     * @param fabricInstaller File. Fabric installer to be deleted.
     * @param forgeInstaller File. Forge installer to be deleted.
     * @param modLoader String. Whether Forge or Fabric files are to be deleted.
     * @param modpackDir String. /server_pack The directory where files are to be deleted.
     * @param minecraftVersion String. Needed for renaming the Forge server jar to work with launch scripts provided by serverpackcreator.
     * @param modLoaderVersion String. Needed for renaming the Forge server jar to work with launch scripts provided by serverpackcreator.
     */
    void cleanUpServerPack(File fabricInstaller, File forgeInstaller, String modLoader, String modpackDir, String minecraftVersion, String modLoaderVersion) {
        appLogger.info(localizationManager.getLocalizedString("serverutilities.log.info.cleanupserverpack.enter"));
        if (modLoader.equalsIgnoreCase("Fabric")) {
            File fabricXML = new File(String.format("%s/server_pack/fabric-installer.xml", modpackDir));
            boolean isXmlDeleted = fabricXML.delete();
            boolean isInstallerDeleted = fabricInstaller.delete();
            if (isXmlDeleted)
            { appLogger.info(String.format(localizationManager.getLocalizedString("serverutilities.log.info.cleanupserverpack.deleted"), fabricXML.getName())); }
            else
            { appLogger.error(String.format(localizationManager.getLocalizedString("serverutilities.log.error.cleanupserverpack.delete"), fabricXML.getName())); }

            if (isInstallerDeleted)
            { appLogger.info(String.format(localizationManager.getLocalizedString("serverutilities.log.info.cleanupserverpack.deleted"), fabricInstaller.getName())); }
            else
            { appLogger.error(String.format(localizationManager.getLocalizedString("serverutilities.log.error.cleanupserverpack.delete"), fabricInstaller.getName())); }

        } else if (modLoader.equalsIgnoreCase("Forge")) {
            try {
                Files.copy(
                        Paths.get(String.format("%s/server_pack/forge-%s-%s.jar", modpackDir, minecraftVersion, modLoaderVersion)),
                        Paths.get(String.format("%s/server_pack/forge.jar", modpackDir)),
                        REPLACE_EXISTING);
                boolean isOldJarDeleted = (new File(
                        String.format("%s/server_pack/forge-%s-%s.jar", modpackDir, minecraftVersion, modLoaderVersion))).delete();
                boolean isInstallerDeleted = forgeInstaller.delete();

                if ((isOldJarDeleted) && (new File(String.format("%s/server_pack/forge.jar", modpackDir)).exists()))
                { appLogger.info(localizationManager.getLocalizedString("serverutilities.log.info.cleanupserverpack.rename")); }
                else
                { appLogger.error(localizationManager.getLocalizedString("serverutilities.log.error.cleanupserverpack.rename")); }

                if (isInstallerDeleted)
                { appLogger.info(String.format(localizationManager.getLocalizedString("serverutilities.log.info.cleanupserverpack.deleted"), forgeInstaller.getName())); }
                else
                { appLogger.error(String.format(localizationManager.getLocalizedString("serverutilities.log.error.cleanupserverpack.delete"), forgeInstaller.getName())); }

            } catch (IOException ex) {
                appLogger.error(localizationManager.getLocalizedString("serverutilities.log.error.cleanupserverpack"), ex);
            }
        } else {
            appLogger.error(String.format(localizationManager.getLocalizedString("configcheck.log.error.checkmodloader"), modLoader));
        }
    }
}