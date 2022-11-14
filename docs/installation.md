# Installation

## Gradle
```kotlin
// build.gradle.kts

buildscript {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath("com.github.flecomte:postgres-json:+")
    }
}

repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.flecomte:postgres-json:+")
}
```