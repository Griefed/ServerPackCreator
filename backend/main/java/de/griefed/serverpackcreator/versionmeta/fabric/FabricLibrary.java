package de.griefed.serverpackcreator.versionmeta.fabric;

/**
 * Library information.
 *
 * @author Griefed
 */
public class FabricLibrary {

  private String name;
  private String url;

  private FabricLibrary() {}

  /**
   * The name of this library.
   *
   * @return {@link String} Library-name.
   * @author Griefed
   */
  public String getName() {
    return name;
  }

  /**
   * The URL to this library.
   *
   * @return {@link String} Library-URL as a String.
   * @author Griefed
   */
  public String getUrl() {
    return url;
  }
}
