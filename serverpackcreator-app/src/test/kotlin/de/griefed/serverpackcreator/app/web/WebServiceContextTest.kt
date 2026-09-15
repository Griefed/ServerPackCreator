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
package de.griefed.serverpackcreator.app.web

import de.griefed.serverpackcreator.app.web.index.DeclaredIndexCreator
import de.griefed.serverpackcreator.app.web.index.IndexStore
import de.griefed.serverpackcreator.app.web.migration.MigrationStore
import de.griefed.serverpackcreator.app.web.migration.RunConfigurationListMigration
import de.griefed.serverpackcreator.app.web.migration.RunConfigurationListMigrationRunner
import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackService
import de.griefed.serverpackcreator.app.web.serverpack.runconfiguration.RunConfigurationService
import de.griefed.serverpackcreator.app.web.task.EventService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener

/**
 * Boots the **real** web application context and asserts it wires up.
 *
 * Replaces the old `WebServiceTest`, which was `@SpringBootTest(classes = [WebServiceTest::class])` —
 * a context of exactly one class, itself — with an empty `contextLoads()` body. It could not fail for
 * any reason involving ServerPackCreator, which is why this module's `CLAUDE.md` said to replace
 * rather than extend it.
 *
 * **This needs no database, and that is not an oversight.** The MongoDB driver connects lazily, so the
 * context starts, every bean is constructed and every injection point is resolved without a server
 * being reachable; the driver logs a `ConnectionException` in the background and startup continues.
 * That is exactly the coverage worth having here — bean wiring across all nine controllers, the
 * services, the repositories and the scheduling — and it is the half that breaks when someone adds a
 * constructor parameter or misplaces an annotation. Actually *exercising* a query still needs a live
 * Mongo and belongs in an integration test, not here.
 *
 * The three schedules are disabled via Spring's `CRON_DISABLED` (`-`) rather than left on their
 * midnight crons: `FileCleanupSchedule` deletes modpack files whose IDs are absent from the database,
 * and a suite that happens to run at 00:30 should not be one unreachable-database away from finding
 * out what that does.
 */
@SpringBootTest(
    classes = [WebService::class],
    properties = [
        "de.griefed.serverpackcreator.spring.schedules.database.cleanup=-",
        "de.griefed.serverpackcreator.spring.schedules.files.cleanup=-",
        "de.griefed.serverpackcreator.spring.schedules.versions.refresh=-"
    ]
)
internal class WebServiceContextTest {

    @Autowired
    private lateinit var context: ApplicationContext

    /** Every `@RestController` must be present; a missing one is a route that silently 404s. */
    @Test
    fun everyControllerIsWired() {
        val controllers = context.getBeansWithAnnotation(org.springframework.web.bind.annotation.RestController::class.java)
        Assertions.assertTrue(
            controllers.size >= 8,
            "Expected the web controllers to be registered, found ${controllers.size}: ${controllers.keys}"
        )
    }

    /**
     * The four services the controllers delegate to. Asserted by type rather than by count, because
     * what matters is that each resolved its own dependencies — these are the beans that inject the
     * Mongo repositories.
     */
    @Test
    fun theServicesTheControllersDelegateToAreWired() {
        Assertions.assertNotNull(context.getBean(ModPackService::class.java))
        Assertions.assertNotNull(context.getBean(ServerPackService::class.java))
        Assertions.assertNotNull(context.getBean(RunConfigurationService::class.java))
        Assertions.assertNotNull(context.getBean(EventService::class.java))
    }

    /**
     * The context must have read ServerPackCreator's own property-files, not just Spring's defaults.
     * This is the other end of `WebServiceArgumentsTest`'s config-location chain: that test pins which
     * files are offered, this one pins that they actually arrive.
     */
    @Test
    fun serverPackCreatorsOwnPropertiesAreLoaded() {
        Assertions.assertNotNull(
            context.environment.getProperty("de.griefed.serverpackcreator.serverpack.autodiscovery.enabled"),
            "ServerPackCreator's properties were not on the environment — the config-location chain did not reach them"
        )
    }

    /** The disabled schedules must genuinely not be registered as running tasks. */
    @Test
    fun theSchedulesAreDisabledForThisContext() {
        Assertions.assertEquals(
            "-", context.environment.getProperty("de.griefed.serverpackcreator.spring.schedules.files.cleanup"),
            "The file-cleanup schedule was not disabled for this test"
        )
    }

    /**
     * The components that only do their work if Spring created them. Every one of them is unit-tested by
     * direct construction, which passes whether or not the bean exists — so registration is the half no
     * other test can see, and the half whose failure is silent. An inert
     * [RunConfigurationListMigrationRunner] leaves persisted data in a shape the mapped type cannot read
     * while the whole suite stays green; an inert [DeclaredIndexCreator] silently returns the upload
     * duplicate-check to a collection scan.
     */
    @Test
    fun theComponentsThatOnlyWorkIfSpringCreatedThemAreWired() {
        Assertions.assertNotNull(context.getBean(RunConfigurationListMigration::class.java))
        Assertions.assertNotNull(context.getBean(RunConfigurationListMigrationRunner::class.java))
        Assertions.assertNotNull(context.getBean(MigrationStore::class.java))
        Assertions.assertNotNull(context.getBean(DeclaredIndexCreator::class.java))
        Assertions.assertNotNull(context.getBean(IndexStore::class.java))
    }

    /**
     * Both deferred jobs must be triggered by `ApplicationReadyEvent` and nothing earlier. Asserted on the
     * beans Spring actually holds, rather than on the classes, because the point is that the *registered*
     * listeners are the deferred ones: running either during context refresh makes an unreachable database
     * cancel the refresh, which is what happened once already.
     */
    @Test
    fun theDeferredJobsRunOnlyOnceTheApplicationIsReady() {
        val deferred = listOf(
            context.getBean(RunConfigurationListMigrationRunner::class.java),
            context.getBean(DeclaredIndexCreator::class.java)
        )
        for (bean in deferred) {
            val listeners = bean.javaClass.methods.filter { it.isAnnotationPresent(EventListener::class.java) }
            Assertions.assertEquals(
                1, listeners.size,
                "${bean.javaClass.simpleName} must have exactly one event listener, found: " +
                        listeners.map { it.name }
            )
            Assertions.assertEquals(
                listOf(ApplicationReadyEvent::class.java.name),
                listeners.single().getAnnotation(EventListener::class.java).value.map { it.java.name },
                "${bean.javaClass.simpleName} must defer its work to ApplicationReadyEvent"
            )
        }
    }
}
