package de.griefed.serverpackcreator.utilities;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * Utility-class revolving around interactions with web-resources.
 * @author Griefed
 */
public class WebUtilities {

    private static final Logger LOG = LogManager.getLogger(WebUtilities.class);

    public WebUtilities() {

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
        } catch (IOException ignored) {

        }

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
            } catch (Exception ignored) {

            }

            try {
                fileOutputStream.close();
            } catch (Exception ignored) {

            }

            try {
                //noinspection ConstantConditions
                readableByteChannel.close();
            } catch (Exception ignored) {

            }

            try {
                //noinspection ConstantConditions
                fileChannel.close();
            } catch (Exception ignored) {

            }

        }

        return new File(fileDestination).exists();
    }

}
