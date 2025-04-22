import com.diffplug.gradle.spotless.SpotlessPlugin
import com.vanniktech.maven.publish.SonatypeHost
import java.net.URI

plugins {
    java
    `java-library`
    signing

    alias(libs.plugins.spotless)
    alias(libs.plugins.publish)

    idea
    eclipse
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.compileJava.configure {
    options.release.set(11)
}

configurations.all {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
}

group = "com.intellectualsites.paster"
version = "1.1.8-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("com.intellectualsites.bom:bom-1.16.x:1.52"))
    compileOnly("com.google.code.gson:gson")
    compileOnly("com.google.guava:guava")
    compileOnly(libs.findbugs)
}

spotless {
    java {
        licenseHeaderFile(rootProject.file("HEADER.txt"))
        target("**/*.java")
    }
}

tasks {

    javadoc {
        val opt = options as StandardJavadocDocletOptions
        opt.links("https://javadoc.io/doc/com.google.code.findbugs/jsr305/3.0.2/")
        opt.noTimestamp()
    }

    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

signing {
    if (!project.hasProperty("skip.signing") && !version.toString().endsWith("-SNAPSHOT")) {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
        signing.isRequired
        sign(publishing.publications)
    }
}

mavenPublishing {
    coordinates(
            groupId = "$group",
            artifactId = project.name,
            version = "${project.version}",
    )

    pom {

        name.set(project.name)
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
                organization.set("IntellectualSites")
                organizationUrl.set("https://github.com/IntellectualSites/")
            }
            developer {
                id.set("NotMyFault")
                name.set("Alexander Brandes")
                organization.set("IntellectualSites")
                email.set("contact(at)notmyfault.dev")
            }
            developer {
                id.set("SirYwell")
                name.set("Hannes Greule")
                organization.set("IntellectualSites")
                organizationUrl.set("https://github.com/IntellectualSites/")
            }
            developer {
                id.set("dordsor21")
                name.set("dordsor21")
                organization.set("IntellectualSites")
                organizationUrl.set("https://github.com/IntellectualSites/")
            }
        }

        scm {
            url.set("https://github.com/IntellectualSites/Paster")
            connection.set("scm:git:https://github.com/IntellectualSites/Paster.git")
            developerConnection.set("scm:git:git@github.com:IntellectualSites/Paster.git")
            tag.set("${project.version}")
        }

        issueManagement{
            system.set("GitHub")
            url.set("https://github.com/IntellectualSites/Paster/issues")
        }

        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    }
}
