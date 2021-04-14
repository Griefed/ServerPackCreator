package de.griefed.serverpackcreator;

public class Main {
    /**
     * Init and "main" has been moved to Handler-class. Main now only passes the cli args to Handler, which then runs the usual operations as they used to be in pre-2.x.x
     * This had to be done so I could work in a non-static environment, which is required in some parts of the GUI.
     * @param args Commandline arguments with which ServerPackCreator is run. Passed to Handler-class which then decides what to do corresponding to input.
     */
    public static void main(String[] args) {
        Reference.handler.main(args);
    }
}
