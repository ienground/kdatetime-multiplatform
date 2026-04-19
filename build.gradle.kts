import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.parcelize)
    id("maven-publish")
    id("sunnychung.publication")
}

group = "io.github.sunny-chung"
version = libs.versions.lib.version.name.get()

val isGitHubActionsCICD = project.hasProperty("CICD") && project.property("CICD") == "GitHubActions"
if (isGitHubActionsCICD) {
    println("Running with GitHub Actions CI/CD")
}

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.sunnychung.lib.android.kdatetime"
        compileSdk = 36
        minSdk = 24
    }

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    val darwinTargets = listOf<KotlinNativeTarget>(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
        watchosArm64(),
        watchosSimulatorArm64(),
        tvosArm64(),
        tvosSimulatorArm64(),
        macosArm64(),
    )

    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
            testTask {
                useMocha {
                    timeout = if (isGitHubActionsCICD) {
                        "61s"
                    } else {
                        "21s"
                    }
                }
            }
        }
        nodejs {
            testTask {
                useMocha {
                    timeout = if (isGitHubActionsCICD) {
                        "61s"
                    } else {
                        "21s"
                    }
                }
            }
        }
    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.serialization.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.datetime)
                implementation(libs.serialization.json)
            }
        }
        val commonJvmMain by creating {
            dependsOn(commonMain)
        }
        val commonJvmTest by creating {
            dependsOn(commonTest)
        }
        val androidMain by getting {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(libs.parcelize.runtime)
            }
        }
        val nonAndroidJvmMain by creating {
            dependsOn(commonJvmMain)
        }
        val nonAndroidJvmTest by creating {
            dependsOn(commonJvmTest)
        }
        val jvmMain by getting {
            dependsOn(nonAndroidJvmMain)
        }
        val jvmTest by getting {
            dependsOn(nonAndroidJvmTest)
        }
        val darwinMain by creating {
            dependsOn(commonMain)
        }
        val darwinTest by creating {
            dependsOn(commonTest)
        }
        val iosMain by creating {
            dependsOn(darwinMain)
        }
        val watchosMain by creating {
            dependsOn(darwinMain)
        }
        val tvosMain by creating {
            dependsOn(darwinMain)
        }
        val macosMain by creating {
            dependsOn(darwinMain)
        }
        val jsMain by getting
        val jsTest by getting

        configure(darwinTargets) {
            val (mainSourceSet, testSourceSet) = when {
                name.startsWith("ios") -> Pair(iosMain, darwinTest)
                name.startsWith("watchos") -> Pair(watchosMain, darwinTest)
                name.startsWith("tvos") -> Pair(tvosMain, darwinTest)
                name.startsWith("macos") -> Pair(macosMain, darwinTest)
                else -> throw UnsupportedOperationException("Target $name is not supported")
            }
            compilations["main"].defaultSourceSet.dependsOn(mainSourceSet)
            compilations["test"].defaultSourceSet.dependsOn(testSourceSet)
        }
    }
}

tasks.withType<Test> {
    testLogging {
        events = setOf(TestLogEvent.STARTED, TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
    }
}