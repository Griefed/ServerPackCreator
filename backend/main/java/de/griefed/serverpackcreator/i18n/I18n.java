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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
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
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the localizationManager for ServerPackCreator.<br> To use it, initialize it by calling
 * {@link #initialize()}. Then use {@link #getMessage(String)} to use a language key from the
 * resource bundle corresponding to the specified locale. If no locale is provided during the launch
 * of ServerPackCreator, en_US is used by default.<br> All localization properties-files need to be
 * stored in the {@code de/griefed/resources/lang/ }-directory and be named using following pattern:
 * lang_{language code in lowercase}_{country code in lowercase}. For example:
 * {@code lang_en_us.properties}.<br> Currently, only supports Strings to be used in localized
 * fields.<br> By default, ServerPackCreator tries to load language definitions from the local
 * filesystem, in the {@code lang}-folder. If no file can be found for the specified locale,
 * ServerPackCreator tries to load language definitions from inside the JAR-file, from the resource
 * bundles. If the specified key can not be retrieved when calling {@link #getMessage(String)}, a
 * default is retrieved from the lang_en_us-bundle inside the JAR-file by default.
 *
 * @author whitebear60
 * @author Griefed
 */
@Component
public final class I18n {

  private static final Logger LOG = LogManager.getLogger(I18n.class);

  private final ResourceBundle FALLBACKRESOURCES = ResourceBundle.getBundle(
      "de/griefed/resources/lang/lang_en_us",
      new Locale("en", "us"), new UTF8Control());
  private final Map<String, String> CURRENT_LANGUAGE = new HashMap<>(2);
  private final String MAP_PATH_LANGUAGE = "language";
  private final String MAP_PATH_COUNTRY = "country";
  private final List<String> SUPPORTED_LANGUAGES =
      new ArrayList<>(Arrays.asList("en_us", "uk_ua", "de_de"));
  private final File LANG_DIR;
  private ResourceBundle filesystemResources = null;
  private ResourceBundle jarResources = null;

  /**
   * Constructor for our I18n using the locale set in the {@link ApplicationProperties}-instance
   * passed to this constructor. If initialization with the provided
   * {@link ApplicationProperties}-instance fails, the I18n is initialized with the default locale
   * {@code  en_us}.
   *
   * @param languagePropertiesDirectory Directory in which the language properties-files reside in.
   *                                    Language key-value-pairs will be read and used from the
   *                                    files in this directory.
   * @author Griefed
   */
  @Autowired
  public I18n(@NotNull File languagePropertiesDirectory) {
    this(languagePropertiesDirectory, "en_us");
  }

  /**
   * Constructor for our I18n with a given locale. If initialization with the provided locale fails,
   * the I18n is initialized with the locale set in the instance of {@link ApplicationProperties}.
   * If this also fails, the default locale {@code en_us } is used.
   *
   * @param languagePropertiesDirectory Directory in which the language properties-files reside in.
   *                                    Language key-value-pairs will be read and used from the
   *                                    files in this directory.
   * @param locale                      The locale to initialize with.
   * @author Griefed
   */
  public I18n(@NotNull File languagePropertiesDirectory,
              @NotNull String locale) {
    LANG_DIR = languagePropertiesDirectory;
    try {
      initialize(locale);
    } catch (IncorrectLanguageException ex) {
      LOG.error("The specified language is not supported: " + locale, ex);
      initialize();
    }
  }

  /**
   * Initializes the I18n with a provided locale.
   *
   * @param locale Locale to be used by application in this run.
   * @throws IncorrectLanguageException Thrown if the language specified in the properties file is
   *                                    not supported by ServerPackCreator or specified in the
   *                                    invalid format.
   * @author whitebear60
   * @author Griefed
   */
  public void initialize(@NotNull String locale) throws IncorrectLanguageException {

    if (!SUPPORTED_LANGUAGES.contains(locale)) {

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

      throw new IncorrectLanguageException(locale);
    }

    try (FileInputStream fileInputStream = new FileInputStream(
        new File(LANG_DIR, "lang_" + locale + ".properties"))) {

      filesystemResources =
          new PropertyResourceBundle(
              new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));

      LOG.debug("Using language-definitions from file on filesystem.");

    } catch (IOException ex) {

      LOG.error(
          "Local language-definitions corrupted, not present or otherwise unreadable. Using definitions from JAR-file. "
              + ex.getMessage());

      try {

        filesystemResources =
            ResourceBundle.getBundle(
                "de/griefed/resources/lang/lang_" + locale,
                new Locale(
                    CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE).toLowerCase(),
                    CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY).toUpperCase()),
                new UTF8Control());

      } catch (Exception ex2) {

        LOG.error(
            "Locale resource for " + locale + " not found in JAR-file. Falling back to en_us. " +
                ex2.getMessage());

        filesystemResources =
            ResourceBundle.getBundle(
                "de/griefed/resources/lang/lang_en_us", new Locale("en", "US"), new UTF8Control());

        locale = "en_us";
      }
    }

    try {

      jarResources =
          ResourceBundle.getBundle(
              "de/griefed/resources/lang/lang_" + locale,
              new Locale(
                  CURRENT_LANGUAGE.get(MAP_PATH_LANGUAGE).toLowerCase(),
                  CURRENT_LANGUAGE.get(MAP_PATH_COUNTRY).toUpperCase()),
              new UTF8Control());

    } catch (Exception ex) {

      LOG.error(
          "Locale resource for " + locale + " not found in JAR-file. Falling back to en_us. "
              + ex.getMessage());
      jarResources = FALLBACKRESOURCES;
    }

    LOG.info(getMessage("encoding.check"));
    System.out.println(getMessage("encoding.check"));
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
   * Acquires a localized String for the provided language key from the initialized locale resource.
   * If the specified langkey can not be found in the lang-file loaded, a fallback from the
   * en_us-definitions is used.
   *
   * @param languageKey The language key to search for.
   * @return Localized string that is referred to by the language key.
   * @author whitebear60
   * @author Griefed
   */
  public @NotNull String getMessage(@NotNull String languageKey) {

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

  @SuppressWarnings("InnerClassMayBeStatic")
  private class UTF8Control extends Control {

    public ResourceBundle newBundle(
        @NotNull String baseName,
        @NotNull Locale locale,
        @NotNull String format,
        @NotNull ClassLoader loader,
        boolean reload)
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
