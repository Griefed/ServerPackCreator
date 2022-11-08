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
package de.griefed.serverpackcreator.spring;

import de.griefed.serverpackcreator.spring.serverpack.ServerPackModel;
import de.griefed.serverpackcreator.spring.serverpack.ServerPackService;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

/**
 * Schedules to cover all kinds of aspects of ServerPackCreator.
 *
 * @author Griefed
 */
@Service
public class Schedules {

  private static final Logger LOG = LogManager.getLogger(Schedules.class);

  private final ServerPackService SERVERPACKSERVICE;
  private final VersionMeta VERSIONMETA;

  /**
   * Constructor for DI.
   *
   * @param injectedServerPackService Instance of {@link ServerPackService}.
   * @param injectedVersionMeta       Instance of {@link VersionMeta}.
   * @author Griefed
   */
  @Autowired
  public Schedules(ServerPackService injectedServerPackService,
                   VersionMeta injectedVersionMeta) {

    SERVERPACKSERVICE = injectedServerPackService;
    VERSIONMETA = injectedVersionMeta;
  }

  /**
   * Check the database every
   * {@code de.griefed.serverpackcreator.spring.schedules.database.cleanup } for validity. <br>
   * Deletes entries from the database which are older than 1 week and have 0 downloads. <br>
   * Deletes entries whose status is {@code Available} but no server pack ZIP-archive can be found.
   * <br>
   *
   * @author Griefed
   */
  @Scheduled(cron = "${de.griefed.serverpackcreator.spring.schedules.database.cleanup}")
  private void cleanDatabase() {
    if (!SERVERPACKSERVICE.getServerPacks().isEmpty()) {

      LOG.info("Cleaning database...");

      for (ServerPackModel pack : SERVERPACKSERVICE.getServerPacks()) {

        if ((new Timestamp(new Date().getTime()).getTime() - pack.getLastModified().getTime())
            >= 604800000
            && pack.getDownloads() == 0) {

          deletePack(pack);

        } else if (pack.getStatus().equals("Available") && !new File(pack.getPath()).isFile()) {

          deletePack(pack);

        } else if (pack.getStatus().equals("Generating")
            && (new Timestamp(new Date().getTime()).getTime() - pack.getLastModified().getTime())
            >= 86400000) {

          deletePack(pack);

        } else {
          LOG.info("No database entries to clean up.");
        }
      }

      LOG.info("Database cleanup completed.");
    }
  }

  private void deletePack(ServerPackModel pack) {
    LOG.info("Deleting archive " + pack.getPath());
    FileUtils.deleteQuietly(new File(pack.getPath()));

    LOG.info(
        "Deleting folder " + pack.getPath().replace("_server_pack-zip", ""));
    FileUtils.deleteQuietly(
        new File(pack.getPath().replace("_server_pack-zip", "")));

    LOG.info("Cleaned server pack " + pack.getId() + " from database.");
    SERVERPACKSERVICE.deleteServerPack(pack.getId());
  }

  @Scheduled(cron = "${de.griefed.serverpackcreator.spring.schedules.files.cleanup}")
  private void cleanFiles() {
    if (!SERVERPACKSERVICE.getServerPacks().isEmpty()) {

      LOG.info("Cleaning files...");

      for (ServerPackModel pack : SERVERPACKSERVICE.getServerPacks()) {

        if (new File(pack.getPath()).isFile()
            && new File(pack.getPath().replace("_server_pack-zip", "")).isDirectory()) {

          LOG.info(
              "Deleting folder "
                  + pack.getPath().replace("_server_pack-zip", ""));
          FileUtils.deleteQuietly(
              new File(pack.getPath().replace("_server_pack-zip", "")));

        } else {
          LOG.info("No files to clean up.");
        }
      }

      LOG.info("File cleanup completed.");
    }
  }

  @Scheduled(cron = "${de.griefed.serverpackcreator.spring.schedules.versions.refresh}")
  private void refreshVersionLister() {
    try {
      VERSIONMETA.update();
    } catch (IOException | ParserConfigurationException | SAXException ex) {
      LOG.error("Could not update VersionMeta.", ex);
    }
  }
}
