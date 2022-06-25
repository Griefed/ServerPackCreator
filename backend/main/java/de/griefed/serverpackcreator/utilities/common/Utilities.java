package de.griefed.serverpackcreator.utilities.common;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Parent-class for all utilities, giving access to any and all utility-classes we may have.
 *
 * @author Griefed
 */
@Component
public class Utilities {

  private final BooleanUtilities BOOLEAN_UTILITIES;

  private final FileUtilities FILE_UTILITIES;
  private final JarUtilities JAR_UTILITIES;
  private final ListUtilities LIST_UTILITIES;
  private final StringUtilities STRING_UTILITIES;
  private final SystemUtilities SYSTEM_UTILITIES;
  private final WebUtilities WEB_UTILITIES;

  @Autowired
  public Utilities(
      LocalizationManager injectedLocalizationManager,
      ApplicationProperties injectedApplicationProperties) {

    this.BOOLEAN_UTILITIES =
        new BooleanUtilities(injectedLocalizationManager, injectedApplicationProperties);
    this.FILE_UTILITIES = new FileUtilities();
    this.JAR_UTILITIES = new JarUtilities();
    this.LIST_UTILITIES = new ListUtilities();
    this.STRING_UTILITIES = new StringUtilities();
    this.SYSTEM_UTILITIES = new SystemUtilities();
    this.WEB_UTILITIES =
        new WebUtilities(injectedApplicationProperties, injectedLocalizationManager);
  }

  public BooleanUtilities BooleanUtils() {
    return BOOLEAN_UTILITIES;
  }

  public FileUtilities FileUtils() {
    return FILE_UTILITIES;
  }

  public JarUtilities JarUtils() {
    return JAR_UTILITIES;
  }

  public ListUtilities ListUtils() {
    return LIST_UTILITIES;
  }

  public StringUtilities StringUtils() {
    return STRING_UTILITIES;
  }

  public SystemUtilities SystemUtils() {
    return SYSTEM_UTILITIES;
  }

  public WebUtilities WebUtils() {
    return WEB_UTILITIES;
  }
}
