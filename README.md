[![banner](https://i.griefed.de/images/2021/03/03/serverpackcreator_banner.png)](https://github.com/Griefed/ServerPackCreator)

[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/Griefed/ServerPackCreator/Java%20CI%20with%20Gradle?label=Gradle%20Build%20Test&logo=GitHub&style=for-the-badge&color=c0ffee&labelColor=325358)](https://github.com/Griefed/ServerPackCreator/actions?query=workflow%3A%22Java+CI+with+Gradle%22)
[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/Griefed/ServerPackCreator/Tag%20&%20Release%20&%20Publish?style=for-the-badge&logo=GitHub&color=c0ffee&labelColor=325358&label=Snag%20It%20Bag%20It%20Tag%20It&)](https://github.com/Griefed/ServerPackCreator/actions?query=workflow%3A%22Tag+%26+Release+%26+Publish%22)
[![GitHub release (latest by date including pre-releases)](https://img.shields.io/github/v/release/Griefed/ServerPackCreator?include_prereleases&label=Latest%20Release&logo=Github&style=for-the-badge&color=c0ffee&labelColor=325358)](https://github.com/Griefed?tab=packages&repo_name=ServerPackCreator)
[![GitHub](https://img.shields.io/github/license/Griefed/ServerPackCreator?logo=GitHub&style=for-the-badge&color=c0ffee&labelColor=325358)](https://github.com/Griefed/ServerPackCreator/blob/main/LICENSE)
[![GitHub all releases](https://img.shields.io/github/downloads/Griefed/ServerPackCreator/total?color=c0ffee&logo=GitHub&style=for-the-badge&labelColor=325358)](https://github.com/Griefed/ServerPackCreator/releases)

**ServerPackCreator is a Command Line Program(CLI) which aims to create a serverpack for any given modpack, thus making modpack-developing, or at least a part of it, a little less time-consuming. You customize the configuration to your liking and off you go.**
**Whenever you are working on an update to your modpack, you simply run ServerPackCreator and BAM! You've got yourself a serverpack for your new modpack version.**

I am making this in my spare time, therefore progress is slow. I also just started getting into Java programming, so expect the code to be of...questionable...quality.
If you're a Java Pro and your eyes bulge when looking at my code, by all means, you're welcome to help me improve it in any way, shape, or form.
You're also welcome to make contributions, or fork it and make your own version of it. It's LGPL-2.1 License for a reason. Do what you want with it.

Use at your own risk! Be aware that data loss is possible. If you wish to test this, there's a description down below.
If you wish to contribute, fork the repository, make your changes, create a pull request and make sure to use [conventional commits](https://github.com/Griefed/ServerPackCreator/wiki)

## Features

- **Supports Forge and Fabric modloaders**
  - Installation of modloader-server for server_pack optional
- **Default start scripts for Linux and Windows systems**
  - Inclusion in server_pack optional
  - Can be customized/replaced by user
- **Delete client side mods**
  - List of mods to delete from server_pack can be customized by user
- **Specify directories to include in server_pack**
  - List of directories can be customized by the user
  - Worlds can be included in the serverpack as well
    - Copying from `saves/world` will result in the world being copied to `server_pack/world`
- **Default server-icon.png**
  - Inclusion in server_pack optioanal
  - Can be customized/replaced by user
- **Default server.properties**
  - Inclusion in server_pack optional
  - Can be customized/replaced by user
- **Create zip-archive of server_pack for immediate upload to CurseForge etc.**
  - Zip-creation is optional
  - Does to include minecraft_server.jar as per Mojang's TOS and EULA
  - Includes download scripts for minecract_server.jar for Linux and Windows systems
  
## Configuration

The initial run of ServerPackCreator will place default-files in a directory called `server_files` in the same directory where the .jar-file resides in.
The config file will be created in the same directory as `ServerPackCreator-x.x.x.jar`.
Among those default files are:

File | Description
---- | ----
serverpackcreator.conf | Configuration file for customization. See [creator.conf](https://github.com/Griefed/ServerPackCreator/blob/main/src/main/resources/serverpackcreator.conf).
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
javaPath | Path to the Java Installation. On Linux systems use `which java` to find the location of your Java install. On Windows use `where java` and exclude the `.exe`-part.
minecraftVersion | The version of Minecraft for which to install the modloader server.
modLoader | Which modloader to install. Must be either "Forge" or "Fabric".
modLoaderVersion | Specific Modloader version to install the server in the serverpack.
includeServerIcon | Whether to include server-icon.png in your serverpack. Must be `true` or `false`.
includeServerProperties | Whether to include server.properties in your serverpack. Must be `true` or `false`.
includeStartScripts | Whether to include start scripts in your serverpack. Must be `true` or `false`.
includeZipCreation | Whether to create a zip-file of your serverpack, saved in the directory you specified with `modpackDir`. Must be `true` or `false`.

After checking the configuration, run ServerPackCreator again, and it'll do it's magic.

## Running

Guides on how to run ServerPackCreator are available at:
1. https://github.com/Griefed/ServerPackCreator/wiki/Running-ServerPackCreator
2. https://wiki.griefed.de/en/Documentation/ServerPackCreator/HowTo

(They're the same, but there's two for redundancies sake)
