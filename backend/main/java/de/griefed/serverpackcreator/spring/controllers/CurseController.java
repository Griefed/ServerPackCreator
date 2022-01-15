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
package de.griefed.serverpackcreator.spring.controllers;

import com.therandomlabs.curseapi.CurseException;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.spring.models.CurseResponse;
import de.griefed.serverpackcreator.spring.services.CurseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * RestController for everything CurseForge related.<br>
 * All requests are in <code>/api/curse</code>.
 * @author Griefed
 */
@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/api/curse")
public class CurseController {

    private static final Logger LOG = LogManager.getLogger(CurseController.class);

    private final CurseService CURSESERVICE;
    private final CurseResponse CURSERESPONSEMODEL;
    private final ApplicationProperties APPLICATIONPROPERTIES;

    /**
     * Constructor responsible for our DI.
     * @author Griefed
     * @param injectedApplicationProperties Instance of {@link ApplicationProperties}
     * @param injectedCurseService Instance of {@link CurseService}
     * @param injectedCurseResponse Instance of {@link CurseResponse}
     */
    @Autowired
    public CurseController(CurseService injectedCurseService, CurseResponse injectedCurseResponse, ApplicationProperties injectedApplicationProperties) {
        this.CURSESERVICE = injectedCurseService;
        this.CURSERESPONSEMODEL = injectedCurseResponse;
        this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    }

    /**
     * GET request for requesting the generation of a server pack. RequestParam is modpack=projectID,fileID<br>
     * Returns a {@link CurseResponse#response(int, int, String, int, String, String)} or {@link CurseResponse#response(String, int, String, int, String, String)} depending
     * on the decision made in {@link CurseService}.<br>
     * @author Griefed
     * @param modpack CurseForge projectID and fileID combination.
     * @return String. Statuscode indicating whether the server pack already exists, will be generated or an error occured.
     */
    @CrossOrigin(origins = {"*"})
    @GetMapping("task")
    public String task(@RequestParam(value = "modpack", defaultValue = "10,60018") String modpack) {
        LOG.info("IMPORTANT!!! - Modpack directory matches CurseForge projectID and fileID format. However, the CurseForge module is currently disabled due to CurseForge changing their API and the way one can access it.");
        LOG.info("IMPORTANT!!! - Downloading and installing a modpack is disabled until further notice.");
        // TODO: Reactivate once custom implementation of CurseForgeAPI has been implemented and CurseForge has provided me with an API key

        return CURSERESPONSEMODEL.response(modpack, 2, "The CurseForge module is currently disabled due to CurseForge changing their API and the way one can access it.", 7000, "error", "negative");

//        String[] project = modpack.split(",");
//        int projectID = Integer.parseInt(project[0]);
//        int fileID = Integer.parseInt(project[1]);
//        try {
//            return CURSESERVICE.createFromCurseModpack(projectID, fileID);
//        } catch (CurseException ex) {
//            LOG.error("Couldn't generate server pack for project " + modpack, ex);
//            return CURSERESPONSEMODEL.response(modpack, 2, "Project or file could not be found!", 3000, "error", "negative");
//        }
    }

    /**
     * GET request for requesting the regeneration of a server pack. RequestParam is modpack=projectID,fileID<br>
     * Returns a {@link CurseResponse#response(int, int, String, int, String, String)} or {@link CurseResponse#response(String, int, String, int, String, String)} depending
     * on the decision made in {@link CurseService}.<br>
     * @author Griefed
     * @param modpack CurseForge projectID and fileID combination.
     * @return String. Returns a {@link CurseResponse} depending on the outcome of {@link CurseService#regenerateFromCurseModpack(String)}.
     */
    @CrossOrigin(origins = {"*"})
    @GetMapping("/regenerate")
    public String regenerate(@RequestParam(value = "modpack", defaultValue = "10,60018") String modpack) {
        LOG.info("IMPORTANT!!! - Modpack directory matches CurseForge projectID and fileID format. However, the CurseForge module is currently disabled due to CurseForge changing their API and the way one can access it.");
        LOG.info("IMPORTANT!!! - Downloading and installing a modpack is disabled until further notice.");
        // TODO: Reactivate once custom implementation of CurseForgeAPI has been implemented and CurseForge has provided me with an API key

        return CURSERESPONSEMODEL.response(modpack, 2, "The CurseForge module is currently disabled due to CurseForge changing their API and the way one can access it.", 7000, "error", "negative");

        //return CURSESERVICE.regenerateFromCurseModpack(modpack);
    }

    /**
     * GET request to check whether regeneration of server packs is available on this instance of ServerPackCreator.
     * @author Griefed
     * @return JSON. Returns <code>{"regenerationActivated": true/false}</code>
     */
    @CrossOrigin(origins = {"*"})
    @GetMapping("/regenerate/active")
    public String regenerateActivated() {
        return "{\"regenerationActivated\": " + APPLICATIONPROPERTIES.getCurseControllerRegenerationEnabled() + "}";
    }

}
