package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
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
    void setup() {
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
        appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.start"), Reference.CONFIG_GEN_ARGUMENT));
        do {
            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.modpack.enter"));
            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.modpack.example"));

            do {
                System.out.print(LocalizationManager.getLocalizedString("clisetup.log.info.modpack.cli"));
                tmpModpackDir = reader.nextLine();
            } while (!Reference.configCheck.checkModpackDir(tmpModpackDir));

            modpackDir = tmpModpackDir.replace("\\", "/");
            appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.checkreturn"), modpackDir));
            System.out.println();

            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.clientmods.enter"));
            do {
                clientMods.addAll(readStringArray());
                appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.checkreturn"), clientMods));
                appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.clientmods.checkreturninfo"));
                System.out.print(LocalizationManager.getLocalizedString("clisetup.log.info.answer"));
            } while (!readBoolean());
            appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.checkreturn"), clientMods));
            tmpClientMods = new String[clientMods.size()];
            clientMods.toArray(tmpClientMods);
            System.out.println();

            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.copydirs.enter"));
            do {
                do {
                    copyDirs.clear();
                    appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.copydirs.specify"));
                    copyDirs.addAll(readStringArray());

                } while (!Reference.configCheck.checkCopyDirs(copyDirs, modpackDir));

                appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.checkreturn"), copyDirs));
                appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.copydirs.checkreturninfo"));
                System.out.print(LocalizationManager.getLocalizedString("clisetup.log.info.answer"));
            } while (!readBoolean());
            appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.checkreturn"), copyDirs));
            tmpCopyDirs = new String[copyDirs.size()];
            copyDirs.toArray(tmpCopyDirs);
            System.out.println();

            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.server.enter"));
            System.out.print(LocalizationManager.getLocalizedString("clisetup.log.info.server.include"));
            includeServerInstallation = readBoolean();
            appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.checkreturn"), includeServerInstallation));

            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.minecraft.enter"));
            do {
                System.out.print(LocalizationManager.getLocalizedString("clisetup.log.info.minecraft.specify"));
                minecraftVersion = reader.nextLine();
            } while (!Reference.configCheck.isMinecraftVersionCorrect(minecraftVersion));
            appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.checkreturn"), minecraftVersion));
            System.out.println();

            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.modloader.enter"));
            do {
                System.out.print(LocalizationManager.getLocalizedString("clisetup.log.info.modloader.cli"));
                modLoader = reader.nextLine();
            } while (!Reference.configCheck.checkModloader(modLoader));
            modLoader = Reference.configCheck.setModloader(modLoader);
            appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.checkreturn"), modLoader));
            System.out.println();

            appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.modloaderversion.enter"), modLoader));
            do {
                System.out.print(LocalizationManager.getLocalizedString("clisetup.log.info.modloaderversion.cli"));
                modLoaderVersion = reader.nextLine();
            } while (!Reference.configCheck.checkModloaderVersion(modLoader, modLoaderVersion, minecraftVersion));
            appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.checkreturn"), modLoaderVersion));
            System.out.println();

            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.java.enter"));
            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.java.enter2"));
            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.java.example"));
            do {
                System.out.print(LocalizationManager.getLocalizedString("clisetup.log.info.java.cli"));
                String tmpJavaPath = reader.nextLine();
                javaPath = Reference.configCheck.getJavaPath(tmpJavaPath);
            } while (!Reference.configCheck.checkJavaPath(javaPath));
            System.out.println();

            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.icon.enter"));
            System.out.print(LocalizationManager.getLocalizedString("clisetup.log.info.icon.cli"));
            includeServerIcon = readBoolean();
            appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.checkreturn"), includeServerIcon));
            System.out.println();

            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.properties.enter"));
            System.out.print(LocalizationManager.getLocalizedString("clisetup.log.info.properties.cli"));
            includeServerProperties = readBoolean();
            appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.checkreturn"), includeServerProperties));
            System.out.println();

            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.scripts.enter"));
            System.out.print(LocalizationManager.getLocalizedString("clisetup.log.info.scripts.cli"));
            includeStartScripts = readBoolean();
            appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.checkreturn"), includeStartScripts));
            System.out.println();

            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.zip.enter"));
            System.out.print(LocalizationManager.getLocalizedString("clisetup.log.info.zip.cli"));
            includeZipCreation = readBoolean();
            appLogger.info(String.format(LocalizationManager.getLocalizedString("clisetup.log.info.checkreturn"), includeZipCreation));

            Reference.configCheck.printConfig(modpackDir,
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
            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.config.enter"));
            System.out.print(LocalizationManager.getLocalizedString("clisetup.log.info.answer"));
        } while (!readBoolean());
        reader.close();

        if (Reference.filesSetup.writeConfigToFile(
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
            appLogger.info(LocalizationManager.getLocalizedString("clisetup.log.info.config.written"));
        }
    }

    /** A helper method for config setup. Prompts user to enter the values that will be stored in arrays in config.
    * @return String List. Returns list with user input values that will be stored in config.
    */
    private List<String> readStringArray() {
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
    String buildString(String... args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Arrays.toString(args));
        stringBuilder.delete(0, 2).reverse().delete(0,2).reverse();
        return stringBuilder.toString();
    }

    /** A helper method for config setup. Prompts user to enter boolean values that will be stored in config and checks entered values to prevent storing non-boolean values in boolean variables.
    * @return Boolean. Converts to boolean and returns value entered by user that will be stored in config.
    */
    private boolean readBoolean() {
        Scanner readerBoolean = new Scanner(System.in);
        String boolRead;
        while (true) {
            boolRead = readerBoolean.nextLine();
            if (boolRead.matches("1")                                                           ||
                boolRead.matches(LocalizationManager.getLocalizedString("cli.input.true")) ||
                boolRead.matches(LocalizationManager.getLocalizedString("cli.input.yes"))  ||
                boolRead.matches(LocalizationManager.getLocalizedString("cli.input.yes.short"))) {
                return true;

            } else if (boolRead.matches("0")                                                            ||
                       boolRead.matches(LocalizationManager.getLocalizedString("cli.input.false")) ||
                       boolRead.matches(LocalizationManager.getLocalizedString("cli.input.no"))    ||
                       boolRead.matches(LocalizationManager.getLocalizedString("cli.input.no.short"))) {
                return false;

            } else {
                appLogger.error(LocalizationManager.getLocalizedString("clisetup.log.error.answer"));
            }
        }
    }
}
