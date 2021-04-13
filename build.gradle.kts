import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Base64

plugins {
    id("com.palantir.git-version") version "0.12.3"
    id("org.jetbrains.kotlin.jvm") version "1.4.20"
    id("org.jetbrains.dokka") version "1.4.10.2"
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
    `maven-publish`
    `java-library`
    signing
    jacoco
}
val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val details = versionDetails()

version = details.version.removeSuffix(".dirty")
group = "io.github.night-crawler"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("http://dl.bintray.com/kotlin/ktor") }
    maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
}

val signingKeyId: String? = System.getenv("SIGN_KEY_ID")
val signingKey: String? = System.getenv("SIGN_KEY")
    ?.takeIf { it.isNotBlank() }
    ?: System.getenv("SIGN_KEY_B64")?.let {
        String(Base64.getDecoder().decode(it))
    }
val signingKeyPassphrase: String? = System.getenv("SIGN_KEY_PASSPHRASE")
val signingOptions = listOf(signingKeyId, signingKey, signingKeyPassphrase)
val isSigningEnabled = signingOptions.all { !it.isNullOrBlank() }
if (!isSigningEnabled) {
    logger.warn("Signing is disabled because some of the SIGN_KEY_ID, SIGN_KEY, SIGN_KEY_PASSPHRASE was blank")
}

logger.info(
    "SIGN_KEY_ID(length={}), SIGN_KEY(length={}), SIGN_KEY_PASSPHRASE(length={})",
    signingKeyId?.length ?: 0, signingKey?.length ?: 0, signingKeyPassphrase?.length ?: 0
)

val sonatypeUser: String? = System.getenv("SONATYPE_USER")
val sonatypePassword: String? = System.getenv("SONATYPE_PASSWORD")
val isRemoteRepositoryEnabled = listOf(sonatypeUser, sonatypePassword).all { !it.isNullOrBlank() }
if (!isRemoteRepositoryEnabled) {
    logger.warn("Remote repository is disabled because SONATYPE_USER or SONATYPE_PASSWORD was blank")
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(sonatypeUser)
            password.set(sonatypePassword)
        }
    }
}

subprojects {
    apply<JacocoPlugin>()
    apply<JavaLibraryPlugin>()
    apply<MavenPublishPlugin>()
    apply<SigningPlugin>()
    apply<org.jetbrains.dokka.gradle.DokkaPlugin>()

    repositories {
        jcenter()
        mavenCentral()
        maven { url = uri("http://dl.bintray.com/kotlin/ktor") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
    }

    java {
        // withJavadocJar()
        withSourcesJar()
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    val dokkaOutputDir = "$buildDir/dokka"

    tasks {
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        javadoc {
            if (JavaVersion.current().isJava9Compatible) {
                (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
            }
        }
        dokkaHtml {
            outputDirectory.set(file(dokkaOutputDir))
        }
        create<JacocoReport>("codeCoverageReport") {
            executionData(fileTree(project.rootDir.absolutePath).include("**/build/jacoco/*.exec"))

            sourceSets(sourceSets["main"])

            reports {
                xml.isEnabled = true
                xml.destination = file("$buildDir/reports/jacoco/report.xml")
                html.isEnabled = true
                csv.isEnabled = false
            }

            dependsOn(project.getTasksByName("test", false))
        }
        withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
            dokkaSourceSets {
                configureEach {
                    includes.from("Module.md")
                }
            }
        }
    }

    val javadocJar by tasks.registering(Jar::class) {
        dependsOn(tasks.dokkaHtml)
        archiveClassifier.set("javadoc")
        from(dokkaOutputDir)
    }

    publishing {
        publications {
            val lib = create<MavenPublication>(project.name) {
                from(components["java"])
                artifactId = project.name
                groupId = rootProject.group as String
                version = rootProject.version as String

                artifact(javadocJar.get())

                pom {
                    name.set("Collection extensions")
                    description.set(project.description)
                    url.set("https://github.com/night-crawler/night-crawler-libs")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/licenses/mit-license.php")
                        }
                    }
                    developers {
                        developer {
                            id.set("night-crawler")
                            name.set("Igor Kalishevsky")
                            email.set("lilo.panic@gmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/night-crawler/night-crawler-libs.git")
                        developerConnection.set("scm:git:ssh://github.com/night-crawler/night-crawler-libs.git")
                        url.set("https://github.com/night-crawler/night-crawler-libs")
                    }
                }
            }
            if (isSigningEnabled) {
                signing {
                    useInMemoryPgpKeys(signingKeyId, signingKey, signingKeyPassphrase)
                    sign(lib)
                }
            }
        }
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>().configureEach {
    if (!name.contains("html", ignoreCase = true)) return@configureEach

    val docs = projectDir.resolve("$buildDir/docs")
    outputDirectory.set(docs)
    doLast {
        docs.resolve("-modules.html").renameTo(docs.resolve("index.html"))
    }
}

tasks.register<JacocoReport>("jacocoRootReport") {
    subprojects {
        this@subprojects.plugins.withType<JacocoPlugin>().configureEach {
            this@subprojects.tasks
                .matching {
                    it.extensions.findByType<JacocoTaskExtension>() != null
                }
                .configureEach {
                    sourceSets(this@subprojects.the<SourceSetContainer>().named("main").get())
                    executionData(this)
                }
        }
    }

    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
}
