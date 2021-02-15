# ServerPackCreator (WIP Title)

Java programm to create a server pack from a given 1.16.5 Minecraft modpack (only SCP4 at first)

![Publish Release](https://github.com/Griefed/ServerPackCreator/workflows/Publish%20Release/badge.svg)
![Java CI with Gradle](https://github.com/Griefed/ServerPackCreator/workflows/Java%20CI%20with%20Gradle/badge.svg)

# WIP! CURRENTLY NOT FOR PUBLIC CONSUMPTION

Use at your own risk! You shouldn't use it atm.

## Features

Planned:
Take any given modpack, if possible for any given Forge version, and:

- [ ] **Download and install Forge server (make this configurable so users can specify their forge version)**
- [X] **Create default start scripts for Linux and Windows systems**
  - [X] **Make creation of start scripts optional**
  - [X] **Provide default/example**
  - [ ] **Change start scripts based on forge version in creator.conf**
- [X] **Delete client side mods**
  - [X] **Make list of client side mods cinfigurable**
- [X] **Allow specifiying directories to include in server_pack**
  - [X] **Remove prefix "saves" when adding a world from a modpack**
- [X] **Copy server-icon.png to "server_pack" directory**
  - [X] **Make creation of server-icon.png optional**
  - [X] **Provide default**
- [X] **Create a server.properties file in "server_pack" directory**
  - [X] **Make creation of server.properties optional**
  - [X] **Provide default**
- [X] **Automatically create zip-archive of the created serverpack**
  - [X] **Make creation of archive optional**

Supporting Fabric or any other modloader is not planned. Feel free to fork and do whatever you want with this software. It's MIT License for a reason.

## Testing

If you want to help testing (I can not be held responsible for loss of data. Make sure you make backups and execute this program in a test environment.):
1. Download `serverpackcreator-x.x.x.jar` from `Packages` on the right side. Example for version 0.7.8:
   ![download-jar](download-jar.png)
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

# Install Forge for your serverpack. Must be true or false.
# The version of Forge your modpack and serverpack are running on.
# NOT YET IMPLEMENTED. IGNORE. THIS DOES NOTHING AT THE MOMENT.
includeForgeInstallation = true
forgeVersion = "36.0.15"

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