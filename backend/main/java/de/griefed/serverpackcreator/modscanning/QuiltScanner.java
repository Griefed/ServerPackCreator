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
package de.griefed.serverpackcreator.modscanning;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <code>quilt.mod.json</code>-based scanning of Fabric-Minecraft mods.
 *
 * @author Griefed
 */
@Component
public class QuiltScanner implements Scanner<TreeSet<File>, Collection<File>> {

  private static final Logger LOG = LogManager.getLogger(QuiltScanner.class);
  private final ObjectMapper OBJECT_MAPPER;

  @Autowired
  public QuiltScanner(ObjectMapper objectMapper) {
    this.OBJECT_MAPPER = objectMapper;
  }

  /**
   * Scan the <code>quilt.mod.json</code>-files in mod JAR-files of a given directory for their
   * sideness.<br>
   * If <code>minecraft.environment</code> specifies <code>client</code>, and is not listed as a
   * dependency for another mod, it is added and therefore later on excluded from the server pack.
   *
   * @param filesInModsDir A list of files in which to check the <code>fabric.mod.json</code>-files.
   * @return List String. List of mods not to include in server pack based on
   *     fabric.mod.json-content.
   * @author Griefed
   */
  public TreeSet<File> scan(Collection<File> filesInModsDir) {
    LOG.info("Scanning Quilt mods for sideness...");

    List<String> modDependencies = new ArrayList<>();
    List<String> clientMods = new ArrayList<>();

    for (File mod : filesInModsDir) {
      if (mod.toString().endsWith("jar")) {

        String modId;

        JarFile jarFile = null;
        JarEntry jarEntry;
        InputStream inputStream = null;

        try {
          jarFile = new JarFile(mod);
          jarEntry = jarFile.getJarEntry("quilt.mod.json");
          inputStream = jarFile.getInputStream(jarEntry);
        } catch (Exception ex) {
          LOG.error("Can not scan " + mod);
        }

        try {

          if (inputStream != null) {

            JsonNode modJson =
                OBJECT_MAPPER
                    .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
                    .readTree(inputStream);

            modId = modJson.get("quilt_loader").get("id").asText();

            // Get this mods id/name
            try {
              if (modJson.get("minecraft").get("environment").asText().equalsIgnoreCase("client")) {
                if (!clientMods.contains(modId)) {
                  clientMods.add(modId);

                  LOG.debug("Added clientMod: " + modId);
                }
              }
            } catch (NullPointerException ignored) {

            }

            // Get this mods dependencies
            try {

              for (JsonNode dependency : modJson.get("quilt_loader").get("depends")) {

                if (dependency.isContainerNode()) {
                  if (!modDependencies.contains(dependency.get("id").asText())) {
                    modDependencies.add(dependency.get("id").asText());
                  }
                } else {
                  if (!modDependencies.contains(dependency.asText())) {
                    modDependencies.add(dependency.asText());
                  }
                }
              }

            } catch (NullPointerException ignored) {

            }
          }

        } catch (IOException ex) {

          LOG.error("Couldn't acquire sideness for mod " + mod, ex);

        } finally {

          try {
            //noinspection ConstantConditions
            jarFile.close();
          } catch (Exception ignored) {

          }

          try {
            //noinspection ConstantConditions
            inputStream.close();
          } catch (Exception ignored) {

          }
        }
      }
    }

    // Remove dependencies from list of clientmods to ensure we do not, well, exclude a dependency
    // of another mod.
    for (String dependency : modDependencies) {

      clientMods.removeIf(n -> (n.contains(dependency)));
      LOG.debug(
          "Removing "
              + dependency
              + " from list of clientmods as it is a dependency for another mod.");
    }

    TreeSet<File> modsDelta = new TreeSet<>();

    // After removing dependencies from the list of potential clientside mods, we can remove any mod
    // that says it is clientside-only.
    for (File mod : filesInModsDir) {

      String modIdTocheck;

      boolean addToDelta = false;

      JarFile jarFile = null;
      JarEntry jarEntry;
      InputStream inputStream = null;

      try {
        jarFile = new JarFile(mod);
        jarEntry = jarFile.getJarEntry("quilt.mod.json");
        inputStream = jarFile.getInputStream(jarEntry);
      } catch (Exception ex) {
        LOG.error("Can not scan " + mod);
      }

      try {

        if (inputStream != null) {

          JsonNode modJson =
              OBJECT_MAPPER
                  .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
                  .readTree(inputStream);

          // Get the modId
          modIdTocheck = modJson.get("quilt_loader").get("id").asText();

          try {
            if (modJson.get("minecraft").get("environment").asText().equalsIgnoreCase("client")) {
              if (clientMods.contains(modIdTocheck)) {
                addToDelta = true;
              }
            }
          } catch (NullPointerException ignored) {

          }

          if (addToDelta) {
            modsDelta.add(mod);
          }
        }

      } catch (Exception ex) {

        LOG.error("Couldn't acquire modId for mod " + mod, ex);

      } finally {

        try {
          //noinspection ConstantConditions
          jarFile.close();
        } catch (Exception ignored) {

        }

        try {
          //noinspection ConstantConditions
          inputStream.close();
        } catch (Exception ignored) {

        }
      }
    }

    return modsDelta;
  }
}
