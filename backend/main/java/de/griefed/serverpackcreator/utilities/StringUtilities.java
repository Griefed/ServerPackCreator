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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Utility-class revolving around Strings.
 * @author Griefed
 */
@Component
public class StringUtilities {

    @Autowired
    public StringUtilities() {

    }

    /**
     * Converts a sequence of Strings, for example from a list, into a concatenated String.
     * @author whitebear60
     * @param args Strings that will be concatenated into one string
     * @return String. Returns concatenated string that contains all provided values.
     */
    public String buildString(String... args) {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(Arrays.toString(args));

        stringBuilder.delete(0, 2).reverse().delete(0,2).reverse();

        return stringBuilder.toString();
    }
}
