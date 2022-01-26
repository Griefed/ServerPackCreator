/* Copyright (C) 2022  Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.utilities;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Utility-class revolving around configuration utilities.
 * @author Griefed
 */
@Component
public class ConfigUtilities {

    private static final Logger LOG = LogManager.getLogger(ConfigUtilities.class);

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final BooleanUtilities BOOLEANUTILITIES;
    private final ListUtilities LISTUTILITIES;
    private final StringUtilities STRINGUTILITIES;
    private final ApplicationProperties APPLICATIONPROPERTIES;

    @Autowired
    public ConfigUtilities(LocalizationManager injectedLocalizationManager, BooleanUtilities injectedBooleanUtilities,
                           ListUtilities injectedListUtilities, ApplicationProperties injectedApplicationProperties,
                           StringUtilities injectedStringUtilities) {

        if (injectedApplicationProperties == null) {
            this.APPLICATIONPROPERTIES = new ApplicationProperties();
        } else {
            this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        }

        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        if (injectedBooleanUtilities == null) {
            this.BOOLEANUTILITIES = new BooleanUtilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        } else {
            this.BOOLEANUTILITIES = injectedBooleanUtilities;
        }

        if (injectedListUtilities == null) {
            this.LISTUTILITIES = new ListUtilities();
        } else {
            this.LISTUTILITIES = injectedListUtilities;
        }

        if (injectedStringUtilities == null) {
            this.STRINGUTILITIES = new StringUtilities();
        } else {
            this.STRINGUTILITIES = injectedStringUtilities;
        }
    }

    /**
     * Ensures the modloader is normalized to first letter upper case and rest lower case. Basically allows the user to
     * input Forge or Fabric in any combination of upper- and lowercase and ServerPackCreator will still be able to
     * work with the users input.
     * @author Griefed
     * @param modloader String. The String to check for case-insensitive cases of either Forge or Fabric.
     * @return String. Returns a normalized String of the specified modloader.
     */
    public String getModLoaderCase(String modloader) {

        if (modloader.equalsIgnoreCase("Forge")) {

            return "Forge";

        } else if (modloader.equalsIgnoreCase("Fabric")) {

            return "Fabric";

        } else if (modloader.toLowerCase().contains("forge")) {

            return "Forge";

        } else if (modloader.toLowerCase().contains("fabric")) {

            return "Fabric";

        } else {

            return "Forge";
        }
    }

    /**
     * Convenience method to write a new configuration file with the {@link ConfigurationModel} passed to it. If the given file already exists, it is replaced.
     * @author Griefed
     * @param configurationModel Instance of {@link ConfigurationModel} to write to a file.
     * @param fileName The file to write to.
     * @return Boolean. Returns true if the configuration file has been successfully written and old ones replaced.
     */
    public boolean writeConfigToFile(ConfigurationModel configurationModel, File fileName) {

        return writeConfigToFile(
                configurationModel.getModpackDir(),
                configurationModel.getClientMods(),
                configurationModel.getCopyDirs(),
                configurationModel.getServerIconPath(),
                configurationModel.getServerPropertiesPath(),
                configurationModel.getIncludeServerInstallation(),
                configurationModel.getJavaPath(),
                configurationModel.getMinecraftVersion(),
                configurationModel.getModLoader(),
                configurationModel.getModLoaderVersion(),
                configurationModel.getIncludeServerIcon(),
                configurationModel.getIncludeServerProperties(),
                configurationModel.getIncludeZipCreation(),
                configurationModel.getJavaArgs(),
                configurationModel.getServerPackSuffix(),
                fileName
        );
    }

