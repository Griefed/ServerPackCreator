package de.griefed.ServerPackCreator.i18n;

import de.griefed.ServerPackCreator.Reference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
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


    private static final Logger logger = LogManager.getLogger(LocalizationManager.class);
    /**
     * Current language of application, mapped for easier further reference.
     */
    private static Map<String, String> currentLanguage = new HashMap<>();

    /**
     * Localized strings that application uses.
     */
    private static ResourceBundle localeResources;

    /**
     * Keys that used for current language mapping.
     */
    private static final String LANGUAGE_MAP_PATH = "language";
    private static final String COUNTRY_MAP_PATH = "country";

    /**
     * List pf the languages that application supports. Must be formatted like this: en_us, de_de etc. Fields are case insensitive.
     */


    /**
     * @throws IncorrectLanguageException Thrown if the language specified in the properties file is not supported by SPC or specified in the invalid format.
     * @param locale Locale to be used by application in this run.
     */
    public static void init(String locale) throws IncorrectLanguageException {
        boolean isLanguageExists = false;
        for (String lang: Reference.SUPPORTED_LANGUAGES) {
            if (lang.equalsIgnoreCase(locale)) {
                logger.debug("Lang is correct");
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
        logger.info(String.format("Using language: %s", getLocalizedString("localeUnlocalizedName")));
        if (!currentLanguage.get(LANGUAGE_MAP_PATH).equalsIgnoreCase("en")) {
            logger.info(String.format("%s %s", getLocalizedString("cli.usingLanguage"), getLocalizedString("localeName")));
        }

    }

    /**
     * @param localePropertiesFile Path to the properties file with the language specified.
     * @throws IncorrectLanguageException Thrown if the language specified in the properties file is not supported by SPC or specified in the invalid format.
     */
    public static void init(File localePropertiesFile) throws IncorrectLanguageException{
        Properties langProperties = new Properties();
        try (FileInputStream fis = new FileInputStream(localePropertiesFile)){
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            langProperties.load(bufferedReader);
            logger.debug(String.format("langProperties = %s", langProperties));
        } catch (Exception e) {
            e.printStackTrace();
        }
        String langProp = langProperties.getProperty("lang");
        logger.debug(langProp);

        boolean isLanguageExists = false;

        for (String lang: Reference.SUPPORTED_LANGUAGES) {
            if (lang.equalsIgnoreCase(langProp)) {
                logger.debug("Lang is correct");
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
        logger.info(String.format("Using language: %s", getLocalizedString("localeUnlocalizedName")));
        if (!currentLanguage.get(LANGUAGE_MAP_PATH).equalsIgnoreCase("en")) {
            logger.info(String.format("%s %s", getLocalizedString("cli.usingLanguage"), getLocalizedString("localeName")));
        }

    }

    /**
     * Initializer with default localization properties path.
     */
    public static void init() {
        try {
            init("en_us");
        } catch (IncorrectLanguageException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets localized string from localization resource bundle.
     * @param languageKey The language key to search for.
     * @return Localized string that is referred by the language key.
     */
    public static String getLocalizedString(String languageKey) {
        try {
            String value =  localeResources.getString(languageKey);
            return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        } catch (MissingResourceException ex) {
            logger.error(String.format("ERROR: Language key %s not found in the language file.", ex.getKey()));
            System.exit(8);
        }
        return null;
    }

    public static String getLocale() {
        return String.format("%s_%s", currentLanguage.get(LANGUAGE_MAP_PATH), currentLanguage.get(COUNTRY_MAP_PATH));
    }

    public static String[] getSupportedLanguages() {
        return Reference.SUPPORTED_LANGUAGES;
    }
}
