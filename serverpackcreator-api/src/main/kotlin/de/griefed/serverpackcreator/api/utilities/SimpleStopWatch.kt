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
package de.griefed.serverpackcreator.api.utilities

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Minimalistic stopwatch to measure the elapsed time between operations.
 *
 * This stopwatch does `not` support multi-threading. When implementing this stopwatch, make sure to take care of this
 * properly, otherwise inconsistency and unexpected errors will be a problem for you.
 * This stopwatch uses [Clock.System.now] to retrieve both the start-time and stop-time. A regular run expects you to
 * call [start], followed by [stop], and then either of [getTime] or [toString] to retrieve the
 * elapsed time as a formatted string.

 * @author Griefed
 */
class SimpleStopWatch {
    @Suppress("MemberVisibilityCanBePrivate")
    var startTime: Instant = Clock.System.now()
        private set

    @Suppress("MemberVisibilityCanBePrivate")
    var stopTime: Instant = Clock.System.now()
        private set

    @Suppress("MemberVisibilityCanBePrivate")
    var elapsedTime: Duration = stopTime - startTime
        private set

    @Suppress("MemberVisibilityCanBePrivate")
    var started = false
        private set

    @Suppress("MemberVisibilityCanBePrivate")
    var stopped = false
        private set

    /**
     * Starts the stopwatch.
     *
     * After having started the stopwatch, call [.stop], followed by any of
     *
     *  * [.getTime]
     *  * [.getTime]
     *  * [.toString]
     *
     * to retrieve the elapsed time.
     *
     * @return This instance of SimpleStopWatch
     * @author Griefed
     */
    fun start(): SimpleStopWatch {
        startTime = Clock.System.now()
        started = true
        stopped = false
        return this
    }

    /**
     * Stops the stopwatch.
     *
     * The stopwatch must have been started before calling this method, otherwise an
     * [IllegalStateException] will be thrown. Call [.start] first!
     *
     * @return This instance of SimpleStopWatch
     * @throws IllegalStateException if the watch was not started.
     * @author Griefed
     */
    @Throws(IllegalStateException::class)
    fun stop(): SimpleStopWatch {
        check(started) { "Stopwatch not started." }
        stopTime = Clock.System.now()
        elapsedTime = stopTime - startTime
        started = false
        stopped = true
        return this
    }

    /**
     * Get the elapsed time of this stopwatch, formatted using `%2dh:%02dm:%02ds:%04dms`.
     *
     * The stopwatch must be started and stopped before calling this method, [.getTime], or
     * [.getTime]. If any of the aforementioned methods are called, but the stopwatch
     * was not started and stopped yet first, an [IllegalStateException] will be thrown.
     *
     * @return The elapsed time of a stopwatch run, formatted using `%2d:%02d:%02d`.
     * @throws IllegalStateException if the watch was not started.
     * @author Griefed
     */
    @Throws(IllegalStateException::class)
    override fun toString() = getTime()

    /**
     * Duration value expressed in the given [unit] and formatted with the specified [decimals]-amount of digits
     * after the decimal point.
     *
     * The stopwatch must be started and stopped before calling this method or
     * [toString]. If any of the aforementioned methods are called, but the stopwatch was not
     * started and stopped yet first, an [IllegalStateException] will be thrown.
     *
     * @param unit Unit to use in duration representation.
     * @param decimals The number of digits after decimal point to show. The value must be non-negative.
     * No more than 12 decimals will be shown, even if a larger number is requested.
     * @return The elapsed time, formatted with the provided formatting.
     * @throws IllegalStateException if the watch was not started.
     * @author Griefed
     */
    @Throws(IllegalStateException::class)
    fun getTime(unit: DurationUnit = DurationUnit.SECONDS, decimals: Int = 2): String {
        return elapsed().toString(unit, decimals)
    }

    /**
     * Get the elapsed time in nanoseconds. Depending on the state of the stopwatch, two values or an
     * [IllegalStateException] may be thrown.
     *
     *  1. Stopwatch is still running: The elapsed time up to the point of calling this method is returned.
     *  1. Stopwatch started and stopped: The elapsed time between the start and stop is returned.
     *  1. Any other state: An [IllegalStateException] is thrown.
     *
     * @return The elapsed time in nanoseconds.
     * @throws IllegalStateException when the stopwatch is in an invalid state for time-retrieval.
     * @author Griefed
     */
    @Throws(IllegalStateException::class)
    fun elapsed() =
        if (started && !stopped) {
            Clock.System.now() - startTime
        } else if (!started && stopped) {
            elapsedTime
        } else {
            throw IllegalStateException("Stopwatch is not running, or has not been run yet.")
        }
}