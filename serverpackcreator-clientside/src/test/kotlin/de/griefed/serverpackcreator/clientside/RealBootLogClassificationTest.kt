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
 * Five **real** boot logs the deployed grinder published, sampled 2026-08-31 from
 * `grinder.serverpackcreator.de/boot-log`. Four of the five were scored `CRASHED` by the bare exit-code
 * rung — i.e. eligible for a clientside `HIGH` — on no sideness evidence at all, and one of the mods
 * (`create_ltab`) was already published in the live fallback list because of it.
 *
 * These are kept as literal excerpts rather than as fixture files because the *lines* are the evidence and
 * the surrounding hundred lines of Forge banner are not. Every excerpt below is verbatim from the console it
 * names. Two rounds of sample-and-fix had no such regression, which is why the same class shipped twice.
 */
internal class RealBootLogClassificationTest {

    /**
     * `Modrinth-create-let-the-adventure-begin-Quilt~Fabric_0.19.3_mc1.20.6~console.log`
     *
     * The jar's mixins target a method that does not exist in the Minecraft it was booted on — the mod was
     * staged on 1.20.6 because the platform's version node ticked it, not because the jar was built for it.
     * Nothing about the mod's own sideness was exercised.
     */
    @Test
    fun aMixinThatCannotFindItsTargetIsNotSidenessEvidence() {
        val console = listOf(
            "[main/ERROR]: Mixin apply failed create_ltab.mixins.json:CreateLtabModRepairItemRecipeMixin -> " +
                "net.minecraft.class_4317: org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException " +
                "Critical injection failure: @Inject annotation on assemble could not find any targets matching " +
                "'Lnet/minecraft/class_4317;method_20807'",
            "org.spongepowered.asm.mixin.throwables.MixinApplyError: Mixin [create_ltab.mixins.json:" +
                "CreateLtabModRepairItemRecipeMixin from mod create_ltab] from phase [DEFAULT] in config " +
                "[create_ltab.mixins.json] FAILED during APPLY",
            "java.lang.BootstrapMethodError: java.lang.RuntimeException: Mixin transformation of " +
                "net.minecraft.class_4317 failed"
        )

        val decided = BootLogClassifier.classify(console, exitCode = 1, timedOut = false, ConsoleRuleSet.EMPTY)

        Assertions.assertEquals(BootResult.INCONCLUSIVE, decided.result)
        Assertions.assertEquals(BootDecision.MIXIN_APPLY_FAILURE, decided.decidedBy)
    }

    /**
     * `CurseForge-debugify-Forge~Forge_42.0.9_mc1.19.1~console.log`
     *
     * The same class through a different mixin failure. `RangedBowAttackGoal` is a *server* class, so the
     * client-only-class marker correctly does not fire — which is precisely why this fell all the way to the
     * exit code.
     */
    @Test
    fun aShadowFieldMissingFromItsTargetIsNotSidenessEvidence() {
        val console = listOf(
            "org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException: @Shadow field f_25782_ " +
                "was not located in the target class net.minecraft.world.entity.ai.goal.RangedBowAttackGoal",
            "org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError: An unexpected critical " +
                "error was encountered"
        )

        val decided = BootLogClassifier.classify(console, exitCode = 1, timedOut = false, ConsoleRuleSet.EMPTY)

        Assertions.assertEquals(BootResult.INCONCLUSIVE, decided.result)
        Assertions.assertEquals(BootDecision.MIXIN_APPLY_FAILURE, decided.decidedBy)
    }

    /**
     * `Modrinth-create-let-the-adventure-begin-Quilt~Quilt_0.31.0-beta.1_mc1.21.1~console.log`
     *
     * A Quilt dependency-solver failure whose phrasing shares nothing with the Fabric one: no `requires`, no
     * `Incompatible mods found`. `dependencyFailureMarkers`' `requires version .{1,80} of ` was written for a
     * *different* Quilt shape and does not reach this one.
     */
    @Test
    fun theQuiltSolverGivingUpIsNotSidenessEvidence() {
        val console = listOf(
            "---- Quilt Loader: Failed to load ----",
            "Unhandled solver error involving the following rules:",
            "Dependency for {org.quiltmc.loader.impl.plugin.quilt.QuiltModOption 'create_ltab' from " +
                "<mods>/create_ltab-4.1.0.jar} on quilt_resource_loader versions [*] " +
                "(0 valid options, 0 invalid options)",
            "[main/ERROR]: Crashed! The full crash report has been saved to " +
                "./crash-reports/crash-2026-08-31_12.32.57.6476-quilt_loader.txt"
        )

        val decided = BootLogClassifier.classify(console, exitCode = 1, timedOut = false, ConsoleRuleSet.EMPTY)

        Assertions.assertEquals(BootResult.INCONCLUSIVE, decided.result)
        Assertions.assertEquals(BootDecision.LOADER_SOLVER_FAILURE, decided.decidedBy)
    }

