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

import de.griefed.serverpackcreator.addons.configurationhandler.ConfigCheckExtension;
import de.griefed.serverpackcreator.addons.serverpackhandler.PostGenExtension;
import de.griefed.serverpackcreator.addons.serverpackhandler.PreGenExtension;
import de.griefed.serverpackcreator.addons.serverpackhandler.PreZipExtension;
import de.griefed.serverpackcreator.addons.swinggui.ConfigPaneExtension;
import de.griefed.serverpackcreator.addons.swinggui.TabExtension;
import java.io.File;
import java.util.List;
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

  private final List<PreGenExtension> PRE_GEN_EXTENSIONS;
  private final List<PreZipExtension> PRE_ZIP_EXTENSIONS;
  private final List<PostGenExtension> POST_GEN_EXTENSIONS;
  private final List<TabExtension> TAB_EXTENSIONS;
  private final List<ConfigPaneExtension> CONFIG_PANE_EXTENSIONS;
  private final List<ConfigCheckExtension> CONFIG_CHECK_EXTENSIONS;

  /**
   * Constructor
   *
   * @author Griefed
   */
  @Autowired
  public ApplicationAddons() {

    LOG.info(
        "Plugins directory: "
            + new File(System.getProperty("pf4j.pluginsDir", "plugins")).getAbsolutePath());

    loadPlugins();
    startPlugins();

    PRE_GEN_EXTENSIONS = getExtensions(PreGenExtension.class);
    PRE_ZIP_EXTENSIONS = getExtensions(PreZipExtension.class);
    POST_GEN_EXTENSIONS = getExtensions(PostGenExtension.class);
    TAB_EXTENSIONS = getExtensions(TabExtension.class);
    CONFIG_PANE_EXTENSIONS = getExtensions(ConfigPaneExtension.class);
    CONFIG_CHECK_EXTENSIONS = getExtensions(ConfigCheckExtension.class);

    availableExtensions();
  }

  /**
   * Print information about available plugins to our logs.
   *
   * @author Griefed
   */
  private void availableExtensions() {

    if (PRE_GEN_EXTENSIONS.isEmpty()
        && PRE_ZIP_EXTENSIONS.isEmpty()
        && POST_GEN_EXTENSIONS.isEmpty()
        && TAB_EXTENSIONS.isEmpty()
        && CONFIG_PANE_EXTENSIONS.isEmpty()
        && CONFIG_CHECK_EXTENSIONS.isEmpty()) {

      LOG.info("No extensions installed.");
      return;
    }

    if (!PRE_GEN_EXTENSIONS.isEmpty()) {
      LOG.info("Available PreGenExtension extensions:");
      PRE_GEN_EXTENSIONS.forEach(
          extension -> {
            LOG.info("  Name:       " + extension.getName());
            LOG.info("    Description:" + extension.getDescription());
            LOG.info("    Version:    " + extension.getVersion());
            LOG.info("    Author:     " + extension.getAuthor());
          });
    } else {
      LOG.info("No PreGenExtensions installed.");
    }

    if (!PRE_ZIP_EXTENSIONS.isEmpty()) {
      LOG.info("Available PreZipExtension extensions:");
      PRE_ZIP_EXTENSIONS.forEach(
          extension -> {
            LOG.info("  Name:       " + extension.getName());
            LOG.info("    Description:" + extension.getDescription());
            LOG.info("    Version:    " + extension.getVersion());
            LOG.info("    Author:     " + extension.getAuthor());
          });
    } else {
      LOG.info("No PreZipExtension installed.");
    }

    if (!POST_GEN_EXTENSIONS.isEmpty()) {
      LOG.info("Available PostGenExtension extensions:");
      POST_GEN_EXTENSIONS.forEach(
          extension -> {
            LOG.info("  Name:       " + extension.getName());
            LOG.info("    Description:" + extension.getDescription());
            LOG.info("    Version:    " + extension.getVersion());
            LOG.info("    Author:     " + extension.getAuthor());
          });
    } else {
      LOG.info("No PostGenExtension installed.");
    }

    if (!TAB_EXTENSIONS.isEmpty()) {
      LOG.info("Available TabExtension extensions:");
      TAB_EXTENSIONS.forEach(
          extension -> {
            LOG.info("  Name:       " + extension.getName());
            LOG.info("    Description:" + extension.getDescription());
            LOG.info("    Version:    " + extension.getVersion());
            LOG.info("    Author:     " + extension.getAuthor());
          });
    } else {
      LOG.info("No TabExtension installed.");
    }

    if (!CONFIG_PANE_EXTENSIONS.isEmpty()) {
      LOG.info("Available ConfigPane extensions:");
      CONFIG_PANE_EXTENSIONS.forEach(
          extension -> {
            LOG.info("  Name:       " + extension.getName());
            LOG.info("    Description:" + extension.getDescription());
            LOG.info("    Version:    " + extension.getVersion());
            LOG.info("    Author:     " + extension.getAuthor());
          });
    } else {
      LOG.info("No ConfigPane installed.");
    }

    if (!CONFIG_CHECK_EXTENSIONS.isEmpty()) {
      LOG.info("Available ConfigCheck extensions:");
      CONFIG_CHECK_EXTENSIONS.forEach(
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
    return PRE_GEN_EXTENSIONS;
  }

  /**
   * List of available {@link PreZipExtension}-extensions.
   *
   * @return List of available {@link PreZipExtension}-extensions.
   * @author Griefed
   */
  List<PreZipExtension> preZipExtensions() {
    return PRE_ZIP_EXTENSIONS;
  }

  /**
   * List of available {@link PostGenExtension}-extensions.
   *
   * @return List of available {@link PostGenExtension}-extensions.
   * @author Griefed
   */
  List<PostGenExtension> postGenExtensions() {
    return POST_GEN_EXTENSIONS;
  }

  /**
   * List of available {@link TabExtension}-extensions.
   *
   * @return List of available {@link TabExtension}-extensions.
   * @author Griefed
   */
  public List<TabExtension> tabExtensions() {
    return TAB_EXTENSIONS;
  }

  /**
   * List of available {@link ConfigPaneExtension}-extensions.
   *
   * @return List of available {@link ConfigPaneExtension}-extensions.
   * @author Griefed
   */
  public List<ConfigPaneExtension> configPaneExtensions() {
    return CONFIG_PANE_EXTENSIONS;
  }

  /**
   * List of available {@link ConfigCheckExtension}-extensions.
   *
   * @return List of available {@link ConfigCheckExtension}-extensions.
   * @author Griefed
   */
  List<ConfigCheckExtension> configCheckExtensions() {
    return CONFIG_CHECK_EXTENSIONS;
  }
}
