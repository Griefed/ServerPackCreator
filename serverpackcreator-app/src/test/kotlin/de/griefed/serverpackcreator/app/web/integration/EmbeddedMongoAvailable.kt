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
         * flapdoodle's default is 30 s (`ProcessDefaults.startTimeout`), and that is not a budget a shared
         * CI host can be relied on to meet. Measured on Forgejo run 836, where the whole runner was
         * running three Gradle/npm builds at once: the probe below reached `Opening WiredTiger` and
         * emitted its next line **14.6 s** later, and the Spring context's `mongod`, started 34 s after
         * it on the same host, produced nothing at all inside the 30 s and took all fourteen
         * database-backed tests down with `Could not start process: Hmm.. no failure or success message
         * after 30000ms`. Nothing was wrong with `mongod`, with the context or with the tests — the
         * deadline was simply shorter than a starved host's disk.
         *
         * Three minutes, because what this budget trades is a slow machine's green run against a broken
         * machine's time-to-red, and only the first of those happens routinely. It is deliberately the
         * same number on both sides: without it the probe answers "mongod starts here" with one deadline
         * while the context fails against another, which is exactly how a guard written to turn this into
         * a SKIP let it through as fourteen failures instead.
         */
        const val START_TIMEOUT_MILLIS = 180_000L

        /**
         * The property flapdoodle's Spring autoconfiguration reads for [START_TIMEOUT_MILLIS].
         *
         * Spelled as `EmbeddedMongoProperties` spells it — the field reaches Spring through
         * `getStarttimeout`/`setStarttimeout`, so the JavaBean property is all one word. Verified that
         * this spelling is actually consumed rather than ignored, which is the failure mode this module
         * has already paid for twice (`spring.data.mongodb.uri`): setting it to `1` fails every test in
         * both classes with flapdoodle's own `no failure or success message after 1ms`. The hyphenated
         * `start-timeout` binds too — Spring's relaxed binding strips the hyphen — but the one-word form
         * is what the class declares, so it is the one that cannot drift.
         */
        const val START_TIMEOUT_PROPERTY = "de.flapdoodle.mongodb.embedded.starttimeout=$START_TIMEOUT_MILLIS"

        /** One start-and-stop per JVM. `lazy` is what makes it once rather than once per class. */
        val probe: Result<Unit> by lazy {
            runCatching {
                var running: TransitionWalker.ReachedState<RunningMongodProcess>? = null
                try {
                    running = Mongod.instance()
                        // The same deadline the Spring contexts get. A probe that is more patient than
                        // the thing it vouches for reports "mongod starts here" and then watches the
                        // context fail on the start it just approved.
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
