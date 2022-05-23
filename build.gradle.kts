plugins {
    java
}

group = "org.semgus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io") { name = "Jitpack" }
}

dependencies {
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    implementation("com.github.SemGuS-git:Semgus-Java:1.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
