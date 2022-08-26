package de.griefed.serverpackcreator.versionmeta.legacyfabric;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

final class LegacyFabricGame extends LegacyFabricVersioning {

  LegacyFabricGame(File manifest, ObjectMapper mapper) throws IOException {
    super(manifest, mapper);
  }
}
