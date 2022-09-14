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
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class to extend from if you want to add your own tabs to the ServerPackCreator GUI.
 *
 * @author Griefed
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class ExtensionTab extends JPanel {

  protected static final Logger LOG_ADDONS = LogManager.getLogger("AddonsLogger");
  protected final Optional<CommentedConfig> ADDON_CONFIG;
  private final VersionMeta VERSIONMETA;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final Utilities UTILITIES;
  private final Optional<File> CONFIG_FILE;

  /**
   * Construct a new panel to add to the ServerPackCreator GUI as an additional tab.
   *
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
   */
  protected ExtensionTab(
      final VersionMeta versionMeta,
      final ApplicationProperties applicationProperties,
      final Utilities utilities,
      final Optional<CommentedConfig> addonConfig,
      final Optional<File> configFile) {

    super();
    VERSIONMETA = versionMeta;
    APPLICATIONPROPERTIES = applicationProperties;
    UTILITIES = utilities;
    ADDON_CONFIG = addonConfig;
    CONFIG_FILE = configFile;
  }

  /**
   * Save the current configuration of this addon to the overlying addons config-file. Requires your
   * addon to register a global configuration-file. If your addon does not provide a
   * configuration-file, this method will not store or save anything. No file will be created. Your
   * addon <strong>MUST</strong> provide the initial configuration-file.
   *
   * @author Griefed
   */
  protected final void saveConfiguration() {
    SwingUtilities.invokeLater(() -> {
      if (ADDON_CONFIG.isPresent() && CONFIG_FILE.isPresent()) {

        TomlFormat.instance().createWriter()
            .write(ADDON_CONFIG.get(), CONFIG_FILE.get(), WritingMode.REPLACE,
                StandardCharsets.UTF_8);

        LOG_ADDONS.info("Configuration saved.");

      } else {
        LOG_ADDONS.info("No configuration or configuration file available.");
      }
    });
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
}
