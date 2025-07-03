/* Copyright (C) 2024  Griefed
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
import org.springframework.data.mongodb.core.mapping.DBRef
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

    @DBRef
    var startArgs: MutableList<StartArgument> = mutableListOf()

    @DBRef
    var clientMods: MutableList<ClientMod> = mutableListOf()

    @DBRef
    var whitelistedMods: MutableList<WhitelistedMod> = mutableListOf()

    constructor(
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
        startArgs: MutableList<StartArgument>,
        clientMods: MutableList<ClientMod>,
        whitelistedMods: MutableList<WhitelistedMod>
    ) : this() {
        this.minecraftVersion = minecraftVersion
        this.modloader = modloader
        this.modloaderVersion = modloaderVersion
        this.startArgs = startArgs
        this.clientMods = clientMods
        this.whitelistedMods = whitelistedMods
    }

    @PersistenceCreator
    private constructor(
        id: String,
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
        startArgs: MutableList<StartArgument>,
        clientMods: MutableList<ClientMod>,
        whitelistedMods: MutableList<WhitelistedMod>
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