    /**
     * Writes a new configuration file with the parameters passed to it. If the given file already exists, it is replaced.
     * @author whitebear60
     * @author Griefed
     * @param modpackDir String. The path to the modpack.
     * @param clientMods List, String. List of clientside-only mods.
     * @param copyDirs List, String. List of directories to include in server pack.
     * @param serverIconPath String. The path to the custom server-icon.png to include in the server pack.
     * @param serverPropertiesPath String. The path to the custom server.properties to include in the server pack.
     * @param includeServer Boolean. Whether the modloader server software should be installed.
     * @param javaPath String. Path to the java executable/binary.
     * @param minecraftVersion String. Minecraft version used by the modpack and server pack.
     * @param modLoader String. Modloader used by the modpack and server pack. Ether Forge or Fabric.
     * @param modLoaderVersion String. Modloader version used by the modpack and server pack.
     * @param includeIcon Boolean. Whether to include a server-icon in the server pack.
     * @param includeProperties Boolean. Whether to include a properties file in the server pack.
     * @param includeZip Boolean. Whether to create a ZIP-archive of the server pack, excluding Mojang's Minecraft server JAR.
     * @param javaArgs String. Java arguments to write the start-scripts with.
     * @param serverPackSuffix String. Suffix to append to the server pack to be generated.
     * @param fileName The name under which to write the new configuration file.
     * @return Boolean. Returns true if the configuration file has been successfully written and old ones replaced.
     */
    public boolean writeConfigToFile(String modpackDir,
                                     List<String> clientMods,
                                     List<String> copyDirs,
                                     String serverIconPath,
                                     String serverPropertiesPath,
                                     boolean includeServer,
                                     String javaPath,
                                     String minecraftVersion,
                                     String modLoader,
                                     String modLoaderVersion,
                                     boolean includeIcon,
                                     boolean includeProperties,
                                     boolean includeZip,
                                     String javaArgs,
                                     String serverPackSuffix,
                                     File fileName) {

        boolean configWritten = false;

        if (javaArgs.equals("")) {
            javaArgs = "empty";
        }

        //Griefed: What the fuck. This reads like someone having a stroke. What have I created here?
        String configString = String.format(
                "%s\nmodpackDir = \"%s\"\n\n" +
                        "%s\nclientMods = %s\n\n" +
                        "%s\ncopyDirs = %s\n\n" +
                        "%s\nserverIconPath = \"%s\"\n\n" +
                        "%s\nserverPropertiesPath = \"%s\"\n\n" +
                        "%s\nincludeServerInstallation = %b\n\n" +
                        "%s\njavaPath = \"%s\"\n\n" +
                        "%s\nminecraftVersion = \"%s\"\n\n" +
                        "%s\nmodLoader = \"%s\"\n\n" +
                        "%s\nmodLoaderVersion = \"%s\"\n\n" +
                        "%s\nincludeServerIcon = %b\n\n" +
                        "%s\nincludeServerProperties = %b\n\n" +
                        "%s\nincludeZipCreation = %b\n\n" +
                        "%s\njavaArgs = \"%s\"\n\n" +
                        "%s\nserverPackSuffix = \"%s\"",
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.modpackdir"), modpackDir.replace("\\","/"),
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.clientmods"), LISTUTILITIES.encapsulateListElements(LISTUTILITIES.cleanList(clientMods)),
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.copydirs"), LISTUTILITIES.encapsulateListElements(LISTUTILITIES.cleanList(copyDirs)),
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.custom.icon"), serverIconPath,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.custom.properties"), serverPropertiesPath,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.includeserverinstallation"), includeServer,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.javapath"), javaPath.replace("\\","/"),
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.minecraftversion"), minecraftVersion,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.modloader"), modLoader,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.modloaderversion"), modLoaderVersion,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.includeservericon"), includeIcon,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.includeserverproperties"), includeProperties,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.includezipcreation"), includeZip,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.javaargs"), javaArgs,
                LOCALIZATIONMANAGER.getLocalizedString("configuration.writeconfigtofile.serverpacksuffix"), serverPackSuffix
        );

        if (fileName.exists()) {
            FileUtils.deleteQuietly(fileName);
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(configString);
            writer.close();
            configWritten = true;
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.info.writeconfigtofile.confignew"));
        } catch (IOException ex) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("defaultfiles.log.error.writeconfigtofile"), ex);
        }

        return configWritten;
    }

    /**
     * Creates a list of all configurations as they appear in the serverpackcreator.conf to pass it to any addon that may run.
     * Values included in this list are:<br>
     * 1. modpackDir<br>
     * 2. clientMods<br>
     * 3. copyDirs<br>
     * 4. javaPath<br>
     * 5. minecraftVersion<br>
     * 6. modLoader<br>
     * 7. modLoaderVersion<br>
     * 8. includeServerInstallation<br>
     * 9. includeServerIcon<br>
     * 10.includeServerProperties<br>
     * 11.includeStartScripts<br>
     * 12.includeZipCreation
     * @author Griefed
     * @param configurationModel An instance of {@link ConfigurationModel} which contains the configuration of the modpack.
     * @return String List. A list of all configurations as strings.
     */
    public List<String> getConfigurationAsList(ConfigurationModel configurationModel) {

        List<String> configurationAsList = new ArrayList<>(100);

        configurationAsList.add(configurationModel.getModpackDir());
        configurationAsList.add(STRINGUTILITIES.buildString(configurationModel.getClientMods().toString()));
        configurationAsList.add(STRINGUTILITIES.buildString(configurationModel.getCopyDirs().toString()));
        configurationAsList.add(configurationModel.getJavaPath());
        configurationAsList.add(configurationModel.getMinecraftVersion());
        configurationAsList.add(configurationModel.getModLoader());
        configurationAsList.add(configurationModel.getModLoaderVersion());
        configurationAsList.add(String.valueOf(configurationModel.getIncludeServerInstallation()));
        configurationAsList.add(String.valueOf(configurationModel.getIncludeServerIcon()));
        configurationAsList.add(String.valueOf(configurationModel.getIncludeServerProperties()));
        configurationAsList.add(String.valueOf(configurationModel.getIncludeZipCreation()));

        LOG.debug(String.format("Configuration to pass to addons is: %s", configurationAsList));

        return configurationAsList;
    }


    /**
     * Convenience method which passes the important fields from an instance of {@link ConfigurationModel} to {@link #printConfigurationModel(String, List, List, boolean, String, String, String, String, boolean, boolean, boolean, String, String, String, String)}
     * @author Griefed
     * @param configurationModel Instance of {@link ConfigurationModel} to print to console and logs.
     */
    public void printConfigurationModel(ConfigurationModel configurationModel) {
        printConfigurationModel(
                configurationModel.getModpackDir(),
                configurationModel.getClientMods(),
                configurationModel.getCopyDirs(),
                configurationModel.getIncludeServerInstallation(),
                configurationModel.getJavaPath(),
                configurationModel.getMinecraftVersion(),
                configurationModel.getModLoader(),
                configurationModel.getModLoaderVersion(),
                configurationModel.getIncludeServerIcon(),
                configurationModel.getIncludeServerProperties(),
                configurationModel.getIncludeZipCreation(),
                configurationModel.getJavaArgs(),
                configurationModel.getServerPackSuffix(),
                configurationModel.getServerIconPath(),
                configurationModel.getServerPropertiesPath()
        );
    }

    /**
     * Prints all passed fields to the console and serverpackcreator.log. Used to show the user the configuration before
     * ServerPackCreator starts the generation of the server pack or, if checks failed, to show the user their last
     * configuration, so they can more easily identify problems with said configuration.<br>
     * Should a user report an issue on GitHub and include their logs (which I hope they do....), this would also
     * help me help them. Logging is good. People should use more logging.
     * @author Griefed
     * @param modpackDirectory String. The used modpackDir field either from a configuration file or from configuration setup.
     * @param clientsideMods String List. List of clientside-only mods to exclude from the server pack...
     * @param copyDirectories String List. List of directories in the modpack which are to be included in the server pack.
     * @param installServer Boolean. Whether to install the modloader server in the server pack.
     * @param javaInstallPath String. Path to the Java executable/binary needed for installing the modloader server in the server pack.
     * @param minecraftVer String. The Minecraft version the modpack uses.
     * @param modloader String. The modloader the modpack uses.
     * @param modloaderVersion String. The version of the modloader the modpack uses.
     * @param includeIcon Boolean. Whether to include the server-icon.png in the server pack.
     * @param includeProperties Boolean. Whether to include the server.properties in the server pack.
     * @param includeZip Boolean. Whether to create a zip-archive of the server pack, excluding the Minecraft server JAR according to Mojang's TOS and EULA.
     * @param javaArgs String. Java arguments to write the start-scripts with.
     * @param serverPackSuffix String. Suffix to append to name of the server pack to be generated.
     * @param serverIconPath String. The path to the custom server-icon.png to be used in the server pack.
     * @param serverPropertiesPath String. The path to the custom server.properties to be used in the server pack.
     */
    public void printConfigurationModel(String modpackDirectory,
                                 List<String> clientsideMods,
                                 List<String> copyDirectories,
                                 boolean installServer,
                                 String javaInstallPath,
                                 String minecraftVer,
                                 String modloader,
                                 String modloaderVersion,
                                 boolean includeIcon,
                                 boolean includeProperties,
                                 boolean includeZip,
                                 String javaArgs,
                                 String serverPackSuffix,
                                 String serverIconPath,
                                 String serverPropertiesPath) {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.start"));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.modpackdir"), modpackDirectory));

        if (clientsideMods.isEmpty()) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.printconfig.noclientmods"));
        } else {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.clientmods"));
            for (String mod : clientsideMods) {
                LOG.info(String.format("    %s", mod));
            }

        }

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.copydirs"));

        if (copyDirectories != null) {

            for (String directory : copyDirectories) {
                LOG.info(String.format("    %s", directory));
            }

        } else {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.error.printconfig.copydirs"));
        }

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.server"), installServer));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.javapath"), javaInstallPath));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.minecraftversion"), minecraftVer));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.modloader"), modloader));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.modloaderversion"), modloaderVersion));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.icon"), includeIcon));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.properties"), includeProperties));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.zip"), includeZip));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.javaargs"), javaArgs));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("configuration.log.info.printconfig.serverpacksuffix"), serverPackSuffix));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("utilities.log.info.config.print.servericon"),serverIconPath));
        LOG.info(String.format(LOCALIZATIONMANAGER.getLocalizedString("utilities.log.info.config.print.serverproperties"),serverPropertiesPath));
    }
}
