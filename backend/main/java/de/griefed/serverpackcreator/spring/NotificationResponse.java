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
package de.griefed.serverpackcreator.spring;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * NotificationResponse to be passed back to the requester.
 *
 * @author Griefed
 */
@Component
public class NotificationResponse {

  private static final Logger LOG = LogManager.getLogger(NotificationResponse.class);

  /**
   * Construct a zipResponse for replying to a file-upload and display in a quasar notification.
   *
   * @param messages {@link List} A list of messages.
   * @param timeout {@link Integer} The timeout in ms until the message gets automatically
   *     discarded.
   * @param icon {@link String} The icon to be displayed in the message.
   * @param colour {@link String} The colour of the message.
   * @param file {@link String} The file name, if available.
   * @param success {@link Boolean} To indicate a successfull event or not.
   * @return {@link String} The message formatted in JSON.
   * @author Griefed
   */
  public String zipResponse(
      List<String> messages,
      int timeout,
      String icon,
      String colour,
      String file,
      boolean success) {
    StringBuilder stringBuilder = new StringBuilder();

    for (String message : messages) {
      stringBuilder.append(message);
    }

    return zipResponse(stringBuilder.toString(), timeout, icon, colour, file, success);
  }

  /**
   * Construct a zipResponse for replying to a file-upload and display in a quasar notification.
   *
   * @param message {@link String} The message itself.
   * @param timeout {@link Integer} The timeout in ms until the message gets automatically
   *     discarded.
   * @param icon {@link String} The icon to be displayed in the message.
   * @param colour {@link String} The colour of the message.
   * @param file {@link String} The file name, if available.
   * @param success {@link Boolean} To indicate a successfull event or not.
   * @return {@link String} The message formatted in JSON.
   * @author Griefed
   */
  public String zipResponse(
      String message, int timeout, String icon, String colour, String file, boolean success) {
    return "{"
        + "\"timeout\": "
        + timeout
        + ","
        + "\"icon\":\""
        + icon
        + "\","
        + "\"color\":\""
        + colour
        + "\","
        + "\"message\":\""
        + message
        + "\","
        + "\"file\":\""
        + file
        + "\","
        + "\"success\":"
        + success
        + "}";
  }
}
