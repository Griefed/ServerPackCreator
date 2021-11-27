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
package de.griefed.serverpackcreator.spring.models;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * CurseResponse to be passed back to the requester.
 * @author Griefed
 */
@Component
public class CurseResponse {

    private static final Logger LOG = LogManager.getLogger(CurseResponse.class);

    /**
     * Construct a response with all values manually set.
     * @author Griefed
     * @param projectName The name of the CurseForge project.
     * @param status The statuscode. Status 0: Already exists. Status 1: OK, generating. Status 2: Error occurred.
     * @param message The message the requester should care about.
     * @param timeout Timeout in milliseconds after which the notification in our Quasar frontend should disappear.
     * @param icon The icon to be displayed in the notification.
     * @param colour The colour of the notification.
     * @return String. Returns the aforementioned parameters wrapped in JSON.
     */
    public String response(String projectName, int status, String message, int timeout, String icon, String colour) {
        return "{" +
                "\"project\": \"" + projectName + "\"," +
                "\"status\": " + status + "," +
                "\"message\": \"" + message + "\"," +
                "\"timeout\": " + timeout + "," +
                "\"icon\": \"" + icon + "\"," +
                "\"colour\": \"" + colour + "\"" +
                "}";
    }

    /**
     * Construct a response with values acquired from the CurseForge API.
     * @author Griefed
     * @param projectID The id with which to gather the name of the project.
     * @param status The statuscode. Status 0: Already exists. Status 1: OK, generating. Status 2: Error occurred.
     * @param message The message the requester should care about.
     * @param timeout Timeout in milliseconds after which the notification in our Quasar frontend should disappear.
     * @param icon The icon to be displayed in the notification.
     * @param colour The colour of the notification.
     * @return String. Returns the aforementioned parameters wrapped in JSON.
     */
    public String response(int projectID, int status, String message, int timeout, String icon, String colour) {
        try {
            return "{" +
                    "\"project\": \"" + CurseAPI.project(projectID).orElseThrow(NullPointerException::new).name() + "\"," +
                    "\"status\": " + status + "," +
                    "\"message\": \"" + message + "\"," +
                    "\"timeout\": " + timeout + "," +
                    "\"icon\": \"" + icon + "\"," +
                    "\"colour\": \"" + colour + "\"" +
                    "}";
        } catch (NullPointerException | CurseException ex) {
            LOG.error("Project name could not be acquired", ex);
            return "{" +
                    "\"project\": \"" + projectID + "\"," +
                    "\"status\": " + status + "," +
                    "\"message\": \"" + message + "\"," +
                    "\"timeout\": " + timeout + "," +
                    "\"icon\": \"" + icon + "\"," +
                    "\"colour\": \"" + colour + "\"" +
                    "}";
        }
    }

}
