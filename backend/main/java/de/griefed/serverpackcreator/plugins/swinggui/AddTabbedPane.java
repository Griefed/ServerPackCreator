package de.griefed.serverpackcreator.plugins.swinggui;

import de.griefed.serverpackcreator.plugins.PluginInformation;

import javax.swing.*;

/**
 * Plugin interface for plugins which are to be executed after the ServerPackCreator GUI have been initialized. This
 * interface allows plugins to add additional {@link javax.swing.JTabbedPane}.
 * @author Griefed
 */
public interface AddTabbedPane extends PluginInformation {

    /**
     * Get the {@link JTabbedPane} from this addon to add it to the ServerPackCreator GUI.
     * @author Griefed
     * @return {@link JTabbedPane}.
     */
    JTabbedPane getTabbedPane();

    /**
     * Get the {@link Icon} for this tabbed pane addon to display to the ServerPackCreator GUI.
     * @author Griefed
     * @return {@link Icon}.
     */
    Icon getTabbedPaneIcon();

    /**
     * Get the title of this tabbed pane addon to display in the ServerPackCreator GUI.
     * @author Griefed
     * @return String. The title of this addons tabbed pane.
     */
    String getTabbedPaneTitle();

    /**
     * Get the tooltip for this tabbed pane addon to display in the ServerPackCreator GUI.
     * @author Griefed
     * @return String. The tooltip of this addons tabbed pane.
     */
    String getTabbedPaneTooltip();
}
