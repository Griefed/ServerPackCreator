package com.griefed;

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
        String serverModsDir = Folders.serverModsPath(packDir);
        try {
            // create stream for `src`
            Stream<Path> files = Files.walk(Paths.get(clientModDir));
            // copy all files and folders from `src` to `dest`
            files.forEach(file -> {
                try {
                    Files.copy(file, Paths.get(serverModsDir).resolve(Paths.get(clientModDir).relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            // close the stream
            files.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    // Copy all configs from client to server
    public static void copyConfig(String packDir) throws IOException {
        String clientConfigDir = packDir + "/config";
        String serverConfigDir = Folders.serverConfigPath(packDir);
        try {
            // create stream for `src`
            Stream<Path> files = Files.walk(Paths.get(clientConfigDir));
            // copy all files and folders from `src` to `dest`
            files.forEach(file -> {
                try {
                    Files.copy(file, Paths.get(serverConfigDir).resolve(Paths.get(clientConfigDir).relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            // close the stream
            files.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    // Copy all scripts from client to server
    public static void copyScripts(String packDir) throws IOException {
        String clientScriptsDir = packDir + "/scripts";
        String serverScriptsDir = Folders.serverScriptsPath(packDir);
        try {
            // create stream for `src`
            Stream<Path> files = Files.walk(Paths.get(clientScriptsDir));
            // copy all files and folders from `src` to `dest`
            files.forEach(file -> {
                try {
                    Files.copy(file, Paths.get(serverScriptsDir).resolve(Paths.get(clientScriptsDir).relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            // close the stream
            files.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    // Copy all defaultconfigs from client to server
    public static void copyDefaultconfigs(String packDir) throws IOException {
        String clientDefaultconfigsDir = packDir + "/defaultconfigs";
        String serverDefaultconfigsDir = Folders.serverDefaultconfigsPath(packDir);
        try {
            // create stream for `src`
            Stream<Path> files = Files.walk(Paths.get(clientDefaultconfigsDir));
            // copy all files and folders from `src` to `dest`
            files.forEach(file -> {
                try {
                    Files.copy(file, Paths.get(serverDefaultconfigsDir).resolve(Paths.get(clientDefaultconfigsDir).relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            // close the stream
            files.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
