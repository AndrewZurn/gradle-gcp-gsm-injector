package com.andrewzurn.gcp.gsm.injector

import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.process.JavaForkOptions

/**
 * Gradle plugin that injects secrets from Google Cloud Secret Manager into
 * task environments and system properties.
 *
 * Users must explicitly configure which tasks should receive secrets via
 * the [GcpGsmInjectorExtension.targetTasks] property.
 */
open class GcpGsmInjectorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "gcpGsmInjector",
            GcpGsmInjectorExtension::class.java,
            project
        )

        project.afterEvaluate {
            val targetTasks = extension.targetTasks.get()
            if (targetTasks.isEmpty()) {
                project.logger.warn("No target tasks configured for gcpGsmInjector")
                return@afterEvaluate
            }

            targetTasks.forEach { taskName ->
                val task = project.tasks.findByName(taskName)
                if (task == null) {
                    project.logger.error("Target task '$taskName' not found")
                    return@forEach
                }

                if (task !is JavaExec && task !is Test) {
                    project.logger.error(
                        "Target task '$taskName' must be a JavaExec or Test task, but was ${task.javaClass.name}"
                    )
                    return@forEach
                }

                task.doFirst {
                    val projectId = extension.projectId.orNull ?: run {
                        throw GradleException("gcpGsmInjector.projectId must be set")
                    }

                    val resolver = createResolver()
                    try {
                        injectSecrets(
                            task = task as JavaForkOptions,
                            resolver = resolver,
                            projectId = projectId,
                            globalOverride = extension.overrideExisting.getOrElse(true),
                            secrets = extension.secrets
                        )
                    } finally {
                        resolver.close()
                    }
                }
            }
        }
    }

    internal open fun createResolver(): SecretResolver = DefaultSecretResolver()
}

/**
 * Injects secrets into a [JavaForkOptions] task by fetching them from Secret Manager.
 */
internal fun injectSecrets(
    task: JavaForkOptions,
    resolver: SecretResolver,
    projectId: String,
    globalOverride: Boolean,
    secrets: NamedDomainObjectContainer<SecretConfig>
) {
    secrets.forEach { secret ->
        val version = secret.version.get()
        val secretValue = try {
            resolver.resolve(projectId, secret.getName(), version)
        } catch (e: GradleException) {
            throw e
        } catch (e: Exception) {
            throw GradleException(
                "Failed to fetch secret '${secret.getName()}' version '$version': ${e.message}",
                e
            )
        }

        val override = secret.overrideExisting.getOrElse(globalOverride)

        secret.envVar.orNull?.let { envVarName ->
            if (override || !task.environment.containsKey(envVarName)) {
                task.environment(envVarName, secretValue)
            }
        }

        secret.systemProperty.orNull?.let { sysPropName ->
            if (override || task.systemProperties[sysPropName] == null) {
                task.systemProperty(sysPropName, secretValue)
            }
        }
    }
}
