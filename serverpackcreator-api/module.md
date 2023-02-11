# Module serverpackcreator-api

The heart and soul of ServerPackCreator. This is what's responsible for turning your modpacks into server packs.

# Package de.griefed.serverpackcreator.api.i18n

i18n i.e. Localization, of ServerPackCreator.

# Package de.griefed.serverpackcreator.api.modscanning

Mod-scanning to determine which mod can safely be excluded from a given modpack and therefor, soon to be server pack.

# Package de.griefed.serverpackcreator.api.plugins

Annotations for ServerPackCreator plugins. If you plan on adding additional entrypoints for plugins, make sure to extend
from [de.griefed.serverpackcreator.api.plugins.BaseInformation] and put your new annotation into a sub-package
corresponding to the part of ServerPackCreator where it will hook into.

# Package de.griefed.serverpackcreator.api.plugins.configurationhandler

Plugin annotations which hook into the [de.griefed.serverpackcreator.api.ConfigurationHandler]-class.

# Package de.griefed.serverpackcreator.api.plugins.serverpackhandler

Plugin annotations which hook into the [de.griefed.serverpackcreator.api.ServerPackHandler]-class.

# Package de.griefed.serverpackcreator.api.plugins.swinggui

Plugin annotations in the Swing-GUI of ServerPackCreator.

# Package de.griefed.serverpackcreator.api.utilities.common

Common utilities used by various parts, projects, things, cheeseburgers. Mostly composed of smaller methods which are
meant to do one and one thing only. Ya know, utilities.

# Package de.griefed.serverpackcreator.api.utilities

The utilities package holds classes and methods for various useful bits and pieces used all throughout ServerPackCreator.
If any given method is helpful in multiple steps throughout the ServerPackCreator architecture, it usually belongs to
one of the utility classes.

Such utilities include, but are not limited to converting Strings to booleans, encapsulating each entry in a String list
with quotation-marks, downloading files, copying files and folders from a JAR-file and so on and so forth.

Basically, whenever a method in any SeverPackCreator class becomes useful to multiple classes, it is usually moved into
any already existing utility-class or a new one is added if no already-fitting one exists.

# Package de.griefed.serverpackcreator.api.versionmeta

The VersionMeta gives you access to various information about various modloaders, their versions, installers,
compatibility and, of course, Minecraft itself.

# Package de.griefed.serverpackcreator.api.versionmeta.fabric

Anything and everything related to the Fabric-modloader.

# Package de.griefed.serverpackcreator.api.versionmeta.forge

Anything and everything related to the Forge-modloader.

# Package de.griefed.serverpackcreator.api.versionmeta.legacyfabric

Anything and everything related to the LegacyFabric-modloader.

# Package de.griefed.serverpackcreator.api.versionmeta.minecraft

Anything and everything related to the Minecraft itself.

# Package de.griefed.serverpackcreator.api.versionmeta.quilt

Anything and everything related to the Quilt-modloader.

# Package de.griefed.serverpackcreator.api

Core-package of ServerPackCreator.

Server packs are created using [de.griefed.serverpackcreator.api.ConfigurationModel], which can
be checked for errors using [de.griefed.serverpackcreator.api.ConfigurationHandler.checkConfiguration] and any of the
available variants. Afterwards, when the checks of the given configuration model return no errors, it is fed into
[de.griefed.serverpackcreator.api.ServerPackHandler.run], which creates finally creates your server pack.

In other words, the intended workflow is as follows:

1. Create a [de.griefed.serverpackcreator.api.ConfigurationModel].
2. Check it using [de.griefed.serverpackcreator.api.ConfigurationHandler.checkConfiguration] or variants.
3. Create the server pack using [de.griefed.serverpackcreator.api.ServerPackHandler.run].

Should you wish to customize your instance of ServerPackCreator, see [de.griefed.serverpackcreator.api.ApplicationProperties].
If you wish to enhance your instance of ServerPackCreator with plugins, see [de.griefed.serverpackcreator.api.Applicationplugins]
and
* [de.griefed.serverpackcreator.api.plugins.configurationhandler.ConfigCheckExtension]
* [de.griefed.serverpackcreator.api.plugins.serverpackhandler.PreGenExtension]
* [de.griefed.serverpackcreator.api.plugins.serverpackhandler.PreZipExtension]
* [de.griefed.serverpackcreator.api.plugins.serverpackhandler.PostGenExtension]
* [de.griefed.serverpackcreator.api.plugins.swinggui.ConfigPanelExtension] and [de.griefed.serverpackcreator.plugins.swinggui.ExtensionConfigPanel]
* [de.griefed.serverpackcreator.api.plugins.swinggui.TabExtension] and [de.griefed.serverpackcreator.plugins.swinggui.ExtensionTab]