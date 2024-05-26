plugins {
    id("serverpackcreator.kotlin-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":serverpackcreator-api"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}