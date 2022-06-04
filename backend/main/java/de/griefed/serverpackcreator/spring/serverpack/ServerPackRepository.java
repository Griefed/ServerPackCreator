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

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Our JPA repository for storing and retrieving {@link ServerPackModel}s to and from our SQLite database. Provides methods for
 * retrieving, storing, deleting. Calls must always be made from {@link ServerPackService} and not directly
 * to this repository. It may seem strange, but this accomplishes a couple of things:<br>
 * 1. Centralized access to the repository.<br>
 * 2. Complete control over what happens with the data retrieved from the repository.<br>
 * 3. Complete control over what happens with the data passed to the repository.<br>
 * 4. Better management and overview of what we are doing with our repository.
 *
 * @author Griefed
 */
@Repository
public interface ServerPackRepository extends CrudRepository<ServerPackModel, Integer> {

    /**
     * Find all server packs using a CurseForge project name.
     *
     * @param projectName String. The project name with which to search for server packs.
     * @return Returns a list of all server packs for the passed CurseForge project name wrapped in an {@link Optional}. I recommend to make use of {@link Optional#isPresent()} and {@link Optional#get()}.
     * @author Griefed
     */
    Optional<List<ServerPackModel>> findAllByProjectName(String projectName);

    /**
     * Find a server pack by its CurseForge file display name.
     *
     * @param fileName String. The CurseForge file display name with which to search for a server pack.
     * @return Returns a server pack for the passed file display name wrapped in an {@link Optional}. I recommend to make use of {@link Optional#isPresent()} and {@link Optional#get()}.
     * @author Griefed
     */
    Optional<ServerPackModel> findByFileName(String fileName);

    /**
     * Find all server packs by their status.
     *
     * @param status String. The status with which to search for server packs.
     * @return Returns a list of server packs for the passed status wrapped in an {@link Optional}. I recommend to make use of {@link Optional#isPresent()} and {@link Optional#get()}.
     * @author Griefed
     */
    Optional<List<ServerPackModel>> findByStatus(String status);

    /**
     * Count all server packs by a CurseForge project name.
     *
     * @param projectName String. The CurseForge project name with which to count all server packs.
     * @return Integer. Returns the amount of server packs for the passed CurseForge project name.
     * @author Griefed
     */
    int countAllByProjectName(String projectName);

}
