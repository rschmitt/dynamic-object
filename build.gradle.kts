plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "com.github.rschmitt"
version = "1.7.3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api("org.clojure:clojure:[1.6.0,)")
    api("com.github.rschmitt:collider:1.0.0")
    api("org.fressian:fressian:0.6.8")
    api("org.clojure:data.fressian:1.1.0")
    implementation("org.ow2.asm:asm:7.1")

    testCompileOnly("org.junit.jupiter:junit-jupiter-api:5.+")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.+")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("collection-check:collection-check:0.1.6")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("benchmark")
    }
}

tasks.register<Test>("benchmark") {
    useJUnitPlatform {
        includeTags("benchmark")
    }
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    options.showFromPublic()
    exclude("com/github/rschmitt/dynamicobject/internal/**")
}

val sonatypeUsername: String? by project
val sonatypePassword: String? by project

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://clojars.org/repo")
    }
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("dynamic-object")
                description.set("Lightweight data modeling for Java, powered by Clojure.")
                url.set("https://github.com/rschmitt/dynamic-object")
                licenses {
                    license {
                        name.set("CC0")
                        url.set("http://creativecommons.org/publicdomain/zero/1.0/")
                    }
                }
                developers {
                    developer {
                        id.set("rschmitt")
                        name.set("Ryan Schmitt")
                        email.set("rschmitt@pobox.com")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:rschmitt/dynamic-object.git")
                    developerConnection.set("scm:git:git@github.com:rschmitt/dynamic-object.git")
                    url.set("git@github.com:rschmitt/dynamic-object.git")
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
    options.release.set(8)
}

defaultTasks("build")
