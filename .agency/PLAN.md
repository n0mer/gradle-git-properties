# PLAN: gitPropertiesName Relative Path Support

## Summary

Extend `gitPropertiesName` to accept a relative path (e.g. `"discord4j/common/git.properties"`),
allowing users to control where `git.properties` lands inside the JAR. Fixes the migration
path for issue #306.

**Scope:** One setter added, tests added, docs updated. No structural changes.

## Key Design Decisions

- `Directory.file(String)` handles subpaths natively — no path construction changes needed
- `PropertiesFileWriter` already calls `mkdirs()` — no directory creation changes needed
- Validation via Groovy setter pattern (same as `commitIdAbbrevLength`)
- Default `"git.properties"` assigned at field init — bypasses setter, that's correct

## TDD Rules (strictly enforced)

- Vertical slices: RED→GREEN per behavior, one at a time
- Each test MUST fail before implementation (RED confirmed)
- Each test MUST pass after minimal implementation (GREEN confirmed)
- No horizontal slicing — no bulk test writing before impl

## Harness Mode Rules

- Verification: fast before slow (unit → functional)
- Subagent isolation: verifier spawned separately, returns summary only
- Stop on first failure — don't run functional tests if unit tests fail
- Build tool: Gradle (`./gradlew test`)

## Tasks

### Task 1 — setGitPropertiesName validator (TDD)
**File:** `src/main/groovy/com/gorylenko/GitPropertiesPlugin.groovy`
**File:** `src/test/groovy/com/gorylenko/GitPropertiesPluginExtensionTest.groovy`
**Role:** implementer
**Method:** Strict TDD vertical slices, one behavior at a time:

Behavior 0 (tracer bullet): plain filename `"my.properties"` accepted, `gitPropertiesName` set correctly — proves setter doesn't break the simple case
Behavior 1: valid subpath `"discord4j/common/git.properties"` accepted
Behavior 2: null rejected
Behavior 3: leading `/` rejected — path must not escape gitPropertiesResourceDir
Behavior 4: `..` segment rejected — path must not escape gitPropertiesResourceDir
Behavior 5: `..` in middle of path rejected — same rule

**Run after each RED→GREEN:**
```bash
./gradlew test --tests "com.gorylenko.GitPropertiesPluginExtensionTest" --quiet
```

### Task 2 — Functional tests for JAR packaging (TDD)
**File:** `src/test/groovy/com/gorylenko/BasicFunctionalTest.groovy`
**Role:** implementer
**Method:** TDD vertical slices:

Behavior 1: `gitPropertiesName = "discord4j/common/git.properties"` → file at that path in JAR, NOT at root
Behavior 2: plain filename `gitPropertiesName = "git-info.properties"` → file at `git-info.properties` at JAR root (single file, no subdir — same as pre-setter behavior)
Behavior 3: default `gitPropertiesName` (not set) → `git.properties` at JAR root (regression)
Behavior 4: leading `/` → `buildAndFail()` with clear message
Behavior 5: `..` segment → `buildAndFail()` with clear message

**Run after each RED→GREEN:**
```bash
./gradlew test --tests "com.gorylenko.BasicFunctionalTest.testGitPropertiesName*" --quiet
```

### Task 3 — Version bump
**File:** `build.gradle`
**Role:** implementer
**What:** `version = "4.0.1"` → `version = "4.0.2"`

### Task 4 — README update
**File:** `README.md`
**Role:** documenter
**What:**
- Output Location section: show `gitPropertiesName` accepting relative path
- Add validation note (leading `/` and `..` rejected)
- Update compatibility table: add 4.0.2 row

### Task 5 — MIGRATION.md update
**File:** `MIGRATION.md`
**Role:** documenter
**What:** Add section under "3.x to 4.0" explaining safe migration for issue #306:
- Do NOT point `gitPropertiesResourceDir` inside `build/resources/main/`
- Use `gitPropertiesName = "subdir/git.properties"` instead

### Task 6 — Scenarios doc update
**File:** `docs/version-parity-validation.md`
**Role:** documenter
**What:** Add new scenarios for `gitPropertiesName` subpath behavior to the existing scenario list

### Task 7 — Feature WHY doc
**File:** `docs/git-properties-jar-path.md`
**Role:** documenter
**What:** Create WHY doc capturing design decisions, root cause of #306, why `gitPropertiesResourceDir` inside `build/resources/main/` is rejected

### Task 8 — Scenario parity verification
**Role:** tester
**What:** Run all test scenarios from `docs/version-parity-validation.md` against:
- Previous version (`4.0.1` plugin jar from `main` worktree)
- New version (`4.0.2` plugin jar from this worktree)

Compare outputs for all existing scenarios — must have 100% parity.
Also run the NEW scenarios added in Task 6 against the new version only.

**How:**
1. Build both jars: `./gradlew shadowJar` in `../main` (v4.0.1) and current worktree (v4.0.2)
2. For each scenario in `docs/version-parity-validation.md`: run against both jars, compare output
3. Report: scenario name | v4.0.1 result | v4.0.2 result | parity

**Pass criteria:** All existing scenarios produce identical output on both versions. New scenarios pass on v4.0.2.

## Execution Order

```
Task 1 (setter + unit tests, TDD)
  → Task 2 (functional tests, TDD)
    → Task 3 (version bump)
      → Task 8 (scenario parity verification)
        → Task 4, 5, 6, 7 (docs, parallel)
```

## Verification Gates

| Gate | Command | Must Pass Before |
|---|---|---|
| Unit | `./gradlew test --tests "*.GitPropertiesPluginExtensionTest" --quiet` | Task 2 |
| Functional | `./gradlew test --tests "*.BasicFunctionalTest" --quiet` | Task 3 |
| Full | `./gradlew test --quiet` | Task 8 |
| Scenario parity | Run all scenarios, compare v4.0.1 vs v4.0.2 | Docs |

## Files Changed

| File | Change |
|---|---|
| `src/main/groovy/com/gorylenko/GitPropertiesPlugin.groovy` | Add `setGitPropertiesName` setter |
| `src/test/groovy/com/gorylenko/GitPropertiesPluginExtensionTest.groovy` | Add unit tests (TDD) |
| `src/test/groovy/com/gorylenko/BasicFunctionalTest.groovy` | Add functional tests (TDD) |
| `build.gradle` | Bump version 4.0.1 → 4.0.2 |
| `README.md` | Update `gitPropertiesName` docs + compatibility table |
| `MIGRATION.md` | Add #306 migration entry |
| `docs/version-parity-validation.md` | Add new scenarios |
| `docs/git-properties-jar-path.md` | New WHY doc |
| `.agency/results/parity-report.md` | Scenario parity report (v4.0.1 vs v4.0.2) |
