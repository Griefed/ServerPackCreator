/* Copyright (C) 2023  Griefed
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
import org.jetbrains.annotations.Contract
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import java.util.concurrent.Executors

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
    private val log = cachedLoggerOf(this.javaClass)
    private val internalProperties = Properties()
    private val jarInformation: JarInformation = JarInformation(ApiProperties::class.java, jarUtilities)
    private val trueFalseRegex = "^(true|false)$".toRegex()
    private val alphaBetaRegex = "^(.*alpha.*|.*beta.*)$".toRegex()
    private val serverPacksRegex = "^(?:\\./)?server-packs$".toRegex()
    private val installCacheRegex = "^(?:\\./)?installers$".toRegex()
    private val imageRegex = ".*\\.([Pp][Nn][Gg]|[Jj][Pp][Gg]|[Jj][Pp][Ee][Gg]|[Bb][Mm][Pp])".toRegex()
    private val pVersionCheckPreRelease =
        "de.griefed.serverpackcreator.versioncheck.prerelease"
    private val pLanguage =
        "de.griefed.serverpackcreator.language"
    private val pConfigurationFallbackModsList =
        "de.griefed.serverpackcreator.configuration.fallbackmodslist"
    private val pConfigurationFallbackModsListRegex =
        "de.griefed.serverpackcreator.configuration.fallbackmodslist.regex"
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
    private val pSpringArtemisQueueMaxDiskUsage =
        "de.griefed.serverpackcreator.spring.artemis.queue.max_disk_usage"
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

    @Suppress("SpellCheckingInspection")
    private var fallbackModsString =
        "3dskinlayers-,Absolutely-Not-A-Zoom-Mod-,AdvancedChat-,AdvancedChatCore-,AdvancedChatHUD-,AdvancedCompas-,Ambience,AmbientEnvironment-,AmbientSounds_,AreYouBlind-,Armor Status HUD-,ArmorSoundTweak-,BH-Menu-,Batty's Coordinates PLUS Mod,BetterAdvancements-,BetterAnimationsCollection-,BetterDarkMode-,BetterF3-,BetterFoliage-,BetterPingDisplay-,BetterPlacement-,BetterTaskbar-,BetterThirdPerson,BetterTitleScreen-,Blur-,BorderlessWindow-,CTM-,ChunkAnimator-,ClientTweaks_,Controller Support-,Controlling-,CraftPresence-,CustomCursorMod-,CustomMainMenu-,DefaultOptions_,DefaultSettings-,DeleteWorldsToTrash-,DetailArmorBar-,Ding-,DistantHorizons-,DripSounds-,Durability101-,DurabilityNotifier-,DynamicSurroundings-,DynamicSurroundingsHuds-,EffectsLeft-,EiraMoticons_,EnchantmentDescriptions-,EnhancedVisuals_,EquipmentCompare-,FPS-Monitor-,FabricCustomCursorMod-,Fallingleaves-,FancySpawnEggs,FancyVideo-API-,FirstPersonMod,FogTweaker-,ForgeCustomCursorMod-,FpsReducer-,FpsReducer2-,FullscreenWindowed-,GameMenuModOption-,HealthOverlay-,HorseStatsMod-,InventoryEssentials_,InventoryHud_[1.17.1].forge-,InventorySpam-,InventoryTweaks-,ItemBorders-,ItemPhysicLite_,ItemStitchingFix-,JBRA-Client-,JustEnoughCalculation-,JustEnoughEffects-,JustEnoughProfessions-,LLOverlayReloaded-,LOTRDRP-,LegendaryTooltips,LegendaryTooltips-,LightOverlay-,MoBends,MouseTweaks-,MyServerIsCompatible-,Neat ,Neat-,NekosEnchantedBooks-,NoAutoJump-,NoFog-,Notes-,NotifMod-,OldJavaWarning-,OptiFine,OptiFine_,OptiForge,OptiForge-,PackMenu-,PackModeMenu-,PickUpNotifier-,Ping-,PresenceFootsteps-,RPG-HUD-,ReAuth-,ResourceLoader-,ResourcePackOrganizer,ShoulderSurfing-,ShulkerTooltip-,SimpleDiscordRichPresence-,SimpleWorldTimer-,SoundFilters-,SpawnerFix-,TRansliterationLib-,TipTheScales-,Tips-,Toast Control-,Toast-Control-,ToastControl-,TravelersTitles-,VoidFog-,WindowedFullscreen-,WorldNameRandomizer-,[1.12.2]DamageIndicatorsMod-,[1.12.2]bspkrscore-,antighost-,anviltooltipmod-,appleskin-,armorchroma-,armorpointspp-,auditory-,authme-,auto-reconnect-,autojoin-,autoreconnect-,axolotl-item-fix-,backtools-,bannerunlimited-,beenfo-1.19-,better-recipe-book-,betterbiomeblend-,bhmenu-,blur-,borderless-mining-,catalogue-,charmonium-,chat_heads-,cherishedworlds-,cirback-1.0-,classicbar-,clickadv-,clienttweaks-,combat_music-,configured-,controllable-,cullleaves-,cullparticles-,custom-crosshair-mod-,customdiscordrpc-,darkness-,dashloader-,defaultoptions-,desiredservers-,discordrpc-,drippyloadingscreen-,drippyloadingscreen_,dynamic-fps-,dynamic-music-,dynamiclights-,dynmus-,effective-,eggtab-,eguilib-,eiramoticons-,enchantment-lore-,entity-texture-features-,entityculling-,exhaustedstamina-,extremesoundmuffler-,fabricemotes-,fancymenu_,fancymenu_video_extension,findme-,flickerfix-,fm_audio_extension_,forgemod_VoxelMap-,freelook-,galacticraft-rpc-,gamestagesviewer-,grid-,helium-,hiddenrecipebook_,infinitemusic-,inventoryprofiles,invtweaks-,itemzoom,itlt-,jeed-,jehc-,jeiintegration_,just-enough-harvestcraft-,justenoughbeacons-,justenoughdrags-,justzoom_,keymap-,keywizard-,konkrete_,konkrete_forge_,lazydfu-,light-overlay-,lightfallclient-,loadmyresources_,lock_minecart_view-,lootbeams-,lwl-,magnesium_extras-,maptooltip-,massunbind,mcbindtype-,mcwifipnp-,medievalmusic-,mightyarchitect-,mindful-eating-,minetogether-,mobplusplus-,modcredits-,modernworldcreation_,modmenu-,modnametooltip-,modnametooltip_,moreoverlays-,mousewheelie-,movement-vision-,multihotbar-,music-duration-reducer-,musicdr-,neiRecipeHandlers-,ngrok-lan-expose-mod-,nopotionshift_,notenoughanimations-,oculus-,ornaments-,overloadedarmorbar-,panorama-,paperdoll-,phosphor-,preciseblockplacing-,realm-of-lost-souls-,rebrand-,replanter-,rubidium-,rubidium_extras-,screenshot-to-clipboard-,shutupexperimentalsettings-,shutupmodelloader-,signtools-,simple-rpc-,simpleautorun-,smartcursor-,smoothboot-,smoothfocus-,sounddeviceoptions-,soundreloader-,spoticraft-,tconplanner-,timestamps-,tooltipscroller-,torchoptimizer-,torohealth-,totaldarkness,toughnessbar-,wisla-,xlifeheartcolors-,yisthereautojump-"

    @Suppress("SpellCheckingInspection")
    private var fallbackModsRegex =
        "^3dskinlayers-.*$,^Absolutely-Not-A-Zoom-Mod-.*$,^AdvancedChat-.*$,^AdvancedChatCore-.*$,^AdvancedChatHUD-.*$,^AdvancedCompas-.*$,^Ambience.*$,^AmbientEnvironment-.*$,^AmbientSounds_.*$,^AreYouBlind-.*$,^Armor Status HUD-.*$,^ArmorSoundTweak-.*$,^BH-Menu-.*$,^Batty's Coordinates PLUS Mod.*$,^BetterAdvancements-.*$,^BetterAnimationsCollection-.*$,^BetterDarkMode-.*$,^BetterF3-.*$,^BetterFoliage-.*$,^BetterPingDisplay-.*$,^BetterPlacement-.*$,^BetterTaskbar-.*$,^BetterThirdPerson.*$,^BetterTitleScreen-.*$,^Blur-.*$,^BorderlessWindow-.*$,^CTM-.*$,^ChunkAnimator-.*$,^ClientTweaks_.*$,^Controller Support-.*$,^Controlling-.*$,^CraftPresence-.*$,^CustomCursorMod-.*$,^CustomMainMenu-.*$,^DefaultOptions_.*$,^DefaultSettings-.*$,^DeleteWorldsToTrash-.*$,^DetailArmorBar-.*$,^Ding-.*$,^DistantHorizons-.*$,^DripSounds-.*$,^Durability101-.*$,^DurabilityNotifier-.*$,^DynamicSurroundings-.*$,^DynamicSurroundingsHuds-.*$,^EffectsLeft-.*$,^EiraMoticons_.*$,^EnchantmentDescriptions-.*$,^EnhancedVisuals_.*$,^EquipmentCompare-.*$,^FPS-Monitor-.*$,^FabricCustomCursorMod-.*$,^Fallingleaves-.*$,^FancySpawnEggs.*$,^FancyVideo-API-.*$,^FirstPersonMod.*$,^FogTweaker-.*$,^ForgeCustomCursorMod-.*$,^FpsReducer-.*$,^FpsReducer2-.*$,^FullscreenWindowed-.*$,^GameMenuModOption-.*$,^HealthOverlay-.*$,^HorseStatsMod-.*$,^InventoryEssentials_.*$,^InventoryHud_[1.17.1].forge-.*$,^InventorySpam-.*$,^InventoryTweaks-.*$,^ItemBorders-.*$,^ItemPhysicLite_.*$,^ItemStitchingFix-.*$,^JBRA-Client-.*$,^JustEnoughCalculation-.*$,^JustEnoughEffects-.*$,^JustEnoughProfessions-.*$,^LLOverlayReloaded-.*$,^LOTRDRP-.*$,^LegendaryTooltips-.*$,^LegendaryTooltips.*$,^LightOverlay-.*$,^MoBends.*$,^MouseTweaks-.*$,^MyServerIsCompatible-.*$,^Neat .*$,^Neat-.*$,^NekosEnchantedBooks-.*$,^NoAutoJump-.*$,^NoFog-.*$,^Notes-.*$,^NotifMod-.*$,^OldJavaWarning-.*$,^OptiFine.*$,^OptiFine_.*$,^OptiForge-.*$,^OptiForge.*$,^PackMenu-.*$,^PackModeMenu-.*$,^PickUpNotifier-.*$,^Ping-.*$,^PresenceFootsteps-.*$,^RPG-HUD-.*$,^ReAuth-.*$,^ResourceLoader-.*$,^ResourcePackOrganizer.*$,^ShoulderSurfing-.*$,^ShulkerTooltip-.*$,^SimpleDiscordRichPresence-.*$,^SimpleWorldTimer-.*$,^SoundFilters-.*$,^SpawnerFix-.*$,^TRansliterationLib-.*$,^TipTheScales-.*$,^Tips-.*$,^Toast Control-.*$,^Toast-Control-.*$,^ToastControl-.*$,^TravelersTitles-.*$,^VoidFog-.*$,^WindowedFullscreen-.*$,^WorldNameRandomizer-.*$,^[1.12.2]DamageIndicatorsMod-.*$,^[1.12.2]bspkrscore-.*$,^antighost-.*$,^anviltooltipmod-.*$,^appleskin-.*$,^armorchroma-.*$,^armorpointspp-.*$,^auditory-.*$,^authme-.*$,^auto-reconnect-.*$,^autojoin-.*$,^autoreconnect-.*$,^axolotl-item-fix-.*$,^backtools-.*$,^bannerunlimited-.*$,^beenfo-1.19-.*$,^better-recipe-book-.*$,^betterbiomeblend-.*$,^bhmenu-.*$,^blur-.*$,^borderless-mining-.*$,^catalogue-.*$,^charmonium-.*$,^chat_heads-.*$,^cherishedworlds-.*$,^cirback-1.0-.*$,^classicbar-.*$,^clickadv-.*$,^clienttweaks-.*$,^combat_music-.*$,^configured-.*$,^controllable-.*$,^cullleaves-.*$,^cullparticles-.*$,^custom-crosshair-mod-.*$,^customdiscordrpc-.*$,^darkness-.*$,^dashloader-.*$,^defaultoptions-.*$,^desiredservers-.*$,^discordrpc-.*$,^drippyloadingscreen-.*$,^drippyloadingscreen_.*$,^dynamic-fps-.*$,^dynamic-music-.*$,^dynamiclights-.*$,^dynmus-.*$,^effective-.*$,^eggtab-.*$,^eguilib-.*$,^eiramoticons-.*$,^enchantment-lore-.*$,^entity-texture-features-.*$,^entityculling-.*$,^exhaustedstamina-.*$,^extremesoundmuffler-.*$,^fabricemotes-.*$,^fancymenu_.*$,^fancymenu_video_extension.*$,^findme-.*$,^flickerfix-.*$,^fm_audio_extension_.*$,^forgemod_VoxelMap-.*$,^freelook-.*$,^galacticraft-rpc-.*$,^gamestagesviewer-.*$,^grid-.*$,^helium-.*$,^hiddenrecipebook_.*$,^infinitemusic-.*$,^inventoryprofiles.*$,^invtweaks-.*$,^itemzoom.*$,^itlt-.*$,^jeed-.*$,^jehc-.*$,^jeiintegration_.*$,^just-enough-harvestcraft-.*$,^justenoughbeacons-.*$,^justenoughdrags-.*$,^justzoom_.*$,^keymap-.*$,^keywizard-.*$,^konkrete_.*$,^konkrete_forge_.*$,^lazydfu-.*$,^light-overlay-.*$,^lightfallclient-.*$,^loadmyresources_.*$,^lock_minecart_view-.*$,^lootbeams-.*$,^lwl-.*$,^magnesium_extras-.*$,^maptooltip-.*$,^massunbind.*$,^mcbindtype-.*$,^mcwifipnp-.*$,^medievalmusic-.*$,^mightyarchitect-.*$,^mindful-eating-.*$,^minetogether-.*$,^mobplusplus-.*$,^modcredits-.*$,^modernworldcreation_.*$,^modmenu-.*$,^modnametooltip-.*$,^modnametooltip_.*$,^moreoverlays-.*$,^mousewheelie-.*$,^movement-vision-.*$,^multihotbar-.*$,^music-duration-reducer-.*$,^musicdr-.*$,^neiRecipeHandlers-.*$,^ngrok-lan-expose-mod-.*$,^nopotionshift_.*$,^notenoughanimations-.*$,^oculus-.*$,^ornaments-.*$,^overloadedarmorbar-.*$,^panorama-.*$,^paperdoll-.*$,^phosphor-.*$,^preciseblockplacing-.*$,^realm-of-lost-souls-.*$,^rebrand-.*$,^replanter-.*$,^rubidium-.*$,^rubidium_extras-.*$,^screenshot-to-clipboard-.*$,^shutupexperimentalsettings-.*$,^shutupmodelloader-.*$,^signtools-.*$,^simple-rpc-.*$,^simpleautorun-.*$,^smartcursor-.*$,^smoothboot-.*$,^smoothfocus-.*$,^sounddeviceoptions-.*$,^soundreloader-.*$,^spoticraft-.*$,^tconplanner-.*$,^timestamps-.*$,^tooltipscroller-.*$,^torchoptimizer-.*$,^torohealth-.*$,^totaldarkness.*$,^toughnessbar-.*$,^wisla-.*$,^xlifeheartcolors-.*$,^yisthereautojump-.*$"

    @Suppress("SpellCheckingInspection")
    private var fallbackDirectoriesInclusionString =
        "addonpacks,blueprints,config,configs,customnpcs,defaultconfigs,global_data_packs,global_packs,kubejs,maps,mods,openloader,scripts,schematics,shrines-saves,structures,structurize,worldshape,Zoestria"

    @Suppress("SpellCheckingInspection")
    private var fallbackDirectoriesExclusionString =
        "animation,asm,cache,changelogs,craftpresence,crash-reports,downloads,icons,libraries,local,logs,overrides,packmenu,profileImage,profileImage,resourcepacks,screenshots,server_pack,shaderpacks,simple-rpc,tv-cache"
    private var fallbackZipExclusionsString =
        "minecraft_server.MINECRAFT_VERSION.jar,server.jar,libraries/net/minecraft/server/MINECRAFT_VERSION/server-MINECRAFT_VERSION.jar"
    private val checkedJavas = hashMapOf<String, Boolean>()
    val i18n4kConfig = I18n4kConfigDefault()

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
    private var fallbackScriptTemplates = "${defaultShellScriptTemplate.name},${defaultPowerShellScriptTemplate.name}"

    /**
     * Directories to include in a server pack.
     */
    @get:Contract(pure = true)
    var directoriesToInclude = TreeSet(fallbackDirectoriesInclusionString.split(","))
        private set

    /**
     * Directories to exclude from a server pack.
     */
    @get:Contract(pure = true)
    var directoriesToExclude = TreeSet(fallbackDirectoriesExclusionString.split(","))
        private set

    /**
     * List of files to be excluded from ZIP-archives. Current filters are:
     *
     *  * `MINECRAFT_VERSION` - Will be replaced with the Minecraft version of the server pack
     *  * `MODLOADER` - Will be replaced with the modloader of the server pack
     *  * `MODLOADER_VERSION` - Will be replaced with the modloader version of the server pack
     *
     * Should you want these filters to be expanded, open an issue on [GitHub](https://github.com/Griefed/ServerPackCreator/issues)
     */
    @get:Contract(pure = true)
    var zipArchiveExclusions = TreeSet(fallbackZipExclusionsString.split(","))
        private set

    /**
     * String-list of clientside-only mods to exclude from server packs.
     */
    @get:Contract(pure = true)
    var clientsideMods = TreeSet(fallbackModsString.split(","))
        private set

    /**
     * Regex-list of clientside-only mods to exclude from server packs.
     */
    @get:Contract(pure = true)
    var clientsideModsRegex = TreeSet(fallbackModsRegex.split(","))
        private set

    /**
     * Default SPC properties.
     */
    @get:Contract(pure = true)
    val serverPackCreatorProperties =
        "serverpackcreator.properties"

    /**
     * Modloaders supported by ServerPackCreator.
     */
    @get:Contract(pure = true)
    var supportedModloaders = arrayOf("Fabric", "Forge", "Quilt", "LegacyFabric")
        private set

    /**
     * Paths to Java installations available to SPC for automatically updating the script variables of a given server pack
     * configuration.
     * * key: Java version
     * * value: Path to the Java .exe or binary
     */
    @get:Contract(pure = true)
    val javaPaths = HashMap<String, String>(256)

    /**
     * Start-script templates to use during server pack generation.
     */
    @get:Contract(pure = true)
    var scriptTemplates: TreeSet<File> = TreeSet<File>()
        private set

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
    @get:Contract(pure = true)
    var exclusionFilter = ExclusionFilter.START
        private set

    /**
     * The version of the ServerPackCreator API.
     */
    @get:Contract(pure = true)
    val apiVersion: String = this.javaClass.getPackage().implementationVersion ?: "dev"

    /**
     * Whether the last loaded configuration file should be saved to as well.
     *
     * @return Whether the last loaded configuration file should be saved to as well.
     * @author Griefed
     */
    @get:Contract(pure = true)
    var isSavingOfLastLoadedConfEnabled = false
        private set

    /**
     * Whether the search for available PreReleases is enabled or disabled. Depending on
     * `de.griefed.serverpackcreator.versioncheck.prerelease`, returns `true` if checks for available PreReleases are
     * enabled, `false` if no checks for available PreReleases should be made.
     */
    @get:Contract(pure = true)
    var isCheckingForPreReleasesEnabled = false
        private set

    /**
     * Whether the exclusion of files from the ZIP-archive of the server pack is enabled.
     */
    @get:Contract(pure = true)
    var isZipFileExclusionEnabled = true
        private set

    /**
     * Is auto excluding of clientside-only mods enabled.
     */
    @get:Contract(pure = true)
    var isAutoExcludingModsEnabled = true
        private set

    /**
     * Whether overwriting of already existing server packs is enabled.
     */
    @get:Contract(pure = true)
    var isServerPacksOverwriteEnabled = true
        private set

    /**
     * Whether cleanup procedures after server pack generation are enabled.
     */
    @get:Contract(pure = true)
    var isServerPackCleanupEnabled = true
        private set

    /**
     * Whether Minecraft pre-releases and snapshots are available to the user in, for example, the GUI.
     */
    @get:Contract(pure = true)
    var isMinecraftPreReleasesAvailabilityEnabled = false
        private set

    /**
     * Whether to automatically update the `SPC_JAVA_SPC`-placeholder in the script variables
     * table with a Java path matching the required Java version for the Minecraft server.
     */
    @get:Contract(pure = true)
    var isJavaScriptAutoupdateEnabled = true
        private set

    /**
     * Aikars Flags commonly used for Minecraft servers to improve performance in various places.
     */
    @Suppress("SpellCheckingInspection")
    @get:Contract(pure = true)
    var aikarsFlags: String =
        "-Xms4G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true"
        private set

    /**
     * Language used by ServerPackCreator.
     */
    @get:Contract(pure = true)
    var language = Locale("en", "GB")
        private set

    /**
     * URL to the HasteBin server where logs and configs are uploaded to.
     */
    @get:Contract(pure = true)
    var hasteBinServerUrl = "https://haste.zneix.eu/documents"
        private set

    /**
     * Java installation used for installing the modloader server during server pack creation.
     */
    @get:Contract(pure = true)
    var javaPath = "java"

    /**
     * Maximum disk usage in percent until Artemis stops accepting new entries.
     */
    @get:Contract(pure = true)
    var queueMaxDiskUsage = 90
        private set

    /**
     * Is the Dark Theme currently active?
     *
     * @return `true` if the Dark Theme is active, otherwise false.
     * @author Griefed
     */
    fun isDarkTheme() = acquireProperty(pGuiDarkMode, "true").toBoolean()

    /**
     * ServerPackCreators home directory, in which all important files and folders are stored in.
     *
     * Stored in `serverpackcreator.properties` under the
     * `de.griefed.serverpackcreator.home`- property.
     *
     * Every operation is based on this home-directory, with the exception being the
     * [serverPacksDirectory], which can be configured independently of ServerPackCreators
     * home-directory.
     */
    lateinit var homeDirectory: File

    /**
     * Directory in which generated server packs, or server packs being generated, are stored in, as
     * well as their ZIP-archives, if created.
     *
     * By default, this directory will be the `server-packs`-directory in the home-directory of
     * ServerPackCreator, but it can be configured using the property
     * `de.griefed.serverpackcreator.configuration.directories.serverpacks` and can even be
     * configured to be completely independent of ServerPackCreators home-directory.
     */
    lateinit var serverPacksDirectory: File

    /**
     * The `serverpackcreator.properties`-file which both resulted from starting
     * ServerPackCreator and provided the settings, properties and configurations for the currently
     * running instance.
     */
    val serverPackCreatorPropertiesFile: File

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
    val minecraftServerManifestsDirectory: File

    /**
     * The Fabric intermediaries manifest containing all required information about Fabrics
     * intermediaries. These intermediaries are used by Quilt, Fabric and LegacyFabric.
     *
     *
     * By default, the `fabric-intermediaries-manifest.json`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    val fabricIntermediariesManifest: File

    /**
     * The LegacyFabric game version manifest containing information about which Minecraft version
     * LegacyFabric is available for.
     *
     *
     * By default, the `legacy-fabric-game-manifest.json`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    val legacyFabricGameManifest: File

    /**
     * LegacyFabric loader manifest containing information about Fabric loader maven versions.
     *
     * By default, the `legacy-fabric-loader-manifest.json`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    val legacyFabricLoaderManifest: File

    /**
     * LegacyFabric installer manifest containing information about available LegacyFabric installers
     * with which to install a server.
     *
     * By default, the `legacy-fabric-installer-manifest.xml`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    val legacyFabricInstallerManifest: File

    /**
     * Fabric installer manifest containing information about available Fabric installers with which
     * to install a server.
     *
     * By default, the `fabric-installer-manifest.xml`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    val fabricInstallerManifest: File

    /**
     * Quilt version manifest containing information about available Quilt loader versions.
     *
     * By default, the `quilt-manifest.xml`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    val quiltVersionManifest: File

    /**
     * Quilt installer manifest containing information about available Quilt installers with which to
     * install a server.
     *
     * By default, the `quilt-installer-manifest.xml`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    val quiltInstallerManifest: File

    /**
     * Forge version manifest containing information about available Forge loader versions.
     *
     *
     * By default, the `forge-manifest.json`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    val forgeVersionManifest: File

    /**
     * Fabric version manifest containing information about available Fabric loader versions.
     *
     *
     * By default, the `fabric-manifest.xml`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    val fabricVersionManifest: File

    /**
     * Minecraft version manifest containing information about available Minecraft versions.
     *
     * By default, the `minecraft-manifest.json`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    val minecraftVersionManifest: File

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
    val manifestsDirectory: File

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
    val workDirectory: File

    /**
     * Modpacks directory in which uploaded modpack ZIP-archives and extracted modpacks are stored.
     *
     * By default, this is the `modpacks`-directory inside the `temp`-directory inside
     * ServerPackCreators home-directory.
     */
    val modpacksDirectory: File

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
    val tempDirectory: File

    /**
     * Default configuration-file for a server pack generation inside ServerPackCreators
     * home-directory.
     */
    val defaultConfig: File

    /**
     * Default server.properties-file used by Minecraft servers. This file resides in the
     * `server_files`-directory inside ServerPackCreators home-directory.
     */
    val defaultServerProperties: File

    /**
     * Default server-icon.png-file used by Minecraft servers. This file resides in the
     * `server_files`-directory inside ServerPackCreators home-directory.
     */
    val defaultServerIcon: File

    /**
     * The database used by the webservice-portion of ServerPackCreaot to do store and provide server packs and
     * related information.
     */
    val serverPackCreatorDatabase: File

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
    val pluginsConfigsDirectory: File

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
    val pluginsDirectory: File

    /**
     * Caching directory for various types of installers. Mainly used by the version-meta for caching modloaders
     * server installers, but also used as the ServerPackCreator installer cache-directory in certain scenarios.
     *
     * @author Griefed
     */
    val installerCacheDirectory: File

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
    val serverFilesDirectory: File

    /**
     * Storage location for logs created by ServerPackCreator. This is the `logs`-directory
     * inside ServerPackCreators home-directory.
     */
    val logsDirectory: File

    /**
     * Directory in which the properties for quick selection are to be stored in and retrieved from.
     */
    val propertiesDirectory: File

    /**
     * Directory in which the icons for quick selection are to be stored in and retrieved from.
     */
    val iconsDirectory: File

    /**
     * Directory in which ServerPackCreator configurations from the GUI get saved in by default.
     */
    val configsDirectory: File

    /**
     * List of server properties for quick selection in a given config tab.
     */
    val propertiesQuickSelections: List<String>
        get() {
            val files = propertiesDirectory.listFiles()?.filterNotNull()
                ?.filter { properties -> properties.name.endsWith(".properties") }
                ?.toTypedArray() ?: arrayOf()
            return files.map { file -> file.name }
        }

    /**
     * List of server icons for quick selection in a given config tab.
     */
    val iconQuickSelections: List<String>
        get() {
            val files = iconsDirectory.listFiles()?.filterNotNull()?.filter { icon -> icon.name.matches(imageRegex) }
                ?.toTypedArray() ?: arrayOf()
            return files.map { file -> file.name }
        }

    /**
     * Reload from a specific properties-file.
     *
     * @param propertiesFile The properties-file with which to loadProperties the settings and
     * configuration.
     * @author Griefed
     */
    fun loadProperties(propertiesFile: File = File(serverPackCreatorProperties)) {
        val props = Properties()
        // Load the properties file from the classpath, providing default values.
        try {
            this.javaClass.getResourceAsStream("/$serverPackCreatorProperties").use {
                internalProperties.load(it)
            }
        } catch (ex: Exception) {
            log.error("Couldn't read properties file.", ex)
        }

        // If our properties-file exists in SPCs home directory, load it.
        if (File(jarInformation.jarFolder, serverPackCreatorProperties).exists()) {
            try {
                File(jarInformation.jarFolder, serverPackCreatorProperties).inputStream().use {
                    props.load(it)
                    internalProperties.putAll(props)
                }
            } catch (ex: IOException) {
                log.error("Couldn't read properties file.", ex)
            }
        }

        // If our properties-file in the directory from which the user is executing SPC exists, load it.
        if (File(serverPackCreatorProperties).exists()) {
            try {
                File(serverPackCreatorProperties).inputStream().use {
                    props.load(it)
                    internalProperties.putAll(props)
                }
            } catch (ex: IOException) {
                log.error("Couldn't read properties file.", ex)
            }
        }

        // Load the specified properties-file.
        if (propertiesFile.absoluteFile.exists()) {
            try {
                propertiesFile.absoluteFile.inputStream().use {
                    props.load(it)
                    internalProperties.putAll(props)
                    log.info("Loading file: " + propertiesFile.path)
                }
            } catch (ex: IOException) {
                log.error("Couldn't read properties file.", ex)
            }
        } else {
            log.info(propertiesFile.absoluteFile.path + " does not exist.")
        }
        setHome()
        if (updateFallback()) {
            log.info("Fallback lists updated.")
        } else {
            setFallbackModsList()
        }
        computeServerPacksDir()
        computeDirsToIncludeList()
        computeDirsToExcludeList()
        computeQueueMaxDiskUsage()
        computeSaveLoadedConfiguration()
        computeCheckForPreReleases()
        computeAikarsFlags()
        computeFilesToExcludeFromZip()
        computeZipFileExclusionEnabled()
        computeScriptTemplates()
        computeAutoExclusionOfMods()
        computeServerPackOverwrite()
        computeServerPackCleanup()
        computeLanguage()
        computeHasteBinServerUrl()
        computeMinecraftPreReleases()
        computeModExclusionFilterMethod()
        computeJavaPath()
        computeJavaScriptsVariablePaths()
        computeAutoUpdateScriptVariablesJavaPlaceholder()
        computeDatabaseProperty()
        computeTomcatLogsDirectory()
        computeTomcatBaseDirectory()
        computeArtemisDataDirectory()
        saveToDisk(File(homeDirectory, serverPackCreatorProperties).absoluteFile)
    }

    /**
     * Check and, if needed, change the path to the serverpackcreator-database for the webservice, to
     * be placed in ServerPackCreators home-directory.
     *
     * @author Griefed
     */
    private fun computeDatabaseProperty() {
        var dbPath = internalProperties.getProperty("spring.datasource.url", "").replace("jdbc:sqlite:", "")
        if (dbPath.isEmpty()) {
            dbPath = "jdbc:sqlite:${File(homeDirectory, "serverpackcreator.db").absoluteFile}"
        }
        internalProperties.setProperty("spring.datasource.url", dbPath)
    }

    /**
     * Check and, if needed, change the path to the Tomcat-logs-directory for the webservice, to be
     * placed in ServerPackCreators home-directory.
     *
     * @author Griefed
     */
    @Suppress("SpellCheckingInspection")
    private fun computeTomcatLogsDirectory() =
        internalProperties.setProperty(
            "server.tomcat.accesslog.directory",
            File("${homeDirectory}${File.separator}logs").absolutePath
        )

    /**
     * Check and, if needed, change the path to the Tomcat-base-directory for the webservice, to be
     * placed in ServerPackCreators home-directory.
     *
     * @author Griefed
     */
    private fun computeTomcatBaseDirectory() =
        internalProperties.setProperty("server.tomcat.basedir", homeDirectory.absolutePath)

    /**
     * Check and, if needed, change the path to the Artemis-data-directory for the webservice, to be
     * placed in ServerPackCreators home-directory.
     *
     * @author Griefed
     */
    private fun computeArtemisDataDirectory() =
        internalProperties.setProperty(
            "spring.artemis.embedded.data-directory",
            File(workDirectory, "artemis").absolutePath
        )

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
        acquireProperty(
            key,
            defaultValue
        )
            .split(",")
            .dropLastWhile { it.isEmpty() }
    } else {
        listOf(acquireProperty(key, defaultValue))
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
        val files: MutableList<File> = ArrayList(10)
        for (entry in getListProperty(key, defaultValue)) {
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
     * Set ServerPackCreators home-directory. If ServerPackCreator is run for the first time on a
     * given machine, the current containing directory will be used and saved as the
     * home-directory.
     *
     * If ServerPackCreator has been run before, and the home-directory-property
     * is set, then that directory will be used as ServerPackCreators continued home-directory.
     *
     * @author Griefed
     */
    private fun setHome() {
        homeDirectory = if (internalProperties.containsKey(pHomeDirectory)
            && File(internalProperties.getProperty(pHomeDirectory)).absoluteFile.isDirectory
        ) {
            File(internalProperties.getProperty(pHomeDirectory)).absoluteFile
        } else if (System.getProperty("user.home").isNotEmpty()
            && File(System.getProperty("user.home")).isDirectory
        ) {
            File(System.getProperty("user.home"), "ServerPackCreator").absoluteFile
        } else {
            if (jarInformation.jarFile.isDirectory) {
                File(File("").absolutePath).absoluteFile
            } else {
                jarInformation.jarFolder.absoluteFile
            }
        }
        if (!homeDirectory.isDirectory) {
            homeDirectory.createDirectories(create = true, directory = true)
        }
        internalProperties.setProperty(pHomeDirectory, homeDirectory.absoluteFile.absolutePath)
        log.info("Home directory: $homeDirectory")
    }

    /**
     * Set the directory where the generated server packs will be stored in.
     *
     * @author Griefed
     */
    private fun computeServerPacksDir() {
        serverPacksDirectory = if (internalProperties.getProperty(pConfigurationDirectoriesServerPacks).isEmpty()
            || internalProperties.getProperty(pConfigurationDirectoriesServerPacks)
                .matches(serverPacksRegex)
        ) {
            File(homeDirectory, "server-packs")
        } else {
            File(internalProperties.getProperty(pConfigurationDirectoriesServerPacks))
        }
        internalProperties.setProperty(
            pConfigurationDirectoriesServerPacks,
            serverPacksDirectory.absolutePath
        )
        log.info("Server packs directory set to: $serverPacksDirectory")
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
                fallbackModsString
            )
        )
        internalProperties.setProperty(
            pConfigurationFallbackModsList,
            clientsideMods.joinToString(",")
        )
        log.info("Fallback mods-list set to:")
        this.listUtilities.printListToLogChunked(clientsideMods.toList(), 5, "    ", true)

        // Regex list
        clientsideModsRegex.addAll(
            getListProperty(
                pConfigurationFallbackModsListRegex,
                fallbackModsRegex
            )
        )
        internalProperties.setProperty(
            pConfigurationFallbackModsListRegex,
            clientsideModsRegex.joinToString(",")
        )
        log.info("Fallback regex mods-list set to:")
        this.listUtilities.printListToLogChunked(clientsideModsRegex.toList(), 5, "    ", true)
    }

    /**
     * List of directories which can be excluded from server packs
     *
     * @author Griefed
     */
    private fun computeDirsToExcludeList() {
        for (entry in getListProperty(
            pConfigurationDirectoriesShouldExclude,
            fallbackDirectoriesExclusionString
        )) {
            addDirectoryToExclude(entry)
        }
        log.info("Directories to exclude set to: $directoriesToExclude")
    }

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
     * List of directories which should always be included in a server pack, no matter what the users
     * specify
     *
     * @author Griefed
     */
    private fun computeDirsToIncludeList() {
        directoriesToInclude.addAll(
            getListProperty(
                pConfigurationDirectoriesMustInclude,
                fallbackDirectoriesInclusionString
            )
        )
        log.info("Directories which must always be included set to: $directoriesToInclude")
    }

    /**
     * Max diskspace usage before no more jobs are accepted when running as a webservice.
     *
     * @author Griefed
     */
    private fun computeQueueMaxDiskUsage() {
        val usage = getIntProperty(pSpringArtemisQueueMaxDiskUsage, 90)
        queueMaxDiskUsage = if (usage in 0..100) {
            getIntProperty(pSpringArtemisQueueMaxDiskUsage, 90)
        } else {
            log.error("Invalid max disk usage set. Defaulting to 90")
            90
        }
        log.info("Queue max disk usage set to: $queueMaxDiskUsage")
    }

    /**
     * Whether the last loaded configuration should be saved to as well.
     *
     * @author Griefed
     */
    private fun computeSaveLoadedConfiguration() {
        isSavingOfLastLoadedConfEnabled = getBoolProperty(pConfigurationSaveLastLoadedConfigEnabled, false)
        log.info("Save last loaded config set to: $isSavingOfLastLoadedConfEnabled")
    }

    /**
     * Whether to check for pre-releases as well.
     *
     * @author Griefed
     */
    private fun computeCheckForPreReleases() {
        isCheckingForPreReleasesEnabled = getBoolProperty(pVersionCheckPreRelease, false)
        if (apiVersion.matches(alphaBetaRegex)) {
            isCheckingForPreReleasesEnabled = true
            log.info("Using pre-release $apiVersion. Checking for pre-releases set to true.")
        } else {
            log.info("Set check for pre-releases to: $isCheckingForPreReleasesEnabled")
        }
    }

    /**
     * Aikars flags to set when the user so desires.
     *
     * @author Griefed
     */
    private fun computeAikarsFlags() {
        aikarsFlags = acquireProperty(pConfigurationAikarsFlags, aikarsFlags)
        log.info("Aikars flags set to: $aikarsFlags")
    }

    /**
     * Files or folders which should be excluded from a ZIP-archive.
     *
     * @author Griefed
     */
    private fun computeFilesToExcludeFromZip() {
        zipArchiveExclusions.addAll(
            getListProperty(
                pServerPackZipExclusions,
                fallbackZipExclusionsString
            )
        )
        log.info("Files which must be excluded from ZIP-archives set to: $zipArchiveExclusions")
    }

    /**
     * Files and folders which should be included in a server pack.
     *
     * @author Griefed
     */
    private fun computeZipFileExclusionEnabled() {
        isZipFileExclusionEnabled = getBoolProperty(pServerPackZipExclusionEnabled, true)
        log.info("Zip file exclusion enabled set to: $isZipFileExclusionEnabled")
    }

    /**
     * Script templates to generate start scripts with.
     *
     * @author Griefed
     */
    private fun computeScriptTemplates() {
        scriptTemplates.clear()
        scriptTemplates.addAll(
            getFileListProperty(
                pServerPackScriptTemplates,
                fallbackScriptTemplates,
                homeDirectory.toString() + File.separator + "server_files"
                        + File.separator
            )
        )
        log.info("Using script templates:")
        for (template in scriptTemplates) {
            log.info("    " + template.path)
        }
    }

    /**
     * Whether clientside-only mods should automatically be detected and excluded.
     *
     * @author Griefed
     */
    private fun computeAutoExclusionOfMods() {
        isAutoExcludingModsEnabled = getBoolProperty(pServerPackAutoDiscoveryEnabled, true)

        // Legacy declaration which may still be present in some serverpackcreator.properties-files.
        try {
            if (internalProperties.getProperty(pServerPackAutoDiscoveryEnabledLegacy)
                    .matches(trueFalseRegex)
            ) {
                isAutoExcludingModsEnabled = java.lang.Boolean.parseBoolean(
                    internalProperties.getProperty(pServerPackAutoDiscoveryEnabledLegacy)
                )
                internalProperties.setProperty(pServerPackAutoDiscoveryEnabled, isAutoExcludingModsEnabled.toString())
                internalProperties.remove(pServerPackAutoDiscoveryEnabledLegacy)
                log.info(
                    "Migrated '$pServerPackAutoDiscoveryEnabledLegacy' to '$pServerPackAutoDiscoveryEnabled'."
                )
            }
        } catch (ignored: Exception) {
            // No legacy declaration present, so we can safely ignore any exception.
        }
        log.info("Auto-discovery of clientside-only mods set to: $isAutoExcludingModsEnabled")
    }

    /**
     * Whether existing server packs should be overwritten.
     *
     * @author Griefed
     */
    private fun computeServerPackOverwrite() {
        isServerPacksOverwriteEnabled = getBoolProperty(pServerPackOverwriteEnabled, true)
        log.info("Overwriting of already existing server packs set to: $isServerPacksOverwriteEnabled")
    }

    /**
     * Whether cleanup procedures should be executed after server pack generation.
     *
     * @author Griefed
     */
    private fun computeServerPackCleanup() {
        isServerPackCleanupEnabled = getBoolProperty(pServerPackCleanupEnabled, true)
        log.info("Cleanup of already existing server packs set to: $isServerPackCleanupEnabled")
    }

    /**
     * URL to send logs and configs to for easier issue reporting on GitHub.
     *
     * @author Griefed
     */
    private fun computeHasteBinServerUrl() {
        hasteBinServerUrl = acquireProperty(
            pConfigurationHasteBinServerUrl,
            "https://haste.zneix.eu/documents"
        )
        log.info("HasteBin documents endpoint set to: $hasteBinServerUrl")
    }

    /**
     * Whether Minecraft pre-releases and snapshots should be made available to the user.
     *
     * @author Griefed
     */
    private fun computeMinecraftPreReleases() {
        isMinecraftPreReleasesAvailabilityEnabled = getBoolProperty(pAllowUseMinecraftSnapshots, false)
        log.info("Minecraft pre-releases and snapshots available set to: $isMinecraftPreReleasesAvailabilityEnabled")
    }

    /**
     * Set in which way user-specified clientside-only mods should be excluded.
     *
     * @author Griefed
     */
    private fun computeModExclusionFilterMethod() {
        try {
            val filterText = acquireProperty(pServerPackAutoDiscoveryFilterMethod, "START")
            exclusionFilter = when (filterText) {
                "END" -> ExclusionFilter.END
                "CONTAIN" -> ExclusionFilter.CONTAIN
                "REGEX" -> ExclusionFilter.REGEX
                "EITHER" -> ExclusionFilter.EITHER
                "START" -> ExclusionFilter.START
                else -> {
                    log.error("Invalid filter specified. Defaulting to START.")
                    ExclusionFilter.START
                }
            }
        } catch (ignored: NullPointerException) {
        } finally {
            internalProperties.setProperty(pServerPackAutoDiscoveryFilterMethod, exclusionFilter.toString())
        }
        log.info("User specified clientside-only mod exclusion filter set to: $exclusionFilter")
    }

    /**
     * Set the Java path using the defined property, if it exists. If the setting is incorrect,
     * ServerPackCreator will try to automatically determine the path and set the property
     * accordingly.
     *
     * @author Griefed
     */
    private fun computeJavaPath() {
        javaPath = if (checkJavaPath(internalProperties.getProperty(pJavaForServerInstall, ""))) {
            internalProperties.getProperty(pJavaForServerInstall)
        } else {
            internalProperties.setProperty(pJavaForServerInstall, acquireJavaPath())
            acquireProperty(pJavaForServerInstall, acquireJavaPath())
        }
        log.info("Java path set to: $javaPath")
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
    private fun acquireJavaPath(pathToJava: String = ""): String {
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
                    return this.fileUtilities.resolveLink(File("$pathToJava.lnk"))
                }
            }
            checkedJavaPath = this.systemUtilities.acquireJavaPathFromSystem()
            log.debug("Acquired path to Java installation: $checkedJavaPath")
        } catch (ex: NullPointerException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = this.systemUtilities.acquireJavaPathFromSystem()
            log.debug("Automatically acquired path to Java installation: $checkedJavaPath", ex)
        } catch (ex: InvalidFileTypeException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = this.systemUtilities.acquireJavaPathFromSystem()
            log.debug("Automatically acquired path to Java installation: $checkedJavaPath", ex)
        } catch (ex: IOException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = this.systemUtilities.acquireJavaPathFromSystem()
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
    fun saveToDisk(propertiesFile: File) {
        try {
            propertiesFile.outputStream().use {
                internalProperties.store(
                    it,
                    "For details about each property, see https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-Help#serverpackcreatorproperties"
                )
            }
        } catch (ex: IOException) {
            log.error("Couldn't write properties-file.", ex)
        }
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
            return checkedJavas[javaPath]!!
        }
        val result: Boolean
        when (this.fileUtilities.checkFileType(pathToJava)) {
            FileType.FILE -> {
                result = testJava(pathToJava)
            }

            FileType.LINK, FileType.SYMLINK -> {
                result = try {
                    testJava(this.fileUtilities.resolveLink(File(pathToJava)))
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
        checkedJavas[javaPath] = result
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
     * Sets the path to the Java 17 executable/binary.
     *
     * @author Griefed
     */
    private fun computeJavaScriptsVariablePaths() {
        for (i in 8..255) {
            if (checkJavaPath(internalProperties.getProperty(pScriptVariablesJavaPaths + i, ""))) {
                if (javaPaths.containsKey(i.toString())) {
                    javaPaths.replace(
                        i.toString(),
                        internalProperties.getProperty(pScriptVariablesJavaPaths + i)
                    )
                } else {
                    javaPaths[i.toString()] = internalProperties.getProperty(pScriptVariablesJavaPaths + i)
                }
                internalProperties.setProperty(pScriptVariablesJavaPaths + i, javaPaths[i.toString()])
            }
        }
        log.info("Available Java paths for scripts for local testing and debugging:")
        for ((key, value) in javaPaths) {
            log.info("Java $key path: $value")
        }
    }

    /**
     * Set whether to automatically update the `SPC_JAVA_SPC`-placeholder in the script
     * variables table with a Java path matching the required Java version for the Minecraft server.
     *
     * @author Griefed
     */
    private fun computeAutoUpdateScriptVariablesJavaPlaceholder() {
        isJavaScriptAutoupdateEnabled = getBoolProperty(pScriptVariablesAutoUpdateJavaPathsEnabled, true)
        log.info("Automatically update SPC_JAVA_SPC-placeholder in script variables table set to: $isJavaScriptAutoupdateEnabled")
    }

    /**
     * Writes the specified locale from -lang your_locale to a lang.properties file to ensure every
     * subsequent start of serverpackcreator is executed using said locale.
     *
     * @param locale The locale the user specified when they ran serverpackcreator with -lang
     * -your_locale.
     * @author Griefed
     */
    fun changeLocale(locale: Locale) {
        computeLanguage(locale)
        saveToDisk(serverPackCreatorPropertiesFile)
        log.info("Changed locale to $language")
    }

    /**
     * Overwrite the language used so the next run of ServerPackCreator uses that language setting.
     *
     * @param locale The language to set for the next run.
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun computeLanguage(locale: Locale = Locale("N", "A")) {
        language = if (locale.language != "N" && locale.country != "A") {
            locale
        } else if (internalProperties.getProperty(pLanguage).contains("_")) {
            val split = internalProperties.getProperty(pLanguage).split("_")
            if (split.size == 3) {
                Locale(split[0], split[1], split[2])
            } else {
                Locale(split[0], split[1])
            }
        } else {
            Locale(internalProperties.getProperty(pLanguage))
        }
        i18n4kConfig.locale = language
        internalProperties.setProperty(pLanguage, language.toTag())
        log.info("Language set to: ${language.displayLanguage} (${language.toTag()}).")
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
            clientsideModsRegex.toList() as ArrayList<String>
        } else {
            clientsideMods.toList() as ArrayList<String>
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
            URL(
                "https://raw.githubusercontent.com/Griefed/ServerPackCreator/main/serverpackcreator-api/src/jvmMain/resources/serverpackcreator.properties"
            ).openStream().use {
                properties = Properties()
                properties!!.load(it)
            }
        } catch (e: IOException) {
            log.debug("GitHub could not be reached.", e)
        }
        var updated = false
        if (properties != null) {
            if (properties!!.getProperty(pConfigurationFallbackModsList) != null &&
                internalProperties.getProperty(pConfigurationFallbackModsList)
                != properties!!.getProperty(pConfigurationFallbackModsList)
            ) {
                internalProperties.setProperty(
                    pConfigurationFallbackModsList,
                    properties!!.getProperty(pConfigurationFallbackModsList)
                )
                clientsideMods.clear()
                clientsideMods.addAll(internalProperties.getProperty(pConfigurationFallbackModsList).split(","))
                log.info("The fallback-list for clientside only mods has been updated to: $clientsideMods")
                updated = true
            }
            if (properties!!.getProperty(pConfigurationFallbackModsListRegex) != null
                && internalProperties.getProperty(pConfigurationFallbackModsListRegex)
                != properties!!.getProperty(pConfigurationFallbackModsListRegex)
            ) {
                internalProperties.setProperty(
                    pConfigurationFallbackModsListRegex,
                    properties!!.getProperty(pConfigurationFallbackModsListRegex)
                )
                clientsideModsRegex.clear()
                clientsideModsRegex.addAll(
                    internalProperties.getProperty(pConfigurationFallbackModsListRegex).split(",")
                )
                log.info("The fallback regex-list for clientside only mods has been updated to: $clientsideModsRegex")
                updated = true
            }
        }
        if (updated) {
            saveToDisk(File(homeDirectory, serverPackCreatorProperties).absoluteFile)
        }
        return updated
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
        if (internalProperties.getProperty(key) == null) {
            defineProperty(key, defaultValue)
        } else {
            internalProperties.getProperty(key, defaultValue)
        }


    /**
     * Set a property in our ApplicationProperties.
     *
     * @param key   The key in which to store the property.
     * @param value The value to store in the specified key.
     * @author Griefed
     */
    private fun defineProperty(key: String, value: String): String {
        internalProperties.setProperty(key, value)
        return value
    }

    /**
     * Set the current theme to Dark Theme or Light Theme.
     *
     * @param dark `true` to activate Dark Theme, `false` otherwise.
     * @author Griefed
     */
    fun setTheme(dark: Boolean) {
        if (dark) {
            defineProperty(pGuiDarkMode, "true")
        } else {
            internalProperties.setProperty(pGuiDarkMode, "false")
        }
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
        return internalProperties.getProperty(customProp)
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
        internalProperties.setProperty(pOldVersion, version)
        saveToDisk(serverPackCreatorPropertiesFile)
    }

    /**
     * Get the old version of ServerPackCreator used to perform necessary migrations between the old
     * and the current version.
     *
     * @return Old version used before updating. Empty if this is the first run of ServerPackCreator.
     */
    fun oldVersion(): String = internalProperties.getProperty(pOldVersion, "")

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

    init {
        i18n4k = i18n4kConfig
        i18n4kConfig.defaultLocale = Locale("en_GB")
        loadProperties(propertiesFile)
        serverPackCreatorPropertiesFile = File(homeDirectory, serverPackCreatorProperties).absoluteFile
        logsDirectory = File(homeDirectory, "logs").absoluteFile
        serverFilesDirectory = File(homeDirectory, "server_files").absoluteFile
        propertiesDirectory = File(serverFilesDirectory, "properties").absoluteFile
        iconsDirectory = File(serverFilesDirectory, "icons").absoluteFile
        configsDirectory = File(homeDirectory, "configs").absoluteFile
        pluginsDirectory = File(homeDirectory, "plugins").absoluteFile
        manifestsDirectory = File(homeDirectory, "manifests").absoluteFile
        serverPackCreatorDatabase =
            File(internalProperties.getProperty("spring.datasource.url", "").replace("jdbc:sqlite:", "")).absoluteFile
        defaultConfig = File(homeDirectory, "serverpackcreator.conf").absoluteFile
        workDirectory = File(homeDirectory, "work").absoluteFile
        pluginsConfigsDirectory = File(pluginsDirectory, "config").absoluteFile
        defaultServerIcon = File(serverFilesDirectory, "server-icon.png").absoluteFile
        defaultServerProperties = File(serverFilesDirectory, "server.properties").absoluteFile
        tempDirectory = File(workDirectory, "temp").absoluteFile
        installerCacheDirectory = File(workDirectory, "installers").absoluteFile
        modpacksDirectory = File(tempDirectory, "modpacks").absoluteFile
        minecraftVersionManifest = File(manifestsDirectory, "minecraft-manifest.json").absoluteFile
        minecraftServerManifestsDirectory = File(manifestsDirectory, "mcserver").absoluteFile
        forgeVersionManifest = File(manifestsDirectory, "forge-manifest.json").absoluteFile
        quiltInstallerManifest = File(manifestsDirectory, "quilt-installer-manifest.xml").absoluteFile
        quiltVersionManifest = File(manifestsDirectory, "quilt-manifest.xml").absoluteFile
        fabricVersionManifest = File(manifestsDirectory, "fabric-manifest.xml").absoluteFile
        fabricInstallerManifest = File(manifestsDirectory, "fabric-installer-manifest.xml").absoluteFile
        fabricIntermediariesManifest = File(manifestsDirectory, "fabric-intermediaries-manifest.json").absoluteFile
        legacyFabricInstallerManifest = File(manifestsDirectory, "legacy-fabric-installer-manifest.xml").absoluteFile
        legacyFabricLoaderManifest = File(manifestsDirectory, "legacy-fabric-loader-manifest.json").absoluteFile
        legacyFabricGameManifest = File(manifestsDirectory, "legacy-fabric-game-manifest.json").absoluteFile
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
    }

    actual companion object {
        @JvmStatic
        actual fun getSeparator(): String {
            return File.separator
        }
    }
}