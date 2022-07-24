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
package de.griefed.serverpackcreator.utilities.common;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.i18n.I18n;
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
  public Utilities(I18n injectedI18n, ApplicationProperties injectedApplicationProperties) {

    this.BOOLEAN_UTILITIES = new BooleanUtilities();
    this.FILE_UTILITIES = new FileUtilities();
    this.JAR_UTILITIES = new JarUtilities();
    this.LIST_UTILITIES = new ListUtilities();
    this.STRING_UTILITIES = new StringUtilities();
    this.SYSTEM_UTILITIES = new SystemUtilities();
    this.WEB_UTILITIES = new WebUtilities(injectedApplicationProperties);
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
