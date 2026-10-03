import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
version = "1.2.0-dev01"

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
        compileSdk = 37
        minSdk = 24
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.addAll(
                "-P",
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.sunnychung.lib.multiplatform.kdatetime.annotation.AndroidParcelize",
            )
        }
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
        watchosX64(),
        tvosArm64(),
        tvosSimulatorArm64(),
        tvosX64(),
        macosArm64(),
        macosX64(),
    )

    /*
        Note: Code compiled by IR has a running time slower than Legacy for 3X that could not pass the tests.
     */
    js {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
            testTask {
                useMocha {
                    timeout = if (isGitHubActionsCICD) {
                        "61s" // GitHub Actions Mac runners are significantly slower
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
                        "61s" // GitHub Actions Mac runners are significantly slower
                    } else {
                        "21s"
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")

    sourceSets {
        val commonMain = getByName("commonMain").apply {
            dependencies {
                implementation(libs.serialization.core)
            }
        }
        val commonTest = getByName("commonTest").apply {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.datetime)
                implementation(libs.serialization.json)
            }
        }
        val commonJvmMain = create("commonJvmMain").apply {
            dependsOn(commonMain)
        }
        val commonJvmTest = create("commonJvmTest").apply {
            dependsOn(commonTest)
        }
        val androidMain = getByName("androidMain").apply {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(libs.parcelize.runtime)
            }
        }
        val nonAndroidJvmMain = create("nonAndroidJvmMain").apply {
            dependsOn(commonJvmMain)
        }
        val nonAndroidJvmTest = create("nonAndroidJvmTest").apply {
            dependsOn(commonJvmTest)
        }
        val jvmMain = getByName("jvmMain").apply {
            dependsOn(nonAndroidJvmMain)
        }
        val jvmTest = getByName("jvmTest").apply {
            dependsOn(nonAndroidJvmTest)
        }
        val darwinMain = create("darwinMain").apply {
            dependsOn(commonMain)
        }
        val darwinTest = create("darwinTest").apply {
            dependsOn(commonTest)
        }
        val iosMain = create("iosMain").apply {
            dependsOn(darwinMain)
        }
        val watchosMain = create("watchosMain").apply {
            dependsOn(darwinMain)
        }
        val tvosMain = create("tvosMain").apply {
            dependsOn(darwinMain)
        }
        val macosMain = create("macosMain").apply {
            dependsOn(darwinMain)
        }

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
