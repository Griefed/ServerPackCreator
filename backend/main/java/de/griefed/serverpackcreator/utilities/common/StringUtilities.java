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
import java.util.List;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * Utility-class revolving around Strings.
 *
 * @author Griefed
 */
@Component
public final class StringUtilities {

  @Contract(pure = true)
  public StringUtilities() {
  }

  /**
   * Converts a list of Strings, for example from a list, into a concatenated String.
   *
   * @param strings List of strings that will be concatenated into one string
   * @return Returns concatenated string that contains all provided values.
   * @author Griefed
   */
  public @NotNull String buildString(@NotNull List<String> strings) {
    return buildString(strings.toString());
  }

  /**
   * Converts a sequence of Strings, for example from a list, into a concatenated String.
   *
   * @param args Strings that will be concatenated into one string
   * @return Returns concatenated string that contains all provided values.
   * @author whitebear60
   */
  public @NotNull String buildString(@NotNull String... args) {

    StringBuilder stringBuilder = new StringBuilder();

    stringBuilder.append(Arrays.toString(args));

    stringBuilder.delete(0, 2).reverse().delete(0, 2).reverse();

    return stringBuilder.toString();
  }

  /**
   * Remove commonly forbidden characters from the passed string, making the resulting String safe
   * to use for files, paths, directories etc. If the passed text ends with a
   * SPACE{@code (&#32;&#32;)} or a DOT{@code (&#32;.&#32;)}, they are also removed.<br>
   * Replaced/removed are:
   *
   * <ul>
   *   <li><b>&#47;</b>
   *   <li><b>&#60;</b>
   *   <li><b>&#62;</b>
   *   <li><b>&#58;</b>
   *   <li><b>&#34;</b>
   *   <li><b>&#92;</b>
   *   <li><b>&#124;</b>
   *   <li><b>&#63;</b>
   *   <li><b>&#42;</b>
   *   <li><b>&#35;</b>
   *   <li><b>&#37;</b>
   *   <li><b>&#38;</b>
   *   <li><b>&#123;</b>
   *   <li><b>&#125;</b>
   *   <li><b>&#36;</b>
   *   <li><b>&#33;</b>
   *   <li><b>&#39;</b>
   *   <li><b>&#64;</b>
   *   <li><b>&#43;</b>
   *   <li><b>&#180;</b>
   *   <li><b>&#96;</b>
   *   <li><b>&#61;</b>
   *   <li><b>&#91;</b>
   *   <li><b>&#93;</b>
   * </ul>
   *
   * <br>
   *
   * @param text The text which you want to be made safe.
   * @return The passed String safe for use for files, paths, directories etc.
   * @author Griefed
   */
  public @NotNull String pathSecureText(@NotNull String text) {

    while (text.endsWith(".") || text.endsWith(" ")) {
      text = text.replace(text.substring(text.length() - 1), "");
    }

    return text.replace("/", "")
               .replace("<", "")
               .replace(">", "")
               .replace(":", "")
               .replace("\"", "")
               .replace("\\", "")
               .replace("|", "")
               .replace("?", "")
               .replace("*", "")
               .replace("#", "")
               .replace("%", "")
               .replace("&", "")
               .replace("{", "")
               .replace("}", "")
               .replace("$", "")
               .replace("!", "")
               .replace("'", "")
               .replace("@", "")
               .replace("+", "")
               .replace("´", "")
               .replace("`", "")
               .replace("=", "")
               .replace("[", "")
               .replace("]", "");
  }

  /**
   * Remove commonly forbidden characters from the passed string, making the resulting String safe
   * to use for files, paths, directories etc. If the passed text ends with a
   * SPACE{@code (&#32;&#32;)} or a DOT{@code (&#32;.&#32;)}, they are also removed.<br> Contrary to
   * {@link #pathSecureText(String)}, this method does <strong>NOT</strong> remove
   * <strong>&#47;</strong> or <strong>&#92;</strong>.
   *
   * <p>Replaced/removed are:
   *
   * <ul>
   *   <li><b>&#60;</b>
   *   <li><b>&#62;</b>
   *   <li><b>&#58;</b>
   *   <li><b>&#34;</b>
   *   <li><b>&#124;</b>
   *   <li><b>&#63;</b>
   *   <li><b>&#42;</b>
   *   <li><b>&#35;</b>
   *   <li><b>&#37;</b>
   *   <li><b>&#38;</b>
   *   <li><b>&#123;</b>
   *   <li><b>&#125;</b>
   *   <li><b>&#36;</b>
   *   <li><b>&#33;</b>
   *   <li><b>&#64;</b>
   *   <li><b>&#43;</b>
   *   <li><b>&#180;</b>
   *   <li><b>&#96;</b>
   *   <li><b>&#61;</b>
   *   <li><b>&#91;</b>
   *   <li><b>&#93;</b>
   * </ul>
   *
   * <br>
   *
   * @param text The text which you want to be made safe.
   * @return The passed String safe for use for files, paths, directories etc.
   * @author Griefed
   */
  public @NotNull String pathSecureTextAlternative(@NotNull String text) {

    while (text.endsWith(".") || text.endsWith(" ")) {
      text = text.replace(text.substring(text.length() - 1), "");
    }

    return text.replace("<", "")
               .replace(">", "")
               .replace(":", "")
               .replace("\"", "")
               .replace("|", "")
               .replace("?", "")
               .replace("*", "")
               .replace("#", "")
               .replace("%", "")
               .replace("&", "")
               .replace("{", "")
               .replace("}", "")
               .replace("$", "")
               .replace("!", "")
               .replace("@", "")
               .replace("+", "")
               .replace("´", "")
               .replace("`", "")
               .replace("=", "")
               .replace("[", "")
               .replace("]", "");
  }

  /**
   * Check the passed string whether it contains any of the following characters:
   *
   * <ul>
   *   <li><b>&#47;</b>
   *   <li><b>&#60;</b>
   *   <li><b>&#62;</b>
   *   <li><b>&#58;</b>
   *   <li><b>&#34;</b>
   *   <li><b>&#92;</b>
   *   <li><b>&#124;</b>
   *   <li><b>&#63;</b>
   *   <li><b>&#42;</b>
   *   <li><b>&#35;</b>
   *   <li><b>&#37;</b>
   *   <li><b>&#38;</b>
   *   <li><b>&#123;</b>
   *   <li><b>&#125;</b>
   *   <li><b>&#36;</b>
   *   <li><b>&#33;</b>
   *   <li><b>&#64;</b>
   *   <li><b>&#43;</b>
   *   <li><b>&#180;</b>
   *   <li><b>&#96;</b>
   *   <li><b>&#61;</b>
   * </ul>
   *
   * <br>
   *
   * @param text The text you want to check.
   * @return {@code true} if none of these characters were found.
   * @author Griefed
   */
  public boolean checkForIllegalCharacters(@NotNull String text) {
    return !text.contains("/")
        && !text.contains("<")
        && !text.contains(">")
        && !text.contains(":")
        && !text.contains("\"")
        && !text.contains("\\")
        && !text.contains("|")
        && !text.contains("?")
        && !text.contains("*")
        && !text.contains("#")
        && !text.contains("%")
        && !text.contains("&")
        && !text.contains("{")
        && !text.contains("}")
        && !text.contains("$")
        && !text.contains("!")
        && !text.contains("@")
        && !text.contains("+")
        && !text.contains("`")
        && !text.contains("´")
        && !text.contains("=");
  }
}
