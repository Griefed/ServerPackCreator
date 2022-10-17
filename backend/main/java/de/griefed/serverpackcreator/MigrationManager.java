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
package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.I18n;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The migration manager of ServerPackCreator is responsible for determining update-steps between a
 * given old version and a given new version. The determined steps between updates are then executed
 * in order to ensure a users environment is up-to-date, even when skipping multiple versions when
 * updating.
 * <p>
 * This does not guarantee a safe update when updating between major versions, as major versions
 * tend to contain breaking changes, and depending on those changes there will still be a need for
 * the user to ensure they can safely update. One example would be major version 3 to major version
 * 4, where the Java version required by ServerPackCreator will rise from Java 8 to Java 17 or
 * later.<br> Aas this is not something ServerPackCreator can take care of, it is a good example for
 * a migration which this manager can not take care of.
 * <p>
 * Other migrations, such as updating the {@code log4j2.xml} and the logs-directory inside said file
 * can and will be taken care of.
 * <p>
 * Some migrations will require the user to restart ServerPackCreator for the executed migrations to
 * take full effect. A given migration method which executes such migrations should register a
 * migration message informing the user about the need to restart SPC.
 * <p>
 * Migration messages are displayed in a dialog when using the GUI as well as printed to the
 * serverpackcreator.log.
 *
 * @author Griefed
 */
@SuppressWarnings("unused")
public final class MigrationManager {

  private static final Logger LOG = LogManager.getLogger(MigrationManager.class);
  private final MigrationMethods MIGRATION_METHODS = new MigrationMethods();
  private final List<MigrationMessage> MIGRATION_MESSAGES = new ArrayList<>(10);
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final I18n I18N;
  private String previous;

  MigrationManager(
      @NotNull ApplicationProperties applicationProperties,
      @NotNull I18n i18n) {
    APPLICATIONPROPERTIES = applicationProperties;
    I18N = i18n;
  }

  /**
   * Perform necessary migrations from the old version to the current version being used.
   *
   * @author Griefed
   */
  void migrate() {
    String old = APPLICATIONPROPERTIES.oldVersion();
    String current = APPLICATIONPROPERTIES.serverPackCreatorVersion();

    //noinspection ConstantConditions
    if (old == null || old.isEmpty()) {
      LOG.info("No old version received. Assuming first time run.");
      APPLICATIONPROPERTIES.setOldVersion(current);
      return;
    }
    if (current.matches(".*(alpha|beta|dev).*")) {
      LOG.info("No migrations to execute. Running alpha, beta or dev version.");
      return;
    }
    if (old.matches(".*(alpha|beta|dev).*")) {
      LOG.info("No migrations to execute. Upgrading from alpha, beta or dev version.");
      return;
    }
    if (isOlder(old,
                current)) {
      LOG.info("No migrations to execute. User went back a version. From " + old + " to " + current
                   + ".");
      return;
    }
    if (old.equals(current)) {
      LOG.info("No migrations to execute. User has not updated.");
    }

    List<Method> migrationMethods = getMigrationMethods(old,
                                                        current);
    if (!migrationMethods.isEmpty()) {

      previous = old;
      migrationMethods.forEach(
          method -> {
            LOG.info("Resolving migrations for: " + toSemantic(method.getName()));
            try {
              method.setAccessible(true);
              method.invoke(MIGRATION_METHODS);
            } catch (IllegalAccessException ex) {
              LOG.error("Could not access migration-method: " + method.getName()
                            + ". Please report this on GitHub and include the logs!",
                        ex);
            } catch (InvocationTargetException ex) {
              LOG.error("Could not invoke migration-method: " + method.getName()
                            + ". Please report this on GitHub and include the logs!",
                        ex);
            }
          }
      );
    } else {
      LOG.info("No migrations to execute.");
    }
    APPLICATIONPROPERTIES.setOldVersion(current);
  }

