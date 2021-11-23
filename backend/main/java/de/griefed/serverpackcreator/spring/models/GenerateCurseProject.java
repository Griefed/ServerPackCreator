package de.griefed.serverpackcreator.spring.models;

public class GenerateCurseProject extends Task {

    private final String projectIDAndFileID;

    /**
     *
     * @author Griefed
     * @param projectIDAndFileID
     */
    public GenerateCurseProject(String projectIDAndFileID) {
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
        return "GENERATE_CURSEPROJECT_" + projectIDAndFileID;
    }
}
