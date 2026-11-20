import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.21"
    id("org.jetbrains.dokka") version "2.3.0"
    // Maven Central publishing with sources/javadoc jars and signing. Publishing runs from CI: see PUBLISHING.md.
    id("com.vanniktech.maven.publish") version "0.37.0"
    id("com.diffplug.spotless") version "8.11.0"
}

group = "ru.onlinesim"
version = "1.0.1"
description = "Kotlin coroutines client for the OnlineSim API: SMS numbers, rent, account and free numbers."

repositories {
    mavenCentral()
}

dependencies {
    // The Java SDK does the HTTP, parsing and the mock; its types are re-exported, so it is `api`.
    api("ru.onlinesim:onlinesim-api:2.1.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// The build runs on JDK 17+; the library targets Java 11 (class files major 55).
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xjdk-release=11")
        allWarningsAsErrors.set(true)
    }
}

// JUnit 6 needs Java 17: only the tests (and the docs examples, which include a JUnit test) are compiled for it.
val docsExamples =
    sourceSets.create("docsExamples") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
configurations[docsExamples.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
tasks.withType<KotlinCompile>().matching { it.name != "compileKotlin" }.configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.set(listOf("-Xjdk-release=17"))
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi") // runTest's currentTime
    }
}
tasks.withType<JavaCompile>().matching { it.name != "compileJava" }.configureEach {
    options.release.set(17)
}

// Every ```kotlin block of README.md and docs/content/*.md that uses `client` or has imports is an example and must
// compile against the SDK. A block with a top-level `fun` or `class` is a file; any other one becomes the body of a
// suspend function with `client` and `tzid` declared unless the block declares them. Signature blocks are skipped.
val extractDocsExamples =
    tasks.register("extractDocsExamples") {
        val sources = files("README.md", fileTree("docs/content") { include("*.md") })
        val out = layout.buildDirectory.dir("generated/docsExamples")
        inputs.files(sources)
        outputs.dir(out)
        doLast {
            val dir = out.get().asFile
            dir.deleteRecursively()
            dir.mkdirs()
            val block = Regex("```kotlin[^\\n]*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
            val isExample = Regex("\\bclient\\b|^import ", RegexOption.MULTILINE)
            var n = 0
            sources.files.sortedBy { it.path }.forEach { source ->
                val blocks = block.findAll(source.readText()).map { it.groupValues[1] }
                blocks.filter { isExample.containsMatchIn(it) }.forEach {
                    val (imports, body) = it.lines().partition { line -> line.startsWith("import ") }
                    val code =
                        if (Regex("^(fun|class|suspend fun|@)", RegexOption.MULTILINE).containsMatchIn(it)) {
                            body.joinToString("\n")
                        } else {
                            val params =
                                listOf("client" to "OnlineSim", "tzid" to "Long")
                                    .filterNot { (name, _) -> Regex("\\bval $name\\b").containsMatchIn(it) }
                                    .joinToString { (name, type) -> "$name: $type" }
                            val lines = body.joinToString("\n")
                            "suspend fun example($params) {\n$lines\n}"
                        }
                    val file = dir.resolve("Example$n.kt")
                    file.writeText(
                        "// ${source.name}\n@file:Suppress(\"UNUSED_VARIABLE\", \"UNUSED_PARAMETER\", \"unused\")\n\n" +
                            "package docs.ex$n\n\nimport ru.onlinesim.kotlin.*\n${imports.joinToString(
                                "\n",
                            )}\n\n$code\n",
                    )
                    n++
                }
            }
            check(n > 20) { "found only $n docs examples" }
        }
    }
kotlin.sourceSets.named("docsExamples") { kotlin.srcDir(extractDocsExamples) }
tasks.named("check") { dependsOn(tasks.named("compileDocsExamplesKotlin")) }

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.8.0")
    }
    kotlinGradle {
        ktlint("1.8.0")
    }
}

dokka {
    moduleName.set("onlinesim-kotlin")
    dokkaSourceSets.named("main") {
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/s00d/onlinesim-kotlin-api/tree/main/src/main/kotlin")
        }
    }
}

mavenPublishing {
    configure(
        KotlinJvm(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"), sourcesJar = SourcesJar.Sources()),
    )
    publishToMavenCentral()
    // Signs when a key is configured (see PUBLISHING.md); without one, publishToMavenLocal works unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    coordinates("ru.onlinesim", "onlinesim-kotlin", version.toString())

    pom {
        name.set("onlinesim-kotlin")
        description.set(project.description)
        inceptionYear.set("2026")
        url.set("https://github.com/s00d/onlinesim-kotlin-api")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("s00d")
                name.set("s00d")
                url.set("https://github.com/s00d")
            }
        }
        scm {
            url.set("https://github.com/s00d/onlinesim-kotlin-api")
            connection.set("scm:git:git://github.com/s00d/onlinesim-kotlin-api.git")
            developerConnection.set("scm:git:ssh://git@github.com/s00d/onlinesim-kotlin-api.git")
        }
    }
}
