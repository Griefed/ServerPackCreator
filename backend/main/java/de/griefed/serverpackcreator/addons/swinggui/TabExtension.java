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
import de.griefed.serverpackcreator.addons.BaseInformation;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JPanel;

/**
 * Extension point for addons which add additional {@link JPanel}s as additional tabs to the
 * ServerPackCreator GUI.
 *
 * @author Griefed
 */
public interface TabExtension extends BaseInformation {

  /**
   * Get the {@link JPanel} from this addon to add it to the ServerPackCreator GUI as an additional
   * tab.
   *
   * @param extensionConfig     Configuration for this addon, conveniently provided by
   *                            ServerPackCreator.
   * @param packSpecificConfigs Modpack and server pack specific configurations for this addon,
   *                            conveniently provided by ServerPackCreator.
   * @return Component to add to the ServerPackCreator GUI as a tab.
   * @author Griefed
   */
  JPanel getTab(CommentedConfig extensionConfig, List<CommentedConfig> packSpecificConfigs);

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
