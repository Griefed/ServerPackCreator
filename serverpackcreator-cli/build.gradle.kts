plugins {
    id("serverpackcreator.kotlin-conventions")
    id("de.comahe.i18n4k") version "0.7.0"
}

repositories {
    mavenCentral()
}

dependencies {
	api(project(":serverpackcreator-api"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

// Explicit dependency to remove Gradle 8 warning
tasks.processResources {
    dependsOn(tasks.generateI18n4kFiles)
}

// Explicit dependency to remove Gradle 8 warning
tasks.sourcesJar {
    dependsOn(tasks.generateI18n4kFiles)
}