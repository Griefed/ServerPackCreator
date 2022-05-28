/* Copyright (C) 2022  Griefed
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
package de.griefed.serverpackcreator.spring;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.utilities.commonutilities.Utilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * RestController for acquiring the configuration of this ServerPackCreator instance.
 * @author Griefed
 */
@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/api/v1/settings")
public class ApplicationPropertiesController {

    private static final Logger LOG = LogManager.getLogger(ApplicationPropertiesController.class);

    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final Utilities UTILITIES;

    /**
     * Constructor for DI.
     * @author Griefed
     * @param injectedApplicationProperties Instance of {@link ApplicationProperties} with the configuration of this ServerPackCreator
     *                                      instance.
     * @param injectedUtilities Instance of {@link Utilities}.
     */
    @Autowired
    public ApplicationPropertiesController(ApplicationProperties injectedApplicationProperties,
                                           Utilities injectedUtilities) {

        this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        this.UTILITIES = injectedUtilities;
    }

    @GetMapping(produces = "application/json")
    public String getConfiguration() {
        // TODO: Fix formatting and content
        /*return "{" +
                "\"SERVERPACKCREATOR_PROPERTIES\":\"" + APPLICATIONPROPERTIES.SERVERPACKCREATOR_PROPERTIES + "\"," +
                "\"START_SCRIPT_WINDOWS\":\"" + APPLICATIONPROPERTIES.START_SCRIPT_WINDOWS + "\"," +
                "\"START_SCRIPT_LINUX\":\"" + APPLICATIONPROPERTIES.START_SCRIPT_LINUX + "\"," +
                "\"USER_JVM_ARGS\":\"" + APPLICATIONPROPERTIES.USER_JVM_ARGS + "\"," +
                "\"PLUGINS_DIRECTORY\":\"" + APPLICATIONPROPERTIES.PLUGINS_DIRECTORY.toString().replace("\\","/") + "\"," +
                "\"FALLBACK_CLIENTSIDE_MODS\":" + LISTUTILITIES.encapsulateListElements(APPLICATIONPROPERTIES.FALLBACK_CLIENTSIDE_MODS) + "," +
                "\"DEFAULT_CONFIG\":\"" + APPLICATIONPROPERTIES.DEFAULT_CONFIG + "\"," +
                "\"OLD_CONFIG\":\"" + APPLICATIONPROPERTIES.OLD_CONFIG + "\"," +
                "\"DEFAULT_SERVER_PROPERTIES\":\"" + APPLICATIONPROPERTIES.DEFAULT_SERVER_PROPERTIES + "\"," +
                "\"DEFAULT_SERVER_ICON\":\"" + APPLICATIONPROPERTIES.DEFAULT_SERVER_ICON + "\"," +
                "\"MINECRAFT_VERSION_MANIFEST\":\"" + APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST + "\"," +
                "\"FORGE_VERSION_MANIFEST\":\"" + APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST + "\"," +
                "\"FABRIC_VERSION_MANIFEST\":\"" + APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST + "\"," +
                "\"FABRIC_INSTALLER_VERSION_MANIFEST\":\"" + APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST + "\"," +
                "\"SERVERPACKCREATOR_DATABASE\":\"" + APPLICATIONPROPERTIES.SERVERPACKCREATOR_DATABASE + "\"," +
                "\"MINECRAFT_VERSION_MANIFEST_LOCATION\":\"" + APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST_LOCATION.toString().replace("\\","/") + "\"," +
                "\"FORGE_VERSION_MANIFEST_LOCATION\":\"" + APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST_LOCATION.toString().replace("\\","/") + "\"," +
                "\"FABRIC_VERSION_MANIFEST_LOCATION\":\"" + APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST_LOCATION.toString().replace("\\","/") + "\"," +
                "\"FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION\":\"" + APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION.toString().replace("\\","/") + "\"," +
                "\"directoryServerPacks\":\"" + APPLICATIONPROPERTIES.getDirectoryServerPacks() + "\"," +
                "\"listFallbackMods\":" + LISTUTILITIES.encapsulateListElements(APPLICATIONPROPERTIES.getListFallbackMods()) + "," +
                "\"listDirectoriesExclude\":" + LISTUTILITIES.encapsulateListElements(APPLICATIONPROPERTIES.getListOfDirectoriesToExclude()) + "," +
                "\"curseControllerRegenerationEnabled\": " + APPLICATIONPROPERTIES.getCurseControllerRegenerationEnabled() + "," +
                "\"queueMaxDiskUsage\": " + APPLICATIONPROPERTIES.getQueueMaxDiskUsage() + "," +
                "\"saveLoadedConfiguration\": " + APPLICATIONPROPERTIES.getSaveLoadedConfiguration() + "," +
                "\"serverPackCreatorVersion\":\"" + APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION() + "\"," +
                "\"versioncheck_prerelease\": " + APPLICATIONPROPERTIES.checkForAvailablePreReleases() + "," +
                "\"isCurseForgeActivated\": " + APPLICATIONPROPERTIES.isCurseForgeActivated() + "" +
                "}";*/
        return "{" +
                "\"listFallbackMods\":" + UTILITIES.ListUtils().encapsulateListElements(APPLICATIONPROPERTIES.getListFallbackMods()) + "," +
                "\"listDirectoriesExclude\":" + UTILITIES.ListUtils().encapsulateListElements(APPLICATIONPROPERTIES.getListOfDirectoriesToExclude()) + "," +
                "\"serverPackCreatorVersion\":\"" + APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION() + "\"," +
                "\"supportedModloaders\":" + UTILITIES.ListUtils().encapsulateListElements(Arrays.asList(APPLICATIONPROPERTIES.SUPPORTED_MODLOADERS())) +
                "}";
    }
}
