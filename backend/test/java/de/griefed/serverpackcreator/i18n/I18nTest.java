package de.griefed.serverpackcreator.i18n;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ServerPackCreator;
import de.griefed.serverpackcreator.utilities.common.FileUtilities;
import de.griefed.serverpackcreator.utilities.common.JarUtilities;
import de.griefed.serverpackcreator.utilities.common.ListUtilities;
import de.griefed.serverpackcreator.utilities.common.SystemUtilities;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class I18nTest {

  private final Logger LOG = LogManager.getLogger(I18nTest.class);
  String[] args = new String[]{"--setup", "backend/test/resources/serverpackcreator.properties"};

  I18nTest() {

  }

  @Test
  void localeTest() {
    ApplicationProperties applicationProperties = ServerPackCreator.getInstance(
            args)
        .getApplicationProperties();

    I18n i18n = new I18n(applicationProperties);
    Assertions.assertEquals("English (United States)", i18n.getMessage("localeUnlocalizedName"));

    LOG.info("localeTest() en_us");
    i18n = new I18n(applicationProperties, "en_us");
    Assertions.assertEquals("English (United States)", i18n.getMessage("localeUnlocalizedName"));

    LOG.info("localeTest() uk_ua");
    i18n = new I18n(applicationProperties, "uk_ua");
    Assertions.assertEquals("Ukrainian (Ukraine)", i18n.getMessage("localeUnlocalizedName"));

    LOG.info("localeTest() de_de");
    i18n = new I18n(applicationProperties, "de_de");
    Assertions.assertEquals("German (Germany)", i18n.getMessage("localeUnlocalizedName"));

    try {
      FileUtils.copyDirectory(
          new File("backend/test/resources/testresources/languages/langMissing"), new File("tests/lang"));
    } catch (Exception ignored) {
    }

    LOG.info("getLocalizedStringTest() ab_cd");
    i18n = new I18n(applicationProperties, "ab_cd");
    Assertions.assertEquals("English (United States)", i18n.getMessage("localeUnlocalizedName"));

    try {
      FileUtils.copyFile(
          new File(
              "backend/test/resources/testresources/languages/langMissing/lang_ef_gh.properties"),
          new File("tests/lang/lang_ef_gh.properties"));
    } catch (Exception ignored) {
    }

    LOG.info("customLanguageTest() ef_gh");
    i18n = new I18n(
        new ApplicationProperties(new FileUtilities(), new SystemUtilities(), new ListUtilities(),
            new JarUtilities()),
        "ef_gh");
    Assertions.assertEquals("I bims 1 Sprache", i18n.getMessage("localeUnlocalizedName"));
  }

}
