package com.griefed;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        // Setup folders for client and server
        String packDir = "./Survive Create Prosper 4 1.16.5";
        CopyFiles.copyMods(packDir);
        CopyFiles.copyConfig(packDir);
        CopyFiles.copyScripts(packDir);
        CopyFiles.copyDefaultconfigs(packDir);
        System.out.println("All done!");
    }
}
