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
 * The one thing worth checking before a grinder takes its first candidate: that the image every boot runs in
 * actually exists. Pure — it asks the [ContainerEngine] and formats the refusal — so the policy is testable
 * without a daemon, which the engine that answers the question is not.
 *
 * @author Griefed
 */
object RuntimeImagePreflight {

    /**
     * Why the daemon must not start, or `null` when [image] is available to [engine].
     *
     * **Why a refusal rather than a warning.** Without the image every loader install throws, so every tuple
     * goes on install cooldown and every candidate that wants one is scored INCONCLUSIVE — a verdict about a
     * mod that was never booted. Those verdicts replace whatever the store held, including decisive HIGH ones,
     * and the re-verify TTL then leaves them standing for 30 days. Grinding on is not degraded service; it is
     * the destruction of the record the daemon exists to keep.
     */
    fun refusalFor(engine: ContainerEngine, image: String): String? {
        if (engine.hasImage(image)) {
            return null
        }
        return "Runtime image '$image' is not available on the container daemon, so no mod could be booted " +
            "and every candidate would be scored INCONCLUSIVE about a boot that never happened. Refusing to " +
            "grind. Either the image was removed (a `docker system prune -a` does it — the image is only in " +
            "use during a boot) or the daemon is unreachable. Rebuild it with " +
            "`docker build --pull -t $image <checkout>/serverpackcreator-grinder/docker`, or re-run " +
            "`install-grinder.sh`, then start the service again."
    }
}
