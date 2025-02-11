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
import de.griefed.serverpackcreator.api.config.ExclusionFilter
import de.griefed.serverpackcreator.api.utilities.common.*
import org.apache.logging.log4j.core.Core
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.ConfigurationFactory
import org.apache.logging.log4j.core.config.ConfigurationSource
import org.apache.logging.log4j.core.config.Order
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.xml.XmlConfiguration
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.*
import java.net.URI
import java.net.URL
import java.util.*
import java.util.prefs.Preferences

/**
 * Base settings of ServerPackCreator, such as working directories, default list of clientside-only
 * mods, default list of directories to include in a server pack, script templates, java paths and
 * much more.
 *
 * @param propertiesFile  serverpackcreator.properties-file containing settings and configurations to load the API with.
 * @author Griefed
 */
@Suppress("unused")
@Plugin(name = "ServerPackCreatorConfigFactory", category = Core.CATEGORY_NAME)
@Order(50)
class ApiProperties(propertiesFile: File = File("serverpackcreator.properties")) : ConfigurationFactory() {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val internalProps = Properties()
    private val spcPreferences = Preferences.userRoot().node("ServerPackCreator")
    private val serverPackCreatorProperties = "serverpackcreator.properties"
    private val jarInformation: JarInformation = JarInformation(this.javaClass)
    private val jarFolderProperties: File = File(jarInformation.jarFolder.absoluteFile, serverPackCreatorProperties)

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
    private val pConfigurationDirectoriesServerPacks =
        "de.griefed.serverpackcreator.configuration.directories.serverpacks"
    private val pServerPackCleanupEnabled =
        "de.griefed.serverpackcreator.serverpack.cleanup.enabled"
    private val pServerPackOverwriteEnabled =
        "de.griefed.serverpackcreator.serverpack.overwrite.enabled"
    private val pConfigurationDirectoriesShouldExclude =
        "de.griefed.serverpackcreator.configuration.directories.shouldexclude"
    private val pConfigurationDirectoriesMustInclude =
        "de.griefed.serverpackcreator.configuration.directories.mustinclude"
    private val pServerPackZipExclusions =
        "de.griefed.serverpackcreator.serverpack.zip.exclude"
    private val pServerPackZipExclusionEnabled =
        "de.griefed.serverpackcreator.serverpack.zip.exclude.enabled"
    private val pServerPackStartScriptTemplatesPrefix =
        "de.griefed.serverpackcreator.serverpack.script.template."
    private val pServerPackJavaScriptTemplatesPrefix =
        "de.griefed.serverpackcreator.serverpack.java.template."
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
    private val pSpringDatasourceUsername =
        "spring.datasource.username"
    private val pSpringDatasourcePassword =
        "spring.datasource.password"
    private val pUpdateServerPack = "de.griefed.serverpackcreator.serverpack.update"
    private val pLogLevel = "de.griefed.serverpackcreator.loglevel"

    @Deprecated("Deprecated as of 6.0.0")
    private val pServerPackScriptTemplates =
        "de.griefed.serverpackcreator.serverpack.script.template"

    private val suffixes = arrayOf(".xml")

    private val propertyFiles: MutableList<File> = mutableListOf()

    /**
     * Default home-directory for ServerPackCreator. The directory containing the
     * ServerPackCreator JAR.
     *
     * @author Griefed
     */
    val home: File = jarInformation.jarFolder.absoluteFile

    private var fallbackModsWhitelist = TreeSet(
        listOf(
            "Ping-Wheel-",
            "appleskin-"
        )
    )

