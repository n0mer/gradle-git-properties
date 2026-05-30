# Implementation Plan: Fix Overlapping Task Outputs (issues #233 / #212 / #197)

## Summary of Findings

`DEFAULT_OUTPUT_DIR = "resources/main"` exists in **two** source files:

- `GitPropertiesPlugin.groovy` line 18 — used in `getGitPropertiesDir()` (line 58) for sourceSets wiring
- `GenerateGitPropertiesTask.groovy` line 28 — used in `getGitPropertiesDir()` (line 172) for the `@OutputFile`

Both must be changed. Tests that hard-code `build/resources/main/git.properties` must be updated for the default-dir case, and tests for custom-dir overrides remain unchanged.

---

## Change 1: `GitPropertiesPlugin.groovy`

File: `src/main/groovy/com/gorylenko/GitPropertiesPlugin.groovy`

### 1a. Change DEFAULT_OUTPUT_DIR constant (line 18)

**Before (line 18):**
```groovy
    private static final String DEFAULT_OUTPUT_DIR = "resources/main"
```

**After:**
```groovy
    private static final String DEFAULT_OUTPUT_DIR = "generated/resources/git"
```

### 1b. Add default sourceSets wiring inside `plugins.withType(JavaPlugin)` block (lines 29–49)

The current `afterEvaluate` block only wires the sourceSets when `gitPropertiesResourceDir` is explicitly set. We need to also wire it for the default case (no custom dir configured), but guard against double-registration when a custom dir is set.

**Before (lines 29–49):**
```groovy
        // if Java plugin is applied, execute this task automatically when "classes" task is executed
        // see https://guides.gradle.org/implementing-gradle-plugins/#reacting_to_plugins
        project.plugins.withType(JavaPlugin) {
            project.tasks.named(JavaPlugin.CLASSES_TASK_NAME).configure {
                dependsOn(task)
            }

            // if Java plugin is used, this method will be called to register gitPropertiesResourceDir to classpath
            // at the end of evaluation phase (to make sure extension values are set)
            project.afterEvaluate {
                if (extension.gitPropertiesResourceDir.present) {
                    String gitPropertiesDir = getGitPropertiesDir(extension, project.layout).asFile.absolutePath
                    def sourceSets = project.extensions.getByType(SourceSetContainer)
                    sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).configure {
                        it.resources.srcDir(gitPropertiesDir)
                    }
                    // Ensure processResources depends on generateGitProperties when using custom resource dir
                    project.tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME).configure {
                        dependsOn(task)
                    }
                }
            }
        }
```

**After:**
```groovy
        // if Java plugin is applied, execute this task automatically when "classes" task is executed
        // see https://guides.gradle.org/implementing-gradle-plugins/#reacting_to_plugins
        project.plugins.withType(JavaPlugin) {
            project.tasks.named(JavaPlugin.CLASSES_TASK_NAME).configure {
                dependsOn(task)
            }

            // Wire the output directory into sourceSets.main.resources so that processResources
            // picks up git.properties as an input (not an output), eliminating the overlapping-outputs
            // problem (issues #233/#212/#197). Done at end of evaluation to allow extension values to be set.
            project.afterEvaluate {
                // gitPropertiesDir overrides: write directly, no sourceSets wiring needed
                if (!extension.gitPropertiesDir.present) {
                    String gitPropertiesDir = getGitPropertiesDir(extension, project.layout).asFile.absolutePath
                    def sourceSets = project.extensions.getByType(SourceSetContainer)
                    sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).configure {
                        it.resources.srcDir(gitPropertiesDir)
                    }
                    // processResources implicitly depends on generateGitProperties via srcDir,
                    // but we add an explicit dependsOn to be safe with strict dependency validation
                    project.tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME).configure {
                        dependsOn(task)
                    }
                }
            }
        }
```

**Rationale for the guard:**
- `gitPropertiesResourceDir` set: user wants the file in a custom resource dir, copy via processResources — this IS the same pattern as before, wire it.
- `gitPropertiesDir` set: user wants the file written directly to an arbitrary directory (not necessarily in the classpath). We do NOT register that as a srcDir — it's the user's responsibility. This preserves the original semantics of `gitPropertiesDir`.
- Neither set (default): wire `build/generated/resources/git` as a srcDir — this is the new behaviour.

