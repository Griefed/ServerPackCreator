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

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.spring.models.ServerPack;
import de.griefed.serverpackcreator.spring.repositories.ServerPackRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Griefed
 */
@Service
public class ServerPackService {

    private static final Logger LOG = LogManager.getLogger(ServerPackService.class);

    ApplicationProperties APPLICATIONPROPERTIES;
    ServerPackRepository SERVERPACKREPOSITORY;

    /**
     *
     * @author Griefed
     * @param injectedApplicationProperties
     * @param injectedServerPackRepository
     */
    @Autowired
    public ServerPackService(ApplicationProperties injectedApplicationProperties, ServerPackRepository injectedServerPackRepository) {
        this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        this.SERVERPACKREPOSITORY = injectedServerPackRepository;
    }

    //TODO: Method: Refresh database. Remove any entries referencing server packs no longer available as files whose created timestamp is older than 24h.

    /**
     *
     * @author Griefed
     * @return
     */
    public List<ServerPack> getServerPacks() {
        List<ServerPack> serverPacks = new ArrayList<>();
        SERVERPACKREPOSITORY.findAll().forEach(serverPacks::add);
        return serverPacks;
    }

    /**
     *
     * @author Griefed
     * @param projectID
     * @return
     */
    public List<ServerPack> getServerPacksByProjectID(int projectID) {
        return new ArrayList<>(SERVERPACKREPOSITORY.findAllByProjectID(projectID));
    }

    /**
     *
     * @author Griefed
     * @param serverPack
     * @return
     */
    public ServerPack insert(ServerPack serverPack) {
        return SERVERPACKREPOSITORY.save(serverPack);
    }

    /**
     *
     * @author Griefed
     * @param id
     * @param serverPack
     */
    public void updateServerPackModelByID(int id, ServerPack serverPack) {
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
            SERVERPACKREPOSITORY.save(serverPackFromDB);
        }
    }

    /**
     *
     * @author Griefed
     * @param projectID
     * @param fileID
     * @param serverPack
     */
    public void updateServerPackModelByProjectIDAndFileID(int projectID, int fileID, ServerPack serverPack) {
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
            SERVERPACKREPOSITORY.save(serverPackFromDB);
        }
    }

    /**
     *
     * @author Griefed
     * @param id
     */
    public void deleteServerPack(int id) {
        SERVERPACKREPOSITORY.deleteById(id);
    }

    /**
     *
     * @author Griefed
     * @param projectID
     * @param fileID
     */
    public void deleteServerPack(int projectID, int fileID) {
        SERVERPACKREPOSITORY.deleteByProjectIDAndFileID(projectID, fileID);
    }
}