plugins {
    id("maven-publish")
    id("org.jetbrains.kotlin.jvm") version "1.3.31"
}

apply(plugin = "kotlin")

group = "fr.postgresjson"
version = "0.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.31")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.9")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda:2.9.9")
    implementation("com.github.jasync-sql:jasync-postgresql:0.9.53")
    implementation("org.slf4j:slf4j-api:1.7.26")

    testImplementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("ch.qos.logback:logback-core:1.2.3")
    testImplementation("io.mockk:mockk:1.9")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.amshove.kluent:kluent:1.47")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "fr.postgresjson"
            artifactId = "postgresjson"
            version = "0.1"

            from(components["java"])
        }
    }
}
