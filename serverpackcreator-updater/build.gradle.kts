plugins {
    id("serverpackcreator.kotlin-conventions")
    id("de.comahe.i18n4k") version "0.5.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":serverpackcreator-api"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

// Explicit dependency to remove Gradle 8 warning
tasks.processResources {
    dependsOn(tasks.generateI18n4kFiles)
}

// Explicit dependency to remove Gradle 8 warning
tasks.sourcesJar {
    dependsOn(tasks.generateI18n4kFiles)
}