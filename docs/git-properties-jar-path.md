# Feature: gitPropertiesName Relative Path Support (v4.0.2)

## Problem

Issue #306: A user migrating from v3 to v4 followed the migration guide's advice to rename
`gitPropertiesDir` to `gitPropertiesResourceDir`. However, they had originally set
`gitPropertiesDir` to a path _inside_ `build/resources/main/` in order to control where
`git.properties` would appear inside the JAR:

```groovy
// v3 usage — worked but was fragile
gitProperties {
    gitPropertiesDir = file("${buildDir}/resources/main/discord4j/common")
}
// Result in JAR: discord4j/common/git.properties
```

After renaming to `gitPropertiesResourceDir` as the migration guide instructed, the build broke:
`generateGitProperties` began writing into `build/resources/main/discord4j/common/`, which is a
subdirectory of `processResources`'s own output directory. This recreated exactly the
overlapping-output problem that v4.0 was designed to fix.

## Root Cause

`gitPropertiesResourceDir` conflates two distinct concerns:

1. **Where to write the file on disk** (the task's output directory)
2. **Where the file appears in the JAR** (the classpath subpath)

When `gitPropertiesResourceDir` is set to any path inside `build/resources/main/`, Gradle's
`processResources` task and `generateGitProperties` both claim ownership of the same output
directory. Gradle's stale-output detection then silently deletes outputs that don't belong to
the current task, causing intermittent corruption on cached builds (the original issue #233/#212).

## Why Not Fix gitPropertiesResourceDir to Accept build/resources/main/

Pointing `gitPropertiesResourceDir` at a path inside `processResources`'s output directory is
structurally incompatible with Gradle's task output model:

- Gradle forbids two tasks from owning the same output directory. Any attempt to wire
  `sourceSets.main.resources.srcDir(gitPropertiesResourceDir)` on a path that is already under
  `processResources`'s output recreates the overlapping-output problem.
- Adding a `dependsOn` relationship does not resolve the conflict: even if
  `processResources` runs after `generateGitProperties`, Gradle's stale-output cleanup still
  removes files it does not recognise as belonging to `processResources`.
- The clean fix requires keeping the two output directories separate, which is the design
  `gitPropertiesResourceDir` already enforces for its default path
  (`build/generated/resources/git/`).

## Solution

Extend `gitPropertiesName` to accept a relative path. This separates the two concerns cleanly:

- **Write location** remains `build/generated/resources/git/<relative-path>` — owned exclusively
  by `generateGitProperties`, never overlapping with `processResources`.
- **JAR subpath** is determined by the relative path in `gitPropertiesName` — `processResources`
  copies the file into the JAR at the correct location.

```groovy
// After (v4.0.2) — correct
gitProperties {
    gitPropertiesName = "discord4j/common/git.properties"
}
// Written to: build/generated/resources/git/discord4j/common/git.properties
// Result in JAR: discord4j/common/git.properties
```

No changes to `gitPropertiesResourceDir` are needed. The default value
(`build/generated/resources/git/`) remains correct and unchanged.

## Validation Rules

`setGitPropertiesName(String name)` rejects values that would escape the resource directory:

| Input | Result |
|-------|--------|
| `null` | Build error: "must be a relative path that stays under gitPropertiesResourceDir" |
| `"/git.properties"` (leading `/`) | Same error |
| `"../git.properties"` (`..` segment) | Same error |
| `"git.properties"` (plain filename) | Accepted — unchanged default behaviour |
| `"discord4j/common/git.properties"` (relative path) | Accepted — subpath in JAR |

All rejections use the same error message to avoid leaking internal path details.

## Migration

```groovy
// Before (v3) — caused silent build cache corruption on v4.0
gitProperties {
    gitPropertiesDir = file("${buildDir}/resources/main/discord4j/common")
}
// Result in JAR: discord4j/common/git.properties

// After (v4.0.2) — correct
gitProperties {
    gitPropertiesName = "discord4j/common/git.properties"
}
// Result in JAR: discord4j/common/git.properties
```

Users who only renamed `gitPropertiesDir` to `gitPropertiesResourceDir` without changing the
value should also migrate to `gitPropertiesName` if the path pointed inside
`build/resources/main/`.
