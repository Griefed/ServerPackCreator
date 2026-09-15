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
 * Pins that every rung of the ladder actually finds its pattern in `boot-rules.default.json`.
 *
 * `BootLogClassifier` holds the ladder's *order* in code and looks each rung's *pattern* up in the bundled
 * file by id. When an id does not resolve, `bundledPattern` produced `Regex("(?!)")` — a regex that matches
 * nothing — with no log and no error anywhere. **That is a silently disabled rung.**
 *
 * The consequences depend on which rung goes, and both directions are bad:
 *
 *  - lose `client-only-class`, `lwjgl-on-a-dedicated-server` or `fml-invalid-dist` and every true positive
 *    silently falls through to the bare exit-code rung, which is not decisive, so **nothing is ever
 *    published again** and the engine looks like it simply found nothing.
 *  - lose a fair-run guard such as `out-of-memory` or `launch-failure` and host trouble stops being excused,
 *    so a starved box publishes its biggest mods as clientside — the failure this engine already has on
 *    record.
 *
 * Nothing guarded it. The file is shipped in our own jar, so a rename there is a packaging bug and belongs
 * to the build; a missing id must not be a runtime surprise found weeks later in a verdict store.
 *
 * A bundled file that cannot be read at all is a **separate, deliberate** behaviour — `DefaultBootRules`
 * degrades to an empty set so the classifier keeps its structural readings — and is not what this pins.
 */
internal class BundledRuleIdsResolveTest {

    /** Force the classifier's patterns to be built, which is when the lookups happen. */
    private fun initialiseClassifier() {
        BootLogClassifier.classify(listOf("nothing in particular"), exitCode = 0, timedOut = false)
    }

    /** **The guard.** Every id the ladder asks for exists in the shipped file. */
    @Test
    fun everyRungFindsItsBundledPattern() {
        initialiseClassifier()

        Assertions.assertEquals(
            emptySet<String>(), BootLogClassifier.missingRuleIds(),
            "a rung whose id does not resolve matches nothing, silently — see this class's comment"
        )
    }

    /** And the detection has teeth: an id that does not exist is recorded rather than shrugged off. */
    @Test
    fun anUnknownIdIsRecordedRatherThanSilentlyIgnored() {
        val pattern = BootLogClassifier.bundledPattern("no-such-rule-id")

        Assertions.assertTrue(
            "no-such-rule-id" in BootLogClassifier.missingRuleIds(),
            "an unresolved id must be visible, or the guard above passes by construction"
        )
        Assertions.assertFalse(
            pattern.containsMatchIn("no-such-rule-id"),
            "the fallback must still match nothing — a rung that cannot find its pattern must not match everything"
        )
    }

    /** The ids are looked up against the same set the rest of the engine reads, not a copy. */
    @Test
    fun theBundledSetIsNonEmptyAndCarriesTheDecisiveRungs() {
        val ids = DefaultBootRules.bundled().rules.map { it.id }.toSet()

        listOf("client-only-class", "lwjgl-on-a-dedicated-server", "fml-invalid-dist").forEach {
            Assertions.assertTrue(it in ids, "the decisive rung '$it' must exist in the shipped file: $ids")
        }
    }
}
