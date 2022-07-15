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
   * @param intermediaryManifest {@link File} Fabric Intermediary manifest-file.
   * @param objectMapper {@link ObjectMapper} to parse JSON.
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
   * @return {@link List} of intermediaries.
   * @throws IOException when the manifest could not be read.
   * @author Griefed
   */
  private List<FabricIntermediary> listIntermediariesFromManifest() throws IOException {
    return OBJECT_MAPPER.readValue(
        INTERMEDIARY_MANIFEST, new TypeReference<List<FabricIntermediary>>() {});
  }

  /**
   * HashMap of available intermediaries.
   *
   * @return {@link HashMap} of available intermediaries.
   * @author Griefed
   */
  protected HashMap<String, FabricIntermediary> getIntermediaries() {
    return INTERMEDIARIES;
  }

  /**
   * Get a specific intermediary, wrapped in an {@link Optional}.
   *
   * @param minecraftVersion {@link String} Minecraft version.
   * @return A specific intermediary, wrapped in an {@link Optional}.
   * @author Griefed
   */
  protected Optional<FabricIntermediary> getIntermediary(String minecraftVersion) {
    return Optional.ofNullable(INTERMEDIARIES.get(minecraftVersion));
  }
}
