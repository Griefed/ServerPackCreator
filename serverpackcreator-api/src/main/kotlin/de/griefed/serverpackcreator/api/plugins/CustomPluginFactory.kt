/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.api.plugins

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.pf4j.DefaultPluginFactory
import org.pf4j.Plugin
import org.pf4j.PluginWrapper

/**
 * Custom plugin factory to enforce plugins having a context in which they run.
 *
 * @author Griefed
 */
class CustomPluginFactory: DefaultPluginFactory() {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    override fun createInstance(pluginClass: Class<*>, pluginWrapper: PluginWrapper): Plugin? {
        val context = PluginContext(pluginWrapper.runtimeMode)
        try {
            val constructor = pluginClass.getConstructor(PluginContext::class.java)
            return constructor.newInstance(context) as Plugin
        } catch (e: Exception) {
            log.error("Could not instantiate plugin.", e)
        }
        return null
    }
}