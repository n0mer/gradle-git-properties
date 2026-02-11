# Gradle Git Properties Plugin

[![Build Status](https://github.com/n0mer/gradle-git-properties/actions/workflows/build.yml/badge.svg)](https://github.com/n0mer/gradle-git-properties/actions/workflows/build.yml)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.gorylenko.gradle-git-properties)](https://plugins.gradle.org/plugin/com.gorylenko.gradle-git-properties)

A Gradle plugin that generates a `git.properties` file containing Git repository metadata at build time.

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [Kotlin DSL](#kotlin-dsl)
- [Spring Boot Integration](#spring-boot-integration)
- [Advanced Usage](#advanced-usage)
- [Compatibility](#compatibility)
- [License](#license)

## Requirements

- Java 8 or higher
- Gradle 5.1 or higher
- A Git repository (`.git` directory)

## Installation

Add the plugin to your build file:

**Groovy DSL** (`build.gradle`)
```groovy
plugins {
    id "com.gorylenko.gradle-git-properties" version "2.5.6"
}
```

**Kotlin DSL** (`build.gradle.kts`)
```kotlin
plugins {
    id("com.gorylenko.gradle-git-properties") version "2.5.6"
}
```

The plugin generates `git.properties` at `build/resources/main/git.properties`. For Java projects, generation occurs automatically during the build. For non-Java projects, run the task explicitly:

```bash
./gradlew generateGitProperties
```

## Configuration

All configuration is optional. The plugin uses sensible defaults.

### Output Location

Customize the output file name and directory:

```groovy
gitProperties {
    gitPropertiesName = "git-info.properties"
    gitPropertiesResourceDir = file("${project.rootDir}/src/main/resources")
}
```

### Date Format

Configure the format for `git.commit.time` using [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html) patterns and [TimeZone](https://docs.oracle.com/javase/8/docs/api/java/util/TimeZone.html) IDs:

```groovy
gitProperties {
    dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
    dateFormatTimeZone = "UTC"
}
```

### Available Properties

By default, the plugin generates all available properties:

| Property | Description |
|----------|-------------|
| `git.branch` | Current branch name |
| `git.commit.id` | Full 40-character commit SHA |
| `git.commit.id.abbrev` | Abbreviated commit SHA (typically 7 characters) |
| `git.commit.id.describe` | Human-readable name from `git describe` |
| `git.commit.time` | Commit timestamp |
| `git.commit.message.short` | Commit message (first line) |
| `git.commit.message.full` | Commit message (full text) |
| `git.commit.user.name` | Commit author name |
| `git.commit.user.email` | Commit author email |
| `git.build.host` | Hostname of the build machine |
| `git.build.user.name` | Name of the user running the build |
| `git.build.user.email` | Email of the user running the build |
| `git.build.version` | Project version (`project.version`) |
| `git.dirty` | `true` if working tree has uncommitted changes |
| `git.tags` | Tags pointing to the current commit |
| `git.closest.tag.name` | Name of the nearest ancestor tag |
| `git.closest.tag.commit.count` | Number of commits since the nearest tag |
| `git.remote.origin.url` | URL of the remote origin |
| `git.total.commit.count` | Total number of commits in the repository |

To generate only specific properties, use the `keys` option:

```groovy
gitProperties {
    keys = ['git.branch', 'git.commit.id', 'git.commit.time']
}
```

### Custom Properties

Add custom properties using static values or closures. Closures receive a [Grgit](https://ajoberstar.org/grgit/) instance for accessing Git data:

```groovy
gitProperties {
    customProperty 'greeting', 'Hello'
    customProperty 'my_custom_git_id', { it.head().id }
    customProperty 'project_version', { project.version }
}
```

You can also override standard properties. This example includes lightweight tags in `git.commit.id.describe`:

```groovy
gitProperties {
    customProperty 'git.commit.id.describe', { it.describe(tags: true) }
}
```

### Branch Name

Override the detected branch name. This is useful in CI environments where builds run in detached HEAD state:

```groovy
gitProperties {
    branch = System.getenv('BRANCH_NAME')
}
```

The plugin automatically detects branch names from these CI environments:

- GitHub Actions
- GitLab CI
- Jenkins
- CircleCI
- Travis CI
- Azure DevOps
- Bitbucket Pipelines
- Bamboo
- AWS CodeBuild

### Git Directory Location

Specify a custom `.git` directory location:

```groovy
gitProperties {
    dotGitDirectory = layout.projectDirectory.dir("../.git")
}
```

To suppress errors when the `.git` directory is missing:

```groovy
gitProperties {
    failOnNoGitDirectory = false
}
```

### Disabling the Plugin

To disable `git.properties` generation:

```groovy
tasks.withType(com.gorylenko.GenerateGitPropertiesTask).configureEach {
    enabled = false
}
```

## Kotlin DSL

Basic configuration:

```kotlin
gitProperties {
    dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
    dateFormatTimeZone = "UTC"
    keys = listOf("git.branch", "git.commit.id", "git.commit.time")
}
```

For custom properties with closures, use `KotlinClosure1`. The closure receiver is a [Grgit](https://ajoberstar.org/grgit/) instance:

```kotlin
import org.gradle.kotlin.dsl.KotlinClosure1
import gradlegitproperties.org.ajoberstar.grgit.Grgit

gitProperties {
    customProperty("greeting", "Hello")
    customProperty("my_custom_git_id", KotlinClosure1<Grgit, String>({ head().id }))
}
```

## Spring Boot Integration

The plugin integrates with [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html). The `/info` endpoint automatically includes Git information when `git.properties` is present on the classpath.

By default, Spring Boot exposes only `git.branch`, `git.commit.id`, and `git.commit.time`. To expose all properties, add to `application.properties`:

```properties
management.info.git.mode=full
```

<details>
<summary>Example response from /actuator/info</summary>

**Default mode:**
```json
{
  "git": {
    "commit": {
      "time": "2018-03-28T05:13:53Z",
      "id": "32ff212"
    },
    "branch": "Fix_issue_68"
  }
}
```

**Full mode** (`management.info.git.mode=full`):
```json
{
  "git": {
    "build": {
      "host": "myserver-1",
      "version": "0.0.1-SNAPSHOT",
      "user": {
        "name": "First Last",
        "email": "username1@example.com"
      }
    },
    "branch": "Fix_issue_68",
    "commit": {
      "message": {
        "short": "Fix issue #68",
        "full": "Fix issue #68"
      },
      "id": {
        "describe": "v1.4.21-28-g32ff212-dirty",
        "abbrev": "32ff212",
        "full": "32ff212b9e2873fa4672f1b5dd41f67aca6e0731"
      },
      "time": "2018-03-28T05:13:53Z",
      "user": {
        "email": "username1@example.com",
        "name": "First Last"
      }
    },
    "closest": {
      "tag": {
        "name": "v1.4.21",
        "commit": {
          "count": "28"
        }
      }
    },
    "dirty": "true",
    "remote": {
      "origin": {
        "url": "git@github.com:n0mer/gradle-git-properties.git"
      }
    },
    "tags": "",
    "total": {
      "commit": {
        "count": "93"
      }
    }
  }
}
```

</details>

## Advanced Usage

### Accessing Properties at Build Time

Use `extProperty` to expose generated properties to other build tasks. This enables use cases such as embedding the Git commit ID in JAR manifests.

```groovy
gitProperties {
    extProperty = 'gitProps'
}

// Ensure properties are always regenerated
generateGitProperties.outputs.upToDateWhen { false }

bootJar {
    dependsOn generateGitProperties
    manifest {
        // Use lazy GString evaluation to defer property access
        attributes('Git-Commit': "${-> project.ext.gitProps['git.commit.id.abbrev']}")
    }
}
```

## Compatibility

| Plugin Version | Gradle | Java |
|----------------|--------|------|
| 2.5.x          | 5.1 – 9.x | 8+ |

The plugin supports Gradle [configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html).

## License

This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

---

*Originally inspired by [@lievendoclo](https://github.com/lievendoclo)'s article "Spring Boot's info endpoint, Git and Gradle" (2014).*
