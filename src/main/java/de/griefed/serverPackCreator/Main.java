package de.griefed.serverPackCreator;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        //replace manual setting of modpack location by reading config file
        String packDir = "./Survive Create Prosper 4 1.16.5";
        System.out.println("Copying Mods...");
        CopyFiles.copyMods(packDir);
        System.out.println("Copying Configs...");
        CopyFiles.copyConfig(packDir);
        System.out.println("Copying Scripts...");
        CopyFiles.copyScripts(packDir);
        System.out.println("Copying Defaultconfigs...");
        CopyFiles.copyDefaultconfigs(packDir);

        System.out.println("All done!");
    }
}
