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

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RestController for everything server pack related, like downloads.<br>
 * All requests are in <code>/api/v1/packs</code>.
 *
 * @author Griefed
 */
@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/api/v1/packs")
public class ServerPackController {

  private final ServerPackService SERVERPACKSERVICE;

  /**
   * Constructor responsible for our DI.
   *
   * @param injectedServerPackService Instance of {@link ServerPackService}
   * @author Griefed
   */
  @Autowired
  public ServerPackController(ServerPackService injectedServerPackService) {
    this.SERVERPACKSERVICE = injectedServerPackService;
  }

  /**
   * GET request for downloading a server pack by the id in the database.
   *
   * @param id Integer. The id of the server pack in the database.
   * @return ResponseEntity Resource. Gives the requester the requested file as a download, if it
   *     was found.
   * @author Griefed
   */
  @GetMapping(value = "/download/{id}", produces = "application/zip")
  public ResponseEntity<Resource> downloadServerPack(@PathVariable int id) {
    return SERVERPACKSERVICE.downloadServerPackById(id);
  }

  /**
   * GET request for retrieving a list of all available server packs.
   *
   * @return List {@link ServerPackModel}. A list of all available server packs on this instance.
   * @author Griefed
   */
  @GetMapping("all")
  public ResponseEntity<List<ServerPackModel>> getAllServerPacks() {
    if (SERVERPACKSERVICE.getServerPacks().isEmpty()) {

      return ResponseEntity.notFound().build();

    } else {

      return ResponseEntity.ok()
          .header("Content-Type", "application/json")
          .body(SERVERPACKSERVICE.getServerPacks());
    }
  }

  /**
   * GET request for voting whether a server pack works or not.
   *
   * @param voting String. The vote, consisting of the id of the server pack and whether the vote
   *     should be incremented or decremented. Example <code>42,up</code> or <code>23,down</code>.
   * @return ResponseEntity OK/BadRequest/NotFound
   * @author Griefed
   */
  @GetMapping("vote/{voting}")
  // TODO: Secure with Captcha so vote spamming is somewhat prevented
  public ResponseEntity<Object> voteForServerPack(@PathVariable("voting") String voting) {
    return SERVERPACKSERVICE.voteForServerPack(voting);
  }
}
