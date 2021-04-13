package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.gui.ReferenceGUI;

public class Main {
    /**
     * Init and "main" has been moved to Handler-class. Main now only passes the cli args to Handler, which then runs the usual operations as they used to be in pre-2.x.x
     * @param args Commandline arguments with which ServerPackCreator is run. Passed to Handler-class which then decides what to do corresponding to input.
     */
    public static void main(String[] args) {
        Reference.handler.main(args);
    }
}
