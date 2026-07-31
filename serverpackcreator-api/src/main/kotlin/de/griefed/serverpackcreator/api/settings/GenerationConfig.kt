/* Copyright (C) 2026 Griefed
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
package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import de.griefed.serverpackcreator.api.config.ExclusionFilter
import de.griefed.serverpackcreator.api.settings.GenerationConfig.Companion.AUTO_DISCOVERY_ENABLED_KEY
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.util.*

/**
 * Settings-group for server pack generation: clientside-mod lists and their whitelist, directory
 * in- and exclusions, pre- and post-install cleanup-files, ZIP-archive exclusions, the
 * clientside-mod exclusion-filter, generation-flags and Aikar's flags. Extracted from
 * ApiProperties (refactor Phase 1b); ApiProperties remains the facade through which consumers
 * access these values.
 */
@Suppress("MemberVisibilityCanBePrivate")
class GenerationConfig(private val store: PropertyStore) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val trueFalseRegex = "^(true|false)$".toRegex()

    /** Property keys and the shipped defaults every new configuration starts from. */
    companion object {
        /**
         * Property-key holding the fallback-list of clientside-only mods.
         */
        const val FALLBACK_MODS_LIST_KEY = "de.griefed.serverpackcreator.configuration.fallbackmodslist"

        /**
         * Property-key holding the whitelist of mods to keep regardless of clientside-matches.
         */
        const val MODS_WHITELIST_KEY = "de.griefed.serverpackcreator.configuration.modswhitelist"

        /**
         * Property-key holding the directories which must always be included in server packs.
         */
        const val DIRECTORIES_MUST_INCLUDE_KEY = "de.griefed.serverpackcreator.configuration.directories.mustinclude"

        /**
         * Property-key holding the directories which should be excluded from server packs.
         */
        const val DIRECTORIES_SHOULD_EXCLUDE_KEY = "de.griefed.serverpackcreator.configuration.directories.shouldexclude"

        /**
         * Property-key holding the files to delete after a modloader-server installation.
         */
        const val POST_INSTALL_CLEANUP_KEY = "de.griefed.serverpackcreator.install.post.files"

        /**
         * Property-key holding the files to delete before a modloader-server installation.
         */
        const val PRE_INSTALL_CLEANUP_KEY = "de.griefed.serverpackcreator.install.pre.files"

        /**
         * Property-key holding the files to exclude from server pack ZIP-archives.
         */
        const val ZIP_EXCLUSIONS_KEY = "de.griefed.serverpackcreator.serverpack.zip.exclude"

        /**
         * Property-key toggling the exclusion of files from server pack ZIP-archives.
         */
        const val ZIP_EXCLUSION_ENABLED_KEY = "de.griefed.serverpackcreator.serverpack.zip.exclude.enabled"

        /**
         * Property-key toggling the automatic exclusion of clientside-only mods.
         */
        const val AUTO_DISCOVERY_ENABLED_KEY = "de.griefed.serverpackcreator.serverpack.autodiscovery.enabled"

        /**
         * Legacy property-key of [AUTO_DISCOVERY_ENABLED_KEY], migrated on read.
         */
        const val AUTO_DISCOVERY_ENABLED_LEGACY_KEY = "de.griefed.serverpackcreator.serverpack.autodiscoverenabled"

        /**
         * Property-key holding the filter-method for clientside-only mod exclusions.
         */
        const val AUTO_DISCOVERY_FILTER_KEY = "de.griefed.serverpackcreator.serverpack.autodiscovery.filter"

        /**
         * Property-key toggling the overwriting of already existing server packs.
         */
        const val OVERWRITE_ENABLED_KEY = "de.griefed.serverpackcreator.serverpack.overwrite.enabled"

        /**
         * Property-key toggling cleanup-procedures after server pack generation.
         */
        const val CLEANUP_ENABLED_KEY = "de.griefed.serverpackcreator.serverpack.cleanup.enabled"

        /**
         * Property-key toggling the availability of Minecraft pre-releases and snapshots.
         */
        const val MINECRAFT_SNAPSHOTS_KEY = "de.griefed.serverpackcreator.minecraft.snapshots"

        /**
         * Property-key toggling updating of existing server packs instead of clean generation.
         */
        const val UPDATE_SERVER_PACK_KEY = "de.griefed.serverpackcreator.serverpack.update"

        /**
         * Property-key holding Aikar's flags for the generated start-scripts.
         */
        const val AIKARS_FLAGS_KEY = "de.griefed.serverpackcreator.configuration.aikar"
    }

    private var fallbackModsWhitelist = TreeSet(
        listOf(
            "Ping-Wheel-",
            "appleskin-",
            "thulium-"
        )
    )

    @Suppress("SpellCheckingInspection")
    private var fallbackMods = TreeSet(
        listOf(
            "[1.8.9] Lunar Block Overlay v1",//https://www.curseforge.com/minecraft/mc-mods/lunar-block-overlay
            "[1.8.9] Lunar Block Overlay-2.0.0",//https://www.curseforge.com/minecraft/mc-mods/lunar-block-overlay
            "3dskinlayers-",                //https://www.curseforge.com/minecraft/mc-mods/skin-layers-3d
            "Absolutely-Not-A-Zoom-Mod-",   //https://www.curseforge.com/minecraft/mc-mods/absolutely-not-a-zoom-mod
            "AdaptiveTooltips-",            //https://www.curseforge.com/minecraft/mc-mods/adaptive-tooltips
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
            "Audio Improvements ",          //https://www.curseforge.com/minecraft/mc-mods/audio-improvements
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
            "BetterThanBunnies-",           //https://www.curseforge.com/minecraft/mc-mods/better-than-bunnies
            "BetterTaskbar-",               //https://www.curseforge.com/minecraft/mc-mods/better-taskbar
            "BetterThirdPerson",            //https://www.curseforge.com/minecraft/mc-mods/better-third-person
            "BetterTitleScreen-",           //https://www.curseforge.com/minecraft/mc-mods/better-title-screen
            "Blur-",                        //https://www.curseforge.com/minecraft/mc-mods/blur
            "BruteForceRenderingCulling-",  //https://www.curseforge.com/minecraft/mc-mods/brute-force-rendering-culling
            "BoccHUD-",                     //https://modrinth.com/mod/bocchud/
            "BorderlessWindow-",            //https://www.curseforge.com/minecraft/mc-mods/borderless
            "CTM-",                         //https://www.curseforge.com/minecraft/mc-mods/ctm
            "Chat Ping ",                   //https://www.curseforge.com/minecraft/mc-mods/chatping
            "ChunkAnimator-",               //https://www.curseforge.com/minecraft/mc-mods/chunk-animator
            "Clear-Water-",                 //https://www.curseforge.com/minecraft/mc-mods/clear-water
            "ClientTweaks_",                //https://www.curseforge.com/minecraft/mc-mods/client-tweaks
            "Cobbleit-",                    //https://www.curseforge.com/minecraft/mc-mods/cobblemon-cobble-it
            "CobblemonMoveInspector-",      //https://www.curseforge.com/minecraft/mc-mods/cobblemon-move-inspector
            "CompletionistsIndex-",         //https://www.curseforge.com/minecraft/mc-mods/completionists-index
            "Controller Support-",          //https://www.curseforge.com/minecraft/mc-mods/controller-mod
            "Controlling-",                 //https://www.curseforge.com/minecraft/mc-mods/controlling
            "CraftPresence-",               //https://www.curseforge.com/minecraft/mc-mods/craftpresence
            "CullLessLeaves-",              //https://www.curseforge.com/minecraft/mc-mods/cull-less-leaves & https://www.curseforge.com/minecraft/mc-mods/culllessleaves-reforged
            "CustomCursorMod-",             //https://www.curseforge.com/minecraft/mc-mods/custom-cursor
            "CustomMainMenu-",              //https://www.curseforge.com/minecraft/mc-mods/custom-main-menu
            "CutThrough-",                  //https://www.curseforge.com/minecraft/mc-mods/cut-through
            "DefaultOptions_",              //https://www.curseforge.com/minecraft/mc-mods/default-options
            "DefaultSettings-",             //https://www.curseforge.com/minecraft/mc-mods/defaultsettings
            "DeleteWorldsToTrash-",         //https://www.curseforge.com/minecraft/mc-mods/delete-worlds-to-trash-forge
            "DetailArmorBar-",              //https://www.curseforge.com/minecraft/mc-mods/detail-armor-bar-forge
            "Ding-",                        //https://www.curseforge.com/minecraft/mc-mods/ding
            "DripSounds-",                  //https://www.curseforge.com/minecraft/mc-mods/waterdripsound
            "Durability101-",               //https://www.curseforge.com/minecraft/mc-mods/durability101
            "DurabilityNotifier-",          //https://www.curseforge.com/minecraft/mc-mods/durability-notifier
            "DynamicSurroundings-",         //https://www.curseforge.com/minecraft/mc-mods/dynamic-surroundings
            "DynamicSurroundingsHuds-",     //https://www.curseforge.com/minecraft/mc-mods/dynamic-surroundings-huds
            "EasyLAN-",                     //https://www.curseforge.com/minecraft/mc-mods/easylan
            "EffectInsights-",              //https://www.curseforge.com/minecraft/mc-mods/effect-insights
            "EffectsLeft-",                 //https://www.curseforge.com/minecraft/mc-mods/effectsleft
            "EiraMoticons_",                //no longer available, legacy entry
            "EnchantmentDescriptions-",     //https://www.curseforge.com/minecraft/mc-mods/enchantment-descriptions
            "EnhancedTooltips-",            //https://www.curseforge.com/minecraft/mc-mods/enhancedtooltips
            "EnhancedVisuals_",             //https://www.curseforge.com/minecraft/mc-mods/enhancedvisuals
            "EquipmentCompare-",            //https://www.curseforge.com/minecraft/mc-mods/equipment-compare
            "EuphoriaPatcher-",             //https://www.curseforge.com/minecraft/mc-mods/euphoria-patches
            "FPS-Monitor-",                 //https://www.curseforge.com/minecraft/mc-mods/fps-monitor
            "Fabric-cobblemon_vocalized-",  //https://www.curseforge.com/minecraft/mc-mods/cobblemon-vocalized
            "FabricCustomCursorMod-",       //https://www.curseforge.com/minecraft/mc-mods/cursor-mod
            "FadingNightVision-",           //https://www.curseforge.com/minecraft/mc-mods/fading-night-vision
            "Fallingleaves-",               //https://www.curseforge.com/minecraft/mc-mods/falling-leaves-forge
            "FancySpawnEggs",               //https://www.curseforge.com/minecraft/mc-mods/fancy-spawn-eggs
            "FancyBlockParticles-",         //https://www.curseforge.com/minecraft/mc-mods/fbp-renewed
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
            "Gnetum-",                      //https://www.curseforge.com/minecraft/mc-mods/gnetum
            "GpuTape-",                     //https://www.curseforge.com/minecraft/mc-mods/gputape
            "GPUTape-",                     //https://www.curseforge.com/minecraft/mc-mods/gputape
            "HealthOverlay-",               //https://www.curseforge.com/minecraft/mc-mods/health-overlay
            "HeldItemTooltips-",            //https://www.curseforge.com/minecraft/mc-mods/held-item-tooltips
            "Hide_Nameplates-",             //https://www.curseforge.com/minecraft/mc-mods/hidenameplates
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
            "Kerria-",                      //https://www.curseforge.com/minecraft/mc-mods/kerria
            "KeybindsPurger-",              //https://www.curseforge.com/minecraft/mc-mods/keybindspurger
            "KeepTheResourcePack-",         //https://www.curseforge.com/minecraft/mc-mods/keep-the-resourcepack
            "KeybindsPurger-",              //https://www.curseforge.com/minecraft/mc-mods/keybindspurger/
            "LeaveMyBarsAlone-",            //https://www.curseforge.com/minecraft/mc-mods/leave-my-bars-alone
            "LLOverlayReloaded-",           //https://www.curseforge.com/minecraft/mc-mods/light-level-overlay-reloaded
            "LongerChatHistory-",           //https://www.curseforge.com/minecraft/mc-mods/longer-chat-history
            "LOTRDRP-",                     //https://www.curseforge.com/minecraft/mc-mods/lotr-drp
            "LegendaryTooltips",            //https://www.curseforge.com/minecraft/mc-mods/legendary-tooltips
            "LegendaryTooltips-",           //https://www.curseforge.com/minecraft/mc-mods/legendary-tooltips
            "LightOverlay-",                //https://www.curseforge.com/minecraft/mc-mods/light-level-overlay-display
            "MaFgLib-",                     //https://modrinth.com/mod/mafglib
            "MenuFPSUnlocker-",             //https://www.curseforge.com/minecraft/mc-mods/menufpsunlocker
            "MinecraftCapes ",              //https://www.curseforge.com/minecraft/mc-mods/minecraftcapes-mod
            "MineMenu-",                    //https://www.curseforge.com/minecraft/mc-mods/minemenu
            "MoBends",                      //https://www.curseforge.com/minecraft/mc-mods/mo-bends
            "Mocap-",                       //https://www.curseforge.com/minecraft/mc-mods/motion-capture-mod-mocap
            "ModernUI-",                    //Gone? Reduces to atoms?
            "MoreCobblemonTweaks-",         //https://www.curseforge.com/minecraft/mc-mods/morecobblemontweaks
            "MouseTweaks-",                 //https://www.curseforge.com/minecraft/mc-mods/mouse-tweaks
            "MovingSlots-",                 //https://www.curseforge.com/minecraft/mc-mods/moving-slots
            "MyServerIsCompatible-",        //https://www.curseforge.com/minecraft/mc-mods/my-server-is-compatible
            "Neat ",                        //https://www.curseforge.com/minecraft/mc-mods/neat
            "Neat-",                        //https://www.curseforge.com/minecraft/mc-mods/neat
            "NekosEnchantedBooks-",         //https://www.curseforge.com/minecraft/mc-mods/nekos-enchanted-books
            "NeoForge-cobblemon_vocalized-",//https://www.curseforge.com/minecraft/mc-mods/cobblemon-vocalized
            "NoAutoJump-",                  //https://www.curseforge.com/minecraft/mc-mods/no-autojump
            "NoFog-",                       //https://www.curseforge.com/minecraft/mc-mods/nofog
            "NoPackCompatCheck-",           //https://www.curseforge.com/minecraft/mc-mods/nopackcompatcheck
            "Notes-",                       //https://www.curseforge.com/minecraft/mc-mods/notes
            "NotifMod-",                    //https://www.curseforge.com/minecraft/mc-mods/notifmod
            "OldJavaWarning-",              //https://www.curseforge.com/minecraft/mc-mods/oldjavawarning
            "OptiFine",                     //https://optifine.net/home
            "OptiFine_",                    //https://optifine.net/home
            "OptiForge",                    //https://www.curseforge.com/minecraft/mc-mods/optiforge
            "OptiForge-",                   //https://www.curseforge.com/minecraft/mc-mods/optiforge
            "OverflowingBars-",             //https://www.curseforge.com/minecraft/mc-mods/overflowing-bars
            "PackMenu-",                    //https://www.curseforge.com/minecraft/mc-mods/packmenu
            "PackModeMenu-",                //https://www.curseforge.com/minecraft/mc-mods/packmodemenu,
            "ParticleEffects-",             //https://www.curseforge.com/minecraft/mc-mods/particle-effects
            "Particle Effects-",            //https://www.curseforge.com/minecraft/mc-mods/particle-effects
            "PartiCull-",                   //https://www.curseforge.com/minecraft/mc-mods/particull
            "Perception-",                  //https://www.curseforge.com/minecraft/mc-mods/perception
            "PickUpNotifier-",              //https://www.curseforge.com/minecraft/mc-mods/pick-up-notifier
            "Ping-",                        //https://www.curseforge.com/minecraft/mc-mods/ping
            "PingHUD-",                     //https://www.curseforge.com/minecraft/mc-mods/pinghud
            "PlayerListHeads-",             //https://www.curseforge.com/minecraft/mc-mods/player-list-heads
            "PresenceFootsteps-",           //https://www.curseforge.com/minecraft/mc-mods/presence-footsteps
            "RPG-HUD-",                     //https://www.curseforge.com/minecraft/mc-mods/rpg-hud
            "RPRenames-",                   //https://modrinth.com/mod/rp-renames
            "ReAuth-",                      //https://www.curseforge.com/minecraft/mc-mods/reauth
            "Redstone Sound Slider-",       //https://www.curseforge.com/minecraft/mc-mods/redstone-sound-slider
            "Reforgium-",                   //https://www.curseforge.com/minecraft/mc-mods/reforgium
            "ResourceLoader-",              //https://www.curseforge.com/minecraft/mc-mods/resource-reloader
            "ResourcePackOrganizer",        //https://www.curseforge.com/minecraft/mc-mods/resource-pack-organizer
            "ResourcePackOverrides-",       //https://www.curseforge.com/minecraft/mc-mods/resource-pack-overrides
            "RocknRoller-",                 //https://www.curseforge.com/minecraft/mc-mods/rockn-roller
            "Ryoamiclights-",               //https://www.curseforge.com/minecraft/mc-mods/ryoamiclights
            "RyoamicLights-",               //https://www.curseforge.com/minecraft/mc-mods/ryoamiclights
            "ShoulderSurfing-",             //https://www.curseforge.com/minecraft/mc-mods/shoulder-surfing-reloaded
            "ShulkerTooltip-",              //https://www.curseforge.com/minecraft/mc-mods/shulkerboxtooltip
            "SimpleDiscordRichPresence-",   //https://www.curseforge.com/minecraft/mc-mods/simple-discord-rich-presence
            "SimpleWorldTimer-",            //https://www.curseforge.com/minecraft/mc-mods/simple-world-timer
            "SoundFilters-",                //https://www.curseforge.com/minecraft/mc-mods/sound-filters
            "Sounds-",                      //https://www.curseforge.com/minecraft/mc-mods/sound
            "SourceHop-",                   //https://www.curseforge.com/minecraft/mc-mods/sourcehop
            "SpawnerFix-",                  //https://www.curseforge.com/minecraft/mc-mods/spawner-fix
            "StylishEffects-",              //https://www.curseforge.com/minecraft/mc-mods/stylish-effects
            "TextruesRubidiumOptions-",     //https://www.curseforge.com/minecraft/mc-mods/textrues-rubidium-options
            "Threads-",                     //https://www.curseforge.com/minecraft/mc-mods/threads
            "TRansliterationLib-",          //https://www.curseforge.com/minecraft/mc-mods/transliterationlib
            "TipTheScales-",                //https://www.curseforge.com/minecraft/mc-mods/tipthescales
            "Tips-",                        //https://www.curseforge.com/minecraft/mc-mods/tips
            "Toast Control-",               //https://www.curseforge.com/minecraft/mc-mods/toast-control
            "Toast-Control-",               //https://www.curseforge.com/minecraft/mc-mods/toast-control
            "ToastControl-",                //https://www.curseforge.com/minecraft/mc-mods/toast-control
            "TravelersTitles-",             //https://www.curseforge.com/minecraft/mc-mods/travelers-titles
            "UIQuest-",                     //https://www.curseforge.com/minecraft/mc-mods/uiquest
            "VoidFog-",                     //https://www.curseforge.com/minecraft/mc-mods/void-fog
            "VR-Combat_",                   //https://www.curseforge.com/minecraft/mc-mods/vr-combat
            "Vramo21-",                     //https://www.curseforge.com/minecraft/mc-mods/vramo
            "Vramo-",                       //https://www.curseforge.com/minecraft/mc-mods/vramo
            "vramo-",                       //https://www.curseforge.com/minecraft/mc-mods/vramo
            "WI-Zoom-",                     //https://www.curseforge.com/minecraft/mc-mods/wi-zoom/
            "WeatherRefind-",               //https://www.curseforge.com/minecraft/mc-mods/weather-refined
            "WindowedFullscreen-",          //https://www.curseforge.com/minecraft/mc-mods/windowed-fullscreen
            "Windy_",                       //https://www.curseforge.com/minecraft/mc-mods/windy-configurable
            "WorldNameRandomizer-",         //https://www.curseforge.com/minecraft/mc-mods/world-name-randomizer
            "YeetusExperimentus-",          //https://www.curseforge.com/minecraft/mc-mods/yeetusexperimentus
            "YungsMenuTweaks-",             //https://www.curseforge.com/minecraft/mc-mods/yungs-menu-tweaks
            "[1.12.2]DamageIndicatorsMod-", //https://www.curseforge.com/minecraft/mc-mods/damage-indicators-mod
            "[1.12.2]bspkrscore-",          //https://www.curseforge.com/minecraft/mc-mods/bspkrscore
            "acceleratedrendering-",        //https://www.curseforge.com/minecraft/mc-mods/accelerated-rendering
            "advancementscreenshot-",       //https://www.curseforge.com/minecraft/mc-mods/advancement-screenshot
            "ae_pattern_improve-",          //https://www.curseforge.com/minecraft/mc-mods/ae2-pattern-qol-improving
            "ahznbstools-",                 //https://www.curseforge.com/minecraft/mc-mods/ahznbs-tools/
            "aiftbtranslator-",             //https://www.curseforge.com/minecraft/mc-mods/ai-ftb-translator
            "antighost-",                   //https://www.curseforge.com/minecraft/mc-mods/antighost
            "anviltooltipmod-",             //https://www.curseforge.com/minecraft/mc-mods/anvil-tooltip-mod
            "appliedsorting-",              //https://www.curseforge.com/minecraft/mc-mods/applied-sorting
            "armorchroma-",                 //https://www.curseforge.com/minecraft/mc-mods/armor-chroma
            "armorhud",                     //https://www.curseforge.com/minecraft/mc-mods/armor-durability-hud
            "armorpointspp-",               //https://www.curseforge.com/minecraft/mc-mods/armorpoints
            "asynclogger-",                 //https://www.curseforge.com/minecraft/mc-mods/asynclogger
            "auditory-",                    //https://www.curseforge.com/minecraft/mc-mods/auditory
            "authme-",                      //Gone? Reduces to atoms?
            "auto-reconnect-",              //https://www.curseforge.com/minecraft/mc-mods/auto-reconnect
            "autojoin-",                    //https://www.curseforge.com/minecraft/mc-mods/autojoin
            "autoreconnect-",               //https://www.curseforge.com/minecraft/mc-mods/autoreconnect
            "autoswap-",                    //https://www.curseforge.com/minecraft/mc-mods/auto-swap
            "axolotl-item-fix-",            //Gone? Reduces to atoms?
            "backtools-",                   //https://www.curseforge.com/minecraft/mc-mods/backtools
            "bannerunlimited-",             //https://www.curseforge.com/minecraft/mc-mods/banner-unlimited
            "bbs-",                         //https://www.curseforge.com/minecraft/mc-mods/bbs-mod
            "beddium-",                     //https://www.curseforge.com/minecraft/mc-mods/beddium
            "beenfo-",                      //https://www.curseforge.com/minecraft/mc-mods/beenfo
            "better_client",                //https://www.curseforge.com/minecraft/mc-mods/better-client
            "better_tab-",                  //https://www.curseforge.com/minecraft/mc-mods/bettertabinfo
            "better_tooltips-",             //https://www.curseforge.com/minecraft/mc-mods/better-tooltips-neoforge
            "better-clouds-",               //Gone? Reduces to atoms?
            "better_hp-",                   //https://www.curseforge.com/minecraft/mc-mods/better-hp
            "better-hp-",                   //https://www.curseforge.com/minecraft/mc-mods/better-hp
            "betterHP_",                    //https://www.curseforge.com/minecraft/mc-mods/better-hp
            "better-recipe-book-",          //Gone? Reduces to atoms?
            "betterbiomeblend-",            //https://www.curseforge.com/minecraft/mc-mods/better-biome-blend
            "bhmenu-",                      //https://www.curseforge.com/minecraft/mc-mods/bisecthosting-server-integration-menu-forge & https://www.curseforge.com/minecraft/mc-mods/bisecthosting-server-integration-menu-fabric & https://www.curseforge.com/minecraft/mc-mods/bisecthosting-server-integration-menu-neoforge
            "biomemusic-",                  //https://www.curseforge.com/minecraft/mc-mods/biome-music
            "blinkload-",                   //https://www.curseforge.com/minecraft/mc-mods/blinkload
            "block-counter-",               //https://www.curseforge.com/minecraft/mc-mods/block-counter
            "block_entity_render_distance-fork",//https://www.curseforge.com/minecraft/mc-mods/block-entity-render-distance-x-sinytra-connector
            "blur-",                        //https://www.curseforge.com/minecraft/mc-mods/blur
            "bocchium-",                    //https://www.curseforge.com/minecraft/mc-mods/bocchium
            "borderless-",                  //https://www.curseforge.com/minecraft/mc-mods/borderless
            "cat_jam-",                     //https://www.curseforge.com/minecraft/mc-mods/cat_jam
            "catalogue-",                   //https://www.curseforge.com/minecraft/mc-mods/catalogue
            "catchindicator-",              //https://www.curseforge.com/minecraft/mc-mods/catch-indicator
            "catchrate-display-",           //https://www.curseforge.com/minecraft/mc-mods/cobblemon-catch-rate-display
            "cave_dust-",                   //https://www.curseforge.com/minecraft/mc-mods/cave-dust
            "certain_questing_additions-",  //https://www.curseforge.com/minecraft/mc-mods/certain-questing-additions
            "cfwinfo-",                     //https://www.curseforge.com/minecraft/mc-mods/create-fuel-and-water-information
            "chestsearchbar-",              //https://www.curseforge.com/minecraft/mc-mods/chest-search-bar
            "charmonium-",                  //https://www.curseforge.com/minecraft/mc-mods/charmonium
            "chatnotify-",                  //https://www.curseforge.com/minecraft/mc-mods/chatnotify
            "chat_heads-",                  //https://www.curseforge.com/minecraft/mc-mods/chat-heads
            "cherishedworlds-",             //https://www.curseforge.com/minecraft/mc-mods/cherished-worlds
            "chloride-",                    //https://www.curseforge.com/minecraft/mc-mods/chloride
            "cirback-1.0-",                 //Gone? Reduces to atoms?
            "citresewn-",                   //https://www.curseforge.com/minecraft/mc-mods/forge-cit
            "classic-c418-music-tweaker-",  //https://www.curseforge.com/minecraft/mc-mods/classic-c418-music-tweaker
            "classicbar-",                  //https://www.curseforge.com/minecraft/mc-mods/classic-bars
            "cleanview",                    //https://www.curseforge.com/minecraft/mc-mods/clean-view
            "clientcrafting-",              //https://www.curseforge.com/minecraft/mc-mods/client-crafting
            "clienttweaks-",                //https://www.curseforge.com/minecraft/mc-mods/client-tweaks
            "createbetterfps-",             //https://www.curseforge.com/minecraft/mc-mods/create-better-fps
            "cobeffectiveness-",            //https://www.curseforge.com/minecraft/mc-mods/cobblemon-effectiveness
            "cobbledex-rei-emi-jei-",       //https://www.curseforge.com/minecraft/mc-mods/cobbledex-rei-emi-jei
            "cobbleit-",                    //https://www.curseforge.com/minecraft/mc-mods/cobblemon-cobble-it
            "cobblemonbattletypes-",        //https://www.curseforge.com/minecraft/mc-mods/cobblemon-in-battle-type-icons
            "cobblemontypechart-",          //https://www.curseforge.com/minecraft/mc-mods/pokemon-type-table-cobblemon-pixelmon
            "cobblemon_emi_compat-",        //https://www.curseforge.com/minecraft/mc-mods/cobblemon-emi-compat
            "cobblemon_iwa-",               //https://www.curseforge.com/minecraft/mc-mods/cobblemon-iwa
            "cobblemon-ui-tweaks-",         //https://modrinth.com/mod/cobblemon-ui-tweaks
            "combat_music-",                //https://www.curseforge.com/minecraft/mc-mods/combat-music
            "configured-",                  //https://www.curseforge.com/minecraft/mc-mods/configured
            "connectedness-",               //https://www.curseforge.com/minecraft/mc-mods/connectedness
            "controllable-",                //https://www.curseforge.com/minecraft/mc-mods/controllable
            "coolrain-",                    //https://www.curseforge.com/minecraft/mc-mods/cool-rain
            "crash_assistant-",             //https://www.curseforge.com/minecraft/mc-mods/crash-assistant
            "colorful_lighting-",           //https://www.curseforge.com/minecraft/mc-mods/colorful-lighting-sodium
            "colorwheel-",                  //https://www.curseforge.com/minecraft/mc-mods/colorwheel
            "colorwheel_patcher-",          //https://www.curseforge.com/minecraft/mc-mods/colorwheel-patcher
            "cubium-",                      //https://www.curseforge.com/minecraft/mc-mods/cubium
            "cullleaves-",                  //https://www.curseforge.com/minecraft/mc-mods/cull-leaves
            "cullparticles-",               //https://www.curseforge.com/minecraft/mc-mods/cull-particles
            "currentgamemusictrack-",       //https://www.curseforge.com/minecraft/mc-mods/current-game-music-track
            "custom-crosshair-mod-",        //https://www.curseforge.com/minecraft/mc-mods/custom-crosshair-mod
            "customcursor-",                //https://www.curseforge.com/minecraft/mc-mods/custom-cursor
            "customdiscordrpc-",            //https://www.curseforge.com/minecraft/mc-mods/custom-discordrpc
            "cwb-",                         //https://www.curseforge.com/minecraft/mc-mods/cubes-without-borders
            "dahud-",                       //https://www.curseforge.com/minecraft/mc-mods/dahud-medieval-rpg-hud
            "darkmodeeverywhere-",          //https://www.curseforge.com/minecraft/mc-mods/dark-mode-everywhere/
            "darkness-",                    //Gone? Reduces to atoms?
            "dashloader-",                  //https://www.curseforge.com/minecraft/mc-mods/dashloader
            "deathlogplus-",                //https://www.curseforge.com/minecraft/mc-mods/deathlogplus
            "defaultoptions-",              //https://www.curseforge.com/minecraft/mc-mods/default-options
            "desiredservers-",              //https://www.curseforge.com/minecraft/mc-mods/desired-servers
            "discordrpc-",                  //https://www.curseforge.com/minecraft/mc-mods/discordrpc
            "distraction_free_recipes-",    //https://www.curseforge.com/minecraft/mc-mods/distraction-free-recipes
            "drippyloadingscreen-",         //https://www.curseforge.com/minecraft/mc-mods/drippy-loading-screen
            "drippyloadingscreen_",         //https://www.curseforge.com/minecraft/mc-mods/drippy-loading-screen
            "drop-confirm-",                //
            "durabilitytooltip-",           //https://www.curseforge.com/minecraft/mc-mods/durability-tooltip
            "dynamic-fps-",                 //https://www.curseforge.com/minecraft/mc-mods/dynamic-fps
            "dynamic-music-",               //https://www.curseforge.com/minecraft/mc-mods/dynamic-music
            "dynamiccrosshair-",            //https://www.curseforge.com/minecraft/mc-mods/dynamic-crosshair
            "dynamiclights-",               //https://www.curseforge.com/minecraft/mc-mods/dynamic-lights
            "dynamiclightsreforged-",       //https://www.curseforge.com/minecraft/mc-mods/dynamiclights-reforged
            "dynmus-",                      //Gone? Reduces to atoms?
            "e4mc-",                        //https://www.curseforge.com/minecraft/mc-mods/e4mc
            "easymt-",                      //https://www.curseforge.com/minecraft/mc-mods/easy-melee-tempo
            "effective-",                   //https://www.curseforge.com/minecraft/mc-mods/effective
            "eggtab-",                      //https://www.curseforge.com/minecraft/mc-mods/eggtab-fabric
            "eguilib-",                     //https://www.curseforge.com/minecraft/mc-mods/eguilib
            "eiramoticons-",                //Gone? Reduces to atoms?
            "embeddium-",                   //https://www.curseforge.com/minecraft/mc-mods/embeddium
            "enchantment-lore-",            //https://www.curseforge.com/minecraft/mc-mods/enchantment-lore
            "enhanced_boss_bars-",          //https://www.curseforge.com/minecraft/mc-mods/enhanced-boss-bars
            "entity-texture-features-",     //https://www.curseforge.com/minecraft/mc-mods/entity-texture-features-fabric
            "entity_texture_features-",     //https://www.curseforge.com/minecraft/mc-mods/entity-texture-features-fabric
            "entity_model_features_",       //https://www.curseforge.com/minecraft/mc-mods/entity-model-features
            "entityculling-",               //https://www.curseforge.com/minecraft/mc-mods/entity-culling
            "essential_",                   //Gone? Reduces to atoms?
            "evonotify-",                   //https://www.curseforge.com/minecraft/mc-mods/cobblemon-evonotify
            "exhaustedstamina-",            //https://www.curseforge.com/minecraft/mc-mods/exhausted-stamina
            "extendedhitbox-",              //https://www.curseforge.com/minecraft/mc-mods/extended-hitbox
            "extremesoundmuffler-",         //https://www.curseforge.com/minecraft/mc-mods/extreme-sound-muffler
            "fabricemotes-",                //https://www.curseforge.com/minecraft/mc-mods/fabric-emotes
            "fall_damage_preview-",         //https://www.curseforge.com/minecraft/mc-mods/fall-damage-preview
            "fancymenu_",                   //https://www.curseforge.com/minecraft/mc-mods/fancymenu
            "fancymenu_video_extension",    //https://www.curseforge.com/minecraft/mc-mods/video-extension-for-fancymenu-forge
            "fast-ip-ping-",                //https://www.curseforge.com/minecraft/mc-mods/fast-ip-ping
            "fastquit-",                    //https://www.curseforge.com/minecraft/mc-mods/fastquit-forge
            "firstperson-",                 //https://www.curseforge.com/minecraft/mc-mods/first-person-model
            "flerovium-",                   //https://www.curseforge.com/minecraft/mc-mods/flerovium
            "flickerfix-",                  //https://www.curseforge.com/minecraft/mc-mods/flickerfix
            "fm_audio_extension_",          //https://www.curseforge.com/minecraft/mc-mods/audio-extension-for-fancymenu-forge
            "fabricmod_VoxelMap-",          //https://www.curseforge.com/minecraft/mc-mods/voxelmap
            "fastspawner-",                 //https://www.curseforge.com/minecraft/mc-mods/fastspawner
            "floppyhud-",                   //https://www.curseforge.com/minecraft/mc-mods/floppy-hud
            "fpsbooster-",                  //https://www.curseforge.com/minecraft/mc-mods/fps-booster-triio
            "forestryworktabledisplay-",    //https://www.curseforge.com/minecraft/mc-mods/forestry-worktable-display
            "forgemod_VoxelMap-",           //https://www.curseforge.com/minecraft/mc-mods/voxelmap
            "forgeshot-",                   //https://www.curseforge.com/minecraft/mc-mods/forgeshot
            "freecam-",                     //https://www.curseforge.com/minecraft/mc-mods/free-cam
            "freelook-",                    //https://www.curseforge.com/minecraft/mc-mods/freelook
            "ftbpromoter-",                 //https://www.curseforge.com/minecraft/mc-mods/ftb-promoter/
            "fullbrightnesstoggle-",        //https://www.curseforge.com/minecraft/mc-mods/full-brightness-toggle
            "fwa+",                         //https://www.curseforge.com/minecraft/mc-mods/fwa
            "galacticraft-rpc-",            //https://www.curseforge.com/minecraft/mc-mods/galacticraft-rpc
            "gamestagesviewer-",            //https://www.curseforge.com/minecraft/mc-mods/game-stages-viewer
            "gbf-",                         //https://www.curseforge.com/minecraft/mc-mods/geckolibbetterfps
            "gpushift-",                    //https://www.curseforge.com/minecraft/mc-mods/gpushift
            "gpumemleakfix-",               //https://www.curseforge.com/minecraft/mc-mods/fix-gpu-memory-leak
            "grid-",                        //https://www.curseforge.com/minecraft/mc-mods/grid
            "guiclock-",                    //https://www.curseforge.com/minecraft/mc-mods/gui-clock
            "guicompass-",                  //https://www.curseforge.com/minecraft/mc-mods/gui-compass
            "guideme-",                     //https://www.curseforge.com/minecraft/mc-mods/guideme
            "guifollowers-",                //https://www.curseforge.com/minecraft/mc-mods/gui-followers
            "hdr_mod-",                     //https://www.curseforge.com/minecraft/mc-mods/shaders-hdr
            "helium-",                      //Gone? Reduces to atoms?
            "hennyfullbright-",             //https://www.curseforge.com/minecraft/mc-mods/henny-fullbright
            "hidehud-",                     //https://www.curseforge.com/minecraft/mc-mods/hidehud
            "hidenameplates-",              //https://www.curseforge.com/minecraft/mc-mods/hidenameplates
            "hiddenrecipebook_",            //https://www.curseforge.com/minecraft/mc-mods/hidden-recipe-book
            "hiddenrecipebook-",            //https://www.curseforge.com/minecraft/mc-mods/hidden-recipe-book
            "hidehands-",                   //https://www.curseforge.com/minecraft/mc-mods/hide-hands
            "idle_boost-",                  //https://www.curseforge.com/minecraft/mc-mods/idle-boost
            "ijmtweaks-",                   //https://www.curseforge.com/minecraft/mc-mods/ijm-tweaks
            "immersivearmorhud-",           //https://www.curseforge.com/minecraft/mc-mods/immersive-armor-hud
            "immersivelanterns-",           //https://www.curseforge.com/minecraft/mc-mods/immersive-lanterns
            "immersivemessages-",           //https://www.curseforge.com/minecraft/mc-mods/immersive-messages-api
            "immersivetips-",               //https://www.curseforge.com/minecraft/mc-mods/immersive-tips
            "improvedsignediting-",         //https://www.curseforge.com/minecraft/mc-mods/improved-sign-editing
            "increase_audio_streams-",      //https://www.curseforge.com/minecraft/mc-mods/increase-audio-streams
            "infinitemusic-",               //https://www.curseforge.com/minecraft/mc-mods/infinite-music
            "inline_tooltips-",             //https://www.curseforge.com/minecraft/mc-mods/inline-tooltips
            "inventoryhud.",                //https://www.curseforge.com/minecraft/mc-mods/inventory-hud-forge
            "inventoryprofiles",            //https://www.curseforge.com/minecraft/mc-mods/inventory-profiles
            "irisblockcompat-",             //https://www.curseforge.com/minecraft/mc-mods/iris-block-compat
            "itemzoom",                     //https://www.curseforge.com/minecraft/mc-mods/itemzoom
            "itlt-",                        //https://www.curseforge.com/minecraft/mc-mods/its-the-little-things
            "jeed-",                        //https://www.curseforge.com/minecraft/mc-mods/just-enough-effect-descriptions-jeed
            "jehc-",                        //https://www.curseforge.com/minecraft/mc-mods/just-enough-harvestcraft
            "jei_hover_search-",            //https://www.curseforge.com/minecraft/mc-mods/jei-hover-search
            "jei_trim_hider-",              //https://www.curseforge.com/minecraft/mc-mods/jei-trim-hider
            "jeiintegration_",              //https://www.curseforge.com/minecraft/mc-mods/jei-integration
            "jerintegration-",              //https://www.curseforge.com/minecraft/mc-mods/jer-integration
            "jeioptimizer",                 //https://www.curseforge.com/minecraft/mc-mods/jeioptimizer
            "jmi-",                         //https://www.curseforge.com/minecraft/mc-mods/journeymap-integration
            "jumpoverfences-",              //https://www.curseforge.com/minecraft/mc-mods/jumpoverfences
            "just-enough-harvestcraft-",    //https://www.curseforge.com/minecraft/mc-mods/just-enough-harvestcraft
            "justenoughbeacons-",           //https://www.curseforge.com/minecraft/mc-mods/just-enough-beacons
            "justenoughdrags-",             //https://www.curseforge.com/minecraft/mc-mods/just-enough-drags
            "justzoom_",                    //https://www.curseforge.com/minecraft/mc-mods/just-zoom
            "keybindspurger-",              //https://www.curseforge.com/minecraft/mc-mods/keybindspurger
            "keymap-",                      //https://www.curseforge.com/minecraft/mc-mods/keymap
            "keywizard-",                   //https://www.curseforge.com/minecraft/mc-mods/keyboard-wizard
            "lazurite-",                    //https://www.curseforge.com/minecraft/mc-mods/lazurite
            "lazydfu-",                     //https://www.curseforge.com/minecraft/mc-mods/lazydfu
            "lib39-",                       //https://www.curseforge.com/minecraft/mc-mods/lib39
            "light-overlay-",               //https://www.curseforge.com/minecraft/mc-mods/light-overlay
            "lightfallclient-",             //https://www.curseforge.com/minecraft/mc-mods/lightfallclient-updated
            "lightspeed-",                  //https://www.curseforge.com/minecraft/mc-mods/lightspeedmod
                                            //https://www.curseforge.com/minecraft/mc-mods/lightspeedre-launch-optimizations
            "litematica-",                  //https://www.curseforge.com/minecraft/mc-mods/litematica-update-port
            "loadmyresources_",             //https://www.curseforge.com/minecraft/mc-mods/load-my-resources-forge
            "lock_minecart_view-",          //Gone? Reduces to atoms?
            "lootbeams-",                   //https://www.curseforge.com/minecraft/mc-mods/lootbeams
            "lwl-",                         //Gone? Reduces to atoms?
            "macos-input-fixes-",           //https://www.curseforge.com/minecraft/mc-mods/macos-input-fixes
            "magnesium_extras-",            //Gone? Reduces to atoms?
            "maptooltip-",                  //https://www.curseforge.com/minecraft/mc-mods/map-tooltip
            "massunbind",                   //https://www.curseforge.com/minecraft/mc-mods/mass-key-unbinder
            "mcbindtype-",                  //https://www.curseforge.com/minecraft/mc-mods/mcbindtype
            "mcqoy-",                       //https://modrinth.com/mod/mcqoy
            "mcwifipnp-",                   //https://www.curseforge.com/minecraft/mc-mods/mcwifipnp
            "medievalmusic-",               //https://www.curseforge.com/minecraft/mc-mods/medieval-music
            "mekalus-",                     //https://www.curseforge.com/minecraft/mc-mods/mekalus-oculus-fork-with-fixed-mekanism-mekasuit
            "memoryusagescreen-",           //https://www.curseforge.com/minecraft/mc-mods/memory-usage-screen
            "mightyarchitect-",             //https://www.curseforge.com/minecraft/mc-mods/the-mighty-architect
            "mindful-eating-",              //https://www.curseforge.com/minecraft/mc-mods/mindful-eating
            "minetogether-",                //https://www.curseforge.com/minecraft/mc-mods/creeperhost-minetogether
            "minihud-",                     //https://www.curseforge.com/minecraft/mc-mods/minihud-update-port
            "miningspeedtooltips-",         //https://www.curseforge.com/minecraft/mc-mods/mining-speed-tooltips
            "moremmog'scheats",             //https://www.curseforge.com/minecraft/mc-mods/mmogs-cheat-menu
            "mmog'scheats3.6kdownloadsplusmorecheats",//https://www.curseforge.com/minecraft/mc-mods/mmogs-cheat-menu
            "mobplusplus-",                 //Gone? Reduces to atoms?
            "modcredits-",                  //https://www.curseforge.com/minecraft/mc-mods/mod-credits
            "modernworldcreation_",         //https://www.curseforge.com/minecraft/mc-mods/modernworldcreation
            "modnametooltip-",              //https://www.curseforge.com/minecraft/mc-mods/mod-name-tooltip
            "modnametooltip_",              //https://www.curseforge.com/minecraft/mc-mods/mod-name-tooltip
            "modtabs-",                     //https://www.curseforge.com/minecraft/mc-mods/mod-tabs
            "moreoverlays-",                //https://www.curseforge.com/minecraft/mc-mods/more-overlays
            "mousewheelie-",                //https://www.curseforge.com/minecraft/mc-mods/mouse-wheelie
            "movement-vision-",             //https://www.curseforge.com/minecraft/mc-mods/movement-vision
            "multihotbar-",                 //https://www.curseforge.com/minecraft/mc-mods/multi-hotbar
            "music_delay_reducer-",         //https://www.curseforge.com/minecraft/mc-mods/music-delay-reducer/
            "music-duration-reducer-",      //https://www.curseforge.com/minecraft/mc-mods/music-duration-reducer
            "musicdr-",                     //Gone? Reduces to atoms?
            "neoculus-",                    //https://www.curseforge.com/minecraft/mc-mods/neoculus
            "nbt_glint-",                   //https://www.curseforge.com/minecraft/mc-mods/nbt-glint
            "neiRecipeHandlers-",           //Gone? Reduces to atoms?
            "ngrok-lan-expose-mod-",        //Gone? Reduces to atoms?
            "no_nv_flash-",                 //https://www.curseforge.com/minecraft/mc-mods/no-nv-flash
            "no_search_bar-",               //https://www.curseforge.com/minecraft/mc-mods/remove-search-bar
            "nomorepopups-",                //https://www.curseforge.com/minecraft/mc-mods/no-more-popups
            "nopotionshift_",               //https://www.curseforge.com/minecraft/mc-mods/no-potion-shift
            "nostartupmessages-",           //https://www.curseforge.com/minecraft/mc-mods/no-startup-messages-please
            "notenoughanimations-",         //https://www.curseforge.com/minecraft/mc-mods/not-enough-animations
            "obe+",                         //https://www.curseforge.com/minecraft/mc-mods/obe
            "obscure_tooltips_fix-",        //https://www.curseforge.com/minecraft/mc-mods/obscure-tooltips-fix
            "oculus-",                      //https://www.curseforge.com/minecraft/mc-mods/oculus
            "ocs-",                         //https://www.curseforge.com/minecraft/mc-mods/optimization-of-campfire-smoke
            "omegamute-",                   //https://www.curseforge.com/minecraft/mc-mods/omega-mute
            "optigui-",                     //https://www.curseforge.com/minecraft/mc-mods/optigui
            "ornaments-",                   //https://www.curseforge.com/minecraft/mc-mods/ornaments
            "overlaytweaks-",               //https://www.curseforge.com/minecraft/mc-mods/overlay-tweaks
            "overloadedarmorbar-",          //https://www.curseforge.com/minecraft/mc-mods/overloaded-armor-bar
            "panorama-",                    //https://www.curseforge.com/minecraft/mc-mods/panorama
            "paperdoll-",                   //https://www.curseforge.com/minecraft/mc-mods/paperdoll
            "particle-rain-",               //https://www.curseforge.com/minecraft/mc-mods/particle-rain
            "perdimensionbrightness-",      //https://www.curseforge.com/minecraft/mc-mods/per-dimension-brightness
            "persistentinventorysearch-",   //https://www.curseforge.com/minecraft/mc-mods/persistent-inventory-search
            "physics-mod-",                 //https://www.curseforge.com/minecraft/mc-mods/physics-mod
            "phosphor-",                    //https://www.curseforge.com/minecraft/mc-mods/phosphor
            "portraitcraft-",               //https://www.curseforge.com/minecraft/mc-mods/portraitcraft
            "preciseblockplacing-",         //Gone? Reduces to atoms?
            "radon-",                       //https://www.curseforge.com/minecraft/mc-mods/radon
            "rcgameshark-client-",          //https://www.curseforge.com/minecraft/mc-mods/rc-gameshark
            "realm-of-lost-souls-",         //https://www.curseforge.com/minecraft/mc-mods/bobs-realm-of-lost-souls
            "rebind_narrator-",             //https://www.curseforge.com/minecraft/mc-mods/rebind-narrator
            "rebind-narrator-",             //https://www.curseforge.com/minecraft/mc-mods/rebind-narrator
            "rebindnarrator-",              //https://www.curseforge.com/minecraft/mc-mods/rebind-narrator
            "rebrand-",                     //https://www.curseforge.com/minecraft/mc-mods/rebrand
            "reflex-",                      //https://www.curseforge.com/minecraft/mc-mods/reflex-antilag
            "reforgium-",                   //https://www.curseforge.com/minecraft/mc-mods/reforgium
            "renderscale-",                 //https://www.curseforge.com/minecraft/mc-mods/renderscale
            "relictium-",                   //https://www.curseforge.com/minecraft/mc-mods/relictium
            "replanter-",                   //https://www.curseforge.com/minecraft/mc-mods/replanter
            "resource_gamma_util-",         //https://www.curseforge.com/minecraft/mc-mods/resource-gamma-utils
            "rrls-",                        //https://www.curseforge.com/minecraft/mc-mods/rrls
            "rubidium-",                    //https://www.curseforge.com/minecraft/mc-mods/rubidium
            "rubidium_extras-",             //https://www.curseforge.com/minecraft/mc-mods/rubidium-extra
            "sclp-",                        //https://www.curseforge.com/minecraft/mc-mods/sodium-chinese-localization-package1-16-x
            "screenshot-to-clipboard-",     //https://www.curseforge.com/minecraft/mc-mods/screenshot-to-clipboard
            "seasonhud-",                   //https://www.curseforge.com/minecraft/mc-mods/seasonhud
            "servercountryflags-",          //https://www.curseforge.com/minecraft/mc-mods/server-country-flags
            "shut_up_gl_error-",            //https://www.curseforge.com/minecraft/mc-mods/shut-up-gl-error
            "shutupexperimentalsettings-",  //https://www.curseforge.com/minecraft/mc-mods/shutup-experimental-settings
            "shutupmodelloader-",           //https://www.curseforge.com/minecraft/mc-mods/shut-up-model-loader
            "signtools-",                   //https://www.curseforge.com/minecraft/bukkit-plugins/signtools
            "simple-rpc-",                  //https://www.curseforge.com/minecraft/mc-mods/simple-discord-rpc
            "simpleautorun-",               //Gone? Reduces to atoms?
            "simplefog-",                   //https://www.curseforge.com/minecraft/mc-mods/simplefog
            "skinlayers3d-",                //https://www.curseforge.com/minecraft/mc-mods/skin-layers-3d
            "smartcullplus-",               //https://www.curseforge.com/minecraft/mc-mods/smartcullplus
            "smartcursor-",                 //https://www.curseforge.com/minecraft/mc-mods/smartcursor
            "smarthud-",                    //https://www.curseforge.com/minecraft/mc-mods/smart-hud
            "smoke-suppression-",           //https://www.curseforge.com/minecraft/mc-mods/smoke-suppression
            "smoothboot-",                  //https://www.curseforge.com/minecraft/mc-mods/smoothboot
            "smoothcameramovement-",        //https://www.curseforge.com/minecraft/mc-mods/smooth-camera-movement
            "smoothfocus-",                 //https://www.curseforge.com/minecraft/mc-mods/smoothfocus
            "smoothswapping-",              //https://www.curseforge.com/minecraft/mc-mods/smooth-swapping
            "sodium-fabric-",               //https://www.curseforge.com/minecraft/mc-mods/sodium
            "sodium-shader-support-",       //https://modrinth.com/mod/sodium-shader-support/
            "sodiumcoreshadersupport-",     //https://www.curseforge.com/minecraft/mc-mods/sodium-core-shader-support
            "sodiumdynamiclights-",         //https://www.curseforge.com/minecraft/mc-mods/dynamiclights-reforged
            "sodiumextras-",                //https://www.curseforge.com/minecraft/mc-mods/magnesium-extras
            "sodiumleafculling-",           //https://www.curseforge.com/minecraft/mc-mods/sodium-leaf-culling
            "sodiumoptionsapi-",            //https://www.curseforge.com/minecraft/mc-mods/sodium-options-api
            "sodiumoptionsmodcompat-",      //https://www.curseforge.com/minecraft/mc-mods/sodium-embeddium-options-mod-compat
            "sounddeviceoptions-",          //https://www.curseforge.com/minecraft/mc-mods/more-sound-config
            "soundreloader-",               //https://www.curseforge.com/minecraft/mc-mods/sound-reloader
            "sounds-",                      //https://www.curseforge.com/minecraft/mc-mods/sound
            "spiffyxgnetum-",               //https://www.curseforge.com/minecraft/mc-mods/spiffyhud-x-gnetum
            "spoticraft-",                  //https://www.curseforge.com/minecraft/mc-mods/spoticraft-inactive and https://www.curseforge.com/minecraft/mc-mods/spoticraft-2
            "status-effect-bars-",          //https://www.curseforge.com/minecraft/mc-mods/status-effect-bars
            "stop_rendering-",              //https://www.curseforge.com/minecraft/mc-mods/stoprendering
            "superior-ambience-",           //https://www.curseforge.com/minecraft/mc-mods/superior-ambience
            "tacz_optimization-",           //https://www.curseforge.com/minecraft/mc-mods/tacz-optimization
            "talkingheads-",                //https://www.curseforge.com/minecraft/mc-mods/talkingheads
            "tconjei-",                     //https://www.curseforge.com/minecraft/mc-mods/tconjei
            "tconplanner-",                 //https://www.curseforge.com/minecraft/mc-mods/tinkers-planner
            "textrues_embeddium_options-",  //https://www.curseforge.com/minecraft/mc-mods/textrues-embeddium-options
            "threatengl-",                  //https://www.curseforge.com/minecraft/mc-mods/tgl
            "timestamp-chat-",              //https://www.curseforge.com/minecraft/mc-mods/timestamp-chat
            "timestamps-",                  //https://www.curseforge.com/minecraft/mc-mods/timestamps
            "tooltipscroller-",             //https://www.curseforge.com/minecraft/mc-mods/tooltip-scroller
            "torchoptimizer-",              //https://www.curseforge.com/minecraft/mc-mods/torch-optimizer
            "torohealth-",                  //https://www.curseforge.com/minecraft/mc-mods/torohealth-damage-indicators
            "totaldarkness",                //https://www.curseforge.com/minecraft/mc-mods/total-darkness
            "toughnessbar-",                //https://www.curseforge.com/minecraft/mc-mods/armor-toughness-bar
            "translucent-window-",          //https://www.curseforge.com/minecraft/mc-mods/translucent-window
            "tridentperf-",                 //https://www.curseforge.com/minecraft/mc-mods/tridentperf-1-0-0
            "tweakeroo-",                   //https://www.curseforge.com/minecraft/mc-mods/tweakeroo-update-port
            "twitchchat-",                  //https://www.curseforge.com/minecraft/mc-mods/twitch-chat-for-streamer
            "vanillin-",                    //https://www.curseforge.com/minecraft/mc-mods/vanillin
            "vanillazoom-",                 //https://www.curseforge.com/minecraft/mc-mods/vanilla-zoom
            "viaforge-",                    //https://www.curseforge.com/minecraft/mc-mods/viaforge
            "wakes-",                       //https://www.curseforge.com/minecraft/mc-mods/wakes
            "watermedia-",                  //https://www.curseforge.com/minecraft/mc-mods/watermedia
            "whats-that-slot-",             //https://www.curseforge.com/minecraft/mc-mods/whats-that-slot
            "wheredididie-",                //https://www.curseforge.com/minecraft/mc-mods/where-did-i-die
            "wisla-",                       //https://www.curseforge.com/minecraft/mc-mods/wisla
            "xenon-",                       //https://www.curseforge.com/minecraft/mc-mods/xenon
            "xanders-sodium-options-",      //https://www.curseforge.com/minecraft/mc-mods/xanders-sodium-options
            "xlifeheartcolors-",            //https://www.curseforge.com/minecraft/mc-mods/x-life-heart-colors
            "yisthereautojump-"             //https://www.curseforge.com/minecraft/mc-mods/y-is-there-autojump-forge
        )
    )

    /**
     * Shipped default for what a new configuration copies out of a modpack. Sorted, so the config file it is
     * written into stays diff-friendly. Every `fallback*` here is what SPC uses when the property is unset or
     * unreadable, which is what keeps a damaged properties file from being fatal.
     */
    val fallbackDirectoriesInclusion = TreeSet(
        listOf(
            "addonpacks",
            "blueprints",
            "config",
            "configs",
            "customnpcs",
            "datapacks",
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
    /** Shipped default for directories never copied, even when an inclusion would otherwise match them. */
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

    /** Shipped default for what is left out of the ZIP — chiefly the files a user must supply themselves. */
    val fallbackZipExclusions = TreeSet(
        listOf(
            "minecraft_server.MINECRAFT_VERSION.jar",
            "server.jar",
            "libraries/net/minecraft/server/MINECRAFT_VERSION/server-MINECRAFT_VERSION.jar"
        )
    )

    /** Files deleted after the loader installer has run — its leftovers, which no server needs. */
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

    /** Files deleted before the installer runs, so a stale artifact cannot be mistaken for a fresh install. */
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

    /** Aikar's recommended JVM flags, offered as one click in the GUI rather than typed out by a user. */
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

    /**
     * Fallback exclusion-filter for clientside-only mod matching.
     */
    val fallbackExclusionFilter = ExclusionFilter.START

    /**
     * Fallback-value for overwriting already existing server packs.
     */
    val fallbackOverwriteEnabled = true

    /**
     * Fallback-value for the exclusion of files from ZIP-archives.
     */
    val fallbackZipFileExclusionEnabled = true

    /**
     * Fallback-value for cleanup-procedures after server pack generation.
     */
    val fallbackServerPackCleanupEnabled = true

    /**
     * Fallback-value for the availability of Minecraft pre-releases and snapshots.
     */
    val fallbackMinecraftPreReleasesAvailabilityEnabled = false

    /**
     * Fallback-value for the automatic exclusion of clientside-only mods.
     */
    val fallbackAutoExcludingModsEnabled = true

    /**
     * Fallback-value for updating existing server packs instead of clean generation.
     */
    val fallbackUpdateServerPack = false

    /**
     * String-list of clientside-only mods to exclude from server packs.
     */
    var clientsideMods = fallbackMods
        private set

    /**
     * String-list of mods to include if present, regardless whether a match was found through
     * [clientsideMods].
     */
    var modsWhitelist = fallbackModsWhitelist
        private set

    /**
     * Regex-list of clientside-only mods to exclude from server packs, derived from
     * [clientsideMods] by wrapping every entry in start-and-anything regex-markers.
     */
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
     * Regex-list of mods to include if present, derived from [modsWhitelist] by wrapping every
     * entry in start-and-anything regex-markers.
     */
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
     * Merges the stored fallback-list of clientside-only mods into [clientsideMods] and writes
     * the merged list back to the store.
     */
    fun loadFallbackModsList() {
        clientsideMods.addAll(
            store.getList(FALLBACK_MODS_LIST_KEY, fallbackMods.joinToString(","))
        )
        store.define(FALLBACK_MODS_LIST_KEY, clientsideMods.joinToString(","))
    }

    /**
     * Merges the stored mod-whitelist into [modsWhitelist] and writes the fallback-whitelist
     * back to the store.
     */
    fun loadFallbackWhitelist() {
        modsWhitelist.addAll(
            store.getList(MODS_WHITELIST_KEY, fallbackModsWhitelist.joinToString(","))
        )
        store.define(MODS_WHITELIST_KEY, fallbackModsWhitelist.joinToString(","))
    }

    /**
     * The clientside-only mod-list matching the active [exclusionFilter]: the regex-variant for
     * [ExclusionFilter.REGEX], the plain list otherwise.
     */
    fun clientSideMods() =
        if (exclusionFilter == ExclusionFilter.REGEX) {
            clientsideModsRegex.toList()
        } else {
            clientsideMods.toList()
        }

    /**
     * The mod-whitelist matching the active [exclusionFilter]: the regex-variant for
     * [ExclusionFilter.REGEX], the plain list otherwise.
     */
    fun whitelistedMods() =
        if (exclusionFilter == ExclusionFilter.REGEX) {
            modsWhitelistRegex.toList()
        } else {
            modsWhitelist.toList()
        }

    /**
     * Directories to include in a server pack; store-entries merge with the fallback-defaults.
     */
    var directoriesToInclude = fallbackDirectoriesInclusion
        get() {
            val entries =
                store.getList(DIRECTORIES_MUST_INCLUDE_KEY, fallbackDirectoriesInclusion.joinToString(","))
            field.addAll(entries)
            return field
        }
        set(value) {
            store.setList(DIRECTORIES_MUST_INCLUDE_KEY, value.toList(), ",")
            field.clear()
            field.addAll(value)
            log.info("Directories which must always be included set to: $value")
        }

    /**
     * Directories to exclude from a server pack; directories present in [directoriesToInclude]
     * always win and are removed from this set.
     */
    var directoriesToExclude = fallbackDirectoriesExclusion
        get() {
            val prop =
                store.getList(DIRECTORIES_SHOULD_EXCLUDE_KEY, fallbackDirectoriesExclusion.joinToString(","))
            val use = TreeSet(prop)
            use.removeIf { entry -> directoriesToInclude.contains(entry) }
            field.clear()
            field.addAll(use)
            return field
        }
        set(value) {
            val use = TreeSet<String>()
            use.addAll(value)
            use.removeIf { entry -> directoriesToInclude.contains(entry) }
            store.setList(DIRECTORIES_SHOULD_EXCLUDE_KEY, use.toList(), ",")
            field.clear()
            field.addAll(use)
            log.info("Directories which must always be excluded set to: $field")
        }

    /**
     * Files to delete after a modloader-server installation; store-entries merge with the
     * fallback-defaults.
     */
    var postInstallCleanupFiles = fallbackPostInstallCleanupFiles
        get() {
            val entries = store.getList(POST_INSTALL_CLEANUP_KEY, fallbackPostInstallCleanupFiles.joinToString(","))
            field.addAll(entries)
            return field
        }
        set(value) {
            store.setList(POST_INSTALL_CLEANUP_KEY, value.toList(), ",")
            field.clear()
            field.addAll(value)
            log.info("Files to cleanup after server installation set to: $value")
        }

    /**
     * Files to delete before a modloader-server installation; store-entries merge with the
     * fallback-defaults.
     */
    var preInstallCleanupFiles = fallbackPreInstallCleanupFiles
        get() {
            val entries = store.getList(PRE_INSTALL_CLEANUP_KEY, fallbackPreInstallCleanupFiles.joinToString(","))
            field.addAll(entries)
            return field
        }
        set(value) {
            store.setList(PRE_INSTALL_CLEANUP_KEY, value.toList(), ",")
            field.clear()
            field.addAll(value)
            log.info("Files to cleanup before server installation set to: $value")
        }

    /**
     * Files to exclude from server pack ZIP-archives; store-entries merge with the
     * fallback-defaults. The placeholders MINECRAFT_VERSION, MODLOADER and MODLOADER_VERSION are
     * replaced during generation.
     */
    var zipArchiveExclusions = fallbackZipExclusions
        get() {
            val entries = store.getList(ZIP_EXCLUSIONS_KEY, fallbackZipExclusions.joinToString(","))
            field.addAll(entries)
            return field
        }
        set(value) {
            store.setList(ZIP_EXCLUSIONS_KEY, value.toList(), ",")
            field.clear()
            field.addAll(value)
            log.info("Files which must be excluded from ZIP-archives set to: $value")
        }

    /**
     * The filter-method with which to determine whether a user-specified clientside-only mod
     * should be excluded from the server pack; invalid or missing values default to
     * [ExclusionFilter.START].
     */
    var exclusionFilter = fallbackExclusionFilter
        get() {
            val prop = store.acquire(AUTO_DISCOVERY_FILTER_KEY, "START")
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
            store.define(AUTO_DISCOVERY_FILTER_KEY, value.toString())
            field = value
            log.info("User specified clientside-only mod exclusion filter set to: $field")
        }

    /**
     * Whether the exclusion of files from the ZIP-archive of the server pack is enabled.
     */
    var isZipFileExclusionEnabled = fallbackZipFileExclusionEnabled
        get() {
            field = store.getBool(ZIP_EXCLUSION_ENABLED_KEY, fallbackZipFileExclusionEnabled)
            return field
        }
        set(value) {
            store.setBool(ZIP_EXCLUSION_ENABLED_KEY, value)
            field = value
            log.info("Zip-file exclusion enabled set to: $field")
        }

    /**
     * Whether auto-excluding of clientside-only mods is enabled. Reading migrates the legacy
     * property-key to the current one, with the legacy value winning.
     */
    var isAutoExcludingModsEnabled = fallbackAutoExcludingModsEnabled
        get() {
            var value = store.getBool(AUTO_DISCOVERY_ENABLED_KEY, fallbackAutoExcludingModsEnabled)
            try {
                val legacyProp = store.properties.getProperty(AUTO_DISCOVERY_ENABLED_LEGACY_KEY)
                if (legacyProp.matches(trueFalseRegex)) {
                    value = java.lang.Boolean.parseBoolean(legacyProp)
                    store.define(AUTO_DISCOVERY_ENABLED_KEY, value.toString())
                    store.properties.remove(AUTO_DISCOVERY_ENABLED_LEGACY_KEY)
                    log.info(
                        "Migrated '$AUTO_DISCOVERY_ENABLED_LEGACY_KEY' to '$AUTO_DISCOVERY_ENABLED_KEY'."
                    )
                }
            } catch (ignored: Exception) {
                // No legacy declaration present, so we can safely ignore any exception.
            }
            field = value
            return field
        }
        set(value) {
            store.setBool(AUTO_DISCOVERY_ENABLED_KEY, value)
            field = value
            log.info("Auto-discovery of clientside-only mods set to: $field")
        }

    /**
     * Whether overwriting of already existing server packs is enabled.
     */
    var isServerPacksOverwriteEnabled = fallbackOverwriteEnabled
        get() {
            field = store.getBool(OVERWRITE_ENABLED_KEY, fallbackOverwriteEnabled)
            return field
        }
        set(value) {
            store.setBool(OVERWRITE_ENABLED_KEY, value)
            field = value
            log.info("Overwriting of already existing server packs set to: $field")
        }

    /**
     * Whether cleanup-procedures after server pack generation are enabled.
     */
    var isServerPackCleanupEnabled = fallbackServerPackCleanupEnabled
        get() {
            field = store.getBool(CLEANUP_ENABLED_KEY, fallbackServerPackCleanupEnabled)
            return field
        }
        set(value) {
            store.setBool(CLEANUP_ENABLED_KEY, value)
            field = value
            log.info("Cleanup of already existing server packs set to: $field")
        }

    /**
     * Whether Minecraft pre-releases and snapshots are available to the user in, for example,
     * the GUI.
     */
    var isMinecraftPreReleasesAvailabilityEnabled = fallbackMinecraftPreReleasesAvailabilityEnabled
        get() {
            field = store.getBool(MINECRAFT_SNAPSHOTS_KEY, fallbackMinecraftPreReleasesAvailabilityEnabled)
            return field
        }
        set(value) {
            store.setBool(MINECRAFT_SNAPSHOTS_KEY, value)
            field = value
            log.info("Minecraft pre-releases and snapshots available set to: $field")
        }

    /**
     * Whether a server pack should be updated instead of cleanly generated.
     */
    var isUpdatingServerPacksEnabled = fallbackUpdateServerPack
        get() {
            field = store.getBool(UPDATE_SERVER_PACK_KEY, fallbackUpdateServerPack)
            return field
        }
        set(value) {
            store.setBool(UPDATE_SERVER_PACK_KEY, value)
            field = value
            log.info("Server pack updating set to: $field")
        }

    /**
     * Aikar's flags, commonly used for Minecraft servers to improve performance in various
     * places.
     */
    var aikarsFlags: String = fallbackAikarsFlags
        get() {
            field = store.acquire(AIKARS_FLAGS_KEY, fallbackAikarsFlags)
            return field
        }
        set(value) {
            store.define(AIKARS_FLAGS_KEY, value)
            field = value
            log.info("Set Aikars flags to: $field.")
        }
}
