/* Copyright (C) 2021  Griefed
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
package de.griefed.serverpackcreator.spring.jms;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.DefaultFiles;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.commons.io.FileUtils;
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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ArtemisConfigTest {

    private static final Logger LOG = LogManager.getLogger(ArtemisConfigTest.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    private final String QUEUE_UNIQUE_ID = "unique_id";
    private final String QUEUE_TYPE = "type";
    private final String QUEUE_TASKS = "tasks.background";
    private final String QUEUE_TASKS_TYPE = "task";
    private final String QUEUE_TASKS_SELECTOR = "$QUEUE_TYPE = '$QUEUE_TASKS_TYPE'";
    private final DefaultFiles DEFAULTFILES;
    private final LocalizationManager LOCALIZATIONMANAGER;
    private ApplicationProperties serverPackCreatorProperties;

    ArtemisConfigTest() {
        try {
            FileUtils.copyFile(new File("backend/main/resources/serverpackcreator.properties"),new File("serverpackcreator.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.serverPackCreatorProperties = new ApplicationProperties();

        LOCALIZATIONMANAGER = new LocalizationManager(serverPackCreatorProperties);
        LOCALIZATIONMANAGER.init();
        DEFAULTFILES = new DefaultFiles(LOCALIZATIONMANAGER, serverPackCreatorProperties);
        DEFAULTFILES.filesSetup();

        jmsTemplate.setReceiveTimeout(JmsDestinationAccessor.RECEIVE_TIMEOUT_NO_WAIT);
    }

    @AfterEach
    void emptyQueue() {
        while(jmsTemplate.receive(QUEUE_TASKS) != null) {
            LOG.info("Emptying queue");
        }
    }

    @Test
    void noUniqueID() {
        for (int i = 1; i < 6; i++) {
            LOG.info("Sending message " + i);
            jmsTemplate.convertAndSend(QUEUE_TASKS, "message " + i);
        }

        @SuppressWarnings("ConstantConditions")
        int size = jmsTemplate.browse(QUEUE_TASKS, (session, browser) -> Collections.list(Objects.requireNonNull(browser).getEnumeration()).size());

        String message = Objects.requireNonNull(jmsTemplate.receiveAndConvert(QUEUE_TASKS)).toString();

        Assertions.assertEquals(5, size);
        Assertions.assertEquals("message 1", message);
    }

    @Test
    void sameID() {
        for (int i = 1; i < 6; i++) {
            LOG.info("Sending message " + i);

            jmsTemplate.convertAndSend(QUEUE_TASKS, "message " + i, message -> {
                message.setStringProperty(QUEUE_UNIQUE_ID, "1");
                return message;
            });
        }

        @SuppressWarnings("ConstantConditions")
        int size = jmsTemplate.browse(QUEUE_TASKS, (session, browser) -> Collections.list(Objects.requireNonNull(browser).getEnumeration()).size());

        String message = Objects.requireNonNull(jmsTemplate.receiveAndConvert(QUEUE_TASKS)).toString();

        Assertions.assertEquals(1, size);
        Assertions.assertEquals("message 5", message);
    }

}
