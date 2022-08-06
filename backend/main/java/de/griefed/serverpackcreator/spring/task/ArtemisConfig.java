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
package de.griefed.serverpackcreator.spring.task;

import de.griefed.serverpackcreator.ApplicationProperties;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConfigurationCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * <a
 * href="https://dev.to/gotson/how-to-implement-a-task-queue-using-apache-artemis-and-spring-boot-2mme">How
 * to implement a task queue using Apache Artemis and Spring Boot</a><br> Huge Thank You to <a
 * href="https://github.com/gotson">Gauthier</a> for writing the above guide on how to implement a
 * JMS. Without it this implementation of Artemis would have either taken way longer or never
 * happened at all. I managed to translate their Kotlin-code to Java and make the necessary changes
 * to fully implement it in ServerPackCreator.<br> Configuration for our Artemis JMS.
 *
 * @author Griefed
 */
@Configuration
public class ArtemisConfig implements ArtemisConfigurationCustomizer {

  private final ApplicationProperties APPLICATIONPROPERTIES;

  /**
   * Constructor responsible for our DI.
   *
   * @param injectedApplicationProperties Instance of {@link ApplicationProperties}.
   * @author Griefed
   */
  @Autowired
  public ArtemisConfig(ApplicationProperties injectedApplicationProperties) {
    this.APPLICATIONPROPERTIES = injectedApplicationProperties;
  }

  /**
   * Customize our configuration.<br> Set the default consumer windows size to 0.<br> Set the
   * maximum disk usage from our property <code>
   * de.griefed.serverpackcreator.spring.artemis.queue.max_disk_usage</code>.<br> Set the address to
   * <code>tasks.background</code>.<br> Set the queue configuration to
   * <code>tasks.background</code>.<br> Set the name to <code>tasks.background</code>.<br> Set the
   * last value key to <code>unique_id</code>.<br> Set the routing type to
   * {@link RoutingType#ANYCAST}.<br> Add our queue configuration..<br> All of this ensures that any
   * message added will be deduplicated and worked on one by one. No messages should, at any time,
   * be processed in parallel. Whilst working on them in parallel would increase the speed at which
   * multiple serer packs are generated, we want to make sure neither the CurseForge API, nor the
   * system our webservice is running on receives a heavy load. Economically speaking, we are trying
   * to be nice neighbours and not claim too many resources for ourselves.
   *
   * @param configuration Artemis configuration.
   * @author Griefed
   */
  @Override
  public void customize(org.apache.activemq.artemis.core.config.Configuration configuration) {

    if (configuration != null) {

      configuration.setMaxDiskUsage(APPLICATIONPROPERTIES.getQueueMaxDiskUsage());

      AddressSettings addressSettings = new AddressSettings();
      addressSettings.setDefaultConsumerWindowSize(0);
      configuration.addAddressesSetting("tasks.background", addressSettings);

      QueueConfiguration queueConfiguration = new QueueConfiguration("tasks.background");
      queueConfiguration.setAddress("tasks.background");
      queueConfiguration.setName("tasks.background");
      queueConfiguration.setLastValueKey("unique_id");
      queueConfiguration.setRoutingType(RoutingType.ANYCAST);

      configuration.addQueueConfiguration(queueConfiguration);
    }
  }
}
