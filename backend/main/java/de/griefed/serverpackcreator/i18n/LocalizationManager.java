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
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <strong>Table of methods</strong><br>
 * {@link #getSupportedLanguages()}<br>
 * {@link #getLangPropertiesFile()}<br>
 * {@link #getLocale()}<br>
 * {@link #init(String)}<br>
 * {@link #init(File)}<br>
 * {@link #init()}<br>
 * {@link #getLocalizedString(String)}<br>
 * {@link #checkLocaleFile()}<br>
 * {@link #writeLocaleToFile(String)}
 * <p>
 * This is the localizationManager for ServerPackCreator.<br>
 * To use it, initialize it by calling {@link #init()}.
 * Then use {@link #getLocalizedString(String)} to use a language key from the resource bundle corresponding to the
 * specified locale. If no locale is provided during the launch of ServerPackCreator, en_US is used by default.<br>
 * All localization properties-files need to be stored in the <code>de/griefed/resources/lang/</code>-directory
 * and be named using following pattern: lang_{language code in lowercase}_{country code in lowercase}.
 * For example: <code>lang_en_us.properties</code>.<br>
 * Currently only supports Strings to be used in localized fields.
 */
@Component
public class LocalizationManager {

    private static final Logger LOG = LogManager.getLogger(LocalizationManager.class);
    private final File FILE_LANGPROPERTIES = new File("lang.properties");

    /**
     * Current language of ServerPackCreator, mapped for easier further reference.
     */
    private final Map<String, String> CURRENT_LANGUAGE = new HashMap<>();

    /**
     * Localized strings which ServerPackCreator uses.
     */
    private ResourceBundle localeResources;

    /**
     * Keys that used for current language mapping.
     */
    private final String MAP_PATH_LANGUAGE = "language";
    private final String MAP_PATH_COUNTRY = "country";

    /**
     * Languages supported by ServerPackCreator.
     */
    private final String[] SUPPORTED_LANGUAGES = {
            "en_us",
            "uk_ua",
            "de_de"
    };

    /**
     * Getter for the array of languages supported by ServerPackCreator.
     * @return String Array. Returns the array of languages supported by ServerPackCreator.
     */
    String[] getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    /**
     * Getter for the lang.properties file which will set the locale for ServerPackCreator.
     * @return File. Returns the file which will set the locale for ServerPackCreator.
     */
    File getLangPropertiesFile() {
        return FILE_LANGPROPERTIES;
    }

    /**
     * Getter for a String containing the currently used language.
     * @return String. Returns a String containing the currently used language.
     */
    public String getLocale() {
        return String.format("%s_%s", CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE), CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY));
    }

    /**
     * Initializes the LocalizationManager with a provided locale.
     * Calls<br>
     * {@link #getSupportedLanguages()}<br>
     * {@link #getLocalizedString(String)}<br>
     * {@link #writeLocaleToFile(String)}
     * @throws IncorrectLanguageException Thrown if the language specified in the properties file is not supported by
     * ServerPackCreator or specified in the invalid format.
     * @param locale Locale to be used by application in this run.
     */
    public void init(String locale) throws IncorrectLanguageException {
        boolean doesLanguageExist = false;

        for (String lang: getSupportedLanguages()) {

            if (lang.equalsIgnoreCase(locale)) {

                // Can not yet use localization, as the localization manager is still being initialized.
                LOG.debug("Lang is correct");
                doesLanguageExist = true;

                break;
            }
        }

        if (Boolean.FALSE.equals(doesLanguageExist)) throw new IncorrectLanguageException();

        String[] langCode;

        if (locale.contains("_")) {

            langCode = locale.split("_");

            CURRENT_LANGUAGE.put(MAP_PATH_LANGUAGE, langCode[0]);
            CURRENT_LANGUAGE.put(MAP_PATH_COUNTRY, langCode[1]);

        } else {
            throw new IncorrectLanguageException();
        }

        localeResources = ResourceBundle.getBundle(String.format("de/griefed/resources/lang/lang_%s", locale));

        LOG.info(String.format("Using language: %s", getLocalizedString("localeUnlocalizedName")));

        if (!CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE).equalsIgnoreCase("en")) {

            LOG.info(String.format("%s %s", getLocalizedString("cli.usingLanguage"), getLocalizedString("localeName")));
        }

        writeLocaleToFile(locale);
    }

    /**
     * Initializes the LocalizationManager with a provided localePropertiesFile.
     * @param localePropertiesFile Path to the locale properties file which specifies the language to use.
     * @throws IncorrectLanguageException Thrown if the language specified in the properties file is not supported by
     * ServerPackCreator or specified in the invalid format.
     */
    public void init(File localePropertiesFile) throws IncorrectLanguageException{

        Properties langProperties = new Properties();

        try (FileInputStream fileInputStream = new FileInputStream(localePropertiesFile)){

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
            langProperties.load(bufferedReader);

            LOG.debug(String.format("langProperties = %s", langProperties));

        } catch (Exception ex) {
            LOG.error(ex);
        }

        String langProp = langProperties.getProperty("lang");
        LOG.debug(langProp);

        boolean doesLanguageExist = false;

        for (String lang: getSupportedLanguages()) {

            if (lang.equalsIgnoreCase(langProp)) {

                LOG.debug("Lang is correct");
                doesLanguageExist = true;

                break;
            }
        }

        if (Boolean.FALSE.equals(doesLanguageExist)) throw new IncorrectLanguageException();

        String defaultLocale = "en_us";
        String langProperty = langProperties.getProperty("lang", defaultLocale);
        String[] langCode;

        if (langProperty.contains("_")) {

            langCode = langProperty.split("_");

            CURRENT_LANGUAGE.put(MAP_PATH_LANGUAGE, langCode[0]);
            CURRENT_LANGUAGE.put(MAP_PATH_COUNTRY, langCode[1]);

        } else {
            throw new IncorrectLanguageException();
        }

        localeResources = ResourceBundle.getBundle(String.format("de/griefed/resources/lang/lang_%s", langProperties.getProperty("lang")), new Locale(CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE), CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY)));
        LOG.info(String.format("Using language: %s", getLocalizedString("localeUnlocalizedName")));

        if (!CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE).equalsIgnoreCase("en")) {

            LOG.info(String.format("%s %s", getLocalizedString("cli.usingLanguage"), getLocalizedString("localeName")));
        }
    }

    /**
     * Initialize the LocalizationManager with en_us as the locale.
     */
    public void init() {
        try {
            init("en_us");
        } catch (IncorrectLanguageException e) {
            LOG.error("Error during default localization initialization.");
        }
    }

    /**
     * Acquires a localized String for the provided language key from the initialized locale resource.
     * @param languageKey The language key to search for.
     * @return Localized string that is referred to by the language key.
     */
    public String getLocalizedString(String languageKey) {
        try {
            String value =  localeResources.getString(languageKey);
            return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        } catch (MissingResourceException ex) {
            LOG.error(String.format("ERROR: Language key %s not found in the language file.", ex.getKey()));
            System.exit(8);
        }
        return null;
    }

    /**
     * Check for existence of a lang.properties file and, if found, assign the language specified therein.
     * If assigning the specified language fails because it is not supported, default to en_us.
     * This method should <strong>not</strong> call {@link #getLocalizedString(String)}, as the initialization of
     * said manager is called from here. Therefore, localized strings are not yet available.
     */
    public void checkLocaleFile() {
        if (getLangPropertiesFile().exists()) {
            try {
                init(getLangPropertiesFile());
            } catch (IncorrectLanguageException e) {

                LOG.error("Incorrect language specified, falling back to English (United States)...");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(getLangPropertiesFile()))) {

                    if (!getLangPropertiesFile().exists()) {
                        boolean langCreated = getLangPropertiesFile().createNewFile();
                        if (langCreated) {
                            LOG.debug("Lang properties file created successfully.");
                        } else {
                            LOG.debug("Lang properties file not created.");
                        }
                    }

                    writer.write(String.format("# Supported languages: %s%n", Arrays.toString(getSupportedLanguages())));
                    writer.write(String.format("lang=en_us%n"));

                } catch (IOException ex) {
                    LOG.error("Error: There was an error writing the localization properties file.", ex);
                }
                init();
            }
        } else {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(getLangPropertiesFile()))) {

                if (!getLangPropertiesFile().exists()) {
                    boolean langCreated = getLangPropertiesFile().createNewFile();
                    if (langCreated) {
                        LOG.debug("Lang properties file created successfully.");
                    } else {
                        LOG.debug("Lang properties file not created.");
                    }
                }

                writer.write(String.format("# Supported languages: %s%n", Arrays.toString(getSupportedLanguages())));
                writer.write(String.format("lang=en_us%n"));

            } catch (IOException ex) {
                LOG.error("Error: There was an error writing the localization properties file.", ex);
            }
            init();
        }
    }

    /**
     * Writes the specified locale from -lang your_locale to a lang.properties file to ensure every subsequent start
     * of serverpackcreator is executed using said locale. This method should <strong>not</strong> call
     * {@link #getLocalizedString(String)}, as the initialization of said manager is called from here. Therefore,
     * localized strings are not yet available.
     * @param locale The locale the user specified when they ran serverpackcreator with -lang -your_locale.
     */
    void writeLocaleToFile(String locale) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getLangPropertiesFile()))) {

            if (!getLangPropertiesFile().exists()) {
                boolean langCreated = getLangPropertiesFile().createNewFile();
                if (langCreated) {
                    LOG.debug("Lang properties file created successfully.");
                } else {
                    LOG.debug("Lang properties file not created.");
                }
            }

            writer.write(String.format("# Supported languages: %s%n", Arrays.toString(getSupportedLanguages())));
            writer.write(String.format("lang=%s%n", locale));

        } catch (IOException ex) {
            LOG.error("Error: There was an error writing the localization properties file.", ex);
        }
    }
}