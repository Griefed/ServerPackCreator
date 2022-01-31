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
package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.plugins.serverpackhandler.PostGenExtension;
import de.griefed.serverpackcreator.plugins.serverpackhandler.PreZipExtension;
import de.griefed.serverpackcreator.plugins.serverpackhandler.PreGenExtension;
import de.griefed.serverpackcreator.plugins.swinggui.TabExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.FileUtils;
import org.pf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Manager for ServerPackCreator plugins. In itself it doesn't do much. It gathers lists of all available extensions for
 * {@link TabExtension},{@link PreGenExtension},{@link PreZipExtension} and {@link PostGenExtension} so they can then be
 * run during server pack generation and during initialization of the GUI.
 * @author Griefed
 */
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

        loadPlugins();
        startPlugins();

        this.PLUGINS_SERVERPACKSTART = getExtensions(PreGenExtension.class);
        this.PLUGINS_SERVERPACKCREATED = getExtensions(PreZipExtension.class);
        this.PLUGINS_SERVERPACKARCHIVECREATED = getExtensions(PostGenExtension.class);
        this.PLUGINS_TABBEDPANE = getExtensions(TabExtension.class);

        availablePluginsAndExtensions();
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        return new SingletonExtensionFactory();
    }

    private void availablePluginsAndExtensions() {
        if (PLUGINS_SERVERPACKSTART.size() > 0) {
            LOG.info("Available PreGenExtension plugins:");
            for (PreGenExtension start : PLUGINS_SERVERPACKSTART) {
                LOG.info("Name:       " + start.getName());
                LOG.info("Description:" + start.getDescription());
                LOG.info("Version:    " + start.getVersion());
                LOG.info("Author:     " + start.getAuthor());
            }
        } else {
            LOG.info("No PreGenExtensions installed.");
        }

        if (PLUGINS_SERVERPACKCREATED.size() > 0) {
            LOG.info("Available PreZipExtension plugins:");
            for (PreZipExtension created : PLUGINS_SERVERPACKCREATED) {
                LOG.info("Name:       " + created.getName());
                LOG.info("Description:" + created.getDescription());
                LOG.info("Version:    " + created.getVersion());
                LOG.info("Author:     " + created.getAuthor());
            }
        } else {
            LOG.info("No PreZipExtension installed.");
        }

        if (PLUGINS_SERVERPACKARCHIVECREATED.size() > 0) {
            LOG.info("Available PostGenExtension plugins:");
            for (PostGenExtension archive : PLUGINS_SERVERPACKARCHIVECREATED) {
                LOG.info("Name:       " + archive.getName());
                LOG.info("Description:" + archive.getDescription());
                LOG.info("Version:    " + archive.getVersion());
                LOG.info("Author:     " + archive.getAuthor());
            }
        } else {
            LOG.info("No PostGenExtension installed.");
        }

        if (PLUGINS_TABBEDPANE.size() > 0) {
            LOG.info("Available TabExtension plugins:");
            for (TabExtension pane : PLUGINS_TABBEDPANE) {
                LOG.info("Name:       " + pane.getName());
                LOG.info("Description:" + pane.getDescription());
                LOG.info("Version:    " + pane.getVersion());
                LOG.info("Author:     " + pane.getAuthor());
            }
        } else {
            LOG.info("No TabExtension installed.");
        }






    }
}
