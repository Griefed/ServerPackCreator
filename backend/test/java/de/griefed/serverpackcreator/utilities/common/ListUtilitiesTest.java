package de.griefed.serverpackcreator.utilities.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ListUtilitiesTest {

  ListUtilities listUtilities;

  ListUtilitiesTest() {
    listUtilities = new ListUtilities();
  }

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
    System.out.println(listUtilities.encapsulateListElements(clientMods));
    Assertions.assertNotNull(listUtilities.encapsulateListElements(clientMods));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"A[mbient]Sounds\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"Back[Tools\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"Bett[er[][]Advancement\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"Bett   erPing\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"cheri[ ]shed\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"ClientT&/$weaks\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"Control§!%(?=)ling\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"Defau/()&=?ltOptions\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"durabi!§/&?lity\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"DynamicS[]urroundings\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"itemz/oom\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"jei-/($?professions\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"jeiinteg}][ration\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"JustEnoughResources\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"MouseTweaks\""));
    Assertions.assertTrue(listUtilities.encapsulateListElements(clientMods).contains("\"Neat\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"OldJavaWarning\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"PackMenu\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"preciseblockplacing\""));
    Assertions.assertTrue(
        listUtilities
            .encapsulateListElements(clientMods)
            .contains("\"SimpleDiscordRichPresence\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"SpawnerFix\""));
    Assertions.assertTrue(
        listUtilities.encapsulateListElements(clientMods).contains("\"WorldNameRandomizer\""));
  }

  @Test
  void cleanListTest() {
    Assertions.assertEquals(
        1,
        listUtilities
            .cleanList(
                new ArrayList<>(
                    Arrays.asList(
                        "", " ", "  ", "   ", "    ", "     ", "", "", "", "", " ", "hamlo")))
            .size());
  }
}
