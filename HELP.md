# Tips, tricks and help

## Custom Themes

As of 5.0.0 you can create your own themes for ServerPackCreator. To get an idea on what a custom theme looks like,
take a look at the different `properties`-files inside the `themes`-directory when using ServerPackCreators GUI.

To see the examples in action, browse to *Settings -> GUI* and select any of the following from the themes-dropdown:

- BisectHosting
- Chorb
- Elocin
- Kreezxil
- ModernGamingWorld

They're people, or in the case of BisectHosting a company, which have had a noticeable impact on ServerPackCreator in a
good way. So as a little "Thank You", I've decided to add custom themes in their name.

The FormDev theme editor was used in the creation of these themes. You can get it here:
https://www.formdev.com/flatlaf/theme-editor/#download

Run the downloaded JAR-file and point it towards the themes-directory. That will allow you to take a proper look at the
custom themes, along with a preview of your changes.

FormDevs documentation for customization is pretty nice. I suggest you take a thorough look at it over at:
https://www.formdev.com/flatlaf/customizing/

Experiment with the existing themes, create your own and have fun!

Every .properties-file in the themes-folder will automatically be added to the list of available themes inside
ServerPackCreator. If you've added or changed a theme whilst running ServerPackCreator, you will need to restart it for
the changes to take effect.

Request for theme creations will be ignored. Please be aware that I implemented this feature so YOU can create themes.
Not in order for ME to create themes FOR you. :D

Happy theming!

## Start Script Templates (3.8.x and up)

As of 3.8.0, ServerPackCreator creates start scripts based on templates.
Inside the `server_files`-directory you will find the two default templates:

1. `default_template.ps1`
2. `default_template.sh`

You have two options now:

1. Edit the existing default templates to your liking and saving them under a different name
2. Create your own templates or add additional ones for different file-endings

## Powershell

> **Windows Updates**
> Sometimes, Windows updates reset the execution policy of PowerShell scripts, which in turn requires
> you, the user, to change it again. Should PowerShell scripts display the behaviour of *Opening and
then immediately closing* again, consult the **Running Powershell scripts**-section again below.
> {.is-warning}


