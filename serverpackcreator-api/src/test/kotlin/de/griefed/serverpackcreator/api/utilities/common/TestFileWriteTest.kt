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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Pins that the writability probe cannot be defeated by what is already in the directory, or by another probe
 * running at the same time.
 *
 * It probed by writing a file called **`poke`** — one fixed name, shared by every caller and every process. That
 * was survivable while the answer only made a GUI file-chooser refuse a directory, and stopped being survivable
 * when `ApiProperties.requireUsableHomeDirectory` put the probe on the construction path behind a `throw`: a
 * false "not writable" is now a fatal startup error naming a home that is perfectly fine. Two ways to get one:
 * something in the directory already holds the name, or a second probe deletes this one's file between the write
 * and the check. Concurrent SPC processes sharing a home is the documented normal condition here — the grinder
 * daemon, a test suite and the developer's GUI — and the grinder alone probes twice per start.
 */
internal class TestFileWriteTest {

    /**
     * Deterministic half: a directory holding something that occupies the probe's name is still writable.
     *
     * With a fixed name this is the same defect as the race, without the timing — `writeText` onto a directory
     * throws, the probe swallows it and reports "not writable" for a directory it never actually tried to write.
     */
    @Test
    fun aDirectoryIsWritableEvenWhenSomethingHoldsTheProbesName(@TempDir dir: File) {
        File(dir, "poke").mkdirs()

        Assertions.assertTrue(
            dir.testFileWrite(),
            "a writable directory must not be reported unwritable because of what it happens to contain — the " +
                "probe has to pick a name nothing else can hold"
        )
        Assertions.assertTrue(File(dir, "poke").isDirectory, "the probe must not have touched existing content")
    }

    /** The probe leaves nothing behind, whatever it is called. */
    @Test
    fun theProbeCleansUpAfterItself(@TempDir dir: File) {
        Assertions.assertTrue(dir.testFileWrite())

        Assertions.assertArrayEquals(
            emptyArray<String>(),
            dir.list(),
            "the probe file must be gone: a leftover is both litter in the user's home and, with a fixed name, " +
                "the thing that makes the next probe fail"
        )
    }

    /**
     * Concurrency half: many probes on one directory at once must all say "writable".
     *
     * This guard cannot false-fail once the probe uses a per-call name — there is no shared state left to lose a
     * race over — which is what makes it safe to assert on. Against the fixed-name version it fails with high
     * probability rather than certainly, so the deterministic guard above is the one that pins the defect; this
     * one pins the property an operator actually depends on.
     */
    @Test
    fun concurrentProbesOnOneDirectoryAllSucceed(@TempDir dir: File) {
        val probes = 64
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)

        try {
            val results = (1..probes).map {
                pool.submit<Boolean> {
                    start.await()
                    dir.testFileWrite()
                }
            }
            start.countDown()

            val failed = results.count { !it.get(30, TimeUnit.SECONDS) }
            Assertions.assertEquals(
                0,
                failed,
                "$failed of $probes concurrent probes reported a writable directory as unwritable — with " +
                    "ApiProperties throwing on that answer, each one is a process that refuses to start"
            )
        } finally {
            pool.shutdownNow()
        }
    }
}
