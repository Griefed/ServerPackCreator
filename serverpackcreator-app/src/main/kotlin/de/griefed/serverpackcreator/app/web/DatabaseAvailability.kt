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
package de.griefed.serverpackcreator.app.web

import com.mongodb.MongoSocketException
import com.mongodb.MongoTimeoutException

/**
 * Tells "the database is not reachable" apart from "something actually went wrong".
 *
 * Both of the application's `ApplicationReadyEvent` listeners — the declared-index creator and the
 * run-configuration migration — catch broadly and carry on, because an unreachable database must never stop
 * the application from starting. That is right. What was wrong is that both then logged the *full stack
 * trace* for the one case they were written to tolerate: `docker/docker-compose.yml` starts the app
 * alongside its `db` service, so losing that race is the normal first boot, and it produced two stack
 * traces every time.
 *
 * Defined by the **cause**, not by the consequence. A category defined by its consequence — "the listener
 * failed" — accumulates everything with that consequence, including the malformed index definition or the
 * half-migrated document that genuinely needs a stack trace. Only the connectivity family is quietened:
 * anything else keeps exactly the logging it had.
 *
 * @author Griefed
 */
object DatabaseAvailability {

    /**
     * How deep the cause chain is walked before giving up.
     *
     * A bound rather than a visited-set because it also terminates a self-referencing cause, which
     * `Throwable` permits and which would otherwise spin here forever. Spring wraps the driver's exception
     * once and the driver wraps the socket error once, so real chains are two or three deep.
     */
    private const val MAX_CAUSE_DEPTH = 16

    /**
     * Whether [throwable] — or anything it wraps — is the driver failing to reach a server.
     *
     * Matched on the driver's own types rather than on Spring's `DataAccessResourceFailureException`:
     * Spring's translation is what the caller happens to receive today, while
     * [MongoTimeoutException] (no server matched the selector in time) and [MongoSocketException]
     * (the connection itself could not be opened) are what the condition actually *is*.
     */
    fun isUnreachable(throwable: Throwable?): Boolean {
        var cause = throwable
        var depth = 0
        while (cause != null && depth < MAX_CAUSE_DEPTH) {
            if (cause is MongoTimeoutException || cause is MongoSocketException) {
                return true
            }
            if (cause.cause === cause) {
                return false
            }
            cause = cause.cause
            depth++
        }
        return false
    }
}
