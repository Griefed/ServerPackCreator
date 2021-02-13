package de.griefed.serverPackCreator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Server {
    // Delete clientside mods from serverpack
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
    // Create zip archive of serverpack.
    public static void zipBuilder(String modpackDir) {
        System.out.println("Creating zip archive of serverpack...");
        final Path sourceDir = Paths.get(modpackDir + "/server_pack");
        String zipFileName = sourceDir.toString().concat(".zip");
        try {
            final ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    try {
                        Path targetFile = sourceDir.relativize(file);
                        outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                        byte[] bytes = Files.readAllBytes(file);
                        outputStream.write(bytes, 0, bytes.length);
                        outputStream.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
