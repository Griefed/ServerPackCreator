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

import de.griefed.serverpackcreator.ServerPackCreator.CommandlineParser;
import de.griefed.serverpackcreator.swing.utilities.BackgroundPanel;
import de.griefed.serverpackcreator.utilities.ReticulatingSplines;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicProgressBarUI;

/** The ServerPackCreator splashscreen shown when {@link CommandlineParser.Mode#GUI} is used. */
public class ServerPackCreatorSplash {

  private final ReticulatingSplines RETICULATING_SPLINES = new ReticulatingSplines();
  private final JWindow SPLASH_WINDOW;
  private final JLabel PROGRESS_TEXT = new JLabel(RETICULATING_SPLINES.reticulate());
  private final JProgressBar PROGRESS_BAR = new JProgressBar();

  /**
   * Create and show our splashscreen.
   *
   * @param version {@link String} The version of ServerPackCreator being run.
   * @author Griefed
   */
  public ServerPackCreatorSplash(String version) {

    // Random random = new Random();
    @SuppressWarnings("ConstantConditions")
    ImageIcon splashScreenBackgroundImage =
        new ImageIcon(
            ServerPackCreatorSplash.class.getResource(
                "/de/griefed/resources/gui/splashscreen" + new Random().nextInt(3) + ".png"));

    BufferedImage bufferedImage =
        new BufferedImage(
            splashScreenBackgroundImage.getIconWidth(),
            splashScreenBackgroundImage.getIconHeight(),
            BufferedImage.TYPE_INT_ARGB);
    Graphics graphics = bufferedImage.createGraphics();
    splashScreenBackgroundImage.paintIcon(null, graphics, 0, 0);
    graphics.dispose();

    Color c0FFEE = new Color(192, 255, 238);
    Color primary = new Color(50, 83, 88);

    // Construct and prepare JFrame with background image
    // this.J_FRAME = new JFrame();
    this.SPLASH_WINDOW = new JWindow();
    this.SPLASH_WINDOW.setContentPane(
        new BackgroundPanel(bufferedImage, BackgroundPanel.ACTUAL, 0.0f, 0.0f));
    this.SPLASH_WINDOW.getContentPane().setLayout(null);
    // this.J_FRAME.setUndecorated(true);
    this.SPLASH_WINDOW.setSize(
        splashScreenBackgroundImage.getIconWidth(), splashScreenBackgroundImage.getIconHeight());
    this.SPLASH_WINDOW.setLocationRelativeTo(null);
    this.SPLASH_WINDOW.getContentPane().setBackground(c0FFEE);

    // Construct and prepare mem progress text
    this.PROGRESS_TEXT.setFont(new Font("arial", Font.BOLD, 20));
    this.PROGRESS_TEXT.setHorizontalAlignment(SwingConstants.CENTER);
    this.PROGRESS_TEXT.setBounds(
        0,
        Math.floorDiv(splashScreenBackgroundImage.getIconHeight(), 2) + 20,
        splashScreenBackgroundImage.getIconWidth(),
        40);
    this.PROGRESS_TEXT.setForeground(c0FFEE);
    this.SPLASH_WINDOW.add(PROGRESS_TEXT);

    // Construct and add progress bar
    float offsetInPercent = 20F;
    this.PROGRESS_BAR.setBounds(
        Math.round(splashScreenBackgroundImage.getIconWidth() / 100F * offsetInPercent),
        Math.floorDiv(splashScreenBackgroundImage.getIconHeight(), 2),
        Math.round(
            splashScreenBackgroundImage.getIconWidth() / 100F * (100F - offsetInPercent * 2)),
        20);
    this.PROGRESS_BAR.setAlignmentY(0.0f);
    this.PROGRESS_BAR.setBorderPainted(true);
    this.PROGRESS_BAR.setStringPainted(true);
    this.PROGRESS_BAR.setBackground(Color.WHITE);
    this.PROGRESS_BAR.setUI(
        new BasicProgressBarUI() {
          // Text-colour when bar is NOT covering the loading-text
          protected Color getSelectionBackground() {
            return primary;
          }

          // Text-colour when the bar IS covering the loading-text
          protected Color getSelectionForeground() {
            return c0FFEE;
          }
        });
    this.PROGRESS_BAR.setForeground(primary);
    this.PROGRESS_BAR.setValue(0);
    this.SPLASH_WINDOW.add(PROGRESS_BAR);

    // Construct and add version label
    JLabel versionLabel = new JLabel(version);
    versionLabel.setFont(new Font("arial", Font.BOLD, 15));
    versionLabel.setBounds(
        15,
        splashScreenBackgroundImage.getIconHeight() - 40,
        splashScreenBackgroundImage.getIconWidth(),
        40);
    versionLabel.setForeground(c0FFEE);
    this.SPLASH_WINDOW.add(versionLabel);

    // Construct and add sum luv
    JLabel someLuv = new JLabel("By Griefed");
    someLuv.setFont(new Font("arial", Font.BOLD, 15));
    someLuv.setBounds(
        splashScreenBackgroundImage.getIconWidth() - 100,
        splashScreenBackgroundImage.getIconHeight() - 40,
        splashScreenBackgroundImage.getIconWidth(),
        40);
    someLuv.setForeground(c0FFEE);
    this.SPLASH_WINDOW.add(someLuv);

    // Construct and add close button in case SPC was not meant to be started, or hangs
    JButton EXIT = new JButton();
    int buttonSize = 16;
    try {
      EXIT.setIcon(
          new ImageIcon(
              ImageIO.read(
                      Objects.requireNonNull(
                          ServerPackCreatorGui.class.getResource(
                              "/de/griefed/resources/gui/error.png")))
                  .getScaledInstance(buttonSize, buttonSize, Image.SCALE_SMOOTH)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    EXIT.setBounds(
        splashScreenBackgroundImage.getIconWidth() - buttonSize, 0, buttonSize, buttonSize);
    EXIT.addActionListener(this::exit);
    EXIT.setContentAreaFilled(false);
    EXIT.setOpaque(false);
    EXIT.setBorder(null);
    EXIT.setBorderPainted(false);
    this.SPLASH_WINDOW.add(EXIT);

    this.SPLASH_WINDOW.setIconImage(
        Toolkit.getDefaultToolkit()
            .getImage(
                Objects.requireNonNull(
                    ServerPackCreatorGui.class.getResource("/de/griefed/resources/gui/app.png"))));

    this.SPLASH_WINDOW.setVisible(true);
  }

  public void update(int progress) {
    this.PROGRESS_TEXT.setText(RETICULATING_SPLINES.reticulate());
    this.PROGRESS_TEXT.setHorizontalAlignment(SwingConstants.CENTER);
    this.PROGRESS_BAR.setValue(progress);
  }

  public void close() {
    this.SPLASH_WINDOW.dispose();
  }

  public void exit(ActionEvent actionEvent) {
    close();
    System.exit(0);
  }
}
