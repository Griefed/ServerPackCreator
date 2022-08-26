package de.griefed.serverpackcreator.versionmeta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public abstract class Manifests {

  /**
   * Reads the file into a {@link Document} and {@link Document#normalize()} it.
   *
   * @param manifest The xml-file to parse into a Document.
   * @return The parsed and normalized document.
   * @throws ParserConfigurationException indicates a serious configuration error.
   * @throws IOException if any IO errors occur.
   * @throws SAXException if any parse errors occur.
   * @author Griefed
   */
  public Document getXml(File manifest)
      throws ParserConfigurationException, IOException, SAXException {

    Document xml = getDocumentBuilder().parse(manifest);
    xml.normalize();
    return xml;
  }

  /**
   * Reads the inputstream into a {@link Document} and {@link Document#normalize()} it.
   *
   * @param manifest The xml-inputstream to parse into a Document.
   * @return The parsed and normalized document.
   * @throws ParserConfigurationException indicates a serious configuration error.
   * @throws IOException if any IO errors occur.
   * @throws SAXException if any parse errors occur.
   * @author Griefed
   */
  public Document getXml(InputStream manifest)
      throws ParserConfigurationException, IOException, SAXException {

    Document xml = getDocumentBuilder().parse(manifest);
    xml.normalize();
    return xml;
  }

  private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder();
  }

  /**
   * Acquire a {@link JsonNode} from the given json inputstream.
   *
   * @param inputStream The inputstream to read.
   * @param mapper ObjectMapper for reading and parsing JSON.
   * @return JSON data from the specified file.
   * @throws IOException when the file could not be parsed/read into a {@link JsonNode}.
   * @author Griefed
   */
  protected JsonNode getJson(InputStream inputStream, ObjectMapper mapper) throws IOException {
    return mapper.readTree(inputStream);
  }

  /**
   * Acquire a {@link JsonNode} from the given json file.
   *
   * @param file The file to read.
   * @param mapper ObjectMapper for reading and parsing JSON.
   * @return JSON data from the specified file.
   * @throws IOException when the file could not be parsed/read into a {@link JsonNode}.
   * @author Griefed
   */
  protected JsonNode getJson(File file, ObjectMapper mapper) throws IOException {
    return mapper.readTree(file);
  }

  /**
   * Acquire a {@link JsonNode} from the given URL.
   *
   * @param url URL to the data which contains your JSON.
   * @param mapper ObjectMapper for reading and parsing JSON.
   * @return JSON data from the specified file.
   * @throws IOException when the file could not be parsed/read into a {@link JsonNode}.
   * @author Griefed
   */
  protected JsonNode getJson(URL url, ObjectMapper mapper) throws IOException {
    return mapper.readTree(url);
  }
}
