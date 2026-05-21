# Specification: JGit Migration Validation

## Overview

Validate that the gradle-git-properties plugin v3.0.0 (JGit-based) produces identical output to v2.5.7 (Grgit-based) across all features and scenarios.

## Context

- Issue #289 migrates from Grgit (archived, unmaintained) to JGit 7.x
- This is a breaking change requiring Java 17+
- Both versions are installed in local Maven repo:
  - `com.gorylenko.gradle-git-properties:gradle-git-properties:2.5.7` (Grgit)
  - `com.gorylenko.gradle-git-properties:gradle-git-properties:3.0.0` (JGit)

## Requirements

### R1: Property Parity
All 18 generated properties must produce identical output:
1. `git.branch`
2. `git.commit.id`
3. `git.commit.id.abbrev`
4. `git.commit.id.describe`
5. `git.commit.time`
6. `git.commit.message.short`
7. `git.commit.message.full`
8. `git.commit.user.name`
9. `git.commit.user.email`
10. `git.build.host`
11. `git.build.user.name`
12. `git.build.user.email`
13. `git.build.version`
14. `git.dirty`
15. `git.tags`
16. `git.closest.tag.name`
17. `git.closest.tag.commit.count`
18. `git.remote.origin.url`
19. `git.total.commit.count`

### R2: Configuration Options Parity
All configuration options must work identically:
- `gitPropertiesName` - custom filename
- `gitPropertiesResourceDir` - custom output directory
- `dotGitDirectory` - explicit .git path
- `keys` - subset of properties
- `customProperties` - static values and closures
- `dateFormat` - custom date format
- `dateFormatTimeZone` - timezone setting
- `branch` - override branch name
- `extProperty` - expose to project.ext
- `failOnNoGitDirectory` - missing .git handling

### R3: Custom Properties API Parity
GitFacade API must return equivalent values:
- `head()` - GitCommit properties
- `status()` - clean/dirty state
- `describe(options)` - tags: true, longDescr: true
- `log(options)` - maxCommits limit
- `branch.current()` - branch name
- `tag.list()` - all tags
- `tag.listOnCommit(id)` - tags on specific commit
- `tag.closest()` - ClosestTag (name, distance)
- `getConfig(section, name)` - git config values
- `isEmpty()` - empty repo detection

### R4: Special Scenarios
Both versions must handle edge cases identically:
- Normal repo (clean, on branch, with tags)
- Detached HEAD (branch = abbreviated SHA)
- No tags (empty strings for tag properties)
- Dirty workdir (staged, unstaged, untracked)
- Shallow clone (limited history)
- Git worktree
- Submodule
- Empty repo (no commits)
- Multi-project builds

### R5: DSL Variants
Both Groovy and Kotlin DSL must work identically.

## Test Matrix (26 scenarios)

### Phase 1: Core Properties (7 scenarios)
1. clean-with-tags - Normal repo, on branch, with annotated tag
2. clean-no-tags - No tags at all
3. dirty-staged - Staged but uncommitted changes
4. dirty-unstaged - Modified but not staged
5. dirty-untracked - Untracked files
6. detached-head - Checkout specific commit SHA
7. lightweight-tags - Lightweight tags only

### Phase 2: Configuration Options (7 scenarios)
8. custom-filename - `gitPropertiesName = "custom.properties"`
9. custom-dir - `gitPropertiesResourceDir = custom path`
10. subset-keys - `keys = ['git.branch', 'git.commit.id']`
11. date-formats - Custom dateFormat + timezone
12. branch-override - Explicit `branch = "override"`
13. ext-property - `extProperty = 'gitProps'`
14. fail-on-missing - `failOnNoGitDirectory = false` with no .git

### Phase 3: Custom Properties API (5 scenarios)
15. custom-static - `customProperty 'key', 'value'`
16. custom-closure - `customProperty 'key', { it.head().id }`
17. api-methods - Test all GitFacade methods via closures
18. override-standard - Override `git.commit.id.describe`
19. escape-hatch - Access `jgit` and `jgitCommands`

### Phase 4: Special Scenarios (5 scenarios)
20. shallow-clone - `git clone --depth=1`
21. worktree - `git worktree add`
22. submodule - Nested submodule
23. empty-repo - No commits
24. multi-project - Root + subproject

### Phase 5: DSL Variants (2 scenarios)
25. kotlin-dsl - Kotlin build script
26. kotlin-closure - KotlinClosure1 custom properties

## Acceptance Criteria

- All scenarios pass with no diff (excluding timestamp-based properties)
- Any intentional behavior changes are documented
- Test harness is reusable for future regression testing

## Known Acceptable Differences

- `git.build.time` - always differs (build timestamp)
- Property ordering in file - content matters, not order

## Test Location

`/tmp/git-props-validation/`

## Constraints

- Tests must use the project's gradlew (Gradle 8.14.4)
- Both plugin versions must point to the same git repo per scenario
- Comparison must filter out acceptable differences before diffing
