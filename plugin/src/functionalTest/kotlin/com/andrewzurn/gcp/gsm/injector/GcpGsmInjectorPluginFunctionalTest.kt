package com.andrewzurn.gcp.gsm.injector

import java.io.File
import kotlin.test.assertTrue
import kotlin.test.Test
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

class GcpGsmInjectorPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    @Test
    fun `can apply plugin and configure extension`() {
        settingsFile.writeText("""
            rootProject.name = "test-project"
        """.trimIndent())

        buildFile.writeText("""
            plugins {
                id("com.andrewzurn.gcp-gsm-injector")
            }

            gcpGsmInjector {
                projectId.set("my-project")
                targetTasks.set(listOf("test"))
                secrets {
                    register("db-password") {
                        envVar.set("DB_PASSWORD")
                        systemProperty.set("db.password")
                    }
                }
            }
        """.trimIndent())

        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("help")
            .withProjectDir(projectDir)

        val result = runner.build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun `logs error when target task is not JavaExec or Test`() {
        settingsFile.writeText("""
            rootProject.name = "test-project"
        """.trimIndent())

        buildFile.writeText("""
            plugins {
                id("com.andrewzurn.gcp-gsm-injector")
                java
            }

            gcpGsmInjector {
                projectId.set("my-project")
                targetTasks.set(listOf("compileJava"))
                secrets {
                    register("secret") {
                        envVar.set("SECRET")
                    }
                }
            }
        """.trimIndent())

        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("help")
            .withProjectDir(projectDir)

        val result = runner.build()

        assertTrue(result.output.contains("must be a JavaExec or Test task"))
    }
}
