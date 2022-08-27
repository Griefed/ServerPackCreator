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

/**
 * The ServerPackCreator core.<br> The heart and soul, if you will.<br> The classes inside this
 * package are responsible for turning your modpack into a server pack. Especially the
 * {@link de.griefed.serverpackcreator.ServerPackHandler}, which receives a configuration and then
 * creates the actual server pack for you.<br><br>A regular run of ServerPackCreator is
 * <ol>
 *   <li>Load or create a configuration. <p>See {@link de.griefed.serverpackcreator.ConfigurationModel#ConfigurationModel(de.griefed.serverpackcreator.utilities.common.Utilities, java.io.File)}<br>and {@link de.griefed.serverpackcreator.ConfigurationHandler#checkConfiguration}<br>or {@link de.griefed.serverpackcreator.ConfigurationHandler#checkConfiguration(java.io.File, de.griefed.serverpackcreator.ConfigurationModel, java.util.List, boolean)}</p></li>
 *   <li>Run ServerPackCreator with the configuration. See {@link de.griefed.serverpackcreator.ServerPackHandler#run(de.griefed.serverpackcreator.ConfigurationModel)}</li>
 * </ol>
 *
 * @author Griefed
 */
package de.griefed.serverpackcreator;
