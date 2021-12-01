/* Copyright (C) 2021  Griefed
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
package de.griefed.serverpackcreator.swing.components.buttons;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.swing.components.filechoosers.ClientsideModsFileChooser;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ClientsideModsButton extends JButton {
/*
    private final LocalizationManager LOCALIZATIONMANAGER;
    private final JComponent JCOMPONENT;
    private final JFileChooser JFILECHOOSER;
    private final JTextField modpackDirectoryTextField;

    public ClientsideModsButton(JComponent jComponent, JTextField textField, LocalizationManager injectedLocalizationManager, ImageIcon icon, Dimension dimension) {
        this.LOCALIZATIONMANAGER = injectedLocalizationManager;

        setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonclientmods"));
        setContentAreaFilled(false);
        setIcon(icon);
        setMinimumSize(dimension);
        setPreferredSize(dimension);
        setMaximumSize(dimension);
        addActionListener(this::selectClientMods);

        JCOMPONENT = jComponent;

        modpackDirectoryTextField = textField;

        JFILECHOOSER = new ClientsideModsFileChooser(LOCALIZATIONMANAGER);
    }

    *//**
     * Upon button-press, open a file-selector for clientside-only mods. If the modpack-directory is specified, the file-selector
     * will open in the mods-directory in the modpack-directory.
     * @author Griefed
     * @param event The event which triggers this method.
     *//*
    private void selectClientMods(ActionEvent event) {

        if (modpackDirectoryTextField.getText().length() > 0 &&
                new File(modpackDirectoryTextField.getText()).isDirectory() &&
                new File(String.format("%s/mods", modpackDirectoryTextField.getText())).isDirectory()) {

            JFILECHOOSER.setCurrentDirectory(new File(String.format("%s/mods", modpackDirectoryTextField.getText())));
        } else {
            JFILECHOOSER.setCurrentDirectory(DIRECTORY_CHOOSER);
        }

        JFILECHOOSER.setDialogTitle(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonclientmods.title"));
        JFILECHOOSER.setFileSelectionMode(JFileChooser.FILES_ONLY);

        JFILECHOOSER.setFileFilter(new FileNameExtensionFilter(
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonclientmods.filter"),
                "jar"
        ));

        JFILECHOOSER.setAcceptAllFileFilterUsed(false);
        JFILECHOOSER.setMultiSelectionEnabled(true);
        JFILECHOOSER.setPreferredSize(CHOOSERDIMENSION);

        if (JFILECHOOSER.showOpenDialog(JCOMPONENT) == JFileChooser.APPROVE_OPTION) {
            File[] clientMods = JFILECHOOSER.getSelectedFiles();
            ArrayList<String> clientModsFilenames = new ArrayList<>();

            for (File mod : clientMods) {
                clientModsFilenames.add(mod.getName());
            }

            TEXTFIELD_CLIENTSIDEMODS.setText(
                    CONFIGURATIONHANDLER.buildString(
                            Arrays.toString(
                                    clientModsFilenames.toArray(new String[0])))
            );

            LOG.debug("Selected mods: " + clientModsFilenames);
        }
    }*/

}
