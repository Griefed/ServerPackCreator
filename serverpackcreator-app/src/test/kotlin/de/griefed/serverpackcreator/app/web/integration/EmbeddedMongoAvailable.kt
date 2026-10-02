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

import de.flapdoodle.embed.mongo.distribution.Versions
import de.flapdoodle.embed.mongo.transitions.Mongod
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess
import de.flapdoodle.embed.mongo.types.StartTimeout
import de.flapdoodle.reverse.TransitionWalker
import de.flapdoodle.reverse.transitions.Start
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

    companion object {
        /**
         * The version these tests pin against — the same line `docker/docker-compose.yml` deploys.
         *
         * Public and `const` so the `@SpringBootTest` annotations can reference it: the probe that decides
         * whether to SKIP and the server the tests actually RUN must be the same version, or the probe
         * validates something the tests never use.
         */
        const val MONGOD_VERSION = "8.0.5"

        /** The property flapdoodle's autoconfiguration reads, pre-composed so no annotation spells it twice. */
        const val VERSION_PROPERTY = "de.flapdoodle.mongodb.embedded.version=" + MONGOD_VERSION

        /**
         * How long `mongod` may take to announce itself before flapdoodle gives up, in milliseconds.
         *
         * Three minutes, against flapdoodle's own default of 30 s, because a CI host shared with other
         * builds can take far longer than a developer machine to get WiredTiger open — and a deadline
         * that expires takes every database-backed test down with it, reporting a slow disk as a broken
         * application.
         *
         * Used on both sides, deliberately: the probe below and the `@SpringBootTest` classes must give
         * `mongod` the same budget, or the probe approves a start the contexts then fail on.
         */
        const val START_TIMEOUT_MILLIS = 180_000L

        /**
         * The property flapdoodle's Spring autoconfiguration reads for [START_TIMEOUT_MILLIS].
         *
         * One word, as `EmbeddedMongoProperties` spells it: the field reaches Spring through
         * `getStarttimeout`/`setStarttimeout`, so that is the JavaBean property name. Spring's relaxed
         * binding accepts `start-timeout` as well, but the one-word form is the one the class declares
         * and so the one that cannot drift from it.
         */
        const val START_TIMEOUT_PROPERTY = "de.flapdoodle.mongodb.embedded.starttimeout=$START_TIMEOUT_MILLIS"

        /** One start-and-stop per JVM. `lazy` is what makes it once rather than once per class. */
        val probe: Result<Unit> by lazy {
            runCatching {
                var running: TransitionWalker.ReachedState<RunningMongodProcess>? = null
                try {
                    running = Mongod.instance()
                        // The same deadline the Spring contexts get, per START_TIMEOUT_MILLIS.
                        .withStartTimeout(
                            Start.to(StartTimeout::class.java)
                                .initializedWith(StartTimeout.of(START_TIMEOUT_MILLIS))
                        )
                        .start(
                            // Derived from MONGOD_VERSION rather than named again: flapdoodle spells
                            // "8.0.5" as the enum constant V8_0_5, and a second literal here is how the
                            // probe and the tests would come to validate different servers.
                            Versions.withFeatures(
                                de.flapdoodle.embed.mongo.distribution.Version.valueOf(
                                    "V" + MONGOD_VERSION.replace('.', '_')
                                )
                            )
                        )
                } finally {
                    running?.close()
                }
            }
        }
    }
}
