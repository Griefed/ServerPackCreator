package de.griefed.ServerPackCreator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

class CLISetup {
  /**
   * CLI for config file generation. Prompts user to enter config file values and then generates a config file with values entered by user.
   */
  static void setup() {
    String modpackDir;
    List<String> clientMods = new ArrayList<>(0);
    List<String> copyDirs = new ArrayList<>(0);
    boolean includeServerInstallation;
    String javaPath = "";
    String minecraftVersion = "";
    String modLoader = "";
    String modLoaderVersion = "";
    Boolean includeServerIcon;
    Boolean includeServerProperties;
    Boolean includeStartScripts;
    Boolean includeZipCreation;

    Scanner reader = new Scanner(System.in);


    System.out.println("First of all, enter your modpack path. This path can be relative or absolute.");
    System.out.println("Example: \"./Some Modpack\" or \"C:\\Minecraft\\Some Modpack\"");
    System.out.println();
    System.out.print("Enter your modpack path: ");
    modpackDir = reader.nextLine();
    System.out.println();
    System.out.println("Enter client mods names, one per line. When you are done, simply press enter with empty input.");
    clientMods.addAll(readStringArray());

    String[] cm = new String[clientMods.size()];
    clientMods.toArray(cm);
    System.out.println("What directories to copy? Specify relative paths from the modpack path you have set already.");
    copyDirs.addAll(readStringArray());
    String[] cd = new String[copyDirs.size()];
    copyDirs.toArray(cd);



    System.out.println("Include server installation?");
    includeServerInstallation = readBoolean();
    if (includeServerInstallation) {
      System.out.println("Which Minecraft version your modpack uses?");
      while (true) {
        minecraftVersion = reader.nextLine();
        if (ConfigCheck.isMinecraftVersionCorrect(minecraftVersion)) break; else {
          System.out.println("Incorrect Minecraft version specified, please, try again.");
        }
      }
      System.out.println();
      System.out.println("What mod loader do you want to use?");
      while (true) {
        modLoader = reader.nextLine();
        if (modLoader.matches("Forge") || modLoader.matches("Fabric")) {
          break;
        } else {
          System.out.println("Invalid mod loader specified, please, try again.");
        }
      }
      System.out.println();
      System.out.println("What mod loader version do you want to use?");
      while (true) {
        modLoaderVersion = reader.nextLine();
        if (modLoaderVersion.isEmpty()) {
          System.out.println("Mod loader version can't be empty, please, try again.");
        } else if (modLoader.matches("Fabric") && !ConfigCheck.isFabricVersionCorrect(modLoaderVersion)) {
          System.out.println("Fabric version is incorrect, please, try again.");
        } else if (modLoader.matches("Forge") && !ConfigCheck.isForgeVersionCorrect(modLoaderVersion, minecraftVersion)) {
          System.out.println("Forge version is incorrect, please, try again.");
        } else break;
      }
      System.out.println();
      System.out.println("When Java is located on your disk?");
      while (true) {
        javaPath = reader.nextLine();
        if (!javaPath.isEmpty() && !javaPath.endsWith("java") && !javaPath.endsWith("java.exe")) {
          System.out.println("Error: Incorrect Java path specified. The java path must end with \"java\" or \"java.exe\".");
        } else if (javaPath.isEmpty()) {
          javaPath = System.getProperty("java.home") + "/bin";
          System.out.println(javaPath);
          break;
        } else break;
      }
    }
    System.out.println();
    System.out.println("Include server icon?");
    includeServerIcon = readBoolean();
    System.out.println();
    System.out.println("Include server.properties?");
    includeServerProperties = readBoolean();
    System.out.println();
    System.out.println("Include start scripts?");
    includeStartScripts = readBoolean();
    System.out.println();
    System.out.println("Create ZIP version of server pack?");
    includeZipCreation = readBoolean();

    String s = String.format("# Path to your modpack. Can be either relative or absolute.\n" +
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
            "includeZipCreation = %b\n", modpackDir, buildString(Arrays.toString(cm)), buildString(Arrays.toString(cd)), includeServerInstallation, javaPath, minecraftVersion, modLoader, modLoaderVersion, includeServerIcon, includeServerProperties, includeStartScripts, includeZipCreation);

    try {
      if (FilesSetup.configFile.exists()) {
        FilesSetup.configFile.delete();
      }
      if (FilesSetup.oldConfigFile.exists()) {
        FilesSetup.oldConfigFile.delete();
      }
      BufferedWriter writer = new BufferedWriter(new FileWriter(FilesSetup.configFile));
      writer.write(s);
      writer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * A helper method for config setup. Prompts user to enter the values that will be stored in arrays in config.
   * @return Returns list with user input values that will be stored in config.
   */
  private static List<String> readStringArray() {
    Scanner reader = new Scanner(System.in);
    ArrayList<String> result = new ArrayList<>(1);
    String s;
    while (true) {
      s = reader.nextLine();
      if (s.isEmpty()) {
        return result;
      } else {
        result.add(s);
      }
    }
  }

  /**
   * @param args Strings that will be concatenated into one string
   * @return Returns concatenated string that contains all provided values.
   */
  private static String buildString(String... args) {
    StringBuilder sb = new StringBuilder();
    sb.append(Arrays.toString(args));
    sb.delete(0, 2).reverse().delete(0,2).reverse();
//    System.out.println(sb);
//    System.out.println(Arrays.toString(sb.));
    return sb.toString();
  }
  /*
  * A helper method for config setup. Prompts user to enter boolean values that will be stored in config and checks entered values to prevent storing non-boolean values in boolean variables.
  * @return Returns value entered by user that will be stored in config.
  */
  private static boolean readBoolean() {
    Scanner reader = new Scanner(System.in);
    String boolRead;
    while (true) {
      boolRead = reader.nextLine();
      if (boolRead.matches("true") || boolRead.matches("1") || boolRead.matches("yes")|| boolRead.matches("y")) {
        return true;
      } else if (boolRead.matches("false") || boolRead.matches("0") || boolRead.matches("no") || boolRead.matches("n" )){
        return false;
      } else {
        System.out.println("Incorrect value specified, please, try again.");
      }
    }
  }
}
