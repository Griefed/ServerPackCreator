package de.griefed.ServerPackCreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

class CLISetup {
    private static final Logger appLogger = LogManager.getLogger("ApplicationLogger");
    /**
     * CLI for config file generation. Prompts user to enter config file values and then generates a config file with values entered by user.
     */
    static void setup() {
        String modpackDir;
        List<String> clientMods = new ArrayList<>(0);
        String tmpModpackDir;
        String[] tmpClientMods;
        List<String> copyDirs;
        String[] tmpCopyDirs;
        boolean includeServerInstallation;
        String javaPath;
        String minecraftVersion;
        String modLoader;
        String modLoaderVersion;
        boolean includeServerIcon;
        boolean includeServerProperties;
        boolean includeStartScripts;
        boolean includeZipCreation;
        Scanner reader = new Scanner(System.in);
        appLogger.info(String.format("You started ServerPackCreator with the \"%s\" argument. Step-by-step generation of config file initiated...", Reference.CONFIG_GEN_ARGUMENT));
        do {
            appLogger.info("Please enter your modpack path. This path can be relative to ServerPackCreator, or absolute.");
            appLogger.info("Example: \"./Some Modpack\" or \"C:\\Minecraft\\Some Modpack\"");
            do {
                System.out.print("Path to modpack directory: ");
                tmpModpackDir = reader.nextLine();
            } while (!ConfigCheck.checkModpackDir(tmpModpackDir));
            modpackDir = tmpModpackDir.replace("\\", "/");
            appLogger.info(String.format("You entered: %s", modpackDir));
            System.out.println();
            appLogger.info("Enter filenames of clientside-only mods, one per line. When you are done, simply press enter with empty input.");
            clientMods.addAll(readStringArray());
            appLogger.info(String.format("You entered: %s", clientMods.toString()));
            tmpClientMods = new String[clientMods.size()];
            clientMods.toArray(tmpClientMods);
            appLogger.info("Which directories should be copied to the server pack? These are folder names inside your modpack directory.");
            do {
                appLogger.info("Specify your directories you want to be copied:");
                copyDirs = new ArrayList<>(0);
                copyDirs.addAll(readStringArray());
            } while (!ConfigCheck.checkCopyDirs(copyDirs, modpackDir));
            appLogger.info(String.format("You entered: %s", copyDirs.toString()));
            tmpCopyDirs = new String[copyDirs.size()];
            copyDirs.toArray(tmpCopyDirs);
            appLogger.info("Do you want ServerPackCreator to install the modloader server for your server pack? Must be true or false.");
            System.out.print("Include modloader server installation: ");
            includeServerInstallation = readBoolean();
            appLogger.info(String.format("You entered: %s", includeServerInstallation));
            appLogger.info("Which version of Minecraft does your modpack use?");
            do {
                System.out.print("Minecraft version: ");
                minecraftVersion = reader.nextLine();
            } while (!ConfigCheck.isMinecraftVersionCorrect(minecraftVersion));
            appLogger.info(String.format("You entered: %s", minecraftVersion));
            System.out.println();
            appLogger.info("What modloader does your modpack use?");
            do {
                System.out.print("Modloader: ");
                modLoader = reader.nextLine();
            } while (!ConfigCheck.checkModloader(modLoader));
            modLoader = ConfigCheck.setModloader(modLoader);
            appLogger.info(String.format("You entered: %s", modLoader));
            System.out.println();
            appLogger.info(String.format("What version of %s does your modpack use?", modLoader));
            do {
                System.out.print("Modloader version: ");
                modLoaderVersion = reader.nextLine();
            } while (!ConfigCheck.checkModloaderVersion(modLoader, modLoaderVersion, minecraftVersion));
            appLogger.info(String.format("You entered: %s", modLoaderVersion));
            System.out.println();
            appLogger.info("Specify the path to your Java installation. Must end with \"java\" on Linux, or \"java.exe\" on Windows.");
            appLogger.info("If you leave this empty, ServerPackCreator will try to determine the path for you.");
            appLogger.info("Example Linux: /usr/bin/java | Example Windows: C:/Program Files/AdoptOpenJDK/jdk-8.0.275.1-hotspot/jre/bin/java.exe");
            do {
                System.out.print("Path to your Java installation: ");
                javaPath = reader.nextLine();
                if (javaPath.isEmpty()) {
                    appLogger.warn("You didn't enter a path to your Java installation. ServerPackCreator will try to get it for you...");
                    String tmpJavaPath = System.getProperty("java.home").replace("\\", "/") + "/bin/java";
                    if (tmpJavaPath.startsWith("C:")) {
                        tmpJavaPath = String.format("%s.exe", tmpJavaPath);
                    }
                    javaPath = tmpJavaPath;
                    appLogger.warn(String.format("ServerPackCreator set the path to your Java installation to: %s", javaPath));
                }
            } while (!ConfigCheck.checkJavaPath(javaPath));
            System.out.println();
            appLogger.info("Do you want ServerPackCreator to include a server-icon in your server pack? Must be true or false.");
            System.out.print("Include server-icon.png: ");
            includeServerIcon = readBoolean();
            appLogger.info(String.format("You entered: %s", includeServerIcon));
            System.out.println();
            appLogger.info("Do you want ServerPackCreator to include a server.properties in your server pack? Must be true or false.");
            System.out.print("Include server.properties: ");
            includeServerProperties = readBoolean();
            appLogger.info(String.format("You entered: %s", includeServerProperties));
            System.out.println();
            appLogger.info("Do you want ServerPackCreator to include start scripts for Linux and Windows in your server pack? Must be true or false.");
            System.out.print("Include start scripts: ");
            includeStartScripts = readBoolean();
            appLogger.info(String.format("You entered: %s", includeStartScripts));
            System.out.println();
            appLogger.info("Do you want ServerPackCreator to create a ZIP-archive of your server pack? Must be true or false.");
            System.out.print("Create ZIP-archive: ");
            includeZipCreation = readBoolean();
            appLogger.info(String.format("You entered: %s", includeZipCreation));
            ConfigCheck.printConfig(modpackDir, clientMods, copyDirs, includeServerInstallation, javaPath, minecraftVersion, modLoader, modLoaderVersion, includeServerIcon, includeServerProperties, includeStartScripts, includeZipCreation);
            appLogger.info("If you are satisfied with these values, enter true. If not, enter false to restart config generation.");
            System.out.print("Answer: ");
        } while (!readBoolean());
        String configString = String.format("# Path to your modpack. Can be either relative or absolute.\n" +
            "# Example: \"./Some Modpack\" or \"C:\\Minecraft\\Some Modpack\"\n" +
            "modpackDir = \"%s\"\n" +
            "\n" +
            "# List of client-only mods to delete from serverpack.\n" +
            "# No need to include version specifics. Must be the filenames of the mods, not their project names on CurseForge!\n" +
            "# Example: [\"AmbientSounds\", \"ClientTweaks\", \"PackMenu\", \"BetterAdvancement\", \"jeiintegration\"]\n" +
            "clientMods = [%s]\n" +
            "\n" +
            "# Name of directories to include in serverpack.\n" +
            "# When specifying \"saves/world_name\", \"world_name\" will be copied to the base directory of the serverpack\n" +
            "# for immediate use with the server.\n" +
            "# Example: [\"config\", \"mods\", \"scripts\"]\n" +
            "copyDirs = [%s]\n" +
            "\n" +
            "# Whether to install a Forge/Fabric server for the serverpack. Must be true or false.\n" +
            "includeServerInstallation = %b\n" +
            "\n" +
            "# Path to the Java executable. On Linux systems it would be something like \"/usr/bin/java\".\n" +
            "# Only needed if includeServerInstallation is true.\n" +
            "javaPath = \"%s\"\n" +
            "\n" +
            "# Which Minecraft version to use.\n" +
            "# Example: \"1.16.5\".\n" +
            "# Only needed if includeServerInstallation is true.\n" +
            "minecraftVersion = \"%s\"\n" +
            "\n" +
            "# Which modloader to install. Must be either \"Forge\" or \"Fabric\".\n" +
            "# Only needed if includeServerInstallation is true.\n" +
            "modLoader = \"%s\"\n" +
            "\n" +
            "# The version of the modloader you want to install.\n" +
            "# Example for Fabric=\"0.7.3\", example for Forge=\"36.0.15\".\n" +
            "# Only needed if includeServerInstallation is true.\n" +
            "modLoaderVersion = \"%s\"\n" +
            "\n" +
            "# Include a server-icon.png in your serverpack. Must be true or false.\n" +
            "# Place your server-icon.png in the same directory as this cfg-file.\n" +
            "# If no server-icon.png is provided but is set to true, a default one will be provided.\n" +
            "# Dimensions must be 64x64!\n" +
            "includeServerIcon = %b\n" +
            "\n" +
            "# Include a server.properties in your serverpack. Must be true or false.\n" +
            "# Place your server.properties in the same directory as this cfg-file.\n" +
            "# If no server.properties is provided but is set to true, a default one will be provided.\n" +
            "# Default value is true\n" +
            "includeServerProperties = %b\n" +
            "\n" +
            "# Include start scripts for windows and linux systems. Must be true or false.\n" +
            "# Default value is true\n" +
            "includeStartScripts = %b\n" +
            "\n" +
            "# Create zip-archive of serverpack. Must be true or false.\n" +
            "# Default value is true\n" +
            "includeZipCreation = %b\n",
            modpackDir,
            buildString(Arrays.toString(tmpClientMods)),
            buildString(Arrays.toString(tmpCopyDirs)),
            includeServerInstallation,
            javaPath,
            minecraftVersion,
            modLoader,
            modLoaderVersion,
            includeServerIcon,
            includeServerProperties,
            includeStartScripts,
            includeZipCreation);
        try {
            if (Reference.configFile.exists()) {
            boolean delConf = Reference.configFile.delete();
            if (delConf) { appLogger.info("Deleted existing config file to replace with new one."); }
            else { appLogger.error("Could not delete existing config file."); }
            }
            if (Reference.oldConfigFile.exists()) {
            boolean delOldConf = Reference.oldConfigFile.delete();
            if (delOldConf) { appLogger.info("Deleted existing config file to replace with new one."); }
            else { appLogger.error("Could not delete existing config file."); }
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(Reference.configFile));
            writer.write(configString);
            writer.close();
        } catch (IOException ex) {
            appLogger.error("Error writing new config file.", ex);
        }
    }
    /** A helper method for config setup. Prompts user to enter the values that will be stored in arrays in config.
    * @return String List. Returns list with user input values that will be stored in config.
    */
    private static List<String> readStringArray() {
        Scanner reader = new Scanner(System.in);
        ArrayList<String> result = new ArrayList<>(1);
        String stringArray;
        while (true) {
            stringArray = reader.nextLine();
            if (stringArray.isEmpty()) {
                return result;
            } else {
                result.add(stringArray);
            }
        }
    }
    /** Converts list of strings into concatenated string.
     * @param args Strings that will be concatenated into one string
     * @return String. Returns concatenated string that contains all provided values.
     */
    private static String buildString(String... args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Arrays.toString(args));
        stringBuilder.delete(0, 2).reverse().delete(0,2).reverse();
        return stringBuilder.toString();
    }
    /** A helper method for config setup. Prompts user to enter boolean values that will be stored in config and checks entered values to prevent storing non-boolean values in boolean variables.
    * @return Boolean. Converts to boolean and returns value entered by user that will be stored in config.
    */
    private static boolean readBoolean() {
        Scanner reader = new Scanner(System.in);
        String boolRead;
        while (true) {
            boolRead = reader.nextLine();
            if (boolRead.matches("[Tt]rue") || boolRead.matches("1") || boolRead.matches("[Yy]es")|| boolRead.matches("[Yy]")) {
                return true;
            } else if (boolRead.matches("[Ff]alse") || boolRead.matches("0") || boolRead.matches("[Nn]o") || boolRead.matches("[Nn]" )){
                return false;
            } else {
                appLogger.error("Incorrect value specified. Please try again.");
            }
        }
    }
}