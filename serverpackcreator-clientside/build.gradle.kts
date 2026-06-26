plugins {
    id("serverpackcreator.kotlin-conventions")
    id("serverpackcreator.dokka-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    // The domain core: VersionMeta, ModScanner, ServerPackHandler, PackConfig, utilities.
    api(project(":serverpackcreator-api"))

    // The API ships jackson-databind, but not the Kotlin module the report-renderer needs for
    // jacksonObjectMapper(); log4j-api-kotlin comes transitively from the API.
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.22.0")

    // Headless-browser download of distribution-locked CurseForge files (locked = no direct URL).
    api("com.microsoft.playwright:playwright:1.60.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.3.21")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.0")
}

tasks.test {
    // MetadataScannerTest boots an offline ApiWrapper whose ModScanner relies on the API's cached
    // version-manifests, exactly as the API's own ModScannerTest does.
    dependsOn(":serverpackcreator-api:processTestResources")
}