The condition `!extension.gitPropertiesDir.present` covers both "gitPropertiesResourceDir set" and "neither set" cases, which are the two cases where wiring is correct.

---

## Change 2: `GenerateGitPropertiesTask.groovy`

File: `src/main/groovy/com/gorylenko/GenerateGitPropertiesTask.groovy`

### 2a. Change DEFAULT_OUTPUT_DIR constant (line 28)

**Before (line 28):**
```groovy
    private static final String DEFAULT_OUTPUT_DIR = "resources/main"
```

**After:**
```groovy
    private static final String DEFAULT_OUTPUT_DIR = "generated/resources/git"
```

No other changes needed in this file. The `getGitPropertiesDir()` method (lines 166–174) already correctly falls through to use `DEFAULT_OUTPUT_DIR` when neither `gitPropertiesResourceDir` nor `gitPropertiesDir` is present, and the overrides remain intact.

---

## Change 3: Version bump

File: `build.gradle`

**Before (line 54):**
```groovy
version = "3.0.3"
```

**After:**
```groovy
version = "4.0.0"
```

Also update the plugin references in the same file (line 7, currently `"com.gorylenko.gradle-git-properties" version "3.0.2"`) — this is the version used to build the plugin itself (bootstrap), leave it pointing at the last published 3.x unless a new release exists. The version that matters for users is line 54.

---

## Change 4: Update tests that hard-code `build/resources/main/git.properties`

After the default output dir changes, `generateGitProperties` (default config) writes to `build/generated/resources/git/git.properties`. `processResources` then copies it to `build/resources/main/git.properties`. Tests that run `generateGitProperties` directly (without `processResources`) and look in `build/resources/main` will break.

Tests fall into two categories:

### 4a. Tests that run `generateGitProperties` only (not `assemble`/`processResources`)

These now need to look in `build/generated/resources/git/git.properties` instead.

Affected files and lines:

| File | Line | Old path | New path |
|------|------|----------|----------|
| `BasicFunctionalTest.groovy` | 215 | `build/resources/main/git.properties` | `build/generated/resources/git/git.properties` |
| `ConfigurationCacheFunctionalTest.groovy` | 86 | `build/resources/main/git.properties` | `build/generated/resources/git/git.properties` |
| `ConfigurationCacheFunctionalTest.groovy` | 136 | `build/resources/main/git.properties` | `build/generated/resources/git/git.properties` |
| `CustomPropertiesFacadeTest.groovy` | 68, 111, 150, 190, 227, 269, 310, 353 | `build/resources/main/git.properties` | `build/generated/resources/git/git.properties` |
| `GenerateGitPropertiesTaskTest.groovy` | 92–93 | `resources/main` assertion | `generated/resources/git` assertion |
| `GitPropertiesPluginTests.groovy` | 50, 75 | `project.buildDir + '/resources/main/git.properties'` | `project.buildDir + '/generated/resources/git/git.properties'` |

### 4b. Tests that run `classes` or `assemble` (processResources runs)

These can legitimately keep looking in `build/resources/main/git.properties` because `processResources` copies it there. However they should also work with the new task ordering.

Affected tests that should keep looking in `build/resources/main`:
- `BackwardCompatibilityFunctionalTest.groovy` line 106 — runs `generateGitProperties` but checks `build/resources/main`. **This test must also change** since it only runs `generateGitProperties` (not `processResources`).
- `WorktreeUpToDateFunctionalTest.groovy` line 55 — runs `generateGitProperties` only → **must change**.
- `FSMonitorFunctionalTest.groovy` line 71 — runs `generateGitProperties` only → **must change**.
- `RealWorktreeFunctionalTest.groovy` lines 69, 127, 175, 221 — runs `generateGitProperties` only → **must change**.
- `WorktreeFunctionalTest.groovy` line 79 — runs `generateGitProperties` only → **must change**.
- `SymlinkedGitDirectoryFunctionalTest.groovy` lines 138, 188 — check which task is called.
- `SubmoduleFunctionalTest.groovy` line 176 — check which task.
- `MultiProjectGitDirectoryFunctionalTest.groovy` lines 84, 156, 229, 295, 373, 455 — check which task.

