package de.griefed.serverpackcreator.utilities;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UpdateCheckerTest {

    private final UpdateChecker UPDATECHECKER;
    private final ApplicationProperties APPLICATIONPROPERTIES;

    UpdateCheckerTest() {

        this.APPLICATIONPROPERTIES = new ApplicationProperties();
        this.UPDATECHECKER = new UpdateChecker(new LocalizationManager(APPLICATIONPROPERTIES),APPLICATIONPROPERTIES);

    }

    @Test
    void refreshTest() {
        Assertions.assertNotNull(UPDATECHECKER.refresh());
    }

    @Test
    void checkForUpdateTest() {
        Assertions.assertNotNull(UPDATECHECKER.checkForUpdate());
    }

}
