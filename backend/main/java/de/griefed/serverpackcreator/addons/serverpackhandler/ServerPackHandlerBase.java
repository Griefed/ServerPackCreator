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
package de.griefed.serverpackcreator.addons.serverpackhandler;

import com.electronwill.nightconfig.core.CommentedConfig;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.addons.BaseInformation;
import java.util.ArrayList;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
interface ServerPackHandlerBase extends BaseInformation {

  /**
   * Run this plugin with the passed {@link ApplicationProperties}, {@link ConfigurationModel} and
   * server pack <code>destination</code>
   *
   * @param applicationProperties Instance of {@link ApplicationProperties} as ServerPackCreator
   *                              itself uses it.
   * @param configurationModel    Instance of {@link ConfigurationModel} for a given server pack.
   * @param destination           String. The destination of the server pack.
   * @param addonConfig           Configuration for this addon, conveniently provided by
   *                              ServerPackCreator.
   * @param packSpecificConfigs   Modpack and server pack specific configurations for this addon,
   *                              conveniently provided by ServerPackCreator.
   * @throws Exception {@link Exception} when an uncaught error occurs in the addon.
   * @author Griefed
   */
  void run(
      final ApplicationProperties applicationProperties,
      final ConfigurationModel configurationModel,
      String destination,
      Optional<CommentedConfig> addonConfig,
      Optional<ArrayList<CommentedConfig>> packSpecificConfigs)
      throws Exception;

}
