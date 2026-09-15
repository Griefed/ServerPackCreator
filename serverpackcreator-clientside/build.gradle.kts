plugins {
    id("serverpackcreator.kotlin-conventions")
    id("serverpackcreator.dokka-conventions")
}


dependencies {
    // The domain core: VersionMeta, ModScanner, ServerPackHandler, PackConfig, utilities.
    api(project(":serverpackcreator-api"))

    // The API ships jackson-databind, but not the Kotlin module the report-renderer needs for
    // jacksonObjectMapper(); log4j-api-kotlin comes transitively from the API.
    api(libs.jacksonModuleKotlin)

    // Headless-browser download of distribution-locked CurseForge files (locked = no direct URL).

    testImplementation(libs.kotlinTestJunit5)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.test {
    // MetadataScannerTest boots an offline ApiWrapper whose ModScanner relies on the API's cached
    // version-manifests, exactly as the API's own ModScannerTest does.
    dependsOn(":serverpackcreator-api:processTestResources")
}
