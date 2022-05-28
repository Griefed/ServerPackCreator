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
package de.griefed.serverpackcreator.swing.utilities;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Based on {@link IconTextField}<br>
 * <a href="https://itqna.net/questions/10833/decorating-jtextfield-icon">it_qna</a>
 */
public class IconTextArea extends JTextArea {
    private static final int ICON_SPACING = 4;
    private Border mBorder;
    private Icon mIcon;

    public IconTextArea(String s) {
        super(s);
    }

    public IconTextArea() {
        super();
    }

    @Override
    public void setBorder(Border border) {
        mBorder = border;

        if (mIcon == null) {

            super.setBorder(border);

        } else {

            //Border margin = BorderFactory.createEmptyBorder(0, mIcon.getIconWidth() + ICON_SPACING, 0, 0);
            Border margin = BorderFactory.createEmptyBorder(0, mIcon.getIconWidth() + ICON_SPACING, 0, 0);
            Border compoud = BorderFactory.createCompoundBorder(border, margin);
            super.setBorder(compoud);

        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (mIcon != null) {
            Insets iconInsets = mBorder.getBorderInsets(this);
            mIcon.paintIcon(this, graphics, iconInsets.left, this.getHeight() - mIcon.getIconHeight());
        }
    }

    public void setIcon(Icon icon) {
        mIcon = icon;
        resetBorder();
    }

    private void resetBorder() {
        setBorder(mBorder);
    }

    public void addDocumentListener(DocumentListener listener) {
        getDocument().addDocumentListener(listener);
    }
}
