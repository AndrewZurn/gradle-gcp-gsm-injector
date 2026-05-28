package com.andrewzurn.gcp.gsm.injector

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Extension DSL for the gcp-gsm-injector plugin.
 */
open class GcpGsmInjectorExtension(project: Project) {
    /**
     * The GCP project ID where secrets are stored.
     */
    val projectId: Property<String> = project.objects.property(String::class.java)

    /**
     * Global default for whether to override existing environment variables or system properties.
     * Defaults to true.
     */
    val overrideExisting: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)

    /**
     * The names of tasks that should have secrets injected before execution.
     * Only JavaExec and Test tasks are supported.
     */
    val targetTasks: ListProperty<String> = project.objects.listProperty(String::class.java)

    /**
     * Container of secret configurations to fetch and inject.
     */
    val secrets: NamedDomainObjectContainer<SecretConfig> = project.objects.domainObjectContainer(SecretConfig::class.java) { name ->
        SecretConfig(name, project.objects)
    }
}
