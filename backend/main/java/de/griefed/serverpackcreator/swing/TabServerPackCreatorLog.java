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
package de.griefed.serverpackcreator.swing;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.swing.utilities.JComponentTailer;
import de.griefed.serverpackcreator.utilities.misc.Generated;
import java.io.File;
import java.util.Properties;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;

/**
 * This class creates the tab which display the latest serverpackcreator.log tailer.
 *
 * @author Griefed
 */
@Generated
public class TabServerPackCreatorLog extends JComponentTailer {

  private final I18n I18N;

  /**
   * <strong>Constructor</strong>
   *
   * <p>Used for Dependency Injection.
   *
   * <p>Receives an instance of {@link I18n} or creates one if the received one is null. Required
   * for use of localization.
   *
   * @param injectedI18n Instance of {@link I18n} required for localized log messages.
   * @param injectedApplicationProperties Instance of {@link Properties} required for various
   *     different things.
   * @param tooltip {@link String} The tooltip text for this tailer.
   * @author Griefed
   */
  public TabServerPackCreatorLog(
      I18n injectedI18n, ApplicationProperties injectedApplicationProperties, String tooltip) {
    super(tooltip);
    ApplicationProperties APPLICATIONPROPERTIES;
    if (injectedApplicationProperties == null) {
      APPLICATIONPROPERTIES = new ApplicationProperties();
    } else {
      APPLICATIONPROPERTIES = injectedApplicationProperties;
    }

    if (injectedI18n == null) {
      this.I18N = new I18n(APPLICATIONPROPERTIES);
    } else {
      this.I18N = injectedI18n;
    }

    createTailer();
  }

  /**
   * @author Griefed
   */
  @Override
  protected void createTailer() {
    class MyTailerListener extends TailerListenerAdapter {
      public void handle(String line) {
        if (line.contains(
            I18N.getMessage("createserverpack.log.info.buttoncreateserverpack.start"))) {
          textArea.setText("");
        }
        if (!line.contains("DEBUG")) {
          textArea.append(line + "\n");
        }
      }
    }
    TailerListener tailerListener = new MyTailerListener();
    Tailer tailer = new Tailer(new File("./logs/serverpackcreator.log"), tailerListener, 200);
    Thread thread = new Thread(tailer);
    thread.setDaemon(true);
    thread.start();
  }
}
