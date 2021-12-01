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
package de.griefed.serverpackcreator.spring.repositories;

import de.griefed.serverpackcreator.spring.models.ServerPack;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Our JPA repository for storing and retrieving {@link ServerPack}s to and from our SQLite database. Provides methods for
 * retrieving, storing, deleting. Calls must always be made from {@link de.griefed.serverpackcreator.spring.services.ServerPackService} and not directly
 * to this repository. It may seem strange, but this accomplishes a couple of things:<br>
 * 1. Centralized access to the repository.<br>
 * 2. Complete control over what happens with the data retrieved from the repository.<br>
 * 3. Complete control over what happens with the data passed to the repository.<br>
 * 4. Better management and overview of what we are doing with our repository.
 * @author Griefed
 */
@Repository
public interface ServerPackRepository extends CrudRepository<ServerPack, Integer> {

    /**
     * Find all server packs using a CurseForge project ID.
     * @author Griefed
     * @param projectID Integer. The CurseForge project ID with which to search for server pack.
     * @return Returns a list of all server packs for the passed CurseForge project ID wrapped in an {@link Optional}. I recommend to make use of {@link Optional#isPresent()} and {@link Optional#get()}.
     */
    Optional<List<ServerPack>> findAllByProjectID(int projectID);

    /**
     * Find a specific server pack by searching with a CurseForge project and file ID.
     * @author Griefed
     * @param projectID Integer. The CurseForge project ID.
     * @param fileID Integer. The CurseForge project file ID.
     * @return Returns the server pack for the passed project and file ID wrapped in an {@link Optional}. I recommend to make use of {@link Optional#isPresent()} and {@link Optional#get()}.
     */
    Optional<ServerPack> findByProjectIDAndFileID(int projectID, int fileID);

    /**
     * Find all server packs using a CurseForge project name.
     * @author Griefed
     * @param projectName String. The project name with which to search for server packs.
     * @return Returns a list of all server packs for the passed CurseForge project name wrapped in an {@link Optional}. I recommend to make use of {@link Optional#isPresent()} and {@link Optional#get()}.
     */
    Optional<List<ServerPack>> findAllByProjectName(String projectName);

    /**
     * Find a server pack by its database id.
     * @author Griefed
     * @param fileID Integer. The database id with which to search for a server pack.
     * @return Returns a server pack for the passed database id wrapped in an {@link Optional}. I recommend to make use of {@link Optional#isPresent()} and {@link Optional#get()}.
     */
    Optional<ServerPack> findByFileID(int fileID);

    /**
     * Find a server pack by its CurseForge file display name.
     * @author Griefed
     * @param fileName String. The CurseForge file display name with which to search for a server pack.
     * @return Returns a server pack for the passed file display name wrapped in an {@link Optional}. I recommend to make use of {@link Optional#isPresent()} and {@link Optional#get()}.
     */
    Optional<ServerPack> findByFileName(String fileName);

    /**
     * Find all server packs by their status.
     * @author Griefed
     * @param status String. The status with which to search for server packs.
     * @return Returns a list of server packs for the passed status wrapped in an {@link Optional}. I recommend to make use of {@link Optional#isPresent()} and {@link Optional#get()}.
     */
    Optional<List<ServerPack>> findByStatus(String status);

    /**
     * Count all server packs by a CurseForge projectID.
     * @author Griefed
     * @param projectID Integer. The CurseForge project ID with which to count all server packs.
     * @return Integer. Returns the amount of server packs for the passed CurseForge projectID.
     */
    int countAllByProjectID(int projectID);

    /**
     * Count all server packs by a CurseForge project name.
     * @author Griefed
     * @param projectName String. The CurseForge project name with which to count all server packs.
     * @return Integer. Returns the amount of server packs for the passed CurseForge project name.
     */
    int countAllByProjectName(String projectName);

    /**
     * Delete all server packs with a CurseForge project ID.
     * @author Griefed
     * @param projectID The CurseForge project ID for which to delete all server packs.
     */
    void deleteAllByProjectID(int projectID);

    /**
     * Delete a server pack with a given CurseForge project and file ID.
     * @author Griefed
     * @param projectID Integer. The CurseForge project ID.
     * @param fileID Integer. The CurseForge file ID.
     */
    void deleteByProjectIDAndFileID(int projectID, int fileID);
}
