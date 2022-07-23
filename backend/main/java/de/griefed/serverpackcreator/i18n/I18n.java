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
package de.griefed.serverpackcreator.i18n;

import de.griefed.serverpackcreator.ApplicationProperties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the localizationManager for ServerPackCreator.<br>
 * To use it, initialize it by calling {@link #initialize()}. Then use {@link #getMessage(String)}
 * to use a language key from the resource bundle corresponding to the specified locale. If no
 * locale is provided during the launch of ServerPackCreator, en_US is used by default.<br>
 * All localization properties-files need to be stored in the <code>de/griefed/resources/lang/
 * </code>-directory and be named using following pattern: lang_{language code in
 * lowercase}_{country code in lowercase}. For example: <code>lang_en_us.properties</code>.<br>
 * Currently, only supports Strings to be used in localized fields.<br>
 * By default, ServerPackCreator tries to load language definitions from the local filesystem, in
 * the <code>lang</code>-folder. If no file can be found for the specified locale, ServerPackCreator
 * tries to load language definitions from inside the JAR-file, from the resource bundles. If the
 * specified key can not be retrieved when calling {@link #getMessage(String)}, a default is
 * retrieved from the lang_en_us-bundle inside the JAR-file by default.
 *
 * @author whitebear60
 * @author Griefed
 */
@Component
public class I18n {

  private static final Logger LOG = LogManager.getLogger(I18n.class);

  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final ResourceBundle FALLBACKRESOURCES =
      ResourceBundle.getBundle(
          "de/griefed/resources/lang/lang_en_us", new Locale("en", "us"), new UTF8Control());
  private final Map<String, String> CURRENT_LANGUAGE = new HashMap<>();
  private final File PROPERTIESFILE = new File("serverpackcreator.properties");
  private final String MAP_PATH_LANGUAGE = "language";
  private final String MAP_PATH_COUNTRY = "country";
  private final List<String> SUPPORTED_LANGUAGES =
      new ArrayList<>(Arrays.asList("en_us", "uk_ua", "de_de"));

  private ResourceBundle filesystemResources = null;
  private ResourceBundle jarResources = null;

  /**
   * Constructor for our I18n using the locale set in the {@link ApplicationProperties}-instance
   * passed to this constructor. If initialization with the provided {@link
   * ApplicationProperties}-instance fails, the I18n is initialized with the default locale <code>
   * en_us</code>.
   *
   * @param injectedApplicationProperties Instance of {@link ApplicationProperties} required for
   *     various different things.
   * @author Griefed
   */
  @Autowired
  public I18n(ApplicationProperties injectedApplicationProperties) {
    this.APPLICATIONPROPERTIES = injectedApplicationProperties;

    try {
      initialize(APPLICATIONPROPERTIES);
    } catch (IncorrectLanguageException ex) {
      LOG.error("Incorrect language specified.", ex);
      initialize();
    }
  }

  /**
   * Constructor for our I18n with a given locale. If initialization with the provided locale fails,
   * the I18n is initialized with the locale set in the instance of {@link ApplicationProperties}.
   * If this also fails, the default locale <code>en_us
   * </code> is used.
   *
   * @param injectedApplicationProperties Instance of {@link ApplicationProperties} required for
   *     various different things.
   * @param locale String. The locale to initialize with.
   * @author Griefed
   */
  public I18n(ApplicationProperties injectedApplicationProperties, String locale) {
    if (injectedApplicationProperties == null) {
      this.APPLICATIONPROPERTIES = new ApplicationProperties();
    } else {
      this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    }
    try {

      initialize(locale);
      writeLocaleToFile(locale);

    } catch (IncorrectLanguageException ex) {
      try {
        initialize(APPLICATIONPROPERTIES);
      } catch (IncorrectLanguageException e) {
        initialize();
      }
    }
  }

  /**
   * Constructor for our I18n using the default locale en_us.
   *
   * @author Griefed
   */
  public I18n() {
    this.APPLICATIONPROPERTIES = new ApplicationProperties();

    initialize();
  }

  /**
   * Initialize the I18n with en_us as the locale.
   *
   * @author whitebear60
   */
  public void initialize() {
    try {
      initialize("en_us");
    } catch (IncorrectLanguageException e) {
      LOG.error("Error during default localization initialization.");
    }
  }

  /**
   * Initializes the I18n with a provided localePropertiesFile.
   *
   * @param propertiesFile Path to the locale properties file which specifies the language to use.
   * @throws IncorrectLanguageException Thrown if the language specified in the properties file is
   *     not supported by ServerPackCreator or specified in the invalid format.
   * @author whitebear60
   * @author Griefed
   */
  public void initialize(File propertiesFile) throws IncorrectLanguageException {

    ApplicationProperties applicationProperties = new ApplicationProperties();

    try (FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {

      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
      applicationProperties.load(bufferedReader);

      LOG.debug("Properties-file used for i18n: " + applicationProperties);

    } catch (Exception ex) {
      LOG.error("Couldn't load properties-file for i18n: " + applicationProperties, ex);
    }

    initialize(applicationProperties);
  }

  /**
   * Initializes the I18n with a provided localePropertiesFile.
   *
   * @param applicationProperties Instance of {@link ApplicationProperties} containing the locale to
   *     use.
   * @throws IncorrectLanguageException Thrown if the language specified in the properties file is
   *     not supported by ServerPackCreator or specified in the invalid format.
   * @author whitebear60
   * @author Griefed
   */
  public void initialize(ApplicationProperties applicationProperties)
      throws IncorrectLanguageException {
    initialize(applicationProperties.getProperty("de.griefed.serverpackcreator.language", "en_us"));
  }

  /**
   * Initializes the I18n with a provided locale.
   *
   * @param locale Locale to be used by application in this run.
   * @throws IncorrectLanguageException Thrown if the language specified in the properties file is
   *     not supported by ServerPackCreator or specified in the invalid format.
   * @author whitebear60
   * @author Griefed
   */
  public void initialize(String locale) throws IncorrectLanguageException {

    boolean doesLanguageExist = false;

    if (SUPPORTED_LANGUAGES.contains(locale)) {

      doesLanguageExist = true;

    } else {

      LOG.warn(
          "The specified language is not officially supported. You may provide your own definitions and translations.");
      LOG.warn(
          "Beware: When doing this, you are on your own and will receive no support. Errors and problems may occur when loading and working with custom language definitions and translations which are not officially supported yet.");
      LOG.warn(
          "When you are satisfied with your translations, feel free to open an issue on GitHub and submit them so they can get officially added and supported to and by ServerPackCreator!");
      LOG.warn("GitHub issues available at: https://github.com/Griefed/ServerPackCreator/issues");
    }

    String[] langCode;

    if (locale.contains("_")) {

      langCode = locale.split("_");

      CURRENT_LANGUAGE.put(MAP_PATH_LANGUAGE, langCode[0]);
      CURRENT_LANGUAGE.put(MAP_PATH_COUNTRY, langCode[1]);

    } else {

      LOG.error("Locales must be formatted like this: \"en_us\",\"uk_ua\",\"de_de\"");

      throw new IncorrectLanguageException();
    }

    try (FileInputStream fileInputStream =
        new FileInputStream(String.format("lang/lang_%s.properties", locale))) {

      filesystemResources =
          new PropertyResourceBundle(
              new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));

      LOG.debug("Using language-definitions from file on filesystem.");

    } catch (IOException ex) {

      LOG.error(
          "Local language-definitions corrupted, not present or otherwise unreadable. Using definitions from JAR-file.");

      try {

        filesystemResources =
            ResourceBundle.getBundle(
                String.format("de/griefed/resources/lang/lang_%s", locale),
                new Locale(
                    CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE).toLowerCase(),
                    CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY).toUpperCase()),
                new UTF8Control());

      } catch (Exception ex2) {

        LOG.error(
            "Locale resource for " + locale + " not found in JAR-file. Falling back to en_us.",
            ex2);

        filesystemResources =
            ResourceBundle.getBundle(
                "de/griefed/resources/lang/lang_en_us", new Locale("en", "US"), new UTF8Control());

        locale = "en_us";
      }
    }

    if (doesLanguageExist && !locale.equals("en_us")) {

      try {

        jarResources =
            ResourceBundle.getBundle(
                String.format("de/griefed/resources/lang/lang_%s", locale),
                new Locale(
                    CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE).toLowerCase(),
                    CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY).toUpperCase()),
                new UTF8Control());

      } catch (Exception ex) {

        LOG.error("Locale resource for " + locale + " not found in JAR-file.", ex);
        jarResources = FALLBACKRESOURCES;
      }
    }

    if (APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION().equals("dev")) {
      LOG.info(getMessage("encoding.check"));
      System.out.println(getMessage("encoding.check"));
    }
  }

  /**
   * Acquires a localized String for the provided language key from the initialized locale resource.
   * If the specified langkey can not be found in the lang-file loaded, a fallback from the
   * en_us-definitions is used.
   *
   * @param languageKey The language key to search for.
   * @return Localized string that is referred to by the language key.
   * @author whitebear60
   * @author Griefed
   */
  public String getMessage(String languageKey) {

    //noinspection UnusedAssignment
    String text = null;
    String value = null;

    try {

      value = filesystemResources.getString(languageKey);

    } catch (MissingResourceException ex) {

      if (jarResources != null) {

        try {

          value = jarResources.getString(languageKey);
          LOG.debug(
              String.format(
                  "Language key \"%s\" not found in local file resource \"%s_%s\". Using fallback from JAR-resources.",
                  languageKey,
                  CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE),
                  CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY)));

        } catch (MissingResourceException ex2) {

          value = FALLBACKRESOURCES.getString(languageKey);
          LOG.debug(
              String.format(
                  "Language key \"%s\" not found in JAR-locale-resource \"%s_%s\". Using fallback from \"en_US\".",
                  ex2.getKey(),
                  CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE),
                  CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY)));
        }

      } else {

        value = FALLBACKRESOURCES.getString(languageKey);
        LOG.debug(
            String.format(
                "Language key \"%s\" not found for locale \"%s_%s\". Using JAR-resources fallback from \"en_US\".",
                ex.getKey(),
                CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE),
                CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY)));
      }

    } finally {

      if (value != null) {

        text = value;

      } else {

        text =
            "i18n: No value found for key "
                + languageKey
                + " , Locale: "
                + CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE)
                + "_"
                + CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY);
      }
    }

    return text;
  }

  /**
   * Writes the specified locale from -lang your_locale to a lang.properties file to ensure every
   * subsequent start of serverpackcreator is executed using said locale. This method should
   * <strong>not</strong> call {@link #getMessage(String)}, as the initialization of said manager is
   * called from here. Therefore, localized strings are not yet available.
   *
   * @param locale The locale the user specified when they ran serverpackcreator with -lang
   *     -your_locale.
   * @author Griefed
   */
  void writeLocaleToFile(String locale) {
    try (OutputStream outputStream = Files.newOutputStream(PROPERTIESFILE.toPath())) {
      APPLICATIONPROPERTIES.setProperty("de.griefed.serverpackcreator.language", locale);
      APPLICATIONPROPERTIES.store(outputStream, null);
    } catch (IOException ex) {
      LOG.error("Couldn't write properties-file.", ex);
    }
  }

  public static class UTF8Control extends Control {
    public ResourceBundle newBundle(
        String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
        throws IOException {
      // The below is a copy of the default implementation.
      String bundleName = toBundleName(baseName, locale);
      String resourceName = toResourceName(bundleName, "properties");
      ResourceBundle bundle = null;
      InputStream stream = null;
      if (reload) {
        URL url = loader.getResource(resourceName);
        if (url != null) {
          URLConnection connection = url.openConnection();
          if (connection != null) {
            connection.setUseCaches(false);
            stream = connection.getInputStream();
          }
        }
      } else {
        stream = loader.getResourceAsStream(resourceName);
      }
      if (stream != null) {
        try {
          // Only this line is changed to make it to read properties files as UTF-8.
          bundle =
              new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } finally {
          stream.close();
        }
      }
      return bundle;
    }
  }
}
