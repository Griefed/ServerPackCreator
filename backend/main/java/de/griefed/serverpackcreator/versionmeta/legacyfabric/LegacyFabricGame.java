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
package de.griefed.serverpackcreator.versionmeta.legacyfabric;

import de.griefed.serverpackcreator.utilities.common.Utilities;
import java.io.File;
import org.jetbrains.annotations.NotNull;

final class LegacyFabricGame extends LegacyFabricVersioning {

  /**
   * Create a new instance holding information about LegacyFabric game versions.
   *
   * @param manifest  The manifest containing game version information.
   * @param utilities Commonly used utilities across ServerPackCreator.
   * @author Griefed
   */
  LegacyFabricGame(@NotNull File manifest,
                   @NotNull Utilities utilities) {
    super(manifest, utilities);
  }
}