**Recommended test approach:** For tests that run only `generateGitProperties`, update the path to `build/generated/resources/git/git.properties`. For tests that run `assemble` or `classes`, keep `build/resources/main/git.properties`.

Let me note what each affected test runs:

| Test | Task invoked | Expected location after fix |
|------|--------------|-----------------------------|
| `BackwardCompatibilityFunctionalTest` | `generateGitProperties` | `build/generated/resources/git/git.properties` |
| `WorktreeUpToDateFunctionalTest` | `generateGitProperties` | `build/generated/resources/git/git.properties` |
| `FSMonitorFunctionalTest` | `generateGitProperties` | `build/generated/resources/git/git.properties` |
| `RealWorktreeFunctionalTest` | `generateGitProperties` | `build/generated/resources/git/git.properties` |
| `WorktreeFunctionalTest` | `generateGitProperties` | `build/generated/resources/git/git.properties` |
| `SymlinkedGitDirectoryFunctionalTest` | needs verify | depends |
| `SubmoduleFunctionalTest` | needs verify | depends |
| `MultiProjectGitDirectoryFunctionalTest` | needs verify | depends |
| `BasicFunctionalTest.testCustomCommitIdAbbrevLength` | `generateGitProperties` | `build/generated/resources/git/git.properties` |
| `BasicFunctionalTest.testProcessResourcesDependsOn...` | `processResources` | keep `build/resources/main/git.properties` |
| `ConfigurationCacheFunctionalTest` | `generateGitProperties` | `build/generated/resources/git/git.properties` |
| `CustomPropertiesFacadeTest` | `generateGitProperties` | `build/generated/resources/git/git.properties` |
| `GenerateGitPropertiesTaskTest` | unit test (task.generate()) | `build/generated/resources/git/git.properties` |
| `GitPropertiesPluginTests` | unit test (task.generate()) | `build/generated/resources/git/git.properties` |

---

## Change 5: New regression test — `OverlappingOutputsBuildCacheFunctionalTest.groovy`

Create: `src/test/groovy/com/gorylenko/OverlappingOutputsBuildCacheFunctionalTest.groovy`

This test is the primary regression proof for issue #233. It runs `clean assemble` (not just `generateGitProperties`) 6 times with build cache enabled and asserts:
1. `git.properties` is present at the root of the produced JAR after every run
2. The task outcomes match expected patterns (SUCCESS on first run, FROM_CACHE on subsequent after clean)

