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
package de.griefed.serverpackcreator.versionmeta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class ManifestParser {

  /**
   * Reads the file into a {@link Document} and {@link Document#normalize()} it.
   *
   * @param manifest The xml-file to parse into a Document.
   * @return The parsed and normalized document.
   * @throws ParserConfigurationException indicates a serious configuration error.
   * @throws IOException                  if any IO errors occur.
   * @throws SAXException                 if any parse errors occur.
   * @author Griefed
   */
  public Document getXml(File manifest)
      throws ParserConfigurationException, IOException, SAXException {

    return getXml(FileUtils.readFileToString(manifest, StandardCharsets.UTF_8));
  }

  /**
   * Reads the string into a {@link Document} and {@link Document#normalize()} it.
   *
   * @param string The xml-string to parse into a Document.
   * @return The parsed and normalized document.
   * @throws ParserConfigurationException indicates a serious configuration error.
   * @throws IOException                  if any IO errors occur.
   * @throws SAXException                 if any parse errors occur.
   * @author Griefed
   */
  public Document getXml(String string)
      throws ParserConfigurationException, IOException, SAXException {

    Document xml = getDocumentBuilder().parse(new InputSource(new StringReader(string)));
    xml.normalize();
    return xml;
  }

  private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder();
  }

  /**
   * Reads the inputstream into a {@link Document} and {@link Document#normalize()} it.
   *
   * @param manifest The xml-inputstream to parse into a Document.
   * @return The parsed and normalized document.
   * @throws ParserConfigurationException indicates a serious configuration error.
   * @throws IOException                  if any IO errors occur.
   * @throws SAXException                 if any parse errors occur.
   * @author Griefed
   */
  public Document getXml(InputStream manifest)
      throws ParserConfigurationException, IOException, SAXException {

    return getXml(StreamUtils.copyToString(manifest,
                                           StandardCharsets.UTF_8));
  }

  /**
   * Acquire a {@link JsonNode} from the given json inputstream.
   *
   * @param inputStream The inputstream to read.
   * @param mapper      ObjectMapper for reading and parsing JSON.
   * @return JSON data from the specified file.
   * @throws IOException when the file could not be parsed/read into a {@link JsonNode}.
   * @author Griefed
   */
  protected JsonNode getJson(InputStream inputStream,
                             ObjectMapper mapper) throws IOException {
    return getJson(StreamUtils.copyToString(inputStream,
                                            StandardCharsets.UTF_8), mapper);
  }

  /**
   * Acquire a {@link JsonNode} from the given json string.
   *
   * @param string The string to read.
   * @param mapper ObjectMapper for reading and parsing JSON.
   * @return JSON data from the specified file.
   * @throws IOException when the file could not be parsed/read into a {@link JsonNode}.
   * @author Griefed
   */
  protected JsonNode getJson(String string,
                             ObjectMapper mapper) throws IOException {
    return mapper.readTree(string);
  }

  /**
   * Acquire a {@link JsonNode} from the given json file.
   *
   * @param file   The file to read.
   * @param mapper ObjectMapper for reading and parsing JSON.
   * @return JSON data from the specified file.
   * @throws IOException when the file could not be parsed/read into a {@link JsonNode}.
   * @author Griefed
   */
  protected JsonNode getJson(File file,
                             ObjectMapper mapper) throws IOException {
    return getJson(FileUtils.readFileToString(file, StandardCharsets.UTF_8), mapper);
  }

  /**
   * Acquire a {@link JsonNode} from the given URL.
   *
   * @param url    URL to the data which contains your JSON.
   * @param mapper ObjectMapper for reading and parsing JSON.
   * @return JSON data from the specified file.
   * @throws IOException when the file could not be parsed/read into a {@link JsonNode}.
   * @author Griefed
   */
  protected JsonNode getJson(URL url,
                             ObjectMapper mapper) throws IOException {
    return mapper.readTree(url);
  }
}
