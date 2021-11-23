package de.griefed.serverpackcreator.spring.models;

public class ScanCurseProject extends Task {

    private final String projectIDAndFileID;

    /**
     *
     * @author Griefed
     * @param projectIDAndFileID
     */
    public ScanCurseProject(String projectIDAndFileID) {
        this.projectIDAndFileID = projectIDAndFileID;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    public String getProjectIDAndFileID() {
        return this.projectIDAndFileID;
    }

    /**
     *
     * @author Griefed
     * @return
     */
    @Override
    public String uniqueId() {
        return "SCAN_CURSEPROJECT_" + projectIDAndFileID;
    }
}

