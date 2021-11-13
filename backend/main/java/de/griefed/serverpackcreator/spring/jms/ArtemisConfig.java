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
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConfigurationCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * https://dev.to/gotson/how-to-implement-a-task-queue-using-apache-artemis-and-spring-boot-2mme
 * @author Griefed
 */
@Configuration
public class ArtemisConfig implements ArtemisConfigurationCustomizer {

    private final ApplicationProperties APPLICATIONPROPERTIES;

    @Autowired
    public ArtemisConfig(ApplicationProperties injectedApplicationProperties) {
        this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    }

    /**
     *
     * @author Griefed
     * @param configuration
     */
    @Override
    public void customize(org.apache.activemq.artemis.core.config.Configuration configuration) {

        if (configuration != null) {

            configuration.setMaxDiskUsage(APPLICATIONPROPERTIES.QUEUE_MAX_DISK_USAGE);

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
