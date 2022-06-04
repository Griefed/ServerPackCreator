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
package de.griefed.serverpackcreator.plugins;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.utilities.misc.Generated;
import org.pf4j.ExtensionPoint;

/**
 * Starting point from which all plugin interfaces in ServerPackCreator extend.<br>
 * DO NOT IMPLEMENT THIS CLASS DIRECTLY WHEN WRITING A PLUGIN!<br>
 * Instead, implement any of the interfaces in the sub-packages of <code>
 * de.griefed.serverpackcreator.plugins.*</code>
 *
 * @author Griefed
 */
@Generated
public interface PluginInformation extends ExtensionPoint {

  /**
   * Run this plugin with the passed {@link ApplicationProperties}, {@link ConfigurationModel} and
   * server pack <code>destination</code>
   *
   * @param applicationProperties Instance of {@link ApplicationProperties} as ServerPackCreator
   *     itself uses it.
   * @param configurationModel Instance of {@link ConfigurationModel} for a given server pack.
   * @param destination String. The destination of the server pack.
   * @throws Exception {@link Exception} when an uncaught error occurs in the addon.
   * @author Griefed
   */
  void run(
      ApplicationProperties applicationProperties,
      ConfigurationModel configurationModel,
      String destination)
      throws Exception;

  /**
   * Get the name of this plugin.
   *
   * @return String. Returns the name of this plugin.
   * @author Griefed
   */
  String getName();

  /**
   * Get the description of this plugin.
   *
   * @return String. Returns the description of this plugin.
   * @author Griefed
   */
  String getDescription();

  /**
   * Get the author of this plugin.
   *
   * @return String. Returns the author of this plugin.
   * @author Griefed
   */
  String getAuthor();

  /**
   * Get the version of this plugin.
   *
   * @return String. Returns the version of this plugin.
   * @author Griefed
   */
  String getVersion();
}