  /**
   * Check if the first version is older than the second one.
   *
   * @param old          The old version used.
   * @param checkAgainst The current version being used.
   * @return {@code true} if the current version is older than the previously used version.
   * @author Griefed
   */
  private boolean isOlder(@NotNull String old,
                          @NotNull String checkAgainst) {
    return older(
        semantics(old),
        semantics(checkAgainst));
  }

  /**
   * Acquire the list of migration-methods to execute based on the current version being used. All
   * methods which match a version newer than the passed one are executed.
   *
   * @param oldVersion The current version being used.
   * @return Methods to execute to ensure proper migration between version updates.
   * @author Griefed
   */
  private @NotNull List<Method> getMigrationMethods(
      @NotNull String oldVersion,
      @NotNull String currentVersion) {

    List<Method> run = new ArrayList<>(100);

    Method[] methods = MIGRATION_METHODS.getClass().getDeclaredMethods();
    HashMap<String, Method> methodMap = new HashMap<>(100);
    TreeSet<String> methodVersions = new TreeSet<>();

    for (Method method : methods) {
      String methodVersion = toSemantic(method.getName());
      methodMap.put(methodVersion,
                    method);
      methodVersions.add(methodVersion);
    }

    for (String methodVersion : methodVersions) {

      if (isNewer(oldVersion,
                  methodVersion) && isOlderOrSame(currentVersion,
                                                  methodVersion)) {

        run.add(methodMap.get(methodVersion));
      }
    }
    return run;
  }

  /**
   * Convert a text-based version to a semantic representation, usable in version comparisons.
   *
   * @param textVersion The text-based version to convert to the semantic format.
   * @return Semantic representation of the text-based version.
   * @author Griefed
   */
  private @NotNull String toSemantic(@NotNull String textVersion) {
    textVersion = textVersion
        .replace("Zero",
                 "0")
        .replace("One",
                 "1")
        .replace("Two",
                 "2")
        .replace("Three",
                 "3")
        .replace("Four",
                 "4")
        .replace("Five",
                 "5")
        .replace("Six",
                 "6")
        .replace("Seven",
                 "7")
        .replace("Eight",
                 "8")
        .replace("Nine",
                 "9")
        .replace("Point",
                 ".");
    return textVersion;
  }

  /**
   * Compare two integer arrays of semantic version against each other and determine whether we have
   * an old version at hand.
   *
   * @param old          The old version numbers.
   * @param checkAgainst The new version numbers to check whether they represent an older version.
   * @return {@code true} if the version numbers checked against represent an older version.
   * @author Griefed
   */
  private boolean older(@NotNull Integer @NotNull [] old,
                        @NotNull Integer @NotNull [] checkAgainst) {
    // Current MAJOR version smaller?
    if (checkAgainst[0] < old[0]) {
      return true;
    }

    // Current MAJOR version equal and current MINOR version smaller?
    if (checkAgainst[0].equals(old[0]) && checkAgainst[1] < old[1]) {
      return true;
    }

    // Current MAJOR version equal, current MINOR equal, current PATCH version smaller?
    return checkAgainst[0].equals(old[0]) && checkAgainst[1].equals(old[1])
        && checkAgainst[2] < old[2];
  }

  /**
   * Get the major, minor and patch numbers of a version.
   *
   * @param version The version of which to get the major, minor and patch number array.
   * @return Array containing the major, minor and patch numbers.
   * @author Griefed
   */
  private @NotNull Integer @NotNull [] semantics(@NotNull String version) {
    return Arrays.stream(version.split("\\."))
                 .map(Integer::valueOf).toArray(Integer[]::new);
  }

  /**
   * Check if the first version is newer than the second one.
   *
   * @param old          The current version being used.
   * @param checkAgainst The version the migration-method represents.
   * @return {@code true} if the migration-method version is newer than the current version.
   * @author Griefed
   */
  private boolean isNewer(@NotNull String old,
                          @NotNull String checkAgainst) {
    return newer(
        semantics(old),
        semantics(checkAgainst));
  }

