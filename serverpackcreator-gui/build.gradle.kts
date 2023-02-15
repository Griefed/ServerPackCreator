plugins {
    id("serverpackcreator.kotlin-conventions")
    id("de.comahe.i18n4k") version "0.5.0"
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":serverpackcreator-api"))
    api("de.griefed:larsonscanner:1.0.4")
    api("commons-io:commons-io:2.11.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")
    implementation(project(":serverpackcreator-updater"))

    //New GUI
    implementation("com.formdev:flatlaf:3.0")
    implementation("com.formdev:flatlaf-extras:3.0")
    implementation("com.formdev:flatlaf-intellij-themes:3.0")
    implementation("com.formdev:flatlaf-fonts-inter:3.19")
    implementation("com.formdev:flatlaf-fonts-roboto:2.137")
    implementation("com.formdev:flatlaf-fonts-roboto-mono:3.000")
    implementation("com.formdev:flatlaf-fonts-jetbrains-mono:2.242")
    implementation("com.miglayout:miglayout-swing:11.0")
    implementation("com.formdev:svgSalamander:1.1.4")

    //Old GUI
    api("io.github.vincenzopalazzo:material-ui-swing:1.1.4")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}
