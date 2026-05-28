package com.andrewzurn.gcp.gsm.injector

import org.gradle.api.GradleException
import org.gradle.api.tasks.JavaExec
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GcpGsmInjectorPluginTest {

    @Test
    fun `plugin injects secrets into JavaExec task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")

        val mockResolver = MockSecretResolver(mapOf("db-password" to "secret123"))

        val extension = GcpGsmInjectorExtension(project)
        extension.projectId.set("test-project")
        extension.targetTasks.set(listOf("runApp"))
        extension.secrets.create("db-password").apply {
            envVar.set("DB_PASSWORD")
            systemProperty.set("db.password")
        }

        val task = project.tasks.create("runApp", JavaExec::class.java)
        task.mainClass.set("com.example.Main")
        task.classpath = project.files()

        injectSecrets(task, mockResolver, "test-project", true, extension.secrets)

        assertEquals("secret123", task.environment["DB_PASSWORD"])
        assertEquals("secret123", task.systemProperties["db.password"])
    }

    @Test
    fun `plugin skips injection when overrideExisting is false and variable exists`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")

        val mockResolver = MockSecretResolver(mapOf("api-key" to "new-secret"))

        val extension = GcpGsmInjectorExtension(project)
        extension.projectId.set("test-project")
        extension.targetTasks.set(listOf("runApp"))
        extension.overrideExisting.set(false)
        extension.secrets.create("api-key").apply {
            envVar.set("API_KEY")
            overrideExisting.set(false)
        }

        val task = project.tasks.create("runApp", JavaExec::class.java)
        task.mainClass.set("com.example.Main")
        task.classpath = project.files()
        task.environment("API_KEY", "existing-value")

        injectSecrets(task, mockResolver, "test-project", false, extension.secrets)

        assertEquals("existing-value", task.environment["API_KEY"])
    }

    @Test
    fun `plugin overrides existing variable by default`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")

        val mockResolver = MockSecretResolver(mapOf("api-key" to "new-secret"))

        val extension = GcpGsmInjectorExtension(project)
        extension.projectId.set("test-project")
        extension.targetTasks.set(listOf("runApp"))
        extension.secrets.create("api-key").apply {
            envVar.set("API_KEY")
        }

        val task = project.tasks.create("runApp", JavaExec::class.java)
        task.mainClass.set("com.example.Main")
        task.classpath = project.files()
        task.environment("API_KEY", "existing-value")

        injectSecrets(task, mockResolver, "test-project", true, extension.secrets)

        assertEquals("new-secret", task.environment["API_KEY"])
    }

    @Test
    fun `injectSecrets propagates resolver errors`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")

        val mockResolver = MockSecretResolver(emptyMap())

        val extension = GcpGsmInjectorExtension(project)
        extension.secrets.create("missing-secret").apply {
            envVar.set("MISSING")
        }

        val task = project.tasks.create("runApp", JavaExec::class.java)
        task.mainClass.set("com.example.Main")
        task.classpath = project.files()

        val exception = assertFailsWith<GradleException> {
            injectSecrets(task, mockResolver, "test-project", true, extension.secrets)
        }

        assertTrue(exception.message!!.contains("Failed to fetch secret 'missing-secret'"))
    }

    class MockSecretResolver(private val secrets: Map<String, String>) : SecretResolver {
        override fun resolve(projectId: String, secretName: String, version: String): String {
            return secrets[secretName] ?: throw RuntimeException("Secret not found: $secretName")
        }

        override fun close() {}
    }
}
