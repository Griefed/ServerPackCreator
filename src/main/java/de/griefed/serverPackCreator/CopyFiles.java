package de.griefed.serverPackCreator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public class CopyFiles {
    // Copy all mods from client to server
    public static void copyMods(String packDir) throws IOException {
        String clientModDir = packDir + "/mods";
        String serverModsDir = CreateFolders.serverModsPath(packDir);
        try {
            Stream<Path> files = Files.walk(Paths.get(clientModDir));
            files.forEach(file -> {
                try {
                    Files.copy(file, Paths.get(serverModsDir).resolve(Paths.get(clientModDir).relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            files.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    // Copy all configs from client to server
    public static void copyConfig(String packDir) throws IOException {
        String clientConfigDir = packDir + "/config";
        String serverConfigDir = CreateFolders.serverConfigPath(packDir);
        try {
            Stream<Path> files = Files.walk(Paths.get(clientConfigDir));
            files.forEach(file -> {
                try {
                    Files.copy(file, Paths.get(serverConfigDir).resolve(Paths.get(clientConfigDir).relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            files.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    // Copy all scripts from client to server
    public static void copyScripts(String packDir) throws IOException {
        String clientScriptsDir = packDir + "/scripts";
        String serverScriptsDir = CreateFolders.serverScriptsPath(packDir);
        try {
            Stream<Path> files = Files.walk(Paths.get(clientScriptsDir));
            files.forEach(file -> {
                try {
                    Files.copy(file, Paths.get(serverScriptsDir).resolve(Paths.get(clientScriptsDir).relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            files.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    // Copy all defaultconfigs from client to server
    public static void copyDefaultconfigs(String packDir) throws IOException {
        String clientDefaultconfigsDir = packDir + "/defaultconfigs";
        String serverDefaultconfigsDir = CreateFolders.serverDefaultconfigsPath(packDir);
        try {
            Stream<Path> files = Files.walk(Paths.get(clientDefaultconfigsDir));
            files.forEach(file -> {
                try {
                    Files.copy(file, Paths.get(serverDefaultconfigsDir).resolve(Paths.get(clientDefaultconfigsDir).relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            files.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