> **Paths with spaces**
> Powershell scripts by default can not be opened with a double-click if the path to said script
> contains spaces. If you wish to remedy this or want to read more about this behaviour, this article
> talks about it in great detail:
[Fix Problem Where Windows PowerShell Cannot Run Script Whose Path Contains Spaces](https://blog.danskingdom.com/fix-problem-where-windows-powershell-cannot-run-script-whose-path-contains-spaces/)<br>
> Keep in mind though that things may still break when working with paths with spaces in them. If
> things still break with a path with spaces, even after trying the fixes from the link above, then I
> suggest moving things to a folder whose path contains no spaces. I'm afraid there's nothing I can do
> about that.
> {.is-warning}

> **Running Powershell scripts**
> By default, running Powershell scripts from untrusted sources is probably disabled on your system.
> As such, you will not be able to run the `start.ps`-scripts just yet. You need to allow running
> unsigned scripts first. See https://superuser.com/a/106363 for a short explanation on how to
> enable/allow running unsigned scripts with Powershell.
> Bear in mind that this introduces a security risk on your system. After making the changes from the
> link above, you can run any Powershell script you like, and as such, introduce any and all security
> risk into your system. So, beware when running scripts from unknown sources. If you run the
> exectuion-policy command from a non-administrator terminal, you may be prompted with a question as
> to how exactly you would like to adjust it. When this happens, please select `[Y]`, as any other option is potentially
> more dangerous.
> {.is-warning}

### Default values

ServerPackCreator always supplies a couple of default key-value-pairs during script creation.

| Key                               | Value                                                                                                                                                                                                                                                                                           |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| SPC_SERVERPACKCREATOR_VERSION_SPC | The version of ServerPackCreator with which the start script was created.                                                                                                                                                                                                                       |
| SPC_MINECRAFT_VERSION_SPC         | The Minecraft version of the modpack from which the server pack is created.                                                                                                                                                                                                                     |
| SPC_MODLOADER_SPC                 | The modloader of the modpack from which the server pack is created.                                                                                                                                                                                                                             |
| SPC_MODLOADER_VERSION_SPC         | The modloader version of the modpack from which the server pack is created.                                                                                                                                                                                                                     |
| SPC_MINECRAFT_SERVER_URL_SPC      | The download URL corresponding to the Minecraft version of the modpack from which the server pack is created.                                                                                                                                                                                   |
| SPC_JAVA_ARGS_SPC                 | Java JVM args which are to be used when running the server pack.                                                                                                                                                                                                                                |
| SPC_JAVA_SPC                      | `java` by default. Start scripts in ZIP-archives will always receive `java` as the value. You may change the value in the table provided in the GUI if you need a different Java path for local testing and debugging. Scripts in the unzipped server pack will have the custom value avaiable. |
| SPC_FABRIC_INSTALLER_VERSION_SPC  | The latest release version of the Fabric installer during the creation of the start scripts.                                                                                                                                                                                                    |
| SPC_QUILT_INSTALLER_VERSION_SPC   | The latest release version of the Quilt installer during the creation of the start scripts.                                                                                                                                                                                                     |

### Placeholders and values (3.14.x and up)

As of 3.14.x you can add and edit placeholders.
Make sure to map your desired value to the appropriate placeholder key.
The GUI provides a table where you can configure these, or any other values, to your liking.

The way this system works is that ServerPackCreator scans the given template for any occurance of a
given placeholder, say `SPC_JAVA_SPC`, and replaces that occurance in the template with the assigned
value, `java` by default unless you changed it.
After every configured placeholder has been replaced with the configured value, the resulting text
is written to the start scripts, based on the file-endings of your provided templates.
BAM! Template engine with customizable content!

> **Working with custom placeholders**
> When adding custom placeholders to your start script templates, make sure to write them in such a
> way that no other text may get replaced with the value by accident.
> Take the default placeholders for example. They have a pre- and suffix, `SPC_` and `_SPC`
> respectively. That way, no other text will get replaced by accident. If you
> were to use placeholders such as `JAVA` only, then bad stuff can and will happen. The default
> shell-script, for example, uses the variable name `JAVA` for Java-based operations. With the
> previously mentioned placeholder `JAVA`, that variable would be removed from the scripts,
> potentially rendering them useless. Nobody wants that, right? :D
> {.is-warning}


> **Default placeholder limitations**
> Of all available default placeholders mentioned above, only one of them can be overriden via a given configuration.
> Said placeholder is `SPC_JAVA_SPC`. This one allows you to set a different Java executable/binary path for your local scripts,
> to make testing your server pack easier. The files in the ZIP-archive will always have the `SPC_JAVA_SPC=java` key-value-pair,
> to ensure any given user of your server pack has the value most likely to work on their system.
> {.is-warning}

### Changing the default templates

Starting from versions **3.13.x** and up, the default templates are always overwritten during
startup of ServerPackCreator, to ensure any user always has the latest version of the default
templates available to them and to ensure any changes made to them, by me, end up in the users
hands.
This has the benefit of making any migration for future versions not your problem.

The downside: If you want to customize these templates, you will have to create separate templates
and set the property for the templates accordingly.

In other words:

1. Copy the desired template to a name of your liking, e.g. `custom-template.ps1`
   and `custom-template.sh`
2. Make your changes to these custom templates
3. Set `de.griefed.serverpackcreator.serverpack.script.template`
   to `custom-template.ps1,custom-template.sh`
4. ...
5. Profit

Now the default templates will not be used to create start scripts for your server packs, but
instead, your custom-templates will be used.

### Adding/removing templates

1. The file-endings of the templates determine the file ending of the start script (`template.bat`
   becomes `start.bat`, `template.sh` becomes `start.sh` and so on)
2. Put your template(s) into the `server_files`-directory
3. Edit the `serverpackcreator.properties`-file and change
   the `de.griefed.serverpackcreator.serverpack.script.template`-property accordingly

**Example:**
Say you've written a very nice template for some `.bat`
-scripts, `my-awesome-batch-script-template.bat`, and want ServerPackCreator to use said template
during the script creation.
In that case you would need to put said template into the `server_files`-directory,
set `de.griefed.serverpackcreator.serverpack.script.template`
to `my-awesome-batch-script-template.bat` so it looks
like `de.griefed.serverpackcreator.serverpack.script.template=my-awesome-batch-script-template.bat`
and you've good to go.

When you now generate a server pack, a `start.bat`-script will be created based on
your `my-awesome-batch-script-template.bat`-template.

So, at the end, after writing your template and adding it to your ServerPackCreator configuration,
your folders and config should look/contain like the following:

- **ServerPackCreator
  Properties:** `de.griefed.serverpackcreator.serverpack.script.template=default_template.ps1,default_template.sh,my-awesome-batch-script-template.bat`
- **server_files-folder should contain:**
    - default_template.ps1
    - default_template.sh
    - my-awesome-batch-script-template.bat
    - [...]

This will result in ServerPackCreator creating three scripts: `start.bat`, `start.ps1`, `start.sh`

> **Encoding**
> When writing custom templates, you must store them with **UTF-8** encoding. That is the encoding in
> which ServerPackCreator reads them. So in order for control characters or any other special
> characters to work as you expect them to, make sure to encode your custom templates in **UTF-8**!
> {.is-warning}

## Automatically updating script Java placeholder (3.14.x and up)

As of ServerPackCreator version 3.14.x you can define properties which, when configured correctly,
will set the `SPC_JAVA_SPC`-placeholder in the script variables depending on which Java version is
required by Minecraft.

The two property in question:

- `de.griefed.serverpackcreator.script.javaN`

Where `N` represents the version of the Java installation you want to make available to
ServerPackCreator for script placeholder editing.

Set any of these to the exact path to your correlating Java executable or binary file.

Examples:

- `de.griefed.serverpackcreator.script.java8=C\:/Program Files/Java/jdk1.8.0_331/bin/java.exe`
- `de.griefed.serverpackcreator.script.java17=C\:/Program Files/Java/jdk-17.0.3.1/bin/java.exe`

Now, when you select your Minecraft version in your GUI, or load an already existing configuration,
ServerPackCreator will set the value for the `SPC_JAVA_SPC`-placeholder to either
`C\:/Program Files/Java/jdk1.8.0_331/bin/java.exe` or
`C\:/Program Files/Java/jdk-17.0.3.1/bin/java.exe`,
depending on whether the Minecraft version requires Java 8 or Java 17.

The `SPC_JAVA_SPC`-placeholder in the scripts in the ZIP-archives, should you choose to let
ServerPackCreator create one, will still be set to `java`.

### Deactivating the automatic updating of the Java placeholder

It may not be desired by everyone to always automatically update the Java placeholder in the script
variables.
If you don't want ServerPackCreator to do so, set the following property in
your `serverpackcreator.properties` to `false`

`de.griefed.serverpackcreator.script.java.autoupdate`

This will disable the automatic updating of the `SPC_JAVA_SPC`-placeholder in your script variables.

## Clientside-only modslist

### Updating

If you wish to use the latest and greatest hits of clientside-only mods ServerPackCreator has to
offer, you need to update it via **Menu Bar -> File -> Update Fallback Clientside Modslist** and
then either

- Clear your clientside-only mods list in your config, save it, then load it again (either via the
  menu or by restarting SPC)

**OR**

- Generate a new configuration and migrate your old config to it

**OR**

- Generate a new configuration and copy the list to your old config manually

**OR**

- Empty the clientside-mods field, save the configuration, load the configuration. This will fill it
  with the default clientside-mods list

If you added custom mods to this list, you must make sure you migrate them over to the new setting
yourself.

### Filter methods (3.12.x and up)

You can change the behaviour with which ServerPackCreator filters the mods with the list of
clientside-only modslist you specify.
By default, ServerPackCreator will check each entry in that list against the name of every mod-JAR
in your modpacks mods-directory and see whether said JAR starts with said entry.

Example:

- JAR: BetterPingDisplay-1.16.5-1.0.jar
- Entry: BetterPingDisplay-

The mod will be excluded, because its name starts with `BetterPingDisplay-`.

This behaviour can be changed by
editing `de.griefed.serverpackcreator.serverpack.autodiscovery.filter=START`
in `serverpackcreator.properties`.

There are five possible settings which affect the way these filters happen:

1. **`START`**

    - Default setting. Checks whether the name of a mod *starts* with a given entry.

2. **`END`**

    - Checks whether the name of a mod *ends* with a given entry.

3. **`CONTAIN`**

    - Checks whether the name of a mod *contains* a given entry.

4. **`REGEX`**

    - Performs a regex-match of the name of a mod against a given entry.

5. **`EITHER`**

    - Not recommended unless you know **exactly** what you are doing. If used carelessly, this can
      result in all mods or unwanted mods being excluded. Checks whether any of the above result in
      a positive match.

## FancyMenu

Starting from version **2.12.1** FancyMenu can be run on servers, too.
If you use **2.12.1** or newer, and you want to include it in your server pack, you need to:

1. Remove the `fancymenu_`-entry from your list of clientside-only mods-list.
2. To be on the safe side, add an entry to your file/directories to include in your server pack for
   your version of FancyMenu:
    - Example: `mods/fancymenu_forge_2.12.1_MC_1.19-1.19.1.jar;mods/fancymenu_forge_2.12.1_MC_1.19-1.19.1.jar`

## Automatic detection of clientside-only mods

ServerPackCreator can try and automatically determine whether a mod is clientside-only or not. This
feature is enabled by default. In order to deactive/activate it manually,
set `de.griefed.serverpackcreator.serverpack.autodiscoverenabled` to `true` (enable) or `false` (
disable).

Keep in mind: This feature is not 100% foolproof. It can not and will not detect every
clientside-only mod due to the diverse nature of Minecraft mods and the way their creators declare
sideness or dependencies.

If you encounter a mod that did not get caught by the autodetection and is not yet present in the
fallback-list, please create
an [improvement request on GitHub](https://github.com/Griefed/ServerPackCreator/issues/new?assignees=Griefed&labels=enhancement&template=improvement.yml&title=%5BImprovement+request%5D%3A+)
with the title being similar to

> [Improvement request]: Add mod-x, mod-y and mod-z to the fallback-list

In the **What's the feature you would like to be improved?**-section, something along the lines of

> I would like the following mods to be added to the fallback-modslist
> - mod-x (Link to CurseForge project)
> - mod-y (Link to CurseForge project)
> - mod-z (Link to CurseForge project)

## JVM flags/args

Say you've got a couple of extra flags or arguments you want your server to start wich. Maybe
allocate more RAM? Use Aikars flags?

Right next to the Java Args textarea is a button which will set your args
to [Aikars Flags](https://aikar.co/mcflags.html). If you've already entered something into said
textarea, ServerPackCreator will ask you whether you want to overwrite the current settings,
replacing them with Aikars flags.

Using said button will set the textarea to:

> -Xms4G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:
> +UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:
> G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:
> G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90
> -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:
> MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true

These will be saved to the start scripts which ServerPackCreator will create during the server pack
generation.

If you wish to change these flags globally, you will need to edit
your `serverpackcreator.properties`-file and
change `de.griefed.serverpackcreator.configuration.aikar` to your liking.

**Note:** This is not a pack-by-pack configuration, but global. Meaning: Changing this setting and
using the appropriate button in the GUI will result in the same setting for all server pack
configurations.

## Excluding files from ZIP-archives

You can globally configure files which should be excluded from ZIP-archives.
To do so, you need to edit your `serverpackcreator.properties`-file and change
the `de.griefed.serverpackcreator.serverpack.zip.exclude`-property to your liking.

There are some filters you can make use of if you want to, for example, exclude a files which has
the Minecraft version, modloader name, or modloader version in its name.

These filters are to be used in plain text, meaning:

1. `MINECRAFT_VERSION`
2. `MODLOADER`
3. `MODLOADER_VERSION`

If any file you specify contains any of these three strings, they will be replaced respectively.
Example:
`minecraft_server.MINECRAFT_VERSION.jar` becomes `minecraft_server.1.16.5.jar` when generating a
server pack for a modpack which uses Minecraft version 1.16.5.

The default setting
is `minecraft_server.MINECRAFT_VERSION.jar,server.jar,libraries/net/minecraft/server/MINECRAFT_VERSION/server-MINECRAFT_VERSION.jar`
and will exclude the Minecraft server jar, a file called `server.jar`, as well as the Minecraft
server jar should it be present in the `libraries`-folder.

To deactivate the exclusion alltogether,
set `de.griefed.serverpackcreator.serverpack.zip.exclude.enabled` to `false`

## Minecraft's snapshots and pre-releases (3.12.x and up)

If your modloader supports Minecraft snapshots or pre-releases, you can use those, too.

Simply set `de.griefed.serverpackcreator.minecraft.snapshots` in your `serverpackcreator.properties`
to `true`

Then, when using the GUI, you will be able to select any weekly release, snapshot, pre-release and
more, just like that.

## Modes

ServerPackCreator has three main modes in which it can operate or in which you can use it.

- CLI (Commandline Interface)
- GUI (Graphical User Interface)
- Webservice (Provide a webservice through which to generate server packs) **Will be reworked in version 6**

There are a couple more ways to use/run ServerPackCreator which may or may not be helpful for you,
depending on how you plan on using it:

| Argument  | Description                                                                                                                                                                                                             |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-help`   | Print ServerPackCreators help to commandline.                                                                                                                                                                           |
| `-update` | Check whether a newer version of ServerPackCreator is available.                                                                                                                                                        |
| `-cgen`   | Run ServerPackCreators configuration generation.                                                                                                                                                                        |
| `-cli`    | Run ServerPackCreator in commandline-mode. If **no** graphical environment is supported, this is the default ServerPackCreator will enter, even when starting ServerPackCreator with no extra arguments at all.         |
| `-web`    | Run ServerPackCreator as a webservice.                                                                                                                                                                                  |
| `-gui`    | Run ServerPackCreator with our GUI. If a graphical environment is supported, this is the default ServerPackCreator will enter, even when starting ServerPackCreator with no extra arguments at all.                     |
| `--setup` | Set up and prepare the environment for subsequent runs of ServerPackCreator. This will create/copy all files needed for ServerPackCreator to function properly from inside its JAR-file and setup everything else, too. |

Each of these modes has its advantages and disadvantages.

### CLI:

| Advantages                                                                                                                                  | Disadvantages                                                                                                         |
|:--------------------------------------------------------------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------|
| No need for a graphical environment. Can be used on a server to generate a server pack for immediate use.                                   | Gathering of information for a configuration file is tedious.                                                         |
| Step-by-Step generation of a configuration-file with the use of the `-cgen` argument. Generated config will be used immediately afterwards. | No convenience features file folder-browsing or jumping to the generated server pack after generation.                |
|                                                                                                                                             | Debugging in case of a broken/erroring configuration file can be time consuming. Careful reading of logs is required. |
|                                                                                                                                             | Manual editing of the configuration-file in case you want to change it.                                               |

### GUI:

| Advantages                                                                                                             | Disadvantages                     |
|:-----------------------------------------------------------------------------------------------------------------------|:----------------------------------|
| Browse the filesystem for folders and file required by the configuration.                                              | Requires a graphical environment. |
| Setting up a configuration by browsing the filesystem and selecting your Minecraft and modloader versions from a list. |                                   |
| Browsing the generated server pack after generation has finished.                                                      |                                   |
| Loading and saving different configurations for quick generation of multiple server packs in short succession.         |                                   |
| Edit the configuration in the GUI. No manual file-editing required.                                                    |                                   |

### Webservice:

| Advantages                                                                                                                                           | Disadvantages                                             |
|:-----------------------------------------------------------------------------------------------------------------------------------------------------|:----------------------------------------------------------|
| No installation/setup required if a public instance is available somewhere.                                                                          | Requires someone to setup a publicly accessible instance. |
| Voting system to let users indicate whether a generated server pack works.                                                                           | Requires somewhat modern browser versions.                |
| Open REST API. Interaction with the webservice does not *require* a browser. You can request a server pack generation and/or download from the CLI.  | Not all browers may be supported.                         |

## Plugins

ServerPackCreator gives you the ability to use plugins, which can enhance your experience in a
limited amount of ways.

Any given plugin has the chance to do various things:

- Before a server pack is generated
- After a server pack was generated, but BEFORE the ZIP-archive is created
- After a server pack and its ZIP-archive were generated
- Adding extra tabs to the GUI, in which more additional features can then be added
- Adding custom configuration checks
- Adding custom elements to the server pack configuration tab

For a list of plugins, check out
the [ServerPackCreator Addons Overview](https://griefed.github.io/ServerPackCreator-Addons-Overview/#/)

### Installing a plugin

If you have a plugin you would like to enhance your ServerPackCreator-experience with, simply
download it and put it into the `plugins`-directory which resides in the base-directory of
ServerPackCreator.

```
ServerPackCreator
├── lang
├── logs
│   └── archive
├── plugins <--------- 
├── server-packs
│   ├── Survive Create Prosper 4 custom (1)
│   └── World of Dragons II
├── server_files
└── work
    ├── modpacks
    └── temp
```

### Disabling a plugin

In order to deactivate any installed plugins, edit the `disabled.txt`-file in the `plugins`-directory.
If these don't exist, create them.

```
ServerPackCreator
├── lang
├── logs
├── plugins
│   ├── ExamplePlugin-3.0.5.jar
│   └── disabled.txt <------
├── server-packs
├── server_files
└── work
    └── ...
```

Then, find the plugin ID of the installed plugins. This can be mentioned in the pluginss repository, but
can also be found in the pluginss manifest.

`SomePlugin-3.0.5.jar\META-INF\MANIFEST.MF`.
Look for the entry `Plugin-Id:`, for example: `Plugin-Id: example-plugin`

Then, in your `disabled.txt`, add a new line containing `example-plugin`, or rather, whatever ID of
any plugins you want to deactivate:

```
########################################
# - Load all plugins except these.   - #
# - Add one plugin-id per line.      - #
########################################
example-plugin
```

**Important:** If ServerPackCreator was running during the disabling of an plugins, you need to
restart it in order for the plugins to become disabled.

## Configuration

### serverpackcreator.conf

The serverpackcreator.conf file allows you to customize a couple of different things:

| Variable                  | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| modpackDir                | The path to the directory/ZIP-archive where your modpack resides in.                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| clientMods                | List of client-side only mods which are to be deleted from the serverpack. You only need to specify the beginning of the filename up, but excluding, the version number. ServerPackCreator checks whether any of the mods which are copied from the modpack to the serverpack start with any strings in this list and, if there's a match, deletes that file from the serverpack. Check out the [example](https://gist.github.com/Griefed/090cc7c1c2b283daa4b46f6cb85e5e00) for an idea of how it's supposed to look. |
| copyDirs                  | List for directories which are to be copied to the serverpack. If you specify a world from the `saves`-directory, ServerPackCreator will copy the the specified world to the base directory of the serverpack. In other words, `/saves/MyAwesomeWorld` becomes `/MyAwesomeWorld`.                                                                                                                                                                                                                                     |
| includeServerInstallation | Whether to install a Forge/Fabric server for the serverpack. Must be `true` or `false`.                                                                                                                                                                                                                                                                                                                                                                                                                               |
| javaPath                  | Path to the Java Installation. On Linux systems use `which java` to find the location of your Java install. On Windows use `where java` and exclude the `.exe`-part.                                                                                                                                                                                                                                                                                                                                                  |
| minecraftVersion          | The version of Minecraft for which to install the modloader server. The same version of Minecraft your modpack uses.                                                                                                                                                                                                                                                                                                                                                                                                  |
| modLoader                 | Which modloader to install. Must be either "Forge", "NeoForge", "Fabric", "Quilt" or "LegacyFabric". The same modloader your modpack uses.                                                                                                                                                                                                                                                                                                                                                                            |
| modLoaderVersion          | Specific Modloader version to install the server in the serverpack. The same version your modpack uses.                                                                                                                                                                                                                                                                                                                                                                                                               |
| includeServerIcon         | Whether to include server-icon.png in your serverpack. Must be `true` or `false`.                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| includeServerProperties   | Whether to include server.properties in your serverpack. Must be `true` or `false`.                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| includeZipCreation        | Whether to create a zip-file of your serverpack, saved in the directory you specified with `modpackDir`. Must be `true` or `false`.                                                                                                                                                                                                                                                                                                                                                                                   |
| javaArgs                  | JVM flags / Java Args to add to the generated start-scripts. Set to "empty" to not use any in your start-scripts.                                                                                                                                                                                                                                                                                                                                                                                                     |
| serverPackSuffix          | A suffix to append to the name of the server pack directory and server pack ZIP-archive. Illegal characters are / < > : " \ &#124; ? * # % & { } $ ! ' @ + ´ \` = and must not end with a SPACE<code>&#32;&#32;</code> or a DOT<code>&#32;.&#32;&#32;</code>                                                                                                                                                                                                                                                          | |
| serverIconPath            | Path to a custom server-icon.png-file to include in the server pack.                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| serverPropertiesPath      | Path to a custom server.properties-file to include in the server pack.                                                                                                                                                                                                                                                                                                                                                                                                                                                |

### serverpackcreator.properties

```properties
# Supported languages: [en_us, uk_ua, de_de]
de.griefed.serverpackcreator.versioncheck.prerelease=false
de.griefed.serverpackcreator.language=en_gb
de.griefed.serverpackcreator.configuration.fallbackmodslist=3dskinlayers-,Absolutely-Not-A-Zoom-Mod-,AdvancedChat-,AdvancedChatCore-,AdvancedChatHUD-,AdvancedCompas-,Ambience,AmbientEnvironment-,AmbientSounds_,AreYouBlind-,Armor Status HUD-,ArmorSoundTweak-,BH-Menu-,Batty's Coordinates PLUS Mod,BetterAdvancements-,BetterAnimationsCollection-,BetterDarkMode-,BetterF3-,BetterFoliage-,BetterPingDisplay-,BetterPlacement-,BetterTaskbar-,BetterThirdPerson,BetterTitleScreen-,Blur-,BorderlessWindow-,CTM-,ChunkAnimator-,ClientTweaks_,Controller Support-,Controlling-,CraftPresence-,CustomCursorMod-,CustomMainMenu-,DefaultOptions_,DefaultSettings-,DeleteWorldsToTrash-,DetailArmorBar-,Ding-,DistantHorizons-,DripSounds-,Durability101-,DurabilityNotifier-,DynamicSurroundings-,DynamicSurroundingsHuds-,EffectsLeft-,EiraMoticons_,EnchantmentDescriptions-,EnhancedVisuals_,EquipmentCompare-,FPS-Monitor-,FabricCustomCursorMod-,Fallingleaves-,FancySpawnEggs,FancyVideo-API-,FirstPersonMod,FogTweaker-,ForgeCustomCursorMod-,FpsReducer-,FpsReducer2-,FullscreenWindowed-,GameMenuModOption-,HealthOverlay-,HorseStatsMod-,InventoryEssentials_,InventoryHud_[1.17.1].forge-,InventorySpam-,InventoryTweaks-,ItemBorders-,ItemPhysicLite_,ItemStitchingFix-,JBRA-Client-,JustEnoughCalculation-,JustEnoughEffects-,JustEnoughProfessions-,JustEnoughResources-,LLOverlayReloaded-,LOTRDRP-,LegendaryTooltips,LegendaryTooltips-,LightOverlay-,MoBends,MouseTweaks-,MyServerIsCompatible-,Neat ,Neat-,NekosEnchantedBooks-,NoAutoJump-,NoFog-,Notes-,NotifMod-,OldJavaWarning-,OptiFine,OptiFine_,OptiForge,OptiForge-,PackMenu-,PackModeMenu-,PickUpNotifier-,Ping-,PresenceFootsteps-,RPG-HUD-,ReAuth-,ResourceLoader-,ResourcePackOrganizer,ShoulderSurfing-,ShulkerTooltip-,SimpleDiscordRichPresence-,SimpleWorldTimer-,SoundFilters-,SpawnerFix-,TRansliterationLib-,TipTheScales-,Tips-,Toast Control-,Toast-Control-,ToastControl-,TravelersTitles-,VoidFog-,WindowedFullscreen-,WorldNameRandomizer-,[1.12.2]DamageIndicatorsMod-,[1.12.2]bspkrscore-,antighost-,anviltooltipmod-,appleskin-,armorchroma-,armorpointspp-,auditory-,authme-,auto-reconnect-,autojoin-,autoreconnect-,axolotl-item-fix-,backtools-,bannerunlimited-,beenfo-1.19-,better-recipe-book-,betterbiomeblend-,bhmenu-,blur-,borderless-mining-,catalogue-,charmonium-,chat_heads-,cherishedworlds-,cirback-1.0-,classicbar-,clickadv-,clienttweaks-,combat_music-,configured-,controllable-,cullleaves-,cullparticles-,custom-crosshair-mod-,customdiscordrpc-,darkness-,dashloader-,defaultoptions-,desiredservers-,discordrpc-,drippyloadingscreen-,drippyloadingscreen_,dynamic-fps-,dynamic-music-,dynamiclights-,dynmus-,effective-,eggtab-,eguilib-,eiramoticons-,enchantment-lore-,entity-texture-features-,entityculling-,exhaustedstamina-,extremesoundmuffler-,fabricemotes-,fancymenu_,fancymenu_video_extension,findme-,flickerfix-,fm_audio_extension_,forgemod_VoxelMap-,freelook-,galacticraft-rpc-,gamestagesviewer-,grid-,helium-,hiddenrecipebook_,infinitemusic-,inventoryprofiles,invtweaks-,itemzoom,itlt-,jeed-,jehc-,jeiintegration_,just-enough-harvestcraft-,justenoughbeacons-,justenoughdrags-,justzoom_,keymap-,keywizard-,konkrete_,konkrete_forge_,lazydfu-,light-overlay-,lightfallclient-,loadmyresources_,lock_minecart_view-,lootbeams-,lwl-,magnesium_extras-,maptooltip-,massunbind,mcbindtype-,mcwifipnp-,medievalmusic-,mightyarchitect-,mindful-eating-,minetogether-,mobplusplus-,modcredits-,modernworldcreation_,modmenu-,modnametooltip-,modnametooltip_,moreoverlays-,mousewheelie-,movement-vision-,multihotbar-,music-duration-reducer-,musicdr-,neiRecipeHandlers-,ngrok-lan-expose-mod-,nopotionshift_,notenoughanimations-,oculus-,ornaments-,overloadedarmorbar-,panorama-,paperdoll-,phosphor-,preciseblockplacing-,realm-of-lost-souls-,rebrand-,replanter-,rubidium-,rubidium_extras-,screenshot-to-clipboard-,shutupexperimentalsettings-,shutupmodelloader-,signtools-,simple-rpc-,simpleautorun-,smartcursor-,smoothboot-,smoothfocus-,sounddeviceoptions-,soundreloader-,spoticraft-,tconplanner-,timestamps-,tooltipscroller-,torchoptimizer-,torohealth-,totaldarkness,toughnessbar-,wisla-,xlifeheartcolors-,yisthereautojump-
de.griefed.serverpackcreator.configuration.fallbackmodslist.regex=^3dskinlayers-.*$,^Absolutely-Not-A-Zoom-Mod-.*$,^AdvancedChat-.*$,^AdvancedChatCore-.*$,^AdvancedChatHUD-.*$,^AdvancedCompas-.*$,^Ambience.*$,^AmbientEnvironment-.*$,^AmbientSounds_.*$,^AreYouBlind-.*$,^Armor Status HUD-.*$,^ArmorSoundTweak-.*$,^BH-Menu-.*$,^Batty's Coordinates PLUS Mod.*$,^BetterAdvancements-.*$,^BetterAnimationsCollection-.*$,^BetterDarkMode-.*$,^BetterF3-.*$,^BetterFoliage-.*$,^BetterPingDisplay-.*$,^BetterPlacement-.*$,^BetterTaskbar-.*$,^BetterThirdPerson.*$,^BetterTitleScreen-.*$,^Blur-.*$,^BorderlessWindow-.*$,^CTM-.*$,^ChunkAnimator-.*$,^ClientTweaks_.*$,^Controller Support-.*$,^Controlling-.*$,^CraftPresence-.*$,^CustomCursorMod-.*$,^CustomMainMenu-.*$,^DefaultOptions_.*$,^DefaultSettings-.*$,^DeleteWorldsToTrash-.*$,^DetailArmorBar-.*$,^Ding-.*$,^DistantHorizons-.*$,^DripSounds-.*$,^Durability101-.*$,^DurabilityNotifier-.*$,^DynamicSurroundings-.*$,^DynamicSurroundingsHuds-.*$,^EffectsLeft-.*$,^EiraMoticons_.*$,^EnchantmentDescriptions-.*$,^EnhancedVisuals_.*$,^EquipmentCompare-.*$,^FPS-Monitor-.*$,^FabricCustomCursorMod-.*$,^Fallingleaves-.*$,^FancySpawnEggs.*$,^FancyVideo-API-.*$,^FirstPersonMod.*$,^FogTweaker-.*$,^ForgeCustomCursorMod-.*$,^FpsReducer-.*$,^FpsReducer2-.*$,^FullscreenWindowed-.*$,^GameMenuModOption-.*$,^HealthOverlay-.*$,^HorseStatsMod-.*$,^InventoryEssentials_.*$,^InventoryHud_[1.17.1].forge-.*$,^InventorySpam-.*$,^InventoryTweaks-.*$,^ItemBorders-.*$,^ItemPhysicLite_.*$,^ItemStitchingFix-.*$,^JBRA-Client-.*$,^JustEnoughCalculation-.*$,^JustEnoughEffects-.*$,^JustEnoughProfessions-.*$,^JustEnoughResources-.*$,^LLOverlayReloaded-.*$,^LOTRDRP-.*$,^LegendaryTooltips-.*$,^LegendaryTooltips.*$,^LightOverlay-.*$,^MoBends.*$,^MouseTweaks-.*$,^MyServerIsCompatible-.*$,^Neat .*$,^Neat-.*$,^NekosEnchantedBooks-.*$,^NoAutoJump-.*$,^NoFog-.*$,^Notes-.*$,^NotifMod-.*$,^OldJavaWarning-.*$,^OptiFine.*$,^OptiFine_.*$,^OptiForge-.*$,^OptiForge.*$,^PackMenu-.*$,^PackModeMenu-.*$,^PickUpNotifier-.*$,^Ping-.*$,^PresenceFootsteps-.*$,^RPG-HUD-.*$,^ReAuth-.*$,^ResourceLoader-.*$,^ResourcePackOrganizer.*$,^ShoulderSurfing-.*$,^ShulkerTooltip-.*$,^SimpleDiscordRichPresence-.*$,^SimpleWorldTimer-.*$,^SoundFilters-.*$,^SpawnerFix-.*$,^TRansliterationLib-.*$,^TipTheScales-.*$,^Tips-.*$,^Toast Control-.*$,^Toast-Control-.*$,^ToastControl-.*$,^TravelersTitles-.*$,^VoidFog-.*$,^WindowedFullscreen-.*$,^WorldNameRandomizer-.*$,^[1.12.2]DamageIndicatorsMod-.*$,^[1.12.2]bspkrscore-.*$,^antighost-.*$,^anviltooltipmod-.*$,^appleskin-.*$,^armorchroma-.*$,^armorpointspp-.*$,^auditory-.*$,^authme-.*$,^auto-reconnect-.*$,^autojoin-.*$,^autoreconnect-.*$,^axolotl-item-fix-.*$,^backtools-.*$,^bannerunlimited-.*$,^beenfo-1.19-.*$,^better-recipe-book-.*$,^betterbiomeblend-.*$,^bhmenu-.*$,^blur-.*$,^borderless-mining-.*$,^catalogue-.*$,^charmonium-.*$,^chat_heads-.*$,^cherishedworlds-.*$,^cirback-1.0-.*$,^classicbar-.*$,^clickadv-.*$,^clienttweaks-.*$,^combat_music-.*$,^configured-.*$,^controllable-.*$,^cullleaves-.*$,^cullparticles-.*$,^custom-crosshair-mod-.*$,^customdiscordrpc-.*$,^darkness-.*$,^dashloader-.*$,^defaultoptions-.*$,^desiredservers-.*$,^discordrpc-.*$,^drippyloadingscreen-.*$,^drippyloadingscreen_.*$,^dynamic-fps-.*$,^dynamic-music-.*$,^dynamiclights-.*$,^dynmus-.*$,^effective-.*$,^eggtab-.*$,^eguilib-.*$,^eiramoticons-.*$,^enchantment-lore-.*$,^entity-texture-features-.*$,^entityculling-.*$,^exhaustedstamina-.*$,^extremesoundmuffler-.*$,^fabricemotes-.*$,^fancymenu_.*$,^fancymenu_video_extension.*$,^findme-.*$,^flickerfix-.*$,^fm_audio_extension_.*$,^forgemod_VoxelMap-.*$,^freelook-.*$,^galacticraft-rpc-.*$,^gamestagesviewer-.*$,^grid-.*$,^helium-.*$,^hiddenrecipebook_.*$,^infinitemusic-.*$,^inventoryprofiles.*$,^invtweaks-.*$,^itemzoom.*$,^itlt-.*$,^jeed-.*$,^jehc-.*$,^jeiintegration_.*$,^just-enough-harvestcraft-.*$,^justenoughbeacons-.*$,^justenoughdrags-.*$,^justzoom_.*$,^keymap-.*$,^keywizard-.*$,^konkrete_.*$,^konkrete_forge_.*$,^lazydfu-.*$,^light-overlay-.*$,^lightfallclient-.*$,^loadmyresources_.*$,^lock_minecart_view-.*$,^lootbeams-.*$,^lwl-.*$,^magnesium_extras-.*$,^maptooltip-.*$,^massunbind.*$,^mcbindtype-.*$,^mcwifipnp-.*$,^medievalmusic-.*$,^mightyarchitect-.*$,^mindful-eating-.*$,^minetogether-.*$,^mobplusplus-.*$,^modcredits-.*$,^modernworldcreation_.*$,^modmenu-.*$,^modnametooltip-.*$,^modnametooltip_.*$,^moreoverlays-.*$,^mousewheelie-.*$,^movement-vision-.*$,^multihotbar-.*$,^music-duration-reducer-.*$,^musicdr-.*$,^neiRecipeHandlers-.*$,^ngrok-lan-expose-mod-.*$,^nopotionshift_.*$,^notenoughanimations-.*$,^oculus-.*$,^ornaments-.*$,^overloadedarmorbar-.*$,^panorama-.*$,^paperdoll-.*$,^phosphor-.*$,^preciseblockplacing-.*$,^realm-of-lost-souls-.*$,^rebrand-.*$,^replanter-.*$,^rubidium-.*$,^rubidium_extras-.*$,^screenshot-to-clipboard-.*$,^shutupexperimentalsettings-.*$,^shutupmodelloader-.*$,^signtools-.*$,^simple-rpc-.*$,^simpleautorun-.*$,^smartcursor-.*$,^smoothboot-.*$,^smoothfocus-.*$,^sounddeviceoptions-.*$,^soundreloader-.*$,^spoticraft-.*$,^tconplanner-.*$,^timestamps-.*$,^tooltipscroller-.*$,^torchoptimizer-.*$,^torohealth-.*$,^totaldarkness.*$,^toughnessbar-.*$,^wisla-.*$,^xlifeheartcolors-.*$,^yisthereautojump-.*$
de.griefed.serverpackcreator.configuration.hastebinserver=https://haste.zneix.eu/documents
de.griefed.serverpackcreator.configuration.aikar=-Xms4G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true
de.griefed.serverpackcreator.serverpack.autodiscovery.enabled=true
de.griefed.serverpackcreator.gui.darkmode=true
de.griefed.serverpackcreator.configuration.directories.serverpacks=
de.griefed.serverpackcreator.serverpack.cleanup.enabled=true
de.griefed.serverpackcreator.serverpack.overwrite.enabled=true
de.griefed.serverpackcreator.configuration.directories.shouldexclude=animation,asm,cache,changelogs,craftpresence,crash-reports,downloads,icons,libraries,local,logs,overrides,packmenu,profileImage,profileImage,resourcepacks,screenshots,server_pack,shaderpacks,simple-rpc,tv-cache
de.griefed.serverpackcreator.spring.schedules.database.cleanup=0 0 0 * * *
de.griefed.serverpackcreator.spring.schedules.files.cleanup=0 0 0 * * *
de.griefed.serverpackcreator.spring.schedules.versions.refresh=0 0 0 * * *
de.griefed.serverpackcreator.spring.artemis.queue.max_disk_usage=90
de.griefed.serverpackcreator.configuration.saveloadedconfig=false
de.griefed.serverpackcreator.configuration.directories.mustinclude=addonpacks,blueprints,config,configs,customnpcs,defaultconfigs,global_data_packs,global_packs,kubejs,maps,mods,openloader,scripts,shrines-saves,structures,structurize,worldshape,Zoestria
de.griefed.serverpackcreator.serverpack.zip.exclude=minecraft_server.MINECRAFT_VERSION.jar,server.jar,libraries/net/minecraft/server/MINECRAFT_VERSION/server-MINECRAFT_VERSION.jar
de.griefed.serverpackcreator.serverpack.zip.exclude.enabled=true
de.griefed.serverpackcreator.serverpack.script.template=default_template.ps1,default_template.sh
de.griefed.serverpackcreator.minecraft.snapshots=false
de.griefed.serverpackcreator.serverpack.autodiscovery.filter=START
de.griefed.serverpackcreator.java=
de.griefed.serverpackcreator.script.java.autoupdate=true
```

| Property                                                             | Description                                                                                                                                                                            |
|----------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| de.griefed.serverpackcreator.versioncheck.prerelease                 | `true` or `false`. Whether to check for available pre-releases, too, when checking for updates.                                                                                        |
| de.griefed.serverpackcreator.language                                | The language in which ServerPackCreator should run.                                                                                                                                    |
| de.griefed.serverpackcreator.configuration.fallbackmodslist          | Comma separated fallback-list of clientside-only mods. Do not edit this manually.                                                                                                      |
| de.griefed.serverpackcreator.configuration.fallbackmodslist.regex    | Comma separated fallback-list of clientside-only mods in regex format. Do not edit this manually.                                                                                      |
| de.griefed.serverpackcreator.configuration.hastebinserver            | Address of a HasteBin server to use for config and logs uploading.                                                                                                                     |
| de.griefed.serverpackcreator.configuration.aikar                     | Aikars commonly used Minecraft server JVM flags to potentially increase the performance of your Minecraft server.                                                                      |
| de.griefed.serverpackcreator.serverpack.autodiscovery.enabled        | `true` or `false`. Whether to try and determine sideness of mods in a modpack automatically and exclude them if they are clientside-only. Set this to `false` to disable it.           |
| de.griefed.serverpackcreator.gui.darkmode                            | GUI-only. `true` or `false`. Whether to use dark-mode in the GUI.                                                                                                                      |
| de.griefed.serverpackcreator.configuration.directories.serverpacks   | The directory in which server packs will be generated and stored in.                                                                                                                   |
| de.griefed.serverpackcreator.serverpack.cleanup.enabled              | `true` or `false`. Whether to cleanup files after generating a server pack.                                                                                                            |
| de.griefed.serverpackcreator.serverpack.overwrite.enabled            | `true` or `false`. Whether an already existing server pack should be overwritten.                                                                                                      |
| de.griefed.serverpackcreator.configuration.directories.shouldexclude | List of directories which should not be in a server pack.                                                                                                                              |
| de.griefed.serverpackcreator.spring.schedules.database.cleanup       | Web-only. Cron-schedule at which checks are run and server packs cleaned up.                                                                                                           |
| de.griefed.serverpackcreator.spring.schedules.files.cleanup          | Web-only. Cron-schedule at which checks are run and files from server pack generations are cleaned up.                                                                                 |
| de.griefed.serverpackcreator.spring.schedules.versions.refresh       | Web-only. Cron-schedule at which the available Minecraft, Forge and Fabric versions are refreshed.                                                                                     |
| de.griefed.serverpackcreator.spring.artemis.queue.max_disk_usage     | Web-only. Maximum disk usage in percent at which no new tasks are accepted, preventing the generation of new server packs.                                                             |
| de.griefed.serverpackcreator.configuration.saveloadedconfig          | GUI-only. `true` or `false`. Whether to overwrite the last manually loaded configuration file, too.                                                                                    |
| de.griefed.serverpackcreator.configuration.directories.mustinclude   | List of directories which must be included in a server pack.                                                                                                                           |
| de.griefed.serverpackcreator.serverpack.zip.exclude                  | Files to exclude from the server pack ZIP-archive. You may use `MINECRAFT_VERSION` as a placeholder for the Minecraft version of your modpack/server pack if a files name contains it. | 
| de.griefed.serverpackcreator.serverpack.zip.exclude.enabled          | `true` or `false`. Whether exclusion of files from a server packs ZIP-archive is enabled.                                                                                              | 
| de.griefed.serverpackcreator.serverpack.script.template              | Start-script template to use for server start script creation.                                                                                                                         | 
| de.griefed.serverpackcreator.minecraft.snapshots                     | Whether Minecraft snapshot versions should be available to the user in the GUI.                                                                                                        | 
| de.griefed.serverpackcreator.serverpack.autodiscovery.filter         | Filter method by which to exclude user-specified clientside-only mods. `START`, `END`, `REGEX`, `CONTAIN`, `EITHER`                                                                    |

`application.properties`

```properties
server.port=8080
server.error.whitelabel.enabled=false
server.tomcat.basedir=.
server.tomcat.accesslog.enabled=true
server.tomcat.accesslog.directory=logs
server.tomcat.accesslog.file-date-format=_yyyy-MM-dd
server.tomcat.accesslog.prefix=tomcat_access
server.tomcat.accesslog.suffix=.log
server.tomcat.accesslog.pattern=common
spring.output.ansi.enabled=ALWAYS
server.error.include-message=ALWAYS
server.error.include-stacktrace=ALWAYS
spring.application.name=ServerPackCreator
spring.datasource.url=jdbc:sqlite:serverpackcreator.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.sqlite.hibernate.dialect.SQLiteDialect
spring.jpa.open-in-view=false
spring.jdbc.template.query-timeout=3600
spring.transaction.default-timeout=3600
spring.artemis.mode=embedded
spring.artemis.embedded.persistent=true
spring.artemis.embedded.data-directory=work/artemis
spring.datasource.tomcat.max-active=50
spring.datasource.tomcat.max-idle=15
spring.datasource.tomcat.min-idle=8
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB
spring.config.import=classpath:application.properties,classpath:serverpackcreator.properties,optional:file:./serverpackcreator.properties
```

If a property above has no description in the table below, it is not to be touched. Changing any of
these can break ServerPackCreator or cause otherwise unwanted behaviour.
Changes to properties not described below are not supported. If you changed a property you were not
meant to fiddle around with, do not report an issue. Such issues will be closed without further
comment.

| Property                                  | Description                                                                                            |
|-------------------------------------------|--------------------------------------------------------------------------------------------------------|
| server.port                               | The port at which ServerPackCreator as a webservice will be available at.                              |
| spring.datasource.url                     | Path to and name of the SQLite database which the webservice will use.                                 |
| spring.artemis.embedded.data-directory    | The path to and name of the directory in which Artemis will store queues and task related information. |
| spring.servlet.multipart.max-file-size    | Maximum filesize for uploads.                                                                          |
| spring.servlet.multipart.max-request-size | Maximum request size for file uploads.                                                                 |
| spring.config.import                      | Additional properties-file to import properties from.                                                  |

## Localization

If you wish to run ServerPackCreator with your locale (if it is already supported), you can either:

1. Run `java -jar ServerPackCreator-X.X.X.jar -lang your_locale` for
   example `java -jar ServerPackCreator-X.X.X.jar -lang en_GB`. This will create the
   lang.properties-file with your specified locale.
2. Running `java -jar ServerPackCreator-x.x.x.jar` without `-lang en_GB` or any other language will
   set the locale to en_us by default and create the lang.properties-file.
3. Modify the `serverpackcreator.properties`-file in the same directory as
   ServerPackCreator-X-X-X.jar and set your locale like this `lang=your_locale` for
   example `lang=en_GB`

If you want to contribute translations to ServerPackCreator, check out the [i18n](serverpackcreator-api/src/commonMain/kotlin/de/griefed/serverpackcreator/api/i18n)-directory containing already available
translations to see how they are set up. Then, make your changes, test them, and open a pull request on GitHub according
to the [Contribution-Guidelines](CONTRIBUTING.md).

### Adding a translation

Say you want to translate ServerPackCreator to german. You would need to add the file `Gui_en_GB.properties`

In the english properties, you will see entries like

```properties
menubar.gui.menu.file=File
menubar.gui.menu.edit=Edit
menubar.gui.menu.view=View
menubar.gui.menu.about=About
menubar.gui.menu.help=Help
```

So, in order to translate them to german, in your `Gui_en_GB.properties`-file, you would add

```properties
menubar.gui.menu.file=Datei
menubar.gui.menu.edit=Bearbeiten
menubar.gui.menu.view=Ansicht
menubar.gui.menu.about=Info
menubar.gui.menu.help=Hilfe
```

Then, either in your `serverpackcreator.properties`
set `de.griefed.serverpackcreator.language=de_DE` or launch ServerPackCreator with the
argument `-lang=de_DE`.

Voila! The menubar will now have german translations!

Keep in mind when using languages other than `en_GB`: Any key not found in your currently set
language will fall back to the english default.
