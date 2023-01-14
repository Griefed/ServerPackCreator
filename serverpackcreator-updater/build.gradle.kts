plugins {
    id("serverpackcreator.kotlin-conventions")
    id("de.comahe.i18n4k") version "0.5.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":serverpackcreator-api"))
    api("de.griefed:versionchecker:1.1.4")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.7.22")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}