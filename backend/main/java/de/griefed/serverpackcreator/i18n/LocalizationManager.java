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

import de.griefed.serverpackcreator.ApplicationProperties;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This is the localizationManager for ServerPackCreator.<br>
 * To use it, initialize it by calling {@link #init()}.
 * Then use {@link #getLocalizedString(String)} to use a language key from the resource bundle corresponding to the
 * specified locale. If no locale is provided during the launch of ServerPackCreator, en_US is used by default.<br>
 * All localization properties-files need to be stored in the <code>de/griefed/resources/lang/</code>-directory
 * and be named using following pattern: lang_{language code in lowercase}_{country code in lowercase}.
 * For example: <code>lang_en_us.properties</code>.<br>
 * Currently, only supports Strings to be used in localized fields.<br>
 * By default, ServerPackCreator tries to load language definitions from the local filesystem, in the <code>lang</code>-folder.
 * If no file can be found for the specified locale, ServerPackCreator tries to load language definitions from inside the JAR-file,
 * from the resource bundles. If the specified key can not be retrieved when calling {@link #getLocalizedString(String)}, a default
 * is retrieved from the lang_en_us-bundle inside the JAR-file by default.
 * @author whitebear60
 * @author Griefed
 */
@Component
public class LocalizationManager {

    private static final Logger LOG = LogManager.getLogger(LocalizationManager.class);

    private final File PROPERTIESFILE = new File("serverpackcreator.properties");

    /**
     * Current language of ServerPackCreator, mapped for easier further reference.
     */
    private final Map<String, String> CURRENT_LANGUAGE = new HashMap<>();

    /**
     * Keys that used for current language mapping.
     */
    private final String MAP_PATH_LANGUAGE = "language";
    private final String MAP_PATH_COUNTRY = "country";

    /**
     * Languages supported by ServerPackCreator.
     */
    private final String[] SUPPORTED_LANGUAGES = new String[] {
            "en_us",
            "uk_ua",
            "de_de"
    };

    /**
     * Localized strings which ServerPackCreator uses.
     */
    private ResourceBundle localeResources;
    private final ResourceBundle fallbackResources;

    private final ApplicationProperties APPLICATIONPROPERTIES;

    /**
     * Constructor for our LocalizationManager using the locale set in serverpackcreator.properties.
     * @author Griefed
     * @param injectedApplicationProperties Instance of {@link Properties} required for various different things.
     */
    @Autowired
    public LocalizationManager(ApplicationProperties injectedApplicationProperties) {
        if (injectedApplicationProperties == null) {
            this.APPLICATIONPROPERTIES = new ApplicationProperties();
        } else {
            this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        }

        this.fallbackResources = setFallbackResources();

        try {
            init(APPLICATIONPROPERTIES);
        } catch (IncorrectLanguageException ex) {
            init();
        }
    }

    /**
     * Constructor for our LocalizationManager with a given locale.
     * @author Griefed
     * @param locale String. The locale to init with.
     * @throws IncorrectLanguageException Thrown if no language could be set by {@link LocalizationManager}.
     */
    public LocalizationManager(String locale) throws IncorrectLanguageException {
        this.APPLICATIONPROPERTIES = new ApplicationProperties();

        this.fallbackResources = setFallbackResources();

        init(locale);
    }

    /**
     * Constructor for our LocalizationManager using the default locale en_us.
     * @author Griefed
     */
    public LocalizationManager() {
        this.APPLICATIONPROPERTIES = new ApplicationProperties();

        this.fallbackResources = setFallbackResources();

        init();
    }

    /**
     * Getter for the array of languages supported by ServerPackCreator.
     * @author whitebear60
     * @return String Array. Returns the array of languages supported by ServerPackCreator.
     */
    String[] getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    /**
     * Getter for the serverpackcreator.properties file which contains the locale for ServerPackCreator.
     * @author whitebear60
     * @return File. Returns the file which will set the locale for ServerPackCreator.
     */
    File getPropertiesFile() {
        return PROPERTIESFILE;
    }

    /**
     * Getter for a String containing the currently used language.
     * @author whitebear60
     * @return String. Returns a String containing the currently used language.
     */
    public String getLocale() {
        return String.format("%s_%s", CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE), CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY));
    }

    /**
     * Initializes the LocalizationManager with a provided localePropertiesFile.
     * @author whitebear60
     * @author Griefed
     * @param serverPackCreatorProperties Instance of {@link ApplicationProperties} containing the locale to use.
     * @throws IncorrectLanguageException Thrown if the language specified in the properties file is not supported by
     * ServerPackCreator or specified in the invalid format.
     */
    public void init(ApplicationProperties serverPackCreatorProperties) throws IncorrectLanguageException{

        String langProp = serverPackCreatorProperties.getProperty("de.griefed.serverpackcreator.language", "en_us");

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

        String[] langCode;

        if (langProp.contains("_")) {

            langCode = langProp.split("_");

            CURRENT_LANGUAGE.put(MAP_PATH_LANGUAGE, langCode[0]);
            CURRENT_LANGUAGE.put(MAP_PATH_COUNTRY, langCode[1]);

        } else {

            throw new IncorrectLanguageException();

        }

        try (InputStream inputStream = FileUtils.openInputStream(new File(String.format("lang/lang_%s.properties", serverPackCreatorProperties.getProperty("de.griefed.serverpackcreator.language"))))) {

            localeResources = new PropertyResourceBundle(inputStream);

            LOG.debug("Using local language-definitions.");

        } catch (IOException ex) {

            if (!ex.toString().startsWith("java.io.FileNotFoundException")) {
                LOG.error("Local language-definitions corrupted, not present or otherwise unreadable. Using defaults.", ex);
            } else {
                LOG.error("Local language-definitions corrupted, not present or otherwise unreadable. Using defaults.");
            }

            localeResources = ResourceBundle.getBundle(String.format("de/griefed/resources/lang/lang_%s", serverPackCreatorProperties.getProperty("de.griefed.serverpackcreator.language")), new Locale(CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE), CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY)));

        }

        LOG.info(String.format("Using language: %s", getLocalizedString("localeUnlocalizedName")));

        if (!CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE).equalsIgnoreCase("en")) {

            LOG.info(String.format("%s %s", getLocalizedString("cli.usingLanguage"), getLocalizedString("localeName")));
        }
    }

    /**
     * Set the fallback resource bundle which is used in case any specified langkey can not be retrieved from local definitions.
     * @author Griefed
     * @return ResourceBundle. Returns a resource bundle containing the lang_en_us langkey definitions.
     */
    private ResourceBundle setFallbackResources() {
        return ResourceBundle.getBundle("de/griefed/resources/lang/lang_en_us", new Locale("en", "us"));
    }

    /**
     * Initializes the LocalizationManager with a provided locale.
     * Calls<br>
     * {@link #getSupportedLanguages()}<br>
     * {@link #getLocalizedString(String)}<br>
     * {@link #writeLocaleToFile(String)}
     * @author whitebear60
     * @author Griefed
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

        try (InputStream inputStream = FileUtils.openInputStream(new File(String.format("lang/lang_%s.properties", locale)))) {

            localeResources = new PropertyResourceBundle(inputStream);

            LOG.debug("Using local language-definitions.");

        } catch (IOException ex) {

            if (!ex.toString().startsWith("java.io.FileNotFoundException")) {
                LOG.error("Local language-definitions corrupted, not present or otherwise unreadable. Using defaults.", ex);
            } else {
                LOG.error("Local language-definitions corrupted, not present or otherwise unreadable. Using defaults.");
            }

            localeResources = ResourceBundle.getBundle(String.format("de/griefed/resources/lang/lang_%s", locale), new Locale(CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE), CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY)));

        }

        LOG.info(String.format("Using language: %s", getLocalizedString("localeUnlocalizedName")));

        if (!CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE).equalsIgnoreCase("en")) {

            LOG.info(String.format("%s %s", getLocalizedString("cli.usingLanguage"), getLocalizedString("localeName")));
        }

        writeLocaleToFile(locale);
    }

    /**
     * Initializes the LocalizationManager with a provided localePropertiesFile.
     * @author whitebear60
     * @author Griefed
     * @param localePropertiesFile Path to the locale properties file which specifies the language to use.
     * @throws IncorrectLanguageException Thrown if the language specified in the properties file is not supported by
     * ServerPackCreator or specified in the invalid format.
     */
    public void init(File localePropertiesFile) throws IncorrectLanguageException{

        ApplicationProperties langProperties = new ApplicationProperties();

        try (FileInputStream fileInputStream = new FileInputStream(localePropertiesFile)){

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
            langProperties.load(bufferedReader);

            LOG.debug(String.format("serverpackcreator.properties = %s", langProperties));

        } catch (Exception ex) {
            LOG.error(ex);
        }

        String langProp = langProperties.getProperty("de.griefed.serverpackcreator.language", "en_us");

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

        String[] langCode;

        if (langProp.contains("_")) {

            langCode = langProp.split("_");

            CURRENT_LANGUAGE.put(MAP_PATH_LANGUAGE, langCode[0]);
            CURRENT_LANGUAGE.put(MAP_PATH_COUNTRY, langCode[1]);

        } else {
            throw new IncorrectLanguageException();
        }

        try (InputStream inputStream = FileUtils.openInputStream(new File(String.format("lang/lang_%s.properties", langProperties.getProperty("de.griefed.serverpackcreator.language"))))) {

            localeResources = new PropertyResourceBundle(inputStream);

            LOG.debug("Using local language-definitions.");

        } catch (IOException ex) {

            if (!ex.toString().startsWith("java.io.FileNotFoundException")) {
                LOG.error("Local language-definitions corrupted, not present or otherwise unreadable. Using defaults.", ex);
            } else {
                LOG.error("Local language-definitions corrupted, not present or otherwise unreadable. Using defaults.");
            }

            localeResources = ResourceBundle.getBundle(String.format("de/griefed/resources/lang/lang_%s", langProperties.getProperty("de.griefed.serverpackcreator.language")), new Locale(CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE), CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY)));

        }

        LOG.info(String.format("Using language: %s", getLocalizedString("localeUnlocalizedName")));

        if (!CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE).equalsIgnoreCase("en")) {

            LOG.info(String.format("%s %s", getLocalizedString("cli.usingLanguage"), getLocalizedString("localeName")));
        }
    }

    /**
     * Initialize the LocalizationManager with en_us as the locale.
     * @author whitebear60
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
     * If the specified langkey can not be found in the lang-file loaded, a fallback from the en_us-definitions is used.
     * @author whitebear60
     * @author Griefed
     * @param languageKey The language key to search for.
     * @return Localized string that is referred to by the language key.
     */
    public String getLocalizedString(String languageKey) {

        //noinspection UnusedAssignment
        String text = null;
        String value = null;

        try {

            value =  localeResources.getString(languageKey);

        } catch (MissingResourceException ex) {

            LOG.debug(String.format("Language key \"%s\" not found for locale \"%s_%s\". Using fallback from \"en_US\".", ex.getKey(), CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE), CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY)));

            value = fallbackResources.getString(languageKey);

        } finally {

            text = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

        }

        return text;

    }

    /**
     * Check for existence of a lang.properties file and, if found, assign the language specified therein.
     * If assigning the specified language fails because it is not supported, default to en_us.
     * This method should <strong>not</strong> call {@link #getLocalizedString(String)}, as the initialization of
     * said manager is called from here. Therefore, localized strings are not yet available.
     * @author Griefed
     */
    public void checkLocaleFile() {
        try {
            init(getPropertiesFile());
        } catch (IncorrectLanguageException e) {

            LOG.error("Incorrect language specified, falling back to English (United States)...");

            init();

            try (OutputStream outputStream = new FileOutputStream(getPropertiesFile())) {
                APPLICATIONPROPERTIES.setProperty("de.griefed.serverpackcreator.language", "en_us");
                APPLICATIONPROPERTIES.store(outputStream, null);
            } catch (IOException ex) {
                LOG.error("Couldn't write properties-file.", ex);
            }

        }
    }

    /**
     * Writes the specified locale from -lang your_locale to a lang.properties file to ensure every subsequent start
     * of serverpackcreator is executed using said locale. This method should <strong>not</strong> call
     * {@link #getLocalizedString(String)}, as the initialization of said manager is called from here. Therefore,
     * localized strings are not yet available.
     * @author Griefed
     * @param locale The locale the user specified when they ran serverpackcreator with -lang -your_locale.
     */
    void writeLocaleToFile(String locale) {
        try (OutputStream outputStream = new FileOutputStream(getPropertiesFile())) {
            APPLICATIONPROPERTIES.setProperty("de.griefed.serverpackcreator.language", locale);
            APPLICATIONPROPERTIES.store(outputStream, null);
        } catch (IOException ex) {
            LOG.error("Couldn't write properties-file.", ex);
        }
    }
}