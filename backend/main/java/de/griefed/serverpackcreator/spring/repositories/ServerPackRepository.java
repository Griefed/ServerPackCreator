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
 *
 * @author Griefed
 */
@Repository
public interface ServerPackRepository extends CrudRepository<ServerPack, Integer> {

    /**
     *
     * @author Griefed
     * @param projectID
     * @return
     */
    List<ServerPack> findAllByProjectID(int projectID);

    /**
     *
     * @author Griefed
     * @param projectID
     * @param fileID
     * @return
     */
    Optional<ServerPack> findByProjectIDAndFileID(int projectID, int fileID);

    /**
     *
     * @author Griefed
     * @param projectName
     * @return
     */
    List<ServerPack> findAllByProjectName(String projectName);

    /**
     *
     * @author Griefed
     * @param fileID
     * @return
     */
    Optional<ServerPack> findByFileID(int fileID);

    /**
     *
     * @author Griefed
     * @param fileName
     * @return
     */
    Optional<ServerPack> findByFileName(String fileName);

    /**
     *
     * @author Griefed
     * @param status
     * @return
     */
    Optional<ServerPack> findByStatus(String status);

    /**
     *
     * @author Griefed
     * @param projectID
     * @return
     */
    int countAllByProjectID(int projectID);

    /**
     *
     * @author Griefed
     * @param projectName
     * @return
     */
    int countAllByProjectName(String projectName);

    /**
     *
     * @author Griefed
     * @param projectID
     */
    void deleteAllByProjectID(int projectID);

    /**
     *
     * @author Griefed
     * @param projectID
     * @param fileID
     */
    void deleteByProjectIDAndFileID(int projectID, int fileID);
}
