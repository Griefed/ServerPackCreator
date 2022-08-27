package de.griefed.serverpackcreator.versionmeta.legacyfabric;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.versionmeta.Manifests;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

abstract class LegacyFabricVersioning extends Manifests {

  private final List<String> RELEASES = new ArrayList<>();
  private final List<String> SNAPSHOTS = new ArrayList<>();
  private final List<String> ALL = new ArrayList<>();
  private final ObjectMapper MAPPER;
  private final File MANIFEST;

  LegacyFabricVersioning(File manifest, ObjectMapper mapper) throws IOException {
    MAPPER = mapper;
    MANIFEST = manifest;
    update();
  }

  /**
   * Update all lists of available versions with new information gathered from the manifest.
   *
   * @throws IOException
   */
  void update() throws IOException {
    RELEASES.clear();
    SNAPSHOTS.clear();
    ALL.clear();

    getJson(MANIFEST, MAPPER).forEach(node -> {
      String version = node.get("version").asText();
      ALL.add(version);
      if (node.get("stable").asBoolean()) {
        RELEASES.add(version);
      } else {
        SNAPSHOTS.add(version);
      }
    });
  }

  /**
   * List of release versions for this meta.
   *
   * @return Release versions.
   * @author Griefed
   */
  List<String> releases() {
    return RELEASES;
  }

  /**
   * List of snapshot/pre-release versions for this meta.
   *
   * @return Snapshot/pre-release versions.
   * @author Griefed
   */
  List<String> snapshots() {
    return SNAPSHOTS;
  }

  /**
   * List of all versions for this meta.
   *
   * @return All versions.
   * @author Griefed
   */
  List<String> all() {
    return ALL;
  }

}
