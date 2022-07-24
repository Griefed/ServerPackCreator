package de.griefed.serverpackcreator.utilities.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringUtilitiesTest {

  StringUtilities stringUtilities;

  StringUtilitiesTest() {
    stringUtilities = new StringUtilities();
  }

  @Test
  void buildStringTest() {
    List<String> args =
        new ArrayList<>(Arrays.asList("config", "mods", "scripts", "seeds", "defaultconfigs"));
    String result = stringUtilities.buildString(args.toString());
    Assertions.assertEquals(args.toString(), String.format("[%s]", result));
  }
}
