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

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Class revolving around with server packs, like downloading, retrieving, deleting, voting etc.
 *
 * @author Griefed
 */
@Service
public class ServerPackService {

  private static final Logger LOG = LogManager.getLogger(ServerPackService.class);

  private final ServerPackRepository SERVERPACKREPOSITORY;

  /**
   * Constructor responsible for our DI.
   *
   * @param injectedServerPackRepository Instance of {@link ServerPackRepository}.
   * @author Griefed
   */
  @Autowired
  public ServerPackService(ServerPackRepository injectedServerPackRepository) {
    this.SERVERPACKREPOSITORY = injectedServerPackRepository;
  }

  /**
   * Download a server pack with the given database id.
   *
   * @param id Integer. The database id of the server pack to download.
   * @return Returns a curseResponse entity with either the server pack as a downloadable file, or a
   *     curseResponse entity with a not found body.
   * @author Griefed
   */
  public ResponseEntity<Resource> downloadServerPackById(int id) {
    if (SERVERPACKREPOSITORY.findById(id).isPresent()
        && SERVERPACKREPOSITORY.findById(id).get().getStatus().matches("Available")) {

      ServerPackModel serverPackModel = SERVERPACKREPOSITORY.findById(id).get();

      Path path = Paths.get(serverPackModel.getPath());
      Resource resource = null;
      String contentType = "application/zip";

      try {
        resource = new UrlResource(path.toUri());
      } catch (MalformedURLException ex) {
        LOG.error("Error generating download for server pack with ID" + id + ".", ex);
      }

      updateDownloadCounter(id);

      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(contentType))
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\""
                  + serverPackModel.getFileDiskName().replace(".zip", "")
                  + "_server_pack.zip"
                  + "\"")
          .body(resource);
    } else {

      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Either upvote or downvote a given server pack.
   *
   * @param voting String. The database id of the server pack and whether it should be up- or
   *     downvoted.
   * @return Returns ok if the vote went through, bad request if the passed vote was malformed, or
   *     not found if the server pack could not be found.
   * @author Griefed
   */
  public ResponseEntity<Object> voteForServerPack(String voting) {
    String[] vote = voting.split(",");
    int id = Integer.parseInt(vote[0]);
    if (SERVERPACKREPOSITORY.findById(id).isPresent()
        && SERVERPACKREPOSITORY.findById(id).get().getStatus().equals("Available")) {

      if (vote[1].equalsIgnoreCase("up")) {

        updateConfirmedCounter(id, +1);
        return ResponseEntity.ok().build();

      } else if (vote[1].equalsIgnoreCase("down")) {

        updateConfirmedCounter(id, -1);
        return ResponseEntity.ok().build();

      } else {

        return ResponseEntity.badRequest().build();
      }

    } else {

      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Get a list of all available server packs.
   *
   * @return List ServerPackModel. Returns a list of all available server packs.
   * @author Griefed
   */
  public List<ServerPackModel> getServerPacks() {
    List<ServerPackModel> serverPackModels = new ArrayList<>();
    SERVERPACKREPOSITORY.findAll().forEach(serverPackModels::add);
    return serverPackModels;
  }

  /**
   * Store a server pack in the database.
   *
   * @param serverPackModel Instance of {@link ServerPackModel} to store in the database.
   * @author Griefed
   */
  public void insert(ServerPackModel serverPackModel) {
    SERVERPACKREPOSITORY.save(serverPackModel);
  }

  /**
   * Update a server pack database entry with the given database id.
   *
   * @param id Integer. The database id of the server pack to initialize.
   * @param serverPackModel Instance of {@link ServerPackModel} with which to initialize the entry
   *     in the database.
   * @author Griefed
   */
  public void updateServerPackByID(int id, ServerPackModel serverPackModel) {
    if (SERVERPACKREPOSITORY.findById(id).isPresent()) {
      ServerPackModel serverPackModelFromDB = SERVERPACKREPOSITORY.findById(id).get();
      LOG.debug("Updating database with: " + serverPackModel.repositoryToString());
      serverPackModelFromDB.setProjectName(serverPackModel.getProjectName());
      serverPackModelFromDB.setFileName(serverPackModel.getFileName());
      serverPackModelFromDB.setFileDiskName(serverPackModel.getFileDiskName());
      serverPackModelFromDB.setSize(serverPackModel.getSize());
      serverPackModelFromDB.setDownloads(serverPackModel.getDownloads());
      serverPackModelFromDB.setConfirmedWorking(serverPackModel.getConfirmedWorking());
      serverPackModelFromDB.setStatus(serverPackModel.getStatus());
      serverPackModelFromDB.setLastModified(new Timestamp(new Date().getTime()));
      serverPackModelFromDB.setPath(serverPackModel.getPath());
      SERVERPACKREPOSITORY.save(serverPackModelFromDB);
    }
  }

  /**
   * Increment the download counter for a given server pack entry in the database identified by the
   * database id.
   *
   * @param id Integer. The database id of the server pack.
   * @author Griefed
   */
  public void updateDownloadCounter(int id) {
    if (SERVERPACKREPOSITORY.findById(id).isPresent()) {
      ServerPackModel serverPackModelFromDB = SERVERPACKREPOSITORY.findById(id).get();
      serverPackModelFromDB.setDownloads(serverPackModelFromDB.getDownloads() + 1);
      SERVERPACKREPOSITORY.save(serverPackModelFromDB);
    }
  }

  /**
   * Either increment or decrement the confirmed working value of a given server pack entry in the
   * database, identified by the database id.
   *
   * @param id Integer. The database id of the server pack.
   * @param vote Integer. Positive for upvote, negative for downvote
   * @author Griefed
   */
  public void updateConfirmedCounter(int id, int vote) {
    if (SERVERPACKREPOSITORY.findById(id).isPresent()) {
      ServerPackModel serverPackModelFromDB = SERVERPACKREPOSITORY.findById(id).get();
      serverPackModelFromDB.setConfirmedWorking(serverPackModelFromDB.getConfirmedWorking() + vote);
      SERVERPACKREPOSITORY.save(serverPackModelFromDB);
    }
  }

  /**
   * Deletes a server pack with the given id.
   *
   * @param id Integer. The database id of the server pack to delete.
   * @author Griefed
   */
  public void deleteServerPack(int id) {
    SERVERPACKREPOSITORY.deleteById(id);
  }
}
