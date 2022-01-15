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
package de.griefed.serverpackcreator.spring.services;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.spring.models.ServerPack;
import de.griefed.serverpackcreator.spring.repositories.ServerPackRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Class revolving around with server packs, like downloading, retrieving, deleting, voting etc.
 * @author Griefed
 */
@Service
public class ServerPackService {

    private static final Logger LOG = LogManager.getLogger(ServerPackService.class);

    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final ServerPackRepository SERVERPACKREPOSITORY;

    /**
     * Constructor responsible for our DI.
     * @author Griefed
     * @param injectedApplicationProperties Instance of {@link ApplicationProperties}.
     * @param injectedServerPackRepository Instance of {@link ServerPackRepository}.
     */
    @Autowired
    public ServerPackService(ApplicationProperties injectedApplicationProperties, ServerPackRepository injectedServerPackRepository) {
        this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        this.SERVERPACKREPOSITORY = injectedServerPackRepository;
    }

    //TODO: Method: Refresh database. Remove any entries referencing server packs no longer available as files whose created timestamp is older than property and has zero downloads

    /**
     * Find a specific server pack by searching with a CurseForge project and file ID.
     * @author Griefed
     * @param projectID Integer. The CurseForge project ID.
     * @param fileID Integer. The CurseForge project file ID.
     * @return Returns the server pack for the passed project and file ID wrapped in an {@link Optional}. I recommend to make use of {@link Optional#isPresent()} and {@link Optional#get()}.
     */
    public Optional<ServerPack> findByProjectIDAndFileID(int projectID, int fileID) {
        return SERVERPACKREPOSITORY.findByProjectIDAndFileID(projectID, fileID);
    }

    /**
     * Delete a server pack with a given CurseForge project and file ID.
     * @author Griefed
     * @param projectID Integer. The CurseForge project ID.
     * @param fileID Integer. The CurseForge file ID.
     */
    protected void deleteByProjectIDAndFileID(int projectID, int fileID) {
        SERVERPACKREPOSITORY.deleteByProjectIDAndFileID(projectID, fileID);
    }

    /**
     * Find a server pack by its database id.
     * @author Griefed
     * @param fileID Integer. The database id with which to search for a server pack.
     * @return Returns a server pack for the passed database id wrapped in an {@link Optional}. I recommend to make use of {@link Optional#isPresent()} and {@link Optional#get()}.
     */
    public Optional<ServerPack> findByFileID(int fileID) {
        return SERVERPACKREPOSITORY.findByFileID(fileID);
    }

