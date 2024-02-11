/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.api

import de.comahe.i18n4k.Locale
import de.comahe.i18n4k.config.I18n4kConfigDefault
import de.comahe.i18n4k.i18n4k
import de.comahe.i18n4k.toTag
import de.griefed.serverpackcreator.api.utilities.common.*
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.util.*
import java.util.prefs.Preferences

/**
 * Base settings of ServerPackCreator, such as working directories, default list of clientside-only
 * mods, default list of directories to include in a server pack, script templates, java paths and
 * much more.
 *
 * @param fileUtilities   Instance of {@link FileUtilities} for file-operations.
 * @param systemUtilities Instance of {@link SystemUtilities} to acquire the Java path
 *                        automatically.
 * @param listUtilities   Used to print the configured fallback modlists in chunks.
 * @param jarUtilities    Instance of {@link JarUtilities} used to acquire .exe or JAR-, as well
 *                        as system information.
 * @param propertiesFile  serverpackcreator.properties-file containing settings and configurations to load the API with.
 * @author Griefed
 */
@Suppress("unused")
actual class ApiProperties(
    private val fileUtilities: FileUtilities,
    private val systemUtilities: SystemUtilities,
    private val listUtilities: ListUtilities,
    jarUtilities: JarUtilities,
    propertiesFile: File = File("serverpackcreator.properties")
) {
    private val log = cachedLoggerOf(javaClass)
    private val internalProps = Properties()
    private val userPreferences = Preferences.userRoot().node("ServerPackCreator")
    private val serverPackCreatorProperties = "serverpackcreator.properties"
    private val jarInformation: JarInformation = JarInformation(javaClass, jarUtilities)
    private val jarFolderProperties: File =
        File(jarInformation.jarFolder.absoluteFile, serverPackCreatorProperties).absoluteFile
    private val pVersionCheckPreRelease =
        "de.griefed.serverpackcreator.versioncheck.prerelease"
    private val pLanguage =
        "de.griefed.serverpackcreator.language"
    private val pConfigurationFallbackUpdateURL =
        "de.griefed.serverpackcreator.configuration.fallback.updateurl"
    private val pConfigurationFallbackModsList =
        "de.griefed.serverpackcreator.configuration.fallbackmodslist"
    private val pConfigurationFallbackModsListRegex =
        "de.griefed.serverpackcreator.configuration.fallbackmodslist.regex"
    private val pConfigurationFallbackModsWhiteList =
        "de.griefed.serverpackcreator.configuration.modswhitelist"
    private val pConfigurationHasteBinServerUrl =
        "de.griefed.serverpackcreator.configuration.hastebinserver"
    private val pConfigurationAikarsFlags =
        "de.griefed.serverpackcreator.configuration.aikar"
    private val pServerPackAutoDiscoveryEnabled =
        "de.griefed.serverpackcreator.serverpack.autodiscovery.enabled"
    private val pServerPackAutoDiscoveryEnabledLegacy =
        "de.griefed.serverpackcreator.serverpack.autodiscoverenabled"
    private val pGuiDarkMode =
        "de.griefed.serverpackcreator.gui.darkmode"
    private val pConfigurationDirectoriesServerPacks =
        "de.griefed.serverpackcreator.configuration.directories.serverpacks"
    private val pServerPackCleanupEnabled =
        "de.griefed.serverpackcreator.serverpack.cleanup.enabled"
    private val pServerPackOverwriteEnabled =
        "de.griefed.serverpackcreator.serverpack.overwrite.enabled"
    private val pConfigurationDirectoriesShouldExclude =
        "de.griefed.serverpackcreator.configuration.directories.shouldexclude"
    private val pSpringSchedulesDatabaseCleanup =
        "de.griefed.serverpackcreator.spring.schedules.database.cleanup"
    private val pStringSchedulesFilesCleanup =
        "de.griefed.serverpackcreator.spring.schedules.files.cleanup"
    private val pSpringSchedulesVersionMetaRefresh =
        "de.griefed.serverpackcreator.spring.schedules.versions.refresh"
    private val pConfigurationSaveLastLoadedConfigEnabled =
        "de.griefed.serverpackcreator.configuration.saveloadedconfig"
    private val pConfigurationDirectoriesMustInclude =
        "de.griefed.serverpackcreator.configuration.directories.mustinclude"
    private val pServerPackZipExclusions =
        "de.griefed.serverpackcreator.serverpack.zip.exclude"
    private val pServerPackZipExclusionEnabled =
        "de.griefed.serverpackcreator.serverpack.zip.exclude.enabled"
    private val pServerPackScriptTemplates =
        "de.griefed.serverpackcreator.serverpack.script.template"
    private val pPostInstallCleanupFiles =
        "de.griefed.serverpackcreator.install.post.files"
    private val pPreInstallCleanupFiles =
        "de.griefed.serverpackcreator.install.pre.files"
    private val pAllowUseMinecraftSnapshots =
        "de.griefed.serverpackcreator.minecraft.snapshots"
    private val pServerPackAutoDiscoveryFilterMethod =
        "de.griefed.serverpackcreator.serverpack.autodiscovery.filter"
    private val pJavaForServerInstall =
        "de.griefed.serverpackcreator.java"
    private val pScriptVariablesJavaPaths =
        "de.griefed.serverpackcreator.script.java"
    private val pScriptVariablesAutoUpdateJavaPathsEnabled =
        "de.griefed.serverpackcreator.script.java.autoupdate"
    private val pHomeDirectory =
        "de.griefed.serverpackcreator.home"
    private val pOldVersion =
        "de.griefed.serverpackcreator.version.old"
    private val customPropertyPrefix =
        "custom.property."
    private val pTomcatBaseDirectory =
        "server.tomcat.basedir"
    private val pTomcatLogsDirectory =
        "server.tomcat.accesslog.directory"
    private val pSpringDatasourceUrl =
        "spring.datasource.url"

    val home: File = if (System.getProperty("user.home").isNotEmpty()) {
        File(System.getProperty("user.home"))
    } else {
        jarInformation.jarFolder.absoluteFile
    }

    private var fallbackModsWhitelist = TreeSet(
        listOf(
            "Ping-Wheel-"
        )
    )

    @Suppress("SpellCheckingInspection")
    private var fallbackMods = TreeSet(
        listOf(
            "3dskinlayers-",
            "Absolutely-Not-A-Zoom-Mod-",
            "AdvancedChat-",
            "AdvancedChatCore-",
            "AdvancedChatHUD-",
            "AdvancedCompas-",
            "Ambience",
            "AmbientEnvironment-",
            "AmbientSounds_",
            "AreYouBlind-",
            "Armor Status HUD-",
            "ArmorSoundTweak-",
            "BH-Menu-",
            "Batty's Coordinates PLUS Mod",
            "BetterAdvancements-",
            "BetterAnimationsCollection-",
            "BetterModsButton-",
            "BetterDarkMode-",
            "BetterF3-",
            "BetterFog-",
            "BetterFoliage-",
            "BetterPingDisplay-",
            "BetterPlacement-",
            "BetterTaskbar-",
            "BetterThirdPerson",
            "BetterTitleScreen-",
            "Blur-",
            "BorderlessWindow-",
            "CTM-",
            "ChunkAnimator-",
            "ClientTweaks_",
            "CompletionistsIndex-",
            "Controller Support-",
            "Controlling-",
            "CraftPresence-",
            "CullLessLeaves-Reforged-",
            "CustomCursorMod-",
            "CustomMainMenu-",
            "DefaultOptions_",
            "DefaultSettings-",
            "DeleteWorldsToTrash-",
            "DetailArmorBar-",
            "Ding-",
            "DistantHorizons-",
            "DripSounds-",
            "Durability101-",
            "DurabilityNotifier-",
            "DynamicSurroundings-",
            "DynamicSurroundingsHuds-",
            "EffectsLeft-",
            "EiraMoticons_",
            "EnchantmentDescriptions-",
            "EnhancedVisuals_",
            "EquipmentCompare-",
            "FPS-Monitor-",
            "FabricCustomCursorMod-",
            "Fallingleaves-",
            "FancySpawnEggs",
            "FancyVideo-API-",
            "farsight-",
            "FirstPersonMod",
            "FogTweaker-",
            "ForgeCustomCursorMod-",
            "FpsReducer-",
            "FpsReducer2-",
            "FullscreenWindowed-",
            "GameMenuModOption-",
            "HealthOverlay-",
            "HeldItemTooltips-",
            "HorseStatsMod-",
            "ImmediatelyFastReforged-",
            "InventoryEssentials_",
            "InventoryHud_[1.17.1].forge-",
            "InventorySpam-",
            "InventoryTweaks-",
            "ItemBorders-",
            "ItemPhysicLite_",
            "ItemStitchingFix-",
            "JBRA-Client-",
            "JustEnoughCalculation-",
            "JustEnoughEffects-",
            "JustEnoughProfessions-",
            "LeaveMyBarsAlone-",
            "LLOverlayReloaded-",
            "LOTRDRP-",
            "LegendaryTooltips",
            "LegendaryTooltips-",
            "LightOverlay-",
            "MinecraftCapes ",
            "MoBends",
            "MouseTweaks-",
            "MyServerIsCompatible-",
            "Neat ",
            "Neat-",
            "NekosEnchantedBooks-",
            "NoAutoJump-",
            "NoFog-",
            "Notes-",
            "NotifMod-",
            "OldJavaWarning-",
            "OptiFine",
            "OptiFine_",
            "OptiForge",
            "OptiForge-",
            "OverflowingBars-",
            "PackMenu-",
            "PackModeMenu-",
            "PickUpNotifier-",
            "Ping-",
            "PingHUD-",
            "PresenceFootsteps-",
            "RPG-HUD-",
            "ReAuth-",
            "Reforgium-",
            "ResourceLoader-",
            "ResourcePackOrganizer",
            "Ryoamiclights-",
            "ShoulderSurfing-",
            "ShulkerTooltip-",
            "SimpleDiscordRichPresence-",
            "SimpleWorldTimer-",
            "SoundFilters-",
            "SpawnerFix-",
            "StylishEffects-",
            "TextruesRubidiumOptions-",
            "TRansliterationLib-",
            "TipTheScales-",
            "Tips-",
            "Toast Control-",
            "Toast-Control-",
            "ToastControl-",
            "TravelersTitles-",
            "VoidFog-",
            "VR-Combat_",
            "WindowedFullscreen-",
            "WorldNameRandomizer-",
            "YeetusExperimentus-",
            "YungsMenuTweaks-",
            "[1.12.2]DamageIndicatorsMod-",
            "[1.12.2]bspkrscore-",
            "antighost-",
            "anviltooltipmod-",
            "appleskin-",
            "armorchroma-",
            "armorpointspp-",
            "auditory-",
            "authme-",
            "auto-reconnect-",
            "autojoin-",
            "autoreconnect-",
            "axolotl-item-fix-",
            "backtools-",
            "bannerunlimited-",
            "beenfo-1.19-",
            "better-recipe-book-",
            "betterbiomeblend-",
            "bhmenu-",
            "blur-",
            "borderless-mining-",
            "cat_jam-",
            "catalogue-",
            "charmonium-",
            "chat_heads-",
            "cherishedworlds-",
            "cirback-1.0-",
            "classicbar-",
            "clickadv-",
            "clienttweaks-",
            "combat_music-",
            "connectedness-",
            "controllable-",
            "cullleaves-",
            "cullparticles-",
            "custom-crosshair-mod-",
            "customdiscordrpc-",
            "darkness-",
            "dashloader-",
            "defaultoptions-",
            "desiredservers-",
            "discordrpc-",
            "drippyloadingscreen-",
            "drippyloadingscreen_",
            "durabilitytooltip-",
            "dynamic-fps-",
            "dynamic-music-",
            "dynamiclights-",
            "dynmus-",
            "effective-",
            "eggtab-",
            "eguilib-",
            "eiramoticons-",
            "embeddium-",
            "enchantment-lore-",
            "entity-texture-features-",
            "entityculling-",
            "essential_",
            "exhaustedstamina-",
            "extremesoundmuffler-",
            "fabricemotes-",
            "fancymenu_",
            "fancymenu_video_extension",
            "flickerfix-",
            "fm_audio_extension_",
            "forgemod_VoxelMap-",
            "freelook-",
            "galacticraft-rpc-",
            "gamestagesviewer-",
            "gpumemleakfix-",
            "grid-",
            "helium-",
            "hiddenrecipebook_",
            "hiddenrecipebook-",
            "infinitemusic-",
            "inventoryprofiles",
            "invtweaks-",
            "itemzoom",
            "itlt-",
            "jeed-",
            "jehc-",
            "jeiintegration_",
            "jumpoverfences-",
            "just-enough-harvestcraft-",
            "justenoughbeacons-",
            "justenoughdrags-",
            "justzoom_",
            "keymap-",
            "keywizard-",
            "lazydfu-",
            "lib39-",
            "light-overlay-",
            "lightfallclient-",
            "lightspeed-",
            "loadmyresources_",
            "lock_minecart_view-",
            "lootbeams-",
            "lwl-",
            "magnesium_extras-",
            "maptooltip-",
            "massunbind",
            "mcbindtype-",
            "mcwifipnp-",
            "medievalmusic-",
            "memoryusagescreen-",
            "mightyarchitect-",
            "mindful-eating-",
            "minetogether-",
            "mobplusplus-",
            "modcredits-",
            "modernworldcreation_",
            "modnametooltip-",
            "modnametooltip_",
            "moreoverlays-",
            "mousewheelie-",
            "movement-vision-",
            "multihotbar-",
            "music-duration-reducer-",
            "musicdr-",
            "neiRecipeHandlers-",
            "ngrok-lan-expose-mod-",
            "no_nv_flash-",
            "nopotionshift_",
            "notenoughanimations-",
            "oculus-",
            "ornaments-",
            "overloadedarmorbar-",
            "panorama-",
            "paperdoll-",
            "physics-mod-",
            "phosphor-",
            "preciseblockplacing-",
            "radon-",
            "realm-of-lost-souls-",
            "rebind_narrator-",
            "rebind-narrator-",
            "rebindnarrator-",
            "rebrand-",
            "reforgium-",
            "replanter-",
            "rrls-",
            "rubidium-",
            "rubidium_extras-",
            "screenshot-to-clipboard-",
            "servercountryflags-",
            "shutupexperimentalsettings-",
            "shutupmodelloader-",
            "signtools-",
            "simple-rpc-",
            "simpleautorun-",
            "smartcursor-",
            "smoothboot-",
            "smoothfocus-",
            "sodium-fabric-",
            "sounddeviceoptions-",
            "soundreloader-",
            "spoticraft-",
            "skinlayers3d-forge",
            "tconplanner-",
            "textrues_embeddium_options-",
            "timestamps-",
            "tooltipscroller-",
            "torchoptimizer-",
            "torohealth-",
            "totaldarkness",
            "toughnessbar-",
            "watermedia-",
            "whats-that-slot-forge-",
            "wisla-",
            "xlifeheartcolors-",
            "yisthereautojump-"
            "ModernUI-",
            "EasyLAN-",
            "MineMenu-"
        )
    )

    @Suppress("MemberVisibilityCanBePrivate")
    val fallbackDirectoriesInclusion = TreeSet(
        listOf(
            "addonpacks",
            "blueprints",
            "config",
            "configs",
            "customnpcs",
            "defaultconfigs",
            "global_data_packs",
            "global_packs",
            "kubejs",
            "maps",
            "mods",
            "openloader",
            "scripts",
            "schematics",
            "shrines-saves",
            "structures",
            "structurize",
            "worldshape",
            "Zoestria"
        )
    )

    @Suppress("MemberVisibilityCanBePrivate")
    val fallbackDirectoriesExclusion = TreeSet(
        listOf(
            "animation",
            "asm",
            "cache",
            "changelogs",
            "craftpresence",
            "crash-reports",
            "downloads",
            "icons",
            "libraries",
            "local",
            "logs",
            "overrides",
            "packmenu",
            "profileImage",
            "profileImage",
            "resourcepacks",
            "screenshots",
            "server_pack",
            "shaderpacks",
            "simple-rpc",
            "tv-cache"
        )
    )

    @Suppress("MemberVisibilityCanBePrivate")
    val fallbackZipExclusions = TreeSet(
        listOf(
            "minecraft_server.MINECRAFT_VERSION.jar",
            "server.jar",
            "libraries/net/minecraft/server/MINECRAFT_VERSION/server-MINECRAFT_VERSION.jar"
        )
    )

    val fallbackPostInstallCleanupFiles = TreeSet(
        listOf(
            "fabric-installer.jar",
            "forge-installer.jar",
            "quilt-installer.jar",
            "installer.log",
            "forge-installer.jar.log",
            "legacyfabric-installer.jar",
            "run.bat",
            "run.sh",
            "user_jvm_args.txt"
        )
    )

    val fallbackPreInstallCleanupFiles = TreeSet(
        listOf(
            "libraries",
            "server.jar",
            "forge-installer.jar",
            "quilt-installer.jar",
            "installer.log",
            "forge-installer.jar.log",
            "legacyfabric-installer.jar",
            "run.bat",
            "run.sh",
            "user_jvm_args.txt",
            "quilt-server-launch.jar",
            "minecraft_server.1.16.5.jar",
            "forge.jar"
        )
    )

    @Suppress("MemberVisibilityCanBePrivate")
    val fallbackAikarsFlags = "-Xms4G" +
            " -Xmx4G" +
            " -XX:+UseG1GC" +
            " -XX:+ParallelRefProcEnabled" +
            " -XX:MaxGCPauseMillis=200" +
            " -XX:+UnlockExperimentalVMOptions" +
            " -XX:+DisableExplicitGC" +
            " -XX:+AlwaysPreTouch" +
            " -XX:G1NewSizePercent=30" +
            " -XX:G1MaxNewSizePercent=40" +
            " -XX:G1HeapRegionSize=8M" +
            " -XX:G1ReservePercent=20" +
            " -XX:G1HeapWastePercent=5" +
            " -XX:G1MixedGCCountTarget=4" +
            " -XX:InitiatingHeapOccupancyPercent=15" +
            " -XX:G1MixedGCLiveThresholdPercent=90" +
            " -XX:G1RSetUpdatingPauseTimePercent=5" +
            " -XX:SurvivorRatio=32" +
            " -XX:+PerfDisableSharedMem" +
            " -XX:MaxTenuringThreshold=1" +
            " -Dusing.aikars.flags=https://mcflags.emc.gs" +
            " -Daikars.new.flags=true"
    val fallbackUpdateURL =
        "https://raw.githubusercontent.com/Griefed/ServerPackCreator/main/serverpackcreator-api/src/jvmMain/resources/serverpackcreator.properties"
    val fallbackExclusionFilter = ExclusionFilter.START
    val fallbackOverwriteEnabled = true
    val fallbackJavaScriptAutoupdateEnabled = true
    val fallbackCheckingForPreReleasesEnabled = false
    val fallbackZipFileExclusionEnabled = true
    val fallbackServerPackCleanupEnabled = true
    val fallbackMinecraftPreReleasesAvailabilityEnabled = false
    val fallbackAutoExcludingModsEnabled = true
    val fallbackArtemisQueueMaxDiskUsage = 90
    val fallbackCleanupSchedule = "0 0 0 * * *"
    val fallbackVersionSchedule = "0 0 0 * * *"
    val fallbackDatabaseCleanupSchedule = "0 0 0 * * *"
    private val checkedJavas = hashMapOf<String, Boolean>()

    @Suppress("MemberVisibilityCanBePrivate")
    val trueFalseRegex = "^(true|false)$".toRegex()

    @Suppress("MemberVisibilityCanBePrivate")
    val alphaBetaRegex = "^(.*alpha.*|.*beta.*)$".toRegex()

    @Suppress("MemberVisibilityCanBePrivate")
    val serverPacksRegex = "^(?:\\./)?server-packs$".toRegex()
    val i18n4kConfig = I18n4kConfigDefault()

    /**
     * String-list of clientside-only mods to exclude from server packs.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var clientsideMods = fallbackMods
        private set

    /**
     * String-list of mods to include if present, regardless whether a match was found through [clientsideMods].
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var modsWhitelist = fallbackModsWhitelist
        private set

    /**
     * Regex-list of clientside-only mods to exclude from server packs.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var clientsideModsRegex: TreeSet<String> = TreeSet()
        get() {
            field.clear()
            for (mod in clientsideMods) {
                field.add("^$mod.*$")
            }
            return field
        }
        private set

    /**
     * Regex-list of mods to include if present, regardless whether a match was found throug [clientsideModsRegex].
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var modsWhitelistRegex: TreeSet<String> = TreeSet()
        get() {
            field.clear()
            for (mod in modsWhitelist) {
                field.add("^$mod.*$")
            }
            return field
        }
        private set


    /**
     * Modloaders supported by ServerPackCreator.
     */
    val supportedModloaders = arrayOf("Fabric", "Forge", "Quilt", "LegacyFabric", "NeoForge")

    /**
     * The folder containing the ServerPackCreator.exe or JAR-file.
     *
     * @return Folder containing the ServerPackCreator.exe or JAR-file.
     * @author Griefed
     */
    fun getJarFolder() = jarInformation.jarFolder

    /**
     * Whether a .exe or JAR-file was used for running ServerPackCreator.
     *
     * @return `true` if a .exe was/is used.
     * @author Griefed
     */
    fun isExe() = jarInformation.isExe

    /**
     * The .exe or JAR-file of ServerPackCreator.
     *
     * @return The .exe or JAR-file of ServerPackCreator.
     * @author Griefed
     */
    fun getJarFile() = jarInformation.jarFile

    /**
     * The name of the .exe or JAR-file.
     *
     * @return The name of the .exe or JAR-file.
     * @author Griefed
     */
    fun getJarName(): String = jarInformation.jarFile.name

    /**
     * The Java version used to run ServerPackCreator.
     *
     * @return Java version.
     * @author Griefed
     */
    fun getJavaVersion() = jarInformation.javaVersion

    /**
     * Architecture of the operating system on which ServerPackCreator is running on.
     *
     * @return Arch.
     * @author Griefed
     */
    fun getOSArch() = jarInformation.osArch

    /**
     * The name of the operating system on which ServerPackCreator is running on.
     *
     * @return OS name.
     * @author Griefed
     */
    fun getOSName() = jarInformation.osName

    /**
     * The version of the OS on which ServerPackCreator is running on.
     *
     * @return Version of the OS.
     * @author Griefed
     */
    fun getOSVersion() = jarInformation.osVersion

    /**
     * The version of the ServerPackCreator API.
     */
    val apiVersion: String = javaClass.getPackage().implementationVersion ?: "dev"

    val devBuild: Boolean
        get() {
            return apiVersion == "dev"
        }

    val preRelease: Boolean
        get() {
            return apiVersion.matches(alphaBetaRegex)
        }

    val configVersion: String = if (preRelease || devBuild) {
        "TEST"
    } else {
        "4"
    }

    init {
        i18n4k = i18n4kConfig
        i18n4kConfig.defaultLocale = Locale("en_GB")
        loadProperties(propertiesFile, false)
    }

    /**
     * Only the first call to this property will return true if this is the first time ServerPackCreator is being run
     * on a given host. Any subsequent call will return false. Handle with care!
     *
     * @author Griefed
     */
    val firstRun: Boolean

    init {
        firstRun = getBoolProperty("de.griefed.serverpackcreator.firstrun", true)
        setBoolProperty("de.griefed.serverpackcreator.firstrun", false)
    }

    /**
     * The default shell-template for the modded server start scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * [scriptTemplates].
     */
    val defaultShellScriptTemplate = File("default_template.sh")

    /**
     * The default PowerShell-template for the modded server start scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * [scriptTemplates].
     */
    val defaultPowerShellScriptTemplate = File("default_template.ps1")

    /**
     * Directories to include in a server pack.
     */
    var directoriesToInclude = fallbackDirectoriesInclusion
        get() {
            val entries =
                getListProperty(pConfigurationDirectoriesMustInclude, fallbackDirectoriesInclusion.joinToString(","))
            field.addAll(entries)
            return field
        }
        set(value) {
            setListProperty(pConfigurationDirectoriesMustInclude, value.toList(), ",")
            field.clear()
            field.addAll(value)
            log.info("Directories which must always be included set to: $value")
        }

    /**
     * Directories to exclude from a server pack.
     */
    var directoriesToExclude = fallbackDirectoriesExclusion
        get() {
            val prop =
                getListProperty(pConfigurationDirectoriesShouldExclude, fallbackDirectoriesExclusion.joinToString(","))
            val use = TreeSet(prop)
            use.removeIf { n -> directoriesToInclude.contains(n) }
            field.clear()
            field.addAll(use)
            return field
        }
        set(value) {
            val use = TreeSet<String>()
            use.addAll(value)
            use.removeIf { n -> directoriesToInclude.contains(n) }
            setListProperty(pConfigurationDirectoriesShouldExclude, use.toList(), ",")
            field.clear()
            field.addAll(use)
            log.info("Directories which must always be excluded set to: $field")
        }

    /**
     * List of files to delete after a server pack server installation.
     */
    var postInstallCleanupFiles = fallbackPostInstallCleanupFiles
        get() {
            val entries = getListProperty(pPostInstallCleanupFiles, fallbackPostInstallCleanupFiles.joinToString(","))
            field.addAll(entries)
            return field
        }
        set(value) {
            setListProperty(pPostInstallCleanupFiles, value.toList(), ",")
            field.clear()
            field.addAll(value)
            log.info("Files to cleanup after server installation set to: $value")
        }

    /**
     * List of files to delete before a server pack server installation.
     */
    var preInstallCleanupFiles = fallbackPreInstallCleanupFiles
        get() {
            val entries = getListProperty(pPreInstallCleanupFiles, fallbackPreInstallCleanupFiles.joinToString(","))
            field.addAll(entries)
            return field
        }
        set(value) {
            setListProperty(pPreInstallCleanupFiles, value.toList(), ",")
            field.clear()
            field.addAll(value)
            log.info("Files to cleanup before server installation set to: $value")
        }

    /**
     * List of files to be excluded from ZIP-archives. Current filters are:
     *
     *  * `MINECRAFT_VERSION` - Will be replaced with the Minecraft version of the server pack
     *  * `MODLOADER` - Will be replaced with the modloader of the server pack
     *  * `MODLOADER_VERSION` - Will be replaced with the modloader version of the server pack
     *
     * Should you want these filters to be expanded, open an issue on [GitHub](https://github.com/Griefed/ServerPackCreator/issues)
     */
    var zipArchiveExclusions = fallbackZipExclusions
        get() {
            val entries = getListProperty(pServerPackZipExclusions, fallbackZipExclusions.joinToString(","))
            field.addAll(entries)
            return field
        }
        set(value) {
            setListProperty(pServerPackZipExclusions, value.toList(), ",")
            field.clear()
            field.addAll(value)
            log.info("Files which must be excluded from ZIP-archives set to: $value")
        }

    /**
     * Paths to Java installations available to SPC for automatically updating the script variables of a given server pack
     * configuration.
     * * key: Java version
     * * value: Path to the Java .exe or binary
     *
     * If you plan on overwriting this property, make sure to format they key-value-pairs as follows:
     * * key: `de.griefed.serverpackcreator.script.java` followed by the number representing the Java version
     * * value: Valid path to a Java installation corresponding to the number used in the key
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var javaPaths = HashMap<String, String>(256)
        get() {
            val paths = HashMap<String, String>(256)
            var path: String
            var position: String
            for (i in 8..255) {
                position = pScriptVariablesJavaPaths + i
                path = internalProps.getProperty(position, "")
                if (checkJavaPath(path)) {
                    paths[i.toString()] = path
                    internalProps.setProperty(position, path)
                }
            }
            field = paths
            return paths
        }
        set(values) {
            var position: Int?
            var newKey: String
            val paths = HashMap<String, String>(256)
            for (i in 8..255) {
                internalProps.remove(pScriptVariablesJavaPaths + i)
            }
            for ((key, value) in values) {
                if (!checkJavaPath(value)) {
                    continue
                }
                position = key.replace(pScriptVariablesJavaPaths, "").toIntOrNull()
                newKey = pScriptVariablesJavaPaths + position
                if (position != null && 8 <= position!! && position!! < 256) {
                    internalProps.setProperty(newKey, value)
                    paths[newKey] = value
                }
            }
            field = paths
            log.info("Available Java paths for scripts:")
            for ((key, value) in field) {
                log.info("Java $key path: $value")
            }
        }

    /**
     * Default list of script templates used by ServerPackCreator.
     *
     * @author Griefed
     */
    fun defaultScriptTemplates(): List<File> {
        // See whether we have custom files.
        val currentFiles = serverFilesDirectory.walk().maxDepth(1).filter {
            it.name.endsWith("sh", ignoreCase = true) ||
                    it.name.endsWith("ps1", ignoreCase = true) ||
                    it.name.endsWith("bat", ignoreCase = true)
        }.toList()
        val customTemplates = currentFiles.filter {
            !it.name.contains("default_template", ignoreCase = true)
        }

        val newTemplates = mutableListOf<File>()
        var shellPresent = false
        var powershellPresent = false
        var batchPresent = false
        for (customTemplate in customTemplates) {
            when {
                customTemplate.name.endsWith("sh", ignoreCase = true) && !shellPresent -> {
                    newTemplates.add(customTemplate.absoluteFile)
                    shellPresent = true
                }

                customTemplate.name.endsWith("ps1", ignoreCase = true) && !powershellPresent -> {
                    newTemplates.add(customTemplate.absoluteFile)
                    powershellPresent = true
                }

                customTemplate.name.endsWith("bat", ignoreCase = true) && !batchPresent -> {
                    newTemplates.add(customTemplate.absoluteFile)
                    batchPresent = true
                }

                else -> {
                    newTemplates.add(customTemplate.absoluteFile)
                }
            }
        }

        if (!shellPresent) {
            newTemplates.add(File(serverFilesDirectory.absolutePath, defaultShellScriptTemplate.name).absoluteFile)
        }
        if (!powershellPresent) {
            newTemplates.add(File(serverFilesDirectory.absolutePath, defaultPowerShellScriptTemplate.name).absoluteFile)
        }

        return newTemplates.toList()
    }

    /**
     * Start-script templates to use during server pack generation.
     */
    @Suppress("SetterBackingFieldAssignment")
    var scriptTemplates: TreeSet<File> = TreeSet<File>()
        get() {
            val entries = if (internalProps.getProperty(pServerPackScriptTemplates) != null
                && internalProps.getProperty(pServerPackScriptTemplates) == "default_template.ps1,default_template.sh"
            ) {
                defaultScriptTemplates()
            } else {
                getListProperty(
                    pServerPackScriptTemplates,
                    defaultScriptTemplates().joinToString(",") { it.absolutePath }
                ).map { File(it).absoluteFile }
            }
            field.clear()
            field.addAll(entries)
            return field
        }
        set(value) {
            val entries = value.map { it.absolutePath }
            setListProperty(pServerPackScriptTemplates, entries, ",")
            field.clear()
            field.addAll(value.map { it.absoluteFile })
            log.info("Using script templates:")
            for (template in field) {
                log.info("    " + template.path)
            }
        }

    /**
     * The URL from which a .properties-file is read during updating of the fallback clientside-mods list.
     * The default can be found in [fallbackUpdateURL].
     */
    var updateUrl = URI(fallbackUpdateURL).toURL()
        get() {
            field = URI(acquireProperty(pConfigurationFallbackUpdateURL, fallbackUpdateURL)).toURL()
            return field
        }
        set(value) {
            defineProperty(pConfigurationFallbackUpdateURL, value.toString())
            field = value
        }

    /**
     * The filter method with which to determine whether a user-specified clientside-only mod should
     * be excluded from the server pack. Available settings are:
     *
     *  * [ExclusionFilter.START]
     *  * [ExclusionFilter.END]
     *  * [ExclusionFilter.CONTAIN]
     *  * [ExclusionFilter.REGEX]
     *  * [ExclusionFilter.EITHER]
     */
    var exclusionFilter = fallbackExclusionFilter
        get() {
            val prop = acquireProperty(pServerPackAutoDiscoveryFilterMethod, "START")
            field = try {
                when (prop) {
                    "END" -> ExclusionFilter.END
                    "CONTAIN" -> ExclusionFilter.CONTAIN
                    "REGEX" -> ExclusionFilter.REGEX
                    "EITHER" -> ExclusionFilter.EITHER
                    "START" -> ExclusionFilter.START
                    else -> {
                        log.error("Invalid filter specified. Defaulting to START.")
                        fallbackExclusionFilter
                    }
                }
            } catch (ex: NullPointerException) {
                log.error("No filter specified. Defaulting to START.")
                fallbackExclusionFilter
            }
            return field
        }
        set(value) {
            internalProps.setProperty(pServerPackAutoDiscoveryFilterMethod, value.toString())
            field = value
            log.info("User specified clientside-only mod exclusion filter set to: $field")
        }

    /**
     * Whether the search for available PreReleases is enabled or disabled. Depending on
     * `de.griefed.serverpackcreator.versioncheck.prerelease`, returns `true` if checks for available PreReleases are
     * enabled, `false` if no checks for available PreReleases should be made.
     */
    var isCheckingForPreReleasesEnabled = fallbackCheckingForPreReleasesEnabled
        get() {
            field = getBoolProperty(pVersionCheckPreRelease, fallbackCheckingForPreReleasesEnabled)
            return field
        }
        set(value) {
            setBoolProperty(pVersionCheckPreRelease, value)
            field = value
            log.info("Checking for pre-releases set to $field.")
        }

    /**
     * Whether the exclusion of files from the ZIP-archive of the server pack is enabled.
     */
    var isZipFileExclusionEnabled = fallbackZipFileExclusionEnabled
        get() {
            field = getBoolProperty(pServerPackZipExclusionEnabled, fallbackZipFileExclusionEnabled)
            return field
        }
        set(value) {
            setBoolProperty(pServerPackZipExclusionEnabled, value)
            field = value
            log.info("Zip-file exclusion enabled set to: $field")
        }

    /**
     * Is auto excluding of clientside-only mods enabled.
     */
    var isAutoExcludingModsEnabled = fallbackAutoExcludingModsEnabled
        get() {
            var value = getBoolProperty(pServerPackAutoDiscoveryEnabled, fallbackAutoExcludingModsEnabled)
            try {
                val legacyProp = internalProps.getProperty(pServerPackAutoDiscoveryEnabledLegacy)
                if (legacyProp.matches(trueFalseRegex)) {
                    value = java.lang.Boolean.parseBoolean(legacyProp)
                    internalProps.setProperty(pServerPackAutoDiscoveryEnabled, value.toString())
                    internalProps.remove(pServerPackAutoDiscoveryEnabledLegacy)
                    log.info(
                        "Migrated '$pServerPackAutoDiscoveryEnabledLegacy' to '$pServerPackAutoDiscoveryEnabled'."
                    )
                }
            } catch (ignored: Exception) {
                // No legacy declaration present, so we can safely ignore any exception.
            }
            field = value
            return field
        }
        set(value) {
            setBoolProperty(pServerPackAutoDiscoveryEnabled, value)
            field = value
            log.info("Auto-discovery of clientside-only mods set to: $field")
        }

    /**
     * Whether overwriting of already existing server packs is enabled.
     */
    var isServerPacksOverwriteEnabled = fallbackOverwriteEnabled
        get() {
            field = getBoolProperty(pServerPackOverwriteEnabled, fallbackOverwriteEnabled)
            return field
        }
        set(value) {
            setBoolProperty(pServerPackOverwriteEnabled, value)
            field = value
            log.info("Overwriting of already existing server packs set to: $field")
        }

    /**
     * Whether cleanup procedures after server pack generation are enabled.
     */
    var isServerPackCleanupEnabled = fallbackServerPackCleanupEnabled
        get() {
            field = getBoolProperty(pServerPackCleanupEnabled, fallbackServerPackCleanupEnabled)
            return field
        }
        set(value) {
            setBoolProperty(pServerPackCleanupEnabled, value)
            field = value
            log.info("Cleanup of already existing server packs set to: $field")
        }

    /**
     * Whether Minecraft pre-releases and snapshots are available to the user in, for example, the GUI.
     */
    var isMinecraftPreReleasesAvailabilityEnabled = fallbackMinecraftPreReleasesAvailabilityEnabled
        get() {
            field = getBoolProperty(pAllowUseMinecraftSnapshots, fallbackMinecraftPreReleasesAvailabilityEnabled)
            return field
        }
        set(value) {
            setBoolProperty(pAllowUseMinecraftSnapshots, value)
            field = value
            log.info("Minecraft pre-releases and snapshots available set to: $field")
        }

    /**
     * Whether to automatically update the `SPC_JAVA_SPC`-placeholder in the script variables
     * table with a Java path matching the required Java version for the Minecraft server.
     */
    var isJavaScriptAutoupdateEnabled = fallbackJavaScriptAutoupdateEnabled
        get() {
            field = getBoolProperty(pScriptVariablesAutoUpdateJavaPathsEnabled, fallbackJavaScriptAutoupdateEnabled)
            return field
        }
        set(value) {
            setBoolProperty(pScriptVariablesAutoUpdateJavaPathsEnabled, value)
            field = value
            log.info("Automatically update SPC_JAVA_SPC-placeholder in script variables table set to: $field")
        }

    /**
     * Aikars Flags commonly used for Minecraft servers to improve performance in various places.
     */
    var aikarsFlags: String = fallbackAikarsFlags
        get() {
            field = acquireProperty(pConfigurationAikarsFlags, fallbackAikarsFlags)
            return field
        }
        set(value) {
            defineProperty(pConfigurationAikarsFlags, value)
            field = value
            log.info("Set Aikars flags to: $field.")
        }

    /**
     * Path to the PostgreSQL database used by the webservice-side of ServerPackCreator.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var jdbcDatabaseUrl: String = "jdbc:postgresql://localhost:5432/serverpackcreator"
        get() {
            var dbPath = internalProps.getProperty(pSpringDatasourceUrl, "")
            if (dbPath.isEmpty()) {
                dbPath = "jdbc:postgresql://localhost:5432/serverpackcreator"
            }
            internalProps.setProperty(pSpringDatasourceUrl, dbPath)
            field = dbPath
            return field
        }
        set(value) {
            if (!value.startsWith("jdbc:postgresql:")) {
                internalProps.setProperty(pSpringDatasourceUrl, "jdbc:postgresql:$value")
            } else {
                internalProps.setProperty(pSpringDatasourceUrl, value)
            }
            field = internalProps.getProperty(pSpringDatasourceUrl)
            log.info("Set database url to: $field.")
            log.warn("Restart ServerPackCreator for this change to take effect.")
        }

    /**
     * Language used by ServerPackCreator.
     */
    var language = Locale("en", "GB")
        get() {
            val prop = internalProps.getProperty(pLanguage)
            val lang = if (prop.contains("_")) {
                val split = prop.split("_")
                if (split.size == 3) {
                    Locale(split[0], split[1], split[2])
                } else {
                    Locale(split[0], split[1])
                }
            } else {
                Locale(prop)
            }
            field = lang
            i18n4kConfig.locale = field
            return field
        }
        set(value) {
            internalProps.setProperty(pLanguage, value.toTag())
            i18n4kConfig.locale = value
            field = value
            log.info("Language set to: ${field.displayLanguage} (${field.toTag()}).")
        }

    /**
     * URL to the HasteBin server where logs and configs are uploaded to.
     */
    var hasteBinServerUrl = "https://haste.zneix.eu/documents"
        get() {
            field = acquireProperty(pConfigurationHasteBinServerUrl, "https://haste.zneix.eu/documents")
            return field
        }
        set(value) {
            defineProperty(pConfigurationHasteBinServerUrl, value)
            field = value
            log.info("HasteBin documents endpoint set to: $field")
        }

    /**
     * Java installation used for installing the modloader server during server pack creation.
     */
    var javaPath = "java"
        get() {
            val prop = internalProps.getProperty(pJavaForServerInstall, "")
            field = if (checkJavaPath(prop)) {
                prop
            } else {
                val acquired = acquireJavaPath()
                internalProps.setProperty(pJavaForServerInstall, acquired)
                acquired
            }
            return field
        }
        set(value) {
            if (checkJavaPath(value)) {
                internalProps.setProperty(pJavaForServerInstall, value)
                field = value
                log.info("Java path set to: $field")
            } else {
                log.error("Invalid Java path specified: $value")
            }
        }

    var webserviceCleanupSchedule: String
        get() {
            return internalProps.getProperty("de.griefed.serverpackcreator.spring.schedules.database.cleanup")
        }
        set(value) {
            internalProps.setProperty("de.griefed.serverpackcreator.spring.schedules.database.cleanup", value)
        }

    var webserviceVersionSchedule: String
        get() {
            return internalProps.getProperty("de.griefed.serverpackcreator.spring.schedules.versions.refresh")
        }
        set(value) {
            internalProps.setProperty("de.griefed.serverpackcreator.spring.schedules.versions.refresh", value)
        }

    var webserviceDatabaseCleanupSchedule: String
        get() {
            return internalProps.getProperty("de.griefed.serverpackcreator.spring.schedules.files.cleanup")
        }
        set(value) {
            internalProps.setProperty("de.griefed.serverpackcreator.spring.schedules.files.cleanup", value)
        }

    fun defaultWebserviceDatabase(): String {
        return "jdbc\\:postgresql\\://localhost\\:5432/serverpackcreator"
    }

    /**
     * Default home-directory for ServerPackCreator. If there's no user-home, then the directory containing the
     * ServerPackCreator JAR will be used as the home-directory for ServerPackCreator.
     *
     * @author Griefed
     */
    fun defaultHomeDirectory(): File {
        return File(home, "ServerPackCreator").absoluteFile
    }

    /**
     * ServerPackCreators home directory, in which all important files and folders are stored in.
     *
     * Changes made to this variable are stored in an overrides.properties inside the installation directory of the
     * ServerPackCreator application.
     *
     * Every operation is based on this home-directory, with the exception being the
     * [serverPacksDirectory], which can be configured independently of ServerPackCreators
     * home-directory.
     */
    var homeDirectory: File = File(home, "ServerPackCreator").absoluteFile
        get() {
            val prop = internalProps.getProperty(pHomeDirectory)
            field = if (internalProps.containsKey(pHomeDirectory) && File(prop).absoluteFile.isDirectory) {
                File(prop).absoluteFile
            } else if (jarInformation.jarPath.toFile().isDirectory) {
                // Dev environment
                File("").absoluteFile
            } else {
                File(home, "ServerPackCreator").absoluteFile
            }
            if (!field.isDirectory) {
                field.createDirectories(create = true, directory = true)
            }
            return field
        }
        set(value) {
            internalProps.setProperty(pHomeDirectory, value.absolutePath)
            userPreferences.put(pHomeDirectory, value.absolutePath)
            field = value.absoluteFile
            log.info("Home directory set to: $field")
            log.warn("Restart ServerPackCreator for this change to take full effect.")
        }

    /**
     * The `serverpackcreator.properties`-file which both resulted from starting
     * ServerPackCreator and provided the settings, properties and configurations for the currently
     * running instance.
     */
    var serverPackCreatorPropertiesFile: File = File(homeDirectory, serverPackCreatorProperties).absoluteFile
        get() {
            field = File(homeDirectory, serverPackCreatorProperties).absoluteFile
            return field
        }
        private set

    /**
     * Default configuration-file for a server pack generation inside ServerPackCreators
     * home-directory.
     */
    var defaultConfig: File = File(homeDirectory, "serverpackcreator.conf").absoluteFile
        get() {
            field = File(homeDirectory, "serverpackcreator.conf").absoluteFile
            return field
        }
        private set

    /**
     * Directory in which ServerPackCreator configurations from the GUI get saved in by default.
     */
    var configsDirectory: File = File(homeDirectory, "configs").absoluteFile
        get() {
            field = File(homeDirectory, "configs").absoluteFile
            return field
        }
        private set

    /**
     * Base-directory for Tomcat, used by the webservice-side of ServerPackCreator.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var tomcatBaseDirectory: File = homeDirectory
        get() {
            val prop = internalProps.getProperty(pTomcatBaseDirectory, homeDirectory.absolutePath)
            val dir = if (prop != homeDirectory.absolutePath) {
                internalProps.setProperty(pTomcatBaseDirectory, homeDirectory.absolutePath)
                homeDirectory.absolutePath
            } else {
                internalProps.getProperty(pTomcatBaseDirectory, homeDirectory.absolutePath)
            }
            field = File(dir).absoluteFile
            return field
        }
        set(value) {
            internalProps.setProperty(pTomcatBaseDirectory, value.absolutePath)
            field = value.absoluteFile
            log.info("Set Tomcat base-directory to: $field")
        }

    fun defaultTomcatBaseDirectory(): File {
        return homeDirectory.absoluteFile
    }

    fun defaultServerPacksDirectory(): File {
        return File(homeDirectory, "server-packs").absoluteFile
    }

    /**
     * Directory in which generated server packs, or server packs being generated, are stored in, as
     * well as their ZIP-archives, if created.
     *
     * By default, this directory will be the `server-packs`-directory in the home-directory of
     * ServerPackCreator, but it can be configured using the property
     * `de.griefed.serverpackcreator.configuration.directories.serverpacks` and can even be
     * configured to be completely independent of ServerPackCreators home-directory.
     */
    var serverPacksDirectory: File = File(homeDirectory, "server-packs")
        get() {
            val prop = internalProps.getProperty(pConfigurationDirectoriesServerPacks)
            val directory: File = if (prop.isNullOrBlank() || prop.matches(serverPacksRegex)) {
                defaultServerPacksDirectory()
            } else {
                File(internalProps.getProperty(pConfigurationDirectoriesServerPacks))
            }
            if (field.absolutePath != directory.absolutePath) {
                field = directory
            }
            return field
        }
        set(value) {
            internalProps.setProperty(pConfigurationDirectoriesServerPacks, value.absolutePath)
            field = value.absoluteFile
            log.info("Server packs directory set to: $field")
        }

    /**
     * Storage location for logs created by ServerPackCreator. This is the `logs`-directory
     * inside ServerPackCreators home-directory.
     */
    var logsDirectory: File = File(homeDirectory, "logs").absoluteFile
        get() {
            field = File(homeDirectory, "logs").absoluteFile
            return field
        }
        private set

    /**
     * Logs-directory for Tomcat, used by the webservice-side of ServerPackCreator.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var tomcatLogsDirectory: File = logsDirectory
        get() {
            val default = File(homeDirectory, "logs").absolutePath
            val dir = internalProps.getProperty(pTomcatLogsDirectory, default)
            field = File(dir).absoluteFile
            return field
        }
        set(value) {
            internalProps.setProperty(pTomcatLogsDirectory, value.absolutePath)
            field = value.absoluteFile
            log.info("Set Tomcat logs-directory to: $field")
        }

    fun defaultTomcatLogsDirectory(): File {
        return File(homeDirectory, "logs").absoluteFile
    }

    /**
     * Directory to which default/fallback manifests are copied to during the startup of
     * ServerPackCreator.
     *
     * When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] is initialized, the
     * manifests copied to this directory will provide ServerPackCreator with the information required
     * to check and create your server packs.
     *
     * By default, this is the `manifests`-directory inside ServerPackCreators home-directory.
     */
    var manifestsDirectory: File = File(homeDirectory, "manifests").absoluteFile
        get() {
            field = File(homeDirectory, "manifests").absoluteFile
            return field
        }
        private set

    /**
     * The Fabric intermediaries manifest containing all required information about Fabrics
     * intermediaries. These intermediaries are used by Quilt, Fabric and LegacyFabric.
     *
     *
     * By default, the `fabric-intermediaries-manifest.json`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    var fabricIntermediariesManifest: File =
        File(manifestsDirectory, "fabric-intermediaries-manifest.json").absoluteFile
        get() {
            field = File(manifestsDirectory, "fabric-intermediaries-manifest.json").absoluteFile
            return field
        }
        private set

    /**
     * The LegacyFabric game version manifest containing information about which Minecraft version
     * LegacyFabric is available for.
     *
     *
     * By default, the `legacy-fabric-game-manifest.json`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    var legacyFabricGameManifest: File = File(manifestsDirectory, "legacy-fabric-game-manifest.json").absoluteFile
        get() {
            field = File(manifestsDirectory, "legacy-fabric-game-manifest.json").absoluteFile
            return field
        }
        private set

    /**
     * LegacyFabric loader manifest containing information about Fabric loader maven versions.
     *
     * By default, the `legacy-fabric-loader-manifest.json`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    var legacyFabricLoaderManifest: File = File(manifestsDirectory, "legacy-fabric-loader-manifest.json").absoluteFile
        get() {
            field = File(manifestsDirectory, "legacy-fabric-loader-manifest.json").absoluteFile
            return field
        }
        private set

    /**
     * LegacyFabric installer manifest containing information about available LegacyFabric installers
     * with which to install a server.
     *
     * By default, the `legacy-fabric-installer-manifest.xml`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    var legacyFabricInstallerManifest: File =
        File(manifestsDirectory, "legacy-fabric-installer-manifest.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "legacy-fabric-installer-manifest.xml").absoluteFile
            return field
        }
        private set

    /**
     * Fabric installer manifest containing information about available Fabric installers with which
     * to install a server.
     *
     * By default, the `fabric-installer-manifest.xml`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    var fabricInstallerManifest: File = File(manifestsDirectory, "fabric-installer-manifest.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "fabric-installer-manifest.xml").absoluteFile
            return field
        }
        private set

    /**
     * Quilt version manifest containing information about available Quilt loader versions.
     *
     * By default, the `quilt-manifest.xml`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var quiltVersionManifest: File = File(manifestsDirectory, "quilt-manifest.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "quilt-manifest.xml").absoluteFile
            return field
        }
        private set

    /**
     * Quilt installer manifest containing information about available Quilt installers with which to
     * install a server.
     *
     * By default, the `quilt-installer-manifest.xml`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    var quiltInstallerManifest: File = File(manifestsDirectory, "quilt-installer-manifest.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "quilt-installer-manifest.xml").absoluteFile
            return field
        }
        private set

    /**
     * Forge version manifest containing information about available Forge loader versions.
     *
     *
     * By default, the `forge-manifest.json`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var forgeVersionManifest: File = File(manifestsDirectory, "forge-manifest.json").absoluteFile
        get() {
            field = File(manifestsDirectory, "forge-manifest.json").absoluteFile
            return field
        }
        private set

    /**
     * NeoForge version manifest containing information about available NeoForge loader versions.
     *
     *
     * By default, the `neoforge-manifest.xml`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var neoForgeVersionManifest: File = File(manifestsDirectory, "neoforge-manifest.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "neoforge-manifest.xml").absoluteFile
            return field
        }
        private set

    /**
     * Fabric version manifest containing information about available Fabric loader versions.
     *
     *
     * By default, the `fabric-manifest.xml`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var fabricVersionManifest: File = File(manifestsDirectory, "fabric-manifest.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "fabric-manifest.xml").absoluteFile
            return field
        }
        private set

    /**
     * Directory to which Minecraft server manifests are copied during the startup of
     * ServerPackCreator.
     *
     * When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] is initialized, the
     * manifests copied to this directory will provide ServerPackCreator with the information required
     * to check and create your server packs.
     *
     * The Minecraft server manifests contain information about the Java version required, the
     * download-URL of the server-JAR and much more.
     *
     * By default, this is the `mcserver`-directory inside the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var minecraftServerManifestsDirectory: File = File(manifestsDirectory, "mcserver").absoluteFile
        get() {
            field = File(manifestsDirectory, "mcserver").absoluteFile
            return field
        }
        private set

    /**
     * Minecraft version manifest containing information about available Minecraft versions.
     *
     * By default, the `minecraft-manifest.json`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var minecraftVersionManifest: File = File(manifestsDirectory, "minecraft-manifest.json").absoluteFile
        get() {
            field = File(manifestsDirectory, "minecraft-manifest.json").absoluteFile
            return field
        }
        private set

    /**
     * Work-directory for storing temporary, non-critical, files and directories.
     *
     * Any file and/or directory inside the work-directory is considered `safe-to-delete`,
     * meaning that it can safely be emptied when ServerPackCreator is not running, without running
     * the risk of corrupting anything. It is not recommended to empty this directory whilst
     * ServerPackCreator is running, as in that case, it may interfere with any currently running
     * operation.
     *
     * By default, this is the `work`-directory inside ServerPackCreators home-directory.
     */
    var workDirectory: File = File(homeDirectory, "work").absoluteFile
        get() {
            field = File(homeDirectory, "work").absoluteFile
            return field
        }
        private set

    /**
     * Caching directory for various types of installers. Mainly used by the version-meta for caching modloaders
     * server installers, but also used as the ServerPackCreator installer cache-directory in certain scenarios.
     *
     * @author Griefed
     */
    var installerCacheDirectory: File = File(workDirectory, "installers").absoluteFile
        get() {
            field = File(workDirectory, "installers").absoluteFile
            return field
        }
        private set

    /**
     * Temp-directory storing files and folders required temporarily during the run of a server pack
     * generation or other operations.
     *
     * One example would be when running ServerPackCreator as a webservice and uploading a zipped
     * modpack for the automatic creation of a server pack from said modpack.
     *
     * Any file and/or directory inside the work-directory is considered `safe-to-delete`,
     * meaning that it can safely be emptied when ServerPackCreator is not running, without running
     * the risk of corrupting anything. It is not recommended to empty this directory whilst
     * ServerPackCreator is running, as in that case, it may interfere with any currently running
     * operation.
     *
     *
     * By default, this directory is `work/temp` inside ServerPackCreators home-directory.
     */
    var tempDirectory: File = File(workDirectory, "temp").absoluteFile
        get() {
            field = File(workDirectory, "temp").absoluteFile
            return field
        }
        private set

    /**
     * Modpacks directory in which uploaded modpack ZIP-archives and extracted modpacks are stored.
     *
     * By default, this is the `modpacks`-directory inside the `temp`-directory inside
     * ServerPackCreators home-directory.
     */
    var modpacksDirectory: File = File(homeDirectory, "modpacks").absoluteFile
        get() {
            field = File(homeDirectory, "modpacks").absoluteFile
            return field
        }
        private set

    /**
     * Directory in which default server-files are stored in.
     *
     * Default server-files are, for example, the `server.properties`, `server-icon.png`,
     * `default_template.sh` and `default_template.ps1`.
     *
     * The properties and icon are placeholders and/or templates for the user to change to their
     * liking, should they so desire. The script-templates serve as a one-size-fits-all template for
     * supporting `Forge`, `Fabric`, `LegacyFabric` and `Quilt`.
     *
     * By default, this directory is `server_files` inside ServerPackCreators home-directory.
     */
    var serverFilesDirectory: File = File(homeDirectory, "server_files").absoluteFile
        get() {
            field = File(homeDirectory, "server_files").absoluteFile
            return field
        }
        private set

    @Suppress("MemberVisibilityCanBePrivate")
    val fallbackScriptTemplates = defaultScriptTemplates().joinToString(",")

    /**
     * Directory in which the properties for quick selection are to be stored in and retrieved from.
     */
    var propertiesDirectory: File = File(serverFilesDirectory, "properties").absoluteFile
        get() {
            field = File(serverFilesDirectory, "properties").absoluteFile
            return field
        }
        private set

    /**
     * Directory in which the icons for quick selection are to be stored in and retrieved from.
     */
    var iconsDirectory: File = File(serverFilesDirectory, "icons").absoluteFile
        get() {
            field = File(serverFilesDirectory, "icons").absoluteFile
            return field
        }
        private set

    /**
     * Default server.properties-file used by Minecraft servers. This file resides in the
     * `server_files`-directory inside ServerPackCreators home-directory.
     */
    var defaultServerProperties: File = File(serverFilesDirectory, "server.properties").absoluteFile
        get() {
            field = File(serverFilesDirectory, "server.properties").absoluteFile
            return field
        }
        private set

    /**
     * Default server-icon.png-file used by Minecraft servers. This file resides in the
     * `server_files`-directory inside ServerPackCreators home-directory.
     */
    var defaultServerIcon: File = File(serverFilesDirectory, "server-icon.png").absoluteFile
        get() {
            field = File(serverFilesDirectory, "server-icon.png").absoluteFile
            return field
        }
        private set

    /**
     * Directory in which plugins for ServerPackCreator are to be placed in.
     *
     * This directory not only holds any potential plugins for ServerPackCreator, but also contains the
     * directory in which plugin-specific config-files are stored in, as well as the
     * `disabled.txt`-file, which allows a user to disable any installed plugin.
     *
     *
     * By default, this is the `plugins`-directory inside the ServerPackCreator home-directory.
     */
    var pluginsDirectory: File = File(homeDirectory, "plugins").absoluteFile
        get() {
            field = File(homeDirectory, "plugins").absoluteFile
            return field
        }
        private set

    /**
     * Directory in which plugin-specific configurations are stored in.
     *
     * When ServerPackCreator starts and loads all available plugins, it will also extract a plugins
     * config-file, if available. This file will be stored inside the config-directory using the ID of
     * the plugin as its name, with `.toml` appended to it. Think of this like the
     * config-directory in a modded Minecraft server. Do the names of the config-files there look
     * familiar to the mods they belong to? Well, they should!
     *
     * By default, this is the `config`-directory inside the `plugins`-directory inside
     * ServerPackCreators home-directory.
     */
    var pluginsConfigsDirectory: File = File(pluginsDirectory, "config").absoluteFile
        get() {
            field = File(pluginsDirectory, "config").absoluteFile
            return field
        }
        private set

    /**
     * Load the [propertiesFile] into the provided [props]
     *
     * @author Griefed
     */
    private fun loadFile(propertiesFile: File, props: Properties) {
        if (!propertiesFile.isFile) {
            log.warn("Properties-file does not exist: ${propertiesFile.absolutePath}.")
            return
        }
        props.entries.removeIf { entry -> entry.value.toString().isBlank() }
        try {
            val tempProps = Properties()
            propertiesFile.inputStream().use {
                tempProps.load(it)
            }
            tempProps.entries.removeIf { entry -> entry.value.toString().isBlank() }
            for ((key, value) in tempProps.entries) {
                props[key] = value
            }
            log.info("Loaded properties from $propertiesFile.")
        } catch (ex: Exception) {
            log.error("Couldn't read properties from ${propertiesFile.absolutePath}.", ex)
        }
    }

    private fun loadOverrides() : Properties {
        val tempProps = Properties()
        val overrides = File(homeDirectory,"overrides.properties")
        if (overrides.isFile) {
            File(homeDirectory,"overrides.properties").inputStream().use {
                tempProps.load(it)
            }
        }
        return tempProps
    }

    /**
     * Load properties using the default file path.
     * Only call this method on an already initialized ApiProperties-object.
     *
     * @author Griefed
     */
    fun loadProperties(saveProps: Boolean = true) {
        loadProperties(serverPackCreatorPropertiesFile, saveProps)
    }

    /**
     * Reload from a specific properties-file.
     *
     * @param propertiesFile The properties-file with which to loadProperties the settings and
     * configuration.
     * @author Griefed
     */
    fun loadProperties(propertiesFile: File = File(serverPackCreatorProperties), saveProps: Boolean = true) {
        val props = Properties()
        val jarFolderFile = File(jarInformation.jarFolder.absoluteFile, serverPackCreatorProperties).absoluteFile
        val serverPackCreatorHomeDir = File(home, "ServerPackCreator").absoluteFile
        val homeDirFile = File(serverPackCreatorHomeDir, serverPackCreatorProperties).absoluteFile
        val relativeDirFile = File(serverPackCreatorProperties).absoluteFile

        // Load the properties file from the classpath, providing default values.
        try {
            javaClass.getResourceAsStream("/$serverPackCreatorProperties").use {
                props.load(it)
            }
            log.info("Loaded properties from classpath.")
        } catch (ex: Exception) {
            log.error("Couldn't read properties from classpath.", ex)
        }

        // If our properties-file exists in SPCs home directory, load it.
        loadFile(jarFolderFile, props)
        // If our properties-file exists in the users home dir ServerPackCreator-dir, load it.
        loadFile(homeDirFile, props)
        // If our properties-file in the directory from which the user is executing SPC exists, load it.
        loadFile(relativeDirFile, props)
        // Load the specified properties-file.
        loadFile(propertiesFile, props)

        // Load all values from the overrides-properties
        for (key in userPreferences.keys()) {
            props[key] = userPreferences[key,null]
        }

        internalProps.putAll(props)
        internalProps.setProperty(pTomcatBaseDirectory, homeDirectory.absolutePath)
        if (internalProps.getProperty(pLanguage) != "en_GB") {
            changeLocale(Locale(internalProps.getProperty(pLanguage)))
        }

        val overrides = loadOverrides()
        for ((key,value) in overrides) {
            log.warn("Overriding:")
            log.warn("  $key")
            log.warn("  $value")
        }
        internalProps.putAll(overrides)

        if (updateFallback()) {
            log.info("Fallback lists updated.")
        } else {
            setFallbackModsList()
            setFallbackWhitelist()
        }
        if (saveProps) {
            //Store properties in the configured SPC home-directory
            saveProperties(serverPackCreatorPropertiesFile)
        }
    }

    /**
     * Set up our fallback list of clientside-only mods.
     *
     * @author Griefed
     */
    private fun setFallbackModsList() {
        // Regular list
        clientsideMods.addAll(
            getListProperty(
                pConfigurationFallbackModsList,
                fallbackMods.joinToString(",")
            )
        )
        internalProps.setProperty(
            pConfigurationFallbackModsList,
            clientsideMods.joinToString(",")
        )
    }

    /**
     * Set up our fallback list of clientside-only mods.
     *
     * @author Griefed
     */
    private fun setFallbackWhitelist() {
        // Regular list
        modsWhitelist.addAll(
            getListProperty(
                pConfigurationFallbackModsWhiteList,
                fallbackModsWhitelist.joinToString(",")
            )
        )
        internalProps.setProperty(
            pConfigurationFallbackModsWhiteList,
            fallbackModsWhitelist.joinToString(",")
        )
    }

    /**
     * Get a property from our ApplicationProperties. If the property is not available, it is created
     * with the specified value, thus allowing subsequent calls.
     *
     * @param key          The key of the property to acquire.
     * @param defaultValue The default value for the specified key in case the key is not present or
     * empty.
     * @return The value stored in the specified key.
     * @author Griefed
     */
    private fun acquireProperty(key: String, defaultValue: String) =
        if (internalProps.getProperty(key).isNullOrBlank()) {
            defineProperty(key, defaultValue)
        } else {
            internalProps.getProperty(key, defaultValue)
        }

    /**
     * Set a property in our ApplicationProperties.
     *
     * @param key   The key in which to store the property.
     * @param value The value to store in the specified key.
     * @return The [value] to which the [key] was set to.
     * @author Griefed
     */
    private fun defineProperty(key: String, value: String): String {
        internalProps.setProperty(key, value)
        return value
    }

    /**
     * Get a list from our properties.
     *
     * @param key          The key of the property which holds the comma-separated list.
     * @param defaultValue The default value to set the property to in case it is undefined.
     * @return The requested list.
     * @author Griefed
     */
    private fun getListProperty(
        key: String,
        defaultValue: String
    ) = if (acquireProperty(key, defaultValue).contains(",")) {
        acquireProperty(key, defaultValue)
            .split(",")
            .dropLastWhile { it.isEmpty() }
    } else {
        listOf(acquireProperty(key, defaultValue))
    }

    /**
     * Join the [value] via usage of the [separator] and overwrite the [key].
     * @author Griefed
     */
    @Suppress("SameParameterValue")
    private fun setListProperty(key: String, value: List<String>, separator: String) {
        internalProps.setProperty(key, value.joinToString(separator))
    }

    /**
     * Get an integer from our properties.
     *
     * @param key          The key of the property which holds the comma-separated list.
     * @param defaultValue The default value to set the property to in case it is undefined.
     * @return The requested integer.
     * @author Griefed
     */
    @Suppress("SameParameterValue")
    private fun getIntProperty(key: String, defaultValue: Int) =
        try {
            acquireProperty(key, defaultValue.toString()).toInt()
        } catch (ex: NumberFormatException) {
            defineProperty(key, defaultValue.toString())
            defaultValue
        }

    /**
     * Set the integer property with the given [key] to the given [value].
     *
     * @author Griefed
     */
    @Suppress("SameParameterValue")
    private fun setIntProperty(key: String, value: Int) = defineProperty(key, value.toString())

    /**
     * Get a list of files from our properties, with each file having a specific prefix.
     *
     * @param key          The key of the property which holds the comma-separated list.
     * @param defaultValue The default value to set the property to in case it is undefined.
     * @param filePrefix   The prefix every file should receive.
     * @return The requested list of files.
     * @author Griefed
     */
    @Suppress("SameParameterValue")
    private fun getFileListProperty(key: String, defaultValue: String, filePrefix: String): List<File> {
        val files: MutableList<File> = ArrayList(4)
        val entries = getListProperty(key, defaultValue)
        for (entry in entries) {
            files.add(File(filePrefix + entry))
        }
        return files
    }

    /**
     * Get a boolean from our properties.
     *
     * @param key          The key of the property which holds the comma-separated list.
     * @param defaultValue The default value to set the property to in case it is undefined.
     * @return The requested integer.
     * @author Griefed
     */
    private fun getBoolProperty(key: String, defaultValue: Boolean) =
        acquireProperty(key, defaultValue.toString()).toBoolean()

    /**
     * Set the integer property with the given [key] to the given [value].
     *
     * @author Griefed
     */
    private fun setBoolProperty(key: String, value: Boolean) = defineProperty(key, value.toString())

    /**
     * Adder for the list of directories to exclude from server packs.
     *
     * @param entry The directory to add to the list of directories to exclude from server packs.
     * @author Griefed
     */
    private fun addDirectoryToExclude(entry: String) {
        if (!directoriesToInclude.contains(entry) && directoriesToExclude.add(entry)) {
            log.debug("Adding $entry to list of files or directories to exclude.")
        }
    }

    /**
     * Check the given path to a Java installation for validity and return it, if it is valid. If the
     * passed path is a UNIX symlink or Windows lnk, it is resolved, then returned. If the passed path
     * is considered invalid, the system default is acquired and returned.
     *
     * @param pathToJava The path to check for whether it is a valid Java installation.
     * @return Returns the path to the Java installation. If user input was incorrect, SPC will try to
     * acquire the path automatically.
     * @author Griefed
     */
    fun acquireJavaPath(pathToJava: String = ""): String {
        var checkedJavaPath: String
        try {
            if (pathToJava.isNotEmpty()) {
                if (checkJavaPath(pathToJava)) {
                    return pathToJava
                }
                if (checkJavaPath("$pathToJava.exe")) {
                    return "$pathToJava.exe"
                }
                if (checkJavaPath("$pathToJava.lnk")) {
                    return fileUtilities.resolveLink(File("$pathToJava.lnk"))
                }
            }
            checkedJavaPath = systemUtilities.acquireJavaPathFromSystem()
            log.debug("Acquired path to Java installation: $checkedJavaPath")
        } catch (ex: NullPointerException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = systemUtilities.acquireJavaPathFromSystem()
            log.debug("Automatically acquired path to Java installation: $checkedJavaPath", ex)
        } catch (ex: InvalidFileTypeException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = systemUtilities.acquireJavaPathFromSystem()
            log.debug("Automatically acquired path to Java installation: $checkedJavaPath", ex)
        } catch (ex: IOException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = systemUtilities.acquireJavaPathFromSystem()
            log.debug("Automatically acquired path to Java installation: $checkedJavaPath", ex)
        }
        return checkedJavaPath
    }

    /**
     * Store the ApplicationProperties to disk, overwriting the existing one.
     *
     * @param propertiesFile The file to store the properties to.
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun saveProperties(propertiesFile: File) {
        cleanupInternalProps()
        try {
            propertiesFile.outputStream().use {
                internalProps.store(
                    it,
                    "For details about each property, see https://help.serverpackcreator.de/settings-and-configs.html"
                )
            }
            log.info("Saved properties to: $propertiesFile")
        } catch (ex: IOException) {
            log.error("Couldn't write properties-file.", ex)
        }
    }

    /**
     * Write the overrides-properties which, as the name implies, will override any other property loaded previously during [loadProperties].
     * CAUTION: Depending on the type of installation, the overrides.properties will reside inside a directory which
     * requires root/admin-privileges to write in. The directory in which this file will be created in is [getJarFolder].
     *
     * @author Griefed
     */
    fun saveOverrides() {
        userPreferences.sync()
    }

    /**
     * Removes unwanted properties. Called during the save-operation, to ensure that legacy-properties are removed.
     *
     * @author Griefed
     */
    private fun cleanupInternalProps() {
        internalProps.remove(pConfigurationFallbackModsListRegex)
    }

    /**
     * Check whether the given path is a valid Java specification.
     *
     * @param pathToJava Path to the Java executable
     * @return `true` if the path is valid.
     * @author Griefed
     */
    private fun checkJavaPath(pathToJava: String): Boolean {
        if (pathToJava.isEmpty()) {
            return false
        }
        if (checkedJavas.containsKey(pathToJava)) {
            return checkedJavas[pathToJava]!!
        }
        val result: Boolean
        when (fileUtilities.checkFileType(pathToJava)) {
            FileType.FILE -> {
                result = testJava(pathToJava)
            }

            FileType.LINK, FileType.SYMLINK -> {
                result = try {
                    testJava(fileUtilities.resolveLink(File(pathToJava)))
                } catch (ex: InvalidFileTypeException) {
                    log.error("Could not read Java link/symlink.", ex)
                    false
                } catch (ex: IOException) {
                    log.error("Could not read Java link/symlink.", ex)
                    false
                }
            }

            FileType.DIRECTORY -> {
                log.error("Directory specified. Path to Java must lead to a lnk, symlink or file.")
                result = false
            }

            FileType.INVALID -> result = false
        }
        checkedJavas[pathToJava] = result
        return result
    }

    /**
     * Test for a valid Java specification by trying to run `java -version`. If the command goes
     * through without errors, it is considered a correct specification.
     *
     * @param pathToJava Path to the java executable/binary.
     * @return `true` if the specified file is a valid Java executable/binary.
     * @author Griefed
     */
    private fun testJava(pathToJava: String): Boolean {
        val testSuccessful: Boolean = try {
            val processBuilder = ProcessBuilder(listOf(pathToJava, "-version"))
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            while (bufferedReader.readLine() != null && bufferedReader.readLine() != "null") {
                println(bufferedReader.readLine())
            }
            bufferedReader.close()
            process.destroyForcibly()
            true
        } catch (e: IOException) {
            log.error("Invalid Java specified.")
            false
        }
        return testSuccessful
    }

    /**
     * Whether a viable path to a Java executable or binary has been configured for
     * ServerPackCreator.
     *
     * @return `true` if a viable path has been set.
     * @author Griefed
     */
    fun javaAvailable() = checkJavaPath(javaPath)

    /**
     * Writes the specified locale from -lang your_locale to a lang.properties file to ensure every
     * subsequent start of serverpackcreator is executed using said locale.
     *
     * @param locale The locale the user specified when they ran serverpackcreator with -lang
     * -your_locale.
     * @author Griefed
     */
    fun changeLocale(locale: Locale) {
        language = locale
        saveProperties(serverPackCreatorPropertiesFile)
        log.info("Changed locale to $language")
    }

    /**
     * Acquire the default fallback list of clientside-only mods. If
     * `de.griefed.serverpackcreator.serverpack.autodiscovery.filter` is set to
     * [ExclusionFilter.REGEX], a regex fallback list is returned.
     *
     * @return The fallback list of clientside-only mods.
     * @author Griefed
     */
    fun clientSideMods() =
        if (exclusionFilter == ExclusionFilter.REGEX) {
            clientsideModsRegex.toList()
        } else {
            clientsideMods.toList()
        }

    /**
     * Acquire the default fallback list of whitelisted mods. If
     * `de.griefed.serverpackcreator.serverpack.autodiscovery.filter` is set to
     * [ExclusionFilter.REGEX], a regex fallback list is returned.
     *
     * @return The fallback list of whitelisted mods.
     * @author Griefed
     */
    fun whitelistedMods() =
        if (exclusionFilter == ExclusionFilter.REGEX) {
            modsWhitelistRegex.toList()
        } else {
            modsWhitelist.toList()
        }

    /**
     * Update the fallback clientside-only mod-list of our `serverpackcreator.properties` from
     * the main-repository or one of its mirrors.
     *
     * @return `true` if the fallback-property was updated.
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun updateFallback(): Boolean {
        var properties: Properties? = null
        try {
            URI(
                acquireProperty(pConfigurationFallbackUpdateURL, fallbackUpdateURL)
            ).toURL().openStream().use {
                properties = Properties()
                properties!!.load(it)
            }
        } catch (e: IOException) {
            log.debug("GitHub could not be reached.", e)
        }
        var updated = false
        if (properties != null) {
            if (properties!!.getProperty(pConfigurationFallbackModsList) != null &&
                internalProps.getProperty(pConfigurationFallbackModsList)
                != properties!!.getProperty(pConfigurationFallbackModsList)
            ) {
                internalProps.setProperty(
                    pConfigurationFallbackModsList,
                    properties!!.getProperty(pConfigurationFallbackModsList)
                )
                clientsideMods.clear()
                clientsideMods.addAll(internalProps.getProperty(pConfigurationFallbackModsList).split(","))
                log.info("The fallback-list for clientside only mods has been updated to: $clientsideMods")
                updated = true
            }
            if (properties!!.getProperty(pConfigurationFallbackModsWhiteList) != null &&
                internalProps.getProperty(pConfigurationFallbackModsWhiteList)
                != properties!!.getProperty(pConfigurationFallbackModsWhiteList)
            ) {
                internalProps.setProperty(
                    pConfigurationFallbackModsWhiteList,
                    properties!!.getProperty(pConfigurationFallbackModsWhiteList)
                )
                modsWhitelist.clear()
                modsWhitelist.addAll(internalProps.getProperty(pConfigurationFallbackModsWhiteList).split(","))
                log.info("The fallback-list for whitelisted mods has been updated to: $modsWhitelist")
                updated = true
            }
        }
        if (updated) {
            saveProperties(File(homeDirectory, serverPackCreatorProperties).absoluteFile)
        }
        return updated
    }

    /**
     * Store a custom property in the serverpackcreator.properties-file. Beware that every property you add
     * receives a prefix, to prevent clashes with any other properties.
     *
     * Said prefix consists of `custom.property.` followed by the property you specified coming in last.
     *
     * Say you have a value in the property `saved`, then the resulting property in the serverpackcreator.properties
     * would be:
     * * `custom.property.saved`
     *
     * @author Griefed
     */
    fun storeCustomProperty(property: String, value: String): String {
        val customProp = "$customPropertyPrefix$property"
        return defineProperty(customProp, value)
    }

    /**
     * Retrieve a custom property in the serverpackcreator.properties-file. Beware that every property you retrieve this
     * way contains a prefix, to prevent clashes with any other properties.
     *
     * Said prefix consists of `custom.property.` followed by the property you specified coming in last.
     *
     * Say you have a property `saved`, then the resulting property in the serverpackcreator.properties would be:
     * `custom.property.saved`
     *
     * @author Griefed
     */
    fun retrieveCustomProperty(property: String): String? {
        val customProp = "$customPropertyPrefix$property"
        return internalProps.getProperty(customProp)
    }

    /**
     * Get the path to the specified Java executable/binary, wrapped in an [Optional] for your
     * convenience.
     *
     * @param javaVersion The Java version to acquire the path for.
     * @return The path to the Java executable/binary, if available.
     * @author Griefed
     */
    fun javaPath(javaVersion: Int) =
        if (javaPaths.containsKey(javaVersion.toString())
            && javaPaths[javaVersion.toString()]?.let { File(it).isFile } == true
        ) {
            Optional.ofNullable(javaPaths[javaVersion.toString()])
        } else {
            Optional.empty()
        }

    /**
     * Get the path to the specified Java executable/binary, wrapped in an [Optional] for your
     * convenience.
     *
     * @param javaVersion The Java version to acquire the path for.
     * @return The path to the Java executable/binary, if available.
     * @author Griefed
     */
    fun javaPath(javaVersion: String) = javaPath(javaVersion.toInt())

    /**
     * Set the old version of ServerPackCreator used to perform necessary migrations between the old
     * and the current version.
     *
     * @param version Old version used before upgrading to the current version.
     * @author Griefed
     */
    fun setOldVersion(version: String) {
        internalProps.setProperty(pOldVersion, version)
        saveProperties(serverPackCreatorPropertiesFile)
    }

    /**
     * Get the old version of ServerPackCreator used to perform necessary migrations between the old
     * and the current version.
     *
     * @return Old version used before updating. Empty if this is the first run of ServerPackCreator.
     */
    fun oldVersion(): String = internalProps.getProperty(pOldVersion, "")

    init {
        serverFilesDirectory.createDirectories(create = true, directory = true)
        propertiesDirectory.createDirectories(create = true, directory = true)
        iconsDirectory.createDirectories(create = true, directory = true)
        configsDirectory.createDirectories(create = true, directory = true)
        workDirectory.createDirectories(create = true, directory = true)
        tempDirectory.createDirectories(create = true, directory = true)
        modpacksDirectory.createDirectories(create = true, directory = true)
        serverPacksDirectory.createDirectories(create = true, directory = true)
        pluginsDirectory.createDirectories(create = true, directory = true)
        pluginsConfigsDirectory.createDirectories(create = true, directory = true)
        manifestsDirectory.createDirectories(create = true, directory = true)
        minecraftServerManifestsDirectory.createDirectories(create = true, directory = true)
        installerCacheDirectory.createDirectories(create = true, directory = true)
        printSettings()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun printSettings() {
        log.info("============================== PROPERTIES ==============================")
        log.info("Set Aikars flags to:   $aikarsFlags")
        log.info("Set database path to:  $jdbcDatabaseUrl")
        log.info("Home directory set to: $homeDirectory")
        log.info("Language set to:       ${language.displayLanguage} (${language.toTag()})")
        log.info("Java path set to:      $javaPath")
        log.info("Set Tomcat base-directory to:       $tomcatBaseDirectory")
        log.info("Server packs directory set to:      $serverPacksDirectory")
        log.info("Set Tomcat logs-directory to:       $tomcatLogsDirectory")
        log.info("Checking for pre-releases set to:   $isCheckingForPreReleasesEnabled")
        log.info("Zip-file exclusion enabled set to:  $isZipFileExclusionEnabled")
        log.info("HasteBin documents endpoint set to: $hasteBinServerUrl")
        log.info("Directories which must always be included set to: $directoriesToInclude")
        log.info("Directories which must always be excluded set to: $directoriesToExclude")
        log.info("Cleanup of already existing server packs set to:  $isServerPackCleanupEnabled")
        log.info("Auto-discovery of clientside-only mods set to:    $isAutoExcludingModsEnabled")
        log.info("Overwriting of already existing server packs set to:        $isServerPacksOverwriteEnabled")
        log.info("Minecraft pre-releases and snapshots available set to:      $isMinecraftPreReleasesAvailabilityEnabled")
        log.info("Files which must be excluded from ZIP-archives set to:      $zipArchiveExclusions")
        log.info("User specified clientside-only mod exclusion filter set to: $exclusionFilter")
        log.info("Automatically update SPC_JAVA_SPC-placeholder in script variables table set to: $isJavaScriptAutoupdateEnabled")
        log.info("Clientside-mods set to:")
        listUtilities.printListToLogChunked(clientsideMods.toList(), 5, "    ", true)
        log.info("Regex clientside-mods-list set to:")
        listUtilities.printListToLogChunked(clientsideModsRegex.toList(), 5, "    ", true)
        log.info("Available Java paths for scripts:")
        for ((key, value) in javaPaths) {
            log.info("    Java $key path: $value")
        }
        log.info("Using script templates:")
        for (template in scriptTemplates) {
            log.info("    " + template.path)
        }
        log.info("============================== PROPERTIES ==============================")
    }

    init {
        System.setProperty("user.dir",homeDirectory.absolutePath)
        saveProperties(File(homeDirectory, serverPackCreatorProperties).absoluteFile)
    }

    actual companion object {
        /**
         * @author Griefed
         */
        @JvmStatic
        actual fun getSeparator(): String {
            return File.separator
        }
    }
}