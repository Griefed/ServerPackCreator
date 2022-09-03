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
package de.griefed.serverpackcreator;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.toml.TomlParser;
import de.griefed.serverpackcreator.addons.configurationhandler.ConfigCheckExtension;
import de.griefed.serverpackcreator.addons.serverpackhandler.PostGenExtension;
import de.griefed.serverpackcreator.addons.serverpackhandler.PreGenExtension;
import de.griefed.serverpackcreator.addons.serverpackhandler.PreZipExtension;
import de.griefed.serverpackcreator.addons.swinggui.ConfigPanelExtension;
import de.griefed.serverpackcreator.addons.swinggui.ExtensionConfigPanel;
import de.griefed.serverpackcreator.addons.swinggui.TabExtension;
import de.griefed.serverpackcreator.swing.TabCreateServerPack;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JTabbedPane;
import net.lingala.zip4j.ZipFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pf4j.JarPluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Manager for ServerPackCreator plugins. In itself it doesn't do much. It gathers lists of all
 * available extensions for {@link TabExtension},{@link PreGenExtension},{@link PreZipExtension} and
 * {@link PostGenExtension} so they can then be run during server pack generation and during
 * initialization of the GUI.
 *
 * @author Griefed
 */
@Component
public final class ApplicationAddons extends JarPluginManager {

  private static final Logger LOG = LogManager.getLogger(ApplicationAddons.class);
  private static final Logger LOG_ADDONS = LogManager.getLogger("AddonsLogger");
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final VersionMeta VERSIONMETA;
  private final Utilities UTILITIES;
  private final HashMap<String, CommentedConfig> ADDON_CONFIGS = new HashMap<>();
  private final HashMap<String, File> ADDON_CONFIG_FILES = new HashMap<>();

  /**
   * Initialize ServerPackCreators addons and give access to any and all available extensions.
   *
   * @param tomlParser                    To read addon-configurations, so they can be provided to
   *                                      extensions.
   * @param injectedApplicationProperties ServerPackCreator settings to be provided to extensions.
   * @param injectedVersionMeta           Version meta to be provided to extensions.
   * @param injectedUtilities             Utilities to be provided to extensions.
   * @author Griefed
   */
  @Autowired
  public ApplicationAddons(TomlParser tomlParser,
      ApplicationProperties injectedApplicationProperties, VersionMeta injectedVersionMeta,
      Utilities injectedUtilities) {

    APPLICATIONPROPERTIES = injectedApplicationProperties;
    VERSIONMETA = injectedVersionMeta;
    UTILITIES = injectedUtilities;

    LOG.info(
        "Addon directory: "
            + new File(System.getProperty("pf4j.pluginsDir", "plugins")).getAbsolutePath());

    loadPlugins();

    startPlugins();

    extractAddonConfigs(tomlParser);

    availableExtensions();
  }

  /**
   * Retrieve the config.toml of an addon and store it in the <code>plugins/config</code>-directory,
   * using the ID of the addon as the name for the extracted file. <code>addon.toml</code>-files
   * must be stored in the root of a addon JAR-file in order for ServerPackCreator to be reliably be
   * able to retrieve it.<br><br>A given addon does not have to provide a config.toml, as not every
   * addons requires a global config. When no file is provided by the addon, no file is extracted.
   * This also means that subsequent runs of any extension provided by the addon do not receive a
   * global, addon-specific, configuration.<br><br>When a config-file has been successfully
   * extracted, it is added to a map with the addons ID, which in turn will be accessed when
   * extensions are run, to retrieve said configuration and pass it to any extensions.
   *
   * @param tomlParser Toml parser to read the config into a {@link CommentedConfig}, mapped to the
   *                   addons ID.
   * @author Griefed
   */
  private void extractAddonConfigs(TomlParser tomlParser) {
    getPlugins().forEach(
        plugin -> {

          String configFile = "plugins/config/" + plugin.getPluginId() + ".toml";

          if (!new File(configFile).exists()) {

            try (ZipFile addonJar = new ZipFile(plugin.getPluginPath().toFile())) {

              addonJar.extractFile("config.toml", configFile);
              registerAddonConfig(tomlParser, plugin.getPluginId(), new File(configFile));

            } catch (Exception ex) {
              LOG.error(
                  "Could not extract config.toml from " + plugin.getPluginPath().toFile().getName()
                      + ". Does it contain a valid config.toml?");
              LOG.debug("", ex);
            }

          }

        });
  }

