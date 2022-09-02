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
package de.griefed.serverpackcreator.addons.configurationhandler;

import com.electronwill.nightconfig.core.CommentedConfig;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.addons.BaseInformation;
import java.util.List;

/**
 * Extension point for configuration checks, so you can run your own checks on a given
 * {@link de.griefed.serverpackcreator.ConfigurationModel} should you so desire.
 *
 * @author Girefed
 */
public interface ConfigCheckExtension extends BaseInformation {

  /**
   * @param configurationModel  The configuration to check.
   * @param encounteredErrors   A list of encountered errors during any and all checks. The list is
   *                            displayed to the user if it contains any entries.
   * @param extensionConfig     Configuration for this addon, conveniently provided by
   *                            ServerPackCreator.
   * @param packSpecificConfigs Modpack and server pack specific configurations for this addon,
   *                            conveniently provided by ServerPackCreator.
   * @return <code>true</code> if an error was encountered. <code>false</code> if the checks were
   * successful.
   * @throws Exception if any unexpected error is encountered during the execution of this method.
   * @author Griefed
   */
  boolean runCheck(final ConfigurationModel configurationModel,
      final List<String> encounteredErrors, CommentedConfig extensionConfig,
      List<CommentedConfig> packSpecificConfigs)
      throws Exception;
}
