package de.griefed.serverpackcreator.swing.utilities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicProgressBarUI;
import org.jetbrains.annotations.NotNull;

public class CustomProgressBarUI extends BasicProgressBarUI {

  private final BufferedImage original;
  private Rectangle r = new Rectangle(0, 0, 20, 20);

  public CustomProgressBarUI(@NotNull BufferedImage image) {
    super();
    this.original = image;
  }

  @Override
  protected void paintIndeterminate(Graphics g, JComponent c) {
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    r = getBox(r);

    g2d.drawImage(
        original.getScaledInstance((int) r.getWidth(), (int) r.getHeight(), Image.SCALE_SMOOTH),
        r.x,
        r.y,
        r.width,
        r.height,
        new Color(0, 0, 0, 0),
        null);
  }
}
