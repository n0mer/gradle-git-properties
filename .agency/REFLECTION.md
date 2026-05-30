# Sprint Reflection — Fix Overlapping Outputs (gradle-git-properties)

**Date:** 2026-05-30
**Feature:** Fix overlapping task outputs (issues #233, #212, #197)
**Version bump:** 3.0.3 → 4.0.0

---

## What Was Built

- `DEFAULT_OUTPUT_DIR` changed from `"resources/main"` to `"generated/resources/git"` in `GitPropertiesPlugin.groovy` and `GenerateGitPropertiesTask.groovy`
- `afterEvaluate` guard flipped from `gitPropertiesResourceDir.present` to `!gitPropertiesDir.present`
- 35 test path references updated across 13 test files
- Regression test `OverlappingOutputsBuildCacheFunctionalTest` added (TDD, RED confirmed run 3)
- README migration section + `docs/fix-overlapping-outputs.md`

---

## What Worked Well

**TDD RED phase as a real sensor.** The regression test caught the failure at run 3 (not run 2), confirming it was testing incremental build behavior correctly — a false RED at run 2 would have meant the test was vacuous.

**Harness subagent pattern kept context clean.** Each phase returned a 10-line summary rather than raw output. This prevented context window saturation across a multi-phase sprint.

**Parallel research subagents.** Root cause analysis and reproducer construction ran concurrently, saving significant wall time.

**`!gitPropertiesDir.present` as the key insight.** The guard inversion is non-obvious: the original `gitPropertiesResourceDir.present` sounds like "run wiring when user sets a resource dir" but actually excluded the default case entirely. Inverting to `!gitPropertiesDir.present` covers both default and resource-dir-explicit cases with one condition.

**Reproducer-first discipline.** Building a minimal reproducer at `/tmp/gradle-git-properties-reproducer/` before touching plugin source validated the fix approach and caught the duplicate-registration trap early.

---

## What Caused Friction

**`DEFAULT_OUTPUT_DIR` in two files.** `GitPropertiesPlugin.groovy` and `GenerateGitPropertiesTask.groovy` each define this constant independently. Updating only one would have produced a silent mismatch — the plugin uses one path, the task another — with no compilation error. A grep before changing any default is now a standing rule.

**Duplicate registration trap.** First fix attempt used `gitPropertiesResourceDir` (the user-facing config property) to call `srcDir()`. The plugin was already processing this property to register the directory as a task input, so the manual `srcDir()` call added it a second time. Switched to `gitPropertiesDir` (the resolved output dir) to avoid double-registration.

**Test path update requires per-test categorization.** There is no safe bulk-replace for the 35 path references. Tests invoking `generateGitProperties` directly point to the new output dir; tests invoking `assemble` point to `build/resources/main` because processResources copies the file there. Each test had to be read individually.

---

## Lessons Stored (lessons-learned)

| ID | Summary |
|----|---------|
| `lesson-20260530-120000-001` | Grep for all constant definitions before changing a default value in a Gradle plugin |
| `lesson-20260530-120000-002` | Invert `afterEvaluate` guard from `property.present` to `!property.present` to cover default + explicit-resource-dir cases |
| `lesson-20260530-120000-003` | Use resolved output dir property (not config property) when wiring into sourceSets to avoid duplicate registration |
| `lesson-20260530-120000-004` | Categorize Gradle functional tests by invoked task before bulk-replacing path assertions |

---

## Signals

- BLOCKED signals: none — all gates passed first attempt
- All regression tests green before PR
