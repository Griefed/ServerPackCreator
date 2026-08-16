package de.griefed.serverpackcreator.api.common

import de.griefed.serverpackcreator.api.utilities.common.ListUtilities
import de.griefed.serverpackcreator.api.utilities.common.parallelMap
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap

class ListUtilitiesTest internal constructor() {

    /**
     * How many live threads the default `parallelMap` context has spawned. The dispatcher built by
     * `newSingleThreadContext("parallelMap")` names its threads after the context, so a survivor is a
     * thread that was allocated and never released. Matched with `contains` rather than equality
     * because kotlinx.coroutines' debug mode decorates a *running* thread's name with ` @coroutine#N`.
     *
     * Deliberately a **count**, not the names: every leaked thread carries the identical name, and
     * `List - Set` drops all occurrences of a duplicate, so a name-difference version reports zero
     * leaks whenever the baseline is already non-empty — i.e. it stops guarding exactly when a
     * previous test in the same JVM has already leaked one.
     */
    private fun liveParallelMapThreadCount() =
        Thread.getAllStackTraces().keys.count { it.name.contains("parallelMap") }

    /**
     * `parallelMap`'s defaulted context must not allocate a thread that outlives the call. A context
     * from `newSingleThreadContext` owns a dedicated thread and has to be `close()`d by whoever
     * created it — a defaulted parameter has no owner, so every invocation leaked one permanently.
     */
    @Test
    fun parallelMapDoesNotLeakAThreadPerInvocation() {
        val before = liveParallelMapThreadCount()

        repeat(4) { invocation ->
            val doubled = listOf(1, 2, 3).parallelMap { it * 2 }
            Assertions.assertEquals(listOf(2, 4, 6), doubled, "invocation $invocation")
        }

        val leaked = liveParallelMapThreadCount() - before
        Assertions.assertEquals(
            0,
            leaked,
            "parallelMap leaked $leaked thread(s) still alive after 4 invocations"
        )
    }

