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
package de.griefed.serverpackcreator.app.web.integration

import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.Mongod
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess
import de.flapdoodle.reverse.TransitionWalker
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Skips a test class when this machine cannot run `mongod` at all, instead of failing it.
 *
 * The tests these guard start a **real** MongoDB in-process, which is the only way to answer questions
 * about what the database actually does — an index that exists, a migration that converts a document,
 * a legacy row that still reads back. It is also the one part of the suite that depends on something
 * outside the JVM, and that dependency is known to be fragile: MongoDB refuses to start on Linux
 * kernels 6.19 and newer ([SERVER-121912](https://jira.mongodb.org/browse/SERVER-121912)), which is
 * exactly why a containerised database was unusable here and flapdoodle is used instead. CI runs on
 * `ubuntu-latest`, whose kernel is not ours to pin.
 *
 * A runner that cannot start `mongod` therefore reports **skipped**, naming the reason, rather than a
 * red build about something unrelated to the change under test. It is a deliberate trade and worth
 * saying out loud: a skipped guard proves nothing, so a CI run that skips these is a CI run with no
 * database coverage, and the skip message is the only thing that will tell you.
 *
 * The probe starts and immediately stops one `mongod`, memoised for the life of the JVM, so the cost is
 * paid once per test run rather than once per class.
 */
class EmbeddedMongoAvailable : ExecutionCondition {

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult =
        probe.fold(
            onSuccess = { ConditionEvaluationResult.enabled("mongod starts on this machine") },
            onFailure = {
                ConditionEvaluationResult.disabled(
                    "SKIPPED, NOT PASSED: mongod could not be started here, so nothing in this class " +
                            "verified anything against a database. Cause: ${it::class.simpleName}: ${it.message}"
                )
            }
        )

    private companion object {
        /** The version these tests pin against; the same line `docker/docker-compose.yml` deploys. */
        const val MONGOD_VERSION = "8.0.5"

        /** One start-and-stop per JVM. `lazy` is what makes it once rather than once per class. */
        val probe: Result<Unit> by lazy {
            runCatching {
                var running: TransitionWalker.ReachedState<RunningMongodProcess>? = null
                try {
                    running = Mongod.instance().start(Version.Main.V8_0)
                } finally {
                    running?.close()
                }
            }
        }
    }
}
