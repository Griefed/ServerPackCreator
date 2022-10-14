/* Copyright (C) 2022  Griefed
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
package de.griefed.serverpackcreator.utilities;

import org.jetbrains.annotations.NotNull;

/**
 * Minimalistic stopwatch to measure the elapsed time between operations.
 * <p>
 * This stopwatch uses {@link System#nanoTime()} to retrieve both the start-time and stop-time. A
 * regular run expects you to call {@link #start()}, followed by {@link #stop()}, and then either of
 * {@link #getTime()}, {@link #getTime(String)}, or {@link #toString()} to retrieve the elapsed time
 * as a formatted string.
 * <p>
 * Hours are calculated in a 24h-format: {@code elapsed time / 1.000.000.000 / 3600 % 24}
 * <p>
 * Minutes are calculated: {@code elapsed time / 1.000.000.000 / 60 % 60}
 * <p>
 * Seconds are calculated: {@code elapsed time / 1.000.000.000 % 60}
 * <p>
 * Milliseconds are calculated: {@code elapsed time / 1.000.000}
 *
 * @author Griefed
 */
public class SimpleStopWatch {

  long start;
  long stop;
  long elapsed;
  boolean started = false;
  boolean stopped = false;

  /**
   * Starts the stopwatch.
   * <p>
   * After having started the stopwatch, call {@link #stop()}, followed by any of
   * <ul>
   *   <li>{@link #getTime()}</li>
   *   <li>{@link #getTime(String)}</li>
   *   <li>{@link #toString()}</li>
   * </ul>
   * to retrieve the elapsed time.
   * @return This instance of {@link SimpleStopWatch}
   * @author Griefed
   */
  public @NotNull SimpleStopWatch start() {
    start = System.nanoTime();
    started = true;
    stopped = false;
    return this;
  }

  /**
   * Stops the stopwatch.
   * <p>
   * The stopwatch must have been started before calling this method, otherwise an
   * {@link IllegalStateException} will be thrown. Call {@link #start()} first!
   *
   * @return This instance of {@link SimpleStopWatch}
   * @throws IllegalStateException if the stopwatch was stopped without having been started first.
   * @author Griefed
   */
  public @NotNull SimpleStopWatch stop() throws IllegalStateException {
    if (!started) {
      throw new IllegalStateException("Stopwatch not started.");
    }
    stop = System.nanoTime();
    elapsed = stop - start;
    started = false;
    stopped = true;
    return this;
  }

  /**
   * Get the elapsed time of this stopwatch, formatted using {@code %2dh:%02dm:%02ds:%04dms}.
   * <p>
   * The stopwatch must be started and stopped before calling this method, {@link #getTime()}, or
   * {@link #getTime(String)}. If any of the aforementioned methods are called, but the stopwatch
   * was not started and stopped yet first, an {@link IllegalStateException} will be thrown.
   *
   * @return The elapsed time of a stopwatch run, formatted using {@code %2d:%02d:%02d}.
   * @throws IllegalStateException if the stopwatch was started, but not stopped yet.
   * @author Griefed
   */
  @Override
  public @NotNull String toString() throws IllegalStateException {
    return getTime();
  }

  /**
   * Get the elapsed time of this stopwatch, formatted using {@code %2dh:%02dm:%02ds:%04dms}.
   * <p>
   * The stopwatch must be started and stopped before calling this method, {@link #getTime(String)},
   * or {@link #toString()}. If any of the aforementioned methods are called, but the stopwatch was
   * not started and stopped yet first, an {@link IllegalStateException} will be thrown.
   *
   * @return The elapsed time of a stopwatch run, formatted using {@code %2d:%02d:%02d}.
   * @throws IllegalStateException if the stopwatch was started, but not stopped yet.
   * @author Griefed
   */
  public @NotNull String getTime() throws IllegalStateException {
    return getTime("%2dh:%02dm:%02ds:%04dms");
  }

  /**
   * Get the elapsed time of this stopwatch using a custom formatting.
   * <p>
   * The stopwatch must be started and stopped before calling this method, {@link #getTime()}, or
   * {@link #toString()}. If any of the aforementioned methods are called, but the stopwatch was not
   * started and stopped yet first, an {@link IllegalStateException} will be thrown.
   *
   * @param format Formatting to apply to the elapsed {@code hours}, {@code minutes} and
   *               {@code seconds}.
   * @return The elapsed time, formatted with the provided formatting.
   * @throws IllegalStateException if the stopwatch was started, but not stopped yet.
   * @author Griefed
   */
  public @NotNull String getTime(@NotNull String format) throws IllegalStateException {
    long elapsedTime = getElapsedNanoseconds();
    int hours = (int) (elapsedTime / 1000000000.0D / 3600.0D % 24.0D);
    int minutes = (int) (elapsedTime / 1000000000.0D / 60.0D % 60.0D);
    int seconds = (int) (elapsedTime / 1000000000.0D % 60.0D);
    int milliseconds = (int) (elapsedTime / 1000000.0D);
    return String.format(format, hours, minutes, seconds, milliseconds);

  }

  /**
   * Get the elapsed time in nanoseconds. Depending on the state of the stopwatch, two values or an
   * {@link IllegalStateException} may be thrown.
   * <ol>
   *   <li>Stopwatch is still running: The elapsed time up to the point of calling this method is returned.</li>
   *   <li>Stopwatch started and stopped: The elapsed time between the start and stop is returned.</li>
   *   <li>Any other state: An {@link IllegalStateException} is thrown.</li>
   * </ol>
   *
   * @return The elapsed time in nanoseconds.
   * @throws IllegalStateException when the stopwatch is in an invalid state for time-retrieval.
   * @author Griefed
   */
  public long getElapsedNanoseconds() throws IllegalStateException {
    if (started && !stopped) {
      return System.nanoTime() - start;
    } else if (!started && stopped) {
      return elapsed;
    } else {
      throw new IllegalStateException("Stopwatch is not running, or has not been run yet.");
    }
  }
}
