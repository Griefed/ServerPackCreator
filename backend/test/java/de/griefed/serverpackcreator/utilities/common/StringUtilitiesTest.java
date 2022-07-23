package de.griefed.serverpackcreator.utilities.common;

import de.griefed.serverpackcreator.Dependencies;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringUtilitiesTest {

  StringUtilitiesTest() {}

  @Test
  void buildStringTest() {
    List<String> args =
        new ArrayList<>(Arrays.asList("config", "mods", "scripts", "seeds", "defaultconfigs"));
    String result =
        Dependencies.getInstance().UTILITIES().StringUtils().buildString(args.toString());
    Assertions.assertEquals(args.toString(), String.format("[%s]", result));
  }
}
