package de.griefed.serverpackcreator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@SpringBootTest(classes = WebServiceTest.class)
@PropertySources({
    @PropertySource("classpath:serverpackcreator.properties"),
    @PropertySource("file:./backend/test/resources/serverpackcreator.properties")
})
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
