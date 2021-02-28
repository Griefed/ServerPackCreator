[![banner](img/serverpackcreator_banner.png)](https://github.com/Griefed/ServerPackCreator)

[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/Griefed/ServerPackCreator/Java%20CI%20with%20Gradle?label=Gradle%20Build%20Test&logo=GitHub&style=for-the-badge&color=c0ffee&labelColor=325358)](https://github.com/Griefed/ServerPackCreator/actions?query=workflow%3A%22Java+CI+with+Gradle%22)
[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/Griefed/ServerPackCreator/Tag%20&%20Release%20&%20Publish?style=for-the-badge&logo=GitHub&color=c0ffee&labelColor=325358&label=Snag%20It%20Bag%20It%20Tag%20It&)](https://github.com/Griefed/ServerPackCreator/actions?query=workflow%3A%22Tag+%26+Release+%26+Publish%22)
[![GitHub release (latest by date including pre-releases)](https://img.shields.io/github/v/release/Griefed/ServerPackCreator?include_prereleases&label=Latest%20Release&logo=Github&style=for-the-badge&color=c0ffee&labelColor=325358)](https://github.com/Griefed?tab=packages&repo_name=ServerPackCreator)
[![GitHub](https://img.shields.io/github/license/Griefed/ServerPackCreator?logo=GitHub&style=for-the-badge&color=c0ffee&labelColor=325358)](https://github.com/Griefed/ServerPackCreator/blob/main/LICENSE)

**ServerPackCreator is a program which aims to create a serverpack for any given modpack, making modpack-developing, or at least a part of it, a little less time-consuming. You customize the configuration to your liking and off you go.**
**Whenever you are working on an update to your modpack, you simply run ServerPackCreator and BAM! You've got yourself a serverpack for your new modpack version.**

I am making this in my spare time, therefore progress is slow. I also just started getting into Java programming, so expect the code to be of...questionable...quality.
If you're a Java Pro and your eyes bulge when looking at my code, by all means, you're welcome to help me improve it in any way, shape, or form.
You're also welcome to make contributions, or fork it and make your own version of it. It's MIT license for a reason. Do what you want with it.

## WIP!

Use at your own risk! Be aware that data loss is possible. If you wish to test this, there's a description down below.
If you wish to contribute, fork the repository, make your changes, create a pull request and make sure to use [conventional commits](https://github.com/Griefed/ServerPackCreator/wiki)

## Features

Planned/wanted:

- [X] **Specify whether to use Forge or Fabric**
  - [X] **Download and install Forge server** (needs additional testing)
    - [X] **Make version configurable** (needs additional testing)
  - [X] **Download and install Fabric server** (needs additional testing) 
    - [X] **Make version configurable** (needs additional testing)
- [X] Logging
  - [X] log actions to action.log
  - [X] log errors to error.log
- [X] **Create default start scripts for Linux and Windows systems**
  - [X] **Make inclusion of start scripts optional**
  - [X] **Provide default/example**
    - [X] **Forge**
    - [X] **Fabric**
- [X] **On first run, generate all default files, tell user to customize and run again, then exit.**
- [X] **Delete client side mods**
  - [X] **Make list of client side mods configurable**
- [X] **Allow specifying directories to include in serverpack**
  - [X] **Remove prefix "saves" when adding a world from a modpack**
- [X] **Copy server-icon.png to "server_pack" directory**
  - [X] **Make inclusion of server-icon.png optional**
  - [X] **Provide default**
- [X] **Create a server.properties file in "server_pack" directory**
  - [X] **Make inclusion of server.properties optional**
  - [X] **Provide default**
- [X] **Automatically create zip-archive of the created serverpack**
  - [X] **Make creation of archive optional**
  
## Running ServerPackCreator

The initial run of ServerPackCreator will place default-files in a directory called `server_files` in the same directory where the .jar-file resides in.
The config file will be created in the same directory as `ServerPackCreator-x.x.x.jar`.
Among those default files are:

File | Description
---- | ----
creator.conf | Configuration file for customization. See [creator.conf](https://github.com/Griefed/ServerPackCreator/blob/main/src/main/resources/creator.conf).
server.properties | Configuration file for the Minecraft server. See [server.properties](https://github.com/Griefed/ServerPackCreator/blob/main/src/main/resources/server_files/server.properties). 
server-icon.png | Icon which is displayed in the server browser in Minecraft. See [server-icon.png](https://github.com/Griefed/ServerPackCreator/blob/main/src/main/resources/server_files/server-icon.png).
start-fabric.bat | Fabric server start script for windows systems. See [start-fabric.bat](https://github.com/Griefed/ServerPackCreator/blob/main/src/main/resources/server_files/start-fabric.bat).
start-fabric.sh | Fabric server start script for linux systems. See [start-fabric.sh](https://github.com/Griefed/ServerPackCreator/blob/main/src/main/resources/server_files/start-fabric.sh).
start-forge.bat | Forge server start script for windows systems. See [start-forge.bat](https://github.com/Griefed/ServerPackCreator/blob/main/src/main/resources/server_files/start-forge.bat).
start-forge.sh | Forge server start script for linux systems. See [start-forge.sh](https://github.com/Griefed/ServerPackCreator/blob/main/src/main/resources/server_files/start-forge.sh).

The creator.conf file allows you to customize a couple of different things:

Variable | Description
-------- | -----------
modpackDir | The path to the directory where your modpack resides in.
clientMods | List of client-side only mods which are to be deleted from the serverpack. You only need to specify the beginning of the filename up, but excluding, the version number. ServerPackCreator checks whether any of the mods which are copied from the modpack to the serverpack start with any strings in this list and, if there's a match, deletes that file from the serverpack.
copyDirs | List for directories which are to be copied to the serverpack. If you specify a world from the `saves`-directory, ServerPackCreator will copy the the specified world to the base directory of the serverpack. In other words, `/saves/MyAwesomeWorld` becomes `/MyAwesomeWorld`. 
includeServerInstallation | Whether to install a Forge/Fabric server for the serverpack. Must be `true` or `false`.
javaPath | Path to the Java Installation.
minecraftVersion | The version of Minecraft for which to install the modloader server.
modLoader | Which modloader to install. Must be either "Forge" or "Fabric".
modLoaderVersion | Specific Modloader version to install the server in the serverpack.
includeServerIcon | Whether to include server-icon.png in your serverpack. Must be `true` or `false`.
includeServerProperties | Whether to include server.properties in your serverpack. Must be `true` or `false`.
includeStartScripts | Whether to inlude start scripts in your serverpack. Must be `true` or `false`.
includeZipCreation | Whether to create a zip-file of your serverpack, saved in the directory you specified with `modpackDir`. Must be `true` or `false`.

After checking the configuration, run ServerPackCreator again, and it'll do it's magic.

## Testing

If you want to help testing (I can not be held responsible for loss of data. Make sure you make backups and execute this program in a test environment.):
1. Download `ServerPackCreator-x.x.x.jar` from `Packages` or `Releases` on the right side.
2. Put it in the parent folder of your modpack folder. Here's an example for my modpack Survive Create Prosper 4:
```console
tests/
│   creator.conf
│   ServerPackCreator-X.X.X.jar
│
├───logs
│       action.log
│       error.log
│
├───server_files
│       server-icon.png
│       server.properties
│       start-fabric.bat
│       start-fabric.sh
│       start-forge.bat
│       start-forge.sh
│
└───Survive Create Prosper 4 1.16.5
    ├───.mixin.out
    ├───config
    ├───defaultconfigs
    ├───downloads
    ├───local
    ├───mods
    ├───mod_data
    ├───packmenu
    ├───paintings
    ├───patchouli_books
    ├───resourcepacks
    ├───saves
    │   └───scp4
    ├───schematics
    ├───screenshots
    ├───scripts
    └───seeds
```
3. Customize the configuration file `creator.conf` to work with your modpack.
   (truncated for better readability)
```editor-config
modpackDir = "./Survive Create Prosper 4 1.16.5"

clientMods = [
    "AmbientSounds",
    "BackTools",
    "BetterAdvancement",
    "BetterPing",
    "cherished",
    "ClientTweaks",
    "Controlling",
    "DefaultOptions",
    "durability",
    "DynamicSurroundings",
    "itemzoom",
    "jei-professions",
    "jeiintegration",
    "JustEnoughResources",
    "MouseTweaks",
    "Neat",
    "OldJavaWarning",
    "PackMenu",
    "preciseblockplacing",
    "SimpleDiscordRichPresence",
    "SpawnerFix",
    "TipTheScales",
    "WorldNameRandomizer"
    ]

copyDirs = [
    "config",
    "defaultconfigs",
    "mods",
    "scripts",
    "seeds",
    "saves/scp4"
    ]

includeServerInstallation = true
javaPath = "C:\\Program Files\\Java\\jdk1.8.0_271\\bin\\java"
minecraftVersion = "1.16.5"
modLoader= "Forge"
modLoaderVersion = "36.0.15"

includeServerIcon = true

includeServerProperties = true

includeStartScripts = true

includeZipCreation = true
```

An example configuration to use Fabric for your serverpack:
```editor-config
modLoader= "Fabric"
modLoaderVersion = "0.11.2"
```

4. Customize any of the other files if need be. 

5. Execute the program from the CLI with `java -jar ServerPackCreator-X.X.X.jar` where `X.X.X`is to be replaced with the version you downloaded.
   
6. ServerPackCreator should now be telling you what it is currently doing. You should also now see a `server_pack`-folder in the directory of your modpack.

7. Check these directories whether they contain all the files which are expected to be there.
