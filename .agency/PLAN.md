# PLAN: Fix Overlapping Task Outputs (issues #233 / #212 / #197)

## Version: 3.0.3 → 4.0.0 (major — breaking change)

---

## Task 1: Fix source files — change DEFAULT_OUTPUT_DIR

**Files:**
- `src/main/groovy/com/gorylenko/GitPropertiesPlugin.groovy` line 18
- `src/main/groovy/com/gorylenko/GenerateGitPropertiesTask.groovy` line 28

**Change:** `"resources/main"` → `"generated/resources/git"` in both files.

**CRITICAL:** Both constants must be updated atomically. If they diverge, the wiring
and write target don't match and processResources won't find the file.

---

## Task 2: Rework sourceSets wiring in GitPropertiesPlugin.groovy

**File:** `src/main/groovy/com/gorylenko/GitPropertiesPlugin.groovy` lines 36–48

**Change:** Replace the `if (extension.gitPropertiesResourceDir.present)` guard
with `if (!extension.gitPropertiesDir.present)`. This makes the srcDir wiring run
for both the default case AND the gitPropertiesResourceDir case, while skipping it
only when gitPropertiesDir is set (user manages classpath themselves).

**Before:**
```groovy
project.afterEvaluate {
    if (extension.gitPropertiesResourceDir.present) {
        String gitPropertiesDir = getGitPropertiesDir(extension, project.layout).asFile.absolutePath
        def sourceSets = project.extensions.getByType(SourceSetContainer)
        sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).configure {
            it.resources.srcDir(gitPropertiesDir)
        }
        project.tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME).configure {
            dependsOn(task)
        }
    }
}
```

**After:**
```groovy
project.afterEvaluate {
    if (!extension.gitPropertiesDir.present) {
        String gitPropertiesDir = getGitPropertiesDir(extension, project.layout).asFile.absolutePath
        def sourceSets = project.extensions.getByType(SourceSetContainer)
        sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).configure {
            it.resources.srcDir(gitPropertiesDir)
        }
        project.tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME).configure {
            dependsOn(task)
        }
    }
}
```

---

## Task 3: Version bump

**File:** `build.gradle` line 54

**Change:** `version = "3.0.3"` → `version = "4.0.0"`

---

## Task 4: Update tests — generateGitProperties-only tests

Tests that invoke only `generateGitProperties` (not `assemble`/`classes`) must
update their expected path from `build/resources/main/git.properties` to
`build/generated/resources/git/git.properties`.

Tests that invoke `assemble` or `classes` keep `build/resources/main/git.properties`
(processResources copies it there).

**Files and lines to update** (generateGitProperties-only tests):
- `GenerateGitPropertiesTaskTest.groovy` lines 92–93
- `GitPropertiesPluginTests.groovy` lines 50, 75
- `BasicFunctionalTest.groovy` line 215
- `BackwardCompatibilityFunctionalTest.groovy` line 106
- `ConfigurationCacheFunctionalTest.groovy` lines 86, 136
- `CustomPropertiesFacadeTest.groovy` lines 68, 111, 150, 190, 227, 269, 310, 353
- `FSMonitorFunctionalTest.groovy` line 71
- `WorktreeUpToDateFunctionalTest.groovy` line 55
- `RealWorktreeFunctionalTest.groovy` lines 69, 127, 175, 221
- `WorktreeFunctionalTest.groovy` line 79

**Files needing task-invocation check before updating:**
- `SymlinkedGitDirectoryFunctionalTest.groovy` lines 138, 188
- `SubmoduleFunctionalTest.groovy` line 176
- `MultiProjectGitDirectoryFunctionalTest.groovy` lines 84, 156, 229, 295, 373, 455

---

## Task 5: Add regression test

**File:** `src/test/groovy/com/gorylenko/OverlappingOutputsBuildCacheFunctionalTest.groovy`

Three test methods:
1. `testGitPropertiesAlwaysPresentInJarWithBuildCache` — runs `clean assemble` 6 times
   with build cache, asserts `git.properties` at JAR root every iteration
2. `testExplicitGitPropertiesDirStillWritesToCustomLocation` — gitPropertiesDir override
3. `testGitPropertiesResourceDirStillCopiedToJar` — gitPropertiesResourceDir override

---

## Task 6: README migration note + version references

**File:** `README.md`

- Add `## Upgrading from 3.x` section with migration steps
- Update version numbers from `3.0.3` to `4.0.0` (lines 37, 40)
- Note the intermediate path change and that JAR location is unchanged

---

## Task 7: Create docs/fix-overlapping-outputs.md

**File:** `docs/fix-overlapping-outputs.md`

WHY document covering: problem, root cause (stale-output cleaner mechanism),
fix approach, result, override behaviour table, references.

---

## Execution Order

Tasks 1 and 2 together (source fix) → Task 3 (version) → Task 4 (test updates) →
Task 5 (new test) → run full test suite → Task 6 (README) → Task 7 (docs)

## Key Risks

1. `DEFAULT_OUTPUT_DIR` in TWO source files — must both change, or wiring breaks silently
2. Test path split: `generateGitProperties`-only → new path; `assemble` → old path
3. `gitPropertiesDir` guard logic: `!present` covers default + gitPropertiesResourceDir; `present` skips wiring
