package de.griefed.serverPackCreator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    public static final File forgeWindowsFile = new File("start-forge.bat");
    public static final File forgeLinuxFile = new File("start-forge.sh");
    public static final File fabricWindowsFile = new File("start-fabric.bat");
    public static final File fabricLinuxFile = new File("start-fabric.sh");

    public Copy() {
        Main.warning();
        boolean firstRun = true;
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
        } else {
            firstRun = false;
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
        if (firstRun) {
            System.out.println("First run! Default files generated. Please customize and run again.");
            System.exit(0);
        } else {
            // Continue to load values from config file.
            Conf.loadConfig();
        }
    }

    public static void copyStartScripts(String modpackDir, String modLoader, String modLoaderVersion, boolean includeStartScripts) {
        if (modLoader.equals("Forge")) {
            if (!forgeWindowsFile.exists()) {
                try {
                    InputStream link = (Copy.class.getResourceAsStream("/" + forgeWindowsFile.getName()));
                    Files.copy(link, forgeWindowsFile.getAbsoluteFile().toPath());
                    link.close();
                    System.out.println("Default Forge Windows start file generated.");
                } catch (IOException e) {
                    System.err.println("Could not extract default Forge Windows start file");
                    //e.printStackTrace();
                }
            }
            if (!forgeLinuxFile.exists()) {
                try {
                    InputStream link = (Copy.class.getResourceAsStream("/" + forgeLinuxFile.getName()));
                    Files.copy(link, forgeLinuxFile.getAbsoluteFile().toPath());
                    link.close();
                    System.out.println("Default Forge Linux start file generated.");
                } catch (IOException e) {
                    System.err.println("Could not extract default Forge Linux start file");
                    //e.printStackTrace();
                }
            }
            System.out.println("Copying Forge start scripts...");
            try {
                Files.copy(Paths.get("./" + forgeWindowsFile), Paths.get(modpackDir + "/server_pack/" + forgeWindowsFile), REPLACE_EXISTING);
                Files.copy(Paths.get("./" + forgeLinuxFile), Paths.get(modpackDir + "/server_pack/" + forgeLinuxFile), REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (modLoader.equals("Fabric")) {
            if (!fabricWindowsFile.exists()) {
                try {
                    InputStream link = (Copy.class.getResourceAsStream("/" + fabricWindowsFile.getName()));
                    Files.copy(link, fabricWindowsFile.getAbsoluteFile().toPath());
                    link.close();
                    System.out.println("Default Fabric Windows start file generated.");
                } catch (IOException e) {
                    System.err.println("Could not extract default Fabric Windows start file");
                    //e.printStackTrace();
                }
            }
            if (!fabricLinuxFile.exists()) {
                try {
                    InputStream link = (Copy.class.getResourceAsStream("/" + fabricLinuxFile.getName()));
                    Files.copy(link, fabricLinuxFile.getAbsoluteFile().toPath());
                    link.close();
                    System.out.println("Default Fabric Linux start file generated.");
                } catch (IOException e) {
                    System.err.println("Could not extract default Fabric Linux start file");
                    //e.printStackTrace();
                }
            }
            System.out.println("Copying Fabric start scripts...");
            try {
                Files.copy(Paths.get("./" + fabricWindowsFile), Paths.get(modpackDir + "/server_pack/" + fabricWindowsFile), REPLACE_EXISTING);
                Files.copy(Paths.get("./" + fabricLinuxFile), Paths.get(modpackDir + "/server_pack/" + fabricLinuxFile), REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Specified invalid modloader. Must be either Forge or Fabric.");
        }
    }

    // Copy all specified directories from modpack to serverpack.
    public static void copyFiles(String modpackDir, List<String> copyDirs) throws IOException {
        String serverPath = modpackDir + "/server_pack";
        Files.createDirectories(Paths.get(serverPath));
        for (int i = 0; i<copyDirs.toArray().length; i++) {
            String clientDir = modpackDir + "/" + copyDirs.get(i);
            String serverDir = serverPath + "/" + copyDirs.get(i);
            //Files.createDirectories(Paths.get(serverDir));
            System.out.println("Setting up " + serverDir + " files.");
            if (copyDirs.get(i).startsWith("saves/")) {
                String savesDir = serverPath + "/" + copyDirs.get(i).substring(6);
                try {
                    Stream<Path> files = Files.walk(Paths.get(clientDir));
                    files.forEach(file -> {
                        try {
                            Files.copy(file, Paths.get(savesDir).resolve(Paths.get(clientDir).relativize(file)), REPLACE_EXISTING);
                        } catch (IOException e) {
                            //e.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            } else {
                try {
                    Stream<Path> files = Files.walk(Paths.get(clientDir));
                    files.forEach(file -> {
                        try {
                            Files.copy(file, Paths.get(serverDir).resolve(Paths.get(clientDir).relativize(file)), REPLACE_EXISTING);
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
    }

    // If true, copy server-icon to serverpack.
    public static void copyIcon(String modpackDir) {
        System.out.println("Copying server-icon.png...");
        try {
            Files.copy(Paths.get("./" + iconFile), Paths.get(modpackDir + "/server_pack/" + iconFile), REPLACE_EXISTING);
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    // If true, copy server.properties to serverpack.
    public static void copyProperties(String modpackDir) {
        System.out.println("Copying server.properties...");
        try {
            Files.copy(Paths.get("./" + propertiesFile), Paths.get(modpackDir + "/server_pack/" + propertiesFile), REPLACE_EXISTING);
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    // If true, copy start scripts to serverpack.
    /*
    public static void copyStartScripts(String modpackDir, String modLoader) {
        if (modLoader.equals("Forge")) {
            System.out.println("Copying start scripts...");
            try {
                Files.copy(Paths.get(("./" + forgeWindowsFile)), Paths.get((modpackDir + "/server_pack/" + forgeWindowsFile)), REPLACE_EXISTING);
                Files.copy(Paths.get(("./" + forgeLinuxFile)), Paths.get((modpackDir + "/server_pack/" + forgeLinuxFile)), REPLACE_EXISTING);
            } catch (IOException e) {
                //e.printStackTrace();
            }
        } else if (modLoader.equals("Fabric")) {
            System.out.println("Copying start scripts...");
            try {
                Files.copy(Paths.get(("./" + fabricWindowsFile)), Paths.get((modpackDir + "/server_pack/" + fabricWindowsFile)), REPLACE_EXISTING);
                Files.copy(Paths.get(("./" + fabricLinuxFile)), Paths.get((modpackDir + "/server_pack/" + fabricLinuxFile)), REPLACE_EXISTING);
            } catch (IOException e) {
                //e.printStackTrace();
            }
        } else {
            System.out.println("Specified invalid modloader. Start scripts not copied.");
        }
    }

     */
}
