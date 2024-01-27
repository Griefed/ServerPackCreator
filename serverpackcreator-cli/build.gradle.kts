plugins {
    id("serverpackcreator.kotlin-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
	api(project(":serverpackcreator-api"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}