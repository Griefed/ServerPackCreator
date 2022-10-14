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

import de.griefed.serverpackcreator.ConfigurationModel;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.NotNull;

/**
 * Class containing all fields and therefore all information gathered from a submitted CurseForge
 * project and fileID, or modpack export. By extending {@link ConfigurationModel} we inherit all
 * basic fields required for the generation of a server pack and can add only those we require in
 * the REST API portion of ServerPackCreator.<br> We mark this class with {@link Entity} because we
 * also use this class for storing information in our database.
 *
 * @author Griefed
 */
@Entity
public class ServerPackModel extends ConfigurationModel {

  @Id
  @GeneratedValue
  @Column(updatable = false, nullable = false)
  int id;

  @Column
  String projectName;
  @Column
  String fileName;
  @Column
  String fileDiskName;
  @Column
  double size;
  @Column
  int downloads;
  @Column
  int confirmedWorking;
  @Column
  String status;
  @Column
  String path;

  @CreationTimestamp
  @Column(updatable = false)
  Timestamp dateCreated;

  @UpdateTimestamp
  Timestamp lastModified;

  /**
   * Constructor for our ServerPackModel.
   *
   * @author Griefed
   */
  public ServerPackModel() {
    this.projectName = "";
    this.fileName = "";
    this.fileDiskName = "";
    this.size = 0;
    this.downloads = 0;
    this.confirmedWorking = 0;
    this.status = "Queued";
  }

  /**
   * Constructor for our ServerPackModel, setting id, projectID, fileID, fileName, displayName,
   * size, downloads, confirmedWorking, status, dateCreated, lastModified manually.
   *
   * @param id               The ID of the server pack in our database.
   * @param fileName         The disk name of the CurseForge project file.
   * @param displayName      The display name of the CurseForge project file.
   * @param size             The size of the generated server pack, in MB.
   * @param downloads        The amount of times this server pack was downloaded.
   * @param confirmedWorking The amount of votes indicating whether this server pack works.
   * @param status           The status of this server pack. Either {@code Queued},
   *                         {@code Generating }, {@code Available}.
   * @param dateCreated      The date and time at which this server pack was requested for
   *                         generation.
   * @param lastModified     The date and time this server pack was last modified. Be it either due
   *                         to regeneration or something else.
   * @author Griefed
   */
  public ServerPackModel(
      int id,
      String fileName,
      String displayName,
      double size,
      int downloads,
      int confirmedWorking,
      String status,
      Timestamp dateCreated,
      Timestamp lastModified) {
    this.id = id;
    this.fileName = fileName;
    this.fileDiskName = displayName;
    this.size = size;
    this.downloads = downloads;
    this.confirmedWorking = confirmedWorking;
    this.status = status;
    this.dateCreated = dateCreated;
    this.lastModified = lastModified;
  }

  /**
   * Getter for the database id of a server pack.
   *
   * @return Returns the database of a server pack.
   * @author Griefed
   */
  public int getId() {
    return id;
  }

  /**
   * Setter for the database id of a server pack.
   *
   * @param id The database id of the server pack.
   * @author Griefed
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * Getter for the name of the project of the server pack.
   *
   * @return The project name of the server pack.
   * @author Griefed
   */
  @Override
  public String getProjectName() {
    return this.projectName;
  }

  /**
   * Setter for the project name of the project of the server pack.
   *
   * @param projectName The project name of the server pack.
   * @author Griefed
   */
  @Override
  public void setProjectName(@NotNull String projectName) {
    this.projectName = projectName;
  }

  /**
   * Getter for the file display name of the project file from which the server pack was generated.
   *
   * @return Returns the file display name of the project file from which the server pack
   * was generated.
   * @author Griefed
   */
  @Override
  public String getFileName() {
    return fileName;
  }

  /**
   * Setter for the file display name of the project file from which the server pack was generated.
   *
   * @param fileName The file display name of the project file from which the server pack was
   *                 generated.
   * @author Griefed
   */
  @Override
  public void setFileName(@NotNull String fileName) {
    this.fileName = fileName;
  }

  /**
   * Getter for the file disk name of the project file from which the server pack was generated.
   *
   * @return The file disk name of the project file from which the server pack was
   * generated.
   * @author Griefed
   */
  @Override
  public String getFileDiskName() {
    return fileDiskName;
  }