```groovy
package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.util.zip.ZipFile

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/**
 * Regression test for issues #233 / #212 / #197:
 * Overlapping task outputs between generateGitProperties and processResources.
 *
 * With build cache enabled and the old default output dir (build/resources/main),
 * Gradle's stale-output cleanup alternately deletes git.properties on every clean build.
 *
 * This test runs "clean assemble" 6 times with build cache and asserts git.properties
 * is present in the JAR every time.
 */
public class OverlappingOutputsBuildCacheFunctionalTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    /**
     * Core regression: clean assemble 6x with build cache must always produce git.properties in JAR.
     */
    @Test
    public void testGitPropertiesAlwaysPresentInJarWithBuildCache() {
        def projectDir = temporaryFolder.newFolder("project")
        def buildCacheDir = temporaryFolder.newFolder("build-cache")

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Initial commit")
        })

        new File(projectDir, "settings.gradle") << """
            buildCache {
                local { directory = '${buildCacheDir.absolutePath}' }
            }
        """.stripIndent()

        new File(projectDir, "build.gradle") << """
            plugins {
                id('java')
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        6.times { int i ->
            // clean first (every iteration)
            GradleRunner.create()
                    .withPluginClasspath()
                    .withArguments("clean", "--build-cache")
                    .withProjectDir(projectDir)
                    .build()

            def result = GradleRunner.create()
                    .withPluginClasspath()
                    .withArguments("assemble", "--build-cache")
                    .withProjectDir(projectDir)
                    .build()

            // assemble must succeed
            assertEquals("Iteration ${i+1}: assemble should succeed",
                    TaskOutcome.SUCCESS, result.task(":assemble").outcome)

            // git.properties must appear in the JAR at its root
            def jarFile = new File(projectDir, "build/libs/${projectDir.name}.jar")
            assertTrue("Iteration ${i+1}: JAR should exist", jarFile.exists())

            def zipFile = new ZipFile(jarFile)
            try {
                def entry = zipFile.getEntry("git.properties")
                assertNotNull("Iteration ${i+1}: git.properties should be at root of JAR", entry)

                // Verify content is non-empty and valid
                def props = new Properties()
                zipFile.getInputStream(entry).withCloseable { props.load(it) }
                assertNotNull("Iteration ${i+1}: git.commit.id should be present",
                        props.getProperty("git.commit.id"))
            } finally {
                zipFile.close()
            }
        }
    }

    /**
     * Verify that explicit gitPropertiesDir (old override) still works.
     * When gitPropertiesDir is set, the file is written there directly (not through processResources).
     * The JAR will NOT contain git.properties in this case — that is intentional/documented behaviour.
     */
    @Test
    public void testExplicitGitPropertiesDirStillWritesToCustomLocation() {
        def projectDir = temporaryFolder.newFolder("project-custom-dir")

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Initial commit")
        })

        def customDir = new File(projectDir, "custom-output")
        customDir.mkdirs()

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('java')
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                gitPropertiesDir = file('custom-output')
            }
        """.stripIndent()

        def result = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("generateGitProperties")
                .withProjectDir(projectDir)
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
        assertTrue("git.properties should be in custom dir",
                new File(customDir, "git.properties").exists())
    }

    /**
     * Verify that gitPropertiesResourceDir (custom resource dir) still ends up in JAR.
     */
    @Test
    public void testGitPropertiesResourceDirStillCopiedToJar() {
        def projectDir = temporaryFolder.newFolder("project-resource-dir")

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Initial commit")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('java')
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                gitPropertiesResourceDir = layout.buildDirectory.dir('custom-git-resources')
            }
        """.stripIndent()

        def result = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("assemble")
                .withProjectDir(projectDir)
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":assemble").outcome)

        def jarFile = new File(projectDir, "build/libs/${projectDir.name}.jar")
        def zipFile = new ZipFile(jarFile)
        try {
            def entry = zipFile.getEntry("git.properties")
            assertNotNull("git.properties should be at root of JAR with custom resource dir", entry)
        } finally {
            zipFile.close()
        }
    }
}
```

---

## Change 6: README migration note

File: `README.md`

In the `## Upgrading from 2.x` section (currently ends at line 59), add a new `## Upgrading from 3.x` section immediately after it (before `## Configuration`):

**Add after line 59 (after the `See [MIGRATION.md]` sentence), before line 61 (`## Configuration`):**

```markdown
## Upgrading from 3.x

Version 4.0 moves the default output directory for `generateGitProperties` from
`build/resources/main/` to `build/generated/resources/git/`. For Java projects, the
file is still copied to `build/resources/main/` by `processResources` and ends up at
the root of the JAR — the runtime location is **unchanged**.

**Why this changed:** The old directory (`build/resources/main/`) is owned by the
`processResources` task. Writing directly into it caused Gradle's stale-output
detection to silently delete `git.properties` on every `clean build` when the build
cache was enabled (Gradle 8.6+). See [docs/fix-overlapping-outputs.md](docs/fix-overlapping-outputs.md)
for a full explanation.

**Migration steps:**

1. If you reference `build/resources/main/git.properties` directly in scripts or
   other tasks, update the path to `build/resources/main/git.properties` — no change
   needed, that final path is unchanged for Java projects.

2. If you use `gitPropertiesDir` to write to a custom directory, behaviour is
   unchanged. The sourceSets wiring is skipped and the file is written where you
   specified.

3. If you use `gitPropertiesResourceDir` to write to a custom resource directory,
   that directory is still registered as a `srcDir` and `processResources` still
   copies the file — no change needed.

4. Non-Java projects (Android, pure Groovy without `java` plugin, etc.) are
   unaffected: the sourceSets wiring is gated on `hasPlugin(JavaPlugin)`.
```

