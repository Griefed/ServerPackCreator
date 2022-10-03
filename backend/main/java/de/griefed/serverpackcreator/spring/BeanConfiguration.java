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

import com.electronwill.nightconfig.toml.TomlParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
   * @return Minecraft version manifest file.
   * @author Griefed
   */
  @Bean
  public File minecraftManifest() {
    return APPLICATIONPROPERTIES.minecraftVersionManifest();
  }

  /**
   * Bean for the location of the Forge version manifest-file.
   *
   * @return Forge version manifest file.
   * @author Griefed
   */
  @Bean
  public File forgeManifest() {
    return APPLICATIONPROPERTIES.forgeVersionManifest();
  }

  /**
   * Bean for the location of the Fabric version manifest-file.
   *
   * @return Fabric version manifest file.
   * @author Griefed
   */
  @Bean
  public File fabricManifest() {
    return APPLICATIONPROPERTIES.fabricVersionManifest();
  }

  /**
   * Bean for the location of the Fabric intermediaries manifest-file.
   *
   * @return Fabric intermediaries manifest file.
   * @author Griefed
   */
  @Bean
  public File fabricIntermediariesManifest() {
    return APPLICATIONPROPERTIES.fabricIntermediariesManifest();
  }

  /**
   * Bean for the location of the Fabric installer version manifest-file.
   *
   * @return Fabric installer versions manifest file.
   * @author Griefed
   */
  @Bean
  public File fabricInstallerManifest() {
    return APPLICATIONPROPERTIES.fabricInstallerManifest();
  }

  /**
   * Bean for the location of the Quilt version manifest-file.
   *
   * @return Quilt versions manifest file.
   * @author Griefed
   */
  @Bean
  public File quiltManifest() {
    return APPLICATIONPROPERTIES.quiltVersionManifest();
  }

  /**
   * Bean for the location of the Quilt installer version manifest-file.
   *
   * @return Quilt installer versions manifest file.
   * @author Griefed
   */
  @Bean
  public File quiltInstallerManifest() {
    return APPLICATIONPROPERTIES.quiltInstallerManifest();
  }

  /**
   * Bean for starting up our Spring Boot Application, serving as our...<br>
   * {@code starts chanting}<br>
   * <strong>public-static-void-main-string-args-public-static-void-main-string-args-public-static-void-main-string-args</strong>
   * <br>
   * <br>
   * ehem...<br> Sorry 'bout that.
   *
   * @return empty String array.
   * @author Griefed
   */
  @Bean
  public String[] args() {
    return new String[0];
  }

  /**
   * {@link ObjectMapper}-bean for use in various JSON-related things. Disabled
   * {@code DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES}, enabled
   * {@code DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY}.
   *
   * @return Objectmapper for JSON parsing.
   * @author Griefed
   */
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());
  }

  /**
   * {@link TomlParser} for parsing {@code .toml}-files.
   *
   * @return Tomlparser for Toml parsing.
   * @author Griefed
   */
  @Bean
  public TomlParser tomlParser() {
    return new TomlParser();
  }

  /**
   * Storage location for Legacy Fabric Game version manifest file.
   *
   * @return ./work/legacy-fabric-game-manifest.json
   * @author Griefed
   */
  @Bean
  public File legacyFabricGameManifest() {
    return APPLICATIONPROPERTIES.legacyFabricGameManifest();
  }

  /**
   * Storage location for Legacy Fabric Loader version manifest file.
   *
   * @return ./work/legacy-fabric-loader-manifest.json
   * @author Griefed
   */
  @Bean
  public File legacyFabricLoaderManifest() {
    return APPLICATIONPROPERTIES.legacyFabricLoaderManifest();
  }

  /**
   * Storage location for Legacy Fabric Installer version manifest file.
   *
   * @return ./work/legacy-fabric-installer-manifest.json
   * @author Griefed
   */
  @Bean
  public File legacyFabricInstallerManifest() {
    return APPLICATIONPROPERTIES.legacyFabricInstallerManifest();
  }
}
