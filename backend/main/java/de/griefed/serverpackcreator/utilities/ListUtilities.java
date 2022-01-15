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
package de.griefed.serverpackcreator.utilities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Utility-class revolving around Lists.
 * @author Griefed
 */
@Component
public class ListUtilities {

    @Autowired
    public ListUtilities() {

    }

    /**
     * Encapsulate every element of the passed String List in quotes. Returns the list as <code>["element1","element2","element3"</code> etc.
     * @author Griefed
     * @param listToEncapsulate The String List of which to encapsulate every element in.
     * @return String. Returns a concatenated String with all elements of the passed list encapsulated.
     */
    public String encapsulateListElements(List<String> listToEncapsulate) {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("[\"").append(listToEncapsulate.get(0).replace("\\", "/")).append("\"");

        for (int i = 1; i < listToEncapsulate.size(); i++) {
            stringBuilder.append(",\"").append(listToEncapsulate.get(i).replace("\\", "/")).append("\"");
        }

        stringBuilder.append("]");

        return stringBuilder.toString();
    }

    /**
     * Prompts the user to enter the values which will make up
     * a String List in the new configuration file. If the user enters an empty line, the method is exited and the
     * String List returned.
     * @author whitebear60
     * @return String List. Returns the list of values entered by the user.
     */
    public List<String> readStringArray() {

        Scanner readerArray = new Scanner(System.in);

        ArrayList<String> result = new ArrayList<>(100);

        String stringArray;

        while (true) {

            stringArray = readerArray.nextLine();

            if (stringArray.isEmpty()) {

                return result;

            } else {

                result.add(stringArray);
            }
        }
    }
}
