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
import de.griefed.serverpackcreator.utilities.misc.Generated;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility-class revolving around Booleans.
 *
 * @author Griefed
 */
public class BooleanUtilities {

  private static final Logger LOG = LogManager.getLogger(BooleanUtilities.class);
  private final I18n I18N;
  private final ApplicationProperties APPLICATIONPROPERTIES;

  public BooleanUtilities(I18n injectedI18n, ApplicationProperties injectedApplicationProperties) {

    if (injectedApplicationProperties == null) {
      this.APPLICATIONPROPERTIES = new ApplicationProperties();
    } else {
      this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    }

    if (injectedI18n == null) {
      this.I18N = new I18n(APPLICATIONPROPERTIES);
    } else {
      this.I18N = injectedI18n;
    }
  }

  /**
   * Converts various strings to booleans, by using regex, to allow for more variations in input.
   * <br>
   * <strong>Converted to <code>TRUE</code> are:<br>
   * </strong> <code>[Tt]rue</code><br>
   * <code>1</code><br>
   * <code>[Yy]es</code><br>
   * <code>[Yy]</code><br>
   * Language Key <code>cli.input.true</code><br>
   * Language Key <code>cli.input.yes</code><br>
   * Language Key <code>cli.input.yes.short</code><br>
   * <strong>Converted to <code>FALSE</code> are:<br>
   * </strong> <code>[Ff]alse</code><br>
   * <code>0</code><br>
   * <code>[Nn]o</code><br>
   * <code>[Nn]</code><br>
   * Language Key <code>cli.input.false</code><br>
   * Language Key <code>cli.input.no</code><br>
   * Language Key <code>cli.input.no.short</code><br>
   *
   * @param stringBoolean String. The string which should be converted to boolean if it matches
   *     certain patterns.
   * @return Boolean. Returns the corresponding boolean if match with pattern was found. If no match
   *     is found, assume and return false.
   * @author Griefed
   */
  public boolean convertToBoolean(String stringBoolean) {

    if (stringBoolean.matches("[Tt]rue")
        || stringBoolean.matches("1")
        || stringBoolean.matches("[Yy]es")
        || stringBoolean.matches("[Yy]")
        || stringBoolean.matches(I18N.getMessage("cli.input.true"))
        || stringBoolean.matches(I18N.getMessage("cli.input.yes"))
        || stringBoolean.matches(I18N.getMessage("cli.input.yes.short"))) {
      return true;

    } else if (stringBoolean.matches("[Ff]alse")
        || stringBoolean.matches("0")
        || stringBoolean.matches("[Nn]o")
        || stringBoolean.matches("[Nn]")
        || stringBoolean.matches(I18N.getMessage("cli.input.false"))
        || stringBoolean.matches(I18N.getMessage("cli.input.no"))
        || stringBoolean.matches(I18N.getMessage("cli.input.no.short"))) {
      return false;

    } else {
      /* This log is meant to be read by the user, therefore we allow translation. */
      LOG.warn(I18N.getMessage("configuration.log.warn.converttoboolean.warn"));
      return false;
    }
  }

  /**
   * Prompts the user to enter values which will then be converted to booleans, either <code>TRUE
   * </code> or <code>FALSE</code>. This prevents any non-boolean values from being written to the
   * new configuration file.
   *
   * @return Boolean. True or False, depending on user input.
   * @author whitebear60
   */
  @Generated
  public boolean readBoolean() {

    Scanner readerBoolean = new Scanner(System.in);

    String boolRead;

    boolRead = readerBoolean.nextLine();

    readerBoolean.close();

    return convertToBoolean(boolRead);
  }
}
