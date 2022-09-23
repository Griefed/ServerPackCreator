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
package de.griefed.serverpackcreator.versionmeta.forge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.versionmeta.ManifestParser;
import de.griefed.serverpackcreator.versionmeta.minecraft.MinecraftMeta;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Information about available Forge loader versions in correlation to Minecraft versions.
 *
 * @author Griefed
 */
final class ForgeLoader extends ManifestParser {

  private static final Logger LOG = LogManager.getLogger(ForgeLoader.class);
  private final ObjectMapper MAPPER;
  private final MinecraftMeta MINECRAFT_META;
  private final List<String> minecraftVersions = new ArrayList<>(100);
  private final List<String> forgeVersions = new ArrayList<>(100);
  private final File MANIFEST;

  /**
   * 1-n Minecraft version to Forge versions.<br> {@code key}: Minecraft version.<br> {@code value}:
   * List of Forge versions for said Minecraft versions.
   */
  private HashMap<String, List<String>> versionMeta;

  /**
   * 1-1 Forge version to Minecraft version<br> {@code key}: Forge version.<br> {@code value}:
   * Minecraft version for said Forge version.
   */
  private HashMap<String, String> forgeToMinecraftMeta;

  /**
   * 1-1 Minecraft + Forge version combination to {@link ForgeInstance}<br> {@code key}: Minecraft
   * version + Forge version. Example: {@code 1.18.2-40.0.44}<br> {@code value}: The
   * {@link ForgeInstance} for said Minecraft and Forge version combination.
   */
  private HashMap<String, ForgeInstance> instanceMeta;

  /**
   * Create a new instance of the Forge Loader.
   *
   * @param forgemanifest         Node containing information about available Forge versions.
   * @param injectedMinecraftMeta Meta for retroactively updating the previously passed meta.
   * @author Griefed
   */
  ForgeLoader(File forgemanifest, ObjectMapper mapper, MinecraftMeta injectedMinecraftMeta) {
    MANIFEST = forgemanifest;
    MAPPER = mapper;
    MINECRAFT_META = injectedMinecraftMeta;
  }

  /**
   * Update the available Forge loader information.
   *
   * @author Griefed
   */
  void update() throws IOException {

    minecraftVersions.clear();
    forgeVersions.clear();
    versionMeta = new HashMap<>(200);
    forgeToMinecraftMeta = new HashMap<>(200);
    instanceMeta = new HashMap<>(200);

    JsonNode forgeManifest = getJson(MANIFEST, MAPPER);
    forgeManifest
        .fieldNames()
        .forEachRemaining(
            field -> {

              /*
               * A field, which represents a supported Minecraft version from the Forge manifest, does NOT necessarily exist
               * in Mojang's Minecraft manifest.
               * Examples:
               *   Forge Manifest Minecraft version: 1.7.10_pre4
               *   Minecraft Manifest version:       1.7.10-pre4
               * So, if we want to acquire a Forge instance for 1.7.10-pre4, it would fail.
               * When retrieving a Forge instance with a Minecraft version from the MinecraftMeta, we need to check for
               * 1.7.10_pre4 AND 1.7.10-pre4.
               */
              String mcVersion;
              if (MINECRAFT_META.getClient(field.replace("_", "-")).isPresent()) {
                mcVersion = field.replace("_", "-");
                minecraftVersions.add(field.replace("_", "-"));
              } else {
                mcVersion = field;
                minecraftVersions.add(field);
              }

              List<String> forgeVersionsForMCVer = new ArrayList<>(100);

              forgeManifest
                  .get(field)
                  .forEach(
                      forge -> {

                        /*
                         * substring of length of Minecraft version plus 1, so entries like "1.18.2-40.0.17" get their
                         * Minecraft version portion removed and result in "40.0.17". The +1 removes the "-", too. :)
                         */
                        forgeVersions.add(forge.asText().substring(mcVersion.length() + 1));
                        forgeVersionsForMCVer.add(forge.asText().substring(mcVersion.length() + 1));

                        try {

                          ForgeInstance forgeInstance =
                              new ForgeInstance(
                                  mcVersion,
                                  forge.asText().substring(mcVersion.length() + 1),
                                  MINECRAFT_META);

                          instanceMeta.put(
                              mcVersion + forge.asText().substring(mcVersion.length()),
                              forgeInstance);

                          forgeToMinecraftMeta.put(
                              forge.asText().substring(mcVersion.length() + 1), mcVersion);

                        } catch (MalformedURLException | NoSuchElementException ex) {

                          // Well, in THEORY this should never be throws, so we don't need to bother
                          // with a thorough error message
                          LOG.debug(
                              "Could not create Forge instance for Minecraft "
                                  + mcVersion
                                  + " and Forge "
                                  + forge.asText().substring(mcVersion.length() + 1),
                              ex);
                        }
                      });

              versionMeta.put(mcVersion, forgeVersionsForMCVer);
            });
  }

  /**
   * Get a list of available Minecraft versions for Forge.
   *
   * @return List of the available Minecraft versions for Forge.
   * @author Griefed
   */
  List<String> minecraftVersions() {
    return minecraftVersions;
  }

  /**
   * Get a list of available Forge versions.
   *
   * @return List of the available Forge versions.
   * @author Griefed
   */
  List<String> forgeVersions() {
    return forgeVersions;
  }

  /**
   * Get the version-meta.
   * <br>key: Minecraft version
   * <br> value: List of Forge versions available for the given Minecraft version.
   *
   * @return Map containing the version meta.
   * @author Griefed
   */
  HashMap<String, List<String>> versionMeta() {
    return versionMeta;
  }

  /**
   * Get the Forge version to Minecraft version meta.<br> key: {@link String} Forge version.<br>
   * version: {@link String} Minecraft version for the given Forge version.
   *
   * @return Map with Forge-to-Minecraft-version mappings.
   * @author Griefed
   */
  HashMap<String, String> forgeToMinecraftMeta() {
    return forgeToMinecraftMeta;
  }

  /**
   * Get the Minecraft-Forge-version meta.<br> key: {@link String} Minecraft version + Forge version
   * concatenation.<br> value: {@link ForgeInstance} for the given Minecraft version + Forge version
   * concatenation.
   *
   * @return Map with Minecraft-Forge-version-to-ForgeInstance mapping.
   * @author Griefed
   */
  HashMap<String, ForgeInstance> instanceMeta() {
    return instanceMeta;
  }
}
