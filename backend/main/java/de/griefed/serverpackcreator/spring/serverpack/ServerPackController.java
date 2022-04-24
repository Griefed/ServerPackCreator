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
package de.griefed.serverpackcreator.spring.serverpack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RestController for everything server pack related, like downloads.<br>
 * All requests are in <code>/api/v1/packs</code>.
 * @author Griefed
 */
@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/api/v1/packs")
public class ServerPackController {

    private static final Logger LOG = LogManager.getLogger(ServerPackController.class);

    private final ServerPackService SERVERPACKSERVICE;

    /**
     * Constructor responsible for our DI.
     * @author Griefed
     * @param injectedServerPackService Instance of {@link ServerPackService}
     */
    @Autowired
    public ServerPackController(ServerPackService injectedServerPackService) {
        this.SERVERPACKSERVICE = injectedServerPackService;
    }

    /**
     * GET request for downloading a server pack by the id in the database.
     * @author Griefed
     * @param id Integer. The id of the server pack in the database.
     * @return ResponseEntity Resource. Gives the requester the requested file as a download, if it was found.
     */
    @GetMapping(value = "/download/{id}", produces = "application/zip")
    public ResponseEntity<Resource> downloadServerPack(@PathVariable int id) {
        return SERVERPACKSERVICE.downloadServerPackById(id);
    }

    /**
     * GET request for retrieving a list of server packs by CurseForge projectID.
     * @author Griefed
     * @param projectID Integer. The id of the CurseForge project.
     * @return List {@link ServerPackModel}. A list of all server packs, if any, with the given CurseForge projectID.
     */
    @GetMapping("project/{projectid}")
    public ResponseEntity<List<ServerPackModel>> getByProjectID(@PathVariable("projectid") int projectID) {
        if (SERVERPACKSERVICE.getServerPacksByProjectID(projectID).isEmpty()) {

            return ResponseEntity.notFound().build();

        } else {

            return ResponseEntity
                    .ok()
                    .header(
                            "Content-Type",
                            "application/json"
                    ).body(
                            SERVERPACKSERVICE.getServerPacksByProjectID(projectID)
                    );
        }

    }

    /**
     * GET request for a server pack matching the given CurseForge fileID.
     * @author Griefed
     * @param fileID Integer. The fileID of the CurseForge project for which to retrieve the server pack.
     * @return {@link ServerPackModel}. The server pack for the corresponding CurseForge fileID, if it was found.
     */
    @GetMapping("file/{fileid}")
    public ResponseEntity<ServerPackModel> getByFileID(@PathVariable("fileid") int fileID) {
        if (SERVERPACKSERVICE.findByFileID(fileID).isPresent()) {

            return ResponseEntity
                    .ok()
                    .header(
                            "Content-Type",
                            "application/json"
                    ).body(
                            SERVERPACKSERVICE.findByFileID(fileID).get()
                    );

        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET request for retrieving a list of all available server packs.
     * @author Griefed
     * @return List {@link ServerPackModel}. A list of all available server packs on this instance.
     */
    @GetMapping("all")
    public ResponseEntity<List<ServerPackModel>> getAllServerPacks() {
        if (SERVERPACKSERVICE.getServerPacks().isEmpty()) {

            return ResponseEntity.notFound().build();

        } else {

            return ResponseEntity
                    .ok()
                    .header(
                            "Content-Type",
                            "application/json"
                    ).body(
                            SERVERPACKSERVICE.getServerPacks()
                    );

        }

    }

    /**
     * GET request for retrieving a server pack for a specific CurseForge projectID and fileID.
     * @author Griefed
     * @param specific String. Comma seperated combination of CurseForge projectID and fileID.
     * @return {@link ServerPackModel}. The server pack for the specified CurseForge projectID and fileID, if it was found.
     */
    @GetMapping("specific/{specific}")
    public ResponseEntity<ServerPackModel> getByFileID(@PathVariable("specific") String specific) {

        String[] project = specific.split(",");

        int projectID = Integer.parseInt(project[0]);
        int fileID = Integer.parseInt(project[1]);

        if (SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).isPresent()) {

            return ResponseEntity
                    .ok()
                    .header(
                            "Content-Type",
                            "application/json"
                    ).body(
                            SERVERPACKSERVICE.findByProjectIDAndFileID(projectID, fileID).get()
                    );

        } else {

            return ResponseEntity.notFound().build();

        }

    }

    /**
     * GET request for voting whether a server pack works or not.
     * @author Griefed
     * @param voting String. The vote, consisting of the id of the server pack and whether the vote should be incremented or decremented. Example <code>42,up</code> or <code>23,down</code>.
     * @return ResponseEntity OK/BadRequest/NotFound
     */
    @GetMapping("vote/{voting}")
    // TODO: Secure with Captcha so vote spamming is somewhat prevented
    public ResponseEntity<Object> voteForServerPack(@PathVariable("voting") String voting) {
        return SERVERPACKSERVICE.voteForServerPack(voting);
    }
}
