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

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.swing.utilities.JComponentTailer;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;

import java.io.File;
import java.util.Properties;

/**
 * This class creates the tab which display the latest serverpackcreator.log tailer.
 * @author Griefed
 */
public class TabServerPackCreatorLog extends JComponentTailer {

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ApplicationProperties APPLICATIONPROPERTIES;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedApplicationProperties Instance of {@link Properties} required for various different things.
     */
    public TabServerPackCreatorLog(LocalizationManager injectedLocalizationManager, ApplicationProperties injectedApplicationProperties) {
        if (injectedApplicationProperties == null) {
            this.APPLICATIONPROPERTIES = new ApplicationProperties();
        } else {
            this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        }

        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }
    }

    /**
     *
     * @author Griefed
     */
    @Override
    protected void createTailer() {
        class MyTailerListener extends TailerListenerAdapter {
            public void handle(String line) {
                if (line.contains(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.start"))) {
                    textArea.setText("");
                }
                if (!line.contains("DEBUG")) {
                    textArea.append(line.substring(line.lastIndexOf(") - ") + 3) + "\n");
                }
            }
        }
        TailerListener tailerListener = new MyTailerListener();
        Tailer tailer = new Tailer(new File("./logs/serverpackcreator.log"), tailerListener, 100);
        Thread thread = new Thread(tailer);
        thread.setDaemon(true);
        thread.start();
    }
}