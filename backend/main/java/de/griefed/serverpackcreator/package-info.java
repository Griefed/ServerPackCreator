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

/**
 * Core-package of ServerPackCreator.
 * <p>
 * Server packs are created using {@link de.griefed.serverpackcreator.ConfigurationModel}, which can
 * be checked for errors using
 * {@link
 * de.griefed.serverpackcreator.ConfigurationHandler#checkConfiguration(de.griefed.serverpackcreator.ConfigurationModel,
 * boolean)} and any of the available variants. Afterwards, when the checks of the given
 * configuration model return no errors, it is fed into
 * {@link
 * de.griefed.serverpackcreator.ServerPackHandler#run(de.griefed.serverpackcreator.ConfigurationModel)},
 * which creates finally creates your server pack.
 * <p>
 * In other words, the intended workflow is as follows:
 * <ol>
 *   <li>Create a {@link de.griefed.serverpackcreator.ConfigurationModel}.</li>
 *   <li>Check it using {@link de.griefed.serverpackcreator.ConfigurationHandler#checkConfiguration(de.griefed.serverpackcreator.ConfigurationModel, boolean)} or variants.</li>
 *   <li>Create the server pack using {@link de.griefed.serverpackcreator.ServerPackHandler#run(de.griefed.serverpackcreator.ConfigurationModel)}.</li>
 * </ol>
 * <p>
 * Should you wish to customize your instance of ServerPackCreator, see {@link de.griefed.serverpackcreator.ApplicationProperties}.
 * If you wish to enhance your instance of ServerPackCreator with addons, see {@link de.griefed.serverpackcreator.ApplicationAddons}
 * and
 * <ul>
 *   <li>{@link de.griefed.serverpackcreator.addons.configurationhandler.ConfigCheckExtension}</li>
 *   <li>{@link de.griefed.serverpackcreator.addons.serverpackhandler.PreGenExtension}</li>
 *   <li>{@link de.griefed.serverpackcreator.addons.serverpackhandler.PreZipExtension}</li>
 *   <li>{@link de.griefed.serverpackcreator.addons.serverpackhandler.PostGenExtension}</li>
 *   <li>{@link de.griefed.serverpackcreator.addons.swinggui.ConfigPanelExtension} and {@link de.griefed.serverpackcreator.addons.swinggui.ExtensionConfigPanel}</li>
 *   <li>{@link de.griefed.serverpackcreator.addons.swinggui.TabExtension} and {@link de.griefed.serverpackcreator.addons.swinggui.ExtensionTab}</li>
 * </ul>
 *
 * @author Griefed
 */
package de.griefed.serverpackcreator;
