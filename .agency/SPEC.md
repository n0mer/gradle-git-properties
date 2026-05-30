# SPEC: Fix Overlapping Task Outputs (issue #233 / #212 / #197)

## Problem

`generateGitProperties` writes to `build/resources/main/git.properties` — the same
directory owned by `processResources`. With Gradle build cache enabled (Gradle 8.6+),
`processResources` uses stale-output detection (`getPreviousOutputFiles()`) which
alternately tracks and deletes `git.properties` on every `clean build`. The result:
`git.properties` is PRESENT on even runs and ABSENT on odd runs (or vice versa).
BUILD is always SUCCESS — the bug is silent.

Reproduced deterministically with plugin 2.4.2 + Gradle 8.7, multi-module project
with at least one file in `src/main/resources/`.

## Root Cause

In `GitPropertiesPlugin.groovy`:
```groovy
private static final String DEFAULT_OUTPUT_DIR = "resources/main"
```
Resolves to `build/resources/main/` — identical to `processResources` destination.
No `mustRunAfter` or `dependsOn` between the two tasks in default config.

## Fix

**The "right Gradle way"** (confirmed working in reproducer):

1. Change default output dir to `build/generated/resources/git/` (non-overlapping)
2. Wire it into `sourceSets.main.resources.srcDir(...)` lazily — this makes
   `processResources` depend on `generateGitProperties` implicitly via Gradle's
   input/output inference. No explicit `dependsOn` needed.

Result: `git.properties` is copied into `build/resources/main/` by `processResources`
(as an input, not an output), and ends up at the root of the JAR — identical to today.

## Versioning

This is a **breaking change** — the default intermediate build output path changes
from `build/resources/main/git.properties` to `build/generated/resources/git/git.properties`.

The plugin version must be bumped to the next **major** version (e.g. 2.x.y → 3.0.0).

The `gradle.properties` (or wherever the plugin version is declared) must be updated
as part of this PR. The changelog / release notes must call out the breaking change
explicitly.

## Scope

### In scope
1. **Fix default output dir + sourceSets wiring** in `GitPropertiesPlugin.groovy`
   - Change `DEFAULT_OUTPUT_DIR` constant
   - Apply `sourceSets.main.resources.srcDir(...)` unconditionally for default config
     (guarded by `hasPlugin(JavaPlugin)` — same guard as existing wiring)
   - Ensure `gitPropertiesDir` and `gitPropertiesResourceDir` still override correctly
     and don't double-register
2. **Update README** with migration note — users who reference
   `build/resources/main/git.properties` as a filesystem path in scripts/Dockerfiles
   need to update to `build/generated/resources/git/git.properties` (or use the
   JAR, which is unchanged)
3. **Add `docs/fix-overlapping-outputs.md`** capturing WHY (decisions, tradeoffs,
   Gradle concepts involved)

### Out of scope
- Deprecating `gitPropertiesDir` / `gitPropertiesResourceDir` (separate issue)
- Android support changes
- Any runtime deprecation warnings

## Constraints

- JAR contents must be identical: `git.properties` at root of JAR
- `gitPropertiesDir` and `gitPropertiesResourceDir` overrides must still work
- Android / non-Java projects must not break (gate on `hasPlugin(JavaPlugin)`)
- Must not introduce duplicate `git.properties` in `processResources` inputs
- Existing tests must continue to pass

## Success Criteria

1. Running `clean assemble` 6 times in a row with build cache enabled always
   produces `git.properties` in the output (no alternating absence)
2. `git.properties` appears at root of JAR (unchanged)
3. Explicit `gitPropertiesDir` config still respected
4. All existing tests pass
5. README updated with migration note
6. `docs/fix-overlapping-outputs.md` created

## References

- Issue #233: https://github.com/n0mer/gradle-git-properties/issues/233
- Issue #212: https://github.com/n0mer/gradle-git-properties/issues/212
- Issue #197: https://github.com/n0mer/gradle-git-properties/issues/197
- Gradle diagnosis: https://github.com/gradle/gradle/issues/34177#issuecomment-3051970053
- Reproducer: /tmp/gradle-git-properties-reproducer/
