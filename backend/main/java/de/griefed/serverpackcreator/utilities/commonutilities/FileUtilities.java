package de.griefed.serverpackcreator.utilities.commonutilities;

import mslinks.ShellLink;
import mslinks.ShellLinkException;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Utility-class revolving around various file-interactions.
 * @author Griefed
 */
public class FileUtilities {

    public enum FileType {

        /**
         * A regular file.
         */
        FILE("File"),

        /**
         * A regular directory.
         */
        DIRECTORY("Directory"),

        /**
         * A Windows link.
         */
        LINK("Link"),

        /**
         * A UNIX symlink.
         */
        SYMLINK("SymLink"),

        /**
         * Not a valid file.
         */
        INVALID("Invalid");

        public final String fileType;

        FileType(String fileType) {
            this.fileType = fileType;
        }

    }

    private static final Logger LOG = LogManager.getLogger(FileUtilities.class);

    public FileUtilities() {

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

        try (ZipFile zip = new ZipFile(zipFile)) {

            zip.extractAll(destinationDirectory);

        } catch (IOException ex) {

            LOG.error("Error: There was an error extracting the archive " + zipFile, ex);
        }
    }

    /**
     * Check the given file for its type, whether it is a regular file, a Windows link or a UNIX symlink.
     * @author Griefed
     * @param file {@link File} The file to check
     * @return {@link FileType} The type of the given file. Either {@link FileType#FILE}, {@link FileType#LINK} or {@link FileType#SYMLINK}
     */
    public FileType checkFileType(File file) {
        if (file.getName().endsWith("lnk")) {
            return FileType.LINK;
        }

        if (file.isDirectory()) {
            return FileType.DIRECTORY;
        }

        if (FileUtils.isSymlink(file)) {
            return FileType.SYMLINK;
        }

        if (file.isFile()) {
            return FileType.FILE;
        }

        return FileType.INVALID;
    }

    /**
     * Check if the given file is a UNIX symlink or Windows lnk.
     * @author Griefed
     * @param file {@link File} The file to check.
     * @return <code>true</code> if the given file is a UNIX symlink or Windows lnk.
     */
    public boolean isLink(String file) {
        return isLink(new File(file));
    }

    /**
     * Check if the given file is a UNIX symlink or Windows lnk.
     * @author Griefed
     * @param file {@link File} The file to check.
     * @return <code>true</code> if the given file is a UNIX symlink or Windows lnk.
     */
    public boolean isLink(File file) {
        if (file.getName().endsWith("lnk")) {
            return true;
        }

        return FileUtils.isSymlink(file);
    }

    /**
     * Resolve a given link/symlink to its source.
     * @author Griefed
     * @param link {@link String} The link you want to resolve.
     * @return {@link String} Path to the source of the link. If the specified file is not a link, the path to the passed file is returned.
     * @throws IOException if the link could not be parsed.
     * @throws InvalidFileTypeException if the specified file is neither a file, lnk nor symlink.
     */
    public String resolveLink(String link) throws InvalidFileTypeException, IOException {
        return resolveLink(new File(link));
    }

    /**
     * Resolve a given link/symlink to its source.
     * @author Griefed
     * @param link {@link File} The link you want to resolve.
     * @return {@link String} Path to the source of the link. If the specified file is not a link, the path to the passed file is returned.
     * @throws IOException if the link could not be parsed.
     * @throws InvalidFileTypeException if the specified file is neither a file, lnk nor symlink.
     */
    public String resolveLink(File link) throws IOException, InvalidFileTypeException {

        FileType type = checkFileType(link);

        switch(type) {
            case LINK:
            case SYMLINK:

                try {
                    return resolveLink(link, type);
                } catch (InvalidFileTypeException | InvalidLinkException | ShellLinkException ex) {
                    LOG.error("Somehow an invalid FileType was specified. Please report this on GitHub!",ex);
                }

            case FILE:
            case DIRECTORY:

                return link.toString();

            case INVALID:
            default:
                throw new InvalidFileTypeException("FileType must be either LINK or SYMLINK");
        }
    }

    /**
     * Resolve a given link/symlink to its source.<br>
     * This would not exist without the great answers from this StackOverflow question: <a href="https://stackoverflow.com/questions/309495/windows-shortcut-lnk-parser-in-java">Windows Shortcut lnk parser in Java</a><br>
     * Huge shoutout to <a href="https://stackoverflow.com/users/675721/codebling">Codebling</a>
     * @author Griefed
     * @param file {@link File} The file of which to acquire the source.
     * @param fileType {@link FileType} The link-type. Either {@link FileType#LINK} for Windows, or {@link FileType#SYMLINK} for UNIX systems.
     * @return {@link String} The path to the source of the given link.
     * @throws InvalidFileTypeException if the specified {@link FileType} is invalid.
     * @throws InvalidLinkException if the specified file is not a valid Windows link.
     * @throws ShellLinkException if the specified file could not be parsed as a Windows link.
     * @throws IOException if the link could not be parsed.
     */
    private String resolveLink(File file, FileType fileType) throws InvalidFileTypeException, IOException, InvalidLinkException, ShellLinkException {
        switch (fileType) {
            case SYMLINK:

                return file.getCanonicalPath();

            case LINK:

                return new ShellLink(file).resolveTarget();

            default:
                throw new InvalidFileTypeException("FileType must be either LINK or SYMLINK");
        }
    }
}
