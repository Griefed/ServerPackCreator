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
package de.griefed.serverpackcreator.spring.task;

import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.ServerPackHandler;
import de.griefed.serverpackcreator.spring.serverpack.ServerPackModel;
import de.griefed.serverpackcreator.spring.serverpack.ServerPackService;
import de.griefed.serverpackcreator.spring.zip.GenerateZip;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <a href="https://dev.to/gotson/how-to-implement-a-task-queue-using-apache-artemis-and-spring-boot-2mme">How to implement a task queue using Apache Artemis and Spring Boot</a><br>
 * Huge Thank You to <a href="https://github.com/gotson">Gauthier</a> for writing the above guide on how to implement a JMS. Without it this implementation of Artemis
 * would have either taken way longer or never happened at all. I managed to translate their Kotlin-code to Java and make
 * the necessary changes to fully implement it in ServerPackCreator.<br>
 * TaskHandler class which determines what to do with all message in our JMS, depending on their task type and content of
 * the message.
 * @author Griefed
 */
@Service
public class TaskHandler {

    private static final Logger LOG = LogManager.getLogger(TaskHandler.class);

    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final ServerPackHandler SERVERPACKHANDLER;
    private final ServerPackService SERVERPACKSERVICE;
    private final StopWatch STOPWATCH_SCANS;

    /**
     * Constructor responsible for our DI.
     * @author Griefed
     * @param injectedConfigurationHandler Instance of {@link ConfigurationHandler}.
     * @param injectedServerPackHandler Instance of {@link ServerPackHandler}.
     * @param injectedServerPackService Instance of {@link ServerPackService}.
     */
    @Autowired
    public TaskHandler(ConfigurationHandler injectedConfigurationHandler,
                       ServerPackHandler injectedServerPackHandler,
                       ServerPackService injectedServerPackService
    ) {
        this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
        this.SERVERPACKHANDLER = injectedServerPackHandler;
        this.SERVERPACKSERVICE = injectedServerPackService;
        this.STOPWATCH_SCANS = new StopWatch();
    }

    /**
     * {@link JmsListener} listening to the destination <code>tasks.background</code> and selector <code>type = 'scan'</code>, so only task
     * that match the <code>scan</code>-type are worked with in this method.<br>
     * If a task is received that matches this type, the CurseForge project and file ID of said task is checked for validity.
     * If the combination is found valid, either a new entry is saved to the database or an already existing one updated,
     * if the existing one has the status <code>Generating</code> and <code>lastModified</code> is bigger than 30 minutes.
     * @author Griefed
     * @param task The task for which to check the CurseForge project and file ID, as well as status.
     */
    @JmsListener(destination = "tasks.background", selector = "type = 'scan'")
    public void handleScan(Task task) {
        LOG.info("Executing task: " + task);

    }

    /**
     * {@link JmsListener} listening to the destination <code>tasks.background</code> and selector <code>type = 'generation'</code>, so only task
     * that match the <code>generation</code>-type are worked with in this method.<br>
     * If a task is received that matches this type, the generation of a new server pack is started.
     * @author Griefed
     * @param task The task with which to generate a server pack from a CurseForge project and file ID.
     */
    @JmsListener(destination = "tasks.background", selector = "type = 'generation'")
    public void handleGeneration(Task task) {
        LOG.info("Executing task: " + task);

        if (task instanceof GenerateZip) {

            LOG.info("Instance of GenerateZip: " + task.uniqueId());

            String[] parameters = ((GenerateZip) task).getZipGenerationProperties().split("&");

            ServerPackModel serverPackModel = new ServerPackModel();

            serverPackModel.setStatus("Generating");
            serverPackModel.setDownloads(0);
            serverPackModel.setConfirmedWorking(0);

            serverPackModel.setFileDiskName(parameters[0]);

            serverPackModel.setModpackDir("./work/modpacks/" + parameters[0]);
            serverPackModel.setMinecraftVersion(parameters[2]);
            serverPackModel.setModLoader(parameters[3]);
            serverPackModel.setModLoaderVersion(parameters[4]);
            serverPackModel.setClientMods(Arrays.asList(parameters[1].split(",")));
            serverPackModel.setJavaPath("");
            serverPackModel.setIncludeServerInstallation(false);

            ServerPackModel pack = null;

            STOPWATCH_SCANS.reset();
            STOPWATCH_SCANS.start();

            List<String> encounteredErrors = new ArrayList<>(100);

            try {

                if (!CONFIGURATIONHANDLER.checkConfiguration(serverPackModel, encounteredErrors, false)) {

                    serverPackModel.setFileName(serverPackModel.getModpackDir().substring(serverPackModel.getModpackDir().lastIndexOf("/") + 1));

                    SERVERPACKSERVICE.insert(serverPackModel);

                    pack = SERVERPACKHANDLER.run(serverPackModel);

                    if (pack != null)
                        SERVERPACKSERVICE.updateServerPackByID(serverPackModel.getId(), pack);

                } else {

                    LOG.error("Configuration check for ZIP-archive " + parameters[0] + " failed.");

                     if (!encounteredErrors.isEmpty()) {
                         LOG.error("Encountered errors: ");
                         for (String error : encounteredErrors)
                             LOG.error(error);
                     }

                }

            } catch (Exception ex) {

                LOG.error("An error occurred generating the server pack for ZIP-archive: " + parameters[0],ex);

                SERVERPACKSERVICE.deleteServerPack(serverPackModel.getId());

                if (!encounteredErrors.isEmpty()) {
                    LOG.error("Encountered errors: ");
                    for (String error : encounteredErrors)
                        LOG.error(error);
                }

            } finally {

                FileUtils.deleteQuietly(new File("./work/modpacks/" + parameters[0]));

                STOPWATCH_SCANS.stop();

                LOG.info("Generation took " + STOPWATCH_SCANS);

                STOPWATCH_SCANS.reset();
            }

        } else {

            LOG.info("This is not the queue you are looking for: " + task.uniqueId());
        }
    }
}
