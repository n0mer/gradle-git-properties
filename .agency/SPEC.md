# SPEC: Control git.properties Path Inside the JAR

## Problem

Users want `git.properties` to land at a specific subpath inside the JAR —
e.g. `discord4j/common/git.properties` instead of the root `git.properties`.

In v3, they achieved this by pointing `gitPropertiesDir` at a subdirectory
of `build/resources/main/`:

```groovy
// v3 — worked, but caused silent build cache corruption (#233, #212)
gitProperties {
    gitPropertiesDir = file("${buildDir}/resources/main/discord4j/common")
}
```

The v4 migration guide says to rename `gitPropertiesDir` → `gitPropertiesResourceDir`.
When the user does this, the plugin adds `srcDir()` wiring on the supplied path.
Pointing `srcDir()` at a subdirectory of `build/resources/main/` recreates the
overlapping-outputs problem that v4 was designed to fix — and triggers
`WorkValidationException` from `sourcesJar` (issue #306).

## Root Cause

`gitPropertiesResourceDir` conflates two distinct concerns:
- **Write location** — where `generateGitProperties` writes the intermediate file
- **JAR path** — where the file appears inside the packaged JAR

## Solution

Extend `gitPropertiesName` to accept a relative path (not just a filename).

```groovy
// Control JAR subpath — no write location change needed
gitProperties {
    gitPropertiesName = "discord4j/common/git.properties"
}
```

`generateGitProperties` writes to:
```
build/generated/resources/git/discord4j/common/git.properties
```

`processResources` copies the full tree → JAR contains `discord4j/common/git.properties`.

## Requirements

### Functional
1. `gitPropertiesName` accepts a relative path with `/` separators
2. The file lands at the correct subpath inside the JAR
3. Default `gitPropertiesName = "git.properties"` behavior is unchanged
4. Works with default `gitPropertiesResourceDir` and custom one (outside `build/resources/main/`)

### Validation
5. `gitPropertiesName` with a leading `/` fails with a clear error message
6. `gitPropertiesName` with `..` segments fails with a clear error message

### Testing
7. Functional test: `gitPropertiesName = "subdir/git.properties"` → file at `subdir/git.properties` in JAR
8. Functional test: default name → file at `git.properties` in JAR (regression)
9. Unit test: validation rejects leading `/` and `..` segments

### Documentation
10. README updated: `gitPropertiesName` docs show relative path example
11. MIGRATION.md updated: explains the safe migration path for issue #306 users

## Constraints

- No new properties — extend `gitPropertiesName` only
- `gitPropertiesResourceDir` behavior unchanged
- `gitPropertiesDir` (deprecated) behavior unchanged
- Backward compatible — existing configs unaffected
- Must NOT allow `gitPropertiesResourceDir` inside `build/resources/main/` (out of scope)

## Acceptance Criteria

- [ ] `gitPropertiesName = "subdir/git.properties"` → `subdir/git.properties` in JAR
- [ ] Default `gitPropertiesName = "git.properties"` unchanged
- [ ] Leading `/` → fail fast with clear error
- [ ] `..` segment → fail fast with clear error
- [ ] No `WorkValidationException` from `sourcesJar` or any task
- [ ] Build cache works across alternating `clean build` runs
- [ ] All existing tests pass
- [ ] Gradle 5.1 – 9.x compatible

## References

- Issue #306: https://github.com/n0mer/gradle-git-properties/issues/306
- PRD: docs/prd-git-properties-jar-path.md
- Root cause analysis: conversation context
