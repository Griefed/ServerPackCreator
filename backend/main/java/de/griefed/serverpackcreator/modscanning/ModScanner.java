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

import de.griefed.serverpackcreator.utilities.common.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Easy-access class for scanning of mods inside a modpack. This class itself does not do much,
 * other than bringing the different mod-scanners to one place for ease-of-use.
 *
 * @author Griefed
 */
@Service
public class ModScanner {

  private final AnnotationScanner ANNOTATION;
  private final FabricScanner FABRIC;
  private final QuiltScanner QUILT;
  private final TomlScanner TOML;

  @Autowired
  public ModScanner(
      AnnotationScanner annotationScanner,
      FabricScanner fabricScanner,
      QuiltScanner quiltScanner,
      TomlScanner tomlScanner
      ) {
    this.ANNOTATION = annotationScanner;
    this.FABRIC = fabricScanner;
    this.QUILT = quiltScanner;
    this.TOML = tomlScanner;
  }

  /**
   * Forge annotation-based scanner.
   *
   * @return {@link AnnotationScanner} for scanning Forge annotations for sideness.
   * @author Griefed
   */
  public AnnotationScanner annotations() {
    return ANNOTATION;
  }

  /**
   * Fabric-based scanner.
   *
   * @return {@link FabricScanner} for scanning Fabric mods for sideness.
   * @author Griefed
   */
  public FabricScanner fabric() {
    return FABRIC;
  }

  /**
   * Quilt-based scanner.
   *
   * @return {@link QuiltScanner} for scanning Quilt mods for sideness.
   * @author Griefed
   */
  public QuiltScanner quilt() {
    return QUILT;
  }

  /**
   * Forge toml-based scanner.
   *
   * @return {@link TomlScanner} for scanning Forge tomls for sideness.
   * @author Griefed
   */
  public TomlScanner tomls() {
    return TOML;
  }
}
