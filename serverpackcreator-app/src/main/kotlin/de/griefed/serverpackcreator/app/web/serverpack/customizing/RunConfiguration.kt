/* Copyright (C) 2026 Griefed
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
package de.griefed.serverpackcreator.app.web.serverpack.customizing

import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId

@Document
class RunConfiguration() {

    @MongoId(FieldType.STRING)
    var id: String? = null
        private set
    var minecraftVersion: String = ""
    var modloader: String = ""
    var modloaderVersion: String = ""

    /**
     * The JVM arguments a server started from this configuration runs with.
     *
     * Plain strings, embedded in this document. They were `@DBRef`s to a `StartArgument` collection
     * whose documents held nothing but their own `@MongoId` — so resolving one returned the string it
     * was already keyed by, at the cost of a join. Same for the two lists below.
     */
    var startArgs: MutableList<String> = mutableListOf()

    /** The clientside-only mods excluded from server packs built with this configuration. */
    var clientMods: MutableList<String> = mutableListOf()

    /** The mods kept regardless of a clientside match. */
    var whitelistedMods: MutableList<String> = mutableListOf()

    constructor(
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
        startArgs: MutableList<String>,
        clientMods: MutableList<String>,
        whitelistedMods: MutableList<String>
    ) : this() {
        this.minecraftVersion = minecraftVersion
        this.modloader = modloader
        this.modloaderVersion = modloaderVersion
        this.startArgs = startArgs
        this.clientMods = clientMods
        this.whitelistedMods = whitelistedMods
    }

    @Suppress("unused")
    @PersistenceCreator
    private constructor(
        id: String,
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
        startArgs: MutableList<String>,
        clientMods: MutableList<String>,
        whitelistedMods: MutableList<String>
    ) : this(minecraftVersion,modloader,modloaderVersion,startArgs,clientMods,whitelistedMods) {
        this.id = id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RunConfiguration

        if (minecraftVersion != other.minecraftVersion) return false
        if (modloader != other.modloader) return false
        if (modloaderVersion != other.modloaderVersion) return false
        if (startArgs != other.startArgs) return false
        if (clientMods != other.clientMods) return false
        if (whitelistedMods != other.whitelistedMods) return false

        return true
    }

    override fun hashCode(): Int {
        var result = minecraftVersion.hashCode()
        result = 31 * result + modloader.hashCode()
        result = 31 * result + modloaderVersion.hashCode()
        result = 31 * result + startArgs.hashCode()
        result = 31 * result + clientMods.hashCode()
        result = 31 * result + whitelistedMods.hashCode()
        return result
    }

    override fun toString(): String {
        return "RunConfiguration(id=$id, minecraftVersion='$minecraftVersion', modloader='$modloader', modloaderVersion='$modloaderVersion', startArgs=$startArgs, clientMods=$clientMods, whitelistedMods=$whitelistedMods)"
    }
}