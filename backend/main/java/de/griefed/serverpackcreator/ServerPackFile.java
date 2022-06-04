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
package de.griefed.serverpackcreator;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A ServerPackFile represents a source-destination-combination of two files/directories. The source is the file/directory, usually in the modpack,
 * whilst the destination is the file to which the source is supposed to be copied to in the server pack.
 *
 * @author Griefed
 */
public class ServerPackFile {

    private static final Logger LOG = LogManager.getLogger(ServerPackFile.class);

    private final File SOURCE_FILE;
    private final Path SOURCE_PATH;
    private final File DESTINATION_FILE;
    private final Path DESTINATION_PATH;

    /**
     * Construct a new ServerPackFile from two {@link File}-objects, a source and a destination.
     *
     * @param sourceFile      {@link File} The source file/directory. Usually a file/directory in a modpack.
     * @param destinationFile {@link File} The destination file/directory in the server pack.
     * @author Griefed
     */
    public ServerPackFile(File sourceFile, File destinationFile) throws InvalidPathException {
        this.SOURCE_FILE = sourceFile;
        this.SOURCE_PATH = sourceFile.toPath();
        this.DESTINATION_FILE = destinationFile;
        this.DESTINATION_PATH = destinationFile.toPath();
    }

    /**
     * Construct a new ServerPackFile from two {@link String}-objects, a source and a destination.
     *
     * @param sourceFile      {@link String} The source file/directory. Usually a file/directory in a modpack.
     * @param destinationFile {@link String} The destination file/directory in the server pack.
     * @author Griefed
     */
    public ServerPackFile(String sourceFile, String destinationFile) throws NullPointerException, InvalidPathException {
        this.SOURCE_FILE = new File(sourceFile);
        this.SOURCE_PATH = SOURCE_FILE.toPath();
        this.DESTINATION_FILE = new File(destinationFile);
        this.DESTINATION_PATH = DESTINATION_FILE.toPath();
    }

    /**
     * Construct a new ServerPackFile from two {@link Path}-objects, a source and a destination.
     *
     * @param sourcePath      {@link Path} The source file/directory. Usually a file/directory in a modpack.
     * @param destinationPath {@link Path} The destination file/directory in the server pack.
     * @author Griefed
     */
    public ServerPackFile(Path sourcePath, Path destinationPath) throws UnsupportedOperationException {
        this.SOURCE_FILE = sourcePath.toFile();
        this.SOURCE_PATH = sourcePath;
        this.DESTINATION_FILE = destinationPath.toFile();
        this.DESTINATION_PATH = destinationPath;
    }

    /**
     * The source-file.
     *
     * @return {@link File} The source-file.
     * @author Griefed
     */
    public File source() {
        return SOURCE_FILE;
    }

    /**
     * The destination-file.
     *
     * @return {@link File} The destination-file.
     * @author Griefed
     */
    public File destination() {
        return DESTINATION_FILE;
    }

    /**
     * The path to the source-file.
     *
     * @return {@link Path} The path to the source-file.
     * @author Griefed
     */
    public Path sourcePath() {
        return SOURCE_PATH;
    }

    /**
     * The path to the destination-file.
     *
     * @return {@link Path} The path to the destination-file.
     * @author Griefed
     */
    public Path destinationPath() {
        return DESTINATION_PATH;
    }

    /**
     * Copy this ServerPackFiles source to the destination. Already existing files are replaced. When the source-file is
     * a directory, then the destination-directory is created as an empty directory. Any contents in the source-directory
     * are NOT copied over to the destination-directory. See {@link Files#copy(Path, Path, CopyOption...)} for an example
     * on how to copy entire directories, or use {@link org.apache.commons.io.FileUtils#copyDirectory(File, File)}.<br><br>
     * This method specifically does NOT copy recursively, because we would potentially copy previously EXCLUDED files,
     * too. We do not want that. At all.
     *
     * @throws SecurityException             In the case of the default provider, and a security manager is installed, the
     *                                       {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read access to the source file,
     *                                       the {@link SecurityManager#checkWrite(String) checkWrite} is invoked to check write access to the target file.
     *                                       If a symbolic link is copied the security manager is invoked to check {@link LinkPermission}{@code ("symbolic")}.
     * @throws UnsupportedOperationException if the array contains a copy option that is not supported.
     * @throws IOException                   if an I/O error occurs
     * @author Griefed
     */
    public void copy() throws SecurityException, UnsupportedOperationException, IOException {
        try {
            if (!SOURCE_FILE.isDirectory()) {

                FileUtils.copyFile(
                        SOURCE_FILE,
                        DESTINATION_FILE,
                        true,
                        StandardCopyOption.REPLACE_EXISTING
                );

            } else {

                Files.copy(
                        SOURCE_PATH,
                        DESTINATION_PATH,
                        REPLACE_EXISTING,
                        COPY_ATTRIBUTES
                );
            }


            LOG.debug("Successfully copied ServerPackFile");
            LOG.debug("    Source: " + SOURCE_PATH);
            LOG.debug("    Destination: " + DESTINATION_PATH);

        } catch (DirectoryNotEmptyException ignored) {
            // If the directory to be copied already exists we're good.
        }
    }

    /**
     * This ServerPackFiles source-file and destination-file as a {@link String}-combination, separated by a <code>;</code>
     *
     * @return {@link String} This ServerPackFiles source-file and destination-file as a {@link String}-combination, separated by a <code>;</code>
     * @author Griefed
     */
    @Override
    public String toString() {
        return SOURCE_PATH + ";" + DESTINATION_PATH;
    }
}
