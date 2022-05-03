package de.griefed.serverpackcreator.swing.utilities;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * <a href="https://itqna.net/questions/10833/decorating-jtextfield-icon">it_qna</a>
 */
public class IconTextField extends JTextField {
    private static final int ICON_SPACING = 4;
    private Border mBorder;
    private Icon mIcon;

    public IconTextField(String s) {
        super(s);
    }

    public IconTextField() {
        super();
    }

    @Override
    public void setBorder(Border border) {
        mBorder = border;

        if (mIcon == null) {

            super.setBorder(border);

        } else {

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
            mIcon.paintIcon(this, graphics, iconInsets.left, iconInsets.top);

        }
    }

    public void setIcon(Icon icon) {
        mIcon = icon;
        resetBorder();
    }

    private void resetBorder()
    {
        setBorder(mBorder);
    }

    public void addDocumentListener(DocumentListener listener) {
        getDocument().addDocumentListener(listener);
    }
}
