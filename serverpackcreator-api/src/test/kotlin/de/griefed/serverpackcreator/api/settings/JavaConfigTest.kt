package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for [JavaConfig], the Java-installation settings-group extracted from ApiProperties in
 * refactor Phase 1b. Uses the running JVM's java-binary as a known-valid installation and pins
 * path-validation, the per-version java-paths map, system-fallback acquisition and the
 * script-autoupdate flag.
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
}
