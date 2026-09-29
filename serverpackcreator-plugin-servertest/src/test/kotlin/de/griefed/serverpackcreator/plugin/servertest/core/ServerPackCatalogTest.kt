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
package de.griefed.serverpackcreator.plugin.servertest.core

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins which directories are offered as launchable server packs, and what the list says about them.
 *
 * The server-packs directory holds more than server packs: every generated pack sits beside its own
 * `<name>_server_pack.zip`, and a user may keep anything else in there. `manifest.json` is the discriminator
 * because it is ServerPackCreator's own "I produced this" marker — the same file `ServerPackUpdater` keys on
 * to tell an update run from a first run.
 *
 * The mapper here is a **bare** `ObjectMapper`, deliberately: the catalog is handed SPC's, whose leniency
 * comes from how `ApiWrapper` happens to build it, and a plugin must not depend on a collaborator's
 * configuration it does not own.
 */
internal class ServerPackCatalogTest {

    private val catalog = ServerPackCatalog(ObjectMapper(), Platform.POSIX)

    /** A generated pack: a manifest naming its versions, and the start scripts SPC writes beside it. */
    private fun generatedPack(
        parent: File,
        name: String,
        manifest: String = """{"files":["start.sh"],"minecraftVersion":"1.21","modloader":"NeoForge","modloaderVersion":"21.0.18"}""",
        withStartScript: Boolean = true
    ): File = File(parent, name).apply {
        mkdirs()
        File(this, "manifest.json").writeText(manifest)
        if (withStartScript) {
            File(this, "start.sh").writeText("#!/usr/bin/env bash\n")
        }
    }

    /** The happy path: a generated pack is listed with its versions and is launchable. */
    @Test
    fun listsAGeneratedPackWithItsManifestVersions(@TempDir serverPacks: File) {
        generatedPack(serverPacks, "NeoForge-1.21")

        val packs = catalog.packsIn(serverPacks)

        Assertions.assertEquals(1, packs.size, "Expected exactly the one generated pack, got $packs")
        val pack = packs.single()
        Assertions.assertEquals("NeoForge-1.21", pack.name)
        Assertions.assertEquals("1.21", pack.minecraftVersion)
        Assertions.assertEquals("NeoForge", pack.modloader)
        Assertions.assertEquals("21.0.18", pack.modloaderVersion)
        Assertions.assertInstanceOf(StartScriptSelection.Available::class.java, pack.selection)
    }

    /**
     * A directory with no manifest is not a server pack. Without this the list would offer a user's unrelated
     * folders — and, worse, offer to run a `start.sh` that is not ServerPackCreator's.
     */
    @Test
    fun skipsADirectoryWithoutAManifest(@TempDir serverPacks: File) {
        File(serverPacks, "not-a-pack").apply { mkdirs(); File(this, "start.sh").writeText("#!/bin/sh\n") }

        Assertions.assertTrue(catalog.packsIn(serverPacks).isEmpty(), "A directory with no manifest.json is not a pack.")
    }

    /** Every pack ships beside its own ZIP. Nothing can be launched out of an archive, so it is not listed. */
    @Test
    fun skipsTheZipArchiveBesideEachPack(@TempDir serverPacks: File) {
        generatedPack(serverPacks, "Fabric-1.20.6")
        File(serverPacks, "Fabric-1.20.6_server_pack.zip").writeText("not really a zip")

        Assertions.assertEquals(listOf("Fabric-1.20.6"), catalog.packsIn(serverPacks).map { it.name })
    }

    /**
     * A pack with no script for this host is listed, blocked, and says why. Hiding it would read as the
     * plugin losing a pack the user can plainly see in their file manager.
     */
    @Test
    fun listsAPackWithNoStartScriptAsBlockedRatherThanHidingIt(@TempDir serverPacks: File) {
        generatedPack(serverPacks, "Scriptless", withStartScript = false)

        val pack = catalog.packsIn(serverPacks).single()
        val missing = Assertions.assertInstanceOf(StartScriptSelection.Missing::class.java, pack.selection)
        Assertions.assertTrue(missing.reason.isNotBlank(), "A blocked pack must carry a reason to show.")
    }

    /**
     * A manifest carrying a field this build has never heard of still reads. SPC is upgraded on its own
     * schedule and the plugin on the user's, so a newer manifest must not make a pack vanish from the list.
     */
    @Test
    fun readsAManifestCarryingAnUnknownField(@TempDir serverPacks: File) {
        generatedPack(
            serverPacks,
            "FromTheFuture",
            manifest = """{"minecraftVersion":"1.99","modloader":"NeoForge","modloaderVersion":"99.0",""" +
                    """"somethingThisBuildHasNeverHeardOf":{"nested":true}}"""
        )

        val pack = catalog.packsIn(serverPacks).single()
        Assertions.assertEquals("1.99", pack.minecraftVersion)
    }

    /**
     * A corrupt manifest still marks the directory as a pack, because its *presence* is the marker. The pack
     * stays launchable with blank versions — the versions are decoration, and refusing to launch over
     * unreadable decoration would be the plugin withholding the one thing the user came for.
     */
    @Test
    fun keepsAPackWhoseManifestCannotBeParsed(@TempDir serverPacks: File) {
        generatedPack(serverPacks, "Corrupt", manifest = "{ this is not json")

        val pack = catalog.packsIn(serverPacks).single()
        Assertions.assertEquals("", pack.minecraftVersion, "An unreadable manifest yields no version, not a crash.")
        Assertions.assertInstanceOf(StartScriptSelection.Available::class.java, pack.selection)
    }

    /** Sorted case-insensitively, so a refresh never reshuffles the rows under the user's cursor. */
    @Test
    fun sortsPacksByNameIgnoringCase(@TempDir serverPacks: File) {
        for (name in listOf("zebra", "Alpha", "middle")) {
            generatedPack(serverPacks, name)
        }

        Assertions.assertEquals(listOf("Alpha", "middle", "zebra"), catalog.packsIn(serverPacks).map { it.name })
    }

    /**
     * A server-packs directory that is not there yields an empty list rather than an exception. This runs
     * from the tab's constructor, which `ApiPlugins` calls outside its try-block, so a throw here would take
     * the whole GUI's tab assembly down with it.
     */
    @Test
    fun anAbsentDirectoryYieldsNoPacksRatherThanThrowing(@TempDir parent: File) {
        Assertions.assertTrue(catalog.packsIn(File(parent, "never-created")).isEmpty())
    }
}
