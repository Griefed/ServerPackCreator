/* Copyright (C) 2021  Griefed
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
package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * <strong>Table of methods</strong>
 * <p>
 * 1. {@link #DefaultFiles(LocalizationManager)}<br>
 * 2. {@link #getConfigFile()}<br>
 * 3. {@link #getOldConfigFile()}<br>
 * 4. {@link #getPropertiesFile()}<br>
 * 5. {@link #getIconFile()}<br>
 * 6. {@link #getForgeWindowsFile()}<br>
 * 7. {@link #getForgeLinuxFile()}<br>
 * 8. {@link #getFabricWindowsFile()}<br>
 * 9. {@link #getFabricLinuxFile()}<br>
 * 10.{@link #filesSetup()}<br>
 * 11.{@link #checkForConfig()}<br>
 * 12.{@link #checkForFabricLinux()}<br>
 * 13.{@link #checkForFabricWindows()}<br>
 * 14.{@link #checkForForgeLinux()}<br>
 * 15.{@link #checkForForgeWindows()}<br>
 * 16.{@link #checkForProperties()}<br>
 * 17.{@link #checkForIcon()}
 * <p>
 * Requires instances of {@link LocalizationManager} for use of localization, but creates one if injected one is null.
 * <p>
 * Ensures all files needed by ServerPackCreator are available. If any one is missing, a new one is generated from the
 * template. Among the default files are:<p>
 * <strong>serverpackcreator.conf</strong><br>
 * <strong>server.properties</strong><br>
 * <strong>server-icon.png</strong><br>
 * <strong>start-forge.bar</strong><br>
 * <strong>start-forge.sh</strong><br>
 * <strong>start-fabric.bat</strong><br>
 * <strong>start-fabric.sh</strong>
 * <p>
 * Should an old configuration file, <em>creator.conf</em>, be detected, it is renamed to <em>serverpackcreator.conf</em>
 * to ensure a configuration file is present at all times.
 */
public class DefaultFiles {

    private static final Logger LOG = LogManager.getLogger(DefaultFiles.class);

