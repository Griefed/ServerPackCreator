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
package de.griefed.serverpackcreator.plugin.servertest.core

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Collections

/**
 * Pins the rendezvous between the extension that hears about a generation and the tab that shows the list.
 *
 * A singleton with a subscriber list is the sort of thing that silently keeps every subscriber it was ever
 * given, so what is pinned here is mostly the *unsubscribing*: a tab that has gone away must stop being
 * delivered to, and a subscriber that throws must not cost another its refresh — this publishes from inside
 * somebody's generation.
 */
internal class GenerationNotifierTest {

    private val handles = mutableListOf<Subscription>()

    /** Cancel everything this test subscribed, so a shared singleton does not leak across the suite. */
    @AfterEach
    fun cancelEverything() {
        handles.forEach { it.cancel() }
        handles.clear()
        Assertions.assertEquals(0, GenerationNotifier.subscriberCount, "A test leaked a subscriber.")
    }

    private fun subscribe(onGenerated: (File) -> Unit) =
        GenerationNotifier.subscribe(onGenerated).also { handles.add(it) }

    /** A subscriber is told which pack was generated, not merely that one was. */
    @Test
    fun tellsASubscriberWhichPackWasGenerated() {
        val seen = Collections.synchronizedList(mutableListOf<File>())
        subscribe { seen.add(it) }

        GenerationNotifier.packGenerated(File("/packs/Freshly-Made"))

        Assertions.assertEquals(listOf(File("/packs/Freshly-Made")), seen)
    }

    /** Every subscriber hears about it, not just the first one registered. */
    @Test
    fun tellsEverySubscriber() {
        var first = 0
        var second = 0
        subscribe { first++ }
        subscribe { second++ }

        GenerationNotifier.packGenerated(File("/packs/Any"))

        Assertions.assertEquals(1, first)
        Assertions.assertEquals(1, second)
    }

    /**
     * A cancelled subscriber is not told again.
     *
     * The one that matters: the tab cancels when it leaves the window, and a notifier that kept delivering
     * would hold a dead tab alive and refresh a list nobody can see.
     */
    @Test
    fun stopsTellingACancelledSubscriber() {
        var told = 0
        val handle = subscribe { told++ }

        GenerationNotifier.packGenerated(File("/packs/Before"))
        handle.cancel()
        GenerationNotifier.packGenerated(File("/packs/After"))

        Assertions.assertEquals(1, told, "A cancelled subscription must not be delivered to.")
        Assertions.assertEquals(0, GenerationNotifier.subscriberCount)
    }

    /** Cancelling twice is harmless, so a caller need not track whether it already did. */
    @Test
    fun cancellingTwiceIsHarmless() {
        val handle = subscribe { }

        handle.cancel()
        handle.cancel()

        Assertions.assertEquals(0, GenerationNotifier.subscriberCount)
    }

    /**
     * One subscriber throwing must not cost another its refresh, and must not escape.
     *
     * This runs inside `ServerPackHandler.run`. `ApiPlugins` would catch an escape and log it, but the
     * generation would still have lost whatever ran after the throw.
     */
    @Test
    fun aThrowingSubscriberCostsNobodyElseTheirRefresh() {
        var reached = 0
        subscribe { throw IllegalStateException("a subscriber misbehaving") }
        subscribe { reached++ }

        Assertions.assertDoesNotThrow { GenerationNotifier.packGenerated(File("/packs/Any")) }
        Assertions.assertEquals(1, reached, "The second subscriber must still have been told.")
    }

    /** Publishing with nobody listening is what happens in CLI and web mode, and must be a no-op. */
    @Test
    fun publishingWithNoSubscribersIsHarmless() {
        Assertions.assertDoesNotThrow { GenerationNotifier.packGenerated(File("/packs/Any")) }
    }
}
