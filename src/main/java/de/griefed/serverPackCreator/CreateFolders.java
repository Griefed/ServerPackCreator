package de.griefed.serverPackCreator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CreateFolders {
    // Create server pack folder and serverPath to use later on
    public static String serverPath(String packDir) throws IOException {
        String serverPath = packDir + "/server_pack";
        Files.createDirectories(Paths.get(serverPath));
        return serverPath;
    }
    // Create server pack folder and serverPath to use later on
    public static String serverModsPath(String packDir) throws IOException {
        String serverModsPath = serverPath(packDir) + "/mods";
        Files.createDirectories(Paths.get(serverModsPath));
        return serverModsPath;
    }
    // Create server pack folder and serverPath to use later on
    public static String serverConfigPath(String packDir) throws IOException {
        String serverConfigPath = serverPath(packDir) + "/config";
        Files.createDirectories(Paths.get(serverConfigPath));
        return serverConfigPath;
    }
    // Create server pack folder and serverPath to use later on
    public static String serverDefaultconfigsPath(String packDir) throws IOException {
        String serverDefaultconfigsPath = serverPath(packDir) + "/defaultconfigs";
        Files.createDirectories(Paths.get(serverDefaultconfigsPath));
        return serverDefaultconfigsPath;
    }
    // Create server pack folder and serverPath to use later on
    public static String serverScriptsPath(String packDir) throws IOException {
        String serverScriptsPath = serverPath(packDir) + "/scripts";
        Files.createDirectories(Paths.get(serverScriptsPath));
        return serverScriptsPath;
    }
}
