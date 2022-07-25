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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <code>fml-cache-annotation.json</code>-based scanning of Forge-Minecraft mods of older versions.
 *
 * @author Griefed
 */
@Component
public class AnnotationScanner implements Scanner<TreeSet<File>, Collection<File>> {

  private static final Logger LOG = LogManager.getLogger(AnnotationScanner.class);
  private final ObjectMapper OBJECT_MAPPER;

  @Autowired
  public AnnotationScanner(ObjectMapper objectMapper) {
    this.OBJECT_MAPPER = objectMapper;
  }

  /**
   * Scan the <code>fml-cache-annotation.json</code>-files in mod JAR-files of a given directory for
   * their sideness.<br>
   * If <code>clientSideOnly</code> specifies <code>"value": "true"</code>, and is not listed as a
   * dependency for another mod, it is added and therefore later on excluded from the server pack.
   *
   * @param filesInModsDir A list of files in which to check the <code>fml-cache-annotation.json
   *                       </code>-files.
   * @return List String. List of mods not to include in server pack based on
   *     fml-cache-annotation.json-content.
   * @author Griefed
   */
  public TreeSet<File> scan(Collection<File> filesInModsDir) {

    LOG.info("Scanning Minecraft 1.12.x and older mods for sideness...");

    TreeSet<String> modDependencies = new TreeSet<>();
    TreeSet<String> clientMods = new TreeSet<>();

    for (File mod : filesInModsDir) {
      if (mod.toString().endsWith("jar")) {

        String modId = null;
        TreeSet<String> additionalMods = new TreeSet<>();

        JarFile jarFile = null;
        JarEntry jarEntry;
        InputStream inputStream = null;

        try {
          jarFile = new JarFile(mod);
          jarEntry = jarFile.getJarEntry("META-INF/fml_cache_annotation.json");
          inputStream = jarFile.getInputStream(jarEntry);
        } catch (Exception ex) {
          LOG.error("Can not scan " + mod);
        }
        // TODO extract method for json acquisition

        try {

          if (inputStream != null) {

            JsonNode modJson = OBJECT_MAPPER.readTree(inputStream);

            // base of json
            for (JsonNode node : modJson) {

              try {
                // iterate though annotations
                for (JsonNode child : node.get("annotations")) {

                  // Get the mod ID and check for clientside only, if we have not yet received a
                  // modID
                  if (modId == null) {

                    try {
                      // Get the modId
                      if (!child.get("values").get("modid").get("value").asText().isEmpty()) {
                        modId = child.get("values").get("modid").get("value").asText();
                      }

                      // Add mod to list of clientmods if clientSideOnly is true
                      if (child
                          .get("values")
                          .get("clientSideOnly")
                          .get("value")
                          .asText()
                          .equalsIgnoreCase("true")) {

                        clientMods.add(modId);
                        LOG.debug("Added clientMod: " + modId);
                      }
                    } catch (NullPointerException ignored) {

                    }

                    // We already received a modId, perform additional checks to prevent false
                    // positives
                  } else {

                    try {
                      // Get the second modID
                      if (!child.get("values").get("modid").get("value").asText().isEmpty()) {

                        // ModIDs are the same, so check for clientside-only
                        if (modId.equals(child.get("values").get("modid").get("value").asText())) {

                          try {
                            // Add mod to list of clientmods if clientSideOnly is true
                            if (child
                                .get("values")
                                .get("clientSideOnly")
                                .get("value")
                                .asText()
                                .equalsIgnoreCase("true")) {

                              clientMods.add(modId);
                              LOG.debug("Added clientMod: " + modId);
                            }
                          } catch (NullPointerException ignored) {

                          }

                          // ModIDs are different, possibly two mods in one JAR-file.......
                        } else {

                          // Add additional modId to list, so we can check those later
                          additionalMods.add(
                              child.get("values").get("modid").get("value").asText());
                        }
                      }

                    } catch (NullPointerException ignored) {

                    }
                  }

                  // Get dependency modIds
                  try {
                    if (!child.get("values").get("dependencies").get("value").asText().isEmpty()) {

                      if (child
                          .get("values")
                          .get("dependencies")
                          .get("value")
                          .asText()
                          .contains(";")) {

                        String[] dependencies =
                            child
                                .get("values")
                                .get("dependencies")
                                .get("value")
                                .asText()
                                .split(";");

                        for (String dependency : dependencies) {

                          if (dependency.matches("(before:.*|after:.*|required-after:.*|)")) {

                            dependency =
                                dependency
                                    .substring(dependency.lastIndexOf(":") + 1)
                                    .replaceAll("(@.*|\\[.*)", "");

                            if (!dependency.equalsIgnoreCase("forge") && !dependency.equals("*")) {
                              modDependencies.add(dependency);

                              LOG.debug("Added dependency " + dependency);
                            }
                          }
                        }
                      } else {
                        if (child
                            .get("values")
                            .get("dependencies")
                            .get("value")
                            .asText()
                            .matches("(before:.*|after:.*|required-after:.*|)")) {

                          String dependency =
                              child
                                  .get("values")
                                  .get("dependencies")
                                  .get("value")
                                  .asText()
                                  .substring(
                                      child
                                              .get("values")
                                              .get("dependencies")
                                              .get("value")
                                              .asText()
                                              .lastIndexOf(":")
                                          + 1)
                                  .replaceAll("(@.*|\\[.*)", "");

                          if (!dependency.equalsIgnoreCase("forge") && !dependency.equals("*")) {
                            modDependencies.add(dependency);

                            LOG.debug("Added dependency " + dependency);
                          }
                        }
                      }
                    }
                  } catch (NullPointerException ignored) {

                  }
                }

              } catch (NullPointerException ignored) {

              }
            }

            if (!additionalMods.isEmpty()) {
              for (String additionalModId : additionalMods) {

                // base of json
                for (JsonNode node : modJson) {

                  try {
                    // iterate though annotations again but this time for the modID of the
                    // second
                    // mod
                    for (JsonNode child : node.get("annotations")) {
                      boolean additionalModDependsOnFirst = false;

                      // check if second mod depends on first
                      try {
                        // if the modId is that of our additional mod, check the dependencies
                        // whether the first modId is present
                        if (child
                            .get("values")
                            .get("modid")
                            .get("value")
                            .asText()
                            .equals(additionalModId)) {
                          if (!child
                              .get("values")
                              .get("dependencies")
                              .get("value")
                              .asText()
                              .isEmpty()) {

                            if (child
                                .get("values")
                                .get("dependencies")
                                .get("value")
                                .asText()
                                .contains(";")) {

                              String[] dependencies =
                                  child
                                      .get("values")
                                      .get("dependencies")
                                      .get("value")
                                      .asText()
                                      .split(";");

                              for (String dependency : dependencies) {

                                if (dependency.matches("(before:.*|after:.*|required-after:.*|)")) {

                                  dependency =
                                      dependency
                                          .substring(dependency.lastIndexOf(":") + 1)
                                          .replaceAll("(@.*|\\[.*)", "");

                                  if (dependency.equals(modId)) {
                                    additionalModDependsOnFirst = true;
                                  }
                                }
                              }
                            } else {
                              if (child
                                  .get("values")
                                  .get("dependencies")
                                  .get("value")
                                  .asText()
                                  .matches("(before:.*|after:.*|required-after:.*|)")) {

                                String dependency =
                                    child
                                        .get("values")
                                        .get("dependencies")
                                        .get("value")
                                        .asText()
                                        .substring(
                                            child
                                                    .get("values")
                                                    .get("dependencies")
                                                    .get("value")
                                                    .asText()
                                                    .lastIndexOf(":")
                                                + 1)
                                        .replaceAll("(@.*|\\[.*)", "");

                                if (dependency.equals(modId)) {
                                  additionalModDependsOnFirst = true;
                                }
                              }
                            }
                          }
                        }

                      } catch (NullPointerException ignored) {

                      }

                      // If the additional mod depends on the first one, check if the additional one
                      // is clientside-only
                      if (additionalModDependsOnFirst) {

                        boolean clientSide = false;

                        try {
                          // iterate though annotations
                          for (JsonNode children : node.get("annotations")) {

                            try {
                              if (children
                                      .get("values")
                                      .get("modid")
                                      .get("value")
                                      .asText()
                                      .equals(additionalModId)
                                  && children
                                      .get("values")
                                      .get("clientSideOnly")
                                      .get("value")
                                      .asText()
                                      .equalsIgnoreCase("true")) {

                                clientSide = true;
                              }
                            } catch (NullPointerException ignored) {

                            }
                          }
                        } catch (NullPointerException ignored) {

                        }

                        // if the additional mod is NOT clientside-only, we have to remove this mod
                        // from the list of clientside-only mods
                        if (!clientSide) {
                          String finalModId = modId;
                          if (clientMods.removeIf(n -> n.equals(finalModId))) {
                            LOG.info(
                                "Removing "
                                    + modId
                                    + " from list of clientside-only mods. It contains multiple mods at once, and one of them is NOT clientside-only.");
                          }
                        }
                      }
                    }
                  } catch (NullPointerException ignored) {

                  }
                }
              }
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
            inputStream.close();
          } catch (Exception ignored) {

          }
        }
      }
    }

    // Remove dependencies from list of clientmods to ensure we do not, well, exclude a dependency
    // of another mod.
    for (String dependency : modDependencies) {

      if (clientMods.removeIf(n -> (n.contains(dependency)))) {
        LOG.debug(
            "Removing "
                + dependency
                + " from list of clientmods as it is a dependency for another mod.");
      }
    }

    TreeSet<File> modsDelta = new TreeSet<>();

    // After removing dependencies from the list of potential clientside mods, we can remove any mod
    // that says it is clientside-only.
    for (File mod : filesInModsDir) {

      String modIdTocheck = null;
      boolean addToDelta = false;

      JarFile jarFile = null;
      JarEntry jarEntry;
      InputStream inputStream = null;

      try {
        jarFile = new JarFile(mod);
        jarEntry = jarFile.getJarEntry("META-INF/fml_cache_annotation.json");
        inputStream = jarFile.getInputStream(jarEntry);
      } catch (Exception ex) {
        LOG.error("Can not scan " + mod);
      }

      try {

        if (inputStream != null) {

          JsonNode modJson = OBJECT_MAPPER.readTree(inputStream);

          // base of json
          for (JsonNode node : modJson) {

            try {
              // iterate though annotations
              for (JsonNode child : node.get("annotations")) {

                // Get the modId
                try {
                  if (!child.get("values").get("modid").get("value").asText().isEmpty()) {
                    modIdTocheck = child.get("values").get("modid").get("value").asText();
                  }
                } catch (NullPointerException ignored) {

                }

                // Add mod to list of clientmods if clientSideOnly is true
                try {
                  if (child
                      .get("values")
                      .get("clientSideOnly")
                      .get("value")
                      .asText()
                      .equalsIgnoreCase("true")) {
                    if (clientMods.contains(modIdTocheck)) {
                      addToDelta = true;
                    }
                  }
                } catch (NullPointerException ignored) {

                }
              }
            } catch (NullPointerException ignored) {

            }
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
