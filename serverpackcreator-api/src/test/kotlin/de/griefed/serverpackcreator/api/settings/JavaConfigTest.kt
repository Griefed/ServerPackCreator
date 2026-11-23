package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Tests for [JavaConfig], the Java-installation settings-group. Uses the running JVM's java-binary as a
 * known-valid installation and pins path-validation, the per-version java-paths map, system-fallback
 * acquisition and the script-autoupdate flag.
 */
internal class JavaConfigTest {

    /**
     * The java-binary of the JVM running this test — a guaranteed-valid Java installation.
     */
    private val runningJava: String = File(
        File(System.getProperty("java.home"), "bin"),
        if (System.getProperty("os.name").lowercase().contains("win")) "java.exe" else "java"
    ).absolutePath

    /**
     * Pins that the java-paths setter accepts only valid Java installations and stores them
     * under the version-suffixed property-key, dropping invalid entries.
     */
    @Test
    fun javaPathsAcceptOnlyValidInstallations() {
        val store = PropertyStore()
        val javaConfig = JavaConfig(store)
        javaConfig.javaPaths = hashMapOf(
            "${JavaConfig.SCRIPT_JAVA_PATHS_PREFIX}17" to runningJava,
            "${JavaConfig.SCRIPT_JAVA_PATHS_PREFIX}21" to "/no/such/java"
        )
        Assertions.assertEquals(
            runningJava,
            store.properties.getProperty("${JavaConfig.SCRIPT_JAVA_PATHS_PREFIX}17")
        )
        Assertions.assertNull(
            store.properties.getProperty("${JavaConfig.SCRIPT_JAVA_PATHS_PREFIX}21"),
            "Invalid Java paths must be dropped"
        )
        Assertions.assertEquals(runningJava, javaConfig.javaPaths["17"])
    }

    /**
     * Pins that validating a Java path prints nothing to stdout.
     *
     * The probe runs `java -version`, and anything it echoes reaches the console of every GUI and CLI user
     * whose Java path is being checked. `JavaConfig` has a logger; the four `println`s in `ApiProperties` are
     * the deliberate exception, because that class *is* log4j's `ConfigurationFactory` and has no logger yet.
     *
     * Asserted on stdout rather than by grepping the source, so it pins the behaviour rather than the
     * spelling.
     */
    @Test
    fun validatingAJavaPathPrintsNothingToStandardOut() {
        val original = System.out
        val captured = java.io.ByteArrayOutputStream()
        try {
            System.setOut(java.io.PrintStream(captured, true))
            JavaConfig(PropertyStore()).javaPaths =
                hashMapOf("${JavaConfig.SCRIPT_JAVA_PATHS_PREFIX}17" to runningJava)
        } finally {
            System.setOut(original)
        }

        Assertions.assertEquals(
            "", captured.toString().trim(),
            "validating a Java path must not write to stdout; it has a logger"
        )
    }

    /**
     * Pins that a java-path can be retrieved per version, wrapped in an Optional, and that
     * unknown versions yield an empty Optional.
     */
    @Test
    fun javaPathByVersionReturnsOptionally() {
        val store = PropertyStore()
        val javaConfig = JavaConfig(store)
        store.define("${JavaConfig.SCRIPT_JAVA_PATHS_PREFIX}17", runningJava)
        Assertions.assertEquals(runningJava, javaConfig.javaPath(17).get())
        Assertions.assertEquals(runningJava, javaConfig.javaPath("17").get())
        Assertions.assertTrue(javaConfig.javaPath(42).isEmpty)
    }

    /**
     * Pins that the configured java-path falls back to a system-acquired Java when unset or
     * invalid, that invalid assignments are rejected, and valid assignments stored.
     */
    @Test
    fun javaPathFallsBackToSystemAndRejectsInvalidAssignment() {
        val store = PropertyStore()
        val javaConfig = JavaConfig(store)
        val acquired = javaConfig.javaPath
        Assertions.assertTrue(File(acquired).exists(), "System-acquired Java must exist: $acquired")
        Assertions.assertTrue(javaConfig.javaAvailable())

        javaConfig.javaPath = "/no/such/java"
        Assertions.assertNotEquals(
            "/no/such/java",
            store.properties.getProperty(JavaConfig.JAVA_FOR_SERVER_INSTALL_KEY),
            "Invalid Java assignments must be rejected"
        )
        javaConfig.javaPath = runningJava
        Assertions.assertEquals(runningJava, store.properties.getProperty(JavaConfig.JAVA_FOR_SERVER_INSTALL_KEY))
        Assertions.assertEquals(runningJava, javaConfig.javaPath)
    }

