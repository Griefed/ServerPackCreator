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
package de.griefed.serverpackcreator.clientside

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the one predicate that decides whether a mod *claims* to support servers, from the platform's
 * self-report and the jar's own sideness spec together.
 *
 * It is shared on purpose. The same answer both drives the "Declared server/both but the server crashed"
 * note and arms the other-version crash re-check, and the two must never disagree about what "declared
 * server" means — a report that prints the contradiction while the re-check silently decided there was none
 * is worse than either behaviour on its own.
 */
internal class ClientsideVerifierServerSupportTest {

    /** Modrinth is the only platform with the field, and `required` is the unambiguous claim. */
    @Test
    fun aPlatformRequiringTheServerSideDeclaresServerSupport() {
        Assertions.assertTrue(ClientsideVerifier.declaresServerSupport(DeclaredSupport.REQUIRED, JarScan.ERROR))
    }

    /**
     * The `iron-chests` shape, and the only signal CurseForge can ever produce: the platform has no sideness
     * field at all, so [DeclaredSupport.UNKNOWN] is all it says, and the claim comes from SPC's own scan of
     * the jar. Miss this and the re-check never arms for a CurseForge mod — which is every mod that produced
     * the false positive this guard exists for.
     */
    @Test
    fun aJarDeclaringServerOrBothDeclaresServerSupportEvenWhenThePlatformSaysNothing() {
        Assertions.assertTrue(ClientsideVerifier.declaresServerSupport(DeclaredSupport.UNKNOWN, JarScan.SERVER_OR_BOTH))
    }

    /** Nothing contradicts a crash here: both signals lean clientside, or say nothing at all. */
    @Test
    fun clientLeaningAndSilentSignalsDeclareNoServerSupport() {
        Assertions.assertFalse(ClientsideVerifier.declaresServerSupport(DeclaredSupport.UNSUPPORTED, JarScan.CLIENT))
        Assertions.assertFalse(ClientsideVerifier.declaresServerSupport(DeclaredSupport.UNKNOWN, JarScan.CLIENT))
        Assertions.assertFalse(ClientsideVerifier.declaresServerSupport(DeclaredSupport.UNKNOWN, JarScan.ERROR))
    }

    /**
     * A distribution-locked file was never scanned, so it declared nothing — "we could not look" must not be
     * read as "it said server", the same distinction the rest of the engine is built on.
     */
    @Test
    fun aDeferredJarScanDeclaresNothing() {
        Assertions.assertFalse(ClientsideVerifier.declaresServerSupport(DeclaredSupport.UNKNOWN, JarScan.DEFERRED))
    }

    /**
     * `optional` deliberately does not count, mirroring the confidence aggregation exactly: it means the mod
     * runs with or without the side, which is not a claim that the server works.
     */
    @Test
    fun anOptionalServerSideIsNotAClaimOfServerSupport() {
        Assertions.assertFalse(ClientsideVerifier.declaresServerSupport(DeclaredSupport.OPTIONAL, JarScan.ERROR))
    }
}
