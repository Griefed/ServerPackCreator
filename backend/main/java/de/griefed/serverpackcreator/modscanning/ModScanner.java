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
package de.griefed.serverpackcreator.modscanning;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Easy-access class for scanning of mods inside a modpack. This class itself does not do much,
 * other than bringing the different mod-scanners to one place for ease-of-use.
 *
 * @author Griefed
 */
@Service
public final class ModScanner {

  private final AnnotationScanner ANNOTATION;
  private final FabricScanner FABRIC;
  private final QuiltScanner QUILT;
  private final TomlScanner TOML;

  @Contract(pure = true)
  @Autowired
  public ModScanner(
      @NotNull AnnotationScanner annotationScanner,
      @NotNull FabricScanner fabricScanner,
      @NotNull QuiltScanner quiltScanner,
      @NotNull TomlScanner tomlScanner
  ) {
    ANNOTATION = annotationScanner;
    FABRIC = fabricScanner;
    QUILT = quiltScanner;
    TOML = tomlScanner;
  }

  /**
   * Forge annotation-based scanner.
   *
   * @return Scanner for scanning Forge annotations for sideness.
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull AnnotationScanner annotations() {
    return ANNOTATION;
  }

  /**
   * Fabric-based scanner.
   *
   * @return Scanner for scanning Fabric mods for sideness.
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull FabricScanner fabric() {
    return FABRIC;
  }

  /**
   * Quilt-based scanner.
   *
   * @return Scanner for scanning Quilt mods for sideness.
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull QuiltScanner quilt() {
    return QUILT;
  }

  /**
   * Forge toml-based scanner.
   *
   * @return Scanner for scanning Forge tomls for sideness.
   * @author Griefed
   */
  @Contract(pure = true)
  public @NotNull TomlScanner tomls() {
    return TOML;
  }
}