    /**
     * Pins that acquireJavaPath returns a valid given path unchanged and falls back to the
     * system-Java for invalid input.
     */
    @Test
    fun acquireJavaPathPrefersValidInputAndFallsBackToSystem() {
        val javaConfig = JavaConfig(PropertyStore())
        Assertions.assertEquals(runningJava, javaConfig.acquireJavaPath(runningJava))
        val fallback = javaConfig.acquireJavaPath("/no/such/java")
        Assertions.assertTrue(File(fallback).exists(), "Fallback Java must exist: $fallback")
    }

    /**
     * Pins that the script-java-autoupdate flag defaults to enabled and round-trips through the
     * store.
     */
    @Test
    fun scriptAutoupdateFlagDefaultsToEnabledAndRoundTrips() {
        val store = PropertyStore()
        val javaConfig = JavaConfig(store)
        Assertions.assertTrue(javaConfig.isJavaScriptAutoupdateEnabled)
        javaConfig.isJavaScriptAutoupdateEnabled = false
        Assertions.assertFalse(javaConfig.isJavaScriptAutoupdateEnabled)
        Assertions.assertEquals(
            "false",
            store.properties.getProperty(JavaConfig.SCRIPT_JAVA_AUTOUPDATE_KEY)
        )
    }

    /**
     * Pins that an executable which exits at once but unsuccessfully is not a Java. A real `java -version` exits 0;
     * anything else that merely starts and stops -- a wrapper failing on its arguments, GNU `yes`, `false` -- has
     * said nothing about being a Java. `/usr/bin/false` exits 1 whatever it is given, on GNU and BSD alike.
     */
    @Test
    fun anExecutableThatExitsUnsuccessfullyIsNotAJava() {
        val failsAtOnce = listOf("/usr/bin/false", "/bin/false").map(::File).firstOrNull { it.canExecute() }
            ?: Assumptions.abort("no `false` binary - the exit-code check is skipped")
        val store = PropertyStore()
        val javaConfig = JavaConfig(store)

        javaConfig.javaPaths = hashMapOf("${JavaConfig.SCRIPT_JAVA_PATHS_PREFIX}17" to failsAtOnce.canonicalPath)

        Assertions.assertNull(
            store.properties.getProperty("${JavaConfig.SCRIPT_JAVA_PATHS_PREFIX}17"),
            "an executable whose -version exits non-zero is not a working Java"
        )
    }

    /**
     * Pins that a "Java" which never exits is rejected within the probe's bound, and that the probe process is
     * gone afterwards. The check runs on the GUI's settings path, so an executable that waits on stdin or hangs
     * must not block it, nor be left running.
     *
     * `grep` stands in for it: it reads `-version` as `-v -e rsion` and then waits on its standard input, which
     * the probe leaves open, so it never exits on GNU or BSD alike; and as a system binary it starts at once,
     * which a freshly written script does not reliably do on a loaded host. (`yes` hangs only on BSD: GNU `yes`
     * rejects `-version` and exits.)
     */
    @Test
    fun aJavaThatNeverExitsIsRejectedAndKilled() {
        val neverExits = listOf("/usr/bin/grep", "/bin/grep").map(::File).firstOrNull { it.canExecute() }
            ?: Assumptions.abort("no `grep` binary - the hanging-probe check is skipped")
        val probePath = neverExits.canonicalPath
        val store = PropertyStore()
        val javaConfig = JavaConfig(store).apply { probeTimeoutMillis = 1_000 }

        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(30), {
            javaConfig.javaPaths = hashMapOf("${JavaConfig.SCRIPT_JAVA_PATHS_PREFIX}17" to probePath)
        }, "validating a Java path must not wait for the probed executable for ever")

        Assertions.assertNull(
            store.properties.getProperty("${JavaConfig.SCRIPT_JAVA_PATHS_PREFIX}17"),
            "a probe that did not finish is not a working Java"
        )
        val survivors = ProcessHandle.current().descendants()
            .filter { child -> child.info().command().map { File(it).canonicalPath == probePath }.orElse(false) }
            .toList()
        survivors.forEach { child -> runCatching { child.onExit().get(10, TimeUnit.SECONDS) } }
        val alive = survivors.filter { it.isAlive }
        alive.forEach { it.destroyForcibly() }
        Assertions.assertTrue(alive.isEmpty(), "the probe process must not outlive the check")
    }
}
