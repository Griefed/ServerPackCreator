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
