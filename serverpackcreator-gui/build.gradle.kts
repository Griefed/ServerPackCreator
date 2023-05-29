plugins {
    id("serverpackcreator.kotlin-conventions")
    id("de.comahe.i18n4k") version "0.5.0"
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":serverpackcreator-api"))
    api("commons-io:commons-io:2.11.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")
    implementation(project(":serverpackcreator-updater"))

    //New GUI
    api("com.formdev:flatlaf:3.0")
    api("com.formdev:flatlaf-extras:3.0")
    api("com.formdev:flatlaf-intellij-themes:3.0")
    api("com.formdev:flatlaf-fonts-jetbrains-mono:2.242")
    api("com.miglayout:miglayout-swing:11.0")
    api("com.formdev:svgSalamander:1.1.4")
    api("net.java.balloontip:balloontip:1.2.4.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

// Explicit dependency to remove Gradle 8 warning
tasks.processResources {
    dependsOn(tasks.generateI18n4kFiles)
}

// Explicit dependency to remove Gradle 8 warning
tasks.sourcesJar {
    dependsOn(tasks.generateI18n4kFiles)
}