  /**
   * Check if the first version is older than or the same as the second one.
   *
   * @param old          The old version used.
   * @param checkAgainst The current version being used.
   * @return {@code true} if the current version is older than or the same as the previously used
   * version.
   * @author Griefed
   */
  private boolean isOlderOrSame(@NotNull String old,
                                @NotNull String checkAgainst) {
    return oldOrSame(
        semantics(old),
        semantics(checkAgainst));
  }

  /**
   * Compare two integer arrays of semantic version against each other and determine whether we have
   * a new version at hand.
   *
   * @param old          The old version numbers.
   * @param checkAgainst The new version numbers to check whether they represent a newer version.
   * @return {@code true} if the version numbers checked against represent a newer version.
   * @author Griefed
   */
  private boolean newer(@NotNull Integer @NotNull [] old,
                        @NotNull Integer @NotNull [] checkAgainst) {
    // Method MAJOR bigger?
    if (checkAgainst[0] > old[0]) {
      return true;
    }

    // Method MAJOR version equal and method MINOR bigger?
    if (checkAgainst[0].equals(old[0]) && checkAgainst[1] > old[1]) {
      return true;
    }

    // Method MAJOR equal, method MINOR equal, method PATCH bigger?
    return checkAgainst[0].equals(old[0]) && checkAgainst[1].equals(old[1])
        && checkAgainst[2] > old[2];
  }

  /**
   * Compare two integer arrays of semantic version against each other and determine whether we have
   * an old version or the same one at hand.
   *
   * @param old          The old version numbers.
   * @param checkAgainst The new version numbers to check whether they represent an older version.
   * @return {@code true} if the version numbers checked against represent an older version or the
   * same.
   * @author Griefed
   */
  @Contract(pure = true)
  private boolean oldOrSame(@NotNull Integer @NotNull [] old,
                            @NotNull Integer @NotNull [] checkAgainst) {
    return checkAgainst[0] <= old[0]
        && checkAgainst[1] <= old[1]
        && checkAgainst[2] <= old[2];
  }

  /**
   * Check if the first version is newer than or the same as the second one.
   *
   * @param current      The old version used.
   * @param checkAgainst The current version being used.
   * @return {@code true} if the current version is newer than or the same as the previously used
   * version.
   * @author Griefed
   */
  private boolean isNewerOrSame(@NotNull String current,
                                @NotNull String checkAgainst) {
    return newOrSame(
        semantics(current),
        semantics(checkAgainst));
  }

  /**
   * Compare two integer arrays of semantic version against each other and determine whether we have
   * a new version or the same one at hand.
   *
   * @param current      The old version numbers.
   * @param checkAgainst The new version numbers to check whether they represent an older version.
   * @return {@code true} if the version numbers checked against represent a newer version or the
   * same.
   * @author Griefed
   */
  @Contract(pure = true)
  private boolean newOrSame(@NotNull Integer @NotNull [] current,
                            @NotNull Integer @NotNull [] checkAgainst) {
    return checkAgainst[0] >= current[0]
        && checkAgainst[1] >= current[1]
        && checkAgainst[2] >= current[2];
  }

  /**
   * Convert a semantic version to text-based representation.
   *
   * @param version The version to convert.
   * @return The semantic version converted to text-based representation.
   * @author Griefed
   */
  private @NotNull String toText(@NotNull String version) {
    StringBuilder textVersion = new StringBuilder();

    for (char character : version.toCharArray()) {
      switch (String.valueOf(character)) {
        case "0":
          textVersion.append("Zero");
          break;
        case "1":
          textVersion.append("One");
          break;
        case "2":
          textVersion.append("Two");
          break;
        case "3":
          textVersion.append("Three");
          break;
        case "4":
          textVersion.append("Four");
          break;
        case "5":
          textVersion.append("Five");
          break;
        case "6":
          textVersion.append("Six");
          break;
        case "7":
          textVersion.append("Seven");
          break;
        case "8":
          textVersion.append("Eight");
          break;
        case "9":
          textVersion.append("Nine");
          break;
        case ".":
          textVersion.append("Point");
          break;
      }
    }
    return textVersion.toString();
  }

