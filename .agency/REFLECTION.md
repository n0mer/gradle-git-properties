# Reflection: JGit Migration Validation

**Date:** 2026-05-21

## What Worked Well

1. **Side-by-side comparison approach** - Running same scenarios against v2.5.7 (Grgit) and v3.0.0 (JGit) allowed direct property comparison. Differences immediately visible in diff output.

2. **Comprehensive scenario coverage** - 26 scenarios covered edge cases: detached HEAD, unicode, merge commits, worktree states. Found the one real parity issue (detached HEAD).

3. **Test harness isolation** - Each scenario ran in isolated `/tmp` directory. No cross-contamination between tests.

4. **Role separation** - TESTER found issues, REVIEWER verified fixes. Clear responsibility boundaries.

## What Caused Issues

1. **Detached HEAD behavior difference** - JGit `repository.branch` returns full SHA in detached state. Grgit returns literal "HEAD". Required explicit detection:
   ```groovy
   if (!head.isSymbolic()) return "HEAD"
   ```

2. **Test template bug (scenario 15)** - Template used GString for DirectoryProperty. Not a parity issue but caused false failure:
   ```groovy
   // Wrong: GString
   gitPropertiesDir = "${project.buildDir}/custom-output"
   // Correct: DirectoryProperty
   gitPropertiesDir = layout.buildDirectory.dir("custom-output")
   ```

3. **CI test failures** - Debug/exploratory tests were committed. Required cleanup commits (d7a14ac, 2e2b3fc).

## Lessons Learned

1. **JGit API quirks matter** - JGit returns raw values, Grgit often normalized them. Document expected behavior explicitly.

2. **Test harness != plugin tests** - Harness bugs can look like parity issues. Always verify both versions fail the same way before declaring a parity issue.

3. **Worktree handling is complex** - Special casing needed for worktree paths. JGit's Repository.resolve() behaves differently than expected for commondir-relative refs.

## Metrics

| Metric | Value |
|--------|-------|
| Scenarios tested | 26 |
| Passed | 24 |
| Failed (parity issue) | 1 (fixed) |
| Skipped (v3-only) | 1 |
| Test harness bugs | 1 |

## Files Changed

- `src/main/groovy/com/gorylenko/jgit/GitBranch.groovy` - Detached HEAD fix
- CI test cleanup (2 commits)
