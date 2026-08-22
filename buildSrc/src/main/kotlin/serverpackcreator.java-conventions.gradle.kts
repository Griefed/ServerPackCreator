@file:Suppress("UnstableApiUsage")

import de.griefed.common.gradle.TestHome
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.text.SimpleDateFormat
import java.util.*


plugins {
    java
    `java-library`
    idea
}

java {
    // Auto JDK setup
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

/**
 * Escape a filesystem path for a `.properties` value: backslashes and colons are separators there, so a Windows
 * path written verbatim would be read back mangled (`C:\dir` becomes `C` + a value starting at `dir`).
 */
private fun escapeForProperties(path: String): String = path.replace("\\", "\\\\").replace(":", "\\:")

// The suite boots an ApiWrapper from build/resources/test/serverpackcreator.properties in dozens of places, and two
// of its values are inherently per-machine: the JDK path SPC writes into generated packs, and the tomcat basedir.
// Committing resolved values means committing one developer's filesystem, so the committed file leaves them blank and
// the build fills them in on the way to build/resources/test. Pinned by `TestPropertiesTest`.
tasks.processTestResources {
    val moduleTestHome = layout.projectDirectory.dir("tests").asFile.absolutePath
    val testJavaExecutable = javaToolchains.launcherFor(java.toolchain).get().executablePath.asFile.absolutePath
    // Declared as inputs so a changed toolchain or module path re-runs the copy instead of serving a stale one.
    inputs.property("spcTestJavaExecutable", testJavaExecutable)
    inputs.property("spcTestModuleHome", moduleTestHome)
    // Escaped out here on purpose: the filter closure must capture only Strings. Calling the
    // script-level escapeForProperties() from inside it would capture the build script itself, which
    // the configuration cache cannot serialize.
    val escapedJavaExecutable = escapeForProperties(testJavaExecutable)
    val escapedModuleTestHome = escapeForProperties(moduleTestHome)
    filesMatching("serverpackcreator.properties") {
        filter { line: String ->
            when {
                line.startsWith("de.griefed.serverpackcreator.java=") ->
                    "de.griefed.serverpackcreator.java=$escapedJavaExecutable"
                line.startsWith("server.tomcat.basedir=") ->
                    "server.tomcat.basedir=$escapedModuleTestHome"
                else -> line
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    // Mockk/ByteBuddy attach an agent to the running JVM; without these the run warns on every start
    // and will fail outright once self-attach is disabled by default.
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Djdk.attach.allowAttachSelf=true")
    // A fresh, isolated test home for every run. The directory is captured as a File so the action
    // closes over that and nothing else; calling a script-level function here would capture the build
    // script, which the configuration cache cannot serialize.
    val testHome = layout.projectDirectory.dir("tests").asFile
    doFirst {
        TestHome.prepare(testHome)
    }
    // Keep test runs off the shared Preferences node. SPC's home directory lives in a per-user, machine-wide node
    // that PathsConfig re-reads on every access and writes back to, so a suite booting an ApiWrapper would relocate
    // the home of every other SPC process on the account — it moved a live grinder daemon's home into a test
    // scratch dir (which the suite then deleted), and equally moves a developer's own GUI home. One node per
    // module, so the suites cannot collide with each other either. Pinned by `PreferencesNodeTest`.
    systemProperty("de.griefed.serverpackcreator.preferences.node", "ServerPackCreator-test-${project.name}")
    // And an isolated home to go with it: `<module>/tests`, the directory the project already reserves for exactly
    // this (gitignored bar its .gitkeep, and what `server.tomcat.basedir` has always pointed at). ApiWrapper.setup()
    // *writes* into the home directory -- README.md, CHANGELOG.md, the server_files templates, manifests, logs -- and
    // with no stored home a dev build falls back to the working directory, which for a test JVM is the module's own
    // source tree; that is how a suite once overwrote serverpackcreator-clientside/README.md's CLI guide with the
    // bundled root README. Pinned by `PathsConfigTest`.
    systemProperty("de.griefed.serverpackcreator.home", layout.projectDirectory.dir("tests").asFile.absolutePath)
    testLogging {
        events = setOf(
            TestLogEvent.PASSED,
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }
}

tasks.compileJava {
    // See: https://docs.oracle.com/en/java/javase/12/tools/javac.html
    @Suppress("SpellCheckingInspection")
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all", // Enables all recommended warnings.
        )
    )
    options.encoding = "UTF-8"
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.clean {
    val testHome = layout.projectDirectory.dir("tests").asFile
    doFirst {
        TestHome.prepare(testHome)
    }
    doLast {
        delete {
            fileTree(projectDir.resolve("src/main/resources/static")) {
                exclude(".gitkeep")
            }
        }
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes(
            mapOf(
                "Built-By" to System.getProperty("user.name"),
                "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date()),
                "Created-By" to "Gradle ${gradle.gradleVersion}",
                "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${
                    System.getProperty("java.vm.version")
                })",
                "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${
                    System.getProperty("os.version")
                }",
                "Implementation-Vendor" to "Griefed",
                "Implementation-Version" to project.version,
                "Implementation-Title" to project.name
            )
        )
    }
}