Also update the version number references in `README.md` from `3.0.3` to `4.0.0`:

- Line 37: `version "3.0.3"` → `version "4.0.0"`
- Line 40: `version("3.0.3")` → `version("4.0.0")`
- Line 43: `The plugin generates \`git.properties\` at \`build/resources/main/git.properties\`.` → update to note this is for Java projects via `processResources`, generated into `build/generated/resources/git/` first.
- Line 363 (compatibility table): add `4.0.x` row.

---

## Change 7: `docs/fix-overlapping-outputs.md`

Create new file: `docs/fix-overlapping-outputs.md`

```markdown
# Fix: Overlapping Task Outputs (issues #233 / #212 / #197)

## Problem

`generateGitProperties` wrote `git.properties` directly into `build/resources/main/`
— the same directory managed by `processResources`.

With Gradle's build cache enabled (Gradle 8.6+), Gradle tracks which task "owns"
each output directory. `processResources` owns `build/resources/main/`. When Gradle
restores `processResources` from cache after a `clean`, it first deletes any files in
that directory not listed as outputs of `processResources` — including `git.properties`,
which was written there by `generateGitProperties` (a different task).

The result: `git.properties` was silently absent from the JAR on every second
`clean build` when the build cache was warm. The BUILD outcome was always SUCCESS.

## Root Cause

In `GitPropertiesPlugin.groovy` and `GenerateGitPropertiesTask.groovy`:

```groovy
private static final String DEFAULT_OUTPUT_DIR = "resources/main"
```

This resolves to `build/resources/main/` — the exact directory owned by `processResources`.
No `mustRunAfter` or `dependsOn` existed between the two tasks in the default configuration.

## Fix

1. **Changed default output directory** to `build/generated/resources/git/`.  
   This directory is owned exclusively by `generateGitProperties`.

2. **Wired the output directory into `sourceSets.main.resources`** (lazily, at
   `afterEvaluate` time, gated on `JavaPlugin` being applied):

   ```groovy
   sourceSets.main.resources.srcDir("build/generated/resources/git")
   ```

   This makes `processResources` treat `build/generated/resources/git/` as an
   *input source directory*, not as an output. `processResources` declares an
   implicit dependency on `generateGitProperties` (Gradle's task input/output
   tracking does this automatically when a srcDir is a task output).

3. Added an explicit `processResources.dependsOn(generateGitProperties)` as a
   belt-and-suspenders guard for strict dependency validation mode.

## Result

- `generateGitProperties` writes `build/generated/resources/git/git.properties`
- `processResources` copies it to `build/resources/main/git.properties` (as part of normal resource processing)
- The JAR contains `git.properties` at its root — **identical to the old behaviour**
- No overlapping outputs → no stale-output interference → consistent builds with build cache

## Override behaviour

| Config | Behaviour |
|--------|-----------|
| Default (no config) | Written to `build/generated/resources/git/`, registered as srcDir, copied to JAR by `processResources` |
| `gitPropertiesResourceDir = X` | Written to `X/`, registered as srcDir, copied to JAR by `processResources` |
| `gitPropertiesDir = X` | Written directly to `X/`, NOT registered as srcDir. User is responsible for classpath inclusion. |

## References

- [Gradle docs: Dealing with stale outputs](https://docs.gradle.org/current/userguide/incremental_build.html#sec:task_output_caching_issue)
- [Gradle docs: Overlapping task outputs](https://docs.gradle.org/current/userguide/incremental_build.html#sec:overlapping_outputs)
- Issues: [#233](https://github.com/n0mer/gradle-git-properties/issues/233), [#212](https://github.com/n0mer/gradle-git-properties/issues/212), [#197](https://github.com/n0mer/gradle-git-properties/issues/197)
```

