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

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.spring.models.ServerPack;
import de.griefed.serverpackcreator.spring.repositories.ServerPackRepository;
import de.griefed.serverpackcreator.spring.services.ServerPackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 *
 * @author Griefed
 */
@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/api/packs")
public class ServerPackController {

    private static final Logger LOG = LogManager.getLogger(ServerPackController.class);

    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final ServerPackService SERVERPACKSERVICE;
    private final ServerPackRepository SERVERPACKREPOSITORY;

    /**
     *
     * @author Griefed
     * @param injectedApplicationProperties
     * @param injectedServerPackService
     */
    @Autowired
    public ServerPackController(ApplicationProperties injectedApplicationProperties, ServerPackService injectedServerPackService, ServerPackRepository injectedServerPackRepository) {
        this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        this.SERVERPACKSERVICE = injectedServerPackService;
        this.SERVERPACKREPOSITORY = injectedServerPackRepository;
    }

    /**
     *
     * @author Griefed
     * @param id
     * @return
     */
    @GetMapping(value = "/download/{id}", produces = "application/zip")
    public ResponseEntity<Resource> downloadServerPack(@PathVariable int id) {
        return SERVERPACKSERVICE.downloadServerPackById(id);
    }

    /**
     *
     * @author Griefed
     * @param projectID
     * @return
     */
    @GetMapping("project/{projectid}")
    public List<ServerPack> getByProjectID(@PathVariable("projectid") int projectID) {
        return SERVERPACKSERVICE.getServerPacksByProjectID(projectID);
    }

    /**
     *
     * @author Griefed
     * @param fileID
     * @return
     */
    @GetMapping("file/{fileid}")
    public ServerPack getByFileID(@PathVariable("fileid") int fileID) {
        if (SERVERPACKREPOSITORY.findByFileID(fileID).isPresent()) {
            return SERVERPACKREPOSITORY.findByFileID(fileID).get();
        } else {
            return null;
        }
    }

    /**
     *
     * @author Griefed
     * @return
     */
    @GetMapping("all")
    public List<ServerPack> getAllServerPacks() {
        return SERVERPACKSERVICE.getServerPacks();
    }

    /**
     *
     * @author Griefed
     * @param specific
     * @return
     */
    @GetMapping("specific/{specific}")
    public ServerPack getByFileID(@PathVariable("specific") String specific) {
        return SERVERPACKREPOSITORY.findByProjectIDAndFileID(Integer.parseInt(specific.split(",")[0]), Integer.parseInt(specific.split(",")[1])).get();
    }

    /**
     *
     * @author Griefed
     * @param voting
     * @return
     */
    @GetMapping("vote/{voting}")
    public ResponseEntity<Object> voteForServerPack(@PathVariable("voting") String voting) {
        return SERVERPACKSERVICE.voteForServerPack(voting);
    }
}