  /**
   * Setter for the file disk name of the project file from which the server pack was generated.
   *
   * @param fileDiskName The file disk name of the project file from which the server pack was
   *                     generated.
   * @author Griefed
   */
  @Override
  public void setFileDiskName(@NotNull String fileDiskName) {
    this.fileDiskName = fileDiskName;
  }

  /**
   * Getter for the size of the generated server pack in MB.
   *
   * @return Returns the size of the generated server pack in MB.
   * @author Griefed
   */
  public double getSize() {
    return size;
  }

  /**
   * Setter for the size of the generated server pack in MB.
   *
   * @param size Double. The size of the generated server pack in MB.
   * @author Griefed
   */
  public void setSize(double size) {
    this.size = size;
  }

  /**
   * Getter for the amount of downloads this server pack has received.
   *
   * @return Returns the amount of downloads this server pack has received.
   * @author Griefed
   */
  public int getDownloads() {
    return downloads;
  }

  /**
   * Setter for the amount of downloads this server pack has received.
   *
   * @param downloads The amount of downloads this server pack has received.
   * @author Griefed
   */
  public void setDownloads(int downloads) {
    this.downloads = downloads;
  }

  /**
   * Getter for the amount of votes indicating whether this server pack works. Positive values
   * indicate a working server pack. Negative values indicate the server pack is not working.
   *
   * @return Integer. Returns the amount of votes indicating whether this server pack works.
   * @author Griefed
   */
  public int getConfirmedWorking() {
    return confirmedWorking;
  }

  /**
   * Setter for the amount of votes indicating whether this server pack works. Positive values
   * indicate a working server pack. Negative values indicate the server pack is not working.
   *
   * @param confirmedWorking The amount of votes indicating whether this server pack works.
   * @author Griefed
   */
  public void setConfirmedWorking(int confirmedWorking) {
    this.confirmedWorking = confirmedWorking;
  }

  /**
   * Getter for the status of the server pack. Either {@code Queued}, {@code Generating},
   * {@code Available}.
   *
   * @return String. Returns the status of a server pack.
   * @author Griefed
   */
  public String getStatus() {
    return status;
  }

  /**
   * Setter for the status of a server pack. Either {@code Queued}, {@code Generating},
   * {@code Available}.
   *
   * @param status The status of a server pack.
   * @author Griefed
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * Getter for the path to the generated server pack.
   *
   * @return String. Returns the path to the generated server pack.
   * @author Griefed
   */
  public String getPath() {
    return path;
  }

  /**
   * Setter for the path to the generated server pack.
   *
   * @param path The path to the generated server pack.
   * @author Griefed
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Getter for the date and time at which this server pack entry was created as a
   * {@link Timestamp}.
   *
   * @return The date and time at which this server pack entry was created as a {@link Timestamp}.
   * @author Griefed
   */
  public Timestamp getDateCreated() {
    return dateCreated;
  }

  /**
   * Setter for the date and time at which this server pack entry was created as a
   * {@link Timestamp}.
   *
   * @param dateCreated The date and time at which this server pack was created as a
   *                    {@link Timestamp}.
   * @author Griefed
   */
  public void setDateCreated(Timestamp dateCreated) {
    this.dateCreated = dateCreated;
  }

  /**
   * Getter for the date and time at which this server pack entry was last modified as a
   * {@link Timestamp}.
   *
   * @return The date and time at which this server pack entry was last modified as a
   * {@link Timestamp}.
   * @author Griefed
   */
  public Timestamp getLastModified() {
    return lastModified;
  }

  /**
   * Setter for the date and time at which this server pack entry was last modified as a
   * {@link Timestamp}.
   *
   * @param lastModified The date and time at which this server pack entry was last modified as a
   *                     {@link Timestamp}.
   * @author Griefed
   */
  public void setLastModified(Timestamp lastModified) {
    this.lastModified = lastModified;
  }

  /**
   * String concatenation of all important values of our server pack entry.
   *
   * @return String. Returns all important information of a server pack entry as a concatenated
   * string.
   * @author Griefed
   */
  public String repositoryToString() {
    return "ServerPackModel{"
        + "id="
        + id
        + ", projectName='"
        + projectName
        + '\''
        + ", fileName='"
        + fileName
        + '\''
        + ", fileDiskName='"
        + fileDiskName
        + '\''
        + ", size="
        + size
        + ", downloads="
        + downloads
        + ", confirmedWorking="
        + confirmedWorking
        + ", status='"
        + status
        + '\''
        + ", dateCreated="
        + dateCreated
        + ", lastModified="
        + lastModified
        + '}';
  }
}
