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
package de.griefed.serverpackcreator.grinder.container

/**
 * How long a container *fixture* may wait for the daemon before it gives up, in milliseconds.
 *
 * Not an assertion about the engine — every use of this is setup, waiting for a sleeper to exist before
 * the test can ask its real question, and each one says so in the message it fails with. A fixture that
 * expires is reporting on the daemon, which is why the number belongs here and not inside a test: it
 * must be generous enough that a loaded host never decides a verdict.
 *
 * Five minutes, and the history is the argument for it. This started at 60 s, failed Forgejo run 716,
 * and was raised to 90 s against that run's measurements — then failed runs 831 and 834 on 2026-10-01,
 * where four of the six failures were fixtures expiring. The host is what changed, not the engine: that
 * push queued nine runs at once, and every workflow in the batch ran 2–5x its usual time (docker-test
 * 13–15 min -> 41.6 min, devbuild ~20 min -> 96.6 min, this job 5.3–8.8 min -> 18.1 and 23.3 min). A
 * budget tuned to the last slow run is a budget that will be re-tuned after the next one, so this one is
 * sized past anything plausible instead. It costs nothing when the daemon is healthy — every wait exits
 * as soon as the container is running — and the price of the pathological case is one slow red build,
 * against the current price of a red build that says nothing.
 *
 * Shared because [ContainerOwnershipIT] and [DockerJavaContainerEngineIT] wait for the same thing on the
 * same daemon for the same reason, and two copies of a number like this drift apart on the first run
 * that only fails one of them.
 */
internal const val FIXTURE_DAEMON_BUDGET_MILLIS = 300_000L
