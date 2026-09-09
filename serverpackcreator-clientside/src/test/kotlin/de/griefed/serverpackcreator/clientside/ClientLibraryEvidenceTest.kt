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
 * Pins the two decisive client-only signatures that shipped as *examples* instead of as defaults.
 *
 * Reported from the live grinder: **`iris` scored INCONCLUSIVE with
 * `java.lang.NoClassDefFoundError: org/lwjgl/Version` in its console.** LWJGL is the client's windowing and
 * OpenGL binding, which a dedicated server never ships, so reaching it while the server starts is as
 * decisive as `net/minecraft/client` — arguably more so, since no environment failure can fabricate it
 * either.
 *
 * **The cause was not rule ordering.** `boot-rules.example.json` — the operator *template*, which the daemon
 * never loads unless somebody copies it — carried `lwjgl-on-a-dedicated-server` and `fml-invalid-dist`,
 * while the bundled defaults carried neither. Nothing matched the line at all, so there was nothing to
 * order: it fell through every rung to the bare exit code, which means *"exited non-zero, nothing recognised
 * why"* and cannot confirm. Out of the box, the engine had never caught either signature.
 *
 * `fml-invalid-dist` matters for a second reason: FML prints *"for invalid dist DEDICATED_SERVER"* when it
 * refuses a client-only class, and NeoForge's ServerStarterJar can print that crash in full and still
 * **exit 0** — the exit-code rung then reads it as inconclusive. A rule is what makes the console outrank
 * the status.
 *
 * `third-party-screen-class` stays an example on purpose: its own note says it is "often a dependency
 * problem, not sideness", it states no verdict, and its negative-lookahead pattern is brittle. A default
 * that fires on a dependency's GUI class would publish mods on someone else's crash.
 */
internal class ClientLibraryEvidenceTest {

    private fun classify(vararg consoleLines: String, exitCode: Int? = 1) =
        BootLogClassifier.classify(consoleLines.toList(), exitCode, timedOut = false, rules = ConsoleRuleSet.EMPTY)

    /** The `iris` report, verbatim. */
    @Test
    fun reachingLwjglOnADedicatedServerIsDecisive() {
        val classified = classify("java.lang.NoClassDefFoundError: org/lwjgl/Version")

        Assertions.assertEquals(BootResult.CRASHED, classified.result)
        Assertions.assertEquals(
            BootDecision.LWJGL_ON_A_DEDICATED_SERVER, classified.decidedBy,
            "a dedicated server never ships LWJGL, so this is client-only evidence"
        )
        Assertions.assertTrue(
            classified.decidedBy?.decisive == true,
            "and it has to be decisive, or it still cannot publish"
        )
    }

    /** The dotted spelling too, since `ClassNotFoundException` reports package names rather than paths. */
    @Test
    fun theDottedSpellingCountsAsWell() {
        Assertions.assertEquals(
            BootDecision.LWJGL_ON_A_DEDICATED_SERVER,
            classify("java.lang.ClassNotFoundException: org.lwjgl.opengl.GL11").decidedBy
        )
    }

    /**
     * FML refusing a client-only class is decisive **even on a zero exit**, which is the whole reason it
     * cannot be left to the exit-code rung.
     */
    @Test
    fun fmlRefusingAClientClassIsDecisiveEvenOnAZeroExit() {
        val classified = classify(
            "Failed to load class net.minecraft.client.Minecraft for invalid dist DEDICATED_SERVER",
            exitCode = 0
        )

        Assertions.assertEquals(BootResult.CRASHED, classified.result, "a zero exit must not hide this")
        Assertions.assertTrue(classified.decidedBy?.decisive == true)
    }

    /**
     * **The fair-run guards still outrank both.** If the loader never bootstrapped, whatever the console
     * says afterwards did not come from this mod being exercised — the ordering that keeps host trouble from
     * being published as a mod's fault.
     */
    @Test
    fun aRunThatNeverHappenedStillOutranksClientLibraryEvidence() {
        val classified = classify(
            "Could not find parent layer for module",
            "java.lang.NoClassDefFoundError: org/lwjgl/Version"
        )

        Assertions.assertEquals(
            BootDecision.LOADER_BOOTSTRAP_FAILURE, classified.decidedBy,
            "a mod that never loaded cannot have been proven clientside"
        )
    }

    /** And both outrank the excuses, exactly as `client-only-class` does. */
    @Test
    fun clientLibraryEvidenceOutranksAnExcuse() {
        val classified = classify(
            "Missing or unsupported mandatory dependencies",
            "java.lang.NoClassDefFoundError: org/lwjgl/Version"
        )

        Assertions.assertEquals(
            BootDecision.LWJGL_ON_A_DEDICATED_SERVER, classified.decidedBy,
            "a clientside mod may also be missing a dependency; the decisive marker must still win"
        )
    }

    /** Both signatures are shipped defaults now, not examples an operator has to discover. */
    @Test
    fun bothSignaturesAreBundledDefaults() {
        val ids = DefaultBootRules.bundled().rules.map { it.id }

        Assertions.assertTrue(ids.contains("lwjgl-on-a-dedicated-server"), ids.toString())
        Assertions.assertTrue(ids.contains("fml-invalid-dist"), ids.toString())
    }

    /**
     * A server-side mod that merely *mentions* lwjgl in a log line must not be caught. The pattern is
     * anchored to the two loader-failure spellings, not to the string "lwjgl".
     */
    @Test
    fun merelyNamingLwjglIsNotEvidence() {
        val classified = classify("[Server thread/INFO]: Skipping org.lwjgl compatibility shim, server detected")

        Assertions.assertNotEquals(
            BootDecision.LWJGL_ON_A_DEDICATED_SERVER, classified.decidedBy,
            "only a failure to load the class is evidence, not a mod talking about it"
        )
    }
}
