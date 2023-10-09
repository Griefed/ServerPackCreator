/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.web.task

import de.griefed.serverpackcreator.api.ApiProperties
import org.apache.activemq.artemis.api.core.QueueConfiguration
import org.apache.activemq.artemis.api.core.RoutingType
import org.apache.activemq.artemis.core.settings.impl.AddressSettings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConfigurationCustomizer
import org.springframework.context.annotation.Configuration

/**
 * [How to implement a task queue using Apache Artemis and Spring Boot](https://dev.to/gotson/how-to-implement-a-task-queue-using-apache-artemis-and-spring-boot-2mme)
 *
 * Huge Thank You to [Gauthier](https://github.com/gotson) for writing the above guide on how to implement a JMS.
 * Without it this implementation of Artemis would have either taken way longer or never happened at all.
 * I managed to translate their Kotlin-code to Java and make the necessary changes to fully implement it in
 * ServerPackCreator.
 *
 * @author Griefed
 */
@Suppress("unused")
@Configuration
class ArtemisConfig @Autowired constructor(private val apiProperties: ApiProperties) :
    ArtemisConfigurationCustomizer {

    /**
     * Customize our configuration.<br></br> Set the default consumer windows size to 0.<br></br> Set the
     * maximum disk usage from our property
     * `de.griefed.serverpackcreator.spring.artemis.queue.max_disk_usage`.<br></br> Set the address
     * to `tasks.background`.<br></br> Set the queue configuration to `tasks.background`.<br></br>
     * Set the name to `tasks.background`.<br></br> Set the last value key to `unique_id`.<br></br>
     * Set the routing type to [RoutingType.ANYCAST].<br></br> Add our queue configuration..<br></br> All
     * of this ensures that any message added will be deduplicated and worked on one by one. No
     * messages should, at any time, be processed in parallel. Whilst working on them in parallel
     * would increase the speed at which multiple serer packs are generated, we want to make sure
     * neither the CurseForge API, nor the system our webservice is running on receives a heavy load.
     * Economically speaking, we are trying to be nice neighbours and not claim too many resources for
     * ourselves.
     *
     * @param configuration Artemis configuration.
     * @author Griefed
     */
    override fun customize(configuration: org.apache.activemq.artemis.core.config.Configuration) {
        configuration.maxDiskUsage = apiProperties.artemisQueueMaxDiskUsage
        val addressSettings = AddressSettings()
        addressSettings.defaultConsumerWindowSize = 0
        configuration.addAddressesSetting("tasks.background", addressSettings)
        val queueConfiguration = QueueConfiguration("tasks.background")
        queueConfiguration.setAddress("tasks.background")
        queueConfiguration.setName("tasks.background")
        queueConfiguration.setLastValueKey("unique_id")
        queueConfiguration.routingType = RoutingType.ANYCAST
        configuration.addQueueConfiguration(queueConfiguration)
    }
}