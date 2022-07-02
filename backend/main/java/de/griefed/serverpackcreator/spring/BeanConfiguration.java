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
package de.griefed.serverpackcreator.spring;

import de.griefed.serverpackcreator.ApplicationProperties;
import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean configuration for running ServerPackCreator as a webservice. This class provides beans for a
 * couple or properties which can not otherwise be provided.
 *
 * @author Groefed
 */
@Configuration
public class BeanConfiguration {

  private final ApplicationProperties APPLICATIONPROPERTIES;

  @Autowired
  public BeanConfiguration(ApplicationProperties applicationProperties) {
    this.APPLICATIONPROPERTIES = applicationProperties;
  }

  /**
   * Bean for the location of the Minecraft version manifest-file.
   *
   * @return {@link File}
   * @author Griefed
   */
  @Bean
  public File minecraftManifest() {
    return APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST_LOCATION();
  }

  /**
   * Bean for the location of the Forge version manifest-file.
   *
   * @return {@link File}
   * @author Griefed
   */
  @Bean
  public File forgeManifest() {
    return APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST_LOCATION();
  }

  /**
   * Bean for the location of the Fabric version manifest-file.
   *
   * @return {@link File}
   * @author Griefed
   */
  @Bean
  public File fabricManifest() {
    return APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST_LOCATION();
  }

  /**
   * Bean for the location of the Fabric installer version manifest-file.
   *
   * @return {@link File}
   * @author Griefed
   */
  @Bean
  public File fabricInstallerManifest() {
    return APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION();
  }

  /**
   * Bean for the location of the Quilt version manifest-file.
   *
   * @return {@link File}
   * @author Griefed
   */
  @Bean
  public File quiltManifest() {
    return APPLICATIONPROPERTIES.QUILT_VERSION_MANIFEST_LOCATION();
  }

  /**
   * Bean for the location of the Quilt installer version manifest-file.
   *
   * @return {@link File}
   * @author Griefed
   */
  @Bean
  public File quiltInstallerManifest() {
    return APPLICATIONPROPERTIES.QUILT_INSTALLER_VERSION_MANIFEST_LOCATION();
  }

  /**
   * Bean for starting up our Spring Boot Application, serving as our...<br>
   * <code>starts chanting</code><br>
   * <strong>public-static-void-main-string-args-public-static-void-main-string-args-public-static-void-main-string-args</strong>
   * <br>
   * <br>
   * ehem...<br>
   * Sorry 'bout that.
   *
   * @return empty String array.
   * @author Griefed
   */
  @Bean
  public String[] args() {
    return new String[0];
  }
}
