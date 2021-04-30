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

package de.griefed.serverpackcreator.i18n;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class is localization manager for your application.
 * <p>To use it, firstly run LocalizationManager.init().
 * Then use LocalizationManager.getLocalizedString() to find the localized string in config file.
 * All localization config files need to be stored in <code>resources/i18n</code> directory
 * and be named using following pattern: lang_{language code in lowercase}_{country code in lowercase}.
 * For example: lang_en_us.lang.</p>
 * Currently supports only strings to be used in localized fields.
 */
public class LocalizationManager {

    private static final Logger localeLogger = LogManager.getLogger(LocalizationManager.class);
    private final File langPropertiesFile = new File("lang.properties");

    /**
     * Current language of application, mapped for easier further reference.
     */
    private Map<String, String> currentLanguage = new HashMap<>();

    /**
     * Localized strings that application uses.
     */
    private ResourceBundle localeResources;

    /**
     * Keys that used for current language mapping.
     */
    private final String LANGUAGE_MAP_PATH = "language";
    private final String COUNTRY_MAP_PATH = "country";
    private final String[] SUPPORTED_LANGUAGES = {
            "en_us",
            "uk_ua",
            "de_de"
    };

    String[] getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    File getLangPropertiesFile() {
        return langPropertiesFile;
    }

    public String getLocale() {
        return String.format("%s_%s", currentLanguage.get(LANGUAGE_MAP_PATH), currentLanguage.get(COUNTRY_MAP_PATH));
    }

    /**
     * @throws IncorrectLanguageException Thrown if the language specified in the properties file is not supported by SPC or specified in the invalid format.
     * @param locale Locale to be used by application in this run.
     */
    public void init(String locale) throws IncorrectLanguageException {
        boolean isLanguageExists = false;
        for (String lang: getSupportedLanguages()) {
            if (lang.equalsIgnoreCase(locale)) {
                localeLogger.debug("Lang is correct");
                isLanguageExists = true;
                break;
            }
        }
        if (Boolean.FALSE.equals(isLanguageExists)) throw new IncorrectLanguageException();
        String[] langCode;
        if (locale.contains("_")) {
            langCode = locale.split("_");
            currentLanguage.put(LANGUAGE_MAP_PATH, langCode[0]);
            currentLanguage.put(COUNTRY_MAP_PATH, langCode[1]);
        } else {
            throw new IncorrectLanguageException();
        }
        localeResources = ResourceBundle.getBundle(String.format("de/griefed/resources/lang/lang_%s", locale));
        localeLogger.info(String.format("Using language: %s", getLocalizedString("localeUnlocalizedName")));
        if (!currentLanguage.get(LANGUAGE_MAP_PATH).equalsIgnoreCase("en")) {
            localeLogger.info(String.format("%s %s", getLocalizedString("cli.usingLanguage"), getLocalizedString("localeName")));
        }
        writeLocaleToFile(locale);
    }

    /**
     * @param localePropertiesFile Path to the properties file with the language specified.
     * @throws IncorrectLanguageException Thrown if the language specified in the properties file is not supported by SPC or specified in the invalid format.
     */
    public void init(File localePropertiesFile) throws IncorrectLanguageException{
        Properties langProperties = new Properties();
        try (FileInputStream fis = new FileInputStream(localePropertiesFile)){
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            langProperties.load(bufferedReader);
            localeLogger.debug(String.format("langProperties = %s", langProperties));
        } catch (Exception e) {
            e.printStackTrace();
        }
        String langProp = langProperties.getProperty("lang");
        localeLogger.debug(langProp);

        boolean isLanguageExists = false;

        for (String lang: getSupportedLanguages()) {
            if (lang.equalsIgnoreCase(langProp)) {
                localeLogger.debug("Lang is correct");
                isLanguageExists = true;
                break;
            }
        }

        if (Boolean.FALSE.equals(isLanguageExists)) throw new IncorrectLanguageException();
        String defaultLocale = "en_us";
        String langProperty = langProperties.getProperty("lang", defaultLocale);
        String[] langCode;
        if (langProperty.contains("_")) {
            langCode = langProperty.split("_");
            currentLanguage.put(LANGUAGE_MAP_PATH, langCode[0]);
            currentLanguage.put(COUNTRY_MAP_PATH, langCode[1]);
        } else {
            throw new IncorrectLanguageException();
        }

        localeResources = ResourceBundle.getBundle(String.format("de/griefed/resources/lang/lang_%s", langProperties.getProperty("lang")), new Locale(currentLanguage.get(LANGUAGE_MAP_PATH), currentLanguage.get(COUNTRY_MAP_PATH)));
        localeLogger.info(String.format("Using language: %s", getLocalizedString("localeUnlocalizedName")));
        if (!currentLanguage.get(LANGUAGE_MAP_PATH).equalsIgnoreCase("en")) {
            localeLogger.info(String.format("%s %s", getLocalizedString("cli.usingLanguage"), getLocalizedString("localeName")));
        }
    }

