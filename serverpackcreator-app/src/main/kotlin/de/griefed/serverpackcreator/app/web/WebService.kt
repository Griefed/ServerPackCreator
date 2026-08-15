/* Copyright (C) 2026 Griefed
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
package de.griefed.serverpackcreator.app.web

import de.griefed.serverpackcreator.api.ApiWrapper
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.scheduling.annotation.EnableScheduling
import java.io.File

@SpringBootApplication
@EnableConfigurationProperties
@EntityScan(value = ["de.griefed.serverpackcreator.app"])
@EnableScheduling
class WebService(private val api: ApiWrapper) {

    fun start(args: Array<String>): ConfigurableApplicationContext {
        val configLocationArgument = configLocationArgument(
            api.apiProperties.serverPackCreatorPropertiesFile,
            api.apiProperties.overridesPropertiesFile,
            File(System.getProperty("user.home"))
        )
        val springArgs = springArguments(args, configLocationArgument)
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
        private val log by lazy { cachedLoggerOf(this.javaClass) }

        /**
         * Combines the applications own commandline arguments with the `--spring.config.location`
         * argument that tells Spring Boot which property-files to read, producing the array handed to
         * [SpringApplication.run]. Extracted from [start] so the composition can be tested without
         * booting a Spring context.
         */
        fun springArguments(args: Array<String>, configLocationArgument: String): Array<String> =
            args + configLocationArgument

        /**
         * The `--spring.config.location` argument: the eight property-file locations Spring reads, in
         * the order it reads them. **Later locations win**, so the two `overrides.properties` entries
         * come last on purpose — that is the file the docker image's `init-spc-config` script composes
         * `SPC_DATABASE_*` into, and therefore where `spring.data.mongodb.uri` arrives from in a
         * container deployment.
         *
         * Extracted from [start] for the same reason [springArguments] was: `start` hands the result
         * straight to Spring Boot, so the composition could not otherwise be asserted. A location that
         * silently goes missing here is a property-file that is never read, which for the database URI
         * is a hard startup failure and for everything else is a setting that quietly does nothing.
         *
         * @param propertiesFile   ServerPackCreator's own properties file, from its home directory.
         * @param overridesFile    The overrides file from that same home directory.
         * @param userHome         The user's home directory, which contributes two more locations.
         */
        fun configLocationArgument(propertiesFile: File, overridesFile: File, userHome: File): String =
            "--spring.config.location=classpath:/application.properties," +
                    "classpath:/serverpackcreator.properties," +
                    "optional:file:${propertiesFile.absolutePath}," +
                    "optional:file:${File(userHome, "serverpackcreator.properties").absolutePath}," +
                    "optional:file:./serverpackcreator.properties," +
                    "optional:file:${overridesFile.absolutePath}," +
                    "optional:file:${File(userHome, "overrides.properties").absolutePath}," +
                    "optional:file:./overrides.properties"

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
                        log.debug("Running webservice with ars: ${args.joinToString(" ")}")
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