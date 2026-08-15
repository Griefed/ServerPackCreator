plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

// buildSrc is a separate build and does NOT inherit the root build's version catalog (verified on
// Gradle 8.14.4: removing this block fails buildSrc compilation with "Unresolved reference: libs"),
// so it points at the same file explicitly. The root build needs no such block — it finds
// gradle/libs.versions.toml by convention.
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
