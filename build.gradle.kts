plugins {
    `java-library`
    `maven-publish`
}

group = "com.github.rschmitt"
version = "1.6.3-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
    api("org.clojure:clojure:[1.6.0,)")
    api("com.github.rschmitt:collider:[0.3.0,)")
    api("org.fressian:fressian:0.6.5")
    api("org.clojure:data.fressian:0.2.0")
    implementation("net.fushizen.invokedynamic.proxy:invokedynamic-proxy:1.2.1")

    testCompileOnly("org.junit.jupiter:junit-jupiter-api:5.5.1")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.5.1")
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

tasks.register<Jar>("sourcesJar") {
    from(sourceSets.main.get().allJava)
    archiveClassifier.set("sources")
}

tasks.register<Jar>("javadocJar") {
    from(tasks["javadoc"])
    archiveClassifier.set("javadoc")
}

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
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}