  /**
   * Parse and register a config.toml of an addon mapped to the addons ID.
   *
   * @param tomlParser  Toml parser to parse the config into a {@link CommentedConfig}.
   * @param addonId     The addons ID to map the config to.
   * @param addonConfig The global configuration file corresponding to the addons ID.
   * @author Griefed
   */
  private void registerAddonConfig(TomlParser tomlParser, String addonId, File addonConfig) {
    try {
      ADDON_CONFIGS.put(addonId, tomlParser.parse(addonConfig, FileNotFoundAction.THROW_ERROR,
          StandardCharsets.UTF_8));
      ADDON_CONFIG_FILES.put(addonId, addonConfig);
    } catch (Exception ex) {
      LOG.error("Could not parse addon config for " + addonId + ", file " + addonConfig.getName(),
          ex);
    }
  }

  /**
   * Get the global addon configuration for an addon of the passed ID. The configuration is wrapped
   * in an {@link Optional}, because an addon may not provide a global configuration. If you intend
   * on using a global configuration for your addon, make sure to check whether it is present before
   * trying to use it!
   *
   * @param addonId The addon ID of the...well...addon.
   * @return The global addon configuration, wrapped in an Optional.
   * @author Griefed
   */
  public Optional<CommentedConfig> getAddonConfig(String addonId) {
    return Optional.ofNullable(ADDON_CONFIGS.get(addonId));
  }

  /**
   * Get the configuration-file for an addon, if it exists. This is wrapped in an {@link Optional},
   * because not every addon may provide a configuration-file to use globally for the relevant
   * addons settings. If you intend on using a global configuration, make sure to check whether the
   * file is present, before moving on!
   *
   * @param addonId The addon ID with which to identify the correct config-file to return.
   * @return The config-file corresponding to the ID of the addon, wrapped in an Optional.
   * @author Griefed
   */
  public Optional<File> getAddonConfigFile(String addonId) {
    return Optional.ofNullable(ADDON_CONFIG_FILES.get(addonId));
  }

  /**
   * Run any and all Pre-Server Pack-Generation extensions, using the passed configuration model and
   * the destination at which the server pack is to be generated and stored at.
   *
   * @param configurationModel The configuration model from which to create the server pack.
   * @param destination        The destination at which the server pack will be generated and stored
   *                           at.
   * @author Griefed
   */
  void runPreGenExtensions(ConfigurationModel configurationModel, String destination) {
    getPlugins().forEach(
        plugin -> {
          if (!plugin.getPluginManager().getExtensions(PreGenExtension.class).isEmpty()) {

            LOG.info("Executing PreGenExtension extensions.");
            LOG_ADDONS.info("Executing PreGenExtension extensions.");

            plugin.getPluginManager().getExtensions(PreGenExtension.class).forEach(
                preGenExt -> {
                  LOG_ADDONS.info("Executing extension " + preGenExt.getName());

                  try {
                    preGenExt.run(
                        APPLICATIONPROPERTIES,
                        configurationModel,
                        destination,
                        getAddonConfig(plugin.getPluginId()),
                        configurationModel.getAddonConfigs(plugin.getPluginId())
                    );
                  } catch (Exception | Error ex) {
                    LOG_ADDONS.error(
                        "Extension " + preGenExt.getName() + " in plugin " + plugin.getPluginId()
                            + " encountered an error.",
                        ex);
                  }
                });

          } else {
            LOG.info("No PreGenExtension extension to execute.");
            LOG_ADDONS.info("No PreGenExtension extension to execute.");
          }
        });
  }

