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
    api("com.github.docker-java:docker-java-core:3.7.1")
    api("com.github.docker-java:docker-java-transport-zerodep:3.7.1")

    // Instant (de)serialization for the file-backed verdict store; jackson-databind + the Kotlin
    // module arrive transitively via -clientside / -api.
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.22.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.3.21")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.0")
}
