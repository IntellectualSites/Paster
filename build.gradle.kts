import org.cadixdev.gradle.licenser.LicenseExtension

plugins {
    java
    `java-library`
    `maven-publish`
    signing

    id("org.cadixdev.licenser") version "0.6.1"
}

the<JavaPluginExtension>().toolchain {
    languageVersion.set(JavaLanguageVersion.of(16))
}

group = "com.intellectualsites.paster"
version = "1.1.2"
var versuffix by extra("SNAPSHOT")
version = if (!project.hasProperty("release")) {
    String.format("%s-%s", project.version, versuffix)
} else {
    String.format(project.version as String)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnlyApi("com.google.code.gson:gson:2.8.0")
    compileOnlyApi("com.google.guava:guava:21.0")
    compileOnlyApi("com.google.code.findbugs:jsr305:3.0.2")
}

configure<LicenseExtension> {
    header.set(resources.text.fromFile(file("HEADER.txt")))
    newLine.set(false)
}

tasks {

    compileJava {
        options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "1000"))
        options.compilerArgs.add("-Xlint:all")
        for (disabledLint in arrayOf("processing", "path", "fallthrough", "serial"))
            options.compilerArgs.add("-Xlint:$disabledLint")
        options.isDeprecation = true
        options.encoding = "UTF-8"
        options.release.set(11)
    }

    javadoc {
        title = project.name + " " + project.version
        val opt = options as StandardJavadocDocletOptions
        opt.addStringOption("Xdoclint:none", "-quiet")
        opt.tags(
                "apiNote:a:API Note:",
                "implSpec:a:Implementation Requirements:",
                "implNote:a:Implementation Note:"
        )
        opt.links("https://javadoc.io/doc/com.google.code.findbugs/jsr305/3.0.2/")
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

signing {
    if (!version.toString().endsWith("-SNAPSHOT")) {
        signing.isRequired
        sign(publishing.publications)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {

                name.set(project.name + " " + project.version)
                description.set("A library focused on collecting and assembling debug data provided from the jvm as json.")
                url.set("https://github.com/IntellectualSites/Paster")

                licenses {
                    license {
                        name.set("GNU General Public License, Version 3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("Sauilitired")
                        name.set("Alexander Söderberg")
                    }
                    developer {
                        id.set("NotMyFault")
                        name.set("NotMyFault")
                    }
                    developer {
                        id.set("SirYwell")
                        name.set("Hannes Greule")
                    }
                    developer {
                        id.set("dordsor21")
                        name.set("dordsor21")
                    }
                }

                scm {
                    url.set("https://github.com/IntellectualSites/Paster")
                    connection.set("scm:https://IntellectualSites@github.com/IntellectualSites/Paster.git")
                    developerConnection.set("scm:git://github.com/IntellectualSites/Paster.git")
                }

                issueManagement{
                    system.set("GitHub")
                    url.set("https://github.com/IntellectualSites/Paster/issues")
                }
            }
        }
    }

    repositories {
        mavenLocal()
        val nexusUsername: String? by project
        val nexusPassword: String? by project
        if (nexusUsername != null && nexusPassword != null) {
            maven {
                val releasesRepositoryUrl  = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotRepositoryUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                url = uri(
                        if (version.toString().endsWith("-SNAPSHOT")) snapshotRepositoryUrl
                        else releasesRepositoryUrl
                )

                credentials {
                    username = nexusUsername
                    password = nexusPassword
                }
            }
        } else {
            logger.warn("No nexus repository is added; nexusUsername or nexusPassword is null.")
        }
    }
}
