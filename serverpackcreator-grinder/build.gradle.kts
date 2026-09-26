import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("serverpackcreator.kotlin-conventions")
    id("serverpackcreator.dokka-conventions")
    application
}

application {
    mainClass.set("de.griefed.serverpackcreator.grinder.GrinderApplication")
}


dependencies {
    // The clientside verification engine — the grinder is a container-backed ServerRunner plus the
    // fire-and-forget orchestration around BootVerifier. Pulls -api transitively.
    api(project(":serverpackcreator-clientside"))

    // Docker Engine API client: each candidate mod boots in its own isolated, network-less container.
    // The zerodep transport keeps the dependency footprint minimal (no Apache HttpClient).
    api(libs.dockerJavaCore)
    api(libs.dockerJavaTransport)

    // Instant (de)serialization for the file-backed verdict store; jackson-databind + the Kotlin
    // module arrive transitively via -clientside / -api.
    api(libs.jacksonJsr310)

    testImplementation(libs.kotlinTestJunit5)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

// ---------------------------------------------------------------------------------------------
// The `benchmark` source set: measurements that are not tests.
//
// StoreWriteBenchmark reports what JsonVerdictStore costs per record(). It cannot fail on the thing
// it measures -- this project pins I/O by request, read and open counts and never by wall-clock, and
// the behaviour those numbers motivated is pinned properly by CoalescedVerdictWritesTest. While it
// lived in src/test every build reported it as three skipped tests, which is three phantom entries
// in a skip count people actually read. Not being in the test source set is now the gate; it needs
// no environment variable.
//
// This is the first custom source set in the repository, and it is declared here rather than in a
// convention plugin on purpose: a module build script is a real script, so nothing about it reaches
// the other six modules or trips the precompiled-script-plugin rules in .claude/rules/build-layout.md.
// ---------------------------------------------------------------------------------------------
sourceSets {
    create("benchmark") {
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

// JUnit and the Kotlin test helpers, without naming them a second time.
configurations["benchmarkImplementation"].extendsFrom(configurations["testImplementation"])
configurations["benchmarkRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Kover instruments every Kotlin compilation, so without this the benchmark counts as production
// code and moves the coverage number for a reason that has nothing to do with the product.
kover {
    currentProject {
        sources {
            excludedSourceSets.add("benchmark")
        }
    }
}

tasks.register<Test>("benchmark") {
    group = "verification"
    description = "Measures JsonVerdictStore write cost. Asserts nothing about the timings -- see the class doc."
    testClassesDirs = sourceSets["benchmark"].output.classesDirs
    classpath = sourceSets["benchmark"].runtimeClasspath
    // Everything below is duplicated from java-conventions rather than inherited, because that plugin
    // configures `tasks.test` by name and not `tasks.withType<Test>().configureEach`. Broadening it
    // would be tidier and would reach all seven modules plus -app's own re-declaration, so it is a
    // separate decision, not a side effect of adding a benchmark.
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Djdk.attach.allowAttachSelf=true")
    // Insurance, not a current need: this benchmark only uses @TempDir. But a task in this project
    // that reaches SPC's home without these has twice relocated a live daemon's home and deleted it.
    systemProperty("de.griefed.serverpackcreator.preferences.node", "ServerPackCreator-test-${project.name}")
    systemProperty("de.griefed.serverpackcreator.home", layout.projectDirectory.dir("tests").asFile.absolutePath)
    // The measurements ARE the output. Without this they are captured into the XML report and never
    // shown, so an invocation prints nothing but BUILD SUCCESSFUL -- which for a benchmark is the same
    // as printing nothing at all. (Found by running it: 3 tests, 0 skipped, 10.2s, zero console output.)
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
    // Deliberately not wired into `check`: that is what keeps `build` from collecting it.
}
