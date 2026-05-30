# Fix: Overlapping Task Outputs (issues #233, #212, #197)

## Problem

In versions up to 3.x, `generateGitProperties` wrote `git.properties` directly into
`build/resources/main/` — the same directory managed by the `processResources` task.

With Gradle's build cache enabled (Gradle 8.6+), `processResources` uses stale-output
detection: before each run it deletes any files in its output directory that it did not
produce itself. On alternating `clean build` runs, `git.properties` was tracked then
untracked as a `processResources` output, causing it to be deleted on every second run.
The build always reported SUCCESS — the data loss was silent.

## Root Cause

```groovy
// GitPropertiesPlugin.groovy and GenerateGitPropertiesTask.groovy (pre-4.0):
private static final String DEFAULT_OUTPUT_DIR = "resources/main"
```

This resolved to `build/resources/main/` — the exact directory owned by `processResources`.
No `mustRunAfter` or `dependsOn` existed between the two tasks in the default configuration.

## Fix

Two changes in 4.0:

**1. Default output directory changed** to `build/generated/resources/git/` — a directory
owned exclusively by `generateGitProperties`, with no overlap with `processResources`.

**2. Output directory wired into `sourceSets.main.resources`** (lazily, at `afterEvaluate`,
gated on `hasPlugin(JavaPlugin)`):

```groovy
sourceSets.main.resources.srcDir("build/generated/resources/git")
processResources.dependsOn(generateGitProperties)
```

This makes `processResources` treat `build/generated/resources/git/` as a *source
directory* (input), not an output. Gradle's task dependency inference automatically
ensures `generateGitProperties` runs before `processResources`.

## Result

| Step | Task | Location |
|------|------|----------|
| 1 | `generateGitProperties` | writes `build/generated/resources/git/git.properties` |
| 2 | `processResources` | copies it to `build/resources/main/git.properties` |
| 3 | `jar` | packages it at root of JAR |

**JAR contents are identical to 3.x.** No overlap. No stale-output interference.

## Override Behaviour

| Configuration | Output location | sourceSets wiring |
|---------------|-----------------|-------------------|
| Default | `build/generated/resources/git/` | Yes (automatic) |
| `gitPropertiesResourceDir = X` | `X/` | Yes |
| `gitPropertiesDir = X` | `X/` | No (user manages classpath) |

## References

- [Issue #233](https://github.com/n0mer/gradle-git-properties/issues/233) — git.properties alternately missing after clean build
- [Issue #212](https://github.com/n0mer/gradle-git-properties/issues/212) — overlapping task outputs
- [Issue #197](https://github.com/n0mer/gradle-git-properties/issues/197) — dotGitDirectory as file input
- [Gradle: Dealing with stale outputs](https://docs.gradle.org/current/userguide/incremental_build.html#sec:task_output_caching_issue)
- [gradle/gradle#34177](https://github.com/gradle/gradle/issues/34177) — upstream Gradle diagnosis
