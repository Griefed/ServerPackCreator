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
                "\"FILE_SERVERPACKCREATOR_PROPERTIES\":\"" + APPLICATIONPROPERTIES.FILE_SERVERPACKCREATOR_PROPERTIES + "\"," +
                "\"FILE_WINDOWS\":\"" + APPLICATIONPROPERTIES.FILE_WINDOWS + "\"," +
                "\"FILE_LINUX\":\"" + APPLICATIONPROPERTIES.FILE_LINUX + "\"," +
                "\"FILE_FORGE_ONE_SEVEN_USER_JVM_ARGS\":\"" + APPLICATIONPROPERTIES.FILE_FORGE_ONE_SEVEN_USER_JVM_ARGS + "\"," +
                "\"PLUGINS_DIRECTORY\":\"" + APPLICATIONPROPERTIES.PLUGINS_DIRECTORY.toString().replace("\\","/") + "\"," +
                "\"LIST_FALLBACK_MODS_DEFAULT\":" + LISTUTILITIES.encapsulateListElements(APPLICATIONPROPERTIES.LIST_FALLBACK_MODS_DEFAULT) + "," +
                "\"FILE_CONFIG\":\"" + APPLICATIONPROPERTIES.FILE_CONFIG + "\"," +
                "\"FILE_CONFIG_OLD\":\"" + APPLICATIONPROPERTIES.FILE_CONFIG_OLD + "\"," +
                "\"FILE_SERVER_PROPERTIES\":\"" + APPLICATIONPROPERTIES.FILE_SERVER_PROPERTIES + "\"," +
                "\"FILE_SERVER_ICON\":\"" + APPLICATIONPROPERTIES.FILE_SERVER_ICON + "\"," +
                "\"FILE_MANIFEST_MINECRAFT\":\"" + APPLICATIONPROPERTIES.FILE_MANIFEST_MINECRAFT + "\"," +
                "\"FILE_MANIFEST_FORGE\":\"" + APPLICATIONPROPERTIES.FILE_MANIFEST_FORGE + "\"," +
                "\"FILE_MANIFEST_FABRIC\":\"" + APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC + "\"," +
                "\"FILE_MANIFEST_FABRIC_INSTALLER\":\"" + APPLICATIONPROPERTIES.FILE_MANIFEST_FABRIC_INSTALLER + "\"," +
                "\"FILE_SERVERPACKCREATOR_DATABASE\":\"" + APPLICATIONPROPERTIES.FILE_SERVERPACKCREATOR_DATABASE + "\"," +
                "\"PATH_FILE_MANIFEST_MINECRAFT\":\"" + APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_MINECRAFT.toString().replace("\\","/") + "\"," +
                "\"PATH_FILE_MANIFEST_FORGE\":\"" + APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FORGE.toString().replace("\\","/") + "\"," +
                "\"PATH_FILE_MANIFEST_FABRIC\":\"" + APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC.toString().replace("\\","/") + "\"," +
                "\"PATH_FILE_MANIFEST_FABRIC_INSTALLER\":\"" + APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC_INSTALLER.toString().replace("\\","/") + "\"," +
                "\"directoryServerPacks\":\"" + APPLICATIONPROPERTIES.getDirectoryServerPacks() + "\"," +
                "\"listFallbackMods\":" + LISTUTILITIES.encapsulateListElements(APPLICATIONPROPERTIES.getListFallbackMods()) + "," +
                "\"listDirectoriesExclude\":" + LISTUTILITIES.encapsulateListElements(APPLICATIONPROPERTIES.getListOfDirectoriesToExclude()) + "," +
                "\"curseControllerRegenerationEnabled\": " + APPLICATIONPROPERTIES.getCurseControllerRegenerationEnabled() + "," +
                "\"queueMaxDiskUsage\": " + APPLICATIONPROPERTIES.getQueueMaxDiskUsage() + "," +
                "\"saveLoadedConfiguration\": " + APPLICATIONPROPERTIES.getSaveLoadedConfiguration() + "," +
                "\"serverPackCreatorVersion\":\"" + APPLICATIONPROPERTIES.getServerPackCreatorVersion() + "\"," +
                "\"versioncheck_prerelease\": " + APPLICATIONPROPERTIES.checkForAvailablePreReleases() + "," +
                "\"isCurseForgeActivated\": " + APPLICATIONPROPERTIES.isCurseForgeActivated() + "" +
                "}";*/
        return "{" +
                "\"listFallbackMods\":" + UTILITIES.ListUtils().encapsulateListElements(APPLICATIONPROPERTIES.getListFallbackMods()) + "," +
                "\"listDirectoriesExclude\":" + UTILITIES.ListUtils().encapsulateListElements(APPLICATIONPROPERTIES.getListOfDirectoriesToExclude()) + "," +
                "\"curseControllerRegenerationEnabled\": " + APPLICATIONPROPERTIES.getCurseControllerRegenerationEnabled() + "," +
                "\"serverPackCreatorVersion\":\"" + APPLICATIONPROPERTIES.getServerPackCreatorVersion() + "\"," +
                "\"isCurseForgeActivated\": " + APPLICATIONPROPERTIES.isCurseForgeActivated() +
                "}";
    }
}
