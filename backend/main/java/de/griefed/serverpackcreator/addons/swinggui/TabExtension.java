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
import de.griefed.serverpackcreator.addons.ExtensionInformation;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.util.Optional;
import javax.swing.Icon;
import javax.swing.JPanel;

/**
 * Extension point for addons which add additional {@link JPanel}s as additional tabs to the
 * ServerPackCreator GUI.
 *
 * @author Griefed
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public interface TabExtension extends ExtensionInformation {

  /**
   * @param versionMeta           Instance of {@link VersionMeta} so you can work with available
   *                              Minecraft, Forge, Fabric, LegacyFabric and Quilt versions.
   * @param applicationProperties Instance of {@link ApplicationProperties} The current
   *                              configuration of ServerPackCreator, like the default list of
   *                              clientside-only mods, the server pack directory etc.
   * @param utilities             Instance of {@link Utilities} commonly used across
   *                              ServerPackCreator.
   * @param addonConfig           Addon specific configuration conveniently provided by
   *                              ServerPackCreator. This is the global configuration of the addon
   *                              which provides the ConfigPanelExtension to ServerPackCreator.
   * @param configFile            The config-file corresponding to the ID of the addon, wrapped in
   *                              an Optional.
   * @return Component to add to the ServerPackCreator GUI as a tab.
   * @author Griefed
   */
  ExtensionTab getTab(
      final VersionMeta versionMeta,
      final ApplicationProperties applicationProperties,
      final Utilities utilities,
      final Optional<CommentedConfig> addonConfig,
      final Optional<File> configFile);

  /**
   * Get the {@link Icon} for this tab to display to the ServerPackCreator GUI.
   *
   * @return Icon to be used by the added tab.
   * @author Griefed
   */
  Icon getTabIcon();

  /**
   * Get the title of this tab to display in the ServerPackCreator GUI.
   *
   * @return The title of this addons tabbed pane.
   * @author Griefed
   */
  String getTabTitle();

  /**
   * Get the tooltip for this tab to display in the ServerPackCreator GUI.
   *
   * @return The tooltip of this addons tabbed pane.
   * @author Griefed
   */
  String getTabTooltip();
}
