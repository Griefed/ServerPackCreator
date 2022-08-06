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
package de.griefed.serverpackcreator.utilities.common;

import de.griefed.serverpackcreator.ApplicationProperties;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility-class revolving around interactions with web-resources.
 *
 * @author Griefed
 */
public class WebUtilities {

  private static final Logger LOG = LogManager.getLogger(WebUtilities.class);

  private final ApplicationProperties APPLICATIONPROPERTIES;

  public WebUtilities(ApplicationProperties injectedApplicationProperties) {
    this.APPLICATIONPROPERTIES = injectedApplicationProperties;
  }

  /**
   * Download the file from the specified URL to the specified destination, replacing the file if it
   * already exists. The destination should end in a valid filename. Any directories up to the
   * specified file will be created.
   *
   * @param destinationFile File. The file to store the web-resource in. Examples:<br>
   *                        /tmp/some_folder/foo.bar<br> C:/temp/some_folder/bar.foo
   * @param downloadURL     URL. The URL to the file you want to download.
   * @return Boolean. Returns true if the file could be found on the hosts filesystem.
   * @author Griefed
   */
  public boolean downloadAndReplaceFile(File destinationFile, URL downloadURL) {
    return downloadAndReplaceFile(
        destinationFile.getAbsoluteFile().toString().replace("\\", "/"), downloadURL);
  }

  /**
   * Download the file from the specified URL to the specified destination, replacing the file if it
   * already exists. The destination should end in a valid filename. Any directories up to the
   * specified file will be created.
   *
   * @param fileDestination String. The file to store the web-resource in. Examples:<br>
   *                        /tmp/some_folder/foo.bar<br> C:/temp/some_folder/bar.foo
   * @param downloadURL     URL. The URL to the file you want to download.
   * @return Boolean. Returns true if the file could be found on the hosts filesystem.
   * @author Griefed
   */
  public boolean downloadAndReplaceFile(String fileDestination, URL downloadURL) {
    FileUtils.deleteQuietly(new File(fileDestination));
    return downloadFile(fileDestination, downloadURL);
  }

  /**
   * Download the file from the specified URL to the specified destination. The destination should
   * end in a valid filename. Any directories up to the specified file will be created.
   *
   * @param destinationFile File. The file to store the web-resource in. Examples:<br>
   *                        /tmp/some_folder/foo.bar<br> C:/temp/some_folder/bar.foo
   * @param downloadURL     URL. The URL to the file you want to download.
   * @return Boolean. Returns true if the file could be found on the hosts filesystem.
   * @author Griefed
   */
  public boolean downloadFile(File destinationFile, URL downloadURL) {
    return downloadFile(
        destinationFile.getAbsoluteFile().toString().replace("\\", "/"), downloadURL);
  }

  /**
   * Download the file from the specified URL to the specified destination. The destination should
   * end in a valid filename. Any directories up to the specified file will be created.
   *
   * @param fileDestination String. The destination where the file should be stored. Must include
   *                        the filename as well. Examples:<br> /tmp/some_folder/foo.bar<br>
   *                        C:/temp/some_folder/bar.foo
   * @param downloadURL     URL. The URL to the file you want to download.
   * @return Boolean. Returns true if the file could be found on the hosts filesystem.
   * @author Griefed
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

      fileOutputStream = new FileOutputStream(fileDestination.replace("\\", "/"));

      fileChannel = fileOutputStream.getChannel();

      fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

    } catch (IOException ex) {
      LOG.error("An error occurred downloading " + fileDestination.replace("\\", "/") + ".", ex);
      FileUtils.deleteQuietly(new File(fileDestination.replace("\\", "/")));
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

  /**
   * Open the given url in a browser.
   *
   * @param url {@link URL} the URI to the website you want to open.
   * @author Griefed
   */
  public void openLinkInBrowser(URL url) {
    try {
      openLinkInBrowser(url.toURI());
    } catch (URISyntaxException ex) {
      LOG.error("Error opening browser with link " + url + ".", ex);
    }
  }

