package de.griefed.serverpackcreator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = WebServiceTest.class)
public class WebServiceTest {

  String[] args = new String[]{"--setup", "backend/test/resources/serverpackcreator.properties"};

  WebServiceTest() {
    ServerPackCreator.getInstance(
                         args)
                     .checkDatabase();
  }

  @Test
  void contextLoads() {
  }
}
