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
package de.griefed.serverpackcreator.plugins.swinggui;

import de.griefed.serverpackcreator.plugins.PluginInformation;
import de.griefed.serverpackcreator.utilities.misc.Generated;

import javax.swing.*;

/**
 * Plugin interface for plugins which are to be executed after the ServerPackCreator GUI have been initialized. This
 * interface allows plugins to add additional {@link JComponent}s as additional tabs.
 *
 * @author Griefed
 */
@Generated
public interface TabExtension extends PluginInformation {

    /**
     * Get the {@link JTabbedPane} from this addon to add it to the ServerPackCreator GUI.
     *
     * @return {@link JTabbedPane}.
     * @author Griefed
     */
    JComponent getTab();

    /**
     * Get the {@link Icon} for this tabbed pane addon to display to the ServerPackCreator GUI.
     *
     * @return {@link Icon}.
     * @author Griefed
     */
    Icon getTabIcon();

    /**
     * Get the title of this tabbed pane addon to display in the ServerPackCreator GUI.
     *
     * @return String. The title of this addons tabbed pane.
     * @author Griefed
     */
    String getTabTitle();

    /**
     * Get the tooltip for this tabbed pane addon to display in the ServerPackCreator GUI.
     *
     * @return String. The tooltip of this addons tabbed pane.
     * @author Griefed
     */
    String getTabTooltip();
}
