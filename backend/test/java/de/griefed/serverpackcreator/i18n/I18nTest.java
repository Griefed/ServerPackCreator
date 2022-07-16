package de.griefed.serverpackcreator.i18n;

import de.griefed.serverpackcreator.ApplicationProperties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class I18nTest {

  private final Logger LOG = LogManager.getLogger(I18nTest.class);

  I18nTest() {
    FileUtils.deleteQuietly(new File("lang"));
  }

  @Test
  void newTest() {
    FileUtils.deleteQuietly(new File("lang"));
    I18n i18n = new I18n();
    Assertions.assertEquals("English (United States)", i18n.getMessage("localeUnlocalizedName"));
  }

  @Test
  void newLocaleTest() {
    FileUtils.deleteQuietly(new File("lang"));

    LOG.info("newLocaleTest() en_us");
    I18n i18n = new I18n(new ApplicationProperties(), "en_us");
    Assertions.assertEquals("English (United States)", i18n.getMessage("localeUnlocalizedName"));

    LOG.info("newLocaleTest() uk_ua");
    i18n = new I18n(new ApplicationProperties(), "uk_ua");
    Assertions.assertEquals("Ukrainian (Ukraine)", i18n.getMessage("localeUnlocalizedName"));

    LOG.info("newLocaleTest() de_de");
    i18n = new I18n(new ApplicationProperties(), "de_de");
    Assertions.assertEquals("German (Germany)", i18n.getMessage("localeUnlocalizedName"));
  }

  @Test
  void newPropertiesTest() {
    FileUtils.deleteQuietly(new File("lang"));

    LOG.info("newPropertiesTest() en_us");
    I18n i18n;
    ApplicationProperties applicationProperties = new ApplicationProperties();
    try (FileInputStream fileInputStream =
        new FileInputStream("backend/test/resources/testresources/languages/en_us.properties")) {
      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
      applicationProperties.load(bufferedReader);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    i18n = new I18n(applicationProperties);
    Assertions.assertEquals("English (United States)", i18n.getMessage("localeUnlocalizedName"));

    LOG.info("newPropertiesTest() de_de");
    try (FileInputStream fileInputStream =
        new FileInputStream("backend/test/resources/testresources/languages/de_de.properties")) {
      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
      applicationProperties.load(bufferedReader);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    i18n = new I18n(applicationProperties);
    Assertions.assertEquals("German (Germany)", i18n.getMessage("localeUnlocalizedName"));

    LOG.info("newPropertiesTest() uk_ua");
    try (FileInputStream fileInputStream =
        new FileInputStream("backend/test/resources/testresources/languages/uk_ua.properties")) {
      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
      applicationProperties.load(bufferedReader);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    i18n = new I18n(applicationProperties);
    Assertions.assertEquals("Ukrainian (Ukraine)", i18n.getMessage("localeUnlocalizedName"));
  }

  @Test
  void getLocalizedStringTest() {
    try {
      FileUtils.copyDirectory(
          new File("backend/test/resources/testresources/languages/langMissing"), new File("lang"));
    } catch (Exception ignored) {
    }

    LOG.info("getLocalizedStringTest() en_us");
    I18n i18n = new I18n(new ApplicationProperties(), "en_us");
    Assertions.assertEquals("English (United States)", i18n.getMessage("localeUnlocalizedName"));

    LOG.info("getLocalizedStringTest() uk_ua");
    i18n = new I18n(new ApplicationProperties(), "uk_ua");
    Assertions.assertEquals("Ukrainian (Ukraine)", i18n.getMessage("localeUnlocalizedName"));

    LOG.info("getLocalizedStringTest() de_de");
    i18n = new I18n(new ApplicationProperties(), "de_de");
    Assertions.assertEquals("German (Germany)", i18n.getMessage("localeUnlocalizedName"));

    LOG.info("getLocalizedStringTest() ab_cd");
    i18n = new I18n(new ApplicationProperties(), "ab_cd");
    Assertions.assertEquals("English (United States)", i18n.getMessage("localeUnlocalizedName"));
  }

  @Test
  void customLanguageTest() {
    try {
      FileUtils.copyFile(
          new File(
              "backend/test/resources/testresources/languages/langMissing/lang_ef_gh.properties"),
          new File("lang/lang_ef_gh.properties"));
    } catch (Exception ignored) {
    }

    LOG.info("customLanguageTest() ef_gh");
    I18n i18n = new I18n(new ApplicationProperties(), "ef_gh");
    Assertions.assertEquals("I bims 1 Sprache", i18n.getMessage("localeUnlocalizedName"));
  }
}
