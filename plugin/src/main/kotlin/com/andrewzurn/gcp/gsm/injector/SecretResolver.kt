package com.andrewzurn.gcp.gsm.injector

import com.google.api.gax.rpc.NotFoundException
import com.google.api.gax.rpc.PermissionDeniedException
import com.google.api.gax.rpc.UnauthenticatedException
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretVersionName
import org.gradle.api.GradleException

/**
 * Resolves secrets from Google Cloud Secret Manager.
 */
interface SecretResolver {
    /**
     * Fetches the value of a secret from Secret Manager.
     *
     * @param projectId The GCP project ID
     * @param secretName The name of the secret
     * @param version The version of the secret to fetch
     * @return The secret value as a UTF-8 string
     */
    fun resolve(projectId: String, secretName: String, version: String): String

    /**
     * Releases any resources held by the resolver.
     */
    fun close()
}

/**
 * Default implementation of [SecretResolver] using the Google Cloud Secret Manager client.
 * Uses Application Default Credentials (ADC) for authentication.
 */
class DefaultSecretResolver : SecretResolver {
    private val client: SecretManagerServiceClient by lazy {
        try {
            SecretManagerServiceClient.create()
        } catch (e: Exception) {
            throw GradleException(
                "Failed to create Secret Manager client. Ensure Application Default Credentials (ADC) are configured. Error: ${e.message}",
                e
            )
        }
    }

    override fun resolve(projectId: String, secretName: String, version: String): String {
        val secretVersionName = SecretVersionName.of(projectId, secretName, version)
        return try {
            val response = client.accessSecretVersion(secretVersionName)
            response.payload.data.toStringUtf8()
        } catch (e: UnauthenticatedException) {
            throw GradleException(
                "Failed to authenticate with Google Cloud Secret Manager. Ensure Application Default Credentials (ADC) are configured (e.g., via `gcloud auth application-default login` or a service account).",
                e
            )
        } catch (e: PermissionDeniedException) {
            throw GradleException(
                "Permission denied accessing secret '$secretName'. Ensure the authenticated account has 'Secret Manager Secret Accessor' role.",
                e
            )
        } catch (e: NotFoundException) {
            throw GradleException(
                "Secret '$secretName' version '$version' not found in project '$projectId'.",
                e
            )
        } catch (e: Exception) {
            throw GradleException(
                "Failed to access secret '$secretName': ${e.message}",
                e
            )
        }
    }

    override fun close() {
        client.close()
    }
}