    /**
     * Initializer with default localization properties path.
     */
    public void init() {
        try {
            init("en_us");
        } catch (IncorrectLanguageException e) {
            localeLogger.error("Error during default localization initialization.");
        }
    }

    /**
     * Gets localized string from localization resource bundle.
     * @param languageKey The language key to search for.
     * @return Localized string that is referred by the language key.
     */
    public String getLocalizedString(String languageKey) {
        try {
            String value =  localeResources.getString(languageKey);
            return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        } catch (MissingResourceException ex) {
            localeLogger.error(String.format("ERROR: Language key %s not found in the language file.", ex.getKey()));
            System.exit(8);
        }
        return null;
    }

    /**
     * Check for existence of a lang.properties-file and if found assign language specified therein. If assigning the specified language fails because it is not supported, default to en_US.
     * This method should not contain the LocalizationManager, as the initialization of said manager is called from here. Therefore, localized string are not yet available.
     * @return Always returns true. Dirty hack until I one day figure out how to init Localization before UI start correctly.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean checkLocaleFile() {
        if (getLangPropertiesFile().exists()) {
            try {
                init(getLangPropertiesFile());
            } catch (IncorrectLanguageException e) {

                localeLogger.error("Incorrect language specified, falling back to English (United States)...");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(getLangPropertiesFile()))) {

                    if (!getLangPropertiesFile().exists()) {
                        boolean langCreated = getLangPropertiesFile().createNewFile();
                        if (langCreated) {
                            localeLogger.debug("Lang properties file created successfully.");
                        } else {
                            localeLogger.debug("Lang properties file not created.");
                        }
                    }

                    writer.write(String.format("# Supported languages: %s%n", Arrays.toString(getSupportedLanguages())));
                    writer.write(String.format("lang=en_us%n"));

                } catch (IOException ex) {
                    localeLogger.error("Error: There was an error writing the localization properties file.", ex);
                }
                init();
            }
        } else {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(getLangPropertiesFile()))) {

                if (!getLangPropertiesFile().exists()) {
                    boolean langCreated = getLangPropertiesFile().createNewFile();
                    if (langCreated) {
                        localeLogger.debug("Lang properties file created successfully.");
                    } else {
                        localeLogger.debug("Lang properties file not created.");
                    }
                }

                writer.write(String.format("# Supported languages: %s%n", Arrays.toString(getSupportedLanguages())));
                writer.write(String.format("lang=en_us%n"));

            } catch (IOException ex) {
                localeLogger.error("Error: There was an error writing the localization properties file.", ex);
            }
            init();
        }
        return true;
    }

    /**
     * Writes the specified locale from -lang your_locale to a lang.properties file to ensure every subsequent start of serverpackcreator is executed using said locale.
     * @param locale The locale the user specified when they ran serverpackcreator with -lang -your_locale.
     * This method should not contain the LocalizationManager, as the initialization of said manager is called from here. Therefore, localized string are not yet available.
     */
    void writeLocaleToFile(String locale) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getLangPropertiesFile()))) {

            if (!getLangPropertiesFile().exists()) {
                boolean langCreated = getLangPropertiesFile().createNewFile();
                if (langCreated) {
                    localeLogger.debug("Lang properties file created successfully.");
                } else {
                    localeLogger.debug("Lang properties file not created.");
                }
            }

            writer.write(String.format("# Supported languages: %s%n", Arrays.toString(getSupportedLanguages())));
            writer.write(String.format("lang=%s%n", locale));

        } catch (IOException ex) {
            localeLogger.error("Error: There was an error writing the localization properties file.", ex);
        }
    }
}