    @Suppress("SpellCheckingInspection")
    private var fallbackMods = TreeSet(
        listOf(
            "3dskinlayers-",                //https://www.curseforge.com/minecraft/mc-mods/skin-layers-3d
            "Absolutely-Not-A-Zoom-Mod-",   //https://www.curseforge.com/minecraft/mc-mods/absolutely-not-a-zoom-mod
            "AdvancedChat-",                //https://www.curseforge.com/minecraft/mc-mods/advancedchat
            "AdvancedChatCore-",            //https://www.curseforge.com/minecraft/mc-mods/advancedchatcore
            "AdvancedChatHUD-",             //https://www.curseforge.com/minecraft/mc-mods/advancedchathud
            "AdvancedCompas-",              //https://www.curseforge.com/minecraft/mc-mods/advanced-compass
            "Ambience",                     //https://www.curseforge.com/minecraft/mc-mods/ambience-music-mod
            "AmbientEnvironment-",          //https://www.curseforge.com/minecraft/mc-mods/ambient-environment
            "AmbientSounds_",               //https://www.curseforge.com/minecraft/mc-mods/ambientsounds
            "AnimaticaReforged-",           //https://www.curseforge.com/minecraft/mc-mods/animaticareforged
            "AreYouBlind-",                 //https://www.curseforge.com/minecraft/mc-mods/are-you-blind
            "Armor Status HUD-",            //https://www.curseforge.com/minecraft/mc-mods/armorstatushud
            "ArmorSoundTweak-",             //https://www.curseforge.com/minecraft/mc-mods/armor-sound-tweak
            "BH-Menu-",                     //https://www.curseforge.com/minecraft/mc-mods/bisecthosting-server-integration-menu-forge & https://www.curseforge.com/minecraft/mc-mods/bisecthosting-server-integration-menu-fabric & https://www.curseforge.com/minecraft/mc-mods/bisecthosting-server-integration-menu-neoforge
            "Batty's Coordinates PLUS Mod", //https://www.curseforge.com/minecraft/mc-mods/batty-ui & https://www.curseforge.com/minecraft/mc-mods/battys-ui-mod-forge
            "BetterAdvancements-",          //https://www.curseforge.com/minecraft/mc-mods/better-advancements
            "BetterAnimationsCollection-",  //https://www.curseforge.com/minecraft/mc-mods/better-animations-collection
            "BetterModsButton-",            //https://www.curseforge.com/minecraft/mc-mods/better-mods-button
            "BetterDarkMode-",              //https://www.curseforge.com/minecraft/mc-mods/betterdarkmode
            "BetterF3-",                    //https://www.curseforge.com/minecraft/mc-mods/betterf3
            "BetterFog-",                   //https://www.curseforge.com/minecraft/mc-mods/better-fog
            "BetterFoliage-",               //https://www.curseforge.com/minecraft/mc-mods/better-foliage
            "BetterPingDisplay-",           //https://www.curseforge.com/minecraft/mc-mods/better-ping-display
            "BetterPlacement-",             //https://www.curseforge.com/minecraft/mc-mods/better-placement
            "BetterTaskbar-",               //https://www.curseforge.com/minecraft/mc-mods/better-taskbar
            "BetterThirdPerson",            //https://www.curseforge.com/minecraft/mc-mods/better-third-person
            "BetterTitleScreen-",           //https://www.curseforge.com/minecraft/mc-mods/better-title-screen
            "Blur-",                        //https://www.curseforge.com/minecraft/mc-mods/blur
            "BoccHUD-",                     //https://modrinth.com/mod/bocchud/
            "BorderlessWindow-",            //https://www.curseforge.com/minecraft/mc-mods/borderless
            "CTM-",                         //https://www.curseforge.com/minecraft/mc-mods/ctm
            "Chat Ping ",                   //https://www.curseforge.com/minecraft/mc-mods/chatping
            "ChunkAnimator-",               //https://www.curseforge.com/minecraft/mc-mods/chunk-animator
            "Clear-Water-",                 //https://www.curseforge.com/minecraft/mc-mods/clear-water
            "ClientTweaks_",                //https://www.curseforge.com/minecraft/mc-mods/client-tweaks
            "CompletionistsIndex-",         //https://www.curseforge.com/minecraft/mc-mods/completionists-index
            "Controller Support-",          //https://www.curseforge.com/minecraft/mc-mods/controller-mod
            "Controlling-",                 //https://www.curseforge.com/minecraft/mc-mods/controlling
            "CraftPresence-",               //https://www.curseforge.com/minecraft/mc-mods/craftpresence
            "CullLessLeaves-",              //https://www.curseforge.com/minecraft/mc-mods/cull-less-leaves & https://www.curseforge.com/minecraft/mc-mods/culllessleaves-reforged
            "CustomCursorMod-",             //https://www.curseforge.com/minecraft/mc-mods/custom-cursor
            "CustomMainMenu-",              //https://www.curseforge.com/minecraft/mc-mods/custom-main-menu
            "DefaultOptions_",              //https://www.curseforge.com/minecraft/mc-mods/default-options
            "DefaultSettings-",             //https://www.curseforge.com/minecraft/mc-mods/defaultsettings
            "DeleteWorldsToTrash-",         //https://www.curseforge.com/minecraft/mc-mods/delete-worlds-to-trash-forge
            "DetailArmorBar-",              //https://www.curseforge.com/minecraft/mc-mods/detail-armor-bar-forge
            "Ding-",                        //https://www.curseforge.com/minecraft/mc-mods/ding
            "DistantHorizons-",             //https://www.curseforge.com/minecraft/mc-mods/distant-horizons
            "DripSounds-",                  //https://www.curseforge.com/minecraft/mc-mods/waterdripsound
            "Durability101-",               //https://www.curseforge.com/minecraft/mc-mods/durability101
            "DurabilityNotifier-",          //https://www.curseforge.com/minecraft/mc-mods/durability-notifier
            "DynamicSurroundings-",         //https://www.curseforge.com/minecraft/mc-mods/dynamic-surroundings
            "DynamicSurroundingsHuds-",     //https://www.curseforge.com/minecraft/mc-mods/dynamic-surroundings-huds
            "EasyLAN-",                     //https://www.curseforge.com/minecraft/mc-mods/easylan
            "EffectsLeft-",                 //https://www.curseforge.com/minecraft/mc-mods/effectsleft
            "EiraMoticons_",                //no longer available, legacy entry
            "EnchantmentDescriptions-",     //https://www.curseforge.com/minecraft/mc-mods/enchantment-descriptions
            "EnhancedVisuals_",             //https://www.curseforge.com/minecraft/mc-mods/enhancedvisuals
            "EquipmentCompare-",            //https://www.curseforge.com/minecraft/mc-mods/equipment-compare
            "EuphoriaPatcher-",             //https://www.curseforge.com/minecraft/mc-mods/euphoria-patches
            "FPS-Monitor-",                 //https://www.curseforge.com/minecraft/mc-mods/fps-monitor
            "FabricCustomCursorMod-",       //https://www.curseforge.com/minecraft/mc-mods/cursor-mod
            "FadingNightVision-",           //https://www.curseforge.com/minecraft/mc-mods/fading-night-vision
            "Fallingleaves-",               //https://www.curseforge.com/minecraft/mc-mods/falling-leaves-forge
            "FancySpawnEggs",               //https://www.curseforge.com/minecraft/mc-mods/fancy-spawn-eggs
            "FancyVideo-API-",              //https://www.curseforge.com/minecraft/mc-mods/fancyvideo-api
            "farsight-",                    //https://www.curseforge.com/minecraft/modpacks/farsight
            "FirstPersonMod",               //https://www.curseforge.com/minecraft/mc-mods/first-person-model
            "FogTweaker-",                  //https://www.curseforge.com/minecraft/mc-mods/fog-tweaker
            "ForgeCustomCursorMod-",        //https://www.curseforge.com/minecraft/mc-mods/cursor-mod
            "Forgematica-",                 //https://www.curseforge.com/minecraft/mc-mods/forgematica
            "FpsReducer-",                  //https://www.curseforge.com/minecraft/mc-mods/fps-reducer
            "FpsReducer2-",                 //https://www.curseforge.com/minecraft/mc-mods/fps-reducer
            "FullscreenWindowed-",          //https://www.curseforge.com/minecraft/mc-mods/fullscreen-windowed-borderless-for-minecraft
            "GameMenuModOption-",           //https://www.curseforge.com/minecraft/mc-mods/gamemenumodoption
            "HealthOverlay-",               //https://www.curseforge.com/minecraft/mc-mods/health-overlay
            "HeldItemTooltips-",            //https://www.curseforge.com/minecraft/mc-mods/held-item-tooltips
            "HorseStatsMod-",               //https://www.curseforge.com/minecraft/bukkit-plugins/horsestats
            "ImmediatelyFast-",             //https://www.curseforge.com/minecraft/mc-mods/immediatelyfast
            "ImmediatelyFastReforged-",     //https://www.curseforge.com/minecraft/mc-mods/immediatelyfast-reforged
            "InventoryEssentials_",         //https://www.curseforge.com/minecraft/mc-mods/inventory-essentials
            "InventoryHud_",                //https://www.curseforge.com/minecraft/mc-mods/inventory-hud-forge
            "InventorySpam-",               //https://www.curseforge.com/minecraft/mc-mods/inventory-spam
            "InventoryTweaks-",             //https://www.curseforge.com/minecraft/mc-mods/inventorytweak
            "ItemBorders-",                 //https://www.curseforge.com/minecraft/mc-mods/item-borders
            "ItemLocks-",                   //https://www.curseforge.com/minecraft/mc-mods/itemlocks
            "ItemPhysicLite_",              //https://www.curseforge.com/minecraft/mc-mods/itemphysic-lite
            "ItemStitchingFix-",            //https://www.curseforge.com/minecraft/mc-mods/item-stitching-fix
            "JBRA-Client-",                 //https://www.curseforge.com/minecraft/mc-mods/jingames-jbra-client
            "JustEnoughCalculation-",       //https://www.curseforge.com/minecraft/mc-mods/just-enough-calculation
            "JustEnoughEffects-",           //https://www.curseforge.com/minecraft/mc-mods/just-enough-effects
            "JustEnoughProfessions-",       //https://www.curseforge.com/minecraft/mc-mods/just-enough-professions-jep
            "KeepTheResourcePack-",         //https://www.curseforge.com/minecraft/mc-mods/keep-the-resourcepack
            "LeaveMyBarsAlone-",            //https://www.curseforge.com/minecraft/mc-mods/leave-my-bars-alone
            "LLOverlayReloaded-",           //https://www.curseforge.com/minecraft/mc-mods/light-level-overlay-reloaded
            "LOTRDRP-",                     //https://www.curseforge.com/minecraft/mc-mods/lotr-drp
            "LegendaryTooltips",            //https://www.curseforge.com/minecraft/mc-mods/legendary-tooltips
            "LegendaryTooltips-",           //https://www.curseforge.com/minecraft/mc-mods/legendary-tooltips
            "LightOverlay-",                //https://www.curseforge.com/minecraft/mc-mods/light-level-overlay-display
            "MaFgLib-",                     //https://modrinth.com/mod/mafglib
            "MinecraftCapes ",              //https://www.curseforge.com/minecraft/mc-mods/minecraftcapes-mod
            "MineMenu-",                    //https://www.curseforge.com/minecraft/mc-mods/minemenu
            "MoBends",                      //https://www.curseforge.com/minecraft/mc-mods/mo-bends
            "ModernUI-",                    //Gone? Reduces to atoms?
            "MouseTweaks-",                 //https://www.curseforge.com/minecraft/mc-mods/mouse-tweaks
            "MyServerIsCompatible-",        //https://www.curseforge.com/minecraft/mc-mods/my-server-is-compatible
            "Neat ",                        //https://www.curseforge.com/minecraft/mc-mods/neat
            "Neat-",                        //https://www.curseforge.com/minecraft/mc-mods/neat
            "NekosEnchantedBooks-",         //https://www.curseforge.com/minecraft/mc-mods/nekos-enchanted-books
            "NoAutoJump-",                  //https://www.curseforge.com/minecraft/mc-mods/no-autojump
            "NoFog-",                       //https://www.curseforge.com/minecraft/mc-mods/nofog
            "Notes-",                       //https://www.curseforge.com/minecraft/mc-mods/notes
            "NotifMod-",                    //https://www.curseforge.com/minecraft/mc-mods/notifmod
            "OldJavaWarning-",              //https://www.curseforge.com/minecraft/mc-mods/oldjavawarning
            "OptiFine",                     //https://optifine.net/home
            "OptiFine_",                    //https://optifine.net/home
            "OptiForge",                    //https://www.curseforge.com/minecraft/mc-mods/optiforge
            "OptiForge-",                   //https://www.curseforge.com/minecraft/mc-mods/optiforge
            "OverflowingBars-",             //https://www.curseforge.com/minecraft/mc-mods/overflowing-bars
            "PackMenu-",                    //https://www.curseforge.com/minecraft/mc-mods/packmenu
            "PackModeMenu-",                //https://www.curseforge.com/minecraft/mc-mods/packmodemenu
            "PickUpNotifier-",              //https://www.curseforge.com/minecraft/mc-mods/pick-up-notifier
            "Ping-",                        //https://www.curseforge.com/minecraft/mc-mods/ping
            "PingHUD-",                     //https://www.curseforge.com/minecraft/mc-mods/pinghud
            "PresenceFootsteps-",           //https://www.curseforge.com/minecraft/mc-mods/presence-footsteps
            "RPG-HUD-",                     //https://www.curseforge.com/minecraft/mc-mods/rpg-hud
            "ReAuth-",                      //https://www.curseforge.com/minecraft/mc-mods/reauth
            "Reforgium-",                   //https://www.curseforge.com/minecraft/mc-mods/reforgium
            "ResourceLoader-",              //https://www.curseforge.com/minecraft/mc-mods/resource-reloader
            "ResourcePackOrganizer",        //https://www.curseforge.com/minecraft/mc-mods/resource-pack-organizer
            "Ryoamiclights-",               //https://www.curseforge.com/minecraft/mc-mods/ryoamiclights
            "RyoamicLights-",               //https://www.curseforge.com/minecraft/mc-mods/ryoamiclights
            "ShoulderSurfing-",             //https://www.curseforge.com/minecraft/mc-mods/shoulder-surfing-reloaded
            "ShulkerTooltip-",              //https://www.curseforge.com/minecraft/mc-mods/shulkerboxtooltip
            "SimpleDiscordRichPresence-",   //https://www.curseforge.com/minecraft/mc-mods/simple-discord-rich-presence
            "SimpleWorldTimer-",            //https://www.curseforge.com/minecraft/mc-mods/simple-world-timer
            "SoundFilters-",                //https://www.curseforge.com/minecraft/mc-mods/sound-filters
            "Sounds-",                      //https://www.curseforge.com/minecraft/mc-mods/sound
            "SpawnerFix-",                  //https://www.curseforge.com/minecraft/mc-mods/spawner-fix
            "StylishEffects-",              //https://www.curseforge.com/minecraft/mc-mods/stylish-effects
            "TextruesRubidiumOptions-",     //https://www.curseforge.com/minecraft/mc-mods/textrues-rubidium-options
            "TRansliterationLib-",          //https://www.curseforge.com/minecraft/mc-mods/transliterationlib
            "TipTheScales-",                //https://www.curseforge.com/minecraft/mc-mods/tipthescales
            "Tips-",                        //https://www.curseforge.com/minecraft/mc-mods/tips
            "Toast Control-",               //https://www.curseforge.com/minecraft/mc-mods/toast-control
            "Toast-Control-",               //https://www.curseforge.com/minecraft/mc-mods/toast-control
            "ToastControl-",                //https://www.curseforge.com/minecraft/mc-mods/toast-control
            "TravelersTitles-",             //https://www.curseforge.com/minecraft/mc-mods/travelers-titles
            "VoidFog-",                     //https://www.curseforge.com/minecraft/mc-mods/void-fog
            "VR-Combat_",                   //https://www.curseforge.com/minecraft/mc-mods/vr-combat
            "WindowedFullscreen-",          //https://www.curseforge.com/minecraft/mc-mods/windowed-fullscreen
            "WorldNameRandomizer-",         //https://www.curseforge.com/minecraft/mc-mods/world-name-randomizer
            "YeetusExperimentus-",          //https://www.curseforge.com/minecraft/mc-mods/yeetusexperimentus
            "YungsMenuTweaks-",             //https://www.curseforge.com/minecraft/mc-mods/yungs-menu-tweaks
            "[1.12.2]DamageIndicatorsMod-", //https://www.curseforge.com/minecraft/mc-mods/damage-indicators-mod
            "[1.12.2]bspkrscore-",          //https://www.curseforge.com/minecraft/mc-mods/bspkrscore
            "ahznbstools-",                 //https://www.curseforge.com/minecraft/mc-mods/ahznbs-tools/
            "antighost-",                   //https://www.curseforge.com/minecraft/mc-mods/antighost
            "anviltooltipmod-",             //https://www.curseforge.com/minecraft/mc-mods/anvil-tooltip-mod
            "armorchroma-",                 //https://www.curseforge.com/minecraft/mc-mods/armor-chroma
            "armorpointspp-",               //https://www.curseforge.com/minecraft/mc-mods/armorpoints
            "auditory-",                    //https://www.curseforge.com/minecraft/mc-mods/auditory
            "authme-",                      //Gone? Reduces to atoms?
            "auto-reconnect-",              //https://www.curseforge.com/minecraft/mc-mods/auto-reconnect
            "autojoin-",                    //https://www.curseforge.com/minecraft/mc-mods/autojoin
            "autoreconnect-",               //https://www.curseforge.com/minecraft/mc-mods/autoreconnect
            "axolotl-item-fix-",            //Gone? Reduces to atoms?
            "backtools-",                   //https://www.curseforge.com/minecraft/mc-mods/backtools
            "bannerunlimited-",             //https://www.curseforge.com/minecraft/mc-mods/banner-unlimited
            "beddium-",                     //https://www.curseforge.com/minecraft/mc-mods/beddium
            "beenfo-",                      //https://www.curseforge.com/minecraft/mc-mods/beenfo
            "better-clouds-",               //Gone? Reduces to atoms?
            "better-recipe-book-",          //Gone? Reduces to atoms?
            "betterbiomeblend-",            //https://www.curseforge.com/minecraft/mc-mods/better-biome-blend
            "bhmenu-",                      //https://www.curseforge.com/minecraft/mc-mods/bisecthosting-server-integration-menu-forge & https://www.curseforge.com/minecraft/mc-mods/bisecthosting-server-integration-menu-fabric & https://www.curseforge.com/minecraft/mc-mods/bisecthosting-server-integration-menu-neoforge
            "blinkload-",                   //https://www.curseforge.com/minecraft/mc-mods/blinkload
            "blur-",                        //https://www.curseforge.com/minecraft/mc-mods/blur
            "borderless-mining-",           //https://www.curseforge.com/minecraft/mc-mods/borderless-mining
            "cat_jam-",                     //https://www.curseforge.com/minecraft/mc-mods/cat_jam
            "catalogue-",                   //https://www.curseforge.com/minecraft/mc-mods/catalogue
            "cave_dust-",                   //https://www.curseforge.com/minecraft/mc-mods/cave-dust
            "cfwinfo-",                     //https://www.curseforge.com/minecraft/mc-mods/create-fuel-and-water-information
            "chestsearchbar-",              //https://www.curseforge.com/minecraft/mc-mods/chest-search-bar
            "charmonium-",                  //https://www.curseforge.com/minecraft/mc-mods/charmonium
            "chat_heads-",                  //https://www.curseforge.com/minecraft/mc-mods/chat-heads
            "cherishedworlds-",             //https://www.curseforge.com/minecraft/mc-mods/cherished-worlds
            "cirback-1.0-",                 //Gone? Reduces to atoms?
            "citresewn-",                   //https://www.curseforge.com/minecraft/mc-mods/forge-cit
            "classicbar-",                  //https://www.curseforge.com/minecraft/mc-mods/classic-bars
            "clienttweaks-",                //https://www.curseforge.com/minecraft/mc-mods/client-tweaks
            "combat_music-",                //https://www.curseforge.com/minecraft/mc-mods/combat-music
            "configured-neoforge-",         //https://www.curseforge.com/minecraft/mc-mods/configured
            "configured-forge-",            //https://www.curseforge.com/minecraft/mc-mods/configured
            "configured-fabric-",           //https://www.curseforge.com/minecraft/mc-mods/configured
            "connectedness-",               //https://www.curseforge.com/minecraft/mc-mods/connectedness
            "controllable-",                //https://www.curseforge.com/minecraft/mc-mods/controllable
            "crash_assistant-",             //https://www.curseforge.com/minecraft/mc-mods/crash-assistant
            "cullleaves-",                  //https://www.curseforge.com/minecraft/mc-mods/cull-leaves
            "cullparticles-",               //https://www.curseforge.com/minecraft/mc-mods/cull-particles
            "custom-crosshair-mod-",        //https://www.curseforge.com/minecraft/mc-mods/custom-crosshair-mod
            "customdiscordrpc-",            //https://www.curseforge.com/minecraft/mc-mods/custom-discordrpc
            "darkness-",                    //Gone? Reduces to atoms?
            "dashloader-",                  //https://www.curseforge.com/minecraft/mc-mods/dashloader
            "defaultoptions-",              //https://www.curseforge.com/minecraft/mc-mods/default-options
            "desiredservers-",              //https://www.curseforge.com/minecraft/mc-mods/desired-servers
            "discordrpc-",                  //https://www.curseforge.com/minecraft/mc-mods/discordrpc
            "drippyloadingscreen-",         //https://www.curseforge.com/minecraft/mc-mods/drippy-loading-screen
            "drippyloadingscreen_",         //https://www.curseforge.com/minecraft/mc-mods/drippy-loading-screen
            "durabilitytooltip-",           //https://www.curseforge.com/minecraft/mc-mods/durability-tooltip
            "dynamic-fps-",                 //https://www.curseforge.com/minecraft/mc-mods/dynamic-fps
            "dynamic-music-",               //https://www.curseforge.com/minecraft/mc-mods/dynamic-music
            "dynamiclights-",               //https://www.curseforge.com/minecraft/mc-mods/dynamic-lights
            "dynamiclightsreforged-",       //https://www.curseforge.com/minecraft/mc-mods/dynamiclights-reforged
            "dynmus-",                      //Gone? Reduces to atoms?
            "e4mc-",                        //https://www.curseforge.com/minecraft/mc-mods/e4mc
            "effective-",                   //https://www.curseforge.com/minecraft/mc-mods/effective
            "eggtab-",                      //https://www.curseforge.com/minecraft/mc-mods/eggtab-fabric
            "eguilib-",                     //https://www.curseforge.com/minecraft/mc-mods/eguilib
            "eiramoticons-",                //Gone? Reduces to atoms?
            "embeddium-",                   //https://www.curseforge.com/minecraft/mc-mods/embeddium
            "enchantment-lore-",            //https://www.curseforge.com/minecraft/mc-mods/enchantment-lore
            "entity-texture-features-",     //https://www.curseforge.com/minecraft/mc-mods/entity-texture-features-fabric
            "entityculling-",               //https://www.curseforge.com/minecraft/mc-mods/entity-culling
            "essential_",                   //Gone? Reduces to atoms?
            "exhaustedstamina-",            //https://www.curseforge.com/minecraft/mc-mods/exhausted-stamina
            "extremesoundmuffler-",         //https://www.curseforge.com/minecraft/mc-mods/extreme-sound-muffler
            "fabricemotes-",                //https://www.curseforge.com/minecraft/mc-mods/fabric-emotes
            "fancymenu_",                   //https://www.curseforge.com/minecraft/mc-mods/fancymenu
            "fancymenu_video_extension",    //https://www.curseforge.com/minecraft/mc-mods/video-extension-for-fancymenu-forge
            "fast-ip-ping-",                //https://www.curseforge.com/minecraft/mc-mods/fast-ip-ping
            "firstperson-",                 //https://www.curseforge.com/minecraft/mc-mods/first-person-model
            "flerovium-",                   //https://www.curseforge.com/minecraft/mc-mods/flerovium
            "flickerfix-",                  //https://www.curseforge.com/minecraft/mc-mods/flickerfix
            "fm_audio_extension_",          //https://www.curseforge.com/minecraft/mc-mods/audio-extension-for-fancymenu-forge
            "fabricmod_VoxelMap-",          //https://www.curseforge.com/minecraft/mc-mods/voxelmap
            "forgemod_VoxelMap-",           //https://www.curseforge.com/minecraft/mc-mods/voxelmap
            "freecam-",                     //https://www.curseforge.com/minecraft/mc-mods/free-cam
            "freelook-",                    //https://www.curseforge.com/minecraft/mc-mods/freelook
            "galacticraft-rpc-",            //https://www.curseforge.com/minecraft/mc-mods/galacticraft-rpc
            "gamestagesviewer-",            //https://www.curseforge.com/minecraft/mc-mods/game-stages-viewer
            "gpumemleakfix-",               //https://www.curseforge.com/minecraft/mc-mods/fix-gpu-memory-leak
            "grid-",                        //https://www.curseforge.com/minecraft/mc-mods/grid
            "helium-",                      //Gone? Reduces to atoms?
            "hiddenrecipebook_",            //https://www.curseforge.com/minecraft/mc-mods/hidden-recipe-book
            "hiddenrecipebook-",            //https://www.curseforge.com/minecraft/mc-mods/hidden-recipe-book
            "ijmtweaks-",                   //https://www.curseforge.com/minecraft/mc-mods/ijm-tweaks
            "immersivemessages-",           //https://www.curseforge.com/minecraft/mc-mods/immersive-messages-api
            "immersivetips-",               //https://www.curseforge.com/minecraft/mc-mods/immersive-tips
            "infinitemusic-",               //https://www.curseforge.com/minecraft/mc-mods/infinite-music
            "inventoryhud.",                //https://www.curseforge.com/minecraft/mc-mods/inventory-hud-forge
            "inventoryprofiles",            //https://www.curseforge.com/minecraft/mc-mods/inventory-profiles
            "invtweaks-",                   //https://www.curseforge.com/minecraft/bukkit-plugins/invtweaks
            "itemzoom",                     //https://www.curseforge.com/minecraft/mc-mods/itemzoom
            "itlt-",                        //https://www.curseforge.com/minecraft/mc-mods/its-the-little-things
            "jeed-",                        //https://www.curseforge.com/minecraft/mc-mods/just-enough-effect-descriptions-jeed
            "jehc-",                        //https://www.curseforge.com/minecraft/mc-mods/just-enough-harvestcraft
            "jeiintegration_",              //https://www.curseforge.com/minecraft/mc-mods/jei-integration
            "jumpoverfences-",              //https://www.curseforge.com/minecraft/mc-mods/jumpoverfences
            "just-enough-harvestcraft-",    //https://www.curseforge.com/minecraft/mc-mods/just-enough-harvestcraft
            "justenoughbeacons-",           //https://www.curseforge.com/minecraft/mc-mods/just-enough-beacons
            "justenoughdrags-",             //https://www.curseforge.com/minecraft/mc-mods/just-enough-drags
            "justzoom_",                    //https://www.curseforge.com/minecraft/mc-mods/just-zoom
            "keymap-",                      //https://www.curseforge.com/minecraft/mc-mods/keymap
            "keywizard-",                   //https://www.curseforge.com/minecraft/mc-mods/keyboard-wizard
            "lazurite-",                    //https://www.curseforge.com/minecraft/mc-mods/lazurite
            "lazydfu-",                     //https://www.curseforge.com/minecraft/mc-mods/lazydfu
            "lib39-",                       //https://www.curseforge.com/minecraft/mc-mods/lib39
            "light-overlay-",               //https://www.curseforge.com/minecraft/mc-mods/light-overlay
            "lightfallclient-",             //https://www.curseforge.com/minecraft/mc-mods/lightfallclient-updated
            "lightspeed-",                  //https://www.curseforge.com/minecraft/mc-mods/lightspeedmod
            "loadmyresources_",             //https://www.curseforge.com/minecraft/mc-mods/load-my-resources-forge
            "lock_minecart_view-",          //Gone? Reduces to atoms?
            "lootbeams-",                   //https://www.curseforge.com/minecraft/mc-mods/lootbeams
            "lwl-",                         //Gone? Reduces to atoms?
            "macos-input-fixes-",           //https://www.curseforge.com/minecraft/mc-mods/macos-input-fixes
            "magnesium_extras-",            //Gone? Reduces to atoms?
            "maptooltip-",                  //https://www.curseforge.com/minecraft/mc-mods/map-tooltip
            "massunbind",                   //https://www.curseforge.com/minecraft/mc-mods/mass-key-unbinder
            "mcbindtype-",                  //https://www.curseforge.com/minecraft/mc-mods/mcbindtype
            "mcwifipnp-",                   //https://www.curseforge.com/minecraft/mc-mods/mcwifipnp
            "medievalmusic-",               //https://www.curseforge.com/minecraft/mc-mods/medieval-music
            "memoryusagescreen-",           //https://www.curseforge.com/minecraft/mc-mods/memory-usage-screen
            "mightyarchitect-",             //https://www.curseforge.com/minecraft/mc-mods/the-mighty-architect
            "mindful-eating-",              //https://www.curseforge.com/minecraft/mc-mods/mindful-eating
            "minetogether-",                //https://www.curseforge.com/minecraft/mc-mods/creeperhost-minetogether
            "mobplusplus-",                 //Gone? Reduces to atoms?
            "modcredits-",                  //https://www.curseforge.com/minecraft/mc-mods/mod-credits
            "modernworldcreation_",         //https://www.curseforge.com/minecraft/mc-mods/modernworldcreation
            "modnametooltip-",              //https://www.curseforge.com/minecraft/mc-mods/mod-name-tooltip
            "modnametooltip_",              //https://www.curseforge.com/minecraft/mc-mods/mod-name-tooltip
            "moreoverlays-",                //https://www.curseforge.com/minecraft/mc-mods/more-overlays
            "mousewheelie-",                //https://www.curseforge.com/minecraft/mc-mods/mouse-wheelie
            "movement-vision-",             //https://www.curseforge.com/minecraft/mc-mods/movement-vision
            "multihotbar-",                 //https://www.curseforge.com/minecraft/mc-mods/multi-hotbar
            "music-duration-reducer-",      //https://www.curseforge.com/minecraft/mc-mods/music-duration-reducer
            "musicdr-",                     //Gone? Reduces to atoms?
            "neiRecipeHandlers-",           //Gone? Reduces to atoms?
            "ngrok-lan-expose-mod-",        //Gone? Reduces to atoms?
            "no_nv_flash-",                 //https://www.curseforge.com/minecraft/mc-mods/no-nv-flash
            "nopotionshift_",               //https://www.curseforge.com/minecraft/mc-mods/no-potion-shift
            "notenoughanimations-",         //https://www.curseforge.com/minecraft/mc-mods/not-enough-animations
            "oculus-",                      //https://www.curseforge.com/minecraft/mc-mods/oculus
            "ornaments-",                   //https://www.curseforge.com/minecraft/mc-mods/ornaments
            "overloadedarmorbar-",          //https://www.curseforge.com/minecraft/mc-mods/overloaded-armor-bar
            "panorama-",                    //https://www.curseforge.com/minecraft/mc-mods/panorama
            "paperdoll-",                   //https://www.curseforge.com/minecraft/mc-mods/paperdoll
            "physics-mod-",                 //https://www.curseforge.com/minecraft/mc-mods/physics-mod
            "phosphor-",                    //https://www.curseforge.com/minecraft/mc-mods/phosphor
            "preciseblockplacing-",         //Gone? Reduces to atoms?
            "radon-",                       //https://www.curseforge.com/minecraft/mc-mods/radon
            "realm-of-lost-souls-",         //https://www.curseforge.com/minecraft/mc-mods/bobs-realm-of-lost-souls
            "rebind_narrator-",             //https://www.curseforge.com/minecraft/mc-mods/rebind-narrator
            "rebind-narrator-",             //https://www.curseforge.com/minecraft/mc-mods/rebind-narrator
            "rebindnarrator-",              //https://www.curseforge.com/minecraft/mc-mods/rebind-narrator
            "rebrand-",                     //https://www.curseforge.com/minecraft/mc-mods/rebrand
            "reforgium-",                   //https://www.curseforge.com/minecraft/mc-mods/reforgium
            "relictium-",                   //https://www.curseforge.com/minecraft/mc-mods/relictium
            "replanter-",                   //https://www.curseforge.com/minecraft/mc-mods/replanter
            "rrls-",                        //https://www.curseforge.com/minecraft/mc-mods/rrls
            "rubidium-",                    //https://www.curseforge.com/minecraft/mc-mods/rubidium
            "rubidium_extras-",             //https://www.curseforge.com/minecraft/mc-mods/rubidium-extra
            "screenshot-to-clipboard-",     //https://www.curseforge.com/minecraft/mc-mods/screenshot-to-clipboard
            "servercountryflags-",          //https://www.curseforge.com/minecraft/mc-mods/server-country-flags
            "shutupexperimentalsettings-",  //https://www.curseforge.com/minecraft/mc-mods/shutup-experimental-settings
            "shutupmodelloader-",           //https://www.curseforge.com/minecraft/mc-mods/shut-up-model-loader
            "signtools-",                   //https://www.curseforge.com/minecraft/bukkit-plugins/signtools
            "simple-rpc-",                  //https://www.curseforge.com/minecraft/mc-mods/simple-discord-rpc
            "simpleautorun-",               //Gone? Reduces to atoms?
            "smartcursor-",                 //https://www.curseforge.com/minecraft/mc-mods/smartcursor
            "smarthud-",                    //https://www.curseforge.com/minecraft/mc-mods/smart-hud
            "smoothboot-",                  //https://www.curseforge.com/minecraft/mc-mods/smoothboot
            "smoothfocus-",                 //https://www.curseforge.com/minecraft/mc-mods/smoothfocus
            "sodium-fabric-",               //https://www.curseforge.com/minecraft/mc-mods/sodium
            "sodium-shader-support-",       //https://modrinth.com/mod/sodium-shader-support/
            "sodiumcoreshadersupport-",     //https://www.curseforge.com/minecraft/mc-mods/sodium-core-shader-support
            "sodiumdynamiclights-",         //https://www.curseforge.com/minecraft/mc-mods/dynamiclights-reforged
            "sodiumleafculling-",           //https://www.curseforge.com/minecraft/mc-mods/sodium-leaf-culling
            "sodiumoptionsapi-",            //https://www.curseforge.com/minecraft/mc-mods/sodium-options-api
            "sodiumoptionsmodcompat-",      //https://www.curseforge.com/minecraft/mc-mods/sodium-embeddium-options-mod-compat
            "sounddeviceoptions-",          //https://www.curseforge.com/minecraft/mc-mods/more-sound-config
            "soundreloader-",               //https://www.curseforge.com/minecraft/mc-mods/sound-reloader
            "spoticraft-",                  //https://www.curseforge.com/minecraft/mc-mods/spoticraft-inactive
            "skinlayers3d-forge",           //https://www.curseforge.com/minecraft/mc-mods/skin-layers-3d
            "tconplanner-",                 //https://www.curseforge.com/minecraft/mc-mods/tinkers-planner
            "textrues_embeddium_options-",  //https://www.curseforge.com/minecraft/mc-mods/textrues-embeddium-options
            "timestamps-",                  //https://www.curseforge.com/minecraft/mc-mods/timestamps
            "tooltipscroller-",             //https://www.curseforge.com/minecraft/mc-mods/tooltip-scroller
            "torchoptimizer-",              //https://www.curseforge.com/minecraft/mc-mods/torch-optimizer
            "torohealth-",                  //https://www.curseforge.com/minecraft/mc-mods/torohealth-damage-indicators
            "totaldarkness",                //https://www.curseforge.com/minecraft/mc-mods/total-darkness
            "toughnessbar-",                //https://www.curseforge.com/minecraft/mc-mods/armor-toughness-bar
            "watermedia-",                  //https://www.curseforge.com/minecraft/mc-mods/watermedia
            "whats-that-slot-forge-",       //https://www.curseforge.com/minecraft/mc-mods/whats-that-slot
            "wisla-",                       //https://www.curseforge.com/minecraft/mc-mods/wisla
            "xenon-",                       //https://www.curseforge.com/minecraft/mc-mods/xenon
            "xlifeheartcolors-",            //https://www.curseforge.com/minecraft/mc-mods/x-life-heart-colors
            "yisthereautojump-"             //https://www.curseforge.com/minecraft/mc-mods/y-is-there-autojump-forge
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
            "modernfix",
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
        "https://raw.githubusercontent.com/Griefed/ServerPackCreator/main/serverpackcreator-api/src/main/resources/serverpackcreator.properties"
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
    val fallbackUpdateServerPack = false
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

    /**
     * Only the first call to this property will return true if this is the first time ServerPackCreator is being run
     * on a given host. Any subsequent call will return false. Handle with care!
     *
     * @author Griefed
     */
    val firstRun: Boolean

    var logLevel = "INFO"
        get() {
            field = acquireProperty(pLogLevel, "INFO").uppercase()
            return field
        }
        set(value) {
            field = value.uppercase()
            defineProperty(pLogLevel, field)
            setLoggingLevel(field)
        }

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

    fun getPreference(pref: String) : Optional<String> {
        return Optional.ofNullable(spcPreferences.get(pref, null))
    }

    fun storePreference(pref: String, value: String) {
        spcPreferences.put(pref, value)
        spcPreferences.sync()
    }

    /**
     * Default list of script templates used by ServerPackCreator.
     *
     * @author Griefed
     */
    @Deprecated("Deprecated as of 6.0.0", ReplaceWith("defaultScriptTemplateMap"))
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
        if (!batchPresent) {
            newTemplates.add(File(serverFilesDirectory.absolutePath, defaultBatchScriptTemplate.name).absoluteFile)
        }

        return newTemplates.toList()
    }

    @Deprecated("Deprecated as of 6.0.0", ReplaceWith("startScriptTemplates"))
    var scriptTemplates: TreeSet<File> = TreeSet<File>()
        get() {
            val scriptSetting = internalProps.getProperty(pServerPackScriptTemplates)
            val entries =
                if (scriptSetting != null && scriptSetting == "default_template.ps1,default_template.sh,default_template.bat") {
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
     * Default map of start-script templates: sh, ps1, bat.
     */
    fun defaultStartScriptTemplates(): HashMap<String, String> {
        return hashMapOf(
            Pair("sh", File(serverFilesDirectory.absolutePath, defaultShellScriptTemplate.name).absolutePath),
            Pair("ps1", File(serverFilesDirectory.absolutePath, defaultPowerShellScriptTemplate.name).absolutePath),
            Pair("bat", File(serverFilesDirectory.absolutePath, defaultBatchScriptTemplate.name).absolutePath)
        )
    }

    /**
     * Start-script templates to use during server pack generation.
     * Each key represents a different template and script-type.
     */
    var startScriptTemplates: HashMap<String, String> = hashMapOf()
        get() {
            val templateProps = internalProps.keys
                .filter { entry -> (entry as String).startsWith(pServerPackStartScriptTemplatesPrefix) }
                .map { entry -> entry as String }
            var type: String
            if (templateProps.isEmpty() || templateProps.any { entry ->
                    entry.replace(pServerPackStartScriptTemplatesPrefix, "").isBlank()
                }) {
                log.warn("Found empty definitions for start script templates. Using defaults.")
                field = defaultStartScriptTemplates()
            } else {
                for (templateProp in templateProps) {
                    type = templateProp.replace(pServerPackStartScriptTemplatesPrefix, "")
                    field[type] = File(internalProps[templateProp] as String).absolutePath
                }
            }
            if (field.isEmpty()) {
                log.error("No start script templates defined. Using defaults.")
                field = defaultStartScriptTemplates()
            }
            return field
        }
        set(map) {
            for ((key, value) in map) {
                defineProperty("$pServerPackStartScriptTemplatesPrefix$key", value)
                log.info("Set $pServerPackStartScriptTemplatesPrefix$key to $value")
            }
            field = map
        }

    /**
     * Default map of start-script templates: sh, ps1, bat.
     */
    fun defaultJavaScriptTemplates(): HashMap<String, String> {
        return hashMapOf(
            Pair("sh", File(serverFilesDirectory.absolutePath, defaultJavaShellScriptTemplate.name).absolutePath),
            Pair("ps1", File(serverFilesDirectory.absolutePath, defaultJavaPowerShellScriptTemplate.name).absolutePath)
        )
    }

    /**
     * Start-script templates to use during server pack generation.
     * Each key represents a different template and script-type.
     */
    var javaScriptTemplates: HashMap<String, String> = hashMapOf()
        get() {
            val templateProps = internalProps.keys
                .filter { entry -> (entry as String).startsWith(pServerPackJavaScriptTemplatesPrefix) }
                .map { entry -> entry as String }
            var type: String
            if (templateProps.isEmpty() || templateProps.any { entry ->
                    entry.replace(
                        pServerPackJavaScriptTemplatesPrefix,
                        ""
                    ).isBlank()
                }) {
                log.warn("Found empty definitions for java script templates. Using defaults.")
                field = defaultJavaScriptTemplates()
            } else {
                for (templateProp in templateProps) {
                    type = templateProp.replace(pServerPackJavaScriptTemplatesPrefix, "")
                    field[type] = File(internalProps[templateProp] as String).absolutePath
                }
            }
            if (field.isEmpty()) {
                log.error("No java script templates defined. Using defaults.")
                field = defaultJavaScriptTemplates()
            }
            return field
        }
        set(map) {
            for ((key, value) in map) {
                defineProperty("$pServerPackJavaScriptTemplatesPrefix$key", value)
                log.info("Set $pServerPackJavaScriptTemplatesPrefix$key to $value")
            }
            field = map
        }

    /**
     * The URL from which a .properties-file is read during updating of the fallback clientside-mods list.
     * The default can be found in [fallbackUpdateURL].
     */
    var updateUrl: URL = URI(fallbackUpdateURL).toURL()
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
     * Whether a server pack should be updated instead of cleanly generated.
     */
    var isUpdatingServerPacksEnabled = false
        get() {
            field = getBoolProperty(pUpdateServerPack, fallbackUpdateServerPack)
            return field
        }
        set(value) {
            setBoolProperty(pUpdateServerPack, value)
            field = value
            log.info("Server pack updating set to: $field")
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
     *
     * When setting this to a different URL, you may leave out the `jdbc:postgresql://`-part, it will be prefixed automatically.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var jdbcDatabaseUrl: String = "jdbc:postgresql://localhost:5432/serverpackcreator"
        get() {
            var dbPath =
                internalProps.getProperty(pSpringDatasourceUrl, "jdbc:postgresql://localhost:5432/serverpackcreator")
            if (dbPath.isEmpty() || dbPath.contains("jdbc:sqlite") || !dbPath.startsWith("jdbc:postgresql://")) {
                log.warn("Your spring.datasource.url-property didn't match a PostgreSQL JDBC URL: $dbPath. It has been migrated to jdbc:postgresql://localhost:5432/serverpackcreator.")
                dbPath = "jdbc:postgresql://localhost:5432/serverpackcreator"
            }
            internalProps.setProperty(pSpringDatasourceUrl, dbPath)
            field = dbPath
            return field
        }
        set(value) {
            if (!value.startsWith("jdbc:postgresql://")) {
                internalProps.setProperty(pSpringDatasourceUrl, "jdbc:postgresql://$value")
            } else {
                internalProps.setProperty(pSpringDatasourceUrl, value)
            }
            field = internalProps.getProperty(pSpringDatasourceUrl)
            log.info("Set database url to: $field.")
            log.warn("Restart ServerPackCreator for this change to take effect.")
        }

    var jdbcDatabaseUsername: String = ""
        get() {
            field = internalProps.getProperty(pSpringDatasourceUsername, "")
            return field
        }
        set(value) {
            field = value
            internalProps.setProperty(pSpringDatasourceUsername, field)
            log.info("Set username url to: $field.")
            log.warn("Restart ServerPackCreator for this change to take effect.")
        }

    var jdbcDatabasePassword: String = ""
        get() {
            field = internalProps.getProperty(pSpringDatasourcePassword, "")
            return field
        }
        set(value) {
            field = value
            internalProps.setProperty(pSpringDatasourcePassword, field)
            log.info("Set password url to: $field.")
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
            val prop = internalProps.getProperty(pJavaForServerInstall, null)
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
        return "jdbc:postgresql://localhost:5432/serverpackcreator"
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
    var homeDirectory: File = home.absoluteFile
        get() {
            val setting = if (getPreference(pHomeDirectory).isPresent) {
                getPreference(pHomeDirectory).get()
            } else if (internalProps.containsKey(pHomeDirectory) && internalProps.getProperty(pHomeDirectory).isNotBlank()) {
                internalProps.getProperty(pHomeDirectory)
            } else if (jarInformation.jarPath.toFile().isDirectory || devBuild) {
                // Dev environment
                File("").absolutePath
            } else if (File(System.getProperty("user.home")).isDirectory) {
                File(System.getProperty("user.home"),"ServerPackCreator").absolutePath
            } else {
                home.absolutePath
            }

            internalProps.remove(pHomeDirectory)
            storePreference(pHomeDirectory, setting)
            field = File(getPreference(pHomeDirectory).get()).absoluteFile

            if (!field.isDirectory) {
                field.create(createFileOrDir = true, asDirectory = true)
            }

            return field
        }
        set(value) {
            storePreference(pHomeDirectory, value.absolutePath)
            field = value.absoluteFile
            log.info("Home-directory set to: $field")
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
     * Overrides which, well, override, any property which may be set in the regular [serverPackCreatorPropertiesFile].
     */
    var overridesPropertiesFile: File = File(homeDirectory, "overrides.properties")
        get() {
            field = File(homeDirectory, "overrides.properties")
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
            val default = logsDirectory.absolutePath
            val prop = internalProps.getProperty(pTomcatLogsDirectory, default)
            val dir = if (File(prop).canWrite()) {
                internalProps.getProperty(pTomcatLogsDirectory, default)
            } else {
                default
            }
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
     * Old NeoForge version manifest containing information about available NeoForge loader versions.
     * This manifest only contains versions for Minecraft 1.20.1.
     *
     *
     * By default, the `neoforge-manifest.xml`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var oldNeoForgeVersionManifest: File = File(manifestsDirectory, "neoforge-manifest.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "neoforge-manifest.xml").absoluteFile
            return field
        }
        private set

    /**
     * New NeoForge version manifest containing information about available NeoForge loader versions.
     * This manifest contains versions for Minecraft 1.20.2 and up.
     *
     *
     * By default, the `neoforge-manifest-new.xml`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var newNeoForgeVersionManifest: File = File(manifestsDirectory, "neoforge-manifest-new.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "neoforge-manifest-new.xml").absoluteFile
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

    /**
     * The default shell-template for the modded server start scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * [startScriptTemplates].
     */
    val defaultShellScriptTemplate = File(serverFilesDirectory, "default_template.sh")

    /**
     * The default PowerShell-template for the modded server start scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * [startScriptTemplates].
     */
    val defaultPowerShellScriptTemplate = File(serverFilesDirectory, "default_template.ps1")

    /**
     * The default Batch-template for the modded server start scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * [startScriptTemplates].
     */
    val defaultBatchScriptTemplate = File(serverFilesDirectory, "default_template.bat")

    /**
     * The default shell-template for the java-install scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * [javaScriptTemplates].
     */
    val defaultJavaShellScriptTemplate = File(serverFilesDirectory, "default_java_template.sh")

    /**
     * The default PowerShell-template for the java-install scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * [javaScriptTemplates].
     */
    val defaultJavaPowerShellScriptTemplate = File(serverFilesDirectory, "default_java_template.ps1")

    /**
     * The default Batch-template for the java-install scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * [javaScriptTemplates].
     */
    val defaultJavaBatchScriptTemplate = File(serverFilesDirectory, "default_java_template.bat")

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

        try {
            //Temp props to load the ones from the specified file into
            val tempProps = Properties()
            propertiesFile.inputStream().use {
                tempProps.load(it)
            }

            tempProps.entries.removeIf { entry -> entry.value.toString().isBlank() }

            //Write temp props into passed props
            for ((key, value) in tempProps.entries) {
                props[key] = value
            }

            props.entries.removeIf { entry -> entry.value.toString().isBlank() }
            propertyFiles.add(propertiesFile)
            log.info("Loaded properties from ${propertiesFile.absolutePath}.")
        } catch (ex: Exception) {
            log.error("Couldn't read properties from ${propertiesFile.absolutePath}.", ex)
        }
    }

    fun loadOverrides(properties: File = overridesPropertiesFile) {
        val tempProps = Properties()
        if (properties.isFile) {
            properties.inputStream().use {
                tempProps.load(it)
            }
        }
        for ((key, value) in tempProps) {
            log.warn("Overriding:")
            log.warn("  $key")
            log.warn("  $value")
        }
        internalProps.putAll(tempProps)
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
    fun loadProperties(
        propertiesFile: File = File(serverPackCreatorProperties).absoluteFile,
        saveProps: Boolean = true
    ) {
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

        internalProps.putAll(props)

        //Acquisition done, now, load from the home-directory
        loadFile(serverPackCreatorPropertiesFile, props)
        internalProps.putAll(props)

        internalProps.setProperty(pTomcatBaseDirectory, homeDirectory.absolutePath)
        if (internalProps.getProperty(pLanguage) != "en_GB") {
            changeLocale(Locale(internalProps.getProperty(pLanguage)))
        }

        // Load all values from the overrides-properties
        loadOverrides(overridesPropertiesFile)

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
    fun acquireJavaPath(pathToJava: String? = null): String {
        var checkedJavaPath: String
        try {
            if (!pathToJava.isNullOrBlank()) {
                if (checkJavaPath(pathToJava)) {
                    return pathToJava
                }
                if (checkJavaPath("$pathToJava.exe")) {
                    return "$pathToJava.exe"
                }
                if (checkJavaPath("$pathToJava.lnk")) {
                    return FileUtilities.resolveLink(File("$pathToJava.lnk"))
                }
            }
            checkedJavaPath = SystemUtilities.acquireJavaPathFromSystem()
            log.debug("Acquired path to Java installation: $checkedJavaPath")
        } catch (ex: NullPointerException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = SystemUtilities.acquireJavaPathFromSystem()
            log.debug("Automatically acquired path to Java installation: $checkedJavaPath", ex)
        } catch (ex: InvalidFileTypeException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = SystemUtilities.acquireJavaPathFromSystem()
            log.debug("Automatically acquired path to Java installation: $checkedJavaPath", ex)
        } catch (ex: IOException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = SystemUtilities.acquireJavaPathFromSystem()
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
        val toSave = TreeSet<File>()
        toSave.addAll(propertyFiles)
        toSave.add(propertiesFile)

        for (props in toSave) {
            if (!props.isFile && props != serverPackCreatorPropertiesFile) {
                //Skip if the file no longer exists
                continue
            }
            try {
                props.outputStream().use {
                    internalProps.store(
                        it,
                        "For details about each property, see https://help.serverpackcreator.de/settings-and-configs.html"
                    )
                }
                log.info("Saved properties to: $propertiesFile")
            } catch (ex: FileNotFoundException) {
                log.error("Couldn't write properties-file ${props.absolutePath}. File either doesn't exist or we don't have write-permission.")
            } catch (ex: IOException) {
                log.error("Couldn't write properties-file ${props.absolutePath}.", ex)
            }
        }
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
    private fun checkJavaPath(pathToJava: String?): Boolean {
        if (pathToJava.isNullOrBlank()) {
            return false
        }
        if (checkedJavas.containsKey(pathToJava)) {
            return checkedJavas[pathToJava]!!
        }
        val result: Boolean
        when (FileUtilities.checkFileType(pathToJava)) {
            FileType.FILE -> {
                result = testJava(pathToJava)
            }

            FileType.LINK, FileType.SYMLINK -> {
                result = try {
                    testJava(FileUtilities.resolveLink(File(pathToJava)))
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
     * Whether the fallback lists for clientside-mods and whitelisted mods have been updated.
     *
     * `true` if either was updated.
     */
    var fallbackUpdated: Boolean = false
        private set

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
        fallbackUpdated = false
        if (properties != null) {
            val newBlacklist = properties!!.getProperty(pConfigurationFallbackModsList)
            val currentBlacklist = internalProps.getProperty(pConfigurationFallbackModsList)
            if (newBlacklist != null && currentBlacklist != newBlacklist) {
                internalProps.setProperty(pConfigurationFallbackModsList, newBlacklist)
                clientsideMods.clear()
                clientsideMods.addAll(internalProps.getProperty(pConfigurationFallbackModsList).split(","))
                log.info("The fallback-list for clientside only mods has been updated to: $clientsideMods")
                fallbackUpdated = true
            }

            val newWhitelist = properties!!.getProperty(pConfigurationFallbackModsWhiteList)
            val currentWhitelist = internalProps.getProperty(pConfigurationFallbackModsWhiteList)
            if (newWhitelist != null && currentWhitelist != newWhitelist) {
                internalProps.setProperty(pConfigurationFallbackModsWhiteList, newWhitelist)
                modsWhitelist.clear()
                modsWhitelist.addAll(internalProps.getProperty(pConfigurationFallbackModsWhiteList).split(","))
                log.info("The fallback-list for whitelisted mods has been updated to: $modsWhitelist")
                fallbackUpdated = true
            }
        }
        if (fallbackUpdated) {
            saveProperties(File(homeDirectory, serverPackCreatorProperties).absoluteFile)
        }
        return fallbackUpdated
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

    fun clearPropertyFileList() {
        propertyFiles.clear()
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
        ListUtilities.printListToLogChunked(clientsideMods.toList(), 5, "    ", true)
        log.info("Regex clientside-mods-list set to:")
        ListUtilities.printListToLogChunked(clientsideModsRegex.toList(), 5, "    ", true)
        log.info("Available Java paths for scripts:")
        for ((key, value) in javaPaths) {
            log.info("    Java $key path: $value")
        }
        log.info("Using script templates:")
        for ((key, value) in startScriptTemplates) {
            log.info("    $key: $value")
        }
        log.info("============================== PROPERTIES ==============================")
    }

    val installLocationXml: File = File(home, "log4j2.xml")
    val log4jXml: File

    init {
        i18n4k = i18n4kConfig
        i18n4kConfig.defaultLocale = Locale("en_GB")
        loadProperties(propertiesFile, false)
        log4jXml = File(homeDirectory, "log4j2.xml")
        try {
            var log4j: String
            val oldLogs = "<Property name=\"log-path\">logs</Property>"
            val newLogs = "<Property name=\"log-path\">${logsDirectory.absolutePath}</Property>"
            this.javaClass.getResourceAsStream("/log4j2.xml").use {
                log4j = it?.readText().toString()
                log4j = log4j.replace(oldLogs, newLogs)
                if (!log4jXml.isFile || devBuild || preRelease) {
                    log4jXml.writeText(log4j)
                }
            }
        } catch (ex: IOException) {
            println("Error reading/writing log4j2.xml.")
            ex.printStackTrace()
        }

        if (devBuild || preRelease) {
            logLevel = "DEBUG"
        } else {
            setLoggingLevel(logLevel)
        }

        firstRun = getBoolProperty("de.griefed.serverpackcreator.firstrun", true)
        setBoolProperty("de.griefed.serverpackcreator.firstrun", false)
        logsDirectory.create(createFileOrDir = true, asDirectory = true)
        serverFilesDirectory.create(createFileOrDir = true, asDirectory = true)
        propertiesDirectory.create(createFileOrDir = true, asDirectory = true)
        iconsDirectory.create(createFileOrDir = true, asDirectory = true)
        configsDirectory.create(createFileOrDir = true, asDirectory = true)
        workDirectory.create(createFileOrDir = true, asDirectory = true)
        tempDirectory.create(createFileOrDir = true, asDirectory = true)
        modpacksDirectory.create(createFileOrDir = true, asDirectory = true)
        serverPacksDirectory.create(createFileOrDir = true, asDirectory = true)
        pluginsDirectory.create(createFileOrDir = true, asDirectory = true)
        pluginsConfigsDirectory.create(createFileOrDir = true, asDirectory = true)
        manifestsDirectory.create(createFileOrDir = true, asDirectory = true)
        minecraftServerManifestsDirectory.create(createFileOrDir = true, asDirectory = true)
        installerCacheDirectory.create(createFileOrDir = true, asDirectory = true)
        printSettings()
        saveProperties(File(homeDirectory, serverPackCreatorProperties).absoluteFile)
    }

    private fun setLoggingLevel(level: String) {
        var loggingConfig = log4jXml.readText()
        loggingConfig = loggingConfig.replace(
            "<Property name=\"log-level-spc\">.*</Property>".toRegex(),
            "<Property name=\"log-level-spc\">${level.uppercase()}</Property>"
        )
        log4jXml.writeText(loggingConfig)
    }

    companion object {
        /**
         * @author Griefed
         */
        @JvmStatic
        fun getSeparator(): String {
            return File.separator
        }
    }

    override fun getSupportedTypes(): Array<String> = suffixes

    /**
     * Depending on whether this is the first run of ServerPackCreator on a users machine, the default
     * log4j2 configuration may be present at different locations. The default one is the config
     * inside the home-directory of SPC, of which we will try to set up our logging with. If said file
     * fails for whatever reason, we will try to use a config inside the directory from which SPC was
     * executed. Should that fail, too, the config from the classpath is used, to ensure we always
     * have default configs available. Should that fail, too, though, log4j is set up with its own
     * default settings.
     *
     * @param loggerContext logger context passed from log4j itself
     * @param source        configuration source passed from log4j itself. Attempts to overwrite it
     * are made, but if all else fails it is used to set up logging with log4j's
     * default config.
     * @return Custom configuration with proper logs-directory set.
     * @author Griefed
     */
    override fun getConfiguration(loggerContext: LoggerContext, source: ConfigurationSource): Configuration {
        val configSource: ConfigurationSource
        if (log4jXml.isFile) {
            try {
                return getXmlConfig(log4jXml, loggerContext)
            } catch (ex: IOException) {
                println("Couldn't parse $log4jXml.")
                ex.printStackTrace()
            }
        } else if (installLocationXml.isFile) {
            try {
                return getXmlConfig(installLocationXml, loggerContext)
            } catch (ex: IOException) {
                println("Couldn't parse $installLocationXml.")
                ex.printStackTrace()
            }
        }
        try {
            configSource = ConfigurationSource(this.javaClass.getResourceAsStream("/log4j2.xml")!!)
            return CustomXMLConfiguration(loggerContext, configSource)
        } catch (ex: IOException) {
            println("Couldn't parse resource log4j2.xml.")
            ex.printStackTrace()
        }
        return CustomXMLConfiguration(loggerContext, source)
    }

    private fun getXmlConfig(sourceFile: File, loggerContext: LoggerContext): CustomXMLConfiguration {
        val configSource: ConfigurationSource
        val stream = sourceFile.inputStream()
        configSource = ConfigurationSource(stream, sourceFile)
        val custom = CustomXMLConfiguration(loggerContext, configSource)
        stream.close()
        return custom
    }

    /**
     * Custom XmlConfiguration to pass our custom log4j2.xml config to log4j.
     *
     * Set up the XML configuration with the passed context and config source. For the config source
     * being used, [ApiProperties.getConfiguration] where
     * multiple attempts at creating a new private val log by lazy { cachedLoggerOf(this.javaClass) } using our own log4j2.xml are made
     * before the default log4j setup is used.
     *
     * @param loggerContext logger context passed from log4j itself
     * @param configSource  configuration source passed from
     * [ApiProperties.getConfiguration].
     * @author Griefed
     */
    inner class CustomXMLConfiguration(loggerContext: LoggerContext?, configSource: ConfigurationSource?) :
        XmlConfiguration(loggerContext, configSource)
}