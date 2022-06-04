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
package de.griefed.serverpackcreator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Griefed
 */
@SpringBootApplication
@EnableScheduling
@PropertySources({
  @PropertySource("classpath:application.properties"),
  @PropertySource("classpath:serverpackcreator.properties")
})
public class WebService {

  /**
   * Start Spring Boot app, providing our Apache Tomcat and serving our frontend.
   *
   * @param args Arguments passed from invocation in {@link Main#main(String[])}.
   * @author Griefed
   */
  public static void main(String[] args) {
    SpringApplication.run(WebService.class, args);
  }
}
