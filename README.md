# ServerPackCreator

[![banner](img/serverpackcreator_banner.png)](https://github.com/Griefed/ServerPackCreator)

[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/Griefed/ServerPackCreator/Java%20CI%20with%20Gradle?label=Gradle%20Build%20Test&logo=GitHub&style=for-the-badge&color=c0ffee&labelColor=325358)](https://github.com/Griefed/ServerPackCreator/actions?query=workflow%3A%22Java+CI+with+Gradle%22)
[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/Griefed/ServerPackCreator/Tag%20&%20Release%20&%20Publish?style=for-the-badge&logo=GitHub&color=c0ffee&labelColor=325358&label=Snag%20It%20Bag%20It%20Tag%20It&)](https://github.com/Griefed/ServerPackCreator/actions?query=workflow%3A%22Tag+%26+Release+%26+Publish%22)
[![GitHub release (latest by date including pre-releases)](https://img.shields.io/github/v/release/Griefed/ServerPackCreator?include_prereleases&label=Latest%20Release&logo=Github&style=for-the-badge&color=c0ffee&labelColor=325358)](https://github.com/Griefed?tab=packages&repo_name=ServerPackCreator)
[![GitHub](https://img.shields.io/github/license/Griefed/ServerPackCreator?logo=GitHub&style=for-the-badge&color=c0ffee&labelColor=325358)](https://github.com/Griefed/ServerPackCreator/blob/main/LICENSE)

### ServerPackCreator is a program which aims to create a serverpack for any given modpack, making modpack-developing, or at least a part of it, a little less time-consuming. You customize the configuration to your liking and off you go. Whenever you are working on an update to your modpack, you simply run ServerPackCreator and BAM! You've got yourself a serverpack for your new modpack version.

I am making this in my spare time, therefor progress is slow. I also just started getting into Java programming, so expect the code to be of...questionable...quality. If you're a Java Pro and your eyes bulge when looking at my code, by all means, you're welcome to help me improve it in any way, shape, or form. 
You're also welcome to make contributions, or fork it and make your own version of it. It's MIT license for a reason. Do what you want with it.

## WIP!

Use at your own risk! Be aware that data loss is possible. If you wish to test this, there's a description down below.
If you wish to contribute, fork the repository, make your changes, create a pull request and make sure to use [conventional commits](https://github.com/Griefed/ServerPackCreator/wiki)

Help for
- setting up Forge downloading
  - installing Forge server after successful download
- setting up Fabric downloading
  - installing Fabric server after successful download

would be greatly appreciated.

## Features

Planned/wanted:

- [X] **Specify whether to use Forge or Fabric**
  - [ ] **Download and install Forge server**
    - [ ] **Make version configurable**
  - [ ] **Download and install Fabric server**
    - [ ] **Make version configurable**
- [ ] Logging
  - [ ] log actions to action.log
  - [ ] log errors to error.log
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

The initial run of ServerPackCreator will place default files in the same directory where the .jar-file resides in. Among those default files are:

File | Description
---- | ----
creator.conf | Configuration file for customization. See [creator.conf](https://github.com/Griefed/ServerPackCreator/blob/main/src/main/resources/creator.conf).
server.properties | Configuration file for the Minecraft server. See [server.properties](https://github.com/Griefed/ServerPackCreator/blob/main/src/main/resources/server.properties). 
server-icon.png | Icon which is displayed in the server browser in Minecraft. See [server-icon.png](https://github.com/Griefed/ServerPackCreator/blob/main/src/main/resources/server-icon.png). 
start.bat | Server start script for windows systems. See [start.bat](https://github.com/Griefed/ServerPackCreator/blob/main/src/main/resources/start.bat).
start.sh | Server start script for linux systems. See [start.sh](https://github.com/Griefed/ServerPackCreator/blob/main/src/main/resources/start.sh).

The creator.conf file allows you to customize a couple of different things:

Variable | Default values | Description
-------- | ------- | -----------
modpackDir | "./Survive Create Prosper 4 1.16.5" | The path to the directory where your modpack resides in.
clientMods | ["AmbientSounds",<br/>"BackTools",<br/>"BetterAdvancement",<br/>"BetterPing",<br/>"cherished",<br/>"ClientTweaks",<br/>"Controlling",<br/>"DefaultOptions",<br/>"durability",<br/>"DynamicSurroundings",<br/>"itemzoom",<br/>"jei-professions",<br/>"jeiintegration",<br/>"JustEnoughResources",<br/>"MouseTweaks",<br/>"Neat",<br/>"OldJavaWarning",<br/>"PackMenu",<br/>"preciseblockplacing",<br/>"SimpleDiscordRichPresence",<br/>"SpawnerFix",<br/>"TipTheScales",<br/>"WorldNameRandomizer"] | List of client-side only mods which are to be deleted from the serverpack. You only need to specify the beginning of the filename up, but excluding, the version number. ServerPackCreator checks whether any of the mods which are copied from the modpack to the serverpack start with any strings in this list and, if there's a match, deletes that file from the serverpack.
copyDirs | ["config",<br/>"defaultconfigs",<br/>"mods",<br/>"scripts",<br/>"seeds",<br/>"saves/scp4"] | List for directories which are to be copied to the serverpack. If you specify a world from the `saves`-directory, ServerPackCreator will copy the the specified world to the base directory of the serverpack. In other words, `/saves/MyAwesomeWorld` becomes `/MyAwesomeWorld`. 
includeServerInstallation | true | Whether to install a Forge/Fabric server for the serverpack. Must be `true` or `false`.
modLoader | "Forge" | Which modloader to install. Must be either "Forge" or "Fabric".
modLoaderVersion | "1.16.5-36.0.15" | Specific Minecraft-Modloader version to install the server in the serverpack.
includeServerIcon | true | Whether to include server-icon.png in your serverpack. Must be `true` or `false`.
includeServerProperties | true | Whether to include server.properties in your serverpack. Must be `true` or `false`.
includeStartScripts | true | Whether to inlude start scripts in your serverpack. Must be `true` or `false`.
includeZipCreation | true | Whether to create a zip-file of your serverpack, saved in the directory you specified with `modpackDir`. Must be `true` or `false`.

## Testing

If you want to help testing (I can not be held responsible for loss of data. Make sure you make backups and execute this program in a test environment.):
1. Download `serverpackcreator-x.x.x.jar` from `Packages` on the right side. Example for version 0.7.8:
   ![download-jar](img/download-jar.png)
2. Put it in the parent folder of your modpack folder. Here's an example for my modpack Survive Create Prosper:
```
tests/
├── Survive Create Prosper 4 1.16.5/
│   ├── config/
│   │   └── ...
│   ├── defaultconfigs/
│   │   └── ...
│   ├── mods/
│   │   └── ...
│   ├── scripts/
│   │   └── ...
│   ├── seeds/
│   │   └── ...
│   └── saves/scp4/
│       └── ...
├── creator.conf
├── server-properties
├── server-icon.png
├── serverproperties-0.7.8.jar
├── start.bat
└── start.sh
```
2.1 Customize the configuration file `creator.conf` to work with your modpack.
```
# Path to your modpack. Can be either relative or absolute.
modpackDir = "./Survive Create Prosper 4 1.16.5"

# List of client mods to delete from serverpack.
# No need to include version specifics.
# Must be the filenames of the mods, not their project names on CurseForge!
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

# Name of directories to include in serverpack.
# When specifying "saves/world", "world" will be copied to the base directory of the serverpack
# for immediate use with the server.
copyDirs = [
    "config",
    "defaultconfigs",
    "mods",
    "scripts",
    "seeds",
    "saves/scp4"
    ]

# Whether to install a Forge/Fabric server for the serverpack. Must be true or false.
# Which modloader to install. Must be either "Forge" or "Fabric".
# The version of the modloader you want to install.
# NOT YET IMPLEMENTED. IGNORE. THIS DOES NOTHING AT THE MOMENT.
includeServerInstallation = true
modLoader= "Forge"
modLoaderVersion = "1.16.5-36.0.15"

# Include a server-icon.png in your serverpack. Must be true or false.
# Place your server-icon.png in the same directory as this cfg-file.
# If no server-icon.png is provided but is set to true, a default one will be provided.
# Dimensions must be 64x64!
includeServerIcon = true

# Include a server.properties in your serverpack. Must be true or false.
# Place your server.properties in the same directory as this cfg-file.
# If no server.properties is provided but is set to true, a default one will be provided.
includeServerProperties = true

# Include start scripts for windows and linux systems. Must be true or false.
includeStartScripts = true

# Create zip-archive of serverpack. Must be true or false.
includeZipCreation = true
```
2.2 Customize any of the other files if need be. 

3. Execute the program from the CLI with `java -jar serverpackcreator-x.x.x.jar` where `-x.x.x.jar`is to be replaced with the version you downloaded. In the example above it would be `java -jar serverpackcreator-0.7.8.jar`.
   
4. ServerPackCreator should now be telling you what it is currently doing. You should also now see a `server_pack`-folder in the directory of your modpack. Example for my modpack:
```
tests/
└── Survive Create Prosper 4 1.16.5/
    ├── config/
    │   └── ...
    ├── defaultconfigs/
    │   └── ...
    ├── mods/
    │   └── ...
    ├── scripts/
    │   └── ...
    ├── seeds/
    │   └── ...
    ├── saves/scp4/
    │   └── ...
    └── server_pack/
        ├── config/
        │   └── ...
        ├── defaultconfigs/
        │   └── ...
        ├── mods/
        │   └── ...
        ├── scripts/
        │   └── ...
        ├── seeds/
        │   └── ...
        └── scp4/
            └── ...
```
Check these directories whether they contain all the files which are expected to be there.
