package com.griefed;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Mods {
    // This class will be all about copy and paste and deleting mods from client to server

    public static void copyMods(String clientModDir, String serverModsDir) throws IOException {
        Files.copy(Paths.get(clientModDir), Paths.get(serverModsDir));
    }


/*
    public static List modsClient(String modDir) { // Create array with all mods inside mod directory to later compare against client side mods

        UNCOMMENT LATER ON WHEN PATH IS PASSED BY MAIN AFTER READING CONFIG
        String modDir = "./Survive Create Prosper 4 1.16.5/mods";

        List<String> modsList = new ArrayList<String>(); // Create new list which will be filled with all mods in modpack
        try {
            File f = new File(modDir);
            FilenameFilter filter = (f1, name) -> {
                return name.endsWith(".jar"); // We want to find only .jar files in our mods directory
            };
            File[] files = f.listFiles(filter);
            for (int i = 0; i < files.length; i++) {
                    modsList.add(files[i].getName().toString());// Get the names of the files by using the .getName() method
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return modsList;
    }

    public boolean modsCheck(String modDir, String[] modsclient) {
        File f = new File("C:\\");
        File[] list = f.listFiles();
        String[] mods = {
                "AmbientSounds",
                "BackTools",
                "BetterAdvancement",
                "BetterPing",
                "cherished",
                "ClientTweaks",
                "Controlling",
                "DefaultOptions",
                "durability",
                "DynamicSurroundings",
                "itemzoom",
                "jei-professions",
                "jeiintegration",
                "JustEnoughResources",
                "MouseTweaks",
                "Neat",
                "OldJavaWarning",
                "PackMenu",
                "preciseblockplacing",
                "SimpleDiscordRichPresence",
                "SpawnerFix",
                "TipTheScales",
                "WorldNameRandomizer"
        };
        return modsCheck;
    }
*/
}
