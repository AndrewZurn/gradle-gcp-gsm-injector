package com.andrewzurn.gcp.gsm.injector

import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/**
 * Configuration for a single secret to be fetched from Google Secret Manager.
 */
class SecretConfig(name: String, objects: ObjectFactory) : Named {
    private val secretName: String = name

    /**
     * The version of the secret to fetch. Defaults to "latest".
     */
    val version: Property<String> = objects.property(String::class.java).convention("latest")

    /**
     * The environment variable name to inject the secret value into.
     * If not set, no environment variable will be injected.
     */
    val envVar: Property<String> = objects.property(String::class.java)

    /**
     * The system property name to inject the secret value into.
     * If not set, no system property will be injected.
     */
    val systemProperty: Property<String> = objects.property(String::class.java)

    /**
     * Whether to override an existing value for the configured envVar or systemProperty.
     * Defaults to true. If false and the variable already exists, it will be left unchanged.
     */
    val overrideExisting: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    override fun getName(): String = secretName
}
