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
package de.griefed.serverpackcreator.addons;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import de.griefed.serverpackcreator.ServerPackCreator;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pf4j.Plugin;
import org.pf4j.PluginManager;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginWrapper;

public abstract class ServerPackCreatorAddon extends Plugin implements BaseInformation {

  protected static final Logger LOG_ADDONS = LogManager.getLogger("AddonsLogger");
  private static final Logger LOG = LogManager.getLogger(ServerPackCreatorAddon.class);
  private final String NAME;
  private final String DESCRIPTION;
  private final String AUTHOR;
  private final String VERSION;

  public ServerPackCreatorAddon(final PluginWrapper wrapper) throws IOException {
    super(wrapper);

    String classPath =
        this.getClass().getResource(this.getClass().getSimpleName() + ".class").toString();

    CommentedConfig addonToml = TomlFormat.instance().createParser().parse(new URL(
        classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/addon.toml").openStream());

    NAME = addonToml.get("name");
    DESCRIPTION = addonToml.get("description");
    AUTHOR = addonToml.get("author");
    VERSION = addonToml.get("version");

  }

  /**
   * This method is called by the application when the plugin is started. See
   * {@link PluginManager#startPlugin(String)}.<br><br> If you intend on overwriting this method,
   * make sure to call <code>super.start()</code> first.
   *
   * @throws PluginRuntimeException if something goes wrong.
   * @author Griefed
   */
  @Override
  public void start() throws PluginRuntimeException {
    super.start();

    LOG.info("Addon-ID:          " + getId());
    LOG.info("Addon-Name:        " + NAME);
    LOG.info("Addon-Description: " + DESCRIPTION);
    LOG.info("Addon-Author:      " + AUTHOR);
    LOG.info("Addon-Version:     " + VERSION);
    LOG.info("Started: " + NAME + " (" + getId() + ")");

    LOG_ADDONS.info("Addon-ID:          " + getId());
    LOG_ADDONS.info("Addon-Name:        " + NAME);
    LOG_ADDONS.info("Addon-Description: " + DESCRIPTION);
    LOG_ADDONS.info("Addon-Author:      " + AUTHOR);
    LOG_ADDONS.info("Addon-Version:     " + VERSION);
    LOG_ADDONS.info("Started: " + NAME + " (" + getId() + ")");
  }

  /**
   * This method is called by the application when the plugin is stopped. See
   * {@link PluginManager#stopPlugin(String)}.<br><br> If you intend on overwriting this method,
   * make sure to call <code>super.start()</code> first.
   *
   * @throws PluginRuntimeException if something goes wrong.
   * @author Griefed
   */
  @Override
  public void stop() throws PluginRuntimeException {
    super.stop();
    LOG.info("Stopped: " + NAME + " (" + getId() + ")");
    LOG_ADDONS.info("Stopped: " + NAME + " (" + getId() + ")");
  }

  public String getId() {
    return getWrapper().getPluginId();
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public String getAuthor() {
    return AUTHOR;
  }

  @Override
  public String getVersion() {
    return VERSION;
  }
}
