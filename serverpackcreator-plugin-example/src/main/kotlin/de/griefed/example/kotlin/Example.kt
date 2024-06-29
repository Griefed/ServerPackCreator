/*
 * MIT License
 *
 * Copyright (C) 2024 Griefed
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package de.griefed.example.kotlin

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.ConfigCheck
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.plugins.PluginContext
import de.griefed.serverpackcreator.api.plugins.ServerPackCreatorPlugin
import de.griefed.serverpackcreator.api.utilities.*
import java.nio.file.Path

/**
 * Extremely simplified plugin by extending [ServerPackCreatorPlugin], which takes care of the basics for you.
 * @author Griefed
 */
class Example(context: PluginContext) : ServerPackCreatorPlugin(context) {

    init {
        val genericListener = object : SPCGenericListener {
            override fun run() {
                println("Helloooooo. I'm a SPCGenericListener!")
                println("Helloooooo. I'm a SPCGenericListener!")
                println("Helloooooo. I'm a SPCGenericListener!")
            }

        }
        val configListener = object : SPCConfigCheckListener {
            override fun run(packConfig: PackConfig, configCheck: ConfigCheck) {
                println("Helloooooo. I'm a SPCConfigCheckListener!")
                println(packConfig.modpackDir)
            }

        }
        val preServerPackListener = object : SPCPreServerPackGenerationListener {
            override fun run(packConfig: PackConfig, serverPackPath: Path) {
                println("Helloooooo. I'm a SPCPreServerPackGenerationListener!")
                println(packConfig.modpackDir)
                println(serverPackPath.toString())
            }

        }
        val preZipListener = object : SPCPreServerPackZipListener {
            override fun run(packConfig: PackConfig, serverPackPath: Path) {
                println("Helloooooo. I'm a SPCPreServerPackZipListener!")
                println(packConfig.modpackDir)
                println(serverPackPath.toString())
            }

        }
        val postGenListener = object : SPCPostGenListener {
            override fun run(packConfig: PackConfig, serverPackPath: Path) {
                println("Helloooooo. I'm a SPCPostGenListener!")
                println(packConfig.modpackDir)
                println(serverPackPath.toString())
            }

        }

        ApiWrapper.api().configurationHandler.addEventListener(genericListener)
        ApiWrapper.api().configurationHandler.addEventListener(configListener)
        ApiWrapper.api().serverPackHandler.addEventListener(genericListener)
        ApiWrapper.api().serverPackHandler.addEventListener(preServerPackListener)
        ApiWrapper.api().serverPackHandler.addEventListener(preZipListener)
        ApiWrapper.api().serverPackHandler.addEventListener(postGenListener)
    }

}