    /**
     * Download a server pack with the given database id.
     * @author Griefed
     * @param id Integer. The database id of the server pack to download.
     * @return Returns a response entity with either the server pack as a downloadable file, or a response entity with a not found body.
     */
    public ResponseEntity<Resource> downloadServerPackById(int id) {
        if (SERVERPACKREPOSITORY.findById(id).isPresent() && SERVERPACKREPOSITORY.findById(id).get().getStatus().matches("Available")) {

            ServerPack serverPack = SERVERPACKREPOSITORY.findById(id).get();

            Path path = Paths.get(serverPack.getPath());
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
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + serverPack.getFileDiskName().replace(".zip","") + "_server_pack.zip" + "\"")
                    .body(resource);
        } else {

            return ResponseEntity.notFound().build();

        }
    }

    /**
     * Either upvote or downvote a given server pack.
     * @author Griefed
     * @param voting String. The database id of the server pack and whether it should be up- or downvoted.
     * @return Returns ok if the vote went through, bad request if the passed vote was malformed, or not found if the server pack could not be found.
     */
    public ResponseEntity<Object> voteForServerPack(String voting) {
        String[] vote = voting.split(",");
        int id = Integer.parseInt(vote[0]);
        if (SERVERPACKREPOSITORY.findById(id).isPresent() && SERVERPACKREPOSITORY.findById(id).get().getStatus().equals("Available")) {

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
     * @author Griefed
     * @return List ServerPack. Returns a list of all available server packs.
     */
    public List<ServerPack> getServerPacks() {
        List<ServerPack> serverPacks = new ArrayList<>();
        SERVERPACKREPOSITORY.findAll().forEach(serverPacks::add);
        return serverPacks;
    }

    /**
     * Get a list of all server packs using a CurseForge project ID.
     * @author Griefed
     * @param projectID Integer. The CurseForge project ID with which to search for server pack.
     * @return Returns a list of all server packs for the passed CurseForge project ID wrapped in an {@link Optional}. I recommend to make use of {@link Optional#isPresent()} and {@link Optional#get()}.
     */
    public List<ServerPack> getServerPacksByProjectID(int projectID) {
        if (SERVERPACKREPOSITORY.findAllByProjectID(projectID).isPresent()) {
            return new ArrayList<ServerPack>(SERVERPACKREPOSITORY.findAllByProjectID(projectID).get());
        } else {
            return new ArrayList<ServerPack>();
        }

    }

    /**
     * Store a server pack in the database.
     * @author Griefed
     * @param serverPack Instance of {@link ServerPack} to store in the database.
     */
    protected void insert(ServerPack serverPack) {
        SERVERPACKREPOSITORY.save(serverPack);
    }

    /**
     * Update a server pack database entry with the given database id.
     * @author Griefed
     * @param id Integer. The database id of the server pack to update.
     * @param serverPack Instance of {@link ServerPack} with which to update the entry in the database.
     */
    protected void updateServerPackByID(int id, ServerPack serverPack) {
        if (SERVERPACKREPOSITORY.findById(id).isPresent()) {
            ServerPack serverPackFromDB = SERVERPACKREPOSITORY.findById(id).get();
            LOG.debug("Updating database with: " + serverPack.repositoryToString());
            serverPackFromDB.setProjectName(serverPack.getProjectName());
            serverPackFromDB.setFileName(serverPack.getFileName());
            serverPackFromDB.setFileDiskName(serverPack.getFileDiskName());
            serverPackFromDB.setSize(serverPack.getSize());
            serverPackFromDB.setDownloads(serverPack.getDownloads());
            serverPackFromDB.setConfirmedWorking(serverPack.getConfirmedWorking());
            serverPackFromDB.setStatus(serverPack.getStatus());
            serverPackFromDB.setLastModified(new Timestamp(new Date().getTime()));
            serverPackFromDB.setPath(serverPack.getPath());
            SERVERPACKREPOSITORY.save(serverPackFromDB);
        }
    }

    /**
     * Increment the download counter for a given server pack entry in the database identified by the database id.
     * @author Griefed
     * @param id Integer. The database id of the server pack.
     */
    protected void updateDownloadCounter(int id) {
        if (SERVERPACKREPOSITORY.findById(id).isPresent()) {
            ServerPack serverPackFromDB = SERVERPACKREPOSITORY.findById(id).get();
            serverPackFromDB.setDownloads(serverPackFromDB.getDownloads() + 1);
            SERVERPACKREPOSITORY.save(serverPackFromDB);
        }
    }

    /**
     * Either increment or decrement the confirmed working value of a given server pack entry in the database, identified by the
     * database id.
     * @author Griefed
     * @param id Integer. The database id of the server pack.
     * @param vote Integer. Positive for upvote, negative for downvote
     */
    protected void updateConfirmedCounter(int id, int vote) {
        if (SERVERPACKREPOSITORY.findById(id).isPresent()) {
            ServerPack serverPackFromDB = SERVERPACKREPOSITORY.findById(id).get();
            serverPackFromDB.setConfirmedWorking(serverPackFromDB.getConfirmedWorking() + vote);
            SERVERPACKREPOSITORY.save(serverPackFromDB);
        }
    }

    /**
     * Update a server pack entry in the database identified by a CurseForge project and file ID combination, using a passed
     * server pack.
     * @author Griefed
     * @param projectID The CurseForge project ID.
     * @param fileID The CurseForge file ID.
     * @param serverPack The server pack with which to update the entry in the database.
     */
    protected void updateServerPackByProjectIDAndFileID(int projectID, int fileID, ServerPack serverPack) {
        if (SERVERPACKREPOSITORY.findByProjectIDAndFileID(projectID, fileID).isPresent()) {
            ServerPack serverPackFromDB = SERVERPACKREPOSITORY.findByProjectIDAndFileID(projectID, fileID).get();
            LOG.debug("Updating database with: " + serverPack.repositoryToString());
            serverPackFromDB.setProjectName(serverPack.getProjectName());
            serverPackFromDB.setFileName(serverPack.getFileName());
            serverPackFromDB.setFileDiskName(serverPack.getFileDiskName());
            serverPackFromDB.setSize(serverPack.getSize());
            serverPackFromDB.setDownloads(serverPack.getDownloads());
            serverPackFromDB.setConfirmedWorking(serverPack.getConfirmedWorking());
            serverPackFromDB.setStatus(serverPack.getStatus());
            serverPackFromDB.setLastModified(new Timestamp(new Date().getTime()));
            serverPackFromDB.setPath(serverPack.getPath());
            SERVERPACKREPOSITORY.save(serverPackFromDB);
        }
    }

    /**
     * Deletes a server pack with the given id.
     * @author Griefed
     * @param id Integer. The database id of the server pack to delete.
     */
    protected void deleteServerPack(int id) {
        SERVERPACKREPOSITORY.deleteById(id);
    }

    /**
     * Delete a server pack with a given CurseForge project and file ID.
     * @author Griefed
     * @param projectID Integer. The CurseForge project ID.
     * @param fileID Integer. The CurseForge file ID.
     */
    protected void deleteServerPack(int projectID, int fileID) {
        SERVERPACKREPOSITORY.deleteByProjectIDAndFileID(projectID, fileID);
    }


}
