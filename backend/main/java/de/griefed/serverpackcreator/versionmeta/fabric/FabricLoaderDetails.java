package de.griefed.serverpackcreator.versionmeta.fabric;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * Acquire details for a Fabric loader of a given Minecraft and Fabric version.
 *
 * @author Griefed
 */
public class FabricLoaderDetails {

  private final ObjectMapper OBJECT_MAPPER;

  /**
   * @param objectMapper {@link ObjectMapper} for acquiring and parsing JSON.
   * @author Griefed
   */
  public FabricLoaderDetails(ObjectMapper objectMapper) {
    this.OBJECT_MAPPER = objectMapper;
  }

  /**
   * Get the details for a given Minecraft and Fabric version combination.
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @param modloaderVersion {@link String} Fabric version,
   * @return {@link FabricDetails} for a given Minecraft and Fabric version combination, wrappen in
   *     an {@link Optional}.
   * @author Griefed
   */
  public Optional<FabricDetails> getDetails(String minecraftVersion, String modloaderVersion) {
    try {
      return Optional.of(
          OBJECT_MAPPER.readValue(
              new URL(
                  "https://meta.fabricmc.net/v2/versions/loader/"
                      + minecraftVersion
                      + "/"
                      + modloaderVersion
                      + "/server/json"),
              FabricDetails.class));

    } catch (IOException e) {

      return Optional.empty();
    }
  }
}
