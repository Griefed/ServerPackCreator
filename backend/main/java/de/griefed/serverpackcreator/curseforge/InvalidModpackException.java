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
package de.griefed.serverpackcreator.curseforge;

/**
 * Exceptions thrown if the specified project isn't a valid Minecraft modpack.
 * @author Griefed
 */
public class InvalidModpackException extends Exception {

    /**
     * Thrown if the specified project is not a valid Minecraft modpack.
     * @author Griefed
     * @param projectID Integer. The id of the CurseForge project.
     * @param projectName String. The name of the CurseForge project.
     */
    public InvalidModpackException(int projectID, String projectName) {
        super("The specified project " + projectName + "(" + projectID + ") is not a Minecraft modpack!");
    }

    /**
     * Thrown if the specified project is not a valid Minecraft modpack.
     * @author Griefed
     */
    public InvalidModpackException() {
        super("The specified project is not a Minecraft modpack!");
    }

}
