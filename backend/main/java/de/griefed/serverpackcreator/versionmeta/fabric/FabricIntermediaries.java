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
package de.griefed.serverpackcreator.versionmeta.fabric;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Intermediaries for Fabric.
 *
 * @author Griefed
 */
public class FabricIntermediaries {

  private final ObjectMapper OBJECT_MAPPER;
  private final File INTERMEDIARY_MANIFEST;
  private final HashMap<String, FabricIntermediary> INTERMEDIARIES = new HashMap<>();

  /**
   * Instantiate Fabric intermediaries.
   *
   * @param intermediaryManifest Fabric Intermediary manifest-file.
   * @param objectMapper         Object mapper for JSON parsing.
   * @throws IOException when the manifest could not be read.
   * @author Griefed
   */
  public FabricIntermediaries(File intermediaryManifest, ObjectMapper objectMapper)
      throws IOException {

    this.INTERMEDIARY_MANIFEST = intermediaryManifest;
    this.OBJECT_MAPPER = objectMapper;
    update();
  }

  /**
   * Update the intermediaries for Fabric.
   *
   * @throws IOException when the manifest could not be read.
   */
  protected void update() throws IOException {
    for (FabricIntermediary intermediary : listIntermediariesFromManifest()) {
      INTERMEDIARIES.put(intermediary.getVersion(), intermediary);
    }
  }

  /**
   * Get a list of intermediaries from the manifest.
   *
   * @return List of intermediaries.
   * @throws IOException when the manifest could not be read.
   * @author Griefed
   */
  private List<FabricIntermediary> listIntermediariesFromManifest() throws IOException {
    return OBJECT_MAPPER.readValue(
        INTERMEDIARY_MANIFEST, new TypeReference<List<FabricIntermediary>>() {
        });
  }

  /**
   * HashMap of available intermediaries.
   *
   * @return Map of available intermediaries.
   * @author Griefed
   */
  protected HashMap<String, FabricIntermediary> getIntermediaries() {
    return INTERMEDIARIES;
  }

  /**
   * Get a specific intermediary, wrapped in an {@link Optional}.
   *
   * @param minecraftVersion Minecraft version.
   * @return A specific intermediary, wrapped in an {@link Optional}.
   * @author Griefed
   */
  protected Optional<FabricIntermediary> getIntermediary(String minecraftVersion) {
    return Optional.ofNullable(INTERMEDIARIES.get(minecraftVersion));
  }
}
