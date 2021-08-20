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
package de.griefed.serverpackcreator.swing;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

/**
 * This class creates the tab which displays the About-tab, with the about text, the list of contributors, buttons for
 * opening PasteBin in your browser, opening ServerPackCreator's issue page on GitHub and for opening the invite link
 * to Griefed's discord server in your browser.
 * @author Griefed
 */
public class TabAbout extends Component {
    private static final Logger LOG = LogManager.getLogger(TabAbout.class);

    private final LocalizationManager LOCALIZATIONMANAGER;
    private Properties serverpackcreatorproperties;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     */
    public TabAbout(LocalizationManager injectedLocalizationManager) {
        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager();
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        try (InputStream inputStream = new FileInputStream("serverpackcreator.properties")) {
            this.serverpackcreatorproperties = new Properties();
            this.serverpackcreatorproperties.load(inputStream);
        } catch (IOException ex) {
            LOG.error("Couldn't read properties file.", ex);
        }

    }

    private final Dimension DIMENSION_MISC_BUTTON = new Dimension(50,50);

    private final ImageIcon ICON_ISSUE = new ImageIcon(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/issue.png")));
    private final ImageIcon ICON_HASTEBIN = new ImageIcon(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/hastebin.png")));
    private final ImageIcon ICON_PROSPER = new ImageIcon(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/prosper.png")));

    private final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();

    private final File FILE_CONFIG = new File("serverpackcreator.conf");
    private final File LOG_SERVERPACKCREATOR = new File("logs/serverpackcreator.log");

    private JComponent aboutPanel;

    private JTextPane textPane;

    private JTextArea textArea;

    private JButton buttonCreatePasteBin;
    private JButton buttonOpenIssue;
    private JButton buttonDiscord;

    private String configURL;
    private String serverpackcreatorlogURL;
    private String textAreaContent;

    private StringSelection stringSelection;

    private String[] options;

    private int userResponse;

    /**
     * Create the tab for the About-page of ServerpackCreator. This tab displays the about text, the list of contributors
     * to ServerPackCreator and offers three buttons to the user. Left to right: Open PasteBin in the user's browser to
     * create pastes of the log files or configuration files or whatever they wish. Open ServerPackCreator's issues
     * page on GitHub in case the user wants to report an issue. Open the invite link to Griefed's discord server in the
     * user's browser.
     * @return JComponent. Returns a JPanel containing a JTextPane with the about text in a styled document, as well as
     * the three aforementioned buttons.
     * @author Griefed
     */
    JComponent aboutTab() {
        aboutPanel = new JPanel(false);
        aboutPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;

        //About Panel
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setMinimumSize(new Dimension(getMaximumSize().width,520));
        textPane.setPreferredSize(new Dimension(getMaximumSize().width,520));
        textPane.setMaximumSize(new Dimension(getMaximumSize().width,520));

        SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        StyleConstants.setBold(attributeSet, true);
        StyleConstants.setFontSize(attributeSet, 14);
        textPane.setCharacterAttributes(attributeSet, true);

        StyledDocument document = textPane.getStyledDocument();
        StyleConstants.setAlignment(attributeSet, StyleConstants.ALIGN_CENTER);
        document.setParagraphAttributes(0, document.getLength(), attributeSet, false);

        try {
            document.insertString(
                    document.getLength(),
                    LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.text"),
                    attributeSet
            ); } catch (BadLocationException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("about.log.error.document"), ex);
        }
        aboutPanel.add(textPane, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        aboutPanel.add(new JSeparator(JSeparator.HORIZONTAL), constraints);

        //Buttons
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(15,10,10,10);
        constraints.weightx = 0.1;
        constraints.weighty = 0;
        constraints.ipady = 40;
        constraints.gridwidth = 1;

        //Button to upload log file to hastebin
        buttonCreatePasteBin = new JButton();
        buttonCreatePasteBin.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin"));
        buttonCreatePasteBin.setIcon(ICON_HASTEBIN);
        buttonCreatePasteBin.setPreferredSize(DIMENSION_MISC_BUTTON);
        buttonCreatePasteBin.addActionListener(this::createHasteBin);
        constraints.gridx = 0;
        constraints.gridy = 2;
        aboutPanel.add(buttonCreatePasteBin, constraints);

        //Button to open a new issue on GitHub
        buttonOpenIssue = new JButton();
        buttonOpenIssue.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.issue"));
        buttonOpenIssue.setIcon(ICON_ISSUE);
        buttonOpenIssue.setPreferredSize(DIMENSION_MISC_BUTTON);
        buttonOpenIssue.addActionListener(this::openIssueLink);
        constraints.gridx = 1;
        constraints.gridy = 2;
        aboutPanel.add(buttonOpenIssue, constraints);

        //Button to open the invite link to the discord server
        buttonDiscord = new JButton();
        buttonDiscord.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.discord"));
        buttonDiscord.setIcon(ICON_PROSPER);
        buttonDiscord.setPreferredSize(DIMENSION_MISC_BUTTON);
        buttonDiscord.addActionListener(this::openDiscordLink);
        constraints.gridx = 2;
        constraints.gridy = 2;
        aboutPanel.add(buttonDiscord, constraints);

        return aboutPanel;
    }

    /**
     * Upon button-press, create HasteBins from the currently used config-file and latest serverpackcreator.log.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void createHasteBin(ActionEvent event) {

        textArea = new JTextArea();
        textArea.setOpaque(false);
        configURL = createHasteBinFromFile(FILE_CONFIG);
        serverpackcreatorlogURL = createHasteBinFromFile(LOG_SERVERPACKCREATOR);

        textAreaContent = String.format(
                "%s\n%s\n" +
                        "%s\n%s\n" +
                        LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.action"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.conf"),
                configURL,
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.spclog"),
                serverpackcreatorlogURL
        );

        textArea.setText(textAreaContent);

        options = new String[]{
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.dialog.yes"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.dialog.no"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.dialog.clipboard"),
        };

        userResponse = JOptionPane.showOptionDialog(
                aboutPanel,
                textArea,
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.dialog"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                ICON_HASTEBIN,
                options,
                options[0]
        );

        switch (userResponse) {
            case 0:

                try {
                    if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {

                        Desktop.getDesktop().browse(URI.create(configURL));
                        Desktop.getDesktop().browse(URI.create(serverpackcreatorlogURL));
                    }
                } catch (IOException ex) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("about.log.error.browser"), ex);
                }
                break;

            case 2:

                stringSelection = new StringSelection(textAreaContent);
                CLIPBOARD.setContents(stringSelection, null);
                break;

            default:
                break;
        }
    }

    /**
     * Upon button-press, open the GitHub issues link in the default-browser of the user.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void openIssueLink(ActionEvent event) {

        try {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("https://github.com/Griefed/ServerPackCreator/issues"));
            }
        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("about.log.error.browser"), ex);
        }

    }

    /**
     * Upon button-press, open the Discord invite link in the default-browser of the user.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void openDiscordLink(ActionEvent event) {

        try {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("https://discord.griefed.de"));
            }
        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("about.log.error.browser"), ex);
        }

    }

    /**
     * Create a HasteBin post from a given text file. The text file provided is read into a string and then passed onto
     * <a href="https://haste.zneix.eu">Haste zneix</a> which creates a HasteBin post out of the passed String and
     * returns the URL to the newly created post.<br>
     * Created with the help of <a href="https://github.com/kaimu-kun/hastebin.java">kaimu-kun's hastebin.java (MIT License)</a>
     * and edited to use HasteBin fork <a href="https://github.com/zneix/haste-server">zneix/haste-server</a>. My fork
     * of kaimu-kun's hastebin.java is available at <a href="https://github.com/Griefed/hastebin.java">Griefed/hastebin.java</a>.
     * @author kaimu-kun
     * @author Griefed
     * @param textFile The file which will be read into a String of which then to create a HasteBin post of.
     * @return String. Returns a String containing the URL to the newly created HasteBin post.
     */
    private String createHasteBinFromFile(File textFile) {
        String text = null;
        String requestURL = serverpackcreatorproperties.getProperty("de.griefed.serverpackcreator.configuration.hastebinserver","https://haste.zneix.eu/documents");
        String response = null;

        int postDataLength;

        URL url = null;

        HttpsURLConnection conn = null;

        byte[] postData;

        DataOutputStream dataOutputStream;

        BufferedReader bufferedReader;

        try { url = new URL(requestURL); }
        catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.abouttab.hastebin.request"), ex);}

        try { text = FileUtils.readFileToString(textFile, "UTF-8"); }
        catch (IOException ex) { LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.abouttab.hastebin.readfile"),ex); }

        postData = Objects.requireNonNull(text).getBytes(StandardCharsets.UTF_8);
        postDataLength = postData.length;

        try { conn = (HttpsURLConnection) Objects.requireNonNull(url).openConnection(); }
        catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.abouttab.hastebin.connection"), ex);}

        Objects.requireNonNull(conn).setDoOutput(true);
        conn.setInstanceFollowRedirects(false);

        try { conn.setRequestMethod("POST"); }
        catch (ProtocolException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.abouttab.hastebin.method"), ex);}

        conn.setRequestProperty("User-Agent", "HasteBin-Creator for ServerPackCreator");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setUseCaches(false);

        try {
            dataOutputStream = new DataOutputStream(conn.getOutputStream());
            dataOutputStream.write(postData);
            bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            response = bufferedReader.readLine();
        } catch (IOException ex) {
            LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.abouttab.hastebin.response"), ex);
        }

        if (Objects.requireNonNull(response).contains("\"key\"")) {
            response = "https://haste.zneix.eu/" + response.substring(response.indexOf(":") + 2, response.length() - 2);
        }

        if (response.contains("https://haste.zneix.eu")) {
            return response;
        } else {
            return LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.abouttab.hastebin.response");
        }

    }
}