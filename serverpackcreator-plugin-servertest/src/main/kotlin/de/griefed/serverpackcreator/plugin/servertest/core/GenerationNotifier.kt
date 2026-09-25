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

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A handle on a subscription, cancelled when the subscriber no longer wants to be told.
 *
 * Returned rather than having callers pass their lambda back, because a lambda has no useful identity —
 * removing "the same" one is not something a caller can reliably ask for.
 */
fun interface Subscription {
    /** Stop delivering to this subscriber. Cancelling an already-cancelled subscription does nothing. */
    fun cancel()
}

/**
 * Tells the tab that ServerPackCreator has just finished generating a server pack.
 *
 * It exists because the two halves cannot reach each other any other way. `ApiPlugins` builds each
 * extension through a `SingletonExtensionFactory`, so the `PostGenExtension` that hears about a generation
 * and the `TabExtension` that owns the list are separate objects with no reference between them, and the
 * shared `CommentedConfig` carries configuration rather than events.
 *
 * **Plugin-scoped, not JVM-global**, despite being an `object`: pf4j loads each plugin in its own
 * classloader, so this singleton is one per plugin instance rather than one per application.
 *
 * Publishing happens on whichever thread ran the generation — the GUI's single-threaded generation
 * dispatcher, a CLI thread, or a web request — never the event dispatch thread. Subscribers marshal for
 * themselves.
 *
 * @author Griefed
 */
object GenerationNotifier {

    /**
     * Copy-on-write because subscribing happens on the event dispatch thread while publishing happens on a
     * generation thread, and the two genuinely overlap: a generation can finish while the user is switching
     * tabs. Iteration over a snapshot also means a subscriber cancelling mid-delivery cannot disturb it.
     */
    private val subscribers = CopyOnWriteArrayList<(File) -> Unit>()

    /** How many subscribers are listening, for guards that need to prove a cancel actually took effect. */
    val subscriberCount: Int get() = subscribers.size

    /**
     * Be told about every server pack generated from now on, until the returned [Subscription] is cancelled.
     *
     * @param onPackGenerated Handed the generated pack's directory.
     */
    fun subscribe(onPackGenerated: (File) -> Unit): Subscription {
        subscribers.add(onPackGenerated)
        // Removal by identity, which is why the handle exists: two lambdas that behave alike are not equal,
        // so a caller could never ask for "the one I registered" by passing it back.
        return Subscription { subscribers.remove(onPackGenerated) }
    }

    /**
     * Tell every subscriber that [serverPack] has been generated.
     *
     * A subscriber that throws is logged past rather than allowed to stop the others — this runs inside a
     * generation, and one misbehaving listener must not cost another its refresh.
     */
    fun packGenerated(serverPack: File) {
        for (subscriber in subscribers) {
            runCatching { subscriber(serverPack) }
                .onFailure { log.error("A server-pack-generated subscriber failed.", it) }
        }
    }

    /** Logged through the name ServerPackCreator routes to `plugins.log`. */
    private val log: Logger = LogManager.getLogger("AddonsLogger")
}
