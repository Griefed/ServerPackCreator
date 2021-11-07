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
package de.griefed.serverpackcreator.spring.models;

import de.griefed.serverpackcreator.ConfigurationModel;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 *
 * @author Griefed
 */
@Entity
public class ServerPack extends ConfigurationModel {

    /**
     *
     * @author Griefed
     */
    public ServerPack() {

    }

    /**
     *
     * @author Griefed
     * @param projectID
     * @param fileID
     */
    public ServerPack(int projectID, int fileID) {
        this.projectID = projectID;
        this.fileID = fileID;
        this.projectName = "";
        this.fileName = "";
        this.fileDiskName = "";
        this.size = 0;
        this.downloads = 0;
        this.confirmedWorking = 0;
        this.status = "Queued";
    }

    /**
     *
     * @author Griefed
     * @param id
     * @param projectID
     * @param fileID
     * @param fileName
     * @param displayName
     * @param size
     * @param downloads
     * @param confirmedWorking
     * @param status
     * @param dateCreated
     * @param lastModified
     */
    public ServerPack(int id, int projectID, int fileID, String fileName, String displayName, double size, int downloads, int confirmedWorking, String status, Timestamp dateCreated, Timestamp lastModified) {
        this.id = id;
        this.projectID = projectID;
        this.fileID = fileID;
        this.fileName = fileName;
        this.fileDiskName = displayName;
        this.size = size;
        this.downloads = downloads;
        this.confirmedWorking = confirmedWorking;
        this.status = status;
        this.dateCreated = dateCreated;
        this.lastModified = lastModified;
    }

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    int id;

    // TODO: Expand with foreign key pointing towards project_table
    @Column
    int projectID;

    // TODO: Expand with foreign key pointing towards file_table
    @Column
    int fileID;

    // TODO: Move to project_table
    @Column
    String projectName;

    // TODO: Move to file_table
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

    @CreationTimestamp
    @Column(updatable = false)
    Timestamp dateCreated;

    @UpdateTimestamp
    Timestamp lastModified;

    /**
     *
     * @author Griefed
     * @return
     */
    public int getId() {
        return id;
    }

    /**
     *
     * @author Griefed
     * @param id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    @Override
    public int getProjectID() {
        return projectID;
    }

    /**
     *
     * @author Griefed
     * @param projectID
     */
    @Override
    public void setProjectID(int projectID) {
        this.projectID = projectID;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    @Override
    public int getFileID() {
        return fileID;
    }

    /**
     *
     * @author Griefed
     * @param fileID
     */
    @Override
    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    @Override
    public String getProjectName() {
        return this.projectName;
    }

    /**
     *
     * @author Griefed
     * @param projectName String. The name of the CurseForge project.
     */
    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    @Override
    public String getFileName() {
        return fileName;
    }

    /**
     *
     * @author Griefed
     * @param fileName String. The name of the CurseForge project file.
     */
    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    @Override
    public String getFileDiskName() {
        return fileDiskName;
    }

    /**
     *
     * @author Griefed
     * @param fileDiskName
     */
    @Override
    public void setFileDiskName(String fileDiskName) {
        this.fileDiskName = fileDiskName;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    public double getSize() {
        return size;
    }

    /**
     *
     * @author Griefed
     * @param size
     */
    public void setSize(double size) {
        this.size = size;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    public int getDownloads() {
        return downloads;
    }

    /**
     *
     * @author Griefed
     * @param downloads
     */
    public void setDownloads(int downloads) {
        this.downloads = downloads;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    public int getConfirmedWorking() {
        return confirmedWorking;
    }

    /**
     *
     * @author Griefed
     * @param confirmedWorking
     */
    public void setConfirmedWorking(int confirmedWorking) {
        this.confirmedWorking = confirmedWorking;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    public String getStatus() {
        return status;
    }

    /**
     *
     * @author Griefed
     * @param downloadUrl
     */
    public void setStatus(String downloadUrl) {
        this.status = downloadUrl;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    public Timestamp getDateCreated() {
        return dateCreated;
    }

    /**
     *
     * @author Griefed
     * @param dateCreated
     */
    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    public Timestamp getLastModified() {
        return lastModified;
    }

    /**
     *
     * @author Griefed
     * @param lastModified
     */
    public void setLastModified(Timestamp lastModified) {
        this.lastModified = lastModified;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    public String repositoryToString() {
        return "ServerPack{" +
                "id=" + id +
                ", projectID=" + projectID +
                ", fileID=" + fileID +
                ", projectName='" + projectName + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileDiskName='" + fileDiskName + '\'' +
                ", size=" + size +
                ", downloads=" + downloads +
                ", confirmedWorking=" + confirmedWorking +
                ", status='" + status + '\'' +
                ", dateCreated=" + dateCreated +
                ", lastModified=" + lastModified +
                '}';
    }
}
