package de.griefed.serverpackcreator.utilities.commonutilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringUtilitiesTest {

    private final StringUtilities STRINGUTILITIES;

    StringUtilitiesTest() {
        this.STRINGUTILITIES = new StringUtilities();
    }

    @Test
    void buildStringTest() {
        List<String> args = new ArrayList<>(Arrays.asList(
                "config",
                "mods",
                "scripts",
                "seeds",
                "defaultconfigs"
        ));
        String result = STRINGUTILITIES.buildString(args.toString());
        Assertions.assertEquals(args.toString(), String.format("[%s]",result));
    }
}
