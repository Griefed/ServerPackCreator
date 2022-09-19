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

import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Utility-class revolving around Booleans.
 *
 * @author Griefed
 */
@Component
public final class BooleanUtilities {

  private static final Logger LOG = LogManager.getLogger(BooleanUtilities.class);

  public BooleanUtilities() {
  }

  /**
   * Converts various strings to booleans, by using regex, to allow for more variations in input.
   * <br>
   * <strong>Converted to {@code TRUE} are:<br>
   * </strong> {@code [Tt]rue}<br>
   * {@code 1}<br> {@code [Yy]es}<br> {@code [Yy]}<br> Language Key {@code cli.input.true}<br>
   * Language Key {@code cli.input.yes}<br> Language Key {@code cli.input.yes.short}<br>
   * <strong>Converted to {@code FALSE} are:<br>
   * </strong> {@code [Ff]alse}<br>
   * {@code 0}<br> {@code [Nn]o}<br> {@code [Nn]}<br> Language Key {@code cli.input.false}<br>
   * Language Key {@code cli.input.no}<br> Language Key {@code cli.input.no.short}<br>
   *
   * @param stringBoolean The string which should be converted to boolean if it matches certain
   *                      patterns.
   * @return Boolean. Returns the corresponding boolean if match with pattern was found. If no match
   * is found, assume and return false.
   * @author Griefed
   */
  public boolean convert(String stringBoolean) {

    if (stringBoolean.matches("1")
        || stringBoolean.matches("[Yy][Ee][Ss]")
        || stringBoolean.matches("[Yy]")
        || stringBoolean.equalsIgnoreCase("true")) {
      return true;

    } else if (stringBoolean.matches("0")
        || stringBoolean.matches("[Nn][Oo]]")
        || stringBoolean.matches("[Nn]")
        || stringBoolean.equalsIgnoreCase("false")) {
      return false;

    } else {
      LOG.warn("Warning. Couldn't parse boolean. Assuming false.");
      return false;
    }
  }

  /**
   * Prompts the user to enter values which will then be converted to booleans, either {@code TRUE }
   * or {@code FALSE}. This prevents any non-boolean values from being written to the new
   * configuration file.
   *
   * @return Boolean. True or False, depending on user input.
   * @author whitebear60
   * @deprecated Will be removed in Milestone 4. Use {@link #readBoolean(Scanner)} instead.
   */
  @Deprecated
  public boolean readBoolean() {
    Scanner readerBoolean = new Scanner(System.in);
    boolean read = readBoolean(readerBoolean);
    readerBoolean.close();
    return read;
  }

  /**
   * Prompts the user to enter values which will then be converted to booleans, either {@code TRUE }
   * or {@code FALSE}. This prevents any non-boolean values from being written to the new
   * configuration file.
   *
   * @param scanner Used for reading the users input.
   * @return Boolean. True or False, depending on user input.
   * @author Griefed
   */
  public boolean readBoolean(Scanner scanner) {
    printBoolMenu();
    return convert(scanner.nextLine());
  }

  /**
   * Print a small help text to tell the user which values are accepted as {@code true} and which
   * values are accepted as {@code false}.
   *
   * @author Griefed
   */
  private void printBoolMenu() {
    System.out.println("True: 1, Yes, Y, true -|- False: 0, No, N, false");
  }
}
