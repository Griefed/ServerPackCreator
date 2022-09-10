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
package de.griefed.serverpackcreator.addons.swinggui;

import com.electronwill.nightconfig.core.CommentedConfig;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.addons.BaseInformation;
import de.griefed.serverpackcreator.addons.ExtensionInformation;
import de.griefed.serverpackcreator.swing.TabCreateServerPack;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.util.Optional;
import javax.swing.JPanel;

/**
 * Extension point for addons which add additional {@link JPanel}s in a given server pack tab,
 * allowing users to customize server pack specific configurations of an addon.
 *
 * @author Griefed
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public interface ConfigPanelExtension extends ExtensionInformation {

  /**
   * This method gets called when an extension of this type is run. All parameters are provided by
   * ServerPackCreator, so you do not have to take care of them. A ConfigPanel is intended to be
   * used to change server pack-specific configurations which can then be used by other extensions
   * in your plugin. A simple example would be downloading a specific version of some mod. You could
   * add a panel which lets the user configure the version of the mod to use. When the user then
   * runs the server pack generation, your setting will be stored in the subsequently generated
   * serverpackcreator.conf, and could be used in any of the
   * {@link de.griefed.serverpackcreator.ServerPackHandler} extension-points, which would then
   * download the version you specified via this here panel.
   *
   * @param versionMeta           Instance of {@link VersionMeta} so you can work with available
   *                              Minecraft, Forge, Fabric, LegacyFabric and Quilt versions.
   * @param applicationProperties Instance of {@link ApplicationProperties} The current
   *                              configuration of ServerPackCreator, like the default list of
   *                              clientside-only mods, the server pack directory etc.
   * @param utilities             Instance of {@link Utilities} commonly used across
   *                              ServerPackCreator.
   * @param tabCreateServerPack   Instance of {@link TabCreateServerPack} to give you access to the
   *                              various fields inside it, like the modpack directory, selected
   *                              Minecraft, modloader and modloader versions, etc.
   * @param addonConfig           Addon specific configuration conveniently provided by
   *                              ServerPackCreator. This is the global configuration of the addon
   *                              which provides the ConfigPanelExtension to ServerPackCreator.
   * @param extensionName         The name the titled border of this ConfigPanel will get.
   * @param pluginID              The same as the PluginId.
   * @return A ConfigPanel allowing users to further customize their ServerPackCreator experience
   * when using an addon.
   * @author Griefed
   */
  ExtensionConfigPanel getPanel(
      final VersionMeta versionMeta,
      final ApplicationProperties applicationProperties,
      final Utilities utilities,
      final TabCreateServerPack tabCreateServerPack,
      final Optional<CommentedConfig> addonConfig,
      final String extensionName,
      final String pluginID);
}
