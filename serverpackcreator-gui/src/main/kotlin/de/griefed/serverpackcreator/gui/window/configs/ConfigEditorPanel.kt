package de.griefed.serverpackcreator.gui.window.configs

import Gui
import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.*
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionConfigPanel
import de.griefed.serverpackcreator.api.plugins.swinggui.ServerPackConfigTab
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.components.*
import de.griefed.serverpackcreator.gui.window.components.interactivetable.InteractiveTable
import net.miginfocom.swing.MigLayout
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Image
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.DocumentEvent

class ConfigEditorPanel(
    private val guiProps: GuiProps, private val configsTab: ConfigsTab,
    private val configurationHandler: ConfigurationHandler,
    private val apiProperties: ApiProperties,
    private val versionMeta: VersionMeta,
    private val utilities: Utilities,
    private val serverPackHandler: ServerPackHandler,
    apiPlugins: ApiPlugins,
    showBrowser: ActionListener
) : JPanel(
    MigLayout(
        "left,wrap",
        "[left,::64]5[left]5[left,grow]5[left,::64]5[left,::64]", "30"
    )
), ServerPackConfigTab {
    private val log = cachedLoggerOf(this.javaClass)
    private val whitespace = "^.*,\\s*\\\\*$".toRegex()

    private var lastLoadedConfiguration: PackConfig? = null

    private val pluginPanels: MutableList<ExtensionConfigPanel> = apiPlugins.getConfigPanels(this).toMutableList()

    private val modpackInfo = JLabel(guiProps.infoIcon)
    private val propertiesInfo = JLabel(guiProps.infoIcon)
    private val iconInfo = JLabel(guiProps.infoIcon)
    private val filesInfo = JLabel(guiProps.infoIcon)
    private val suffixInfo = JLabel(guiProps.infoIcon)
    private val minecraftInfo = JLabel(guiProps.infoIcon)
    private val javaVersionInfo = JLabel(guiProps.infoIcon)
    private val modloaderInfo = JLabel(guiProps.infoIcon)
    private val modloaderVersionInfo = JLabel(guiProps.infoIcon)
    private val includeIconInfo = JLabel(guiProps.infoIcon)
    private val includeZIPInfo = JLabel(guiProps.infoIcon)
    private val includePropertiesInfo = JLabel(guiProps.infoIcon)
    private val includeServerInfo = JLabel(guiProps.infoIcon)
    private val iconPreview = JLabel(guiProps.serverIcon)
    private val exclusionsInfo = JLabel(guiProps.infoIcon)
    private val argumentsInfo = JLabel(guiProps.infoIcon)
    private val scriptInfo = JLabel(guiProps.infoIcon)

    private val modpackBrowser = JButton(guiProps.folderIcon)
    private val modpackInspect = JButton(guiProps.inspectIcon)
    private val propertiesBrowser = JButton(guiProps.folderIcon)
    private val propertiesOpen = JButton(guiProps.openIcon)
    private val iconBrowser = JButton(guiProps.folderIcon)
    private val serverPackFilesRevert = JButton(guiProps.revertIcon)
    private val serverPackFilesBrowser = JButton(guiProps.folderIcon)
    private val serverPackFilesReset = JButton(guiProps.resetIcon)

    private val javaVersion = ElementLabel("8", 16)

    private val serverPackSuffix = JTextField("-4,0,0")

    private val includeIcon = JCheckBox("Include Server Icon")
    private val includeZip = JCheckBox("Create ZIP Archive")
    private val includeProperties = JCheckBox("Include Server Properties")
    private val prepareServer = JCheckBox("Prepare Local Server")

    private val noVersions =
        DefaultComboBoxModel(arrayOf(Gui.createserverpack_gui_createserverpack_forge_none.toString()))
    private val legacyFabricVersions = DefaultComboBoxModel(versionMeta.legacyFabric.loaderVersionsArrayDescending())
    private val fabricVersions = DefaultComboBoxModel(versionMeta.fabric.loaderVersionsArrayDescending())
    private val quiltVersions = DefaultComboBoxModel(versionMeta.quilt.loaderVersionsArrayDescending())

    private val propertiesQuickSelect = JComboBox(DefaultComboBoxModel(arrayOf("server.properties")))
    private val iconQuickSelect = JComboBox(DefaultComboBoxModel(arrayOf("server-icon.png")))
    private val minecraftVersions = JComboBox(DefaultComboBoxModel(arrayOf("1.16.5", "1.18.2", "1.12.2")))
    private val modloaders = JComboBox(DefaultComboBoxModel(arrayOf("Forge", "Fabric", "Quilt", "LegacyFabric")))
    private val modloaderVersions = JComboBox(DefaultComboBoxModel(arrayOf("1.2.3", "4.5.6")))

    private val exclusionsRevert = JButton(guiProps.revertIcon)
    private val exclusionsBrowser = JButton(guiProps.folderIcon)
    private val exclusionsReset = JButton(guiProps.resetIcon)
    private val scriptKVPairsRevert = JButton(guiProps.revertIcon)
    private val scriptKVPairsBrowser = JButton(guiProps.folderIcon)
    private val scriptKVPairsReset = JButton(guiProps.resetIcon)
    private val aikarsFlags = JButton()

    private val modpackDirectory = FileTextField("C:\\Minecraft\\Game\\Instances\\Survive Create Prosper 4")
    private val propertiesFile = FileTextField("C:/Minecraft/ServerPackCreator/server_files/scp3.properties")
    private val iconFile = FileTextField("C:/Minecraft/ServerPackCreator/server_files/server-icon.png")

    private val serverPackFiles = ScrollTextArea("config, customnpcs, mods, scripts")
    private val exclusions = ScrollTextArea(
        "^Armor Status HUD-.*\$, ^[1.12.2]bspkrscore-.*\$, ^[1.12.2]DamageIndicatorsMod-.*\$, ^3dskinlayers-.*\$, ^Absolutely-Not-A-Zoom-Mod-.*\$, ^AdvancedChat-.*\$, ^AdvancedCompas-.*\$, ^AdvancementPlaques-.*\$, ^Ambience.*\$, ^AmbientEnvironment-.*\$, ^AmbientSounds_.*\$, ^antighost-.*\$, ^anviltooltipmod-.*\$, ^appleskin-.*\$, ^armorchroma-.*\$, ^armorpointspp-.*\$, ^ArmorSoundTweak-.*\$, ^AromaBackup-.*\$, ^authme-.*\$, ^autobackup-.*\$, ^autoreconnect-.*\$, ^auto-reconnect-.*\$, ^axolotl-item-fix-.*\$, ^backtools-.*\$, ^Backups-.*\$, ^bannerunlimited-.*\$, ^Batty's Coordinates PLUS Mod.*\$, ^beenfo-1.19-.*\$, ^BetterAdvancements-.*\$, ^BetterAnimationsCollection-.*\$, ^betterbiomeblend-.*\$, ^BetterDarkMode-.*\$, ^BetterF3-.*\$, ^BetterFoliage-.*\$, ^BetterPingDisplay-.*\$, ^BetterPlacement-.*\$, ^better-recipe-book-.*\$, ^BetterTaskbar-.*\$, ^BetterThirdPerson.*\$, ^BetterTitleScreen-.*\$, ^bhmenu-.*\$, ^BH-Menu-.*\$, ^blur-.*\$, ^borderless-mining-.*\$, ^BorderlessWindow-.*\$, ^catalogue-.*\$, ^charmonium-.*\$, ^chat_heads-.*\$, ^cherishedworlds-.*\$, ^ChunkAnimator-.*\$, ^cirback-1.0-.*\$, ^classicbar-.*\$, ^clickadv-.*\$, ^clienttweaks-.*\$, ^ClientTweaks_.*\$, ^combat_music-.*\$, ^configured-.*\$, ^controllable-.*\$, ^Controller Support-.*\$, ^Controlling-.*\$, ^CraftPresence-.*\$, ^CTM-.*\$, ^cullleaves-.*\$, ^cullparticles-.*\$, ^custom-crosshair-mod-.*\$, ^CustomCursorMod-.*\$, ^customdiscordrpc-.*\$, ^CustomMainMenu-.*\$, ^darkness-.*\$, ^dashloader-.*\$, ^defaultoptions-.*\$, ^DefaultOptions_.*\$, ^DefaultSettings-.*\$, ^DeleteWorldsToTrash-.*\$, ^desiredservers-.*\$, ^DetailArmorBar-.*\$, ^Ding-.*\$, ^discordrpc-.*\$, ^DistantHorizons-.*\$, ^drippyloadingscreen-.*\$, ^drippyloadingscreen_.*\$, ^DripSounds-.*\$, ^Durability101-.*\$, ^DurabilityNotifier-.*\$, ^dynamic-fps-.*\$, ^dynamiclights-.*\$, ^dynamic-music-.*\$, ^DynamicSurroundings-.*\$, ^DynamicSurroundingsHuds-.*\$, ^dynmus-.*\$, ^effective-.*\$, ^EffectsLeft-.*\$, ^eggtab-.*\$, ^eguilib-.*\$, ^eiramoticons-.*\$, ^EiraMoticons_.*\$, ^EnchantmentDescriptions-.*\$, ^enchantment-lore-.*\$, ^EnhancedVisuals_.*\$, ^entityculling-.*\$, ^entity-texture-features-.*\$, ^EquipmentCompare-.*\$, ^exhaustedstamina-.*\$, ^extremesoundmuffler-.*\$, ^FabricCustomCursorMod-.*\$, ^fabricemotes-.*\$, ^Fallingleaves-.*\$, ^fancymenu_.*\$, ^fancymenu_video_extension.*\$, ^FancySpawnEggs.*\$, ^FancyVideo-API-.*\$, ^findme-.*\$, ^FirstPersonMod.*\$, ^flickerfix-.*\$, ^fm_audio_extension_.*\$, ^FogTweaker-.*\$, ^ForgeCustomCursorMod-.*\$, ^forgemod_VoxelMap-.*\$, ^FPS-Monitor-.*\$, ^FpsReducer-.*\$, ^FpsReducer2-.*\$, ^freelook-.*\$, ^ftb-backups-.*\$, ^ftbbackups2-.*\$, ^FullscreenWindowed-.*\$, ^galacticraft-rpc-.*\$, ^GameMenuModOption-.*\$, ^gamestagesviewer-.*\$, ^grid-.*\$, ^HealthOverlay-.*\$, ^hiddenrecipebook_.*\$, ^HorseStatsMod-.*\$, ^infinitemusic-.*\$, ^InventoryEssentials_.*\$, ^InventoryHud_[1.17.1].forge-.*\$, ^inventoryprofiles.*\$, ^InventorySpam-.*\$, ^InventoryTweaks-.*\$, ^invtweaks-.*\$, ^ItemBorders-.*\$, ^ItemPhysicLite_.*\$, ^ItemStitchingFix-.*\$, ^itemzoom.*\$, ^itlt-.*\$, ^JBRA-Client-.*\$, ^jeed-.*\$, ^jehc-.*\$, ^jeiintegration_.*\$, ^justenoughbeacons-.*\$, ^JustEnoughCalculation-.*\$, ^justenoughdrags-.*\$, ^JustEnoughEffects-.*\$, ^just-enough-harvestcraft-.*\$, ^JustEnoughProfessions-.*\$, ^JustEnoughResources-.*\$, ^justzoom_.*\$, ^keymap-.*\$, ^keywizard-.*\$, ^konkrete_.*\$, ^konkrete_forge_.*\$, ^lazydfu-.*\$, ^LegendaryTooltips.*\$, ^LegendaryTooltips-.*\$, ^lightfallclient-.*\$, ^LightOverlay-.*\$, ^light-overlay-.*\$, ^LLOverlayReloaded-.*\$, ^loadmyresources_.*\$, ^lock_minecart_view-.*\$, ^lootbeams-.*\$, ^LOTRDRP-.*\$, ^lwl-.*\$, ^magnesium_extras-.*\$, ^maptooltip-.*\$, ^massunbind.*\$, ^mcbindtype-.*\$, ^mcwifipnp-.*\$, ^medievalmusic-.*\$, ^mightyarchitect-.*\$, ^mindful-eating-.*\$, ^minetogether-.*\$, ^MoBends.*\$, ^mobplusplus-.*\$, ^modcredits-.*\$, ^modernworldcreation_.*\$, ^modmenu-.*\$, ^modnametooltip-.*\$, ^modnametooltip_.*\$, ^moreoverlays-.*\$, ^MouseTweaks-.*\$, ^mousewheelie-.*\$, ^movement-vision-.*\$, ^multihotbar-.*\$, ^musicdr-.*\$, ^music-duration-reducer-.*\$, ^MyServerIsCompatible-.*\$, ^Neat-.*\$, ^Neat .*\$, ^neiRecipeHandlers-.*\$, ^NekosEnchantedBooks-.*\$, ^ngrok-lan-expose-mod-.*\$, ^NoAutoJump-.*\$, ^NoFog-.*\$, ^nopotionshift_.*\$, ^notenoughanimations-.*\$, ^Notes-.*\$, ^NotifMod-.*\$, ^oculus-.*\$, ^OldJavaWarning-.*\$, ^openbackup-.*\$, ^OptiFine.*\$, ^OptiForge.*\$, ^OptiForge-.*\$, ^ornaments-.*\$, ^overloadedarmorbar-.*\$, ^PackMenu-.*\$, ^PackModeMenu-.*\$, ^panorama-.*\$, ^paperdoll-.*\$, ^phosphor-.*\$, ^PickUpNotifier-.*\$, ^Ping-.*\$, ^preciseblockplacing-.*\$, ^PresenceFootsteps-.*\$, ^realm-of-lost-souls-.*\$, ^ReAuth-.*\$, ^rebrand-.*\$, ^replanter-.*\$, ^ResourceLoader-.*\$, ^ResourcePackOrganizer.*\$, ^RPG-HUD-.*\$, ^rubidium-.*\$, ^rubidium_extras-.*\$, ^screenshot-to-clipboard-.*\$, ^ShoulderSurfing-.*\$, ^ShulkerTooltip-.*\$, ^shutupexperimentalsettings-.*\$, ^shutupmodelloader-.*\$, ^signtools-.*\$, ^simpleautorun-.*\$, ^simplebackup-.*\$, ^SimpleBackups-.*\$, ^SimpleDiscordRichPresence-.*\$, ^simple-rpc-.*\$, ^SimpleWorldTimer-.*\$, ^smartcursor-.*\$, ^smoothboot-.*\$, ^smoothfocus-.*\$, ^sounddeviceoptions-.*\$, ^SoundFilters-.*\$, ^soundreloader-.*\$, ^SpawnerFix-.*\$, ^spoticraft-.*\$, ^tconplanner-.*\$, ^textile_backup-.*\$, ^timestamps-.*\$, ^Tips-.*\$, ^TipTheScales-.*\$, ^Toast Control-.*\$, ^ToastControl-.*\$, ^Toast-Control-.*\$, ^tooltipscroller-.*\$, ^torchoptimizer-.*\$, ^torohealth-.*\$, ^totaldarkness.*\$, ^toughnessbar-.*\$, ^TRansliterationLib-.*\$, ^TravelersTitles-.*\$, ^VoidFog-.*\$, ^WindowedFullscreen-.*\$, ^wisla-.*\$, ^WorldNameRandomizer-.*\$, ^xlifeheartcolors-.*\$, ^yisthereautojump-.*\$"
    )
    private val startArgs =
        ScrollTextArea("-Xms8G -Xmx8G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true")

    private val scriptKVPairs = InteractiveTable()

    private val advancedSettingsPanel = AdvancedSettingsPanel(
        exclusionsInfo,
        argumentsInfo,
        scriptInfo,
        exclusions,
        exclusionsRevert,
        exclusionsBrowser,
        exclusionsReset,
        startArgs,
        aikarsFlags,
        scriptKVPairs,
        scriptKVPairsRevert,
        scriptKVPairsBrowser,
        scriptKVPairsReset
    )
    private val pluginsSettingsPanel = PluginsSettingsPanel(pluginPanels)
    private val collapsibleAdvanced = CollapsiblePanel("Advanced Settings", advancedSettingsPanel)
    private val collapsiblePlugins = CollapsiblePanel("Plugins", pluginsSettingsPanel)
    private val modpackChanges = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            title.titleLabel.text = modpackDirectory.file.name
            validateModpackDir()
        }
    }
    private val suffixChanges = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            validateSuffix()
        }
    }
    private val exclusionChanges = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            validateExclusions()
        }
    }
    private val serverPackFilesChanges = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            validateServerPackFiles()
        }
    }
    private val iconChanges = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            validateServerIcon()
        }
    }
    private val propertyChanges = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            validateServerProperties()
        }
    }

    val title: Title

    init {
        if (apiProperties.isMinecraftPreReleasesAvailabilityEnabled) {
            minecraftVersions.setModel(DefaultComboBoxModel(versionMeta.minecraft.allVersionsArrayDescending()))
        } else {
            minecraftVersions.setModel(DefaultComboBoxModel(versionMeta.minecraft.releaseVersionsArrayDescending()))
        }
        if (minecraftVersions.selectedItem == null) {
            minecraftVersions.selectedIndex = 0
        }

        modloaders.model = DefaultComboBoxModel(apiProperties.supportedModloaders)
        if (modloaders.selectedItem == null) {
            modloaders.selectedIndex = 0
        }

        modpackBrowser.addActionListener(showBrowser)
        propertiesBrowser.addActionListener(showBrowser)
        iconBrowser.addActionListener(showBrowser)
        serverPackFilesBrowser.addActionListener(showBrowser)
        exclusionsBrowser.addActionListener(showBrowser)
        scriptKVPairsBrowser.addActionListener(showBrowser)

        modpackInspect.addActionListener { updateGuiFromSelectedModpack() }
        propertiesOpen.addActionListener { openServerProperties() }

        exclusionsRevert.addActionListener { revertExclusions() }
        serverPackFilesRevert.addActionListener { revertServerPackFiles() }

        exclusionsReset.addActionListener { setClientSideMods(apiProperties.clientSideMods()) }
        serverPackFilesReset.addActionListener { setCopyDirectories(apiProperties.directoriesToInclude.toMutableList()) }

        modpackDirectory.document.addDocumentListener(modpackChanges)
        serverPackSuffix.document.addDocumentListener(suffixChanges)
        exclusions.addDocumentListener(exclusionChanges)
        serverPackFiles.addDocumentListener(serverPackFilesChanges)
        iconFile.document.addDocumentListener(iconChanges)
        propertiesFile.document.addDocumentListener(propertyChanges)

        minecraftVersions.addActionListener { updateMinecraftValues() }
        modloaders.addActionListener { setModloader(modloaders.selectedItem!!.toString()) }

        prepareServer.addActionListener { checkServer() }
        includeIcon.addActionListener { validateServerIcon() }
        includeProperties.addActionListener { validateServerProperties() }

        aikarsFlags.addActionListener { setAikarsFlagsAsJavaArguments() }

        // Modpack directory
        add(modpackInfo, "cell 0 0,grow")
        add(ElementLabel("Modpack Directory"), "cell 1 0,grow")
        add(modpackDirectory, "cell 2 0,grow")
        add(modpackBrowser, "cell 3 0, h 30!,w 30!")
        add(modpackInspect, "cell 4 0")

        // Server Properties
        add(propertiesInfo, "cell 0 1,grow")
        add(ElementLabel("Server Properties"), "cell 1 1,grow")
        add(propertiesFile, "cell 2 1, split 3,grow")
        add(ElementLabel("Quick select:"), "cell 2 1")
        add(propertiesQuickSelect, "cell 2 1,w 200!")
        add(propertiesBrowser, "cell 3 1")
        add(propertiesOpen, "cell 4 1")

        // Server Icon
        add(iconInfo, "cell 0 2,grow")
        add(ElementLabel("Server Icon"), "cell 1 2,grow")
        add(iconFile, "cell 2 2, split 2,grow")
        add(ElementLabel("Quick select:"), "cell 2 2")
        add(iconQuickSelect, "cell 2 2,w 200!")
        add(iconBrowser, "cell 3 2")
        add(iconPreview, "cell 4 2")

        // Server Files
        add(filesInfo, "cell 0 3 1 3,grow")
        add(ElementLabel("Server-files"), "cell 1 3 1 3,grow")
        add(serverPackFiles, "cell 2 3 1 3,grow,w 10:500:,h 100!")
        add(serverPackFilesRevert, "cell 3 3 2 1, h 30!, aligny center, alignx center,growx")
        add(serverPackFilesBrowser, "cell 3 4 2 1, h 30!, aligny center, alignx center,growx")
        add(serverPackFilesReset, "cell 3 5 2 1, h 30!, aligny top, alignx center,growx")

        // Server Pack Suffix
        add(suffixInfo, "cell 0 6,grow")
        add(ElementLabel("Server Pack Suffix"), "cell 1 6,grow")
        add(serverPackSuffix, "cell 2 6,grow")

        // Minecraft Version
        add(minecraftInfo, "cell 0 7,grow")
        add(ElementLabel("Minecraft Version"), "cell 1 7,grow")
        add(minecraftVersions, "cell 2 7,w 200!")
        // Java Version Of Minecraft Version
        add(javaVersionInfo, "cell 2 7, w 40!, gapleft 40")
        add(ElementLabel("Java", 16), "cell 2 7")
        add(javaVersion, "cell 2 7, w 40!")

        // Modloader
        add(modloaderInfo, "cell 0 8,grow")
        add(ElementLabel("Modloader"), "cell 1 8,grow")
        add(modloaders, "cell 2 8,w 200!")
        // Include Server Icon
        add(includeIconInfo, "cell 2 8, w 40!, gapleft 40,grow")
        add(includeIcon, "cell 2 8, w 200!")
        //Create ZIP Archive
        add(includeZIPInfo, "cell 2 8, w 40!,grow")
        add(includeZip, "cell 2 8, w 200!")

        //Modloader Version
        add(modloaderVersionInfo, "cell 0 9,grow")
        add(ElementLabel("Modloader Version"), "cell 1 9,grow")
        add(modloaderVersions, "cell 2 9,w 200!")
        //Include Server Properties
        add(includePropertiesInfo, "cell 2 9, w 40!, gapleft 40,grow")
        add(includeProperties, "cell 2 9, w 200!")
        //Install Local Server
        add(includeServerInfo, "cell 2 9, w 40!,grow")
        add(prepareServer, "cell 2 9, w 200!")

        aikarsFlags.icon = CompoundIcon(
            arrayOf(
                TextIcon(aikarsFlags, "Aikars"),
                TextIcon(aikarsFlags, "Flats")
            ),
            5,
            CompoundIcon.Axis.Y_AXIS
        )

        // Advanced Settings
        add(collapsibleAdvanced, "cell 0 10 5,grow")

        // Plugins
        add(collapsiblePlugins, "cell 0 11 5,grow")

        title = Title()

    }

    /**
     * TODO docs
     */
    inner class Title : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {

        val titleLabel = JLabel(modpackDirectory.file.name)
        val closeButton = JButton(guiProps.closeIcon)

        init {
            isOpaque = false
            titleLabel.border = BorderFactory.createEmptyBorder(0, 0, 0, 5)
            add(titleLabel)
            closeButton.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    val currentTab = configsTab.tabs.selectedIndex
                    configsTab.tabs.remove(this@ConfigEditorPanel)

                    if (currentTab - 1 > 0) {
                        configsTab.tabs.selectedIndex = currentTab - 1
                    }
                }
            })
            closeButton.isVisible = false
            add(closeButton)
        }
    }

    override fun setClientSideMods(entries: MutableList<String>) {
        exclusions.text = utilities.stringUtilities.buildString(entries)
    }

    override fun setCopyDirectories(entries: MutableList<String>) {
        serverPackFiles.text = utilities.stringUtilities.buildString(entries.toString())
    }

    override fun setIconInclusionTicked(ticked: Boolean) {
        includeIcon.isSelected = ticked
    }

    override fun setJavaArguments(javaArguments: String) {
        startArgs.text = javaArguments
    }

    override fun setMinecraftVersion(version: String) {
        for (i in 0 until minecraftVersions.model.size) {
            if (minecraftVersions.model.getElementAt(i) == version) {
                minecraftVersions.selectedIndex = i
                break
            }
        }
    }

    override fun setModloader(modloader: String) {
        when (modloader) {
            "Fabric" -> modloaders.setSelectedIndex(0)
            "Forge" -> modloaders.setSelectedIndex(1)
            "Quilt" -> modloaders.setSelectedIndex(2)
            "LegacyFabric" -> modloaders.setSelectedIndex(3)
        }
        setModloaderVersionsModel()
    }

    override fun setModloaderVersion(version: String) {
        for (i in 0 until modloaderVersions.model.size) {
            if (modloaderVersions.model.getElementAt(i) == version) {
                modloaderVersions.selectedIndex = i
                break
            }
        }
    }

    override fun setModpackDirectory(directory: String) {
        modpackDirectory.text = directory
    }

    override fun setPropertiesInclusionTicked(ticked: Boolean) {
        includeProperties.isSelected = ticked
    }

    override fun setScriptVariables(variables: HashMap<String, String>) {
        scriptKVPairs.loadData(variables)
    }

    override fun setServerIconPath(path: String) {
        iconFile.text = path
    }

    override fun setServerInstallationTicked(ticked: Boolean) {
        prepareServer.isSelected = ticked
    }

    override fun setServerPackSuffix(suffix: String) {
        serverPackSuffix.text = utilities.stringUtilities.pathSecureText(suffix)
    }

    override fun setServerPropertiesPath(path: String) {
        propertiesFile.text = path
    }

    override fun setZipArchiveCreationTicked(ticked: Boolean) {
        includeZip.isSelected = ticked
    }

    override fun getClientSideMods(): String {
        return exclusions.text.replace(", ", ",")
    }

    override fun getClientSideModsList(): MutableList<String> {
        return utilities.listUtilities.cleanList(
            getClientSideMods().split(",")
                .dropLastWhile { it.isEmpty() }
                .toMutableList()
        )
    }

    override fun getCopyDirectories(): String {
        return serverPackFiles.text.replace(", ", ",")
    }

    override fun getCopyDirectoriesList(): MutableList<String> {
        return utilities.listUtilities.cleanList(
            getCopyDirectories().split(",")
                .dropLastWhile { it.isEmpty() }
                .toMutableList()
        )
    }

    override fun getCurrentConfiguration(): PackConfig {
        return PackConfig(
            getClientSideModsList(),
            getCopyDirectoriesList(),
            getModpackDirectory(),
            getMinecraftVersion(),
            getModloader(),
            getModloaderVersion(),
            getJavaArguments(),
            getServerPackSuffix(),
            getServerIconPath(),
            getServerPropertiesPath(),
            isServerInstallationTicked(),
            isServerIconInclusionTicked(),
            isServerPropertiesInclusionTicked(),
            isZipArchiveCreationTicked(),
            getScriptSettings(),
            getConfigPanelConfigs()
        )
    }

    // TODO rename to getStartArguments ?
    override fun getJavaArguments(): String {
        return startArgs.text
    }

    override fun getMinecraftVersion(): String {
        return minecraftVersions.selectedItem!!.toString()
    }

    override fun getModloader(): String {
        return modloaders.selectedItem!!.toString()
    }

    override fun getModloaderVersion(): String {
        return modloaderVersions.selectedItem!!.toString()
    }

    override fun getModpackDirectory(): String {
        return modpackDirectory.text
    }

    override fun getScriptSettings(): HashMap<String, String> {
        return scriptKVPairs.getData()
    }

    override fun getServerIconPath(): String {
        return iconFile.text
    }

    override fun getServerPackSuffix(): String {
        return utilities.stringUtilities.pathSecureText(serverPackSuffix.text)
    }

    override fun getServerPropertiesPath(): String {
        return propertiesFile.text
    }

    override fun isMinecraftServerAvailable(): Boolean {
        return versionMeta.minecraft.isServerAvailable(minecraftVersions.selectedItem!!.toString())
    }

    override fun isServerInstallationTicked(): Boolean {
        return prepareServer.isSelected
    }

    override fun isServerIconInclusionTicked(): Boolean {
        return includeIcon.isSelected
    }

    override fun isServerPropertiesInclusionTicked(): Boolean {
        return includeProperties.isSelected
    }

    override fun isZipArchiveCreationTicked(): Boolean {
        return includeZip.isSelected
    }

    override fun clearScriptVariables() {
        scriptKVPairs.clearData()
    }

    override fun setAikarsFlagsAsJavaArguments() {
        if (getJavaArguments().isNotEmpty()) {
            when (JOptionPane.showConfirmDialog(
                this,
                Gui.createserverpack_gui_createserverpack_javaargs_confirm_message.toString(),
                Gui.createserverpack_gui_createserverpack_javaargs_confirm_title.toString(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
            )) {
                0 -> setJavaArguments(apiProperties.aikarsFlags)
                1 -> {}
                else -> {}
            }
        } else {
            setJavaArguments(apiProperties.aikarsFlags)
        }
    }

    /**
     * TODO docs
     */
    override fun validateInputFields() {
        validateModpackDir()
        validateSuffix()
        validateExclusions()
        validateServerPackFiles()
        validateServerIcon()
        validateServerProperties()
    }

    /**
     * TODO docs
     */
    override fun acquireRequiredJavaVersion(): String {
        return if (versionMeta.minecraft.getServer(getMinecraftVersion()).isPresent && versionMeta.minecraft.getServer(
                getMinecraftVersion()
            ).get().javaVersion().isPresent
        ) {
            versionMeta.minecraft.getServer(getMinecraftVersion()).get().javaVersion().get().toString()
        } else {
            "?"
        }
    }

    /**
     * Set the modloader version combobox model depending on the currently selected modloader, with
     * the specified Minecraft version.
     *
     * @param minecraftVersion The Minecraft version to work with in the GUI update.
     * @author Griefed
     */
    private fun setModloaderVersionsModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) =
        when (modloaders.selectedIndex) {
            0 -> updateFabricModel(minecraftVersion)
            1 -> updateForgeModel(minecraftVersion)
            2 -> updateQuiltModel(minecraftVersion)
            3 -> updateLegacyFabricModel(minecraftVersion)
            else -> {
                log.error("Invalid modloader selected.")
            }
        }

    /**
     * TODO docs
     */
    private fun updateFabricModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        if (versionMeta.fabric.isMinecraftSupported(minecraftVersion)) {
            modloaderVersions.setModel(fabricVersions)
        } else {
            modloaderVersions.setModel(noVersions)
        }
    }

    /**
     * TODO docs
     */
    private fun updateForgeModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        if (versionMeta.forge.supportedForgeVersionsDescendingArray(minecraftVersion).isPresent) {
            modloaderVersions.setModel(
                DefaultComboBoxModel(
                    versionMeta.forge.supportedForgeVersionsDescendingArray(minecraftVersion).get()
                )
            )
        } else {
            modloaderVersions.setModel(noVersions)
        }
    }

    /**
     * TODO docs
     */
    private fun updateQuiltModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        if (versionMeta.fabric.isMinecraftSupported(minecraftVersion)) {
            modloaderVersions.setModel(quiltVersions)
        } else {
            modloaderVersions.setModel(noVersions)
        }
    }

    /**
     * TODO docs
     */
    private fun updateLegacyFabricModel(minecraftVersion: String = minecraftVersions.selectedItem!!.toString()) {
        if (versionMeta.legacyFabric.isMinecraftSupported(minecraftVersion)) {
            modloaderVersions.setModel(legacyFabricVersions)
        } else {
            modloaderVersions.setModel(noVersions)
        }
    }

    /**
     * Validate the input field for modpack directory.
     *
     * @author Griefed
     */
    private fun validateModpackDir(): List<String> {
        val errors: MutableList<String> = ArrayList(20)
        SwingUtilities.invokeLater {
            if (configurationHandler.checkModpackDir(getModpackDirectory(), errors, false)) {
                modpackInfo.icon = guiProps.infoIcon
                modpackInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelmodpackdir_tip.toString()
            } else {
                modpackInfo.icon = guiProps.errorIcon
                modpackInfo.toolTipText = "<html>${errors.joinToString("<br>")}</html>"
            }
        }
        for (error in errors) {
            log.error(error)
        }
        return errors.toList()
    }

    /**
     * Validate the input field for server pack suffix.
     *
     * @author Griefed
     */
    private fun validateSuffix(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        SwingUtilities.invokeLater {
            if (utilities.stringUtilities.checkForIllegalCharacters(serverPackSuffix.text)) {
                suffixInfo.icon = guiProps.infoIcon
                suffixInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelsuffix_tip.toString()
            } else {
                errors.add(Gui.configuration_log_error_serverpack_suffix.toString())
                suffixInfo.icon = guiProps.errorIcon
                suffixInfo.toolTipText = "<html>${errors.joinToString("<br>")}</html>"
            }
        }
        for (error in errors) {
            log.error(error)
        }
        return errors.toList()
    }

    /**
     * Validate the input field for client mods.
     *
     * @author Griefed
     */
    private fun validateExclusions(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        SwingUtilities.invokeLater {

            if (!getClientSideMods().matches(whitespace)) {
                exclusionsInfo.icon = guiProps.infoIcon
                exclusionsInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelclientmods_tip.toString()
            } else {
                errors.add(Gui.configuration_log_error_formatting.toString())
                exclusionsInfo.icon = guiProps.errorIcon
                exclusionsInfo.toolTipText = "<html>${errors.joinToString("<br>")}</html>"
            }
        }
        for (error in errors) {
            log.error(error)
        }
        return errors.toList()
    }

    /**
     * Validate the input field for copy directories.
     *
     * @author Griefed
     */
    private fun validateServerPackFiles(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        SwingUtilities.invokeLater {
            configurationHandler.checkCopyDirs(
                getCopyDirectoriesList(), getModpackDirectory(), errors, false
            )
            if (getCopyDirectories().matches(whitespace)) {
                errors.add(Gui.configuration_log_error_formatting.toString())
            }
            if (errors.isNotEmpty()) {
                filesInfo.icon = guiProps.errorIcon
                filesInfo.toolTipText = "<html>${errors.joinToString("<br>")}</html>"
            } else {
                filesInfo.icon = guiProps.infoIcon
                filesInfo.toolTipText = Gui.createserverpack_gui_createserverpack_labelcopydirs_tip.toString()
            }
        }
        for (error in errors) {
            log.error(error)
        }
        return errors.toList()
    }

    /**
     * Validate the input field for server icon.
     *
     * @author Griefed
     */
    private fun validateServerIcon(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        SwingUtilities.invokeLater {
            if (getServerIconPath().isNotEmpty()) {
                if (configurationHandler.checkIconAndProperties(getServerIconPath())) {
                    iconInfo.icon = guiProps.infoIcon
                    iconInfo.toolTipText = Gui.createserverpack_gui_createserverpack_textfield_iconpath.toString()
                    try {
                        iconPreview.icon = ImageIcon(
                            ImageIO.read(
                                File(getServerIconPath())
                            ).getScaledInstance(32, 32, Image.SCALE_SMOOTH)
                        )
                    } catch (ex: IOException) {
                        log.error("Error generating server icon preview.", ex)
                        errors.add(Gui.configuration_log_error_servericon_error.toString())
                    }
                } else {
                    errors.add(Gui.createserverpack_gui_createserverpack_servericon_preview_none.toString())
                    iconInfo.icon = guiProps.errorIcon
                    iconPreview.icon = guiProps.iconError
                    iconInfo.toolTipText = "<html>${errors.joinToString("<br>")}</html>"
                }
            } else {
                try {
                    iconInfo.toolTipText = Gui.createserverpack_gui_createserverpack_servericon_preview.toString()
                    iconPreview.icon = ImageIcon(
                        ImageIO.read(apiProperties.defaultServerIcon)
                            .getScaledInstance(32, 32, Image.SCALE_SMOOTH)
                    )
                } catch (ex: IOException) {
                    log.error("Error generating server icon preview.", ex)
                    errors.add(Gui.configuration_log_error_servericon_error.toString())
                }
            }
        }
        for (error in errors) {
            log.error(error)
        }
        return errors.toList()
    }

    /**
     * Validate the inputfield for server properties.
     *
     * @author Griefed
     */
    private fun validateServerProperties(): List<String> {
        val errors: MutableList<String> = ArrayList(10)
        SwingUtilities.invokeLater {
            if (configurationHandler.checkIconAndProperties(getServerPropertiesPath())) {
                propertiesInfo.icon = guiProps.infoIcon
                propertiesInfo.toolTipText =
                    Gui.createserverpack_gui_createserverpack_textfield_propertiespath.toString()
            } else {
                errors.add(Gui.configuration_log_error_serverproperties.toString())
                propertiesInfo.icon = guiProps.errorIcon
                propertiesInfo.toolTipText = "<html>${errors.joinToString("<br>")}</html>"
            }
        }
        for (error in errors) {
            log.error(error)
        }
        return errors.toList()
    }

    /**
     * Get the configurations of the available ExtensionConfigPanel as a hashmap, so we can store them
     * in our serverpackcreator.conf.
     *
     * @return Map containing lists of CommentedConfigs mapped to the corresponding pluginID.
     */
    private fun getConfigPanelConfigs(): HashMap<String, ArrayList<CommentedConfig>> {
        val configs: HashMap<String, ArrayList<CommentedConfig>> =
            HashMap<String, ArrayList<CommentedConfig>>(10)
        if (pluginPanels.isNotEmpty()) {
            for (panel in pluginPanels) {
                configs[panel.pluginID] = panel.serverPackExtensionConfig()
            }
        }
        return configs
    }

    /**
     * Adds a mouse listener to the passed button which sets the content area of said button
     * filled|not-filled when the mouse enters|leaves the button.
     *
     * @param buttonToAddMouseListenerTo JButton. The button to add the mouse listener to.
     * @author Griefed
     */
    private fun addMouseListenerContentAreaFilledToButton(buttonToAddMouseListenerTo: JButton) {
        buttonToAddMouseListenerTo.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(event: MouseEvent) {
                buttonToAddMouseListenerTo.isContentAreaFilled = true
            }

            override fun mouseExited(event: MouseEvent) {
                buttonToAddMouseListenerTo.isContentAreaFilled = false
            }
        })
    }

    /**
     * Scan the modpack directory for various manifests and, if any are found, parse them and try to
     * load the Minecraft version, modloader and modloader version.
     *
     * @author Griefed
     */
    fun updateGuiFromSelectedModpack() {
        SwingUtilities.invokeLater {
            if (File(getModpackDirectory()).isDirectory) {
                try {
                    val packConfig = PackConfig()
                    var updated = false
                    if (File(getModpackDirectory(), "manifest.json").isFile) {
                        configurationHandler.updateConfigModelFromCurseManifest(
                            packConfig, File(
                                getModpackDirectory(), "manifest.json"
                            )
                        )
                        updated = true
                    } else if (File(getModpackDirectory(), "minecraftinstance.json").isFile) {
                        configurationHandler.updateConfigModelFromMinecraftInstance(
                            packConfig, File(
                                getModpackDirectory(), "minecraftinstance.json"
                            )
                        )
                        updated = true
                    } else if (File(getModpackDirectory(), "modrinth.index.json").isFile) {
                        configurationHandler.updateConfigModelFromModrinthManifest(
                            packConfig, File(
                                getModpackDirectory(), "modrinth.index.json"
                            )
                        )
                        updated = true
                    } else if (File(getModpackDirectory(), "instance.json").isFile) {
                        configurationHandler.updateConfigModelFromATLauncherInstance(
                            packConfig, File(
                                getModpackDirectory(), "instance.json"
                            )
                        )
                        updated = true
                    } else if (File(getModpackDirectory(), "config.json").isFile) {
                        configurationHandler.updateConfigModelFromConfigJson(
                            packConfig, File(
                                getModpackDirectory(), "config.json"
                            )
                        )
                        updated = true
                    } else if (File(getModpackDirectory(), "mmc-pack.json").isFile) {
                        configurationHandler.updateConfigModelFromMMCPack(
                            packConfig, File(
                                getModpackDirectory(), "mmc-pack.json"
                            )
                        )
                        updated = true
                    }
                    val dirsToInclude = TreeSet(getCopyDirectoriesList())
                    val files = File(getModpackDirectory()).listFiles()
                    if (files != null && files.isNotEmpty()) {
                        for (file in files) {
                            if (apiProperties.directoriesToInclude.contains(file.name)) {
                                dirsToInclude.add(file.name)
                            }
                        }
                    }
                    if (updated) {
                        setMinecraftVersion(packConfig.minecraftVersion)
                        setModloader(packConfig.modloader)
                        setModloaderVersion(packConfig.modloaderVersion)
                        setCopyDirectories(java.util.ArrayList(dirsToInclude))
                        JOptionPane.showMessageDialog(
                            this,
                            Gui.createserverpack_gui_modpack_scan_message(
                                getMinecraftVersion(),
                                getModloader(),
                                getModloaderVersion(),
                                utilities.stringUtilities.buildString(dirsToInclude.toList())
                            ) + "   ",
                            Gui.createserverpack_gui_modpack_scan.toString(),
                            JOptionPane.INFORMATION_MESSAGE,
                            guiProps.infoIcon
                        )
                    }
                } catch (ex: IOException) {
                    log.error("Couldn't update GUI from modpack manifests.", ex)
                }
            }
        }
    }

    /**
     * Reverts the list of clientside-only mods to the value of the last loaded configuration, if one
     * is available.
     *
     * @author Griefed
     */
    private fun revertExclusions() {
        if (lastLoadedConfiguration != null) {
            setClientSideMods(lastLoadedConfiguration!!.clientMods)
        }
    }

    /**
     * Reverts the list of copy directories to the value of the last loaded configuration, if one is
     * available.
     *
     * @author Griefed
     */
    private fun revertServerPackFiles() {
        if (lastLoadedConfiguration != null) {
            setCopyDirectories(lastLoadedConfiguration!!.copyDirs)
        }
    }

    /**
     * Setter for the Minecraft version depending on which one is selected in the GUI.
     *
     * @author Griefed
     */
    private fun updateMinecraftValues() {
        setModloaderVersionsModel(minecraftVersions.selectedItem!!.toString())
        updateRequiredJavaVersion()
        checkMinecraftServer()
    }

    /**
     * TODO docs
     */
    private fun updateRequiredJavaVersion() {
        javaVersion.text = acquireRequiredJavaVersion()
        if (javaVersion.text != "?" && apiProperties.javaPath(javaVersion.text).isPresent && !scriptKVPairs.getData()["SPC_JAVA_SPC"].equals(
                apiProperties.javaPath(javaVersion.text).get()
            ) && apiProperties.isJavaScriptAutoupdateEnabled
        ) {
            val path = apiProperties.javaPath(javaVersion.text).get()
            val data: HashMap<String, String> = scriptKVPairs.getData()
            data.replace(
                "SPC_JAVA_SPC", path
            )
            scriptKVPairs.loadData(data)
            log.info("Automatically adjusted script variable SPC_JAVA_SPC to $path")
        }
    }

    /**
     * Check whether the selected Minecraft version has a server available. If no server is available,
     * or no URL to download the server for the selected version is available, a warning is
     * displayed.
     *
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun checkMinecraftServer() {
        val mcVersion = minecraftVersions.selectedItem?.toString()
        val server = versionMeta.minecraft.getServer(mcVersion!!)
        if (!server.isPresent) {
            JOptionPane.showMessageDialog(
                this,
                Gui.createserverpack_gui_createserverpack_minecraft_server_unavailable(mcVersion) + "   ",
                Gui.createserverpack_gui_createserverpack_minecraft_server.toString(),
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
            )
        } else if (server.isPresent && !server.get().url().isPresent) {
            JOptionPane.showMessageDialog(
                this,
                Gui.createserverpack_gui_createserverpack_minecraft_server_url_unavailable(mcVersion) + "   ",
                Gui.createserverpack_gui_createserverpack_minecraft_server.toString(),
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
            )
        }
    }

    /**
     * If the checkbox is ticked and no Java is available, a message is displayed, warning the user
     * that Javapath needs to be defined for the modloader-server installation to work. If "Yes" is
     * clicked, a filechooser will open where the user can select their Java-executable/binary. If
     * "No" is selected, the user is warned about the consequences of not setting the
     * Javapath.<br></br><br></br>
     *
     *
     * If the installer for the current combination of the aforementioned versions can not be reached,
     * or is otherwise unavailable, the user is informed about SPC not being able to install it, thus
     * clearing the selection of the checkbox.
     *
     * Whenever the state of the server-installation checkbox changes, the global Java setting is
     * checked for validity, as well as the current combination of Minecraft version, modloader and
     * modloader version server-installer is available.<br></br><br></br>
     *
     *
     * If the checkbox is ticked and no Java is available, a message is displayed, warning the user
     * that Javapath needs to be defined for the modloader-server installation to work. If "Yes" is
     * clicked, a filechooser will open where the user can select their Java-executable/binary. If
     * "No" is selected, the user is warned about the consequences of not setting the
     * Javapath.<br></br><br></br>
     *
     *
     * If the installer for the current combination of the aforementioned versions can not be reached,
     * or is otherwise unavailable, the user is informed about SPC not being able to install it, thus
     * clearing the selection of the checkbox.
     *
     * @return `true` if, and only if, no problem was encountered.
     * @author Griefed
     */
    fun checkServer(): Boolean {
        var okay = true
        if (isServerInstallationTicked()) {
            val mcVersion = minecraftVersions.selectedItem!!.toString()
            val modloader = modloaders.selectedItem!!.toString()
            val modloaderVersion = modloaderVersions.selectedItem!!.toString()
            if (!checkJava()) {
                this.setServerInstallationTicked(false)
                okay = false
            }
            if (!serverPackHandler.serverDownloadable(mcVersion, modloader, modloaderVersion)) {
                JOptionPane.showMessageDialog(
                    this, Gui.createserverpack_gui_createserverpack_checkboxserver_unavailable_message(
                        modloader, mcVersion, modloader, modloaderVersion, modloader
                    ) + "    ", Gui.createserverpack_gui_createserverpack_checkboxserver_unavailable_title(
                        mcVersion, modloader, modloaderVersion
                    ), JOptionPane.WARNING_MESSAGE, guiProps.warningIcon
                )
                this.setServerInstallationTicked(false)
                okay = false
            }
        }
        return okay
    }

    /**
     * If no Java is available, a message is displayed, warning the user that Javapath needs to be
     * defined for the modloader-server installation to work. If "Yes" is clicked, a filechooser will
     * open where the user can select their Java-executable/binary. If "No" is selected, the user is
     * warned about the consequences of not setting the Javapath.
     *
     * @return `true` if Java is available or was configured by the user.
     * @author Griefed
     */
    private fun checkJava(): Boolean {
        return if (!apiProperties.javaAvailable()) {
            when (JOptionPane.showConfirmDialog(
                this,
                Gui.createserverpack_gui_createserverpack_checkboxserver_confirm_message.toString(),
                Gui.createserverpack_gui_createserverpack_checkboxserver_confirm_title.toString(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                guiProps.warningIcon
            )) {
                0 -> {
                    chooseJava()
                    true
                }

                1 -> {
                    JOptionPane.showMessageDialog(
                        this,
                        Gui.createserverpack_gui_createserverpack_checkboxserver_message_message.toString(),
                        Gui.createserverpack_gui_createserverpack_checkboxserver_message_title.toString(),
                        JOptionPane.ERROR_MESSAGE,
                        guiProps.errorIcon
                    )
                    false
                }

                else -> false
            }
        } else {
            true
        }
    }

    /**
     * Opens a filechooser to select the Java-executable/binary.
     *
     * @author Griefed
     */
    private fun chooseJava() {
        val javaChooser = JFileChooser()
        if (File("%s/bin/".format(System.getProperty("java.home"))).isDirectory) {
            javaChooser.currentDirectory = File("%s/bin/".format(System.getProperty("java.home")))
        } else {
            javaChooser.currentDirectory = apiProperties.homeDirectory
        }
        javaChooser.dialogTitle = Gui.createserverpack_gui_buttonjavapath_tile.toString()
        javaChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        javaChooser.isAcceptAllFileFilterUsed = true
        javaChooser.isMultiSelectionEnabled = false
        javaChooser.preferredSize = Dimension(750, 450)
        if (javaChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            apiProperties.javaPath = javaChooser.selectedFile.path
            log.debug("Set path to Java executable to: ${javaChooser.selectedFile.path}")
        }
    }

    /**
     * When the GUI has finished loading, try and load the existing serverpackcreator.conf-file into
     * ServerPackCreator.
     *
     * @param configFile The configuration file to parse and load into the GUI.
     * @author Griefed
     */
    fun loadConfig(configFile: File) {
        try {
            val packConfig = PackConfig(utilities, configFile)
            lastLoadedConfiguration = packConfig
            setModpackDirectory(packConfig.modpackDir)
            if (packConfig.clientMods.isEmpty()) {
                this.setClientSideMods(apiProperties.clientSideMods())
                log.debug("Set clientMods with fallback list.")
            } else {
                this.setClientSideMods(packConfig.clientMods)
            }
            if (packConfig.copyDirs.isEmpty()) {
                setCopyDirectories(mutableListOf("mods", "config"))
            } else {
                setCopyDirectories(packConfig.copyDirs)
            }
            setScriptVariables(packConfig.scriptSettings)
            setServerIconPath(packConfig.serverIconPath)
            setServerPropertiesPath(packConfig.serverPropertiesPath)
            if (packConfig.minecraftVersion.isEmpty()) {
                packConfig.minecraftVersion = versionMeta.minecraft.latestRelease().version
            }
            setMinecraftVersion(packConfig.minecraftVersion)
            setModloader(packConfig.modloader)
            setModloaderVersion(packConfig.modloaderVersion)
            this.setServerInstallationTicked(packConfig.isServerInstallationDesired)
            this.setIconInclusionTicked(packConfig.isServerIconInclusionDesired)
            this.setPropertiesInclusionTicked(packConfig.isServerPropertiesInclusionDesired)
            this.setZipArchiveCreationTicked(packConfig.isZipCreationDesired)
            setJavaArguments(packConfig.javaArgs)
            this.setServerPackSuffix(packConfig.serverPackSuffix)
            for (panel in pluginPanels) {
                panel.setServerPackExtensionConfig(packConfig.getPluginConfigs(panel.pluginID))
            }
        } catch (ex: Exception) {
            log.error("Couldn't load configuration file.", ex)
            JOptionPane.showMessageDialog(
                this,
                Gui.createserverpack_gui_config_load_error_message.toString() + " " + ex.cause + "   ",
                Gui.createserverpack_gui_config_load_error.toString(),
                JOptionPane.ERROR_MESSAGE,
                guiProps.errorIcon
            )
        }
    }

    /**
     * Open the server.properties-file in the local editor. If no file is specified by the user, the
     * default server.properties in `server_files` will be opened.
     *
     * @author Griefed
     */
    private fun openServerProperties() {
        if (File(getServerPropertiesPath()).isFile) {
            utilities.fileUtilities.openFile(getServerPropertiesPath())
        } else {
            utilities.fileUtilities.openFile(apiProperties.defaultServerProperties)
        }
    }
}