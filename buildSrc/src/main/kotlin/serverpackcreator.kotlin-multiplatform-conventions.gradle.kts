import de.griefed.common.gradle.constant.JDK_VERSION
import de.griefed.common.gradle.constant.KOTLIN_VERSION
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
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
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
