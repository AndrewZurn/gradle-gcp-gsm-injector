# gcp-gsm-injector

A Gradle plugin that injects secrets from **Google Cloud Secret Manager** into the local environments and system properties at runtime. This eliminates the need to hardcode secrets or manually configure them on every developer machine when developing/running an application locally, ensuring a smooth development experience from cloning to running the application.

## Prerequisites

- A GCP project with [Secret Manager](https://cloud.google.com/secret-manager) enabled
- [Application Default Credentials (ADC)](https://cloud.google.com/docs/authentication/application-default-credentials) configured on your machine:
  ```bash
  gcloud auth application-default login
  ```

## Quick Start

### 1. Publish locally

From the root of this repo, run:

```bash
./gradlew publishToMavenLocal
```

This installs the plugin to your local Maven repository (`~/.m2/repository`).

### 2. Use in another project

Add the plugin to your application's `settings.gradle.kts` (or `settings.gradle`):

```kotlin
pluginManagement {
    repositories {
        mavenLocal() // Required to find the locally published plugin
        gradlePluginPortal()
        mavenCentral()
    }
}
```

Then apply it in your application's `build.gradle.kts`:

```kotlin
plugins {
    id("com.andrewzurn.gcp-gsm-injector") version "0.1.0-SNAPSHOT"
}

gcpGsmInjector {
    projectId.set("my-gcp-project")
    targetTasks.set(listOf("bootRun", "test"))

    secrets {
        register("db-password") {
            envVar.set("DB_PASSWORD")
            systemProperty.set("db.password")
        }
        register("stripe-api-key") {
            version.set("5")
            envVar.set("STRIPE_API_KEY")
        }
    }
}
```

When you run `./gradlew bootRun` (for a spring boot app) or `./gradlew test` (or whatever tasks you configure this to target via `targetTasks`), the plugin will fetch the secrets from GSM and inject them before the task executes.

## Configuration Reference

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `projectId` | **Yes** | — | The GCP project ID containing your secrets. |
| `targetTasks` | **Yes** | `[]` | List of task names to inject secrets into. Only `JavaExec` and `Test` tasks are supported. |
| `overrideExisting` | No | `true` | Whether fetched secrets should override existing env vars / system properties. |
| `secrets` | No | `[]` | Named block of secrets to fetch. |

### Secret block properties

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `version` | No | `latest` | The secret version to fetch. If omitted, `latest` is used automatically. |
| `envVar` | No | — | Environment variable name to inject the secret into. |
| `systemProperty` | No | — | System property name to inject the secret into. |
| `overrideExisting` | No | `true` | Per-secret override of the global `overrideExisting` flag. |

## Error Handling

The plugin provides clear error messages for common failures:

- **ADC not configured** — `Failed to authenticate with Google Cloud Secret Manager. Ensure Application Default Credentials (ADC) are configured...`
- **Permission denied** — `Permission denied accessing secret 'X'. Ensure the authenticated account has 'Secret Manager Secret Accessor' role.`
- **Secret not found** — `Secret 'X' version 'Y' not found in project 'Z'.`
- **Invalid task type** — `Target task 'X' must be a JavaExec or Test task...`

## Future Enhancements / Todos

- [ ] **Local caching** — Add an optional local file cache (e.g., under `build/`) so secrets can be reused across consecutive task runs without hitting GSM every time. Include an `--offline` or `cache.enabled` toggle.
- [ ] **Secret rotation / TTL** — Support automatic re-fetching when a secret version is rotated.
