import de.griefed.common.gradle.constant.JDK_VERSION
import de.griefed.common.gradle.constant.KOTLIN_VERSION
import java.text.SimpleDateFormat
import java.util.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("serverpackcreator.java-conventions")
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.allWarningsAsErrors = false
            kotlinOptions.apiVersion = KOTLIN_VERSION
            val compilerArgs = kotlinOptions.freeCompilerArgs.toMutableList()
            compilerArgs.add("-Xjsr305=strict")
            kotlinOptions.freeCompilerArgs = compilerArgs.toList()
            kotlinOptions.jvmTarget = JDK_VERSION
            kotlinOptions.languageVersion = KOTLIN_VERSION
            jvmToolchain {
                languageVersion.set(
                    JavaLanguageVersion.of(JDK_VERSION)
                )
            }
            withJava()
            testRuns["test"].executionTask.configure {
                useJUnitPlatform()
            }
            tasks.withType<Jar> {
                doFirst {
                    duplicatesStrategy = DuplicatesStrategy.INCLUDE
                    manifest {
                        attributes(
                            mapOf(
                                "Implementation-Title" to project.name,
                                "Implementation-Version" to project.version,
                                "Built-By" to System.getProperty("user.name"),
                                "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date()),
                                "Created-By" to "Gradle ${gradle.gradleVersion}",
                                "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${
                                    System.getProperty("java.vm.version")
                                })",
                                "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${
                                    System.getProperty("os.version")
                                }",
                                "Implementation-Vendor" to "Griefed",
                                "Implementation-Version" to project.version,
                                "Implementation-Title" to project.name
                            )
                        )
                    }
                }
            }
        }
    }
//    js(IR) {
//        useCommonJs()
//        browser {
//            commonWebpackConfig {
//                cssSupport {
//                    enabled = true
//                }
//            }
//        }
//    }
}