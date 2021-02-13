package de.griefed.serverPackCreator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class Server {
    // deleting clientside mods from serverpack
    public static void deleteClientMods(String modpackDir, List<String> clientMods) throws IOException {
        System.out.println("Deleting client side mods...");
        File serverMods = new File(modpackDir + "/server_pack/mods");
            for (File f : Objects.requireNonNull(serverMods.listFiles())) {
                for (int i = 0; i < clientMods.toArray().length; i++) {
                    if (f.getName().startsWith(clientMods.get(i))) {
                        f.delete();
                }
            }
        }
    }
}
