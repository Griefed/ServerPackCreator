package de.griefed.serverpackcreator.utilities.commonutilities;

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
}