    private final LocalizationManager LOCALIZATIONMANAGER;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection. Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     */
    public DefaultFiles(LocalizationManager injectedLocalizationManager) {
        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager();
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }
    }

    private final File FILE_CONFIG = new File("serverpackcreator.conf");
    private final File FILE_CONFIG_OLD = new File("creator.conf");
    private final File FILE_PROPERTIES = new File("server.properties");
    private final File FILE_ICON = new File("server-icon.png");
    private final File FILE_FORGE_WINDOWS = new File("start-forge.bat");
    private final File FILE_FORGE_LINUX = new File("start-forge.sh");
    private final File FILE_FABRIC_WINDOWS = new File("start-fabric.bat");
    private final File FILE_FABRIC_LINUX = new File("start-fabric.sh");

    /**
     * Getter for serverpackcreator.conf.
     * @return Returns the serverpackcreator.conf-file for use in {@link #checkForConfig()}
     */
    public File getConfigFile() {
        return FILE_CONFIG;
    }

    /**
     * Getter for creator.conf.
     * @return Returns the creator.conf-file for use in {@link #checkForConfig()}.
     */
    public File getOldConfigFile() {
        return FILE_CONFIG_OLD;
    }

    /**
     * Getter for server.properties.
     * @return Returns the server.properties-file for use in {@link #checkForProperties()}
     */
    public File getPropertiesFile() {
        return FILE_PROPERTIES;
    }

    /**
     * Getter for server-icon.png
     * @return Returns the server-icon.png-file for use in {@link #checkForIcon()}
     */
    public File getIconFile() {
        return FILE_ICON;
    }

    /**
     * Getter for start-forge.bat.
     * @return Returns the start-forge.bat-file for use in {@link #checkForForgeWindows()}
     */
    public File getForgeWindowsFile() {
        return FILE_FORGE_WINDOWS;
    }

    /**
     * Getter for start-forge.sh.
     * @return Returns the start-forge.sh-file for use in {@link #checkForForgeLinux()}
     */
    public File getForgeLinuxFile() {
        return FILE_FORGE_LINUX;
    }

    /**
     * Getter for start-fabric.bat.
     * @return Returns the start-fabric.bat-file for use in {@link #checkForFabricWindows()}
     */
    public File getFabricWindowsFile() {
        return FILE_FABRIC_WINDOWS;
    }

    /**
     * Getter for start-fabric.sh.
     * @return Returns the start-fabric.sh-file for use in {@link #checkForFabricLinux()}
     */
    public File getFabricLinuxFile() {
        return FILE_FABRIC_LINUX;
    }

    /** Calls individual methods which check for existence of default files. Only this method should be called to check
     * for existence of all default files.<p>
     * If any file was newly generated from it's template, a warning is printed informing the user about said newly
     * generated file. If every file was present and none was generated, "Setup completed." is printed to the console
     * and log.
     */
    void filesSetup() {
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.info.filessetup.enter"));

        try { Files.createDirectories(Paths.get("./server_files")); }
        catch (IOException ex) { LOG.error(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.error.filessetup"), ex); }

        boolean doesConfigExist         = checkForConfig();
        boolean doesFabricLinuxExist    = checkForFabricLinux();
        boolean doesFabricWindowsExist  = checkForFabricWindows();
        boolean doesForgeLinuxExist     = checkForForgeLinux();
        boolean doesForgeWindowsExist   = checkForForgeWindows();
        boolean doesPropertiesExist     = checkForProperties();
        boolean doesIconExist           = checkForIcon();

        // Inform user about customization of files if any of them were generated from the template.
        if (doesConfigExist            ||
                doesFabricLinuxExist   ||
                doesFabricWindowsExist ||
                doesForgeLinuxExist    ||
                doesForgeWindowsExist  ||
                doesPropertiesExist    ||
                doesIconExist) {

            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.warn.filessetup.warning0"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.warn.filessetup.warning1"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.warn.filessetup.warning2"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.warn.filessetup.warning3"));
            LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.warn.filessetup.warning0"));

        } else {
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.info.filessetup.finish"));
        }
    }
    /** Check for old config file, if found rename to new name. If neither old nor new config file can be found, a new
     * config file is generated.
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForConfig() {
        boolean firstRun = false;
        if (getOldConfigFile().exists()) {
            try {
                Files.copy(getOldConfigFile().getAbsoluteFile().toPath(), getConfigFile().getAbsoluteFile().toPath());

                boolean isOldConfigDeleted = getOldConfigFile().delete();
                if (isOldConfigDeleted) {
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.info.checkforconfig.old"));
                }

            } catch (IOException ex) {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.error.checkforconfig.old"), ex);
            }
        } else if (!getConfigFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/%s", getConfigFile().getName())));

                if (link != null) {
                    Files.copy(link, getConfigFile().getAbsoluteFile().toPath());
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.info.checkforconfig.config"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.error.checkforconfig.config"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /** Checks for existence of Fabric start script for Linux. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForFabricLinux() {
        boolean firstRun = false;
        if (!getFabricLinuxFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getFabricLinuxFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getFabricLinuxFile())));
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.info.checkforfabriclinux"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.error.checkforfabriclinux"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /** Checks for existence of Fabric start script for Windows. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForFabricWindows() {
        boolean firstRun = false;
        if (!getFabricWindowsFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getFabricWindowsFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getFabricWindowsFile())));
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.info.checkforfabricwindows"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.error.checkforfabricwindows"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /** Checks for existence of Forge start script for Linux. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForForgeLinux() {
        boolean firstRun = false;
        if (!getForgeLinuxFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getForgeLinuxFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getForgeLinuxFile())));
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.info.checkforforgelinux"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.error.checkforforgelinux"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /** Checks for existence of Forge start script for Windows. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForForgeWindows() {
        boolean firstRun = false;
        if (!getForgeWindowsFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getForgeWindowsFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getForgeWindowsFile())));
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.info.checkforforgewindows"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.error.checkforforgewindows"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /** Checks for existence of server.properties file. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForProperties() {
        boolean firstRun = false;
        if (!getPropertiesFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getPropertiesFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getPropertiesFile())));
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.info.checkforproperties"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.error.checkforproperties"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }

    /** Checks for existence of server-icon.png file. If it is not found, it is generated.
     * @return Boolean. Returns true if the file was generated, so {@link #filesSetup()} can inform the user about
     * said newly generated file.
     */
    boolean checkForIcon() {
        boolean firstRun = false;
        if (!getIconFile().exists()) {
            try {
                InputStream link = (DefaultFiles.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getIconFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getIconFile())));
                    link.close();
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.info.checkforicon"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("filessetup.log.error.checkforicon"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }
}