import com.diffplug.spotless.LineEnding
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset
import java.time.Duration

val isCiBuild = providers.environmentVariable("CI").map { it.toBoolean() }.orElse(false).get()
val isSnapshot = providers.gradleProperty("isSnapshot").map { it.toBoolean() }.orElse(false).get()

val mavenRepoUrl = providers.gradleProperty("mavenRepoUrl")
val mavenRepoName = if (isSnapshot) "snapshots" else "releases"

val baseVersion = project.version.toString()
project.version = if (isSnapshot) "$baseVersion-SNAPSHOT" else if (!isCiBuild) "$baseVersion.local" else baseVersion

plugins {
    java

    alias(libs.plugins.shadow)
    alias(libs.plugins.download)

    alias(libs.plugins.spotless)
    checkstyle

    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.dsljson)
    implementation(libs.commons.collections)
    implementation(libs.flatlaf)

    testImplementation(libs.junit)
}

base {
    archivesName = project.name
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

tasks.jar {
    enabled = false

    manifest {
        attributes("Enable-Native-Access" to "ALL-UNNAMED")
    }
}

tasks.shadowJar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to "leaf-${project.name}",
                "Implementation-Version" to project.version,
                "Main-Class" to "${project.group}.${project.name}.Main"
            )
        )
    }

    minimize {
        exclude(dependency("${libs.flatlaf.get()}"))
    }

    archiveClassifier.set("")
}

tasks.publish {
    mustRunAfter(checkVersion)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}

// Workaround for https://youtrack.jetbrains.com/issue/KT-46466
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(tasks.withType<Sign>())
}

tasks.withType<Sign>().configureEach {
    enabled = isCiBuild && !isSnapshot
}

val checkVersion = tasks.register("checkVersion") {
    description = "Ensures that the version being released has not already been released"

    doLast {
        val groupPath = rootProject.group.toString().replace(".", "/")
        val artifactId = project.name
        val version = project.version.toString()
        val url = "$mavenRepoUrl/$mavenRepoName/$groupPath/$artifactId/$version/$artifactId-$version.jar"

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI(url))
            .method("HEAD", HttpRequest.BodyPublishers.noBody())
            .timeout(Duration.ofSeconds(5))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.discarding())
        when (response.statusCode()) {
            200 -> throw RuntimeException("Artifact $artifactId with version $version already published!")
            404 -> println("Artifact $artifactId with version $version is not published.")
            else -> println("Unexpected response code: ${response.statusCode()}")
        }
    }
}

spotless {
    setEncoding(Charset.forName("UTF-8"))
    setLineEndings(LineEnding.UNIX)

    java {
        targetExclude("**/generated/**")

        eclipse().configFile(file("eclipse-formatter.xml"))
            .sortMembersEnabled(true)
            // SF,SI,SM,F,I,C,M,T = Static Fields, Static Initializers, Static Methods,
            //   Fields, Initializers, Constructors, Methods, (Nested) Types
            .sortMembersOrder("SF,F,SI,I,C,SM,M,T")
            .sortMembersDoNotSortFields(true)
            .sortMembersVisibilityOrderEnabled(true)
            // B,R,D,V = Public, Protected, Package, Private
            .sortMembersVisibilityOrder("B,R,D,V")

        cleanthat()
        // formatAnnotations()

        importOrder(
            "java|javax",
            "",
            "net.fabricmc",
            "\\#net.fabricmc",
            "$group",
            "\\#",
            "\\#$group",
            "zombie",
            "\\#zombie"
        )
        removeUnusedImports()
        forbidWildcardImports()
        forbidModuleImports()

        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()

        toggleOffOn()
        licenseHeaderFile(file("HEADER")).yearSeparator("-").onlyIfContentMatches("^[^/]{2}")
    }

    kotlin {
        targetExclude("src/test/resources/**")

        ktlint()

        trimTrailingWhitespace()
        endWithNewline()

        toggleOffOn()
        licenseHeaderFile(file("HEADER")).yearSeparator("-").onlyIfContentMatches("^[^/]{2}")
    }
}

checkstyle {
    configFile = file("checkstyle.xml")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["shadow"])
            artifact(tasks.named("sourcesJar"))

            pom {
                name = rootProject.name
                group = rootProject.group
                description = rootProject.description
                url = property("url").toString()
                inceptionYear = "2025"

                developers {
                    developer {
                        id = "aoqia"
                        name = "aoqia"
                        email = "aoqia@aoqia.dev"
                    }
                }

                licenses {
                    license {
                        name = "AGPL-3.0-or-later"
                        url = "https://spdx.org/licenses/AGPL-3.0-or-later.html"
                    }
                    license {
                        name = "Apache-2.0"
                        url = "https://spdx.org/licenses/Apache-2.0.html"
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "leaf"
            url = uri("https://maven.aoqia.dev/${if (isSnapshot) "snapshots" else "releases"}")

            credentials {
                username = providers.gradleProperty("mavenUsername").orNull
                password = providers.gradleProperty("mavenPassword").orNull
            }

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

signing {
    isRequired = isCiBuild and !isSnapshot

    val signingKey = providers.gradleProperty("signingKey")
    val signingPassword = providers.gradleProperty("signingPassword")
    if (signingKey.isPresent && signingPassword.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
    }

    sign(publishing.publications)
}
