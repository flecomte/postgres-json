import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    jacoco

    id("maven-publish")
    id("org.jetbrains.kotlin.jvm") version "1.3.50"

    id("org.jlleitschuh.gradle.ktlint") version "8.2.0"
    id("org.owasp.dependencycheck") version "5.1.0"
    id("fr.coppernic.versioning") version "3.1.2"
}

group = "flecomte"
version = versioning.info.tag

repositories {
    mavenCentral()
    jcenter()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.31")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.9")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda:2.9.9")
    implementation("com.github.jasync-sql:jasync-postgresql:1.0.7")
    implementation("org.slf4j:slf4j-api:1.7.26")

    testImplementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("ch.qos.logback:logback-core:1.2.3")
    testImplementation("io.mockk:mockk:1.9")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.amshove.kluent:kluent:1.47")
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    repositories {
        maven {
            name = "postgres-json"
            url = uri("https://maven.pkg.github.com/flecomte/postgres-json")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("postgres-json") {
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}