  /**
   * Run any and all Pre-ZIP-archive creation extensions, using the passed configuration model and
   * the destination at which the server pack is to be generated and stored at.
   *
   * @param configurationModel The configuration model from which to create the server pack.
   * @param destination        The destination at which the server pack will be generated and stored
   *                           at.
   * @author Griefed
   */
  void runPreZipExtensions(ConfigurationModel configurationModel, String destination) {
    getPlugins().forEach(
        plugin -> {
          if (!plugin.getPluginManager().getExtensions(PreZipExtension.class).isEmpty()) {

            LOG.info("Executing PreZipExtension extensions.");
            LOG_ADDONS.info("Executing PreZipExtension extensions.");

            plugin.getPluginManager().getExtensions(PreZipExtension.class).forEach(
                preZipExt -> {
                  LOG_ADDONS.info("Executing extension " + preZipExt.getName());

                  try {
                    preZipExt.run(
                        APPLICATIONPROPERTIES,
                        configurationModel,
                        destination,
                        getAddonConfig(plugin.getPluginId()),
                        configurationModel.getAddonConfigs(plugin.getPluginId())
                    );
                  } catch (Exception | Error ex) {
                    LOG_ADDONS.error(
                        "Extension " + preZipExt.getName() + " in plugin " + plugin.getPluginId()
                            + " encountered an error.",
                        ex);
                  }
                }
            );

          } else {
            LOG.info("No PreZipExtension extension to execute.");
            LOG_ADDONS.info("No PreZipExtension extension to execute.");
          }
        }
    );
  }

  /**
   * Run any and all Post-server pack-generation extensions, using the passed configuration model
   * and the destination at which the server pack is to be generated and stored at.
   *
   * @param configurationModel The configuration model from which to create the server pack.
   * @param destination        The destination at which the server pack will be generated and stored
   *                           at.
   * @author Griefed
   */
  void runPostGenExtensions(ConfigurationModel configurationModel, String destination) {
    getPlugins().forEach(
        plugin -> {
          if (!plugin.getPluginManager().getExtensions(PostGenExtension.class).isEmpty()) {

            LOG.info("Executing PostGenExtension extensions.");
            LOG_ADDONS.info("Executing PostGenExtension extensions.");

            plugin.getPluginManager().getExtensions(PostGenExtension.class).forEach(
                postGenExt -> {
                  LOG_ADDONS.info("Executing extension " + postGenExt.getName());

                  try {
                    postGenExt.run(
                        APPLICATIONPROPERTIES,
                        configurationModel,
                        destination,
                        getAddonConfig(plugin.getPluginId()),
                        configurationModel.getAddonConfigs(plugin.getPluginId())
                    );
                  } catch (Exception | Error ex) {
                    LOG_ADDONS.error(
                        "Extension " + postGenExt.getName() + " in plugin " + plugin.getPluginId()
                            + " encountered an error.",
                        ex);
                  }
                }
            );

          } else {
            LOG.info("No PostGenExtension extension to execute.");
            LOG_ADDONS.info("No PostGenExtension extension to execute.");
          }
        }
    );
  }

  /**
   * Add any and all additional tabs to the ServerPackCreator tabbed pane (main GUI). You may use
   * this to add tabs to your own {@link JTabbedPane}, if you so desire. Could be pretty awesome to
   * have your addons extra tabs in a separate window!
   *
   * @param tabbedPane The tabbed pane to which the additional panels should be added to as tabs.
   * @author Griefed
   */
  public void addTabExtensionTabs(JTabbedPane tabbedPane) {
    getPlugins().forEach(
        plugin -> {
          if (!plugin.getPluginManager().getExtensions(TabExtension.class).isEmpty()) {

            LOG.info("Executing TabExtensions extensions.");
            LOG_ADDONS.info("Executing TabExtensions extensions.");

            plugin.getPluginManager().getExtensions(TabExtension.class).forEach(
                tabExt -> {
                  LOG_ADDONS.info("Executing extension " + tabExt.getName());

                  try {

                    tabbedPane.addTab(
                        tabExt.getTabTitle(),
                        tabExt.getTabIcon(),
                        tabExt.getTab(
                            VERSIONMETA,
                            APPLICATIONPROPERTIES,
                            UTILITIES,
                            getAddonConfig(plugin.getPluginId()),
                            getAddonConfigFile(plugin.getPluginId())
                        ),
                        tabExt.getTabTooltip()
                    );
                  } catch (Exception | Error ex) {
                    LOG_ADDONS.error(
                        "Extension " + tabExt.getName() + " in plugin " + plugin.getPluginId()
                            + " encountered an error.",
                        ex);
                  }
                }
            );

          } else {
            LOG.info("No TabExtension extension to execute.");
            LOG_ADDONS.info("No TabExtension extension to execute.");
          }
        }
    );
  }

