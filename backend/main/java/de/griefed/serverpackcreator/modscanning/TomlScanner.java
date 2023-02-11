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
package de.griefed.serverpackcreator.modscanning;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.jar.JarFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class TomlScanner implements Scanner<TreeSet<File>, Collection<File>> {

  private static final Logger LOG = LogManager.getLogger(TomlScanner.class);
  private final TomlParser PARSER;
  private final String FORGE_MC = "^(forge|minecraft)$";
  private final String BOTH_SERVER = "^(BOTH|SERVER)$";

  @Contract(pure = true)
  @Autowired
  public TomlScanner(TomlParser tomlParser) {
    this.PARSER = tomlParser;
  }

  /**
   * Scan the {@code mods.toml}-files in mod JAR-files of a given directory for their sideness.
   * <br>
   * If {@code [[mods]]} specifies {@code side=BOTH|SERVER}, it is added.<br> If
   * {@code [[dependencies.modId]]} for Forge|Minecraft specifies {@code side=BOTH|SERVER }, it is
   * added.<br> Any modId of a dependency specifying {@code side=BOTH|SERVER} is added.<br> If no
   * sideness can be found for a given mod, it is added to prevent false positives.
   *
   * @param filesInModsDir A list of files in which to check the {@code mods.toml}-files.
   * @return Mods not to include in server pack based on mods.toml-configuration.
   * @author Griefed
   */
  @Override
  public @NotNull TreeSet<File> scan(@NotNull Collection<File> filesInModsDir) {

    TreeSet<File> serverMods = new TreeSet<>();

    TreeSet<String> idsRequiredOnServer = new TreeSet<>();

    CommentedConfig config;

    for (File modJar : filesInModsDir) {
      try {

        config = getConfig(modJar);

        // get all [[dependencies.n]] which are not minecraft|forge, but required by the mod
        idsRequiredOnServer.addAll(getModDependencyIdsRequiredOnServer(config));

        // get all mods required on the server
        idsRequiredOnServer.addAll(getModIdsRequiredOnServer(config));

      } catch (Exception e) {

        LOG.debug("Could not fully scan " + modJar.getName() + ". " + e.getMessage());
        serverMods.add(modJar);
      }
    }

    for (File modJar : filesInModsDir) {
      try {

        config = getConfig(modJar);

        TreeSet<String> idsInMod = getModIdsInJar(config);

        for (String id : idsInMod) {
          if (idsRequiredOnServer.contains(id)) {
            serverMods.add(modJar);
          }
        }

      } catch (Exception e) {

        LOG.debug("Could not fully scan " + modJar.getName() + ". " + e.getMessage());
        serverMods.add(modJar);
      }
    }

    TreeSet<File> excluded = new TreeSet<>(filesInModsDir);
    excluded.removeAll(serverMods);

    return excluded;
  }

  /**
   * Get all ids of mods required for running the server.
   *
   * @param config Base-config of the toml of the mod which contains all information.
   * @return Set of ids of mods required.
   * @throws ScanningException if the mod specifies no mods.
   */
  private @NotNull TreeSet<String> getModIdsRequiredOnServer(@NotNull CommentedConfig config)
      throws ScanningException {

    ArrayList<Map<String, Object>> configs = new ArrayList<>(100);
    TreeSet<String> ids = new TreeSet<>();

    if (config.valueMap().get("mods") == null) {
      throw new ScanningException("No mods specified.");
    } else {

      for (CommentedConfig commentedConfig :
          (ArrayList<CommentedConfig>) config.valueMap().get("mods")) {

        configs.add(commentedConfig.valueMap());
      }
    }

    Map<String, ArrayList<CommentedConfig>> dependencies = getMapOfDependencyLists(config);
    boolean containedForgeOrMinecraft = false;

    for (Map<String, Object> mod : configs) {

      String modId = mod.get("modId").toString();
      if (dependencies.containsKey(modId)) {

        for (CommentedConfig dependency : dependencies.get(modId)) {
          try {

            if (getModId(dependency).matches(FORGE_MC)) {

              containedForgeOrMinecraft = true;

              try {
                if (getSide(dependency).matches(BOTH_SERVER)) {
                  ids.add(modId);
                }
              } catch (NullPointerException ex) {
                // no side specified....assuming both|server
                ids.add(modId);
              }
            }

          } catch (NullPointerException e) {
            // no modId specified in dependency...assuming forge|minecraft and both|server
            containedForgeOrMinecraft = true;
            ids.add(modId);
          }
        }
      } else {
        // contains no self referencing dependency...
        ids.add(modId);
      }

      if (!containedForgeOrMinecraft) {
        ids.add(modId);
      }
    }

    return ids;
  }

  /**
   * Acquire a list of ids of dependencies required by the passed mod in order to run on a modded
   * server. Only if all dependencies in this mod specify {@code CLIENT} for either {@code forge }
   * or {@code minecraft} is a dependency not added to the list of required dependencies. Otherwise,
   * all modIds mentioned in the dependencies of this mod, which are neither {@code forge} nor
   * {@code minecraft} get added to the list.
   *
   * @param config Base-config of the toml of the mod which contains all information.
   * @return Set of ids of mods required as dependencies.
   * @throws ScanningException if the mod has invalid dependency declarations or specifies no mods.
   */
  private @NotNull TreeSet<String> getModDependencyIdsRequiredOnServer(@NotNull CommentedConfig config)
      throws ScanningException {
    TreeSet<String> ids = new TreeSet<>();

    Map<String, ArrayList<CommentedConfig>> dependencies = getMapOfDependencyLists(config);

    TreeSet<String> idsInMod = getModIdsInJar(config);

    try {
      boolean confidentOnClientSide = true;
      for (String modId : idsInMod) {

        if (dependencies.containsKey(modId)) {

          for (CommentedConfig dependency : dependencies.get(modId)) {

            if (getModId(dependency).matches(FORGE_MC)
                && getSide(dependency).matches(BOTH_SERVER)) {
              confidentOnClientSide = false;
            }
          }

        } else {
          confidentOnClientSide = false;
          break;
        }
      }

      if (confidentOnClientSide) {
        return ids;
      }

    } catch (NullPointerException ignored) {

    }

    for (Map.Entry<String, ArrayList<CommentedConfig>> entry : dependencies.entrySet()) {
      for (CommentedConfig commentedConfig : entry.getValue()) {

        try {

          // dependency forge|minecraft?
          if (!getModId(commentedConfig).matches(FORGE_MC)) {

            try {

              // dependency required on the server?
              if (getSide(commentedConfig).matches(BOTH_SERVER)) {
                ids.add(getModId(commentedConfig));
              }

            } catch (NullPointerException ex) {
              // dependency specifies no side
              ids.add(getModId(commentedConfig));
            }
          }

        } catch (NullPointerException e) {

          // dependency specifies no modId, so use parent.
          if (!entry.getKey().toLowerCase().matches(FORGE_MC)) {
            ids.add(entry.getKey());
          }
        }
      }
    }

    return ids;
  }

  /**
   * Acquire a set of ids of mods required for running the server.
   *
   * @param config Base-config of the toml of the mod which contains all information.
   * @return Set of ids of mods required.
   * @throws ScanningException if the mod specifies no...well...mods.
   */
  private @NotNull TreeSet<String> getModIdsInJar(@NotNull CommentedConfig config)
      throws ScanningException {
    TreeSet<String> ids = new TreeSet<>();
    if (config.valueMap().get("mods") == null) {

      throw new ScanningException("No mods specified.");

    } else {

      for (CommentedConfig commentedConfig :
          (ArrayList<CommentedConfig>) config.valueMap().get("mods")) {

        ids.add(getModId(commentedConfig));
      }
    }

    return ids;
  }

  /**
   * Acquire the base toml-config of a mod.
   *
   * @param file The file from which to acquire the toml config.
   * @return Config read from the toml in the mod.
   * @throws IOException if the mods.toml file could not be read/found.
   */
  private @NotNull CommentedConfig getConfig(@NotNull File file) throws IOException {
    JarFile jarFile = new JarFile(file);
    InputStream tomlStream = jarFile.getInputStream(jarFile.getJarEntry("META-INF/mods.toml"));
    CommentedConfig config = PARSER.parse(tomlStream);
    jarFile.close();
    tomlStream.close();
    return config;
  }

  /**
   * Acquire a map of all dependencies specified by a mod.
   *
   * @param config Base-config of the toml of the mod which contains all * information.
   * @return Map of dependencies for the passed mod config, String keys are mapped to ArrayLists of
   * CommentedConfigs.
   * @throws ScanningException if the mod declares no dependencies.
   */
  private @NotNull Map<String, ArrayList<CommentedConfig>> getMapOfDependencyLists(@NotNull CommentedConfig config)
      throws ScanningException {

    if (config.valueMap().get("dependencies") == null) {
      throw new ScanningException("No dependencies specified.");
    }

    Map<String, ArrayList<CommentedConfig>> dependencies = new HashMap<>(100);

    if (config.valueMap().get("dependencies") instanceof ArrayList) {

      dependencies.put(
          getModId(((ArrayList<CommentedConfig>) config.valueMap().get("mods")).get(0)),
          (ArrayList<CommentedConfig>) config.valueMap().get("dependencies"));

    } else {

      for (Map.Entry<String, Object> entry :
          ((CommentedConfig) config.valueMap().get("dependencies")).valueMap().entrySet()) {

        dependencies.put(
            entry.getKey().toLowerCase(), (ArrayList<CommentedConfig>) entry.getValue());
      }
    }

    return dependencies;
  }

  /**
   * Acquire the modId from the passed config.
   *
   * @param config Mod- or dependency-config which contains the modId.
   * @return {@code modId} from the passed config, in lower-case letters.
   */
  private @NotNull String getModId(@NotNull CommentedConfig config) {
    return config.valueMap().get("modId").toString().toLowerCase();
  }

  /**
   * Acquire the side of the config of the passed dependency.
   *
   * @param config Mod- or dependency-config which contains the modId.
   * @return {@code side} from the passed config, in upper-case letters.
   */
  private @NotNull String getSide(@NotNull CommentedConfig config) {
    return config.valueMap().get("side").toString().toUpperCase();
  }
}