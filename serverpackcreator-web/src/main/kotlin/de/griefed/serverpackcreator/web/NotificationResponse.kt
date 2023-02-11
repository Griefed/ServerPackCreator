/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.web

import org.springframework.stereotype.Component

/**
 * NotificationResponse to be passed back to the requester.
 *
 * @author Griefed
 */
@Component
class NotificationResponse {
    /**
     * Construct a zipResponse for replying to a file-upload and display in a quasar notification.
     *
     * @param messages A list of messages.
     * @param timeout  The timeout in ms until the message gets automatically discarded.
     * @param icon     The icon to be displayed in the message.
     * @param colour   The colour of the message.
     * @param file     The file name, if available.
     * @param success  To indicate a successful event or not.
     * @return The message formatted in JSON.
     * @author Griefed
     */
    fun zipResponse(
        messages: List<String>,
        timeout: Int,
        icon: String,
        colour: String,
        file: String,
        success: Boolean
    ): String {
        val stringBuilder = StringBuilder()
        for (message in messages) {
            stringBuilder.append(message)
        }
        return zipResponse(stringBuilder.toString(), timeout, icon, colour, file, success)
    }

    /**
     * Construct a zipResponse for replying to a file-upload and display in a quasar notification.
     *
     * @param message The message itself.
     * @param timeout The timeout in ms until the message gets automatically discarded.
     * @param icon    The icon to be displayed in the message.
     * @param colour  The colour of the message.
     * @param file    The file name, if available.
     * @param success To indicate a successful event or not.
     * @return The message formatted in JSON.
     * @author Griefed
     */
    fun zipResponse(
        message: String,
        timeout: Int,
        icon: String,
        colour: String,
        file: String?,
        success: Boolean
    ): String {
        return ("{"
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
                + "}")
    }
}