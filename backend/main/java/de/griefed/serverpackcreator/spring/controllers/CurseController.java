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
import de.griefed.serverpackcreator.ServerPackHandler;
import de.griefed.serverpackcreator.spring.services.CurseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/api/curse")
public class CurseController {

    private static final Logger LOG = LogManager.getLogger(CurseController.class);


    private final ServerPackHandler SERVERPACKHANDLER;
    private final CurseService CURSESERVICE;

    @Autowired
    public CurseController(ServerPackHandler injectedServerPackHandler, CurseService injectedCurseService) {
        this.SERVERPACKHANDLER = injectedServerPackHandler;
        this.CURSESERVICE = injectedCurseService;
    }

    @CrossOrigin(origins = {"*"})
    @GetMapping("")
    public String generateServerPackFromProjectFile(@RequestParam(value = "project", defaultValue = "0,0") String projectFile) {
        try {
            return CURSESERVICE.createFromCurseModpack(projectFile);
        } catch (CurseException ex) {
            LOG.error("An error occured trying to create a server pack from a CurseForge project.", ex);
            return "Error.";
        }
    }

}
