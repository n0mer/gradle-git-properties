# SPEC: Migrate from Grgit to JGit 7.x

**Issue:** #289
**Branch:** `feature/jgit-migration`
**Created:** 2026-05-21

---

## Summary

Replace archived Grgit dependency with JGit 7.x for long-term maintenance and native git worktree support.

---

## Background

- **Grgit is archived**: https://github.com/ajoberstar/grgit — no future updates
- **Grgit incompatible with JGit 7.x**: Breaking API changes (e.g., `RefDatabase#getRef` removed)
- **Current state**: `org.ajoberstar.grgit:grgit-core:4.1.1` with JGit 5.13.0

---

## Requirements

### Constraints

| Constraint | Value |
|------------|-------|
| Java minimum | 17 (JGit 7.x requirement) |
| Gradle support | 7.x, 8.x |
| Version bump | Major (3.0.0) — breaking change |

### Core Functionality

All 18 standard git properties must generate correctly:

| Property | Behavior |
|----------|----------|
| `git.branch` | Returns current branch name |
| `git.commit.id` | Returns full 40-character SHA |
| `git.commit.id.abbrev` | Returns abbreviated SHA |
| `git.commit.id.describe` | Returns git describe output with `-dirty` suffix when applicable |
| `git.commit.time` | Returns commit timestamp with configurable format/timezone |
| `git.commit.message.short` | Returns first line of commit message |
| `git.commit.message.full` | Returns full commit message |
| `git.commit.user.name` | Returns commit author name |
| `git.commit.user.email` | Returns commit author email |
| `git.build.host` | Returns build machine hostname |
| `git.build.user.name` | Returns git config user.name |
| `git.build.user.email` | Returns git config user.email |
| `git.build.version` | Returns project version |
| `git.dirty` | Returns true if working tree has uncommitted changes |
| `git.tags` | Returns tags pointing to HEAD |
| `git.closest.tag.name` | Returns nearest ancestor tag |
| `git.closest.tag.commit.count` | Returns commits since nearest tag |
| `git.remote.origin.url` | Returns remote origin URL |
| `git.total.commit.count` | Returns total commits in repo |

### Git Worktree Support

| Requirement |
|-------------|
| `git.branch` returns the worktree's branch, not main repo's branch |
| `git.commit.id` returns the worktree's HEAD commit |
| All properties work correctly when build runs from a git worktree directory |
| Worktree with detached HEAD handled gracefully |

### Custom Properties API (Grgit-Compatible Facade)

Provide facade to minimize migration for existing users:

| API | Behavior |
|-----|----------|
| `it.head().id` | Returns full commit SHA |
| `it.head().abbreviatedId` | Returns short SHA |
| `it.head().author.name` | Returns author name |
| `it.head().author.email` | Returns author email |
| `it.head().dateTime` | Returns commit time |
| `it.head().shortMessage` | Returns first line of commit message |
| `it.head().fullMessage` | Returns full commit message |
| `it.describe()` | Returns git describe output |
| `it.describe(tags: true)` | Includes lightweight tags |
| `it.describe(longDescr: true)` | Returns long format |
| `it.branch.current().name` | Returns current branch name |
| `it.tag.list()` | Returns list of tags |
| `it.status().clean` | Returns true/false for dirty state |
| `it.log(maxCommits: n)` | Returns commit history |

### Escape Hatch

| API | Behavior |
|-----|----------|
| `it.jgit` | Exposes `org.eclipse.jgit.lib.Repository` |
| `it.jgitCommands` | Exposes `org.eclipse.jgit.api.Git` |

### Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Empty repository (no commits) | Handled gracefully, properties return empty/null |
| Shallow clone | Describe fallback works |
| Detached HEAD | Branch returns HEAD SHA or appropriate indicator |
| Missing `.git` with `failOnNoGitDirectory = false` | Build doesn't fail |
| No tags | Describe returns null/empty gracefully |
| No remote origin | Property returns empty |

### Build & Dependencies

| Requirement |
|-------------|
| No Grgit dependency in final artifact |
| JGit 7.x dependency included and relocated (`gradlegitproperties.org.eclipse.jgit`) |
| No dependency conflicts with user projects using different JGit versions |
| Configuration cache compatible |

### Documentation

| Requirement |
|-------------|
| README documents Java 17+ requirement |
| README documents custom properties facade API |
| README documents escape hatch for advanced JGit usage |
| Migration guide for users upgrading from 2.x |
| CHANGELOG documents breaking changes |

---

## Out of Scope

- Maintaining 2.x branch (can be done later)
- Support for Java < 17
- Grgit 5.x upgrade path (going directly to JGit 7.x)

---

## Success Criteria

1. `./gradlew check` passes
2. All 18 properties generate correctly
3. Worktree tests pass
4. No Grgit classes in plugin JAR
5. CI passes on Java 17 and 21

---

## References

- GitHub Issue: https://github.com/n0mer/gradle-git-properties/issues/289
- Grgit (archived): https://github.com/ajoberstar/grgit
- JGit: https://www.eclipse.org/jgit/
- Related worktree issue: #281
