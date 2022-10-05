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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Commonly used XML utilities.
 *
 * @author Griefed
 */
@Component
public class XmlUtilities {

  private final DocumentBuilder DOCUMENTBUILDER;

  public XmlUtilities(DocumentBuilder documentBuilder) {
    DOCUMENTBUILDER = documentBuilder;
  }

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

    Document xml = DOCUMENTBUILDER.parse(new InputSource(new StringReader(string)));
    xml.normalize();
    return xml;
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
}
