package de.griefed.serverpackcreator.modscanner;

import de.griefed.serverpackcreator.Dependencies;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ModScannerTest {

  @Test
  void tomlTest() {
    Collection<File> files =
        FileUtils.listFiles(
            new File("backend/test/resources/forge_tests/mods"), new String[] {"jar"}, true);

    List<File> excluded =
        new ArrayList<>(Dependencies.getInstance().MODSCANNER().tomls().scan(files));

    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/aaaaa.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/bbbbb.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/ccccc.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/ddddd.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/fffff.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/ggggg.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/hhhhh.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/iiiii.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/jjjjj.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/kkkkk.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/lllll.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/nnnnn.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/ppppp.jar")));

    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/qqqqq.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/rrrrr.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/testmod.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/uuuuu.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/vvvvv.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/wwwww.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/xxxxx.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/yyyyy.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_tests/mods/zzzzz.jar")));
  }

  @Test
  void fabricTest() {
    Collection<File> files =
        FileUtils.listFiles(
            new File("backend/test/resources/fabric_tests/mods"), new String[] {"jar"}, true);

    List<File> excluded =
        new ArrayList<>(Dependencies.getInstance().MODSCANNER().fabric().scan(files));

    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/aaaaa.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/bbbbb.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/ccccc.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/ddddd.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/eeeee.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/fffff.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/ggggg.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/hhhhh.jar")));

    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/iiiii.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/jjjjj.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/kkkkk.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/lllll.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/mmmmm.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/nnnnn.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/ooooo.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/ppppp.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/qqqqq.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/fabric_tests/mods/testmod.jar")));
  }

  @Test
  void quiltTest() {
    Collection<File> files =
        FileUtils.listFiles(
            new File("backend/test/resources/quilt_tests/mods"), new String[] {"jar"}, true);

    List<File> excluded =
        new ArrayList<>(Dependencies.getInstance().MODSCANNER().quilt().scan(files));

    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/quilt_tests/mods/aaaaa.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/quilt_tests/mods/bbbbb.jar")));

    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/quilt_tests/mods/testmod.jar")));
  }

  @Test
  void annotationTest() {
    Collection<File> files =
        FileUtils.listFiles(
            new File("backend/test/resources/forge_old/mods"), new String[] {"jar"}, true);

    List<File> excluded =
        new ArrayList<>(Dependencies.getInstance().MODSCANNER().annotations().scan(files));

    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_old/mods/aaaaa.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_old/mods/bbbbb.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_old/mods/ccccc.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_old/mods/ddddd.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_old/mods/eeeee.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_old/mods/fffff.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_old/mods/ggggg.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_old/mods/hhhhh.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_old/mods/iiiii.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_old/mods/jjjjj.jar")));
    Assertions.assertTrue(
        excluded.contains(new File("backend/test/resources/forge_old/mods/kkkkk.jar")));

    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_old/mods/lllll.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_old/mods/mmmmm.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_old/mods/nnnnn.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_old/mods/ooooo.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_old/mods/ppppp.jar")));
    Assertions.assertFalse(
        excluded.contains(new File("backend/test/resources/forge_old/mods/qqqqq.jar")));
  }
}
