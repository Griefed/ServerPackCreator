/* Copyright (C) 2022  Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.utilities;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility-class revolving around the system we are running on.
 * @author Griefed
 */
@Component
public class SystemUtilities {

    private static final Logger LOG = LogManager.getLogger(SystemUtilities.class);

    private final List<String> DRIVES = new ArrayList<>(Arrays.asList(
            "A:","B:","C:","D:","E:","F:","G:","H:","I:","J:","K:","L:","M:","N:","O:","P:","Q:","R:","S:","T:","U:","V:","W:","X:","Y:","Z:"
    ));

    @Autowired
    public SystemUtilities() {

    }

    /**
     * Automatically acquire the path to the systems default Java installation.
     * @author Griefed
     * @return String. The path to the systems default Java installation.
     */
    public String acquireJavaPathFromSystem() {

        LOG.debug("Acquiring path to Java installation from system properties...");

        String javaPath = "Couldn't acquire JavaPath";

        if (new File(System.getProperty("java.home")).exists()) {
            javaPath = String.format("%s/bin/java",System.getProperty("java.home").replace("\\", "/"));

            if (!javaPath.startsWith("/")) {
                for (String letter : DRIVES) {
                    if (javaPath.startsWith(letter)) {

                        LOG.debug("We're running on Windows. Ensuring javaPath ends with .exe");
                        javaPath = String.format("%s.exe", javaPath);
                    }
                }
            }
        }

        return javaPath;
    }

    /**
     * Download the file from the specified URL to the specified destination. The destination should end in a valid filename.
     * Any directories up to the specified file will be created.
     * @author Griefed
     * @param fileDestination String. The destination where the file should be stored. Must include the filename as well.
     *                    Examples:<br>/tmp/some_folder/foo.bar<br>C:/temp/some_folder/bar.foo
     * @param downloadURL URL. The URL to the file you want to download.
     * @return Boolean. Returns true if the file could be found on the hosts filesystem.
     */
    public boolean downloadFile(String fileDestination, URL downloadURL) {

        try {
            FileUtils.createParentDirectories(new File(fileDestination));
        } catch (IOException ignored) {}

        ReadableByteChannel readableByteChannel = null;
        FileOutputStream fileOutputStream = null;
        FileChannel fileChannel = null;

        try {

            readableByteChannel = Channels.newChannel(downloadURL.openStream());

            fileOutputStream = new FileOutputStream(fileDestination.replace("\\","/"));

            fileChannel = fileOutputStream.getChannel();

            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

        } catch (IOException ex) {
            LOG.error("An error occurred downloading " + fileDestination.replace("\\","/") + ".", ex);
            FileUtils.deleteQuietly(new File(fileDestination.replace("\\","/")));
        } finally {
            try {
                //noinspection ConstantConditions
                fileOutputStream.flush();
            } catch (Exception ignored) {}
            try {
                fileOutputStream.close();
            } catch (Exception ignored) {}
            try {
                //noinspection ConstantConditions
                readableByteChannel.close();
            } catch (Exception ignored) {}
            try {
                //noinspection ConstantConditions
                fileChannel.close();
            } catch (Exception ignored) {}
        }

        return new File(fileDestination).exists();
    }

    /**
     * Move a file from source to destination, and replace the destination file if it exists.
     * @author Griefed
     * @param sourceFile The source file.
     * @param destinationFile The destination file to be replaced by the source file.
     * @return Boolean. Returns true if the file was sucessfully replaced.
     * @throws IOException Thrown if an error occurs when the file is moved.
     */
    public boolean replaceFile(File sourceFile, File destinationFile) throws IOException {

        if (sourceFile.exists() && destinationFile.delete()) {

            FileUtils.moveFile(sourceFile, destinationFile);
            return true;

        }

        LOG.error("Source file not found.");

        return false;
    }

    /**
     * Unzips the downloaded modpack ZIP-archive to the specified directory.
     * @author Griefed
     * @param zipFile String. The path to the ZIP-archive which we want to unzip.
     * @param destinationDirectory The directory into which the ZIP-archive will be unzipped into.
     */
    public void unzipArchive(String zipFile, String destinationDirectory) {
        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info("Extracting ZIP-file: " + zipFile);
        try {
            new ZipFile(zipFile).extractAll(destinationDirectory);
        } catch (ZipException ex) {
            LOG.error("Error: There was an error extracting the archive " + zipFile, ex);
        }
    }

}