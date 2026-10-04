import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.`maven-publish`
import org.gradle.kotlin.dsl.signing
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.*

plugins {
    `maven-publish`
    signing
}

// Stub secrets to let the project sync and build without the publication values set up
ext["signing.keyId"] = null
ext["signing.password"] = null
ext["signing.secretKeyRingFile"] = null
ext["ossrhUsername"] = null
ext["ossrhPassword"] = null

val githubRepoName = ext["github-repo-name"]?.toString() ?: throw RuntimeException("Missing gradle property -- github-repo-name")
println("githubRepoName = $githubRepoName")

// Grabbing secrets from local.properties file or from environment variables, which could be used on CI
val secretPropsFile = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    secretPropsFile.reader().use {
        Properties().apply {
            load(it)
        }
    }.onEach { (name, value) ->
        ext[name.toString()] = value
    }
}
mapOf(
    "signing.keyId" to "SIGNING_KEY_ID",
    "signing.password" to "SIGNING_PASSWORD",
    "signing.secretKeyRingFile" to "SIGNING_SECRET_KEY_RING_FILE",
    "ossrhUsername" to "OSSRH_USERNAME",
    "ossrhPassword" to "OSSRH_PASSWORD",
).forEach { (property, environmentVariable) ->
    if (ext[property] == null) {
        val standardProperty = when (property) {
            "ossrhUsername" -> "mavenCentralUsername"
            "ossrhPassword" -> "mavenCentralPassword"
            else -> property
        }
        ext[property] = providers.gradleProperty(property).orNull
            ?: providers.gradleProperty(standardProperty).orNull
            ?: System.getenv(environmentVariable)
    }
}

/**
 * Using this "common" javadocJar creates error.
 *
 * Ref:
 * - https://github.com/gradle/gradle/issues/26091
 * - https://youtrack.jetbrains.com/issue/KT-46466
 */
//val javadocJar by tasks.registering(Jar::class) {
//    archiveClassifier.set("javadoc")
//}

fun getExtraString(name: String) = if (ext.has(name)) ext[name]?.toString() else null

publishing {
    // Configure maven central repository
    repositories {
        maven {
            name = "sonatype"
            setUrl("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = getExtraString("ossrhUsername")
                password = getExtraString("ossrhPassword")
            }
        }
    }

    // Configure all publications
    publications.withType<MavenPublication> {
        // Stub javadoc.jar artifact
        val javadocJar = tasks.register("${name}JavadocJar", Jar::class) {
            archiveClassifier.set("javadoc")
            archiveBaseName.set("${archiveBaseName.get()}-$name")
        }
        artifact(javadocJar)

        // Provide artifacts information requited by Maven Central
        pom {
            name.set("KDateTime Multiplatform")
            description.set("A Kotlin Multiplatform library to provide regular date-time functionality needed with very minimal platform dependencies.")
            url.set("https://github.com/sunny-chung/$githubRepoName")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("sunny-chung")
                    name.set("Sunny Chung")
                    email.set("sunnychung@live.hk")
                }
            }
            scm {
                url.set("https://github.com/sunny-chung/$githubRepoName")
            }
        }
    }
}

// Signing artifacts. Signing.* extra properties values will be used
signing {
    sign(publishing.publications)
}

val validateMavenCentralPublication = tasks.register("validateMavenCentralPublication") {
    group = "publishing"
    description = "Maven Central 배포 인증 및 서명 설정을 확인합니다."
    notCompatibleWithConfigurationCache("로컬 배포 설정을 실행 시점에 확인합니다.")
    doLast {
        val publishingType = getExtraString("publication.publishingType") ?: "user_managed"
        check(publishingType in listOf("user_managed", "automatic", "portal_api")) {
            "publication.publishingType은 user_managed, automatic, portal_api 중 하나여야 합니다."
        }
        val missing = listOf("ossrhUsername", "ossrhPassword", "signing.keyId", "signing.secretKeyRingFile")
            .filter { getExtraString(it).isNullOrBlank() }
        check(missing.isEmpty()) {
            "local.properties에 배포 설정이 필요합니다: ${missing.joinToString()}"
        }
        check(project.file(getExtraString("signing.secretKeyRingFile")!!).isFile) {
            "signing.secretKeyRingFile에 지정한 서명 키 파일이 없습니다."
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    if (name.endsWith("PublicationToSonatypeRepository")) {
        dependsOn(validateMavenCentralPublication)
    }
}

tasks.register("publishToMavenCentral") {
    group = "publishing"
    description = "전체 플랫폼을 업로드하고 해당 네임스페이스의 Central Portal로 전송합니다."
    dependsOn("publishAllPublicationsToSonatypeRepository")
    notCompatibleWithConfigurationCache("Maven Central 전송에 로컬 배포 설정을 사용합니다.")
    doLast {
        val namespace = getExtraString("publication.namespace") ?: project.group.toString()
        val encodedNamespace = URLEncoder.encode(namespace, "UTF-8")
        val publishingType = getExtraString("publication.publishingType") ?: "user_managed"
        val connection = URI(
            "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/$encodedNamespace?publishing_type=$publishingType"
        ).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 30_000
            connection.readTimeout = 120_000
            val credentials = "${getExtraString("ossrhUsername")}:${getExtraString("ossrhPassword")}"
            val token = Base64.getEncoder().encodeToString(credentials.toByteArray(Charsets.UTF_8))
            connection.setRequestProperty("Authorization", "Bearer $token")
            check(connection.responseCode in 200..299) {
                "Maven Central 전송 실패: HTTP ${connection.responseCode} (네임스페이스: $namespace)"
            }
            logger.lifecycle("$namespace 배포를 Central Portal로 전송했습니다. 공개 방식: $publishingType")
        } finally {
            connection.disconnect()
        }
    }
}

// skip signing if publishing to local maven
tasks.withType<Sign>().configureEach {
    mustRunAfter(validateMavenCentralPublication)
    onlyIf("publish to remote") {
        gradle.taskGraph.allTasks.any { it.name.matches("publish.*PublicationToSonatypeRepository".toRegex()) }
            .also { if (it) println("Will sign artifacts") }
    }
}
