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
import de.griefed.serverpackcreator.versionmeta.minecraft.MinecraftMeta;
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
public class ForgeLoader {

  private static final Logger LOG = LogManager.getLogger(ForgeLoader.class);

  private final MinecraftMeta MINECRAFT_META;
  private final List<String> minecraftVersions = new ArrayList<>();
  private final List<String> forgeVersions = new ArrayList<>();

  /**
   * 1-n Minecraft version to Forge versions.<br>
   * <code>key</code>: Minecraft version.<br>
   * <code>value</code>: List of Forge versions for said Minecraft versions.
   */
  private HashMap<String, List<String>> versionMeta;

  /**
   * 1-1 Forge version to Minecraft version<br>
   * <code>key</code>: Forge version.<br>
   * <code>value</code>: Minecraft version for said Forge version.
   */
  private HashMap<String, String> forgeToMinecraftMeta;

  /**
   * 1-1 Minecraft + Forge version combination to {@link ForgeInstance}<br>
   * <code>key</code>: Minecraft version + Forge version. Example: <code>1.18.2-40.0.44</code><br>
   * <code>value</code>: The {@link ForgeInstance} for said Minecraft and Forge version
   * combination.
   */
  private HashMap<String, ForgeInstance> instanceMeta;

  /**
   * Create a new instance of the Forge Loader.
   *
   * @param forgemanifest         {@link JsonNode} containing information about available Forge
   *                              versions.
   * @param injectedMinecraftMeta {@link MinecraftMeta} for retroactively updating the previously
   *                              passed meta.
   * @author Griefed
   */
  protected ForgeLoader(JsonNode forgemanifest, MinecraftMeta injectedMinecraftMeta) {
    this.MINECRAFT_META = injectedMinecraftMeta;
    update(forgemanifest);
  }

  /**
   * Update the available Forge loader information.
   *
   * @param forgeManifest {@link JsonNode} containing information about available Forge versions.
   * @author Griefed
   */
  protected void update(JsonNode forgeManifest) {

    minecraftVersions.clear();
    forgeVersions.clear();
    versionMeta = new HashMap<>();
    forgeToMinecraftMeta = new HashMap<>();
    instanceMeta = new HashMap<>();

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

              List<String> forgeVersionsForMCVer = new ArrayList<>();

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
   * @return {@link String}-list of the available Minecraft versions for Forge.
   * @author Griefed
   */
  protected List<String> minecraftVersions() {
    return minecraftVersions;
  }

  /**
   * Get a list of available Forge versions.
   *
   * @return {@link String}-list of the available Forge versions.
   * @author Griefed
   */
  protected List<String> forgeVersions() {
    return forgeVersions;
  }

  /**
   * Get the {@link ForgeLoader} version-meta.<br> key: {@link String} Minecraft version<br> value:
   * {@link String}-list of Forge versions available for the given Minecraft version.
   *
   * @return {@link HashMap} containing the version meta.
   * @author Griefed
   */
  protected HashMap<String, List<String>> versionMeta() {
    return versionMeta;
  }

  /**
   * Get the Forge version to Minecraft version meta.<br> key: {@link String} Forge version.<br>
   * version: {@link String} Minecraft version for the given Forge version.
   *
   * @return {@link HashMap} with Forge-to-Minecraft-version mappings.
   * @author Griefed
   */
  protected HashMap<String, String> forgeToMinecraftMeta() {
    return forgeToMinecraftMeta;
  }

  /**
   * Get the Minecraft-Forge-version meta.<br> key: {@link String} Minecraft version + Forge version
   * concatenation.<br> value: {@link ForgeInstance} for the given Minecraft version + Forge version
   * concatenation.
   *
   * @return {@link HashMap} with Minecraft-Forge-version-to-ForgeInstance mapping.
   * @author Griefed
   */
  protected HashMap<String, ForgeInstance> instanceMeta() {
    return instanceMeta;
  }
}
