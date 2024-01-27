plugins {
    id("serverpackcreator.kotlin-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":serverpackcreator-api"))
    api("commons-io:commons-io:2.14.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
    api("org.jetbrains.kotlin:kotlin-reflect:1.9.10")
    implementation(project(":serverpackcreator-updater"))

    //New GUI
    api("com.formdev:flatlaf:3.2.5")
    api("com.formdev:flatlaf-extras:3.2.5")
    api("com.formdev:flatlaf-intellij-themes:3.2.5")
    api("com.formdev:flatlaf-fonts-jetbrains-mono:2.242")
    api("com.formdev:flatlaf-fonts-inter:3.19")
    api("com.formdev:flatlaf-fonts-roboto:2.137")
    api("com.formdev:flatlaf-fonts-roboto-mono:3.000")
    api("com.miglayout:miglayout-swing:11.2")
    api("com.formdev:svgSalamander:1.1.4")
    api("net.java.balloontip:balloontip:1.2.4.1")
    api("com.cronutils:cron-utils:9.2.1")
    api("tokyo.northside:tipoftheday:0.4.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}