  /**
   * Create config panels for the passed server pack configuration tab. Note that this method does
   * <strong>NOT</strong> add the panels to the tab, it only creates them and passes the server
   * pack config tab object-reference to each config panel, so they, in turn, may use any available
   * fields and methods for their own operations. A given server pack config tab needs to add the
   * panels which are returned by this method, so a user may make their configurations accordingly.
   *
   * @param tabCreateServerPack The server pack configuration tab to which the config panels are to
   *                            be added.
   * @return A list of config panels specifically created for the passed server pack
   * configuration-tab.
   * @author Griefed
   */
  public List<ExtensionConfigPanel> getConfigPanels(
      TabCreateServerPack tabCreateServerPack) {
    List<ExtensionConfigPanel> panels = new ArrayList<>();
    getPlugins().forEach(
        plugin -> {
          if (!plugin.getPluginManager().getExtensions(ConfigPanelExtension.class).isEmpty()) {

            LOG.info("Executing ConfigPanelExtension extensions.");
            LOG_ADDONS.info("Executing ConfigPanelExtension extensions.");

            plugin.getPluginManager().getExtensions(ConfigPanelExtension.class).forEach(
                configPanel -> {
                  LOG_ADDONS.info("Executing extension " + configPanel.getName());

                  try {

                    panels.add(
                        configPanel.getPanel(
                            VERSIONMETA,
                            APPLICATIONPROPERTIES,
                            UTILITIES,
                            tabCreateServerPack,
                            getAddonConfig(plugin.getPluginId()),
                            configPanel.getName(),
                            plugin.getPluginId()
                        )
                    );
                  } catch (Exception | Error ex) {
                    LOG_ADDONS.error(
                        "Extension " + configPanel.getName() + " in plugin " + plugin.getPluginId()
                            + " encountered an error.",
                        ex);
                  }
                }
            );

          } else {
            LOG.info("No ConfigPanelExtension extension to execute.");
            LOG_ADDONS.info("No ConfigPanelExtension extension to execute.");
          }
        }
    );
    return panels;
  }

  /**
   * Run any and all configuration-check extensions, using the passed configuration model and the
   * destination at which the server pack is to be generated and stored at.
   *
   * @param configurationModel The configuration model containing the server pack and addon
   *                           configurations to check.
   * @param encounteredErrors  A list of encountered errors to add to in case anything goes wrong.
   *                           This list is displayed to the user after am unsuccessful server pack
   *                           generation to help them figure out what went wrong.
   * @return <code>true</code> if any custom check detected an error with the configuration.
   * <strong>Only</strong> return <code>false</code> when not a <strong>single</strong> check
   * errored.
   * @author Griefed
   */
  boolean runConfigCheckExtensions(ConfigurationModel configurationModel,
      List<String> encounteredErrors) {
    AtomicBoolean hasError = new AtomicBoolean(false);

    getPlugins().forEach(
        plugin -> {
          if (!plugin.getPluginManager().getExtensions(ConfigCheckExtension.class).isEmpty()) {

            LOG.info("Executing ConfigCheckExtensions extensions.");
            LOG_ADDONS.info("Executing ConfigCheckExtensions extensions.");

            plugin.getPluginManager().getExtensions(ConfigCheckExtension.class).forEach(
                configCheckExt -> {
                  LOG_ADDONS.info("Executing addon " + configCheckExt.getName());

                  try {
                    if (configCheckExt
                        .runCheck(
                            configurationModel,
                            encounteredErrors,
                            getAddonConfig(plugin.getPluginId()),
                            configurationModel.getAddonConfigs(plugin.getPluginId())
                        )
                    ) {
                      hasError.set(true);
                    }
                  } catch (Exception | Error ex) {
                    LOG_ADDONS.error(
                        "Extension " + configCheckExt.getName() + " in plugin "
                            + plugin.getPluginId()
                            + " encountered an error.",
                        ex);
                  }
                }
            );

          } else {
            LOG.info("No ConfigCheckExtension extension to execute.");
            LOG_ADDONS.info("No ConfigCheckExtension extension to execute.");
          }
        }
    );
    return hasError.get();
  }

