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
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Plugin;
import org.pf4j.PluginManager;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginWrapper;

/**
 * A ServerPackCreator addon provides additional functionality to ServerPackCreator via any of
 * <ul>
 *   <li>{@link de.griefed.serverpackcreator.addons.configurationhandler.ConfigCheckExtension}</li>
 *   <li>{@link de.griefed.serverpackcreator.addons.serverpackhandler.PostGenExtension}</li>
 *   <li>{@link de.griefed.serverpackcreator.addons.serverpackhandler.PreGenExtension}</li>
 *   <li>{@link de.griefed.serverpackcreator.addons.serverpackhandler.PreZipExtension}</li>
 *   <li>{@link de.griefed.serverpackcreator.addons.swinggui.ConfigPanelExtension}</li>
 *   <li>{@link de.griefed.serverpackcreator.addons.swinggui.TabExtension}</li>
 * </ul>
 * and
 * <ul>
 *   <li>{@link de.griefed.serverpackcreator.addons.swinggui.ExtensionConfigPanel}</li>
 *   <li>{@link de.griefed.serverpackcreator.addons.swinggui.ExtensionTab}</li>
 * </ul>
 * <p>
 * For details on pf4j, the library used to realize the addon-functionality in ServerPackCreator, visit <a href="https://pf4j.org">pf4j.org</a>
 *
 * @author Griefed
 */
@SuppressWarnings("unused")
public abstract class ServerPackCreatorAddon extends Plugin implements BaseInformation {

  protected static final Logger LOG_ADDONS = LogManager.getLogger("AddonsLogger");
  private static final Logger LOG = LogManager.getLogger(ServerPackCreatorAddon.class);
  private final String NAME;
  private final String DESCRIPTION;
  private final String AUTHOR;
  private final String VERSION;

  /**
   * Using the pre-made ServerPackCreatorAddon-class and extending it, you can save yourself the
   * hassle of writing the code which provides ServerPackCreator with information about your addon.
   * <p>
   * The only thing you need to take care of is your {@code build.gradle} and the
   * {@code Name},{@code Description},{@code Author} and {@code Version} fields.
   *
   * @param wrapper PluginWrapper provided by ServerPackCreator. Do not touch unless you know what
   *                you are doing.
   * @throws IOException if the addon could not be initialized.
   */
  public ServerPackCreatorAddon(@NotNull final PluginWrapper wrapper) throws IOException {
    super(wrapper);

    String classPath =
        Objects.requireNonNull(
            this.getClass().getResource(this.getClass().getSimpleName() + ".class")).toString();

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
   * make sure to call {@code super.start()} first.
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
   * make sure to call {@code super.start()} first.
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

  public @NotNull String getId() {
    return getWrapper().getPluginId();
  }

  @Override
  public @NotNull String getName() {
    return NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public @NotNull String getAuthor() {
    return AUTHOR;
  }

  @Override
  public @NotNull String getVersion() {
    return VERSION;
  }
}
