package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.IncorrectLanguageException;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class Main {
    private static final Logger appLogger = LogManager.getLogger(Main.class);

    /**
     * Init and "main" has been moved to Handler-class. Main now only inits the Localizationmanager and passes the cli args, if any,  to Handler, which then runs the usual operations as they used to be in pre-2.x.x
     * @param args Commandline arguments with which ServerPackCreator is run. Passed to Handler-class which then decides what to do corresponding to input.
     */
    public static void main(String[] args) {
        List<String> programArgs = Arrays.asList(args);
        if (Arrays.asList(args).contains(Reference.LANG_ARGUMENT)) {
            try {
                LocalizationManager.init(programArgs.get(programArgs.indexOf(Reference.LANG_ARGUMENT) + 1));
            } catch (IncorrectLanguageException e) {
                appLogger.info(programArgs.get(programArgs.indexOf(Reference.LANG_ARGUMENT) + 1));
                appLogger.error("Incorrect language specified, falling back to English (United States)...");
                LocalizationManager.init();
            } finally {
                Handler.main(args);
            }
        } else {
            FilesSetup.checkLocaleFile();
            Handler.main(args);
        }
    }
}
