package com.griefed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CreateFolders {
    public static String serverPath(String packDir) throws IOException {
        // Create server pack folder and serverPath to use later on
        String serverPath = packDir + "/server_pack";
        Files.createDirectories(Paths.get(serverPath));
        return serverPath;
    }
    public static String serverModsPath(String packDir) throws IOException {
        // Create server pack folder and serverPath to use later on
        String serverModsPath = serverPath(packDir) + "/mods";
        Files.createDirectories(Paths.get(serverModsPath));
        return serverModsPath;
    }
    public static String serverConfigPath(String packDir) throws IOException {
        // Create server pack folder and serverPath to use later on
        String serverConfigPath = serverPath(packDir) + "/config";
        Files.createDirectories(Paths.get(serverConfigPath));
        return serverConfigPath;
    }
    public static String serverDefaultconfigsPath(String packDir) throws IOException {
        // Create server pack folder and serverPath to use later on
        String serverDefaultconfigsPath = serverPath(packDir) + "/defaultconfigs";
        Files.createDirectories(Paths.get(serverDefaultconfigsPath));
        return serverDefaultconfigsPath;
    }
    public static String serverScriptsPath(String packDir) throws IOException {
        // Create server pack folder and serverPath to use later on
        String serverScriptsPath = serverPath(packDir) + "/scripts";
        Files.createDirectories(Paths.get(serverScriptsPath));
        return serverScriptsPath;
    }
}
