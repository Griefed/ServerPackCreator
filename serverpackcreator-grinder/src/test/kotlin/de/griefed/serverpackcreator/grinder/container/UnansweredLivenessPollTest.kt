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

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.InspectContainerCmd
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.command.KillContainerCmd
import com.github.dockerjava.api.command.LogContainerCmd
import com.github.dockerjava.api.command.RemoveContainerCmd
import com.github.dockerjava.api.command.StartContainerCmd
import com.github.dockerjava.api.command.StopContainerCmd
import de.griefed.serverpackcreator.grinder.report.capturingLogsOf
import org.apache.logging.log4j.Level
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Pins how [DockerJavaContainerEngine.run]'s poll loop treats a liveness inspect the daemon does not answer.
 *
 * One unanswered poll is "unknown", not "stopped": reading it as stopped ends the wait, stops and force-removes a
 * container that was still booting, and classifies a truncated console. Only an unbroken run of unanswered polls
 * abandons the boot, and it does so by throwing, so the row reads `ERROR` with the daemon named as the reason
 * rather than as a verdict about the mod.
 *
 * Driven through the real `run` against a scripted docker-java client, because the defect lives in the loop's
 * wiring, not in any one helper.
 */
internal class UnansweredLivenessPollTest {

    private val json = ObjectMapper()
    private val spec = ContainerSpec(image = "spc-grinder-runtime:latest", command = listOf("bash", "start.sh"), workingDir = "/pack", mounts = emptyList())

    private fun running() = json.readValue("""{"State":{"Running":true}}""", InspectContainerResponse::class.java)

    private fun exited(code: Int) =
        json.readValue("""{"State":{"Running":false,"ExitCode":$code}}""", InspectContainerResponse::class.java)

    /** What the scripted daemon did besides answering inspects. */
    private class Calls {
        @Volatile var stopped = false
        @Volatile var removed = false
    }

    /**
     * A command whose builder methods return itself and whose `exec` answers [exec], which is the whole shape of a
     * docker-java command.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> command(type: Class<T>, exec: (Array<Any?>?) -> Any?): T =
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, args ->
            when {
                method.name == "exec" -> exec(args)
                method.name == "close" -> null
                method.name == "hashCode" -> System.identityHashCode(proxy)
                method.name == "equals" -> proxy === args?.firstOrNull()
                method.name == "toString" -> type.simpleName
                method.returnType.isAssignableFrom(type) -> proxy
                else -> throw UnsupportedOperationException(method.name)
            }
        } as T

    /** A client whose container inspects answer [inspects] in order, the last one repeating. */
    private fun client(calls: Calls, inspects: List<() -> InspectContainerResponse>): DockerClient {
        val script = ConcurrentLinkedQueue(inspects)
        val created = json.readValue("""{"Id":"c0ffee"}""", CreateContainerResponse::class.java)
        return Proxy.newProxyInstance(DockerClient::class.java.classLoader, arrayOf(DockerClient::class.java)) { _, method, _ ->
            when (method.name) {
                "createContainerCmd" -> command(CreateContainerCmd::class.java) { created }
                "startContainerCmd" -> command(StartContainerCmd::class.java) { null }
                "logContainerCmd" -> command(LogContainerCmd::class.java) { args -> args?.first() }
                "inspectContainerCmd" -> command(InspectContainerCmd::class.java) {
                    (if (script.size > 1) script.poll() else script.peek())()
                }
                "stopContainerCmd" -> command(StopContainerCmd::class.java) { calls.stopped = true; null }
                "killContainerCmd" -> command(KillContainerCmd::class.java) { calls.stopped = true; null }
                "removeContainerCmd" -> command(RemoveContainerCmd::class.java) { calls.removed = true; null }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as DockerClient
    }

    private val unanswered: () -> InspectContainerResponse = { throw IllegalStateException("daemon went away") }

    @Test
    fun oneUnansweredPollDoesNotEndTheBoot() {
        val calls = Calls()
        val engine = DockerJavaContainerEngine(
            client(calls, listOf(::running, unanswered, ::running, { exited(1) })),
            maxUnansweredPolls = 3
        )

        val (output, logged) = capturingLogsOf(DockerJavaContainerEngine::class.java) { log ->
            engine.run(spec, Regex("Done \\("), Duration.ofMinutes(1)) to log.hasThrowableAt(Level.WARN)
        }

        Assertions.assertFalse(calls.stopped, "one unanswered inspect must not end the wait and stop a booting container")
        Assertions.assertEquals(1, output.exitCode, "the boot ran until the container exited on its own")
        Assertions.assertFalse(output.timedOut)
        Assertions.assertTrue(logged, "the unanswered poll must reach the journal with its cause")
    }

    /** The bound is on an unbroken run: an answer in between starts the count again. */
    @Test
    fun anAnsweredPollStartsTheCountAgain() {
        val calls = Calls()
        val engine = DockerJavaContainerEngine(
            client(calls, listOf(::running, unanswered, unanswered, ::running, unanswered, unanswered, { exited(1) })),
            maxUnansweredPolls = 3
        )

        val output = engine.run(spec, Regex("Done \\("), Duration.ofMinutes(1))

        Assertions.assertEquals(1, output.exitCode, "four unanswered polls in two runs of two must not abandon the boot")
    }

    @Test
    fun anUnbrokenRunOfUnansweredPollsAbandonsTheBootAndSaysWhy() {
        val calls = Calls()
        val engine = DockerJavaContainerEngine(client(calls, listOf(::running, unanswered)), maxUnansweredPolls = 3)

        val failure = capturingLogsOf(DockerJavaContainerEngine::class.java) { log ->
            Assertions.assertThrows(IllegalStateException::class.java) {
                engine.run(spec, Regex("Done \\("), Duration.ofMinutes(1))
            }.also {
                Assertions.assertTrue(log.hasThrowableAt(Level.ERROR), "abandoning a boot must be logged at ERROR with the cause")
            }
        }

        Assertions.assertTrue(
            failure.message.orEmpty().contains("c0ffee"),
            "the reason the row carries must name the container: ${failure.message}"
        )
        Assertions.assertTrue(calls.removed, "an abandoned container is still removed")
    }
}
