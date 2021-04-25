package de.griefed.serverpackcreator;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

class CLISetupTest {
    @Mock
    Logger appLogger;

    @InjectMocks
    CLISetup cLISetup;

    @BeforeEach
    void setUp() {
        FilesSetup.checkLocaleFile();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testBuildString() {
        List<String> args = Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        );
        String result = Reference.cliSetup.buildString(args.toString());
        Assertions.assertEquals(args.toString(), String.format("[%s]",result));
    }
}
