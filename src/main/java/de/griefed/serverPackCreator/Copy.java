package de.griefed.serverPackCreator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Copy {

    public static final File configFile = new File("creator.conf");
    public static final File propertiesFile = new File("server.properties");
    public static final File iconFile = new File("server-icon.png");
    public static final File windowsFile = new File("start.bat");
    public static final File linuxFile = new File("start.sh");

    public Copy() {
        // Copy all default files to base directory where jar resides.
        if (!configFile.exists()) {
            try {
                InputStream link = (getClass().getResourceAsStream("/" + configFile.getName()));
                Files.copy(link, configFile.getAbsoluteFile().toPath());
                link.close();
                System.out.println("Default config file generated. Please customize.");
            } catch (IOException e) {
                System.err.println("Could not extract default config-file");
                e.printStackTrace();
            }
        }
        if (!propertiesFile.exists()) {
            try {
                InputStream link = (getClass().getResourceAsStream("/" + propertiesFile.getName()));
                Files.copy(link, propertiesFile.getAbsoluteFile().toPath());
                link.close();
                System.out.println("Default server.properties file generated. Please customize if you intend on using it.");
            } catch (IOException e) {
                System.err.println("Could not extract default server.properties file");
                e.printStackTrace();
            }
        }
        if (!iconFile.exists()) {
            try {
                InputStream link = (getClass().getResourceAsStream("/" + iconFile.getName()));
                Files.copy(link, iconFile.getAbsoluteFile().toPath());
                link.close();
                System.out.println("Default server-icon.png file generated. Please customize if you intend on using it.");
            } catch (IOException e) {
                System.err.println("Could not extract default server-icon.png file");
                e.printStackTrace();
            }
        }
        if (!windowsFile.exists()) {
            try {
                InputStream link = (getClass().getResourceAsStream("/" + windowsFile.getName()));
                Files.copy(link, windowsFile.getAbsoluteFile().toPath());
                link.close();
                System.out.println("Default Windows start file generated.");
            } catch (IOException e) {
                System.err.println("Could not extract default Windows start file");
                e.printStackTrace();
            }
        }
        if (!linuxFile.exists()) {
            try {
                InputStream link = (getClass().getResourceAsStream("/" + linuxFile.getName()));
                Files.copy(link, linuxFile.getAbsoluteFile().toPath());
                link.close();
                System.out.println("Default Linux start file generated.");
            } catch (IOException e) {
                System.err.println("Could not extract default Linux start file");
                e.printStackTrace();
            }
        }
        // Continue to load values from config file.
        Conf.loadConfig();
    }

    // Copy all specified directories from modpack to serverpack.
    public static void copyFiles(String modpackDir, List<String> copyDirs) throws IOException {
        String serverPath = modpackDir + "/server_pack";
        Files.createDirectories(Paths.get(serverPath));
        for (int i = 0; i<copyDirs.toArray().length; i++) {
            String clientDir = modpackDir + "/" + copyDirs.get(i);
            String serverDir = serverPath + "/" + copyDirs.get(i);
            Files.createDirectories(Paths.get(serverDir));
            System.out.println("Setting up " + serverDir + " files.");
            try {
                Stream<Path> files = Files.walk(Paths.get(clientDir));
                files.forEach(file -> {
                    try {
                        Files.copy(file, Paths.get(serverDir).resolve(Paths.get(clientDir).relativize(file)),REPLACE_EXISTING);
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                });
                files.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }

    // If true, copy server-icon to serverpack.
    public static void copyIcon(String modpackDir) {
        String sourceIcon = "./" + iconFile;
        String destIcon = modpackDir + "/server_pack/" + iconFile;
        System.out.println("Copying server-icon.png...");
        try {
            Files.copy(Paths.get(sourceIcon), Paths.get(destIcon), REPLACE_EXISTING);
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    // If true, copy server.properties to serverpack.
    public static void copyProperties(String modpackDir) {
        String sourceProperties = "./" + propertiesFile;
        String destProperties = modpackDir + "/server_pack/" + propertiesFile;
        System.out.println("Copying server.properties...");
        try {
            Files.copy(Paths.get(sourceProperties), Paths.get(destProperties), REPLACE_EXISTING);
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    // If true, copy start scripts to serverpack.
    public static void copyStartScripts(String modpackDir) {
        String sourceWindows = "./" + windowsFile;
        String destWindows = modpackDir + "/server_pack/" + windowsFile;
        System.out.println("Copying start scripts...");
        try {
            Files.copy(Paths.get(sourceWindows), Paths.get(destWindows), REPLACE_EXISTING);
        } catch (IOException e) {
            //e.printStackTrace();
        }
        String sourceLinux = "./" + linuxFile;
        String destLinux = modpackDir + "/server_pack/" + linuxFile;
        try {
            Files.copy(Paths.get(sourceLinux), Paths.get(destLinux), REPLACE_EXISTING);
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

}
