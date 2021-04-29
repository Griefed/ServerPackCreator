package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FilesSetup {

    private static final Logger appLogger = LogManager.getLogger(FilesSetup.class);

    private final File configFile = new File("serverpackcreator.conf");
    private final File oldConfigFile     = new File("creator.conf");
    private final File propertiesFile    = new File("server.properties");
    private final File iconFile          = new File("server-icon.png");
    private final File forgeWindowsFile  = new File("start-forge.bat");
    private final File forgeLinuxFile    = new File("start-forge.sh");
    private final File fabricWindowsFile = new File("start-fabric.bat");
    private final File fabricLinuxFile   = new File("start-fabric.sh");

    private LocalizationManager localizationManager;

    public FilesSetup(LocalizationManager injectedLocalizationManager) {
        if (injectedLocalizationManager == null) {
            this.localizationManager = new LocalizationManager();
        } else {
            this.localizationManager = injectedLocalizationManager;
        }
    }

    public File getConfigFile() {
        return configFile;
    }

    public File getOldConfigFile() {
        return oldConfigFile;
    }

    public File getPropertiesFile() {
        return propertiesFile;
    }

    public File getIconFile() {
        return iconFile;
    }

    public File getForgeWindowsFile() {
        return forgeWindowsFile;
    }

    public File getForgeLinuxFile() {
        return forgeLinuxFile;
    }

    public File getFabricWindowsFile() {
        return fabricWindowsFile;
    }

    public File getFabricLinuxFile() {
        return fabricLinuxFile;
    }

    /** Calls individual methods which check for existence of default files. If any of these methods return true, serverpackcreator will exit, giving the user the chance to customize it before the program runs in production.
     */
    void filesSetup() {
        appLogger.info(localizationManager.getLocalizedString("filessetup.log.info.filessetup.enter"));
        try {
            Files.createDirectories(Paths.get("./server_files"));
        } catch (IOException ex) {
            appLogger.error(localizationManager.getLocalizedString("filessetup.log.error.filessetup"), ex);
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

            appLogger.warn(localizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning0"));
            appLogger.warn(localizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning1"));
            appLogger.warn(localizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning2"));
            appLogger.warn(localizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning3"));
            appLogger.warn(localizationManager.getLocalizedString("filessetup.log.warn.filessetup.warning0"));

        } else {
            appLogger.info(localizationManager.getLocalizedString("filessetup.log.info.filessetup.finish"));
        }
    }
    /** Check for old config file, if found rename to new name. If neither old nor new config file can be found, a new config file is generated.
     * @return Boolean. Returns true if new config file was generated.
     */
    boolean checkForConfig() {
        boolean firstRun = false;
        if (getOldConfigFile().exists()) {
            try {
                Files.copy(getOldConfigFile().getAbsoluteFile().toPath(), getConfigFile().getAbsoluteFile().toPath());

                boolean isOldConfigDeleted = getOldConfigFile().delete();
                if (isOldConfigDeleted) {
                    appLogger.info(localizationManager.getLocalizedString("filessetup.log.info.chechforconfig.old"));
                }

            } catch (IOException ex) {
                appLogger.error(localizationManager.getLocalizedString("filessetup.log.error.checkforconfig.old"), ex);
            }
        } else if (!getConfigFile().exists()) {
            try {
                InputStream link = (FilesSetup.class.getResourceAsStream(String.format("/de/griefed/resources/%s", getConfigFile().getName())));

                if (link != null) {
                    Files.copy(link, getConfigFile().getAbsoluteFile().toPath());
                    link.close();
                }

                appLogger.info(localizationManager.getLocalizedString("filessetup.log.info.checkforconfig.config"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(localizationManager.getLocalizedString("filessetup.log.error.checkforconfig.config"), ex);
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
        if (!getFabricLinuxFile().exists()) {
            try {
                InputStream link = (FilesSetup.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getFabricLinuxFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getFabricLinuxFile())));
                    link.close();
                }

                appLogger.info(localizationManager.getLocalizedString("filessetup.log.info.checkforfabriclinux"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(localizationManager.getLocalizedString("filessetup.log.error.checkforfabriclinux"), ex);
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
        if (!getFabricWindowsFile().exists()) {
            try {
                InputStream link = (FilesSetup.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getFabricWindowsFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getFabricWindowsFile())));
                    link.close();
                }

                appLogger.info(localizationManager.getLocalizedString("filessetup.log.info.checkforfabricwindows"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(localizationManager.getLocalizedString("filessetup.log.error.checkforfabricwindows"), ex);
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
        if (!getForgeLinuxFile().exists()) {
            try {
                InputStream link = (FilesSetup.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getForgeLinuxFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getForgeLinuxFile())));
                    link.close();
                }

                appLogger.info(localizationManager.getLocalizedString("filessetup.log.info.checkforforgelinux"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(localizationManager.getLocalizedString("filessetup.log.error.checkforforgelinux"), ex);
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
        if (!getForgeWindowsFile().exists()) {
            try {
                InputStream link = (FilesSetup.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getForgeWindowsFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getForgeWindowsFile())));
                    link.close();
                }

                appLogger.info(localizationManager.getLocalizedString("filessetup.log.info.checkforforgewindows"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(localizationManager.getLocalizedString("filessetup.log.error.checkforforgewindows"), ex);
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
        if (!getPropertiesFile().exists()) {
            try {
                InputStream link = (FilesSetup.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getPropertiesFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getPropertiesFile())));
                    link.close();
                }

                appLogger.info(localizationManager.getLocalizedString("filessetup.log.info.checkforproperties"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(localizationManager.getLocalizedString("filessetup.log.error.checkforproperties"), ex);
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
        if (!getIconFile().exists()) {
            try {
                InputStream link = (FilesSetup.class.getResourceAsStream(String.format("/de/griefed/resources/server_files/%s", getIconFile().getName())));
                if (link != null) {
                    Files.copy(link, Paths.get(String.format("./server_files/%s", getIconFile())));
                    link.close();
                }

                appLogger.info(localizationManager.getLocalizedString("filessetup.log.info.checkforicon"));
                firstRun = true;

            } catch (IOException ex) {
                if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                    appLogger.error(localizationManager.getLocalizedString("filessetup.log.error.checkforicon"), ex);
                    firstRun = true;
                }
            }
        }
        return firstRun;
    }
}