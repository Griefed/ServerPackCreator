plugins {
    id("serverpackcreator.kotlin-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":serverpackcreator-api"))
    api("commons-io:commons-io:2.16.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
    api("org.jetbrains.kotlin:kotlin-reflect:1.9.23")
    implementation(project(":serverpackcreator-updater"))

    //New GUI
    api("com.formdev:flatlaf:3.4")
    api("com.formdev:flatlaf-extras:3.4")
    api("com.formdev:flatlaf-intellij-themes:3.4")
    api("com.formdev:flatlaf-fonts-jetbrains-mono:2.304")
    api("com.formdev:flatlaf-fonts-inter:4.0")
    api("com.formdev:flatlaf-fonts-roboto:2.137")
    api("com.formdev:flatlaf-fonts-roboto-mono:3.000")
    api("com.miglayout:miglayout-swing:11.3")
    api("com.formdev:svgSalamander:1.1.4")
    api("net.java.balloontip:balloontip:1.2.4.1")
    api("com.cronutils:cron-utils:9.2.1")
    api("tokyo.northside:tipoftheday:0.4.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}