package de.griefed.ServerPackCreator;

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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class ServerUtilities {
    private static final Logger appLogger = LogManager.getLogger("ServerUtilities");
    /** Optional. Depending on serverpackcreator.conf(includeServerInstallation,modLoader,includeZipCreation) this will call generation of download scripts which will download Mojang's minecraft_server.jar. These scripts are most useful for the zip-archive of the serverpack.
     * TODO: Write param docs
     * @param modLoader
     * @param modpackDir
     * @param minecraftVersion
     */
    static void generateDownloadScripts(String modLoader, String modpackDir, String minecraftVersion) {
        if (modLoader.equalsIgnoreCase("Fabric")) {
            fabricShell(modpackDir, minecraftVersion);
            fabricBatch(modpackDir, minecraftVersion);
        } else if (modLoader.equalsIgnoreCase("Forge")) {
            forgeShell(modpackDir, minecraftVersion);
            forgeBatch(modpackDir, minecraftVersion);
        } else {
            appLogger.error("Specified invalid Modloader: " + modLoader);
        }
    }
    /** Optional. Generates minecraft_server.jar download scripts for Fabric,Linux
     * TODO: Write param docs
     * @param modpackDir
     * @param minecraftVersion
     */
    private static void fabricShell(String modpackDir, String minecraftVersion) {
        try {
            String downloadMinecraftServer = (new URL(LauncherMeta.getLauncherMeta().getVersion(minecraftVersion).getVersionMeta().downloads.get("server").url)).toString();
            String shFabric = "#!/bin/bash\n#Download the Minecraft_server.jar for your modpack\n\nwget -O server.jar " + downloadMinecraftServer;
            Path pathSh = Paths.get(modpackDir + "/server_pack/download_minecraft-server.jar_fabric.sh");
            byte[] strToBytesSh = shFabric.getBytes();
            Files.write(pathSh, strToBytesSh);
            String readSh = Files.readAllLines(pathSh).get(0);
            appLogger.debug("fabricShell.readSh was: " + readSh);
        } catch (IOException ex) {
            appLogger.error("Error creating shell script for Fabric.", ex);
        }
        appLogger.info("Fabric shell script generated.");
    }
    /** Optional. Generates minecraft_server.jar download scripts for Fabric,Windows
     * TODO: Write param docs
     * @param modpackDir
     * @param minecraftVersion
     */
    private static void fabricBatch(String modpackDir, String minecraftVersion) {
        try {
            String downloadMinecraftServer = (new URL(LauncherMeta.getLauncherMeta().getVersion(minecraftVersion).getVersionMeta().downloads.get("server").url)).toString();
            String batFabric = "powershell -Command \"(New-Object Net.WebClient).DownloadFile('" + downloadMinecraftServer + "', 'server.jar')\"";
            Path pathBat = Paths.get(modpackDir + "/server_pack/download_minecraft-server.jar_fabric.bat");
            byte[] strToBytesBat = batFabric.getBytes();
            Files.write(pathBat, strToBytesBat);
            String readBat = Files.readAllLines(pathBat).get(0);
            appLogger.debug("fabricBatch.readBat was: " + readBat);
        } catch (IOException ex) {
            appLogger.error("Error creating batch script for Fabric.", ex);
        }
        appLogger.info("Fabric batch script generated.");
    }
    /** Optional. Generates minecraft_server.jar download scripts for Forge,Linux
     * TODO: Write param docs
     * @param modpackDir
     * @param minecraftVersion
     */
    private static void forgeShell(String modpackDir, String minecraftVersion) {
        try {
            String downloadMinecraftServer = (new URL(LauncherMeta.getLauncherMeta().getVersion(minecraftVersion).getVersionMeta().downloads.get("server").url)).toString();
            String shForge = "#!/bin/bash\n# Download the Minecraft_server.jar for your modpack\n\nwget -O minecraft_server." + minecraftVersion +".jar " + downloadMinecraftServer;
            Path pathSh = Paths.get(modpackDir + "/server_pack/download_minecraft-server.jar_forge.sh");
            byte[] strToBytesSh = shForge.getBytes();
            Files.write(pathSh, strToBytesSh);
            String readSh = Files.readAllLines(pathSh).get(0);
            appLogger.debug("forgeShell.readSh was: " + readSh);
        } catch (IOException ex) {
            appLogger.error("Error creating shell script for Forge.", ex);
        }
        appLogger.info("Forge shell script generated.");
    }
    /** Optional. Generates minecraft_server.jar download scripts for Forge,Windows
     * TODO: Write param docs
     * @param modpackDir
     * @param minecraftVersion
     */
    private static void forgeBatch(String modpackDir, String minecraftVersion) {
        try {
            String downloadMinecraftServer = (new URL(LauncherMeta.getLauncherMeta().getVersion(minecraftVersion).getVersionMeta().downloads.get("server").url)).toString();
            String batForge = "powershell -Command \"(New-Object Net.WebClient).DownloadFile('" + downloadMinecraftServer + "', 'minecraft_server." + minecraftVersion + ".jar')\"";
            Path pathBat = Paths.get(modpackDir + "/server_pack/download_minecraft-server.jar_forge.bat");
            byte[] strToBytesBat = batForge.getBytes();
            Files.write(pathBat, strToBytesBat);
            String readBat = Files.readAllLines(pathBat).get(0);
            appLogger.debug("forgeBatch.readBat was: " + readBat);
        } catch (IOException ex) {
            appLogger.error("Error creating shell script for Forge.", ex);
        }
        appLogger.info("Forge batch script generated.");
    }
    /** Optional. Depending on serverpackcreator.conf(modLoader,includeServerInstallation) this will download the specified version of Fabric.
     *  TODO: Write param docs
     * @param modpackDir
     */
    static void downloadFabricJar(String modpackDir) {
        try {
            appLogger.info("Trying to download Fabric installer...");
            URL downloadFabric = new URL("https://maven.fabricmc.net/net/fabricmc/fabric-installer/" + latestFabric(modpackDir) + "/fabric-installer-" + latestFabric(modpackDir) + ".jar");
            ReadableByteChannel readableByteChannel = Channels.newChannel(downloadFabric.openStream());
            FileOutputStream downloadFabricFileOutputStream = new FileOutputStream(modpackDir + "/server_pack/fabric-installer.jar");
            FileChannel downloadFabricFileChannel = downloadFabricFileOutputStream.getChannel();
            downloadFabricFileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadFabricFileOutputStream.flush();
            downloadFabricFileOutputStream.close();
        } catch (IOException ex) {
            appLogger.error("Error downloading Fabric.", ex);
        }
    }
    /** Optional. Depending on serverpackcreator.conf(modLoader,includeServerInstallation), this returns the latest installer version for the Fabric installer to be used in ServerSetup.installServer.
     * TODO: Write param docs
     * @param modpackDir
     * @return
     */
    private static String latestFabric(String modpackDir) {
        String result;
        try {
            URL downloadFabricXml = new URL("https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml");
            ReadableByteChannel downloadFabricXmlReadableByteChannel = Channels.newChannel(downloadFabricXml.openStream());
            FileOutputStream downloadFabricXmlFileOutputStream = new FileOutputStream(modpackDir + "/server_pack/fabric-installer.xml");
            FileChannel downloadFabricXmlFileChannel = downloadFabricXmlFileOutputStream.getChannel();
            downloadFabricXmlFileOutputStream.getChannel().transferFrom(downloadFabricXmlReadableByteChannel, 0, Long.MAX_VALUE);
            downloadFabricXmlFileOutputStream.flush();
            downloadFabricXmlFileOutputStream.close();
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document fabricXml = builder.parse(new File(modpackDir + "/server_pack/fabric-installer.xml"));
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            result = (String) xpath.evaluate("/metadata/versioning/release", fabricXml, XPathConstants.STRING);
            return result;
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException ex) {
            appLogger.error("Could not retrieve XML file. Defaulting to Installer version 0.7.1.", ex);
            return "0.7.1";
        } finally {
            appLogger.info("Successfully retrieved Fabric XML.");
        }
    }
    /** Optional. Depending on serverpackcreator.conf(modLoader,includeServerInstallation,modLoaderVersion) this downloads the specified version of the Forge installer to be used in ServerSetup.installServer.
     * TODO: Write param docs
     * @param minecraftVersion
     * @param modLoaderVersion
     * @param modpackDir
     */
    static void downloadForgeJar(String minecraftVersion, String modLoaderVersion, String modpackDir) {
        try {
            appLogger.info("Trying to download specified Forge installer...");
            URL downloadForge = new URL("https://files.minecraftforge.net/maven/net/minecraftforge/forge/" + minecraftVersion + "-" + modLoaderVersion + "/forge-" + minecraftVersion + "-" + modLoaderVersion + "-installer.jar");
            ReadableByteChannel readableByteChannel = Channels.newChannel(downloadForge.openStream());
            FileOutputStream downloadForgeFileOutputStream = new FileOutputStream(modpackDir + "/server_pack/forge-installer.jar");
            FileChannel downloadForgeFileChannel = downloadForgeFileOutputStream.getChannel();
            downloadForgeFileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadForgeFileOutputStream.flush();
            downloadForgeFileOutputStream.close();
        } catch (IOException ex) {
            appLogger.error("An error occurred downloading Forge: ", ex);
        }
    }
    /** Optional. Depending on serverpackcreator.conf(includeZipCreation) this will delete Mojang's minecraft_server.jar from the zip-archive so users do not accidentally upload a file containing software from Mojang.
     * With help from https://stackoverflow.com/questions/5244963/delete-files-from-a-zip-archive-without-decompressing-in-java-or-maybe-python and https://bugs.openjdk.java.net/browse/JDK-8186227
     *  TODO: Write param docs
     * @param modLoader
     * @param modpackDir
     */
    static void deleteMinecraftJar(String modLoader, String modpackDir) {
        if (modLoader.equalsIgnoreCase("Forge")) {
            Map<String, String> zip_properties = new HashMap<>();
            zip_properties.put("create", "false");
            zip_properties.put("encoding", "UTF-8");
            Path serverpackZip = Paths.get(modpackDir + "/server_pack.zip");
            URI zipUri = URI.create("jar:" + serverpackZip.toUri());
            try (FileSystem zipfs = FileSystems.newFileSystem(zipUri, zip_properties)) {
                Path pathInZipfile = zipfs.getPath("minecraft_server.1.16.5.jar");
                appLogger.info("Deleting minecraft_server.jar from server_pack.zip.");
                Files.delete(pathInZipfile);
                appLogger.info("File successfully deleted");
            } catch (IOException ex) {
                appLogger.error("Error deleting minecraft-server.jar from archive.", ex);
            }
        } else if (modLoader.equalsIgnoreCase("Fabric")) {
            Map<String, String> zip_properties = new HashMap<>();
            zip_properties.put("create", "false");
            zip_properties.put("encoding", "UTF-8");
            Path serverpackZip = Paths.get(modpackDir + "/server_pack.zip");
            URI zipUri = URI.create("jar:" + serverpackZip.toUri());
            try (FileSystem zipfs = FileSystems.newFileSystem(zipUri, zip_properties)) {
                Path pathInZipfile = zipfs.getPath("server.jar");
                appLogger.info("Deleting minecraft_server.jar from server_pack.zip.");
                Files.delete(pathInZipfile);
                appLogger.info("File successfully deleted");
            } catch (IOException ex) {
                appLogger.error("Error deleting minecraft-server.jar from archive.", ex);
            }
        } else {
            appLogger.error("Specified invalid modloader: " + modLoader);
        }
    }
    /** Optional. This deletes remnant files from Fabric/Forge installation no longer needed.
     * TODO: Write param docs
     * @param fabricInstaller
     * @param forgeInstaller
     * @param modLoader
     * @param modpackDir
     * @param minecraftVersion
     * @param modLoaderVersion
     */
    static void cleanUpServerPack(File fabricInstaller, File forgeInstaller, String modLoader, String modpackDir, String minecraftVersion, String modLoaderVersion) {
        appLogger.info("Cleanup after modloader server installation.");
        if (modLoader.equalsIgnoreCase("Fabric")) {
            File fabricXML = new File(modpackDir + "/server_pack/fabric-installer.xml");
            boolean isXmlDeleted = fabricXML.delete();
            boolean isInstallerDeleted = fabricInstaller.delete();
            if (isXmlDeleted) { appLogger.info("Deleted " + fabricXML.getName()); }
            else { appLogger.error("Could not delete " + fabricXML.getName()); }
            if (isInstallerDeleted) { appLogger.info("Deleted " + fabricInstaller.getName()); }
            else { appLogger.error("Could not delete " + fabricInstaller.getName()); }
        } else if (modLoader.equalsIgnoreCase("Forge")) {
            try {
                Files.copy(Paths.get(modpackDir + "/server_pack/forge-" + minecraftVersion + "-" + modLoaderVersion + ".jar"), Paths.get(modpackDir + "/server_pack/forge.jar"), REPLACE_EXISTING);
                boolean isOldJarDeleted = (new File(modpackDir + "/server_pack/forge-" + minecraftVersion + "-" + modLoaderVersion + ".jar")).delete();
                boolean isInstallerDeleted = forgeInstaller.delete();
                if ((isOldJarDeleted) && (new File(modpackDir + "/server_pack/forge.jar").exists())) { appLogger.info("Renamed forge.jar and deleted old one."); }
                else { appLogger.error("There was an error during renaming or deletion of the forge server jar."); }
                if (isInstallerDeleted) { appLogger.info("Deleted " + forgeInstaller.getName()); }
                else { appLogger.error("Could not delete " + forgeInstaller.getName()); }
            } catch (IOException ex) {
                appLogger.error("Error during Forge cleanup.", ex);
            }
        } else {
            appLogger.error("Specified invalid modloader: " + modLoader);
        }
    }
}