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

import com.mongodb.MongoSocketOpenException
import com.mongodb.MongoTimeoutException
import com.mongodb.ServerAddress
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessResourceFailureException
import java.net.ConnectException

/**
 * Pins which failures count as "the database is not reachable", because that decides which ones get a
 * stack trace in the log and which get one quiet line.
 *
 * The distinction is the point. Both `ApplicationReadyEvent` listeners tolerate an unreachable database by
 * design — docker-compose starts the app alongside its `db` service, so losing that race is the normal
 * first boot — and both used to log a full stack trace for it. Quietening the *consequence* ("the listener
 * failed") would have swallowed the malformed index definition too; only the cause is quietened.
 */
internal class DatabaseAvailabilityTest {

    private val address = ServerAddress("localhost", 27017)

    /** No server matched the selector in time — what an absent database produces once selection expires. */
    @Test
    fun aServerSelectionTimeoutIsUnreachable() {
        Assertions.assertTrue(
            DatabaseAvailability.isUnreachable(MongoTimeoutException("Timed out while waiting for a server"))
        )
    }

    /** The socket could not be opened at all — what an absent database produces immediately. */
    @Test
    fun aRefusedSocketIsUnreachable() {
        val refused = MongoSocketOpenException("Exception opening socket", address, ConnectException("Connection refused"))

        Assertions.assertTrue(DatabaseAvailability.isUnreachable(refused))
    }

    /**
     * Spring wraps the driver's exception before either listener sees it, so the real input is a
     * `DataAccessResourceFailureException` with the driver's type underneath.
     */
    @Test
    fun theSpringTranslatedFormIsUnreachable() {
        val wrapped = DataAccessResourceFailureException(
            "Timed out while waiting for a server",
            MongoTimeoutException("Timed out while waiting for a server")
        )

        Assertions.assertTrue(DatabaseAvailability.isUnreachable(wrapped))
    }

    /**
     * Anything else is NOT unreachable and keeps its stack trace. This is the guard that stops the fix
     * becoming a blanket silencer: a malformed index definition or a half-migrated document still shouts.
     */
    @Test
    fun anOrdinaryFailureIsNotUnreachable() {
        Assertions.assertFalse(DatabaseAvailability.isUnreachable(IllegalStateException("index definition is wrong")))
        Assertions.assertFalse(
            DatabaseAvailability.isUnreachable(
                DataAccessResourceFailureException("something else", IllegalStateException("not connectivity"))
            )
        )
        Assertions.assertFalse(DatabaseAvailability.isUnreachable(null))
    }

    /** A self-referencing cause must terminate rather than spin, which `Throwable` permits. */
    @Test
    fun aSelfReferencingCauseTerminates() {
        val looping = object : RuntimeException("loops") {
            override val cause: Throwable get() = this
        }

        Assertions.assertFalse(DatabaseAvailability.isUnreachable(looping))
    }
}
