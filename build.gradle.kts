import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
val containerAlwaysOn: String by project
val disableLint: String by project

plugins {
    jacoco

    id("maven-publish")
    kotlin("jvm") version "1.7.20"

    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("org.owasp.dependencycheck") version "7.2.0"
    id("fr.coppernic.versioning") version "3.2.1"
    id("com.avast.gradle.docker-compose") version "0.16.9"
    id("org.sonarqube") version "+"
}

group = "io.github.flecomte"
version = versioning.info.tag

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
}

tasks.test {
    useJUnit()
    useJUnitPlatform()
    systemProperty("junit.jupiter.execution.parallel.enabled", true)
    if (!disableLint.toBoolean()) {
        finalizedBy(tasks.ktlintCheck)
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
    }
}

tasks.sonarqube.configure {
    dependsOn(tasks.jacocoTestReport)
}

tasks.publishToMavenLocal {
    dependsOn(tasks.test)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.20")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.20")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0-rc1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda:2.14.0-rc1")
    implementation("com.github.jasync-sql:jasync-postgresql:2.1.7")
    implementation("org.slf4j:slf4j-api:2.0.3")

    testImplementation("ch.qos.logback:logback-classic:1.4.3")
    testImplementation("ch.qos.logback:logback-core:1.4.3")
    testImplementation("io.mockk:mockk:1.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.20")
    testImplementation("org.amshove.kluent:kluent:1.68")
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

apply(plugin = "docker-compose")
dockerCompose {
    setProjectName("postgres-json")
    setProperty("useComposeFiles", listOf("docker-compose.yml"))
    setProperty("stopContainers", !containerAlwaysOn.toBoolean())
    isRequiredBy(project.tasks.test)
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

tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf {
        versioning.info.run {
            !dirty && tag != null && tag.matches("""[0-9]+\.[0-9]+\.[0-9]+""".toRegex())
        }
    }

    dependsOn(tasks.test)
}
