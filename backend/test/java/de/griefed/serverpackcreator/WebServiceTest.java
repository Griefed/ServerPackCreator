package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.I18n;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@SpringBootTest(classes = WebServiceTest.class)
@PropertySources({@PropertySource("classpath:serverpackcreator.properties")})
public class WebServiceTest {

  private final ServerPackCreator SERVERPACKCREATOR;
  private final I18n I18N;
  private final ApplicationProperties APPLICATIONPROPERTIES;

  WebServiceTest() throws IOException {
    try {
      FileUtils.copyFile(
          new File("backend/main/resources/serverpackcreator.properties"),
          new File("serverpackcreator.properties"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      FileUtils.copyFile(
          new File("backend/test/resources/serverpackcreator.db"),
          new File("serverpackcreator.db"));
    } catch (IOException e) {
      e.printStackTrace();
    }

    this.APPLICATIONPROPERTIES = new ApplicationProperties();
    this.I18N = new I18n(APPLICATIONPROPERTIES);
    this.SERVERPACKCREATOR = new ServerPackCreator(new String[] {"--setup"});
    this.SERVERPACKCREATOR.run(ServerPackCreator.CommandlineParser.Mode.SETUP);
    this.SERVERPACKCREATOR.checkDatabase();
  }

  @Test
  void contextLoads() {}
}
