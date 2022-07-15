package de.griefed.serverpackcreator.versionmeta.fabric;

/**
 * A Fabric intermediary.
 *
 * @author Griefed
 */
public class FabricIntermediary {

  private String maven;
  private String version;
  private boolean stable;

  private FabricIntermediary() {}

  /**
   * The maven mapping of this intermediary.
   *
   * @return maven mapping.
   * @author Griefed
   */
  public String getMaven() {
    return maven;
  }

  /**
   * The version of this intermediary.
   *
   * @return {@link String} The version of this intermediary.
   * @author Griefed
   */
  public String getVersion() {
    return version;
  }

  /**
   * Whether this intermediary is considered stable.
   *
   * @return {@link Boolean} Whether this intermediary is considered stable.
   * @author Griefed
   */
  public boolean isStable() {
    return stable;
  }
}
