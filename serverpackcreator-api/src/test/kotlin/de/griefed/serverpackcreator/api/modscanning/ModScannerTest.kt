package de.griefed.serverpackcreator.api.modscanning

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.utilities.common.FilterType
import de.griefed.serverpackcreator.api.utilities.common.filteredWalk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class ModScannerTest internal constructor() {
    private var modScanner: ModScanner =
        ApiWrapper.api(File("src/test/resources/serverpackcreator.properties")).modScanner

    @Suppress("SpellCheckingInspection")
    @Test
    fun tomlTest() {
        val files: Collection<File> =
            File("src/test/resources/forge_tests/mods").filteredWalk(listOf("jar"), FilterType.ENDS_WITH)

        val excluded: List<File> = modScanner.forgeTomlScanner.scan(files).first.toList() //Pair<Collection<File>, Collection<Pair<String,String>>>
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_tests/mods/aaaaa.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_tests/mods/bbbbb.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_tests/mods/ccccc.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_tests/mods/ddddd.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_tests/mods/fffff.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_tests/mods/ggggg.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_tests/mods/hhhhh.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_tests/mods/iiiii.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_tests/mods/jjjjj.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_tests/mods/kkkkk.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_tests/mods/lllll.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_tests/mods/nnnnn.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_tests/mods/ppppp.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_tests/mods/qqqqq.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_tests/mods/rrrrr.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_tests/mods/testmod.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_tests/mods/uuuuu.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_tests/mods/vvvvv.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_tests/mods/wwwww.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_tests/mods/xxxxx.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_tests/mods/yyyyy.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_tests/mods/zzzzz.jar"))
        )
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun fabricTest() {
        val files: Collection<File> =
            File("src/test/resources/fabric_tests/mods").filteredWalk(listOf("jar"), FilterType.ENDS_WITH)

        val excluded: List<File> = modScanner.fabricScanner.scan(files).first.toList()
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/fabric_tests/mods/aaaaa.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/fabric_tests/mods/bbbbb.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/fabric_tests/mods/ccccc.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/fabric_tests/mods/ddddd.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/fabric_tests/mods/eeeee.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/fabric_tests/mods/fffff.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/fabric_tests/mods/ggggg.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/fabric_tests/mods/hhhhh.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/fabric_tests/mods/iiiii.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/fabric_tests/mods/jjjjj.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/fabric_tests/mods/kkkkk.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/fabric_tests/mods/lllll.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/fabric_tests/mods/mmmmm.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/fabric_tests/mods/nnnnn.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/fabric_tests/mods/ooooo.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/fabric_tests/mods/ppppp.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/fabric_tests/mods/qqqqq.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/fabric_tests/mods/testmod.jar"))
        )
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun quiltTest() {
        val files: Collection<File> =
            File("src/test/resources/quilt_tests/mods").filteredWalk(listOf("jar"), FilterType.ENDS_WITH)

        val excluded: List<File> = modScanner.quiltScanner.scan(files).first.toList()
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/quilt_tests/mods/aaaaa.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/quilt_tests/mods/bbbbb.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/quilt_tests/mods/testmod.jar"))
        )
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun annotationTest() {
        val files: Collection<File> =
            File("src/test/resources/forge_old/mods").filteredWalk(listOf("jar"), FilterType.ENDS_WITH)

        val excluded: List<File> = modScanner.forgeAnnotationScanner.scan(files).first.toList()
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_old/mods/aaaaa.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_old/mods/bbbbb.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_old/mods/ccccc.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_old/mods/ddddd.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_old/mods/eeeee.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_old/mods/fffff.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_old/mods/ggggg.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_old/mods/hhhhh.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_old/mods/iiiii.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_old/mods/jjjjj.jar"))
        )
        Assertions.assertTrue(
            excluded.contains(File("src/test/resources/forge_old/mods/kkkkk.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_old/mods/lllll.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_old/mods/mmmmm.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_old/mods/nnnnn.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_old/mods/ooooo.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_old/mods/ppppp.jar"))
        )
        Assertions.assertFalse(
            excluded.contains(File("src/test/resources/forge_old/mods/qqqqq.jar"))
        )
    }
}