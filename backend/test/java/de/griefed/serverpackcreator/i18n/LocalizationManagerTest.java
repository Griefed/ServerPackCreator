package de.griefed.serverpackcreator.i18n;

import de.griefed.serverpackcreator.ApplicationProperties;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class LocalizationManagerTest {

    private final Logger LOG = LogManager.getLogger(LocalizationManagerTest.class);

    LocalizationManagerTest() {
        FileUtils.deleteQuietly(new File("lang"));
    }

    @Test
    void newTest() {
        FileUtils.deleteQuietly(new File("lang"));

        LOG.info("newTest() en_us");
        LocalizationManager localizationManager = new LocalizationManager();
        Assertions.assertEquals("English (United States)",localizationManager.getLocalizedString("localeUnlocalizedName"));
    }

    @Test
    void newLocaleTest() {
        FileUtils.deleteQuietly(new File("lang"));

        LOG.info("newLocaleTest() en_us");
        LocalizationManager localizationManager = new LocalizationManager("en_us");
        Assertions.assertEquals("English (United States)",localizationManager.getLocalizedString("localeUnlocalizedName"));

        LOG.info("newLocaleTest() uk_ua");
        localizationManager = new LocalizationManager("uk_ua");
        Assertions.assertEquals("Ukrainian (Ukraine)",localizationManager.getLocalizedString("localeUnlocalizedName"));

        LOG.info("newLocaleTest() de_de");
        localizationManager = new LocalizationManager("de_de");
        Assertions.assertEquals("German (Germany)",localizationManager.getLocalizedString("localeUnlocalizedName"));
    }

    @Test
    void newPropertiesTest() {
        FileUtils.deleteQuietly(new File("lang"));

        LOG.info("newPropertiesTest() en_us");
        LocalizationManager localizationManager;
        ApplicationProperties applicationProperties = new ApplicationProperties();
        try (FileInputStream fileInputStream = new FileInputStream("backend/test/resources/testresources/languages/en_us.properties")){
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
            applicationProperties.load(bufferedReader);
        } catch (Exception ex) { ex.printStackTrace(); }
        localizationManager = new LocalizationManager(applicationProperties);
        Assertions.assertEquals("English (United States)",localizationManager.getLocalizedString("localeUnlocalizedName"));

        LOG.info("newPropertiesTest() de_de");
        try (FileInputStream fileInputStream = new FileInputStream("backend/test/resources/testresources/languages/de_de.properties")){
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
            applicationProperties.load(bufferedReader);
        } catch (Exception ex) { ex.printStackTrace(); }
        localizationManager = new LocalizationManager(applicationProperties);
        Assertions.assertEquals("German (Germany)",localizationManager.getLocalizedString("localeUnlocalizedName"));

        LOG.info("newPropertiesTest() uk_ua");
        try (FileInputStream fileInputStream = new FileInputStream("backend/test/resources/testresources/languages/uk_ua.properties")){
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
            applicationProperties.load(bufferedReader);
        } catch (Exception ex) { ex.printStackTrace(); }
        localizationManager = new LocalizationManager(applicationProperties);
        Assertions.assertEquals("Ukrainian (Ukraine)",localizationManager.getLocalizedString("localeUnlocalizedName"));
    }

    @Test
    void getLocalizedStringTest() throws IOException {
        FileUtils.copyDirectory(new File("backend/test/resources/testresources/languages/langMissing"), new File("lang"));

        LOG.info("getLocalizedStringTest() en_us");
        LocalizationManager localizationManager = new LocalizationManager("en_us");
        Assertions.assertEquals("English (United States)",localizationManager.getLocalizedString("localeUnlocalizedName"));

        LOG.info("getLocalizedStringTest() uk_ua");
        localizationManager = new LocalizationManager("uk_ua");
        Assertions.assertEquals("Ukrainian (Ukraine)",localizationManager.getLocalizedString("localeUnlocalizedName"));

        LOG.info("getLocalizedStringTest() de_de");
        localizationManager = new LocalizationManager("de_de");
        Assertions.assertEquals("German (Germany)",localizationManager.getLocalizedString("localeUnlocalizedName"));

        LOG.info("getLocalizedStringTest() ab_cd");
        localizationManager = new LocalizationManager("ab_cd");
        Assertions.assertEquals("English (United States)",localizationManager.getLocalizedString("localeUnlocalizedName"));
    }

    @Test
    void customLanguageTest() throws IOException {
        FileUtils.copyFile(new File("backend/test/resources/testresources/languages/langMissing/lang_ef_gh.properties"), new File("lang/lang_ef_gh.properties"));

        LOG.info("customLanguageTest() ef_gh");
        LocalizationManager localizationManager = new LocalizationManager("ef_gh");
        Assertions.assertEquals("I bims 1 Sprache",localizationManager.getLocalizedString("localeUnlocalizedName"));
    }

}
