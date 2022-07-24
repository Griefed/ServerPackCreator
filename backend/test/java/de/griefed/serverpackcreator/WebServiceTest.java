package de.griefed.serverpackcreator;

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

  ServerPackCreator serverPackCreator;

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
    serverPackCreator = Dependencies.getInstance().SERVER_PACK_CREATOR();
    serverPackCreator.run(ServerPackCreator.CommandlineParser.Mode.SETUP);
    serverPackCreator.checkDatabase();
  }

  @Test
  void contextLoads() {}
}
