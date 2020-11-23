plugins {
    kotlin("jvm").version("1.4.10")
    java

}

repositories {
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.10")

    testImplementation(kotlin("test-common"))
    testImplementation(kotlin("test-annotations-common"))
    testImplementation(kotlin("test-junit5"))

    testImplementation("org.springframework.boot:spring-boot-starter-test:2+")

}

tasks {
    test {
        useJUnitPlatform()
    }
}
