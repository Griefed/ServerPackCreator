package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.IncorrectLanguageException;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class FilesSetup {
    private static final Logger appLogger = LogManager.getLogger(FilesSetup.class);
    /** Calls individual methods which check for existence of default files. If any of these methods return true, serverpackcreator will exit, giving the user the chance to customize it before the program runs in production.
     */
    void filesSetup() {
        appLogger.info(LocalizationManager.getLocalizedString("filessetup.log.info.filessetup.enter"));
        try {
            Files.createDirectories(Paths.get("./server_files"));
        } catch (IOException ex) {
            appLogger.error(LocalizationManager.getLocalizedString("filessetup.log.error.filessetup"), ex);
        }
        boolean doesConfigExist         = checkForConfig();
        boolean doesFabricLinuxExist    = checkForFabricLinux();
        boolean doesFabricWindowsExist  = checkForFabricWindows();
        boolean doesForgeLinuxExist     = checkForForgeLinux();
        boolean doesForgeWindowsExist   = checkForForgeWindows();
        boolean doesPropertiesExist     = checkForProperties();
        boolean doesIconExist           = checkForIcon();

        if (doesConfigExist            ||
                doesFabricLinuxExist   ||
                doesFabricWindowsExist ||
                doesForgeLinuxExist    ||
                doesForgeWindowsExist  ||
                doesPropertiesExist    ||
                doesIconExist) {

            appLogger.warn(LocalizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning0"));
            appLogger.warn(LocalizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning1"));
            appLogger.warn(LocalizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning2"));
            appLogger.warn(LocalizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning3"));
            appLogger.warn(LocalizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning0"));

        } else {
            appLogger.info(LocalizationManager.getLocalizedString("filessetup.log.info.filessetup.finish"));
        }
    }
    /** Check for old config file, if found rename to new name. If neither old nor new config file can be found, a new config file is generated.
     * @return Boolean. Returns true if new config file was generated.
     */
    boolean checkForConfig() {
        boolean firstRun = false;
        if (Reference.oldConfigFile.exists()) {
            try {
                Files.copy(Reference.oldConfigFile.getAbsoluteFile().toPath(), Reference.configFile.getAbsoluteFile().toPath());

                boolean isOldConfigDeleted = Reference.oldConfigFile.delete();
                if (isOldConfigDeleted) {
                    appLogger.info(LocalizationManager.getLocalizedString("filessetup.log.info.chechforconfig.old"));
                }

            } catch (IOException ex) {
                appLogger.error(LocalizationManager.getLocalizedString("filessetup.log.error.checkforconfig.old"), ex);
            }
        } else if (!Reference.configFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream(String.format("/de/griefed/resources/%s", Reference.configFile.getName())));

                if (link != null) {
                    Files.copy(link, Reference.configFile.getAbsoluteFile().toPath());
                    link.close();
                }

                appLogger.info(LocalizationManager.getLocalizedString("filessetup.log.info.checkforconfig.config"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(LocalizationManager.getLocalizedString("filessetup.log.error.checkforconfig.config"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /** Checks for existence of Fabric start script for Linux. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated.
     */
    boolean checkForFabricLinux() {
        boolean firstRun = false;
        if (!Reference.fabricLinuxFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", Reference.fabricLinuxFile.getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", Reference.fabricLinuxFile)));
                    link.close();
                }

                appLogger.info(LocalizationManager.getLocalizedString("filessetup.log.info.checkforfabriclinux"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(LocalizationManager.getLocalizedString("filessetup.log.error.checkforfabriclinux"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /** Checks for existence of Fabric start script for Windows. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated.
     */
    boolean checkForFabricWindows() {
        boolean firstRun = false;
        if (!Reference.fabricWindowsFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", Reference.fabricWindowsFile.getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", Reference.fabricWindowsFile)));
                    link.close();
                }

                appLogger.info(LocalizationManager.getLocalizedString("filessetup.log.info.checkforfabricwindows"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(LocalizationManager.getLocalizedString("filessetup.log.error.checkforfabricwindows"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /** Checks for existence of Forge start script for Linux. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated.
     */
    boolean checkForForgeLinux() {
        boolean firstRun = false;
        if (!Reference.forgeLinuxFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", Reference.forgeLinuxFile.getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", Reference.forgeLinuxFile)));
                    link.close();
                }

                appLogger.info(LocalizationManager.getLocalizedString("filessetup.log.info.checkforforgelinux"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(LocalizationManager.getLocalizedString("filessetup.log.error.checkforforgelinux"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /** Checks for existence of Forge start script for Windows. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated.
     */
    boolean checkForForgeWindows() {
        boolean firstRun = false;
        if (!Reference.forgeWindowsFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", Reference.forgeWindowsFile.getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", Reference.forgeWindowsFile)));
                    link.close();
                }

                appLogger.info(LocalizationManager.getLocalizedString("filessetup.log.info.checkforforgewindows"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(LocalizationManager.getLocalizedString("filessetup.log.error.checkforforgewindows"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /** Checks for existence of server.properties file. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated.
     */
    boolean checkForProperties() {
        boolean firstRun = false;
        if (!Reference.propertiesFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", Reference.propertiesFile.getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", Reference.propertiesFile)));
                    link.close();
                }

                appLogger.info(LocalizationManager.getLocalizedString("filessetup.log.info.checkforproperties"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(LocalizationManager.getLocalizedString("filessetup.log.error.checkforproperties"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /** Checks for existence of server-icon.png file. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated.
     */
    boolean checkForIcon() {
        boolean firstRun = false;
        if (!Reference.iconFile.exists()) {
            try {
                InputStream link = (CopyFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", Reference.iconFile.getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", Reference.iconFile)));
                    link.close();
                }

                appLogger.info(LocalizationManager.getLocalizedString("filessetup.log.info.checkforicon"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(LocalizationManager.getLocalizedString("filessetup.log.error.checkforicon"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /**
     * Check for existence of a lang.properties-file and if found assign language specified therein. If assigning the specified language fails because it is not supported, default to en_US.
     * This method should not contain the Localizationmanager, as the initialization of said manager is called from here. Therefore, localized string are not yet available.
     */
    public void checkLocaleFile() {
        if (Reference.langPropertiesFile.exists()) {
            try {
                LocalizationManager.init(Reference.langPropertiesFile);
            } catch (IncorrectLanguageException e) {

                appLogger.error("Incorrect language specified, falling back to English (United States)...");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(Reference.langPropertiesFile))) {

                    if (!Reference.langPropertiesFile.exists()) {
                        boolean langCreated = Reference.langPropertiesFile.createNewFile();
                        if (langCreated) {
                            appLogger.debug("Lang properties file created successfully.");
                        } else {
                            appLogger.debug("Lang properties file not created.");
                        }
                    }

                    writer.write(String.format("# Supported languages: %s%n", Arrays.toString(LocalizationManager.getSupportedLanguages())));
                    writer.write(String.format("lang=en_us%n"));

                } catch (IOException ex) {
                    appLogger.error("Error: There was an error writing the localization properties file.", ex);
                }
                LocalizationManager.init();
            }
        } else {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(Reference.langPropertiesFile))) {

                if (!Reference.langPropertiesFile.exists()) {
                    boolean langCreated = Reference.langPropertiesFile.createNewFile();
                    if (langCreated) {
                        appLogger.debug("Lang properties file created successfully.");
                    } else {
                        appLogger.debug("Lang properties file not created.");
                    }
                }

                writer.write(String.format("# Supported languages: %s%n", Arrays.toString(LocalizationManager.getSupportedLanguages())));
                writer.write(String.format("lang=en_us%n"));

            } catch (IOException ex) {
                appLogger.error("Error: There was an error writing the localization properties file.", ex);
            }
            LocalizationManager.init();
        }
    }

    /**
     * Writes the specified locale from -lang your_locale to a lang.properties file to ensure every subsequent start of ServerPackCreator is executed using said locale.
     * @param locale The locale the user specified when they ran ServerPackCreator with -lang -your_locale.
     * This method should not contain the Localizationmanager, as the initialization of said manager is called from here. Therefore, localized string are not yet available.
     */
    public void writeLocaleToFile(String locale) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Reference.langPropertiesFile))) {

            if (!Reference.langPropertiesFile.exists()) {
                boolean langCreated = Reference.langPropertiesFile.createNewFile();
                if (langCreated) {
                    appLogger.debug("Lang properties file created successfully.");
                } else {
                    appLogger.debug("Lang properties file not created.");
                }
            }

            writer.write(String.format("# Supported languages: %s%n", Arrays.toString(LocalizationManager.getSupportedLanguages())));
            writer.write(String.format("lang=%s%n", locale));

        } catch (IOException ex) {
            appLogger.error("Error: There was an error writing the localization properties file.", ex);
        }
    }

    /** Writes a new configuration file with the parameters passed to it.
     * @param modpackDir String. The path to the modpack.
     * @param clientMods List, String. List of clientside-only mods.
     * @param copyDirs List, String. List of directories to include in server pack.
     * @param includeServer Boolean. Whether to include a modloader server installation.
     * @param javaPath String. Path to the java executable.
     * @param minecraftVersion String. Minecraft version used by the modpack and server pack.
     * @param modLoader String. Modloader used by the modpack and server pack. Ether Forge or Fabric.
     * @param modLoaderVersion String. Modloader version used by the modpack and server pack.
     * @param includeIcon Boolean. Whether to include a server-icon in the server pack.
     * @param includeProperties Boolean. Whether to include a properties file in the server pack.
     * @param includeScripts Boolean. Whether to include start scripts in the server pack.
     * @param includeZip Boolean. Whether to create a ZIP-archive of the server pack, excluding Mojang's Minecraft server jar.
     * @return Boolean. Returns true if the configuration file has been successfully written and old ones replaced.
     */
    boolean writeConfigToFile(String modpackDir,
                                     String clientMods,
                                     String copyDirs,
                                     boolean includeServer,
                                     String javaPath,
                                     String minecraftVersion,
                                     String modLoader,
                                     String modLoaderVersion,
                                     boolean includeIcon,
                                     boolean includeProperties,
                                     boolean includeScripts,
                                     boolean includeZip ) {
        boolean configWritten = false;
        String configString = String.format(
                        "# Path to your modpack. Can be either relative or absolute.\n" +
                        "# Example: \"./Some Modpack\" or \"C:\\Minecraft\\Some Modpack\"\n" +
                        "# Can also be a combination of CurseForge projectID and fileID. Example for Survive Create Prosper 4 4.6.7: \"390331,3215793\"\n" +
                        "modpackDir = \"%s\"\n" +
                        "\n" +
                        "# List of client-only mods to delete from serverpack.\n" +
                        "# No need to include version specifics. Must be the filenames of the mods, not their project names on CurseForge!\n" +
                        "# Example: [AmbientSounds,ClientTweaks,PackMenu,BetterAdvancement,jeiintegration]\n" +
                        "clientMods = [%s]\n" +
                        "\n" +
                        "# Name of directories to include in serverpack.\n" +
                        "# When specifying \"saves/world_name\", \"world_name\" will be copied to the base directory of the serverpack\n" +
                        "# for immediate use with the server. Automatically set when projectID,fileID for modpackDir has been specified.\n" +
                        "# Example: [config,mods,scripts]\n" +
                        "copyDirs = [%s]\n" +
                        "\n" +
                        "# Whether to install a Forge/Fabric server for the serverpack. Must be true or false.\n" +
                        "# Default value is true.\n" +
                        "includeServerInstallation = %b\n" +
                        "\n" +
                        "# Path to the Java executable. On Linux systems it would be something like \"/usr/bin/java\".\n" +
                        "# Only needed if includeServerInstallation is true.\n" +
                        "javaPath = \"%s\"\n" +
                        "\n" +
                        "# Which Minecraft version to use. Example: \"1.16.5\".\n" +
                        "# Automatically set when projectID,fileID for modpackDir has been specified.\n" +
                        "# Only needed if includeServerInstallation is true.\n" +
                        "minecraftVersion = \"%s\"\n" +
                        "\n" +
                        "# Which modloader to install. Must be either \"Forge\" or \"Fabric\".\n" +
                        "# Automatically set when projectID,fileID for modpackDir has been specified.\n" +
                        "# Only needed if includeServerInstallation is true.\n" +
                        "modLoader = \"%s\"\n" +
                        "\n" +
                        "# The version of the modloader you want to install. Example for Fabric=\"0.7.3\", example for Forge=\"36.0.15\".\n" +
                        "# Automatically set when projectID,fileID for modpackDir has been specified.\n" +
                        "# Only needed if includeServerInstallation is true.\n" +
                        "modLoaderVersion = \"%s\"\n" +
                        "\n" +
                        "# Include a server-icon.png in your serverpack. Must be true or false.\n" +
                        "# Customize server-icon.png in ./server_files.\n" +
                        "# Dimensions must be 64x64!\n" +
                        "# Default value is true.\n" +
                        "includeServerIcon = %b\n" +
                        "\n" +
                        "# Include a server.properties in your serverpack. Must be true or false.\n" +
                        "# Customize server.properties in ./server_files.\n" +
                        "# If no server.properties is provided but is set to true, a default one will be provided.\n" +
                        "# Default value is true.\n" +
                        "includeServerProperties = %b\n" +
                        "\n" +
                        "# Include start scripts for windows and linux systems. Must be true or false.\n" +
                        "# Customize files beginning with \"start-\" in ./server_files.\n" +
                        "# Default value is true.\n" +
                        "includeStartScripts = %b\n" +
                        "\n" +
                        "# Create zip-archive of serverpack. Must be true or false.\n" +
                        "# Default value is true.\n" +
                        "includeZipCreation = %b\n",
                modpackDir,
                clientMods,
                copyDirs,
                includeServer,
                javaPath,
                minecraftVersion,
                modLoader,
                modLoaderVersion,
                includeIcon,
                includeProperties,
                includeScripts,
                includeZip
        );
        if (Reference.configFile.exists()) {
            boolean delConf = Reference.configFile.delete();
            if (delConf) {
                appLogger.info(LocalizationManager.getLocalizedString("filessetup.log.info.writeconfigtofile.config")); }
            else {
                appLogger.error(LocalizationManager.getLocalizedString("filessetup.log.error.writeconfigtofile.config")); }
        }
        if (Reference.oldConfigFile.exists()) {
            boolean delOldConf = Reference.oldConfigFile.delete();
            if (delOldConf) {
                appLogger.info(LocalizationManager.getLocalizedString("filessetup.log.info.writeconfigtofile.old")); }
            else {
                appLogger.error(LocalizationManager.getLocalizedString("filessetup.log.error.writeconfigtofile.old")); }
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(Reference.configFile));
            writer.write(configString);
            writer.close();
            configWritten = true;
        } catch (IOException ex) {
            appLogger.error(LocalizationManager.getLocalizedString("filessetup.log.error.writeconfigtofile"), ex);
        }
        return configWritten;
    }
}