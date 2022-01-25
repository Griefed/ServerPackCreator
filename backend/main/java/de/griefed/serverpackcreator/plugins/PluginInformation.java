package de.griefed.serverpackcreator.plugins;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationModel;
import org.pf4j.ExtensionPoint;

/**
 * Starting point from which all plugin interfaces in ServerPackCreator extend.<br>
 * DO NOT IMPLEMENT THIS CLASS DIRECTLY WHEN WRITING A PLUGIN!<br>
 * Instead, implement any of the interfaces in the sub-packages of <code>de.griefed.serverpackcreator.plugins.*</code>
 * @author Griefed
 */
public interface PluginInformation extends ExtensionPoint {

    /**
     * Run this plugin with the passed {@link ApplicationProperties}, {@link ConfigurationModel} and server pack <code>destination</code>
     * @author Griefed
     * @param applicationProperties Instance of {@link ApplicationProperties} as ServerPackCreator itself uses it.
     * @param configurationModel Instance of {@link ConfigurationModel} for a given server pack.
     * @param destination String. The destination of the server pack.
     * @throws Exception {@link Exception} when an uncaught error occurs in the addon.
     */
    void run(ApplicationProperties applicationProperties, ConfigurationModel configurationModel, String destination) throws Exception;

    /**
     * Get the name of this plugin.
     * @author Griefed
     * @return String. Returns the name of this plugin.
     */
    String getName();

    /**
     * Get the description of this plugin.
     * @author Griefed
     * @return String. Returns the description of this plugin.
     */
    String getDescription();

    /**
     * Get the author of this plugin.
     * @author Griefed
     * @return String. Returns the author of this plugin.
     */
    String getAuthor();

    /**
     * Get the version of this plugin.
     * @author Griefed
     * @return String. Returns the version of this plugin.
     */
    String getVersion();
}
