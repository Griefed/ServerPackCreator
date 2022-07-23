package de.griefed.serverpackcreator;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.jar.JarFile;

public class TomlScanner {

  private final TomlParser PARSER;
  private final String FORGE_MC = "^(forge|minecraft)$";
  private final String BOTH_SERVER = "^(BOTH|SERVER)$";

  public TomlScanner(TomlParser tomlParser) {
    this.PARSER = tomlParser;
  }

  public Map<String, List<String>> scan(File[] jarFiles) {
    List<String> allFiles = new ArrayList<>();
    for (File modJar : jarFiles) {
      allFiles.add(modJar.getName());
    }

    List<String> serverMods = new ArrayList<>();

    TreeSet<String> idsRequiredOnServer = new TreeSet<>();

    CommentedConfig config;

    for (File modJar : jarFiles) {
      try {

        config = getConfig(modJar);

        // get all [[dependencies.n]] which are not minecraft|forge, but required by the mod
        idsRequiredOnServer.addAll(getIdsOfInstalledModsDependencies(config));

        // get all mods required on the server
        idsRequiredOnServer.addAll(getIdsOfInstalledModsRequiredOnServer(config));

      } catch (Exception e) {

        // TODO replace with logging
        System.out.println("Error scanning " + modJar.getName() + ". Line 54. " + e);
        serverMods.add(modJar.getName());
      }
    }

    for (File modJar : jarFiles) {
      try {

        config = getConfig(modJar);

        TreeSet<String> idsInMod = getModIdsInJar(config);

        for (String id : idsInMod) {
          if (idsRequiredOnServer.contains(id)) {
            serverMods.add(modJar.getName());
          }
        }

      } catch (Exception e) {

        // TODO replace with logging
        System.out.println("Error scanning " + modJar.getName() + ". Line 74. " + e.getMessage());
        serverMods.add(modJar.getName());
      }
    }

    List<String> excluded = new ArrayList<>(allFiles);
    excluded.removeAll(serverMods);

    return new HashMap<String, List<String>>() {
      {
        put("mods", allFiles);
        put("excluded", excluded);
      }
    };
  }

  private TreeSet<String> getIdsOfInstalledModsRequiredOnServer(CommentedConfig config)
      throws ScanningException {

    ArrayList<Map<String, Object>> configs = new ArrayList<>();
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

  private TreeSet<String> getIdsOfInstalledModsDependencies(CommentedConfig config)
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

  private TreeSet<String> getModIdsInJar(CommentedConfig config) throws ScanningException {
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

  private CommentedConfig getConfig(File file) throws IOException {
    JarFile jarFile = new JarFile(file);
    InputStream tomlStream = jarFile.getInputStream(jarFile.getJarEntry("META-INF/mods.toml"));
    CommentedConfig config = PARSER.parse(tomlStream);
    jarFile.close();
    tomlStream.close();
    return config;
  }

  private Map<String, ArrayList<CommentedConfig>> getMapOfDependencyLists(CommentedConfig config)
      throws ScanningException {

    if (config.valueMap().get("dependencies") == null) {
      throw new ScanningException("No dependencies specified.");
    }

    Map<String, ArrayList<CommentedConfig>> dependencies = new HashMap<>();

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

  private String getModId(CommentedConfig config) {
    return config.valueMap().get("modId").toString().toLowerCase();
  }

  private String getSide(CommentedConfig config) {
    return config.valueMap().get("side").toString().toUpperCase();
  }
}
