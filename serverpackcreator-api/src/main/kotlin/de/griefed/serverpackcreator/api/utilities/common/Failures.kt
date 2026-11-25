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
package de.griefed.serverpackcreator.api.utilities.common

import org.apache.logging.log4j.Logger
import kotlin.coroutines.cancellation.CancellationException

/**
 * [block]'s result, or `null` with [message] and the failure logged at WARN to [log] when it throws an
 * [Exception] — for a value whose absence the caller can answer without, but whose loss must not go unseen.
 *
 * Only an [Exception] is turned into `null`: an [Error] and a [CancellationException] propagate, and an
 * [InterruptedException] re-asserts the thread's interrupt status, which throwing it had cleared.
 */
inline fun <T> orNullLogged(log: Logger, message: String, block: () -> T): T? = try {
    block()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (ex: Exception) {
    if (ex is InterruptedException) {
        Thread.currentThread().interrupt()
    }
    log.warn(message, ex)
    null
}
