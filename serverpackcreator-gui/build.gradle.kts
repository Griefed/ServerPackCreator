plugins {
    id("serverpackcreator.kotlin-conventions")
    id("de.comahe.i18n4k") version "0.5.0"
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":serverpackcreator-api"))
    api("io.github.vincenzopalazzo:material-ui-swing:1.1.4")
    api("de.griefed:larsonscanner:1.0.4")
    api("commons-io:commons-io:2.11.0")
    implementation(project(":serverpackcreator-updater"))
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.7.22")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}