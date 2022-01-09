/* Copyright (C) 2021  Griefed
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
package de.griefed.serverpackcreator.utilities;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BooleanUtilitiesTest {

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final BooleanUtilities BOOLEANUTILITIES;

    BooleanUtilitiesTest() {
        this.LOCALIZATIONMANAGER = new LocalizationManager();
        this.APPLICATIONPROPERTIES = new ApplicationProperties();
        this.BOOLEANUTILITIES = new BooleanUtilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
    }

    @Test
    void convertToBooleanTestTrue() {
        Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("True"));
        Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("true"));
        Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("1"));
        Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("Yes"));
        Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("yes"));
        Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("Y"));
        Assertions.assertTrue(BOOLEANUTILITIES.convertToBoolean("y"));
    }

    @Test
    void convertToBooleanTestFalse() {
        Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("False"));
        Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("false"));
        Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("0"));
        Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("No"));
        Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("no"));
        Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("N"));
        Assertions.assertFalse(BOOLEANUTILITIES.convertToBoolean("n"));
    }
}
