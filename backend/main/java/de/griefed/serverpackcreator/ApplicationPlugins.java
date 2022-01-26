package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.plugins.serverpackhandler.PostGenExtension;
import de.griefed.serverpackcreator.plugins.serverpackhandler.PreZipExtension;
import de.griefed.serverpackcreator.plugins.serverpackhandler.PreGenExtension;
import de.griefed.serverpackcreator.plugins.swinggui.TabExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.FileUtils;
import org.pf4j.JarPluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
public class ApplicationPlugins extends JarPluginManager {

    private static final Logger LOG = LogManager.getLogger(ApplicationPlugins.class);

    public final List<PreGenExtension> PLUGINS_SERVERPACKSTART;
    public final List<PreZipExtension> PLUGINS_SERVERPACKCREATED;
    public final List<PostGenExtension> PLUGINS_SERVERPACKARCHIVECREATED;
    public final List<TabExtension> PLUGINS_TABBEDPANE;

    @Autowired
    public ApplicationPlugins() {

        LOG.info("Plugins directory: " + new File(System.getProperty("pf4j.pluginsDir", "plugins")).getAbsolutePath());

        try {
            FileUtils.mkdir(new File(System.getProperty("pf4j.pluginsDir", "plugins")), true);
        } catch (IOException ignored) {}

        loadPlugins();
        startPlugins();

        this.PLUGINS_SERVERPACKSTART = getExtensions(PreGenExtension.class);
        this.PLUGINS_SERVERPACKCREATED = getExtensions(PreZipExtension.class);
        this.PLUGINS_SERVERPACKARCHIVECREATED = getExtensions(PostGenExtension.class);
        this.PLUGINS_TABBEDPANE = getExtensions(TabExtension.class);

        availablePluginsAndExtensions();
    }

    private void availablePluginsAndExtensions() {
        LOG.info("Available PreGenExtension plugins:");
        for (PreGenExtension start : PLUGINS_SERVERPACKSTART) {
            LOG.info("Name:       " + start.getName());
            LOG.info("Description:" + start.getDescription());
            LOG.info("Version:    " + start.getVersion());
            LOG.info("Author:     " + start.getAuthor());
        }


        LOG.info("Available PreZipExtension plugins:");
        for (PreZipExtension created : PLUGINS_SERVERPACKCREATED) {
            LOG.info("Name:       " + created.getName());
            LOG.info("Description:" + created.getDescription());
            LOG.info("Version:    " + created.getVersion());
            LOG.info("Author:     " + created.getAuthor());
        }


        LOG.info("Available PostGenExtension plugins:");
        for (PostGenExtension archive : PLUGINS_SERVERPACKARCHIVECREATED) {
            LOG.info("Name:       " + archive.getName());
            LOG.info("Description:" + archive.getDescription());
            LOG.info("Version:    " + archive.getVersion());
            LOG.info("Author:     " + archive.getAuthor());
        }


        LOG.info("Available TabExtension plugins:");
        for (TabExtension pane : PLUGINS_TABBEDPANE) {
            LOG.info("Name:       " + pane.getName());
            LOG.info("Description:" + pane.getDescription());
            LOG.info("Version:    " + pane.getVersion());
            LOG.info("Author:     " + pane.getAuthor());
        }
    }
}
