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
 * Pins what "timed out" means for a boot.
 *
 * Asserted on the decision rather than through a runner, because neither runner can be made to answer this
 * deterministically: the container engine needs a live daemon, the host runner starts its own process, and
 * the case that matters — the budget elapsing *while the thing being waited on has already finished* —
 * cannot be staged without controlling the clock of a running boot. The decision needs neither.
 *
 * What it costs to get wrong is not a mislabelled field. `BootLogClassifier.classify` branches on the
 * timeout second, immediately after the ready line and before every evidence rung it has, and returns
 * INCONCLUSIVE — so a boot wrongly called a timeout throws away its exit code, its crash, and the
 * client-only class marker that is the whole point of the engine.
 *
 * @author Griefed
 */
internal class BootDeadlineTest {

    /**
     * **A run that finished is never a timeout, whatever the clock says.**
     *
     * This is the whole guard. A wait that ends because the container or process is gone has an exit code
     * and a complete console, and both mean exactly what they say — even where the budget elapsed during the
     * setup around the wait, which on a loaded CI host it routinely does.
     */
    @Test
    fun aRunThatFinishedIsNotATimeout() {
        Assertions.assertFalse(
            BootDeadline.timedOut(ready = false, stillRunning = false),
            "a run that is no longer running ended on its own terms; reporting it as a timeout discards its " +
                "exit code and its console"
        )
    }

    /** A run still going when the wait ended, having never announced itself, is the one that was given up on. */
    @Test
    fun aRunStillGoingWhenTheWaitEndedIsATimeout() {
        Assertions.assertTrue(
            BootDeadline.timedOut(ready = false, stillRunning = true),
            "nothing else ends a wait on a running boot that never became ready"
        )
    }

    /** The ready line settles it before anything else is asked, running or not. */
    @Test
    fun aReadyRunIsNeverATimeout() {
        Assertions.assertFalse(
            BootDeadline.timedOut(ready = true, stillRunning = true),
            "a server that announced itself was not given up on"
        )
        Assertions.assertFalse(
            BootDeadline.timedOut(ready = true, stillRunning = false),
            "a server that announced itself and then exited was not given up on either"
        )
    }
}
