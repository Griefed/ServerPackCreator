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
 *
 * @author Griefed
 */
@Component
public class CurseResponse {

    private static final Logger LOG = LogManager.getLogger(CurseResponse.class);

    /**
     *
     * @author Griefed
     * @param projectName
     * @param status
     * @param message
     * @param timeout
     * @param icon
     * @param colour
     * @return
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
     *
     * @author Griefed
     * @param projectID
     * @param status
     * @param message
     * @param timeout
     * @param icon
     * @param colour
     * @return
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
                    "}";        }
    }

}
