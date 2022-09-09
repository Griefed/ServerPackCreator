package de.griefed.serverpackcreator.addons;

import org.pf4j.ExtensionPoint;

public interface ExtensionInformation extends BaseInformation, ExtensionPoint {

  /**
   * Get the if of this extension. Used by ServerPackCreator to determine which configuration, if any,
   * to provide to any given extension being run.
   *
   * @return The ID of this extension.
   * @author Griefed
   */
  String getExtensionId();

}
