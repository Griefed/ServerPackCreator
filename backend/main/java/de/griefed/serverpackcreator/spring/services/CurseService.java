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
package de.griefed.serverpackcreator.spring.services;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.ServerPackHandler;
import de.griefed.serverpackcreator.curseforge.InvalidFileException;
import de.griefed.serverpackcreator.curseforge.InvalidModpackException;
import de.griefed.serverpackcreator.spring.models.CurseResponse;
import de.griefed.serverpackcreator.spring.models.ServerPack;
import de.griefed.serverpackcreator.spring.repositories.ServerPackRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Griefed
 */
@Service
public class CurseService {

    private static final Logger LOG = LogManager.getLogger(CurseService.class);

    private final ServerPackHandler SERVERPACKHANDLER;
    private final RunGeneration RUNGENERATION = new RunGeneration();
    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final CurseResponse CURSERESPONSEMODEL;
    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final ServerPackService SERVERPACKSERVICE;
    private final ServerPackRepository SERVERPACKREPOSITORY;

    /**
     *
     * @author Griefed
     * @param injectedServerPackHandler
     * @param injectedConfigurationHandler
     * @param injectedCurseResponse
     * @param injectedApplicationProperties
     */
    @Autowired
    public CurseService(ServerPackHandler injectedServerPackHandler, ConfigurationHandler injectedConfigurationHandler, CurseResponse injectedCurseResponse, ApplicationProperties injectedApplicationProperties, ServerPackService injectedServerpackService, ServerPackRepository injectedServerPackRepository) {
        this.SERVERPACKHANDLER = injectedServerPackHandler;
        this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
        this.CURSERESPONSEMODEL = injectedCurseResponse;
        this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        this.SERVERPACKSERVICE = injectedServerpackService;
        this.SERVERPACKREPOSITORY = injectedServerPackRepository;
    }

    /**
     * Status 0: Already exists<br>
     * Status 1: OK, generating<br>
     * Status 2: Error occurred
     * @author Griefed
     * @param modpack CurseForge projectID and fileID combination.
     * @return String. Statuscode indicating whether the server pack already exists, will be generated or an error occured.
     */
    public String createFromCurseModpack(String modpack) throws CurseException {

        if (SERVERPACKREPOSITORY.findByProjectIDAndFileID(Integer.parseInt(modpack.split(",")[0]), Integer.parseInt(modpack.split(",")[1])).isPresent()) {

            if (SERVERPACKREPOSITORY.findByProjectIDAndFileID(Integer.parseInt(modpack.split(",")[0]), Integer.parseInt(modpack.split(",")[1])).get().getStatus().equals("Available")) {
                return CURSERESPONSEMODEL.response(Integer.parseInt(modpack.split(",")[0]), 0, "The modpack you requested a server pack for has already been generated. Check the downloads-section!", 5000, "info", "info");
            }

            if (SERVERPACKREPOSITORY.findByProjectIDAndFileID(Integer.parseInt(modpack.split(",")[0]), Integer.parseInt(modpack.split(",")[1])).get().getStatus().equals("Queued")) {
                return CURSERESPONSEMODEL.response(Integer.parseInt(modpack.split(",")[0]), 1, "The modpack you requested a server pack for has already been queued!", 5000, "info", "info");
            }

        }

        try {

            ServerPack serverPack = new ServerPack();

            if (CONFIGURATIONHANDLER.checkCurseForge(modpack, serverPack)) {

                serverPack.setModpackDir(modpack);
                serverPack.setProjectID(Integer.parseInt(modpack.split(",")[0]));
                serverPack.setFileID(Integer.parseInt(modpack.split(",")[1]));
                serverPack.setStatus("Queued");
                SERVERPACKSERVICE.insert(serverPack);
                RUNGENERATION.run(serverPack);

                return CURSERESPONSEMODEL.response(serverPack.getProjectID(), 1, "Submitted. Come back later and check the downloads-section to see whether your server pack for " + CurseAPI.project(serverPack.getProjectID()).get().name() + " is ready for download!", 7000, "done", "positive");

            } else {

                return CURSERESPONSEMODEL.response(modpack, 2, "Project or file could not be found!", 3000, "error", "negative");

            }

        } catch (InvalidModpackException ex) {
            LOG.error("Couldn't generate server pack for project: " + modpack, ex);
            return CURSERESPONSEMODEL.response(modpack.split(",")[0], 2, "The specified project is not a Minecraft modpack!", 3000, "error", "negative");

        } catch (InvalidFileException ex) {
            LOG.error("The specified file is not a file of project: " + modpack, ex);
            return CURSERESPONSEMODEL.response(modpack.split(",")[0], 2, "The specified file could not be found for this project!", 3000, "error", "negative");

        } catch (CurseException ex) {
            LOG.error("The specified project does not exist: " + modpack, ex);
            return CURSERESPONSEMODEL.response(modpack.split(",")[0], 2, "The specified project does not exist!", 3000, "error", "negative");
        }
    }

    /**
     * Status 0: Already exists<br>
     * Status 1: OK, generating<br>
     * Status 2: Error occurred
     * @author Griefed
     * @param modpack CurseForge projectID and fileID combination.
     */
    public String regenerateFromCurseModpack(String modpack) {

        ServerPack serverPack = SERVERPACKREPOSITORY.findByProjectIDAndFileID(Integer.parseInt(modpack.split(",")[0]), Integer.parseInt(modpack.split(",")[1])).get();
        serverPack.setModpackDir(modpack);
        serverPack.setStatus("Queued");
        SERVERPACKSERVICE.updateServerPackModelByID(serverPack.getId(), serverPack);
        RUNGENERATION.run(serverPack);

        return CURSERESPONSEMODEL.response(modpack, 1, "Regenerating project", 3000, "done", "positive");
    }

    /**
     *
     * @author Griefed
     */
    @Async
    private class RunGeneration {

        // TODO: Refactor to use queueing system

        /**
         *
         * @author Griefed
         * @param serverPack
         */
        @Async
        public void run(ServerPack serverPack) {
            final ExecutorService executorService = Executors.newSingleThreadExecutor();
            try {
                executorService.execute(() -> {
                    ServerPack pack = SERVERPACKHANDLER.run(serverPack);
                    SERVERPACKSERVICE.updateServerPackModelByID(pack.getId(), pack);
                    System.gc();
                    System.runFinalization();
                    LOG.debug("Done.");
                    executorService.shutdown();
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        executorService.awaitTermination(5000, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ex) {
                        LOG.error("Couldn't terminate executor.", ex);
                    }

                });
            } catch (RejectedExecutionException ex) {
                LOG.error("Couldn't accept task.", ex);
            }
        }
    }
}