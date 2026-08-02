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
package de.griefed.serverpackcreator.api.versionmeta.forge

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the Minecraft → Forge version mapping.
 *
 * Where NeoForge derives its mapping from a *pattern*, Forge derives it from **string surgery**: the manifest is keyed
 * by Minecraft version and its entries repeat that key (`1.18.2` → `1.18.2-40.0.17`), so the Forge version is what
 * remains after cutting `minecraftVersion.length + 1` characters off the front. That is exact, and exactly as
 * silent when it is wrong — a cut in the wrong place yields a plausible-looking but bogus version string rather than
 * an error, which is the same failure mode that let the NeoForge mapping over-claim 820 builds unnoticed.
 */
internal class ForgeVersionMappingTest {

    /** Straightforward entries across the eras Forge has shipped. */
    @Test
    fun theMinecraftPrefixAndSeparatorAreRemoved() {
        Assertions.assertEquals("40.0.17", ForgeLoader.forgeVersionFrom("1.18.2-40.0.17", "1.18.2"))
        Assertions.assertEquals("47.4.22", ForgeLoader.forgeVersionFrom("1.20.1-47.4.22", "1.20.1"))
        Assertions.assertEquals("52.1.16", ForgeLoader.forgeVersionFrom("1.21.1-52.1.16", "1.21.1"))
        Assertions.assertEquals("14.23.5.2860", ForgeLoader.forgeVersionFrom("1.12.2-14.23.5.2860", "1.12.2"))
    }

    /**
     * **The invariant the cut silently depends on.** Forge's manifest writes `1.7.10_pre4` where Mojang's writes
     * `1.7.10-pre4`, and `ForgeLoader` reconciles the two by swapping `_` for `-` before using the *reconciled*
     * version's length to cut the *unreconciled* entry. That only works because the swap is length-preserving.
     * Should reconciliation ever grow or shrink the string, this cut would slice in the wrong place and produce a
     * corrupt Forge version with no error at all — so both halves are pinned here.
     */
    @Test
    fun theUnderscoreToHyphenReconciliationIsLengthPreserving() {
        val forgeKey = "1.7.10_pre4"
        val mojangVersion = forgeKey.replace("_", "-")

        Assertions.assertEquals("1.7.10-pre4", mojangVersion)
        Assertions.assertEquals(
            forgeKey.length,
            mojangVersion.length,
            "the cut uses the reconciled length against the raw entry — a non-length-preserving swap corrupts it"
        )
        Assertions.assertEquals(
            "10.12.2.1121",
            ForgeLoader.forgeVersionFrom("1.7.10_pre4-10.12.2.1121", mojangVersion),
            "a pre-release must map correctly even though the entry and the Minecraft version spell it differently"
        )
    }

    /** Forge versions carry four components on older Minecraft; nothing may be truncated. */
    @Test
    fun multiComponentForgeVersionsSurviveIntact() {
        Assertions.assertEquals("9.11.1.1345", ForgeLoader.forgeVersionFrom("1.6.4-9.11.1.1345", "1.6.4"))
        Assertions.assertEquals(
            "11.15.1.2318-1.7.10",
            ForgeLoader.forgeVersionFrom("1.7.10-11.15.1.2318-1.7.10", "1.7.10"),
            "entries that repeat the Minecraft version at the end keep it — only the leading key is stripped"
        )
    }

    /**
     * Characterises the cut's behaviour on input the real manifest does not produce — kept as a boundary record, not
     * as a claim that this happens. Measured 2026-07-31: **all 5025 entries across 77 Minecraft keys carry their own
     * key as a prefix**, and `minecraftVersion` is always derived from that key, so the wrong-offset case below
     * cannot arise in practice; it is pinned only to show that the function cannot detect it.
     *
     * An entry with nothing after its key **is** handled: it yields `null`, and `ForgeLoader.update` logs and skips it,
     * so one malformed entry costs one version instead of aborting the whole Forge load — `update` catches only
     * `MalformedURLException` and `NoSuchElementException`, so a slice error would have escaped it. The guard is a
     * length check on purpose: `startsWith("$minecraftVersion-")` would reject the legitimate `1.7.10_pre4` entry,
     * because entries carry the *raw* manifest key while the Minecraft version may be the reconciled one.
     */
    @Test
    fun anEntryWithNothingAfterItsKeyIsRejectedRatherThanThrowing() {
        // Wrong offset, no error: the caller cannot tell this apart from a good mapping.
        Assertions.assertEquals(".17", ForgeLoader.forgeVersionFrom("1.18.2-40.0.17", "1.18.2-40."))

        // Nothing after the key, and nothing after the separator: rejected, and neither throws.
        Assertions.assertNull(ForgeLoader.forgeVersionFrom("1.18.2", "1.18.2"))
        Assertions.assertNull(ForgeLoader.forgeVersionFrom("1.18.2-", "1.18.2"))
        // The shortest legitimate entry still maps.
        Assertions.assertEquals("4", ForgeLoader.forgeVersionFrom("1.18.2-4", "1.18.2"))
    }
}
