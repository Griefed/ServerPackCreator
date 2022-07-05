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

import de.griefed.serverpackcreator.plugins.serverpackhandler.PostGenExtension;
import de.griefed.serverpackcreator.plugins.serverpackhandler.PreGenExtension;
import de.griefed.serverpackcreator.plugins.serverpackhandler.PreZipExtension;
import de.griefed.serverpackcreator.plugins.swinggui.TabExtension;
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
public class ApplicationPlugins extends JarPluginManager {

  private static final Logger LOG = LogManager.getLogger(ApplicationPlugins.class);

  private final List<PreGenExtension> PLUGINS_PreGenExtension;
  private final List<PreZipExtension> PLUGINS_PreZipExtension;
  private final List<PostGenExtension> PLUGINS_PostGenExtension;
  private final List<TabExtension> PLUGINS_TabExtension;

  /**
   * Constructor
   *
   * @author Griefed
   */
  @Autowired
  public ApplicationPlugins() {

    LOG.info(
        "Plugins directory: "
            + new File(System.getProperty("pf4j.pluginsDir", "plugins")).getAbsolutePath());

    loadPlugins();
    startPlugins();

    this.PLUGINS_PreGenExtension = getExtensions(PreGenExtension.class);
    this.PLUGINS_PreZipExtension = getExtensions(PreZipExtension.class);
    this.PLUGINS_PostGenExtension = getExtensions(PostGenExtension.class);
    this.PLUGINS_TabExtension = getExtensions(TabExtension.class);

    availablePluginsAndExtensions();
  }

  /**
   * Print information about available plugins to our logs.
   *
   * @author Griefed
   */
  private void availablePluginsAndExtensions() {

    if (PLUGINS_PreGenExtension.isEmpty()
        && PLUGINS_PreZipExtension.isEmpty()
        && PLUGINS_PostGenExtension.isEmpty()
        && PLUGINS_TabExtension.isEmpty()) {

      LOG.info("No plugins installed.");
      return;
    }

    if (!PLUGINS_PreGenExtension.isEmpty()) {
      LOG.info("Available PreGenExtension plugins:");
      PLUGINS_PreGenExtension.forEach(
          plugin -> {
            LOG.info("Name:       " + plugin.getName());
            LOG.info("Description:" + plugin.getDescription());
            LOG.info("Version:    " + plugin.getVersion());
            LOG.info("Author:     " + plugin.getAuthor());
          });
    } else {
      LOG.info("No PreGenExtensions installed.");
    }

    if (!PLUGINS_PreZipExtension.isEmpty()) {
      LOG.info("Available PreZipExtension plugins:");
      PLUGINS_PreZipExtension.forEach(
          plugin -> {
            LOG.info("Name:       " + plugin.getName());
            LOG.info("Description:" + plugin.getDescription());
            LOG.info("Version:    " + plugin.getVersion());
            LOG.info("Author:     " + plugin.getAuthor());
          });
    } else {
      LOG.info("No PreZipExtension installed.");
    }

    if (!PLUGINS_PostGenExtension.isEmpty()) {
      LOG.info("Available PostGenExtension plugins:");
      PLUGINS_PostGenExtension.forEach(
          plugin -> {
            LOG.info("Name:       " + plugin.getName());
            LOG.info("Description:" + plugin.getDescription());
            LOG.info("Version:    " + plugin.getVersion());
            LOG.info("Author:     " + plugin.getAuthor());
          });
    } else {
      LOG.info("No PostGenExtension installed.");
    }

    if (!PLUGINS_TabExtension.isEmpty()) {
      LOG.info("Available TabExtension plugins:");
      PLUGINS_TabExtension.forEach(
          plugin -> {
            LOG.info("Name:       " + plugin.getName());
            LOG.info("Description:" + plugin.getDescription());
            LOG.info("Version:    " + plugin.getVersion());
            LOG.info("Author:     " + plugin.getAuthor());
          });
    } else {
      LOG.info("No TabExtension installed.");
    }
  }

  /**
   * List of available {@link PreGenExtension}-plugins.
   *
   * @return List of available {@link PreGenExtension}-plugins.
   * @author Griefed
   */
  public List<PreGenExtension> pluginsPreGenExtension() {
    return PLUGINS_PreGenExtension;
  }

  /**
   * List of available {@link PreZipExtension}-plugins.
   *
   * @return List of available {@link PreZipExtension}-plugins.
   * @author Griefed
   */
  public List<PreZipExtension> pluginsPreZipExtension() {
    return PLUGINS_PreZipExtension;
  }

  /**
   * List of available {@link PostGenExtension}-plugins.
   *
   * @return List of available {@link PostGenExtension}-plugins.
   * @author Griefed
   */
  public List<PostGenExtension> pluginsPostGenExtension() {
    return PLUGINS_PostGenExtension;
  }

  /**
   * List of available {@link TabExtension}-plugins.
   *
   * @return List of available {@link TabExtension}-plugins.
   * @author Griefed
   */
  public List<TabExtension> pluginsTabExtension() {
    return PLUGINS_TabExtension;
  }
}
