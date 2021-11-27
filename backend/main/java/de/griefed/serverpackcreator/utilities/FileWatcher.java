/* Copyright (C) 2021  Griefed
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
package de.griefed.serverpackcreator.utilities;

import de.griefed.serverpackcreator.AddonsHandler;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.DefaultFiles;
import de.griefed.serverpackcreator.Main;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Simple FileWatcher which monitors whether <code>serverpackcreator.properties</code>, <code>server.properties</code> or
 * <code>server-icon.png</code> were deleted. If any of the aforementioned files gets deleted, the default is restored.
 * In the case of <code>serverpackcreator.properties</code>, defaults are then loaded.
 * @author Griefed
 */
@Component
public class FileWatcher {

    private static final Logger LOG = LogManager.getLogger(FileWatcher.class);

    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final DefaultFiles DEFAULTFILES;
    private final VersionLister VERSIONLISTER;
    private final AddonsHandler ADDONSHANDLER;
    private final LocalizationManager LOCALIZATIONMANAGER;

    /**
     * Constructor creating our FileWatcher.
     * @author Griefed
     */
    @Autowired
    public FileWatcher(ApplicationProperties injectedApplicationProperties, DefaultFiles injectedDefaultFiles, VersionLister injectedVersionLister, AddonsHandler injectedAddonsHandler, LocalizationManager injectedLocalitationManager) {

        this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        this.DEFAULTFILES = injectedDefaultFiles;
        this.VERSIONLISTER = injectedVersionLister;
        this.ADDONSHANDLER = injectedAddonsHandler;
        this.LOCALIZATIONMANAGER = injectedLocalitationManager;

        FileAlterationObserver FILEALTERATIONOBSERVER = new FileAlterationObserver(new File("."));

        FileAlterationListener FILEALTERATIONLISTENER = new FileAlterationListener() {
            @Override
            public void onStart(FileAlterationObserver observer) {}

            @Override
            public void onDirectoryCreate(File directory) {}

            @Override
            public void onDirectoryChange(File directory) {}

            @Override
            public void onDirectoryDelete(File directory) {}

            @Override
            public void onFileCreate(File file) {}

            @Override
            public void onFileChange(File file) {}

            @Override
            public void onFileDelete(File file) {
                if (!file.toString().replace("\\","/").startsWith("./server-packs") &&
                        !file.toString().replace("\\","/").startsWith("./work/modpacks")) {

                    if (check(file, APPLICATIONPROPERTIES.FILE_SERVERPACKCREATOR_PROPERTIES)) {

                        createFile(APPLICATIONPROPERTIES.FILE_SERVERPACKCREATOR_PROPERTIES);
                        APPLICATIONPROPERTIES.reload();
                        LOG.info("Restored serverpackcreator.properties and loaded defaults.");

                    } else if (check(file, APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES)) {

                        DEFAULTFILES.checkForFile(APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES);
                        LOG.info("Restored default server.properties.");

                    } else if (check(file, APPLICATIONPROPERTIES.FILE_SERVER_ICON)) {

                        DEFAULTFILES.checkForFile(APPLICATIONPROPERTIES.FILE_SERVER_ICON);
                        LOG.info("Restored default server-icon.png.");
                    }
                }
            }

            @Override
            public void onStop(FileAlterationObserver observer) {}

            private boolean check(File watched, File toCreate) {
                return watched.toString().replace("\\","/").substring(watched.toString().replace("\\","/").lastIndexOf("/") + 1).equals(toCreate.toString());
            }

            private void createFile(File toCreate) {
                try {
                    FileUtils.copyInputStreamToFile(
                            Objects.requireNonNull(Main.class.getResourceAsStream("/" + toCreate.getName())),
                            toCreate);

                } catch (IOException ex) {
                    LOG.error("Error creating file: " + toCreate, ex);
                }
            }
        };
        FILEALTERATIONOBSERVER.addListener(FILEALTERATIONLISTENER);

        FileAlterationMonitor FILEALTERATIONMONITOR = new FileAlterationMonitor(1000);
        FILEALTERATIONMONITOR.addObserver(FILEALTERATIONOBSERVER);

        try {
            FILEALTERATIONMONITOR.start();
        } catch (Exception ex) {
            LOG.error("Error starting the FileWatcher Monitor.", ex);
        }
    }
}
