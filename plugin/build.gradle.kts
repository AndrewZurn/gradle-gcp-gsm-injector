/*
 * Gradle plugin for injecting Google Cloud Secret Manager secrets into task environments.
 */

plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`

    // Apply the Kotlin JVM plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)

    // Publish to Maven repositories (including local)
    `maven-publish`

    // Publish to the Gradle Plugin Portal
    alias(libs.plugins.gradle.plugin.publish)
}

group = "com.andrewzurn"
version = "0.2.0"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Google Cloud Secret Manager client
    implementation(libs.gcp.secretmanager)

    // Use the Kotlin Test integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website = "https://github.com/AndrewZurn/gradle-gcp-gsm-injector"
    vcsUrl = "https://github.com/AndrewZurn/gradle-gcp-gsm-injector"

    // Define the plugin
    val gcpGsmInjector by plugins.creating {
        id = "com.andrewzurn.gcp-gsm-injector"
        implementationClass = "com.andrewzurn.gcp.gsm.injector.GcpGsmInjectorPlugin"
        displayName = "GCP Secret Manager Injector"
        description = "A Gradle plugin that injects secrets from Google Cloud Secret Manager into task environments and system properties."
        tags = listOf("gcp", "secret-manager", "secrets", "environment-variables", "google-cloud")
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}