---

## Risks and Edge Cases

### Risk 1: `gitPropertiesDir` override — no conflict

When `gitPropertiesDir` is set, the `afterEvaluate` block now skips the `srcDir` wiring entirely (guarded by `!extension.gitPropertiesDir.present`). The file is written to the user-specified directory. This is correct — `gitPropertiesDir` semantics are "write here, I'll handle classpath myself".

### Risk 2: `gitPropertiesResourceDir` override — no double-registration

When `gitPropertiesResourceDir` is set, `gitPropertiesDir` is not set, so `!extension.gitPropertiesDir.present` is true, and the wiring runs for `gitPropertiesResourceDir.get()`. This is identical to the previous behaviour (the old code ran the wiring only when `gitPropertiesResourceDir.present`). No double-registration.

### Risk 3: Both `gitPropertiesDir` AND `gitPropertiesResourceDir` set simultaneously

`getGitPropertiesDir()` in the task resolves `gitPropertiesResourceDir` first (line 167). The plugin's `afterEvaluate` block skips wiring when `gitPropertiesDir.present`. This is subtly inconsistent: the task writes to `gitPropertiesResourceDir` but the plugin does not wire it. However, this was already a pathological config in 3.x and the spec does not require fixing it. Current behaviour: the file is written to `gitPropertiesResourceDir`, no srcDir wiring, no copy to JAR. The user set `gitPropertiesDir` which opted out of wiring — document as undefined behaviour.

### Risk 4: `DEFAULT_OUTPUT_DIR` in two files

**This is critical.** Both files must be updated atomically. The constant in `GitPropertiesPlugin.groovy` controls what directory is wired as a `srcDir`. The constant in `GenerateGitPropertiesTask.groovy` controls where the file is actually written (`@OutputFile`). If they diverge, the wiring and the write target don't match, and `processResources` won't find the file.

### Risk 5: Non-Java projects

The `srcDir` wiring is inside `project.plugins.withType(JavaPlugin)`, so non-Java projects (Android, Kotlin Multiplatform without JVM, etc.) are unaffected. The task will still write to `build/generated/resources/git/git.properties` by default, but nothing copies it to the classpath — same as the current behaviour for non-Java projects.

### Risk 6: `build/resources/main/git.properties` hard-coded in tests

There are ~25 test references to `build/resources/main/git.properties`. These must be updated or the test suite will fail. The distinction is:
- Tests that only run `generateGitProperties` → path changes to `build/generated/resources/git/git.properties`
- Tests that run `assemble` or `classes` (which triggers `processResources`) → path stays `build/resources/main/git.properties`

### Risk 7: Lazy provider chain for the wired directory

The `getGitPropertiesDir()` in the plugin returns an eagerly-resolved `Directory` (calls `.get()` at the end). This is fine inside `afterEvaluate` because we're past configuration time. However, the `srcDir(...)` call receives a `String` (`.asFile.absolutePath`), not a lazy `Provider`. If the build directory is relocated after `afterEvaluate`, this breaks — but this was already the case in 3.x and is not a new regression.

### Risk 8: `BuildCacheFunctionalTest` — existing test passes `generateGitProperties`, not `assemble`

The existing `BuildCacheFunctionalTest.testPluginSupportsBuildCache` only runs `generateGitProperties`. After the fix, the file is at `build/generated/resources/git/git.properties`. The test does not assert the file location, only the task outcomes — so it continues to pass as-is. No change needed.

---

## Complete File Change Summary

| File | Type | Description |
|------|------|-------------|
| `src/main/groovy/com/gorylenko/GitPropertiesPlugin.groovy` | Modify | Change `DEFAULT_OUTPUT_DIR`, rework `afterEvaluate` wiring |
| `src/main/groovy/com/gorylenko/GenerateGitPropertiesTask.groovy` | Modify | Change `DEFAULT_OUTPUT_DIR` (line 28) |
| `build.gradle` | Modify | Version bump `3.0.3` → `4.0.0` (line 54) |
| `README.md` | Modify | Add `## Upgrading from 3.x` section, update version references |
| `docs/fix-overlapping-outputs.md` | Create | WHY document |
| `src/test/groovy/com/gorylenko/OverlappingOutputsBuildCacheFunctionalTest.groovy` | Create | New regression test |
| All test files with `build/resources/main/git.properties` that run `generateGitProperties` only | Modify | Update path to `build/generated/resources/git/git.properties` |

