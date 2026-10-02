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
 * Every use of this is setup — waiting for a container to be running before a test can ask its real
 * question — so expiry is a statement about the daemon, not about the engine under test, and each wait
 * says so in the message it fails with. Five minutes is therefore deliberately far more than a healthy
 * daemon needs: a loaded CI host must never be able to decide a verdict. It costs nothing when the
 * daemon is responsive, because every wait returns as soon as the container is up.
 *
 * Shared by [ContainerOwnershipIT] and [DockerJavaContainerEngineIT], which wait for the same thing on
 * the same daemon.
 */
internal const val FIXTURE_DAEMON_BUDGET_MILLIS = 300_000L
