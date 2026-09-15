package de.griefed.serverpackcreator.app.gui.utilities

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests for [ComponentCoroutineScope], the lifecycle-owned scope that replaced the GUI's
 * fire-and-forget `GlobalScope.launch`es. Pins the four behaviours its doc-comment promises:
 * scope stability while active, cancellation of in-flight work, re-creation after a cancel
 * (detach/re-attach), and `SupervisorJob` sibling-independence. All timing-sensitive waits are
 * bounded by [withTimeout]/latch timeouts so a regression fails fast instead of hanging.
 */
internal class ComponentCoroutineScopeTest {

    /**
     * Pins that repeated [ComponentCoroutineScope.scope] calls return the same scope while it is
     * active — the component reuses one scope rather than allocating a fresh one per launch.
     */
    @Test
    fun scopeIsStableWhileActive() {
        val componentScope = ComponentCoroutineScope()
        Assertions.assertSame(componentScope.scope(), componentScope.scope())
    }

    /**
     * Pins the core reason the class exists: [ComponentCoroutineScope.cancel] cancels coroutines
     * still in flight on the scope, so a disposed component's work does not outlive it.
     */
    @Test
    fun cancelStopsInFlightCoroutines() {
        val componentScope = ComponentCoroutineScope()
        val coroutineStarted = CountDownLatch(1)
        val job = componentScope.scope().launch(Dispatchers.Default) {
            coroutineStarted.countDown()
            awaitCancellation()
        }
        Assertions.assertTrue(coroutineStarted.await(2, TimeUnit.SECONDS), "Coroutine never started")

        componentScope.cancel()

        runBlocking { withTimeout(2_000) { job.join() } }
        Assertions.assertTrue(job.isCancelled, "Cancelling the scope must cancel its in-flight work")
    }

    /**
     * Pins the detach/re-attach support: after a [ComponentCoroutineScope.cancel], the next
     * [ComponentCoroutineScope.scope] hands back a new, active scope that can still run work — so a
     * component removed and later re-added keeps functioning.
     */
    @Test
    fun scopeIsRecreatedAndUsableAfterCancel() {
        val componentScope = ComponentCoroutineScope()
        val cancelledScope = componentScope.scope()
        componentScope.cancel()

        val freshScope = componentScope.scope()
        Assertions.assertNotSame(cancelledScope, freshScope, "Cancelled scope must be replaced")
        Assertions.assertTrue(freshScope.isActive, "Re-created scope must be active")

        val ranOnFreshScope = CompletableDeferred<Boolean>()
        runBlocking {
            withTimeout(2_000) {
                freshScope.launch(Dispatchers.Default) { ranOnFreshScope.complete(true) }
                Assertions.assertTrue(ranOnFreshScope.await(), "Re-created scope must run new work")
            }
        }
    }

    /**
     * Pins the [kotlinx.coroutines.SupervisorJob] guarantee: a child coroutine that throws does not
     * cancel its siblings on the same scope — matching the independence the old per-call
     * `GlobalScope.launch`es had. The failing child carries its own handler so its expected
     * exception does not surface as noise; the surviving sibling outlives it.
     */
    @Test
    fun aFailingChildDoesNotCancelItsSiblings() {
        val componentScope = ComponentCoroutineScope()
        val scope = componentScope.scope()
        val swallowExpectedFailure = CoroutineExceptionHandler { _, _ -> }
        val siblingOutcome = CompletableDeferred<String>()

        scope.launch(Dispatchers.Default + swallowExpectedFailure) {
            throw RuntimeException("boom")
        }
        scope.launch(Dispatchers.Default) {
            delay(200)
            siblingOutcome.complete("survived")
        }

        runBlocking {
            Assertions.assertEquals("survived", withTimeout(2_000) { siblingOutcome.await() })
        }
    }
}
