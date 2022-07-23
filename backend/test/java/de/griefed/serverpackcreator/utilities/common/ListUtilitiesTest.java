package de.griefed.serverpackcreator.utilities.common;

import de.griefed.serverpackcreator.Dependencies;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ListUtilitiesTest {

  ListUtilitiesTest() {}

  @Test
  void encapsulateListElementsTest() {
    List<String> clientMods =
        new ArrayList<>(
            Arrays.asList(
                "A[mbient]Sounds",
                "Back[Tools",
                "Bett[er[][]Advancement",
                "Bett   erPing",
                "cheri[ ]shed",
                "ClientT&/$weaks",
                "Control§!%(?=)ling",
                "Defau/()&=?ltOptions",
                "durabi!§/&?lity",
                "DynamicS[]urroundings",
                "itemz\\oom",
                "jei-/($?professions",
                "jeiinteg}][ration",
                "JustEnoughResources",
                "MouseTweaks",
                "Neat",
                "OldJavaWarning",
                "PackMenu",
                "preciseblockplacing",
                "SimpleDiscordRichPresence",
                "SpawnerFix",
                "TipTheScales",
                "WorldNameRandomizer"));
    System.out.println(
        Dependencies.getInstance().UTILITIES().ListUtils().encapsulateListElements(clientMods));
    Assertions.assertNotNull(
        Dependencies.getInstance().UTILITIES().ListUtils().encapsulateListElements(clientMods));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"A[mbient]Sounds\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"Back[Tools\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"Bett[er[][]Advancement\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"Bett   erPing\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"cheri[ ]shed\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"ClientT&/$weaks\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"Control§!%(?=)ling\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"Defau/()&=?ltOptions\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"durabi!§/&?lity\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"DynamicS[]urroundings\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"itemz/oom\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"jei-/($?professions\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"jeiinteg}][ration\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"JustEnoughResources\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"MouseTweaks\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"Neat\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"OldJavaWarning\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"PackMenu\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"preciseblockplacing\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"SimpleDiscordRichPresence\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"SpawnerFix\""));
    Assertions.assertTrue(
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .encapsulateListElements(clientMods)
            .contains("\"WorldNameRandomizer\""));
  }

  @Test
  void cleanListTest() {
    Assertions.assertEquals(
        1,
        Dependencies.getInstance()
            .UTILITIES()
            .ListUtils()
            .cleanList(
                new ArrayList<>(
                    Arrays.asList(
                        "", " ", "  ", "   ", "    ", "     ", "", "", "", "", " ", "hamlo")))
            .size());
  }
}
