package de.griefed.serverpackcreator.curseforgemodpack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class Files {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String projectID;
    private String fileID;

    public String getProjectID() {
        return projectID;
    }

    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    public String getFileID() {
        return fileID;
    }

    public void setFileID(String fileID) {
        this.fileID = fileID;
    }

    @Override
    public String toString() {
        return String.format("%s,%s",projectID,fileID);
    }
}