# Gradle Git Properties Plugin

[![Build Status](https://github.com/n0mer/gradle-git-properties/actions/workflows/build.yml/badge.svg)](https://github.com/n0mer/gradle-git-properties/actions/workflows/build.yml)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.gorylenko.gradle-git-properties)](https://plugins.gradle.org/plugin/com.gorylenko.gradle-git-properties)

A Gradle plugin that generates a `git.properties` file containing Git repository metadata at build time.

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [Spring Boot Integration](#spring-boot-integration)
- [Advanced Usage](#advanced-usage)
- [Compatibility](#compatibility)
- [Migration Guide](#migration-guide)
- [License](#license)

## Requirements

- Java 17 or higher
- Gradle 5.1 – 9.x
- A Git repository (`.git` directory or git worktree)

## Installation

Add the plugin to your build file:

**Groovy DSL** (`build.gradle`)
```groovy
plugins {
    id "com.gorylenko.gradle-git-properties" version "4.0.1"
}
```

**Kotlin DSL** (`build.gradle.kts`)
```kotlin
plugins {
    id("com.gorylenko.gradle-git-properties") version "4.0.1"
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

> **Note:** The older `gitPropertiesDir` property is deprecated. Replace it with `gitPropertiesResourceDir`.

### Commit Timestamp Format

Configure the format and timezone for `git.commit.time` using [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html) patterns and [TimeZone](https://docs.oracle.com/javase/8/docs/api/java/util/TimeZone.html) IDs.

By default (no configuration), `git.commit.time` is formatted as `yyyy-MM-dd'T'HH:mm:ssZ` using the local system timezone (RFC 822 numeric offset, e.g. `+0300`).

> **Note:** `Z` pattern (RFC 822 numeric offset) produces `+0000` for UTC, not a literal `Z`. Use `XXX` (ISO 8601) to get `Z` for UTC and offsets like `+05:30` for other timezones.

> **Warning:** An unrecognized `dateFormatTimeZone` ID silently falls back to UTC. Verify IDs against [Java's supported timezone IDs](https://docs.oracle.com/javase/8/docs/api/java/util/TimeZone.html#getAvailableIDs--).

| Goal | `dateFormat` | `dateFormatTimeZone` | Example output |
|------|-------------|----------------------|----------------|
| Default (no config) | `yyyy-MM-dd'T'HH:mm:ssZ` | *(local timezone)* | `2024-03-20T08:13:53+0300` |
| RFC 822 numeric offset, UTC | `yyyy-MM-dd'T'HH:mm:ssZ` | `UTC` | `2024-03-20T05:13:53+0000` |
| ISO 8601 with `Z` suffix | `yyyy-MM-dd'T'HH:mm:ssXXX` | `UTC` | `2024-03-20T05:13:53Z` |
| ISO 8601 with local offset | `yyyy-MM-dd'T'HH:mm:ssXXX` | *(local timezone)* | `2024-03-20T08:13:53+03:00` |
| Epoch seconds | `""` (empty string — bypasses SimpleDateFormat) | *(not set)* | `1710904433` |

### Commit ID Abbreviation Length

Configure the length of `git.commit.id.abbrev` (default: 7, range: 2-40):

```groovy
gitProperties {
    commitIdAbbrevLength = 10
}
```

### Available Properties

By default, the plugin generates all available properties:

| Property | Description |
|----------|-------------|
| `git.branch` | Current branch name |
| `git.commit.id` | Full 40-character commit SHA |
| `git.commit.id.abbrev` | Abbreviated commit SHA (default 7 characters, configurable) |
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

Add custom properties using static values or closures. Closures receive a `GitFacade` instance for accessing Git data:

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

#### GitFacade API

The `GitFacade` class provides these methods for custom properties:

| Method | Returns | Description |
|--------|---------|-------------|
| `head()` | `GitCommit` | HEAD commit (id, abbreviatedId, author, dateTime, shortMessage, fullMessage) |
| `status()` | `GitStatus` | Working tree status (clean property) |
| `describe(options)` | `String` | Git describe output. Options: `tags: true`, `longDescr: true` |
| `log(options)` | `List<GitCommit>` | Commit history. Options: `maxCommits: N` |
| `branch.current()` | `GitBranchInfo` | Current branch info (use `.name` for branch name) |
| `tag.list()` | `List<GitTag>` | All tags (use `.name` for tag name) |
| `getConfig(section, name)` | `String` | Git config value |
| `isEmpty()` | `boolean` | True if repository has no commits |

#### Escape Hatch (Advanced)

For operations not covered by `GitFacade`, access the underlying JGit API:

```groovy
gitProperties {
    // Access raw JGit Repository
    customProperty 'refs.count', { it.jgit.refDatabase.refs.size() }
    
    // Access JGit Git command interface (caller must close)
    customProperty 'stash.count', {
        def git = it.jgitCommands
        try {
            return git.stashList().call().size()
        } finally {
            git.close()
        }
    }
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

### Kotlin DSL Notes

Most configuration works identically in Kotlin DSL. For custom properties with closures, use `KotlinClosure1`:

```kotlin
import org.gradle.kotlin.dsl.KotlinClosure1
import com.gorylenko.jgit.GitFacade

gitProperties {
    dateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX"
    dateFormatTimeZone = "UTC"
    keys = listOf("git.branch", "git.commit.id", "git.commit.time")
    customProperty("greeting", "Hello")
    customProperty("my_custom_git_id", KotlinClosure1<GitFacade, String>({ head().id }))
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

Use `extProperty` to expose generated properties to other build tasks. This enables use cases such as printing Git info during the build or embedding the Git commit ID in JAR manifests.

**Printing Git properties from a task:**

```groovy
gitProperties {
    extProperty = 'gitProps'
}

// Ensure properties are always regenerated
generateGitProperties.outputs.upToDateWhen { false }

task printGitProperties {
    dependsOn generateGitProperties
    // Capture project.ext before doLast for configuration cache compatibility
    def ext = project.ext
    doLast {
        println "git.branch=" + ext.gitProps['git.branch']
    }
}
```

> **Why `def ext = project.ext` before `doLast`?** Gradle's configuration cache forbids referencing `project` inside `doLast` (execution phase). Capturing `project.ext` at configuration time—before `doLast`—keeps the task configuration-cache safe.

**Embedding Git info in a JAR manifest (Spring Boot example):**

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

| Plugin Version | Gradle | Java | Notes |
|----------------|--------|------|-------|
| 4.0.x          | 5.1 – 9.x | 17+ | Fixed overlapping outputs; `processResources` auto-wired |
| 3.0.x          | 5.1 – 9.x | 17+ | JGit backend, git worktree support |
| 2.5.x          | 5.1 – 9.x | 8+ | Grgit backend (deprecated) |

The plugin supports Gradle [configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html) and [git worktrees](https://git-scm.com/docs/git-worktree).

## Migration Guide

### Upgrading from 3.x

Version 4.0 changes the default output directory for `generateGitProperties` from `build/resources/main/` to `build/generated/resources/git/`. The file still ends up at the root of your JAR — `processResources` copies it there.

- **Default config:** No action needed.
- **`gitPropertiesDir` set explicitly:** Behaviour unchanged, but deprecated — migrate to `gitPropertiesResourceDir`.
- **`gitPropertiesResourceDir` set explicitly:** Behaviour unchanged.

See [MIGRATION.md](MIGRATION.md) for the full migration table.

### Upgrading from 2.x

Version 3.0 replaces the Grgit backend with JGit. Key changes:

- **Java 17+ required** (was Java 8)
- **Custom properties**: Closures now receive `GitFacade` instead of Grgit. See [GitFacade API](#gitfacade-api) for available methods.
- **JGit escape hatch**: For advanced use cases, access `jgit` (Repository) or `jgitCommands` (Git) directly.

Standard configuration options (`keys`, `dateFormat`, `branch`, etc.) are unchanged.

See [MIGRATION.md](MIGRATION.md) for detailed upgrade instructions.

## License

This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

---

*Originally inspired by [@lievendoclo](https://github.com/lievendoclo)'s article "Spring Boot's info endpoint, Git and Gradle" (2014).*