  /**
   * A list of migration messages, if any, to display in logs, GUI or any other place you can think
   * of. A migration message includes the versions from which and to which version the migration
   * took place, as well as a list of changes made during it, if any.
   *
   * @return A list of migrations that took place.
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull List<MigrationMessage> getMigrationMessages() {
    return MIGRATION_MESSAGES;
  }

  /**
   * A migration message include any and all information for a particular migration which has taken
   * place during the startup of ServerPackCreator. It contains the
   *
   * @author Griefed
   */
  @SuppressWarnings("InnerClassMayBeStatic")
  public final class MigrationMessage {

    private final String FROM;
    private final String TO;
    private final List<String> CHANGES = new ArrayList<>(20);

    private MigrationMessage(@NotNull String from,
                             @NotNull String to,
                             @NotNull List<String> changes) {
      FROM = from;
      TO = to;
      CHANGES.addAll(changes);
    }

    @Contract(pure = true)
    public @NotNull String fromVersion() {
      return FROM;
    }

    @Contract(pure = true)
    public @NotNull String toVersion() {
      return TO;
    }

    @Contract(pure = true)
    public @NotNull List<String> changes() {
      return CHANGES;
    }

    @Contract(pure = true)
    public int count() {
      return CHANGES.size();
    }

    public @NotNull String get() {
      return toString();
    }

    @Override
    public @NotNull String toString() {
      String header = "From " + FROM + " to " + TO + " the following changes were made:\n";
      StringBuilder content = new StringBuilder();
      content.append(header).append("\n");
      for (int i = 0; i < CHANGES.size(); i++) {
        content.append("  (").append(i + 1).append("): ").append(CHANGES.get(i)).append("\n");
      }
      return content.toString();
    }
  }

  /**
   * Inner class which holds all methods responsible for performing migration-steps.
   * <strong>ONLY</strong> methods which perform migration steps are allowed here!
   *
   * @author Griefed
   */
  @SuppressWarnings({"InnerClassMayBeStatic", "unused"})
  private final class MigrationMethods {

    /**
     * Migrate the log4j2.xml to the new settings due to home-directory preparations.
     *
     * @author Griefed
     */
    private void ThreePointOneFivePointZero() {
      try {
        List<String> changes = new ArrayList<>(10);

        File log4J2Xml = new File(APPLICATIONPROPERTIES.homeDirectory(),
                                  "log4j2.xml");
        String oldLogs = "<Property name=\"log-path\">logs</Property>";
        String newLogs =
            "<Property name=\"log-path\">"
                + APPLICATIONPROPERTIES.logsDirectory()
                + "</Property>";

        String log4j = FileUtils.readFileToString(log4J2Xml,
                                                  StandardCharsets.UTF_8);

        boolean changed = false;
        if (log4j.contains(oldLogs)) {

          changed = true;
          log4j = log4j.replace(oldLogs,
                                newLogs);

          String message = String.format(
              I18N.getMessage("migrationmanager.migration.threepointonefilepointzero.directory")
              ,
              APPLICATIONPROPERTIES.logsDirectory());

          changes.add(message);
        }

        if (log4j.contains("<Configuration status=\"WARN\">")) {
          changed = true;
          log4j = log4j.replace("<Configuration status=\"WARN\">",
                                "<Configuration monitorInterval=\"30\">");

          changes.add(
              I18N.getMessage("migrationmanager.migration.threepointonefilepointzero.interval"));

        }

        if (changed) {

          FileUtils.writeStringToFile(log4J2Xml,
                                      log4j,
                                      StandardCharsets.UTF_8);
          changes.add(
              I18N.getMessage("migrationmanager.migration.threepointonefilepointzero.restart"));
        }

        if (!changes.isEmpty()) {
          MIGRATION_MESSAGES.add(new MigrationMessage(
              previous,
              "3.15.0",
              changes));
        }
      } catch (IOException ex) {
        LOG.error("Error reading/writing log4j2.xml.",
                  ex);
      }
    }
  }
}