  /**
   * Open the given uri in a browser.
   *
   * @param uri {@link URI} the URI to the website you want to open.
   * @author Griefed
   */
  public void openLinkInBrowser(URI uri) {
    try {
      if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(uri);
      }
    } catch (IOException ex) {
      LOG.error("Error opening browser with link " + uri + ".", ex);
    }
  }

  /**
   * Checks the filesize of the given file whether it is smaller or bigger than 10 MB.
   *
   * @param fileToCheck The file or directory to check.
   * @return Boolean. True if the file is smaller, false if the file is bigger than 10 MB.
   * @author Griefed
   */
  public boolean hasteBinPreChecks(File fileToCheck) {
    long fileSize = FileUtils.sizeOf(fileToCheck);

    try {
      if (fileSize < 10000000
          && FileUtils.readFileToString(fileToCheck, StandardCharsets.UTF_8).length() < 400000) {
        LOG.debug("Smaller. " + fileSize + " byte.");
        return true;
      } else {
        LOG.debug("Bigger. " + fileSize + " byte.");
        return false;
      }
    } catch (IOException ex) {
      LOG.error("Couldn't read file: " + fileToCheck, ex);
    }

    return false;
  }

  /**
   * Create a HasteBin post from a given text file. The text file provided is read into a string and
   * then passed onto <a href="https://haste.zneix.eu">Haste zneix</a> which creates a HasteBin post
   * out of the passed String and returns the URL to the newly created post.<br> Created with the
   * help of <a href="https://github.com/kaimu-kun/hastebin.java">kaimu-kun's hastebin.java (MIT
   * License)</a> and edited to use HasteBin fork <a
   * href="https://github.com/zneix/haste-server">zneix/haste-server</a>. My fork of kaimu-kun's
   * hastebin.java is available at <a
   * href="https://github.com/Griefed/hastebin.java">Griefed/hastebin.java</a>.
   *
   * @param textFile The file which will be read into a String of which then to create a HasteBin
   *                 post of.
   * @return String. Returns a String containing the URL to the newly created HasteBin post.
   * @author <a href="https://github.com/kaimu-kun">kaimu-kun/hastebin.java</a>
   * @author Griefed
   */
  public String createHasteBinFromFile(File textFile) {
    String text = null;
    String requestURL =
        APPLICATIONPROPERTIES.getProperty(
            "de.griefed.serverpackcreator.configuration.hastebinserver",
            "https://haste.zneix.eu/documents");

    String response = null;

    int postDataLength;

    URL url = null;

    HttpsURLConnection conn = null;

    byte[] postData;

    try {
      url = new URL(requestURL);
    } catch (IOException ex) {
      LOG.error("Error during acquisition of request URL.", ex);
    }

    try {
      text = FileUtils.readFileToString(textFile, "UTF-8");
    } catch (IOException ex) {
      LOG.error("Error reading text from file.", ex);
    }

    postData = Objects.requireNonNull(text).getBytes(StandardCharsets.UTF_8);
    postDataLength = postData.length;

    try {
      conn = (HttpsURLConnection) Objects.requireNonNull(url).openConnection();
    } catch (IOException ex) {
      LOG.error("Error during opening of connection to URL.", ex);
    }

    Objects.requireNonNull(conn).setDoOutput(true);
    conn.setInstanceFollowRedirects(false);

    try {
      conn.setRequestMethod("POST");
    } catch (ProtocolException ex) {
      LOG.error("Error during request of POST method.", ex);
    }

    conn.setRequestProperty("User-Agent", "HasteBin-Creator for ServerPackCreator");
    conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
    conn.setUseCaches(false);

    try (DataOutputStream dataOutputStream = new DataOutputStream(conn.getOutputStream())) {
      // dataOutputStream = new DataOutputStream(conn.getOutputStream());
      dataOutputStream.write(postData);

      try (BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(conn.getInputStream()))) {

        response = bufferedReader.readLine();

      } catch (IOException ex) {
        LOG.error("Error encountered when acquiring HasteBin.", ex);
      }

    } catch (IOException ex) {
      LOG.error("Error encountered when acquiring HasteBin.", ex);
    }

    if (Objects.requireNonNull(response).contains("\"key\"")) {
      response =
          requestURL.replace("/documents", "/")
              + response.substring(response.indexOf(":") + 2, response.length() - 2);
    }

    if (response.contains(requestURL.replace("/documents", ""))) {
      return response;
    } else {
      return "Error encountered when acquiring response from URL.";
    }
  }

  /**
   * Get the reponse of a call to a URL as a string.
   *
   * @param url {@link URL} The URL you want to get the response from
   * @return {@link String} The response.
   * @throws IOException if the URL could not be called or a communication error occurred.
   */
  public String getResponseAsString(URL url) throws IOException {

    BufferedReader in =
        new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));

    StringBuilder response = new StringBuilder();
    String currentLine;

    while ((currentLine = in.readLine()) != null) {
      response.append(currentLine);
    }

    in.close();

    return response.toString();
  }

  /**
   * Get the response-code of a call to a URL as an integer.
   *
   * @param url {@link URL} The URL you want to get the response from
   * @return {@link String} The response.
   * @throws IOException if the URL could not be called or a communication error occurred.
   */
  public int getResponseCode(URL url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    return connection.getResponseCode();
  }
}
