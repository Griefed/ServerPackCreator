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
package de.griefed.serverpackcreator.spring.controllers;

import com.therandomlabs.curseapi.CurseException;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.spring.models.CurseResponseModel;
import de.griefed.serverpackcreator.spring.services.CurseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author Griefed
 */
@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/api/curse")
public class CurseController {

    private static final Logger LOG = LogManager.getLogger(CurseController.class);

    private final CurseService CURSESERVICE;
    private final CurseResponseModel CURSERESPONSEMODEL;
    private final ApplicationProperties APPLICATIONPROPERTIES;

    /**
     *
     * @author Griefed
     * @param injectedCurseService
     * @param injectedCurseResponseModel
     */
    @Autowired
    public CurseController(CurseService injectedCurseService, CurseResponseModel injectedCurseResponseModel, ApplicationProperties injectedApplicationProperties) {
        this.CURSESERVICE = injectedCurseService;
        this.CURSERESPONSEMODEL = injectedCurseResponseModel;
        this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    }

    /**
     * Status 0: Already exists<br>
     * Status 1: OK, generating<br>
     * Status 2: Error occurred
     * @author Griefed
     * @param modpack CurseForge projectID and fileID combination.
     * @return String. Statuscode indicating whether the server pack already exists, will be generated or an error occured.
     */
    @CrossOrigin(origins = {"*"})
    @GetMapping("")
    public String generate(@RequestParam(value = "modpack", defaultValue = "10,60018") String modpack) {
        try {
            return CURSESERVICE.createFromCurseModpack(modpack);
        } catch (CurseException ex) {
            LOG.error("Couldn't generate server pack for project " + modpack, ex);
            return CURSERESPONSEMODEL.response(modpack, 2, "Project or file could not be found!", 3000, "error", "negative");
        }
    }

    /**
     * Status 0: Already exists<br>
     * Status 1: OK, generating<br>
     * Status 2: Error occurred
     * @author Griefed
     * @param modpack CurseForge projectID and fileID combination.
     */
    @CrossOrigin(origins = {"*"})
    @GetMapping("/regenerate")
    public String regenerate(@RequestParam(value = "modpack", defaultValue = "10,60018") String modpack) {
        if (APPLICATIONPROPERTIES.getCURSE_CONTROLLER_REGENERATION_ENABLED()) {
            CURSESERVICE.regenerateFromCurseModpack(modpack);
            return CURSERESPONSEMODEL.response(modpack, 1, "Regenerating project", 3000, "done", "positive");
        } else {
            return CURSERESPONSEMODEL.response(modpack, 2, "Regeneration is disabled on this instance!", 4000, "info", "warning");
        }
    }

    /**
     *
     * @author Griefed
     * @return
     */
    @CrossOrigin(origins = {"*"})
    @GetMapping("/regenerate/active")
    public String regenerateActivated() {
        return "{\"regenerationActivated\": " + APPLICATIONPROPERTIES.getCURSE_CONTROLLER_REGENERATION_ENABLED() + "}";
    }

}
