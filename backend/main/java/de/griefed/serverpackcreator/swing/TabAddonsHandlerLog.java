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

import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.swing.utilities.JComponentTailer;
import java.io.File;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;

/**
 * This class creates the tab which display the latest addons.log tailer.
 *
 * @author Griefed
 */
public class TabAddonsHandlerLog extends JComponentTailer {

  /**
   * <strong>Constructor</strong>
   *
   * <p>Used for Dependency Injection.
   *
   * <p>Receives an instance of {@link I18n} or creates one if the received one is null. Required
   * for use of localization.
   *
   * @param tooltip       The tooltip text for this tailer.
   * @param logsDirectory The directory in which the logs reside in.
   * @author Griefed
   */
  public TabAddonsHandlerLog(String tooltip,
                             File logsDirectory) {
    super(tooltip);
    createTailer(logsDirectory);
  }

  /**
   * @author Griefed
   */
  @Override
  protected void createTailer(File logsDirectory) {
    class MyTailerListener extends TailerListenerAdapter {

      public void handle(String line) {
        if (!line.contains("DEBUG")) {
          textArea.append(line + "\n");
        }
      }
    }
    TailerListener tailerListener = new MyTailerListener();
    Tailer tailer = new Tailer(new File(logsDirectory, "addons.log"), tailerListener, 100);
    Thread thread = new Thread(tailer);
    thread.setDaemon(true);
    thread.start();
  }
}
