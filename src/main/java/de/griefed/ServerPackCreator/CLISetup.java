package de.griefed.ServerPackCreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

class CLISetup {
    private static final Logger appLogger = LogManager.getLogger(CLISetup.class);

    /**
     * CLI for config file generation. Prompts user to enter config file values and then generates a config file with values entered by user.
     */
    static void setup() {
        List<String> clientMods,
                     copyDirs;

        clientMods = new ArrayList<>(0);
        copyDirs = new ArrayList<>(0);

        String[] tmpClientMods,
                 tmpCopyDirs;
        boolean includeServerInstallation,
                includeServerIcon,
                includeServerProperties,
                includeStartScripts,
                includeZipCreation;
        String modpackDir,
               javaPath,
               minecraftVersion,
               modLoader,
               modLoaderVersion,
               tmpModpackDir;

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
            do {
                clientMods.addAll(readStringArray());
                appLogger.info(String.format("You entered: %s", clientMods));
                appLogger.info("If you are satisfied with these values, enter true. If not, enter false to restart clientmod configuration.");
                System.out.print("Answer: ");
            } while (!readBoolean());
            appLogger.info(String.format("You entered: %s", clientMods));
            tmpClientMods = new String[clientMods.size()];
            clientMods.toArray(tmpClientMods);
            System.out.println();

            appLogger.info("Which directories should be copied to the server pack? These are folder names inside your modpack directory.");
            do {
                do {

                    copyDirs.clear();
                    appLogger.info("Specify your directories you want to be copied:");
                    copyDirs.addAll(readStringArray());
                } while (!ConfigCheck.checkCopyDirs(copyDirs, modpackDir));

                appLogger.info(String.format("You entered: %s", copyDirs));
                appLogger.info("If you are satisfied with these values, enter true. If not, enter false to restart clientmod configuration.");
                System.out.print("Answer: ");
            } while (!readBoolean());
            appLogger.info(String.format("You entered: %s", copyDirs));
            tmpCopyDirs = new String[copyDirs.size()];
            copyDirs.toArray(tmpCopyDirs);
            System.out.println();

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
                String tmpJavaPath = reader.nextLine();
                javaPath = ConfigCheck.getJavaPath(tmpJavaPath);
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

            ConfigCheck.printConfig(modpackDir,
                    clientMods,
                    copyDirs,
                    includeServerInstallation,
                    javaPath,
                    minecraftVersion,
                    modLoader,
                    modLoaderVersion,
                    includeServerIcon,
                    includeServerProperties,
                    includeStartScripts,
                    includeZipCreation);
            appLogger.info("If you are satisfied with these values, enter true. If not, enter false to restart config generation.");
            System.out.print("Answer: ");
        } while (!readBoolean());
        reader.close();

        if (FilesSetup.writeConfigToFile(
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
                includeZipCreation
        )) {
            appLogger.info("New config file successfully written. Thanks go to Whitebear60 for initially writing the CLI-Config-Generation.");
        }
    }

    /** A helper method for config setup. Prompts user to enter the values that will be stored in arrays in config.
    * @return String List. Returns list with user input values that will be stored in config.
    */
    private static List<String> readStringArray() {
        Scanner readerArray = new Scanner(System.in);
        ArrayList<String> result = new ArrayList<>(1);
        String stringArray;
        while (true) {
            stringArray = readerArray.nextLine();
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
    static String buildString(String... args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Arrays.toString(args));
        stringBuilder.delete(0, 2).reverse().delete(0,2).reverse();
        return stringBuilder.toString();
    }

    /** A helper method for config setup. Prompts user to enter boolean values that will be stored in config and checks entered values to prevent storing non-boolean values in boolean variables.
    * @return Boolean. Converts to boolean and returns value entered by user that will be stored in config.
    */
    private static boolean readBoolean() {
        Scanner readerBoolean = new Scanner(System.in);
        String boolRead;
        while (true) {
            boolRead = readerBoolean.nextLine();
            if (boolRead.matches("[Tt]rue") || boolRead.matches("1") || boolRead.matches("[Yy]es")|| boolRead.matches("[Yy]") || boolRead.matches(LocalizationManager.getLocalizedString("cli.input.yes")) || boolRead.matches(LocalizationManager.getLocalizedString("cli.input.yes.short"))) {
                return true;
            } else if (boolRead.matches("[Ff]alse") || boolRead.matches("0") || boolRead.matches("[Nn]o") || boolRead.matches("[Nn]" ) || boolRead.matches(LocalizationManager.getLocalizedString("cli.input.no")) || boolRead.matches(LocalizationManager.getLocalizedString("cli.input.no.short"))){
                return false;
            } else {
                appLogger.error("Incorrect value specified. Please try again.");
            }
        }
    }
}