  /**
   * Print information about available plugins to our logs.
   *
   * @author Griefed
   */
  private void availableExtensions() {

    if (preGenExtensions().isEmpty()
        && preZipExtensions().isEmpty()
        && postGenExtensions().isEmpty()
        && tabExtensions().isEmpty()
        && configPanelExtensions().isEmpty()
        && configCheckExtensions().isEmpty()) {

      LOG.info("No extensions installed.");
      return;
    }

    if (!preGenExtensions().isEmpty()) {
      LOG.info("Available PreGenExtension extensions:");
      preGenExtensions().forEach(
          extension -> {
            LOG.info("  Name:       " + extension.getName());
            LOG.info("    Description:" + extension.getDescription());
            LOG.info("    Version:    " + extension.getVersion());
            LOG.info("    Author:     " + extension.getAuthor());
          });
    } else {
      LOG.info("No PreGenExtensions installed.");
    }

    if (!preZipExtensions().isEmpty()) {
      LOG.info("Available PreZipExtension extensions:");
      preZipExtensions().forEach(
          extension -> {
            LOG.info("  Name:       " + extension.getName());
            LOG.info("    Description:" + extension.getDescription());
            LOG.info("    Version:    " + extension.getVersion());
            LOG.info("    Author:     " + extension.getAuthor());
          });
    } else {
      LOG.info("No PreZipExtension installed.");
    }

    if (!postGenExtensions().isEmpty()) {
      LOG.info("Available PostGenExtension extensions:");
      postGenExtensions().forEach(
          extension -> {
            LOG.info("  Name:       " + extension.getName());
            LOG.info("    Description:" + extension.getDescription());
            LOG.info("    Version:    " + extension.getVersion());
            LOG.info("    Author:     " + extension.getAuthor());
          });
    } else {
      LOG.info("No PostGenExtension installed.");
    }

    if (!tabExtensions().isEmpty()) {
      LOG.info("Available TabExtension extensions:");
      tabExtensions().forEach(
          extension -> {
            LOG.info("  Name:       " + extension.getName());
            LOG.info("    Description:" + extension.getDescription());
            LOG.info("    Version:    " + extension.getVersion());
            LOG.info("    Author:     " + extension.getAuthor());
          });
    } else {
      LOG.info("No TabExtension installed.");
    }

    if (!configPanelExtensions().isEmpty()) {
      LOG.info("Available ConfigPane extensions:");
      configPanelExtensions().forEach(
          extension -> {
            LOG.info("  Name:       " + extension.getName());
            LOG.info("    Description:" + extension.getDescription());
            LOG.info("    Version:    " + extension.getVersion());
            LOG.info("    Author:     " + extension.getAuthor());
          });
    } else {
      LOG.info("No ConfigPane installed.");
    }

    if (!configCheckExtensions().isEmpty()) {
      LOG.info("Available ConfigCheck extensions:");
      configCheckExtensions().forEach(
          extension -> {
            LOG.info("  Name:       " + extension.getName());
            LOG.info("    Description:" + extension.getDescription());
            LOG.info("    Version:    " + extension.getVersion());
            LOG.info("    Author:     " + extension.getAuthor());
          });
    } else {
      LOG.info("No ConfigCheck installed.");
    }
  }

  /**
   * List of available {@link PreGenExtension}-extensions.
   *
   * @return List of available {@link PreGenExtension}-extensions.
   * @author Griefed
   */
  List<PreGenExtension> preGenExtensions() {
    return getExtensions(PreGenExtension.class);
  }

  /**
   * List of available {@link PreZipExtension}-extensions.
   *
   * @return List of available {@link PreZipExtension}-extensions.
   * @author Griefed
   */
  List<PreZipExtension> preZipExtensions() {
    return getExtensions(PreZipExtension.class);
  }

  /**
   * List of available {@link PostGenExtension}-extensions.
   *
   * @return List of available {@link PostGenExtension}-extensions.
   * @author Griefed
   */
  List<PostGenExtension> postGenExtensions() {
    return getExtensions(PostGenExtension.class);
  }

  /**
   * List of available {@link TabExtension}-extensions.
   *
   * @return List of available {@link TabExtension}-extensions.
   * @author Griefed
   */
  public List<TabExtension> tabExtensions() {
    return getExtensions(TabExtension.class);
  }

  /**
   * List of available {@link ConfigPanelExtension}-extensions.
   *
   * @return List of available {@link ConfigPanelExtension}-extensions.
   * @author Griefed
   */
  public List<ConfigPanelExtension> configPanelExtensions() {
    return getExtensions(ConfigPanelExtension.class);
  }

  /**
   * List of available {@link ConfigCheckExtension}-extensions.
   *
   * @return List of available {@link ConfigCheckExtension}-extensions.
   * @author Griefed
   */
  List<ConfigCheckExtension> configCheckExtensions() {
    return getExtensions(ConfigCheckExtension.class);
  }
}