    /**
     * `Modrinth-damagevignette-NeoForge~NeoForge_20.4.251_mc1.20.4~console.log`
     *
     * A **Forge** jar staged for a **NeoForge** boot: one platform file claimed both loaders, so the wrong
     * jar was picked. Forge's `javafml` is not NeoForge's, and the jar's bundled MixinExtras collides with
     * NeoForge's own. Neither line says anything about sideness.
     */
    @Test
    fun aJarBuiltForAnotherLoaderIsNotSidenessEvidence() {
        val console = listOf(
            "[main/ERROR] [loading.LanguageLoadingProvider/LOADING]: Missing language javafml version [46,) " +
                "wanted by DamageVignette-2.0.2-forge+mc1.20.jar, found 2.0",
            "java.lang.module.ResolutionException: Modules mixinextras.neoforge and MixinExtras.beta._6 export " +
                "package com.llamalad7.mixinextras.sugar to module minecraft"
        )

        val decided = BootLogClassifier.classify(console, exitCode = 1, timedOut = false, ConsoleRuleSet.EMPTY)

        Assertions.assertEquals(BootResult.INCONCLUSIVE, decided.result)
        Assertions.assertEquals(BootDecision.RUNTIME_MISMATCH, decided.decidedBy)
    }

    /**
     * `CurseForge-debugify-Forge~Fabric_0.19.3_mc26.2~console.log` — the control. This one the ladder
     * **already** classified correctly, and it must keep doing so: if a marker added for the four above
     * started claiming this, the fix would have moved the problem rather than solved it.
     */
    @Test
    fun theFabricDependencyFailureIsStillADependencyFailure() {
        val console = listOf(
            "[main/ERROR]: Mod 'Debugify' (debugify) 26.2.0.0 requires any version of fabric-resource-loader-v0, " +
                "which is missing!",
            "[main/ERROR]: Incompatible mods found!",
            "net.fabricmc.loader.impl.FormattedException: Some of your mods are incompatible with the game or each other!"
        )

        val decided = BootLogClassifier.classify(console, exitCode = 1, timedOut = false, ConsoleRuleSet.EMPTY)

        Assertions.assertEquals(BootResult.INCONCLUSIVE, decided.result)
        Assertions.assertEquals(BootDecision.DEPENDENCY_FAILURE, decided.decidedBy)
    }

    /**
     * **A whole tuple's worth of false positives from one broken cache entry.** Measured 2026-08-31: all 90
     * boots against the cached `NeoForge 21.11.45 / Minecraft 1.21.11` install died on a missing log4j-core,
     * `corgilib` (a library mod) and `chisels-bits` (a building mod that runs on servers) among them, and the
     * ones reaching a non-zero exit were published as clientside.
     *
     * The server is supposed to *have* a logging framework, so this says nothing whatsoever about the mod —
     * which is why it is a classifier marker and **not** an operator rule. A rule would make it decisive
     * evidence, and it is the precise opposite: the absence of evidence.
     */
    @Test
    fun aMissingLoggingFrameworkIsTheHarnessBreaking() {
        val console = listOf(
            "java.lang.NoClassDefFoundError: org/apache/logging/log4j/core/config/ConfigurationSource",
            "Caused by: java.lang.ClassNotFoundException: org.apache.logging.log4j.core.config.ConfigurationSource"
        )

        val decided = BootLogClassifier.classify(console, exitCode = 1, timedOut = false, ConsoleRuleSet.EMPTY)

        Assertions.assertEquals(BootResult.INCONCLUSIVE, decided.result)
        Assertions.assertEquals(BootDecision.RUNTIME_MISMATCH, decided.decidedBy)
    }

    /**
     * **The guard that stops the new markers going too far.** A mod reaching for a client-only class *inside*
     * a mixin is a genuine clientside signal, and the client-only-class marker sits above every new marker
     * for exactly that reason. Get this backwards and the fix silently discards true positives — the
     * expensive direction.
     */
    @Test
    fun aClientOnlyClassInsideAMixinFailureIsStillDecisive() {
        val console = listOf(
            "org.spongepowered.asm.mixin.throwables.MixinApplyError: Mixin [foo.mixins.json:BarMixin] FAILED during APPLY",
            "Caused by: java.lang.NoClassDefFoundError: net/minecraft/client/gui/screens/Screen"
        )

        val decided = BootLogClassifier.classify(console, exitCode = 1, timedOut = false, ConsoleRuleSet.EMPTY)

        Assertions.assertEquals(BootResult.CRASHED, decided.result)
        Assertions.assertEquals(BootDecision.CLIENT_ONLY_CLASS, decided.decidedBy)
        Assertions.assertTrue(decided.decidedBy.decisive)
    }
}
