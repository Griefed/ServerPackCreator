package com.griefed;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        // Setup folders for client and server
        String packDir = "./Survive Create Prosper 4 1.16.5";
        String clientModDir = packDir + "/mods";
        String clientConfigDir = packDir + "/config";
        String clientDefaultconfigsDir = packDir + "/defaultconfigs";
        String clientScriptsDir = packDir + "/scripts";
        String serverDir = Folders.serverPath(packDir);
        String serverModsDir = Folders.serverModsPath(packDir);
        String serverConfigDir = Folders.serverConfigPath(packDir);
        String serverDefaultconfigsDir = Folders.serverDefaultconfigsPath(packDir);
        String serverScriptsDir = Folders.serverScriptsPath(packDir);

        //System.out.println(Mods.modsList(modDir));
        //System.out.println(Mods.modsServer(modDir));
    }
}
