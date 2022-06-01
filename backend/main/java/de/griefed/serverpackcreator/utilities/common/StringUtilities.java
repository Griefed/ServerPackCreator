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

import java.util.Arrays;

/**
 * Utility-class revolving around Strings.
 * @author Griefed
 */
public class StringUtilities {

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

    /**
     * Remove commonly forbidden characters from the passed string, making the resulting String safe to use for files, paths,
     * directories etc. If the passed text ends with a SPACE<code>(&#32;&#32;)</code> or a DOT<code>(&#32;.&#32;)</code>, they are also removed.<br>
     * Replaced/removed are:
     * <ul>
     *     <li><b>&#47;</b></li>
     *     <li><b>&#60;</b></li>
     *     <li><b>&#62;</b></li>
     *     <li><b>&#58;</b></li>
     *     <li><b>&#34;</b></li>
     *     <li><b>&#92;</b></li>
     *     <li><b>&#124;</b></li>
     *     <li><b>&#63;</b></li>
     *     <li><b>&#42;</b></li>
     *     <li><b>&#35;</b></li>
     *     <li><b>&#37;</b></li>
     *     <li><b>&#38;</b></li>
     *     <li><b>&#123;</b></li>
     *     <li><b>&#125;</b></li>
     *     <li><b>&#36;</b></li>
     *     <li><b>&#33;</b></li>
     *     <li><b>&#39;</b></li>
     *     <li><b>&#64;</b></li>
     *     <li><b>&#43;</b></li>
     *     <li><b>&#180;</b></li>
     *     <li><b>&#96;</b></li>
     *     <li><b>&#61;</b></li>
     * </ul><br>
     * @author Griefed
     * @param text {@link String} The text which you want to be made safe.
     * @return {@link String} The passed String safe for use for files, paths, directories etc.
     */
    public String pathSecureText(String text) {

        while (text.endsWith(".") || text.endsWith(" ")) {
            text = text.replace(text.substring(text.length() - 1),"");
        }

        return text
                .replace("/","")
                .replace("<","")
                .replace(">","")
                .replace(":","")
                .replace("\"","")
                .replace("\\","")
                .replace("|","")
                .replace("?","")
                .replace("*","")
                .replace("#","")
                .replace("%","")
                .replace("&","")
                .replace("{","")
                .replace("}","")
                .replace("$","")
                .replace("!","")
                .replace("'","")
                .replace("@","")
                .replace("+","")
                .replace("´","")
                .replace("`","")
                .replace("=","");
    }

    /**
     * Check the passed string whether it contains any of the following characters:
     * <ul>
     *     <li><b>&#47;</b></li>
     *     <li><b>&#60;</b></li>
     *     <li><b>&#62;</b></li>
     *     <li><b>&#58;</b></li>
     *     <li><b>&#34;</b></li>
     *     <li><b>&#92;</b></li>
     *     <li><b>&#124;</b></li>
     *     <li><b>&#63;</b></li>
     *     <li><b>&#42;</b></li>
     *     <li><b>&#35;</b></li>
     *     <li><b>&#37;</b></li>
     *     <li><b>&#38;</b></li>
     *     <li><b>&#123;</b></li>
     *     <li><b>&#125;</b></li>
     *     <li><b>&#36;</b></li>
     *     <li><b>&#33;</b></li>
     *     <li><b>&#39;</b></li>
     *     <li><b>&#64;</b></li>
     *     <li><b>&#43;</b></li>
     *     <li><b>&#180;</b></li>
     *     <li><b>&#96;</b></li>
     *     <li><b>&#61;</b></li>
     * </ul><br>
     * @author Griefed
     * @param text {@link String} The text you want to check.
     * @return <code>true</code> if none of these characters were found.
     */
    public boolean checkForIllegalCharacters(String text) {
        return !text.contains("/") &&
                !text.contains("<") &&
                !text.contains(">") &&
                !text.contains(":") &&
                !text.contains("\"") &&
                !text.contains("\\") &&
                !text.contains("|") &&
                !text.contains("?") &&
                !text.contains("*") &&
                !text.contains("#") &&
                !text.contains("%") &&
                !text.contains("&") &&
                !text.contains("{") &&
                !text.contains("}") &&
                !text.contains("$") &&
                !text.contains("!") &&
                !text.contains("'") &&
                !text.contains("@") &&
                !text.contains("+") &&
                !text.contains("`") &&
                !text.contains("´") &&
                !text.contains("=");
    }
}
