package de.griefed.ServerPackCreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class ConfigCheck {
    private static final Logger appLogger = LogManager.getLogger("ConfigCheck");
    /** Optional. Check the specified Minecraft version against Mojang's version manifest to validate the version.
     *
     * @return Returns boolean depending on whether the specified Minecraft version could be found in Mojang's manifest.
     */
    static boolean isMinecraftVersionCorrect(String mcver) {
        try {
            URL manifestJsonURL = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
            FileOutputStream downloadManifestOutputStream = null;
            try {
                downloadManifestOutputStream = new FileOutputStream("mcmanifest.json");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                File file = new File("mcmanifest.json");
                if (!file.exists()){
                    appLogger.info("Manifest JSON File does not exist, creating...");
                    boolean jsonCreated = file.createNewFile();
                    if (jsonCreated) {
                        appLogger.info("Manifest JSON File created");
                    } else {
                        appLogger.error("Error. Could not create Manifest JSON File.");
                    }
                }
                downloadManifestOutputStream = new FileOutputStream("mcmanifest.json");
            }
            FileChannel downloadManifestOutputStreamChannel = downloadManifestOutputStream.getChannel();
            downloadManifestOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadManifestOutputStream.flush();
            downloadManifestOutputStream.close();
            File manifestJsonFile = new File("mcmanifest.json");
            Scanner myReader = new Scanner(manifestJsonFile);
            String data = myReader.nextLine();
            myReader.close();
            data = data.replaceAll("\\s", "");
            boolean contains = data.trim().contains(String.format("\"id\":\"%s\"", mcver));
            manifestJsonFile.deleteOnExit();
            return contains;
        } catch (Exception ex) {
            appLogger.error("An error occurred during Minecraft version validation.", ex);
            return false;
        }
    }
    /** Optional. Check the specified Minecraft version against Fabric's version manifest to validate the version.
     *
     * @return Returns boolean depending on whether the specified Minecraft version could be found in Fabric's manifest.
     */
    static boolean isFabricVersionCorrect(String version) {
        try {
            URL manifestJsonURL = new URL("https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml");
            ReadableByteChannel readableByteChannel = Channels.newChannel(manifestJsonURL.openStream());
            FileOutputStream downloadManifestOutputStream = null;
            try {
                downloadManifestOutputStream = new FileOutputStream("fabric-manifest.xml");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                File file = new File("fabric-manifest.xml");
                if (!file.exists()){
                    appLogger.info("Fabric Manifest XML File does not exist, creating...");
                    boolean jsonCreated = file.createNewFile();
                    if (jsonCreated) {
                        appLogger.info("Fabric Manifest XML File created");
                    } else {
                        appLogger.error("Error. Could not create Fabric Manifest XML File.");
                    }
                }
                downloadManifestOutputStream = new FileOutputStream("fabric-manifest.xml");
            }
            FileChannel downloadManifestOutputStreamChannel = downloadManifestOutputStream.getChannel();
            downloadManifestOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            downloadManifestOutputStream.flush();
            downloadManifestOutputStream.close();
            File manifestXMLFile = new File("fabric-manifest.xml");
            Scanner myReader = new Scanner(manifestXMLFile);
            ArrayList<String> d = new ArrayList<>();
            while (myReader.hasNextLine()) {
                d.add(myReader.nextLine());
            }
            String[] data = new String[d.size()];
            String manifestXML;
            d.toArray(data);
            manifestXML = Arrays.toString(data);
            myReader.close();
            manifestXML = manifestXML.replaceAll("\\s", "");
            boolean contains = manifestXML.trim().contains(String.format("<version>%s</version>", version));
            manifestXMLFile.deleteOnExit();
            return contains;
        } catch (Exception ex) {
            appLogger.error("An error occurred during Minecraft version validation.", ex);
            return false;
        }
    }

}
