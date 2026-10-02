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

/**
 * Whether a boot ended because its budget ran out, as opposed to ending on its own terms.
 *
 * One home for a decision two runners make identically — [ServerRunner]'s host process and the grinder's
 * container engine — and a decision worth stating once, because it is easy to write as a question about the
 * clock when it is really a question about the run. A wall-clock reading answers *"has this much time
 * passed?"*, which on a loaded host is true of a run that finished long ago; `timedOut` has to answer
 * *"did this run have to be given up on?"*.
 *
 * Pure, so it is pinned without a Docker daemon or a real server pack, the same reason `SuspendAwareDeadline`
 * and the shutdown-wait budget live apart from the engines that use them.
 *
 * @author Griefed
 */
object BootDeadline {

    /**
     * Whether a wait that has just ended should be reported as a timeout.
     *
     * Asked of the state the wait ended in, never of the clock: a wait ends for exactly three reasons — the
     * ready line appeared, the thing being waited on finished, or the budget was spent — and only the third
     * is a timeout. So a run that is **still running** and never became **ready** is one that had to be given
     * up on, and anything else is a run that ended by itself and whose exit code and console mean what they
     * say.
     *
     * Reading the clock here instead misreports the second case as the third whenever the setup around the
     * wait outlasts the budget, which costs the console all of its evidence: `BootLogClassifier` branches on
     * a timeout before it looks at a single line, and returns INCONCLUSIVE.
     *
     * @param ready Whether the console produced the ready line.
     * @param stillRunning Whether the process or container was still running when the wait ended.
     */
    fun timedOut(ready: Boolean, stillRunning: Boolean): Boolean = !ready && stillRunning
}
