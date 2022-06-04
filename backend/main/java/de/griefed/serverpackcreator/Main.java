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
package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.utilities.misc.Generated;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;

import java.io.IOException;

/**
 * The Main-class is only responisble for creating an isntance of {@link ServerPackCreator} with the passed {@link String}-array
 * from the commandline and then subsequently running ServerPackCreator in the determined mode, with the determined locale.
 * <br><br>
 * For a list of available commandline arguments, check out {@link CommandlineParser.Mode}
 *
 * @author Griefed
 */
@Generated
public class Main {

    /**
     * Initialize ServerPackCreator with the passed commandline-arguments and run.
     *
     * @param args Commandline arguments with which ServerPackCreator is run. Determines which mode ServerPackCreator
     *             will enter and which locale is used.
     * @throws IOException if the {@link VersionMeta} could not be instantiated.
     * @author Griefed
     */
    public static void main(String[] args) throws IOException {

        ServerPackCreator serverPackCreator = new ServerPackCreator(args);

        serverPackCreator.run();

    }
}