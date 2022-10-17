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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.griefed.serverpackcreator.ApplicationProperties;
import javax.xml.parsers.DocumentBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Parent-class for all utilities, giving access to any and all utility-classes we may have.
 *
 * @author Griefed
 */
@Component
public final class Utilities {

  private final BooleanUtilities BOOLEAN_UTILITIES;
  private final FileUtilities FILE_UTILITIES;
  private final JarUtilities JAR_UTILITIES;
  private final ListUtilities LIST_UTILITIES;
  private final StringUtilities STRING_UTILITIES;
  private final SystemUtilities SYSTEM_UTILITIES;
  private final WebUtilities WEB_UTILITIES;
  private final JsonUtilities JSON_UTILITIES;
  private final XmlUtilities XML_UTILITIES;

  @Autowired
  public Utilities(@NotNull ApplicationProperties injectedApplicationProperties,
                   @NotNull ObjectMapper injectedObjectMapper,
                   @NotNull DocumentBuilder documentBuilder) {
    BOOLEAN_UTILITIES = new BooleanUtilities();
    FILE_UTILITIES = new FileUtilities();
    JAR_UTILITIES = new JarUtilities();
    LIST_UTILITIES = new ListUtilities();
    STRING_UTILITIES = new StringUtilities();
    SYSTEM_UTILITIES = new SystemUtilities();
    WEB_UTILITIES = new WebUtilities(injectedApplicationProperties);
    JSON_UTILITIES = new JsonUtilities(injectedObjectMapper);
    XML_UTILITIES = new XmlUtilities(documentBuilder);
  }

  @Contract(pure = true)
  public Utilities(
      @NotNull BooleanUtilities booleanUtilities,
      @NotNull FileUtilities fileUtilities,
      @NotNull JarUtilities jarUtilities,
      @NotNull ListUtilities listUtilities,
      @NotNull StringUtilities stringUtilities,
      @NotNull SystemUtilities systemUtilities,
      @NotNull WebUtilities webUtilities,
      @NotNull JsonUtilities jsonUtilities,
      @NotNull XmlUtilities xmlUtilities) {
    BOOLEAN_UTILITIES = booleanUtilities;
    FILE_UTILITIES = fileUtilities;
    JAR_UTILITIES = jarUtilities;
    LIST_UTILITIES = listUtilities;
    STRING_UTILITIES = stringUtilities;
    SYSTEM_UTILITIES = systemUtilities;
    WEB_UTILITIES = webUtilities;
    JSON_UTILITIES = jsonUtilities;
    XML_UTILITIES = xmlUtilities;
  }

  @Contract(pure = true)
  public @NotNull BooleanUtilities BooleanUtils() {
    return BOOLEAN_UTILITIES;
  }

  @Contract(pure = true)
  public @NotNull FileUtilities FileUtils() {
    return FILE_UTILITIES;
  }

  @Contract(pure = true)
  public @NotNull JarUtilities JarUtils() {
    return JAR_UTILITIES;
  }

  @Contract(pure = true)
  public @NotNull ListUtilities ListUtils() {
    return LIST_UTILITIES;
  }

  @Contract(pure = true)
  public @NotNull StringUtilities StringUtils() {
    return STRING_UTILITIES;
  }

  @Contract(pure = true)
  public @NotNull SystemUtilities SystemUtils() {
    return SYSTEM_UTILITIES;
  }

  @Contract(pure = true)
  public @NotNull WebUtilities WebUtils() {
    return WEB_UTILITIES;
  }

  @Contract(pure = true)
  public @NotNull JsonUtilities JsonUtilities() {
    return JSON_UTILITIES;
  }

  @Contract(pure = true)
  public @NotNull XmlUtilities XmlUtilities() {
    return XML_UTILITIES;
  }
}
