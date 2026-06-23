/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.app.gui.utilities

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

/**
 * A [CoroutineScope] whose lifetime a Swing component owns, replacing fire-and-forget
 * `GlobalScope.launch` so that no coroutine outlives the component that started it. The component
 * holds one instance, launches its UI coroutines via [scope] (each call still picks its own
 * dispatcher and [kotlinx.coroutines.CoroutineStart], exactly as before), and calls [cancel] from
 * its `removeNotify()` so that closing or disposing the component stops its in-flight work.
 *
 * [scope] lazily re-creates itself after a [cancel], so a component that is detached and later
 * re-attached keeps working. Access is `@Synchronized` because launches may be started off the
 * Event-Dispatch-Thread (e.g. from a background dispatcher), so the lazy re-create must not race.
 *
 * A [SupervisorJob] backs each scope so one failed child coroutine does not cancel its siblings —
 * matching the independence the previous per-call `GlobalScope.launch`es had.
 */
class ComponentCoroutineScope {

    private var coroutineScope = CoroutineScope(SupervisorJob())

    /**
     * The scope to launch coroutines on. Re-creates a fresh scope when the previous one was
     * cancelled, so the owning component stays usable across a detach/re-attach cycle.
     */
    @Synchronized
    fun scope(): CoroutineScope {
        if (!coroutineScope.isActive) {
            coroutineScope = CoroutineScope(SupervisorJob())
        }
        return coroutineScope
    }

    /**
     * Cancel every coroutine started on the current scope. Call this from the owning component's
     * `removeNotify()` so that in-flight UI work stops when the component leaves the screen.
     */
    @Synchronized
    fun cancel() {
        coroutineScope.cancel()
    }
}
