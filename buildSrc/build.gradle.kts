plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
    implementation("org.jetbrains.kotlin:kotlin-allopen:1.8.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.7.20")

    implementation("org.springframework.boot:spring-boot-gradle-plugin:2.7.7")

    implementation("org.panteleyev:jpackage-gradle-plugin:1.5.0")

    implementation("com.github.jk1:gradle-license-report:2.0")
}
