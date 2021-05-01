/* Copyright (C) 2021  Griefed
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

package de.griefed.serverpackcreator.curseforgemodpack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
//TODO: Write docs for class
public class CurseMinecraft {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String version;
    private List<CurseModLoaders> modLoaders;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<CurseModLoaders> getModLoaders() {
        return modLoaders;
    }

    public void setModLoaders(List<CurseModLoaders> modLoaders) {
        this.modLoaders = modLoaders;
    }

    @Override
    public String toString() {
        return String.format("%s,%s",version,modLoaders);
    }
}