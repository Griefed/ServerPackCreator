package de.griefed.serverpackcreator.spring.jms;

import de.griefed.serverpackcreator.ServerPackCreator;
import de.griefed.serverpackcreator.ServerPackCreator.Mode;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.JmsDestinationAccessor;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.xml.sax.SAXException;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ArtemisConfigTest {

  private static final Logger LOG = LogManager.getLogger(ArtemisConfigTest.class);
  private final JmsTemplate jmsTemplate;
  private final String QUEUE_UNIQUE_ID = "unique_id";
  private final String QUEUE_TASKS = "tasks.background";
  String[] args = new String[]{"--setup", "backend/test/resources/serverpackcreator.properties"};

  @Autowired
  ArtemisConfigTest(JmsTemplate injectedJmsTemplate)
      throws IOException, ParserConfigurationException, SAXException {
    ServerPackCreator.getInstance(args).run(Mode.SETUP);

    this.jmsTemplate = injectedJmsTemplate;
    this.jmsTemplate.setReceiveTimeout(JmsDestinationAccessor.RECEIVE_TIMEOUT_NO_WAIT);
  }

  @AfterEach
  void emptyQueue() {
    while (jmsTemplate.receive(QUEUE_TASKS) != null) {
      LOG.info("Emptying queue");
    }
  }

  @Test
  void noUniqueID() {
    for (int i = 1; i < 6; i++) {
      LOG.info("Sending message " + i);
      jmsTemplate.convertAndSend(QUEUE_TASKS, "message " + i);
    }

    int size =
        jmsTemplate.browse(
            QUEUE_TASKS, (session, browser) -> Collections.list(browser.getEnumeration()).size());

    String message = Objects.requireNonNull(jmsTemplate.receiveAndConvert(QUEUE_TASKS)).toString();

    Assertions.assertEquals(5, size);
    Assertions.assertEquals("message 1", message);
  }

  @Test
  void sameID() {
    for (int i = 1; i < 6; i++) {
      LOG.info("Sending message " + i);

      jmsTemplate.convertAndSend(
          QUEUE_TASKS,
          "message " + i,
          message -> {
            message.setStringProperty(QUEUE_UNIQUE_ID, "1");
            return message;
          });
    }

    int size =
        jmsTemplate.browse(
            QUEUE_TASKS, (session, browser) -> Collections.list(browser.getEnumeration()).size());

    String message = Objects.requireNonNull(jmsTemplate.receiveAndConvert(QUEUE_TASKS)).toString();

    Assertions.assertEquals(1, size);
    Assertions.assertEquals("message 5", message);
  }
}
