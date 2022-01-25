package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.plugins.serverpackhandler.ServerPackArchiveCreated;
import de.griefed.serverpackcreator.plugins.serverpackhandler.ServerPackCreated;
import de.griefed.serverpackcreator.plugins.serverpackhandler.ServerPackStart;
import de.griefed.serverpackcreator.plugins.swinggui.AddTabbedPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pf4j.JarPluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApplicationPlugins extends JarPluginManager {

    private static final Logger LOG = LogManager.getLogger(ApplicationPlugins.class);

    private final ApplicationProperties APPLICATIONPROPERTIES;

    public final List<ServerPackStart> PLUGINS_SERVERPACKSTART;
    public final List<ServerPackCreated> PLUGINS_SERVERPACKCREATED;
    public final List<ServerPackArchiveCreated> PLUGINS_SERVERPACKARCHIVECREATED;
    public final List<AddTabbedPane> PLUGINS_TABBEDPANE;

    @Autowired
    public ApplicationPlugins(ApplicationProperties injectedApplicationProperties) {
        if (injectedApplicationProperties == null) {
            this.APPLICATIONPROPERTIES = new ApplicationProperties();
        } else {
            this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        }

        this.pluginsRoots.add(APPLICATIONPROPERTIES.PLUGINS_DIRECTORY);

        loadPlugins();
        startPlugins();

        this.PLUGINS_SERVERPACKSTART = getExtensions(ServerPackStart.class);
        this.PLUGINS_SERVERPACKCREATED = getExtensions(ServerPackCreated.class);
        this.PLUGINS_SERVERPACKARCHIVECREATED = getExtensions(ServerPackArchiveCreated.class);
        this.PLUGINS_TABBEDPANE = getExtensions(AddTabbedPane.class);

        availablePluginsAndExtensions();
    }

    private void availablePluginsAndExtensions() {
        LOG.info("Available ServerPackStart plugins:");
        for (ServerPackStart start : PLUGINS_SERVERPACKSTART) {
            LOG.info("Name:       " + start.getName());
            LOG.info("Description:" + start.getDescription());
            LOG.info("Version:    " + start.getVersion());
            LOG.info("Author:     " + start.getAuthor());
        }


        LOG.info("Available ServerPackCreated plugins:");
        for (ServerPackCreated created : PLUGINS_SERVERPACKCREATED) {
            LOG.info("Name:       " + created.getName());
            LOG.info("Description:" + created.getDescription());
            LOG.info("Version:    " + created.getVersion());
            LOG.info("Author:     " + created.getAuthor());
        }


        LOG.info("Available ServerPackArchiveCreated plugins:");
        for (ServerPackArchiveCreated archive : PLUGINS_SERVERPACKARCHIVECREATED) {
            LOG.info("Name:       " + archive.getName());
            LOG.info("Description:" + archive.getDescription());
            LOG.info("Version:    " + archive.getVersion());
            LOG.info("Author:     " + archive.getAuthor());
        }


        LOG.info("Available AddTabbedPane plugins:");
        for (AddTabbedPane pane : PLUGINS_TABBEDPANE) {
            LOG.info("Name:       " + pane.getName());
            LOG.info("Description:" + pane.getDescription());
            LOG.info("Version:    " + pane.getVersion());
            LOG.info("Author:     " + pane.getAuthor());
        }
    }
}
