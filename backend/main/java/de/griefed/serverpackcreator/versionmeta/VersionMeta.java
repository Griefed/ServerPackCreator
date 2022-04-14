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
package de.griefed.serverpackcreator.versionmeta;

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.versionmeta.fabric.FabricMeta;
import de.griefed.serverpackcreator.versionmeta.forge.ForgeMeta;
import de.griefed.serverpackcreator.versionmeta.minecraft.MinecraftMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * VersionMeta containing available versions and important details for Minecraft, Fabric and Forge.
 * @author Griefed
 */
@Service
public class VersionMeta {

    private final MinecraftMeta MINECRAFT_META;
    private final FabricMeta FABRIC_META;
    private final ForgeMeta FORGE_META;

    /**
     * Constructor.
     * @author Griefed
     * @param injectedApplicationProperties Instance of {@link ApplicationProperties}.
     * @throws IOException if one of the metas could not be initialized.
     */
    @Autowired
    public VersionMeta(ApplicationProperties injectedApplicationProperties) throws IOException {
        ApplicationProperties APPLICATIONPROPERTIES;
        if (injectedApplicationProperties == null) {
            APPLICATIONPROPERTIES = new ApplicationProperties();
        } else {
            APPLICATIONPROPERTIES = injectedApplicationProperties;
        }

        this.FORGE_META = new ForgeMeta(APPLICATIONPROPERTIES);
        this.MINECRAFT_META = new MinecraftMeta(APPLICATIONPROPERTIES, this.FORGE_META);
        this.FABRIC_META = new FabricMeta(APPLICATIONPROPERTIES);

        this.FORGE_META.initialize(this.MINECRAFT_META);

        update();
    }

    /**
     * Update the Minecraft, Forge and Fabric metas. Usually called when the manifest files have been refreshed.
     * @author Griefed
     * @return Instance of {@link VersionMeta}.
     * @throws IOException if any of the metas could not be updated.
     */
    public VersionMeta update() throws IOException {
        this.MINECRAFT_META.update();
        this.FABRIC_META.update();
        this.FORGE_META.update();
        return this;
    }

    /**
     * The MinecraftMeta instance for working with Minecraft versions and information about them.
     * @author Griefed
     * @return Instance of {@link MinecraftMeta}.
     */
    public MinecraftMeta minecraft() {
        return MINECRAFT_META;
    }

    /**
     * The FabricMeta instance for working with Fabric versions and information about them.
     * @author Griefed
     * @return Instance of {@link FabricMeta}.
     */
    public FabricMeta fabric() {
        return FABRIC_META;
    }

    /**
     * The ForgeMeta instance for working with Forge versions and information about them.
     * @author Griefed
     * @return Instance of {@link ForgeMeta}.
     */
    public ForgeMeta forge() {
        return FORGE_META;
    }
}