---

## Exact Line-by-Line Change Locations

### `GitPropertiesPlugin.groovy`
- Line 18: `"resources/main"` → `"generated/resources/git"`
- Lines 36–48: replace `if (extension.gitPropertiesResourceDir.present)` guard with `if (!extension.gitPropertiesDir.present)` and keep the `srcDir` + `dependsOn` wiring inside

### `GenerateGitPropertiesTask.groovy`
- Line 28: `"resources/main"` → `"generated/resources/git"`

### `build.gradle`
- Line 54: `"3.0.3"` → `"4.0.0"`

### `src/test/groovy/com/gorylenko/GenerateGitPropertiesTaskTest.groovy`
- Lines 92–93: change `resources/main` → `generated/resources/git` in the string comparison

### `src/test/groovy/com/gorylenko/GitPropertiesPluginTests.groovy`
- Line 50: `'/resources/main/git.properties'` → `'/generated/resources/git/git.properties'`
- Line 75: `'/resources/main/git.properties'` → `'/generated/resources/git/git.properties'`

### `src/test/groovy/com/gorylenko/BasicFunctionalTest.groovy`
- Line 215: `"build/resources/main/git.properties"` → `"build/generated/resources/git/git.properties"`
- Line 151 test (`testProcessResourcesDependsOnGenerateGitPropertiesWithCustomResourceDir`): this test runs `processResources` and checks the custom dir, no path change needed

### `src/test/groovy/com/gorylenko/BackwardCompatibilityFunctionalTest.groovy`
- Line 106: `"build/resources/main/git.properties"` → `"build/generated/resources/git/git.properties"`

### `src/test/groovy/com/gorylenko/ConfigurationCacheFunctionalTest.groovy`
- Line 86: `"build/resources/main/git.properties"` → `"build/generated/resources/git/git.properties"`
- Line 136: `"build/resources/main/git.properties"` → `"build/generated/resources/git/git.properties"`

### `src/test/groovy/com/gorylenko/CustomPropertiesFacadeTest.groovy`
- Lines 68, 111, 150, 190, 227, 269, 310, 353: all 8 occurrences → `"build/generated/resources/git/git.properties"`

### `src/test/groovy/com/gorylenko/FSMonitorFunctionalTest.groovy`
- Line 71: `"build/resources/main/git.properties"` → `"build/generated/resources/git/git.properties"`

### `src/test/groovy/com/gorylenko/WorktreeUpToDateFunctionalTest.groovy`
- Line 55: `"build/resources/main/git.properties"` → `"build/generated/resources/git/git.properties"`

### `src/test/groovy/com/gorylenko/RealWorktreeFunctionalTest.groovy`
- Lines 69, 127, 175, 221: all 4 occurrences → `"build/generated/resources/git/git.properties"`

### `src/test/groovy/com/gorylenko/WorktreeFunctionalTest.groovy`
- Line 79: `"build/resources/main/git.properties"` → `"build/generated/resources/git/git.properties"`

### Tests that need verification before changing

These files contain `build/resources/main/git.properties` references but I could not confirm which task they invoke from the lines read. The implementer should check what task is run in each test:

- `src/test/groovy/com/gorylenko/SymlinkedGitDirectoryFunctionalTest.groovy` (lines 138, 188)
- `src/test/groovy/com/gorylenko/SubmoduleFunctionalTest.groovy` (line 176)
- `src/test/groovy/com/gorylenko/MultiProjectGitDirectoryFunctionalTest.groovy` (lines 84, 156, 229, 295, 373, 455)

For each: if the test invokes only `generateGitProperties`, change path. If it invokes `assemble` or `classes`, keep `build/resources/main/git.properties`.
