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
import de.griefed.serverpackcreator.spring.models.ServerPackModel;
import de.griefed.serverpackcreator.spring.repositories.ServerPackRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ServerPackServiceImpl implements ServerPackService{

    private static final Logger LOG = LogManager.getLogger(ServerPackServiceImpl.class);

    ApplicationProperties APPLICATIONPROPERTIES;
    ServerPackRepository SERVERPACKREPOSITORY;

    @Autowired
    public ServerPackServiceImpl(ApplicationProperties injectadApplicationProperties, ServerPackRepository injectedServerPackRepository) {
        this.APPLICATIONPROPERTIES = injectadApplicationProperties;
        this.SERVERPACKREPOSITORY = injectedServerPackRepository;
    }

    @Override
    public List<ServerPackModel> getServerPacks() {
        List<ServerPackModel> serverPackModels = new ArrayList<>();
        SERVERPACKREPOSITORY.findAll().forEach(serverPackModels::add);
        return serverPackModels;
    }

    @Override
    public ServerPackModel getServerPackById(int id) {
        return SERVERPACKREPOSITORY.findById(id).get();
    }

    @Override
    public ServerPackModel insert(ServerPackModel serverPackModel) {
        return SERVERPACKREPOSITORY.save(serverPackModel);
    }

    @Override
    public void updateServerPackModel(int id, ServerPackModel serverPackModel) {
        ServerPackModel serverPackModelFromDB = SERVERPACKREPOSITORY.findById(id).get();
        LOG.debug("Updating database with: " + serverPackModelFromDB.repositoryToString());
        serverPackModelFromDB.setProjectName(serverPackModel.getProjectName());
        serverPackModelFromDB.setFileName(serverPackModel.getFileName());
        serverPackModelFromDB.setSize(serverPackModel.getSize());
        serverPackModelFromDB.setDownloads(serverPackModel.getDownloads());
        serverPackModelFromDB.setConfirmedWorking(serverPackModel.getConfirmedWorking());
        serverPackModelFromDB.setFilePath(serverPackModel.getFilePath());
        SERVERPACKREPOSITORY.save(serverPackModelFromDB);
    }

    @Override
    public void deleteServerPack(int serverPackId) {
        SERVERPACKREPOSITORY.deleteById(serverPackId);
    }
}
