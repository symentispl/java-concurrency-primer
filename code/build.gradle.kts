plugins {
    id("java")
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile> {
    options.release.set(25)
}
