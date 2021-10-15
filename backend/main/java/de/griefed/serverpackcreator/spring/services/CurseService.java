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

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.ServerPackHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CurseService {

    private final ServerPackHandler SERVERPACKHANDLER;

    @Autowired
    public CurseService(ServerPackHandler injectedServerPackHandler) {
        this.SERVERPACKHANDLER = injectedServerPackHandler;
    }

    public String createFromCurseModpack(String projectFileCombination) throws CurseException {

        String[] project = projectFileCombination.split(",");
        int projectID, fileID;
        projectID = Integer.parseInt(project[0]);
        fileID = Integer.parseInt(project[1]);

        if (CurseAPI.project(Integer.parseInt(project[0])).isPresent()) {

            ConfigurationModel configurationModel = new ConfigurationModel();
            configurationModel.setModpackDir(projectFileCombination);

            class RunGeneration extends Thread {

                private ConfigurationModel configurationModel;

                public RunGeneration(ConfigurationModel injectedConfigurationModel) {
                    this.configurationModel = injectedConfigurationModel;
                }

                public void run() {
                    SERVERPACKHANDLER.run(this.configurationModel);
                }

            }

            RunGeneration runGeneration = new RunGeneration(configurationModel);
            runGeneration.start();

            return CurseAPI.project(projectID).get().name() + " - " + curseFileName(projectID, fileID);
        } else {
            return "Project not found.";
        }
    }

    private String curseFileName(int projectID, int fileID) {
        String name;

        try {

            name = CurseAPI.project(projectID).orElseThrow(NullPointerException::new).files().fileWithID(fileID).displayName();

        } catch (CurseException | NullPointerException ignored) {

            try {

                name = CurseAPI.project(projectID).orElseThrow(NullPointerException::new).files().fileWithID(fileID).nameOnDisk();

            } catch (CurseException | NullPointerException ignored1) {

                name = String.valueOf(fileID);

            }

        }

        return name;
    }


}
