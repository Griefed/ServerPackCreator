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
import de.griefed.serverpackcreator.swing.TabCreateServerPack;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Template ConfigPanel for use in {@link ConfigPanelExtension} extensions.
 *
 * @author Griefed
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class ExtensionConfigPanel extends JPanel {

  protected static final Logger LOG = LogManager.getLogger(ExtensionConfigPanel.class);
  protected static final Logger LOG_ADDONS = LogManager.getLogger("AddonsLogger");
  protected final ArrayList<CommentedConfig> SERVERPACK_EXTENSION_CONFIG = new ArrayList<>(100);
  private final Optional<CommentedConfig> ADDON_CONFIG;
  private final VersionMeta VERSIONMETA;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final Utilities UTILITIES;
  private final TabCreateServerPack SERVERPACK_CONFIGURATION_TAB;
  private final String EXTENSION_ID;

  /**
   * Construct a panel which allows users to further customize their ServerPackCreator experience.
   *
   * @param versionMeta           Instance of {@link VersionMeta} so you can work with available
   *                              Minecraft, Forge, Fabric, LegacyFabric and Quilt versions.
   * @param applicationProperties Instance of {@link Properties} The current configuration of
   *                              ServerPackCreator, like the default list of clientside-only mods,
   *                              the server pack directory etc.
   * @param utilities             Instance of {@link Utilities} commonly used across
   *                              ServerPackCreator.
   * @param tabCreateServerPack   Instance of {@link TabCreateServerPack} to give you access to the
   *                              various fields inside it, like the modpack directory, selected
   *                              Minecraft, modloader and modloader versions, etc.
   * @param addonConfig           Addon specific configuration conveniently provided by
   *                              ServerPackCreator. This is the global configuration of the addon
   *                              which provides the ConfigPanelExtension to ServerPackCreator.
   * @param extensionName         The name the titled border of this ConfigPanel will get.
   * @param pluginID              The ID of the addon providing this extension implementation. The
   *                              pluginID determines which extension specific configurations are
   *                              provided to this panel, and how they are stored in a given
   *                              serverpackcreator.conf.
   * @author Griefed
   */
  protected ExtensionConfigPanel(
      final VersionMeta versionMeta,
      final ApplicationProperties applicationProperties,
      final Utilities utilities,
      final TabCreateServerPack tabCreateServerPack,
      final Optional<CommentedConfig> addonConfig,
      final String extensionName,
      final String pluginID) {

    super();
    VERSIONMETA = versionMeta;
    APPLICATIONPROPERTIES = applicationProperties;
    UTILITIES = utilities;
    SERVERPACK_CONFIGURATION_TAB = tabCreateServerPack;
    ADDON_CONFIG = addonConfig;
    EXTENSION_ID = pluginID;
    setBorder(BorderFactory.createTitledBorder(extensionName));
  }

  /**
   * The global configuration of the addon which provides this ConfigPanel extension. Wrapped in an
   * {@link Optional}, so you can check beforehand whether a config is available.
   *
   * @return The global addon configuration, wrapped in an Optional.
   * @author Griefed
   */
  protected final Optional<CommentedConfig> getAddonConfig() {
    return ADDON_CONFIG;
  }

  /**
   * The ID of the addon providing this extension implementation. The pluginID determines which
   * extension specific configurations are provided to this panel, and how they are stored in a
   * given serverpackcreator.conf.
   *
   * @return The ID of the addon which provides this extension implementation.
   * @author Griefed
   */
  public final String pluginID() {
    return EXTENSION_ID;
  }

  /**
   * Retrieve this extensions server pack specific configuration. When no configuration with configs
   * for this extension has been loaded yet, the returned list is empty. Fill it with life!
   *
   * @return Config list to be used in subsequent server pack generation runs, by various other
   * extensions.
   * @author Griefed
   */
  public abstract ArrayList<CommentedConfig> serverPackExtensionConfig();

  /**
   * Pass the extension configuration to the configuration panel so it can then, in turn, load the
   * available configurations and make them editable, if so desired.
   *
   * @param serverPackExtensionConfig The list of extension configurations to pass to the
   *                                  configuration panel.
   * @author Griefed
   */
  public abstract void setServerPackExtensionConfig(
      ArrayList<CommentedConfig> serverPackExtensionConfig);

  /**
   * Get the tab in which this ConfigPanel resides in, giving you access to various fields for
   * further operations.
   *
   * @return The server pack config tab in which this ConfigPanel resides in.
   * @author Griefed
   */
  protected final TabCreateServerPack getTabCreateServerPack() {
    return SERVERPACK_CONFIGURATION_TAB;
  }

  /**
   * Get the version meta used by ServerPackCreator, giving you access to Minecraft, Forge, Fabric,
   * LegacyFabric and Quilt versions.
   *
   * @return The version meta used by ServerPackCreator
   * @author Griefed
   */
  protected final VersionMeta getVersionMeta() {
    return VERSIONMETA;
  }

  /**
   * Get the application properties which make up the current configuration of ServerPackCreator.
   * The application properties contains various settings like the fallback list of clientside-only
   * mods, the server pack directory in which server packs are generated and stored in, and more.
   *
   * @return The application properties which make up the current configuration of
   * ServerPackCreator.
   * @author Griefed
   */
  protected final ApplicationProperties getApplicationProperties() {
    return APPLICATIONPROPERTIES;
  }

  /**
   * Common utilities used across ServerPackCreator.
   *
   * @return Utilities used across ServerPackCreator.
   * @author Griefed
   */
  protected final Utilities getUtilities() {
    return UTILITIES;
  }

  /**
   * Clear the interface, or in other words, reset this extensions config panel UI. If your Config
   * Panel Extensions has no elements you wish to reset, then simply overwrite this method with an
   * empty method body.<br><br> The {@code clear()}-method is called when the owning
   * {@code TabCreateServerPack.clearInterface()}-method is called.
   */
  public abstract void clear();
}
