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
package de.griefed.serverpackcreator.web

import de.griefed.serverpackcreator.api.ApiWrapper
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableConfigurationProperties
@EntityScan(value = ["de.griefed.serverpackcreator.web"])
@EnableScheduling
class WebService(private val api: ApiWrapper) {
    private val log = cachedLoggerOf(this.javaClass)

    fun start(args: Array<String>): ConfigurableApplicationContext {
        val lastIndex = "--spring.config.location=classpath:/application.properties," +
                "classpath:/serverpackcreator.properties," +
                "optional:file:./serverpackcreator.properties," +
                "optional:file:${api.apiProperties.serverPackCreatorPropertiesFile.absolutePath}"
        val springArgs = if (args.isEmpty()) {
            arrayOf(lastIndex)
        } else {
            val temp = args.toList().toTypedArray()
            temp[temp.lastIndex] = lastIndex
            temp.toList().toTypedArray()
        }
        log.debug("Running webservice with args:${springArgs.contentToString()}")
        log.debug("Application name: ${getSpringBootApplicationContext(springArgs).applicationName}")
        log.debug("Property sources:")
        for (property in getSpringBootApplicationContext().environment.propertySources) {
            log.debug("    ${property.name}: ${property.source}")
        }
        log.debug("System properties:")
        for ((key, value) in getSpringBootApplicationContext().environment.systemProperties) {
            log.debug("    Key: $key - Value: $value")
        }
        log.debug("System environment:")
        for ((key, value) in getSpringBootApplicationContext().environment.systemEnvironment) {
            log.debug("    Key: $key - Value: $value")
        }
        return getSpringBootApplicationContext()
    }

    companion object {
        @Volatile
        private var springBootApplicationContext: ConfigurableApplicationContext? = null

        /**
         * This instances application context when running as a webservice. When no instance of the Spring
         * Boot application context is available yet, it will be created and the Spring Boot application
         * will be started with the given arguments.
         *
         * @param args CLI arguments to pass to Spring Boot when it has not yet been started.
         * @return Application context of Spring Boot.
         * @author Griefed
         */
        @Synchronized
        fun getSpringBootApplicationContext(args: Array<String> = arrayOf()): ConfigurableApplicationContext {
            if (springBootApplicationContext == null) {
                synchronized(this) {
                    if (springBootApplicationContext == null) {
                        springBootApplicationContext = SpringApplication.run(WebService::class.java, *args)
                    }
                }
            }
            return springBootApplicationContext!!
        }
    }
}

fun main(args: Array<String>) {
    WebService(ApiWrapper.api()).start(args)
}