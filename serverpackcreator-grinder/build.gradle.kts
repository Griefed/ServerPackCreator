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

// Kover instruments every Kotlin compilation, so without this the benchmark counts as production
// code and moves the coverage number for a reason that has nothing to do with the product.
kover {
    currentProject {
        sources {
            excludedSourceSets.add("benchmark")
        }
    }
}

// main's own dependencies -- clientside, jackson -- reach the benchmark. `implementation` extends `api`,
// so the module's `api(...)` declarations come with it and nothing is named twice. Deliberately NOT
// `testImplementation`: the benchmark needs no test framework, which is the point of it being a program.
configurations["benchmarkImplementation"].extendsFrom(configurations["implementation"])
configurations["benchmarkRuntimeOnly"].extendsFrom(configurations["runtimeOnly"])

tasks.register<JavaExec>("benchmark") {
    group = "verification"
    description = "Measures JsonVerdictStore write cost. Asserts nothing about the timings -- see the class doc."
    mainClass.set("de.griefed.serverpackcreator.grinder.report.StoreWriteBenchmarkKt")
    classpath = sourceSets["benchmark"].runtimeClasspath
    // JavaExec rather than Test, and this is the load-bearing part. Measured on this build: EVERY task
    // of type `Test` is pulled into `check` -- by type, not by name or group, and with nothing in any
    // build file declaring it. A JUnit benchmark therefore ran on every `./gradlew build` however it was
    // registered, which is the whole thing this source set exists to prevent. Probed three ways before
    // believing it: renaming the task changed nothing, moving it out of the `verification` group changed
    // nothing, removing the Kover block changed nothing.
    systemProperty("de.griefed.serverpackcreator.preferences.node", "ServerPackCreator-test-${project.name}")
    systemProperty("de.griefed.serverpackcreator.home", layout.projectDirectory.dir("tests").asFile.absolutePath)
}