    /**
     * A function called `parallelMap` has to actually run its elements in parallel. Pinned by giving
     * every element blocking work and collecting the threads it landed on; a single-threaded context
     * confines all of them to one. Skipped on a single-core machine, where no dispatcher can spread.
     *
     * Threads are identified by [Thread.threadId], never by name: Gradle enables assertions on test
     * tasks, which flips kotlinx.coroutines' `auto` debug mode on, and that appends ` @coroutine#N`
     * to the thread name. Collecting names therefore yields one entry per *coroutine* and passes
     * against a single-threaded context — this test did exactly that before the switch.
     */
    @Test
    fun parallelMapRunsElementsOnMoreThanOneThread() {
        Assumptions.assumeTrue(Runtime.getRuntime().availableProcessors() > 1, "needs >1 core")
        val threads = ConcurrentHashMap.newKeySet<Long>()

        val results = (1..8).toList().parallelMap {
            threads.add(Thread.currentThread().threadId())
            Thread.sleep(50)
            it
        }

        Assertions.assertEquals((1..8).toList(), results)
        Assertions.assertTrue(
            threads.size > 1,
            "every element ran on one thread (ids=$threads); the default context is not parallel"
        )
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun encapsulateListElementsTest() {
        val clientMods: List<String> = listOf(
            "A[mbient]Sounds",
            "Back[Tools",
            "Bett[er[][]Advancement",
            "Bett   erPing",
            "cheri[ ]shed",
            "ClientT&/\$weaks",
            "Control§!%(?=)ling",
            "Defau/()&=?ltOptions",
            "durabi!§/&?lity",
            "DynamicS[]urroundings",
            "itemz\\oom",
            "jei-/($?professions",
            "jeiinteg}][ration",
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
        )
        println(ListUtilities.encapsulateListElements(clientMods))
        Assertions.assertNotNull(ListUtilities.encapsulateListElements(clientMods))
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"A[mbient]Sounds\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"Back[Tools\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"Bett[er[][]Advancement\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"Bett   erPing\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"cheri[ ]shed\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"ClientT&/\$weaks\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"Control§!%(?=)ling\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"Defau/()&=?ltOptions\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"durabi!§/&?lity\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"DynamicS[]urroundings\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"itemz/oom\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"jei-/($?professions\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"jeiinteg}][ration\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"JustEnoughResources\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"MouseTweaks\"")
        )
        Assertions.assertTrue(ListUtilities.encapsulateListElements(clientMods).contains("\"Neat\""))
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"OldJavaWarning\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"PackMenu\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"preciseblockplacing\"")
        )
        Assertions.assertTrue(
            ListUtilities
                .encapsulateListElements(clientMods)
                .contains("\"SimpleDiscordRichPresence\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"SpawnerFix\"")
        )
        Assertions.assertTrue(
            ListUtilities.encapsulateListElements(clientMods).contains("\"WorldNameRandomizer\"")
        )
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun cleanListTest() {
        Assertions.assertEquals(
            1,
            ListUtilities.cleanList(
                mutableListOf(
                    "",
                    " ",
                    "  ",
                    "   ",
                    "    ",
                    "     ",
                    "",
                    "",
                    "",
                    "",
                    " ",
                    "hamlo"
                )
            ).size
        )
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun chunkyTest() {
        val mods: List<String> =
            "Armor Status HUD-,[1.12.2]bspkrscore-,[1.12.2]DamageIndicatorsMod-,3dskinlayers-,Absolutely-Not-A-Zoom-Mod-,AdvancedChat-,AdvancedCompas-,AdvancementPlaques-,Ambience,AmbientEnvironment-,AmbientSounds_,antighost-,anviltooltipmod-,appleskin-,armorchroma-,armorpointspp-,ArmorSoundTweak-,AromaBackup-,authme-,autobackup-,autoreconnect-,auto-reconnect-,axolotl-item-fix-,backtools-,Backups-,bannerunlimited-,Batty's Coordinates PLUS Mod,beenfo-1.19-,BetterAdvancements-,BetterAnimationsCollection-,betterbiomeblend-,BetterDarkMode-,BetterF3-,BetterFoliage-,BetterPingDisplay-,BetterPlacement-,better-recipe-book-,BetterTaskbar-,BetterThirdPerson,BetterTitleScreen-,bhmenu-,BH-Menu-,blur-,Blur-,borderless-mining-,BorderlessWindow-,catalogue-,charmonium-,chat_heads-,cherishedworlds-,ChunkAnimator-,cirback-1.0-,classicbar-,clickadv-,clienttweaks-,ClientTweaks_,combat_music-,configured-,controllable-,Controller Support-,Controlling-,CraftPresence-,CTM-,cullleaves-,cullparticles-,custom-crosshair-mod-,CustomCursorMod-,customdiscordrpc-,CustomMainMenu-,darkness-,dashloader-,defaultoptions-,DefaultOptions_,DefaultSettings-,DeleteWorldsToTrash-,desiredservers-,DetailArmorBar-,Ding-,discordrpc-,DistantHorizons-,drippyloadingscreen-,drippyloadingscreen_,DripSounds-,Durability101-,DurabilityNotifier-,dynamic-fps-,dynamiclights-,dynamic-music-,DynamicSurroundings-,DynamicSurroundingsHuds-,dynmus-,effective-,EffectsLeft-,eggtab-,eguilib-,eiramoticons-,EiraMoticons_,EnchantmentDescriptions-,enchantment-lore-,EnhancedVisuals_,entityculling-,entity-texture-features-,EquipmentCompare-,exhaustedstamina-,extremesoundmuffler-,FabricCustomCursorMod-,fabricemotes-,Fallingleaves-,fancymenu_,fancymenu_video_extension,FancySpawnEggs,FancyVideo-API-,findme-,FirstPersonMod,flickerfix-,fm_audio_extension_,FogTweaker-,ForgeCustomCursorMod-,forgemod_VoxelMap-,FPS-Monitor-,FpsReducer-,FpsReducer2-,freelook-,ftb-backups-,ftbbackups2-,FullscreenWindowed-,galacticraft-rpc-,GameMenuModOption-,gamestagesviewer-,grid-,HealthOverlay-,hiddenrecipebook_,HorseStatsMod-,infinitemusic-,InventoryEssentials_,InventoryHud_[1.17.1].forge-,inventoryprofiles,InventorySpam-,InventoryTweaks-,invtweaks-,ItemBorders-,ItemPhysicLite_,ItemStitchingFix-,itemzoom,itlt-,JBRA-Client-,jeed-,jehc-,jeiintegration_,justenoughbeacons-,JustEnoughCalculation-,justenoughdrags-,JustEnoughEffects-,just-enough-harvestcraft-,JustEnoughProfessions-,JustEnoughResources-,justzoom_,keymap-,keywizard-,konkrete_,konkrete_forge_,lazydfu-,LegendaryTooltips,LegendaryTooltips-,lightfallclient-,LightOverlay-,light-overlay-,LLOverlayReloaded-,loadmyresources_,lock_minecart_view-,lootbeams-,LOTRDRP-,lwl-,magnesium_extras-,maptooltip-,massunbind,mcbindtype-,mcwifipnp-,medievalmusic-,mightyarchitect-,mindful-eating-,minetogether-,MoBends,mobplusplus-,modcredits-,modernworldcreation_,modmenu-,modnametooltip-,modnametooltip_,moreoverlays-,MouseTweaks-,mousewheelie-,movement-vision-,multihotbar-,musicdr-,music-duration-reducer-,MyServerIsCompatible-,Neat-,Neat ,neiRecipeHandlers-,NekosEnchantedBooks-,ngrok-lan-expose-mod-,NoAutoJump-,NoFog-,nopotionshift_,notenoughanimations-,Notes-,NotifMod-,oculus-,OldJavaWarning-,openbackup-,OptiFine,OptiForge,OptiForge-,ornaments-,overloadedarmorbar-,PackMenu-,PackModeMenu-,panorama-,paperdoll-,phosphor-,PickUpNotifier-,Ping-,preciseblockplacing-,PresenceFootsteps-,realm-of-lost-souls-,ReAuth-,rebrand-,replanter-,ResourceLoader-,ResourcePackOrganizer,RPG-HUD-,rubidium-,rubidium_extras-,screenshot-to-clipboard-,ShoulderSurfing-,ShulkerTooltip-,shutupexperimentalsettings-,shutupmodelloader-,signtools-,simpleautorun-,simplebackup-,SimpleBackups-,SimpleDiscordRichPresence-,simple-rpc-,SimpleWorldTimer-,smartcursor-,smoothboot-,smoothfocus-,sounddeviceoptions-,SoundFilters-,soundreloader-,SpawnerFix-,spoticraft-,tconplanner-,textile_backup-,timestamps-,Tips-,TipTheScales-,Toast Control-,ToastControl-,Toast-Control-,tooltipscroller-,torchoptimizer-,torohealth-,totaldarkness,toughnessbar-,TRansliterationLib-,TravelersTitles-,VoidFog-,WindowedFullscreen-,wisla-,WorldNameRandomizer-,xlifeheartcolors-,yisthereautojump-".split(
                ","
            )
        ListUtilities.printListToConsoleChunked(mods, 5, "    ", true)
        ListUtilities.printListToLogChunked(mods, 5, "    ", true)
    }
}