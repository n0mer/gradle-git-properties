# Architecture Design: Grgit to JGit 7.x Migration

**Date:** 2026-05-21
**Branch:** `feature/jgit-migration`
**Target Version:** 3.0.0

---

## 1. Executive Summary

This document outlines the migration from Grgit (archived, JGit 5.x) to JGit 7.x directly. The migration introduces a `GitFacade` layer that provides Grgit-compatible API for custom properties while using JGit 7.x internally.

Key changes:
- Replace `org.ajoberstar.grgit:grgit-core:4.1.1` with `org.eclipse.jgit:org.eclipse.jgit:7.x`
- Introduce `GitFacade` + `GitCommit` to preserve custom properties API
- Native git worktree support via proper JGit repository detection
- Bump Java requirement to 17 (JGit 7.x requirement)

---

## 2. Current Architecture

### 2.1 Component Diagram (Current)

```
+---------------------------+
|     GitProperties         |  Entry point
+------------+--------------+
             |
             v opens Grgit
+---------------------------+
|      Grgit.open()         |  org.ajoberstar.grgit
+------------+--------------+
             |
             v wraps
+---------------------------+
|   JGit 5.13.x (bundled)   |  org.eclipse.jgit
+---------------------------+

Properties call:
  repo.head().id
  repo.branch.current().name
  repo.describe()
  repo.status().clean
```

### 2.2 Files Affected

| File | Lines | Grgit Usage |
|------|-------|-------------|
| `GitProperties.groovy` | 108 | `Grgit.open()`, passes `repo` to closures |
| `AbstractGitProperty.groovy` | 14 | `isEmpty(Grgit repo)` |
| `BranchProperty.groovy` | 114 | `repo.branch.current().name` |
| `CommitIdProperty.groovy` | 10 | `repo.head().id` |
| `CommitIdAbbrevProperty.groovy` | 10 | `repo.head().abbreviatedId` |
| `CommitUserNameProperty.groovy` | 10 | `repo.head().author.name` |
| `CommitUserEmailProperty.groovy` | 10 | `repo.head().author.email` |
| `CommitMessageShortProperty.groovy` | 10 | `repo.head().shortMessage` |
| `CommitMessageFullProperty.groovy` | 10 | `repo.head().fullMessage` |
| `CommitTimeProperty.groovy` | 33 | `repo.head().dateTime` |
| `CommitIdDescribeProperty.groovy` | 42 | `repo.describe()`, `repo.status().clean` |
| `DirtyProperty.groovy` | 9 | `repo.status().clean` |
| `TagsProperty.groovy` | 14 | `repo.tag.list()`, `repo.head()` |
| `ClosestTagNameProperty.groovy` | 42 | `repo.describe(longDescr: true)` |
| `ClosestTagCommitCountProperty.groovy` | 40 | `repo.describe(longDescr: true)` |
| `TotalCommitCountProperty.groovy` | 14 | via `CacheSupport` |
| `RemoteOriginUrlProperty.groovy` | 36 | `repo.repository.jgit.repository.config` |
| `BuildUserNameProperty.groovy` | 15 | `repo.repository.jgit.repository.config` |
| `BuildUserEmailProperty.groovy` | 15 | `repo.repository.jgit.repository.config` |
| `CacheSupport.groovy` | 39 | `repo.describe()`, `repo.repository.jgit.log()` |
| `GitRepositoryBuilder.groovy` (test) | 63 | Test helper, Grgit for setup |

---

## 3. Target Architecture

### 3.1 Component Diagram (Target)

```
+---------------------------+
|     GitProperties         |  Entry point
+------------+--------------+
             |
             v creates
+---------------------------+
|       GitFacade           |  Grgit-compatible API facade
| +------------------------+|
| | GitCommit              ||  head() returns this
| | GitBranch              ||  branch.current() returns this
| | GitStatus              ||  status() returns this
| +------------------------+|
+------------+--------------+
             |
             v wraps (internal)
+---------------------------+
|   JGit 7.x Repository     |  org.eclipse.jgit (relocated)
|   + Git command API       |
+---------------------------+

Custom properties:
  facade.head().id          -> GitCommit.id
  facade.branch.current()   -> GitBranch
  facade.describe()         -> JGit DescribeCommand
  facade.status().clean     -> GitStatus.clean
  facade.jgit               -> Repository (escape hatch)
  facade.jgitCommands       -> Git (escape hatch)
```

### 3.2 Class Diagram

```
+---------------------------------------+
|            GitFacade                  |
+---------------------------------------+
| - repository: Repository              |
| - git: Git                            |
+---------------------------------------+
| + open(File): GitFacade               | static factory
| + close(): void                       |
| + head(): GitCommit                   |
| + branch: GitBranch                   |
| + status(): GitStatus                 |
| + describe(Map?): String              |
| + tag: GitTagService                  |
| + log(Map?): List<GitCommit>          |
| + jgit: Repository                    | escape hatch
| + jgitCommands: Git                   | escape hatch
+---------------------------------------+

+---------------------------------------+
|            GitCommit                  |
+---------------------------------------+
| - commit: RevCommit                   |
| - repository: Repository              |
+---------------------------------------+
| + id: String                          |
| + abbreviatedId: String               |
| + author: GitPerson                   |
| + committer: GitPerson                |
| + dateTime: ZonedDateTime             |
| + shortMessage: String                |
| + fullMessage: String                 |
+---------------------------------------+

+---------------------------------------+
|            GitPerson                  |
+---------------------------------------+
| + name: String                        |
| + email: String                       |
+---------------------------------------+

+---------------------------------------+
|            GitBranch                  |
+---------------------------------------+
| - repository: Repository              |
+---------------------------------------+
| + current(): GitBranchInfo            |
+---------------------------------------+

+---------------------------------------+
|          GitBranchInfo                |
+---------------------------------------+
| + name: String                        |
| + fullName: String                    |
+---------------------------------------+

+---------------------------------------+
|            GitStatus                  |
+---------------------------------------+
| - status: Status                      |
+---------------------------------------+
| + clean: boolean                      |
| + added: Set<String>                  |
| + modified: Set<String>               |
| + removed: Set<String>                |
| + untracked: Set<String>              |
+---------------------------------------+

+---------------------------------------+
|          GitTagService                |
+---------------------------------------+
| - git: Git                            |
+---------------------------------------+
| + list(): List<GitTag>                |
+---------------------------------------+

+---------------------------------------+
|             GitTag                    |
+---------------------------------------+
| + name: String                        |
| + commit: GitCommit                   |
+---------------------------------------+
```

### 3.3 File Organization

```
src/main/groovy/com/gorylenko/
  GitProperties.groovy            # Modified: use GitFacade instead of Grgit
  GitPropertiesPlugin.groovy      # Unchanged
  GenerateGitPropertiesTask.groovy # Unchanged
  PropertiesFileWriter.groovy     # Unchanged

  jgit/                           # NEW: JGit facade package
    GitFacade.groovy              # Main facade class
    GitCommit.groovy              # Commit wrapper
    GitPerson.groovy              # Author/committer wrapper
    GitBranch.groovy              # Branch operations
    GitBranchInfo.groovy          # Branch info DTO
    GitStatus.groovy              # Status wrapper
    GitTagService.groovy          # Tag operations
    GitTag.groovy                 # Tag wrapper
    RepositoryFactory.groovy      # Factory with worktree support

  properties/                     # Modified: use GitFacade
    AbstractGitProperty.groovy    # Modified: GitFacade param
    BranchProperty.groovy         # Modified
    CommitIdProperty.groovy       # Modified
    ... (all 18 properties)
```

---

## 4. Migration Strategy

### 4.1 Dual-Path Approach

During migration, we maintain **both** Grgit and JGit paths:

```groovy
// Phase 2-3: Properties can work with either
class AbstractGitProperty extends Closure<String> {
    boolean isEmpty(Object repo) {
        if (repo instanceof GitFacade) {
            return repo.isEmpty()
        } else {
            // Legacy Grgit path
            return !repo.repository.jgit.repository.resolve('HEAD')
        }
    }
}
```

This allows:
1. Incremental migration (one property at a time)
2. Easy rollback if issues found
3. Tests can verify both paths produce same output

### 4.2 Phase Breakdown

| Phase | Description | Outcome |
|-------|-------------|---------|
| 0 | Test infrastructure | Test helpers work with JGit directly |
| 1 | GitFacade core | Standalone facade with unit tests |
| 2 | Property migration | Each property uses GitFacade, TDD |
| 3 | Integration | Wire GitFacade into GitProperties |
| 4 | Dependency swap | Remove Grgit, JGit 7.x only |
| 5 | Finalize | Docs, CI, version bump |

---

## 5. Task Breakdown

### Phase 0: Test Infrastructure

| ID | Task | Test | Files |
|----|------|------|-------|
| 0.1 | Create JGit test repository builder | Test can create repo, commit, tag using pure JGit | `src/test/groovy/.../jgit/JGitTestHelper.groovy` |

### Phase 1: GitFacade Core (Standalone, No Integration)

| ID | Task | Test | Files |
|----|------|------|-------|
| 1.1 | RepositoryFactory.open() with worktree | Opens worktree dir, returns correct branch | `RepositoryFactory.groovy`, `RepositoryFactoryTest.groovy` |
| 1.2 | GitFacade.open() + close() | Opens repo, closes without leak | `GitFacade.groovy`, `GitFacadeTest.groovy` |
| 1.3 | GitFacade.isEmpty() | Returns true for empty repo, false otherwise | `GitFacade.groovy` |
| 1.4 | GitCommit + GitPerson | head() returns commit with id, abbreviatedId, author, dateTime, messages | `GitCommit.groovy`, `GitPerson.groovy` |
| 1.5 | GitBranch.current() | Returns current branch name | `GitBranch.groovy`, `GitBranchInfo.groovy` |
| 1.6 | GitStatus.clean | Returns true/false for dirty state | `GitStatus.groovy` |
| 1.7 | GitFacade.describe() | Returns describe output with options | `GitFacade.groovy` |
| 1.8 | GitTagService.list() | Returns tags pointing to HEAD | `GitTagService.groovy`, `GitTag.groovy` |
| 1.9 | GitFacade.log() | Returns commit history | `GitFacade.groovy` |
| 1.10 | Escape hatches (jgit, jgitCommands) | Returns raw JGit objects | `GitFacade.groovy` |
| 1.11 | Config access | Read user.name, user.email, remote.origin.url | `GitFacade.groovy` |

### Phase 2: Property Migration (TDD, One at a Time)

| ID | Task | Test | Files |
|----|------|------|-------|
| 2.1 | AbstractGitProperty accepts GitFacade | isEmpty works with GitFacade | `AbstractGitProperty.groovy` |
| 2.2 | CommitIdProperty | Returns full SHA via GitFacade | `CommitIdProperty.groovy`, test |
| 2.3 | CommitIdAbbrevProperty | Returns abbreviated SHA | `CommitIdAbbrevProperty.groovy` |
| 2.4 | CommitUserNameProperty | Returns author name | `CommitUserNameProperty.groovy` |
| 2.5 | CommitUserEmailProperty | Returns author email | `CommitUserEmailProperty.groovy` |
| 2.6 | CommitMessageShortProperty | Returns first line | `CommitMessageShortProperty.groovy` |
| 2.7 | CommitMessageFullProperty | Returns full message | `CommitMessageFullProperty.groovy` |
| 2.8 | CommitTimeProperty | Returns formatted timestamp | `CommitTimeProperty.groovy` |
| 2.9 | BranchProperty | Returns branch name (incl. CI env vars) | `BranchProperty.groovy` |
| 2.10 | DirtyProperty | Returns dirty state | `DirtyProperty.groovy` |
| 2.11 | CommitIdDescribeProperty | Returns describe with dirty suffix | `CommitIdDescribeProperty.groovy` |
| 2.12 | TagsProperty | Returns tags at HEAD | `TagsProperty.groovy` |
| 2.13 | ClosestTagNameProperty | Returns nearest tag name | `ClosestTagNameProperty.groovy` |
| 2.14 | ClosestTagCommitCountProperty | Returns commits since tag | `ClosestTagCommitCountProperty.groovy` |
| 2.15 | TotalCommitCountProperty | Returns total commit count | `TotalCommitCountProperty.groovy`, `CacheSupport.groovy` |
| 2.16 | RemoteOriginUrlProperty | Returns sanitized remote URL | `RemoteOriginUrlProperty.groovy` |
| 2.17 | BuildUserNameProperty | Returns git config user.name | `BuildUserNameProperty.groovy` |
| 2.18 | BuildUserEmailProperty | Returns git config user.email | `BuildUserEmailProperty.groovy` |
| 2.19 | BuildVersionProperty | Returns project version (no change needed) | Verify only |
| 2.20 | BuildHostProperty | Returns hostname (no change needed) | Verify only |

### Phase 3: Integration

| ID | Task | Test | Files |
|----|------|------|-------|
| 3.1 | GitProperties uses GitFacade | All 18 properties generate correctly | `GitProperties.groovy` |
| 3.2 | Worktree functional test passes | Branch/commit correct in worktree | `RealWorktreeFunctionalTest.groovy` |
| 3.3 | Custom properties work | Closure receives GitFacade | `BasicFunctionalTest.groovy` |

### Phase 4: Dependency Swap

| ID | Task | Test | Files |
|----|------|------|-------|
| 4.1 | Remove Grgit dependency | Compiles without Grgit | `build.gradle` |
| 4.2 | Add JGit 7.x | All tests pass | `build.gradle` |
| 4.3 | Update shadow relocations | No JGit conflicts | `build.gradle` |
| 4.4 | Update Java version | Java 17 minimum | `build.gradle` |
| 4.5 | Remove test Grgit usage | GitRepositoryBuilder uses JGit | `GitRepositoryBuilder.groovy` |

### Phase 5: Finalize

| ID | Task | Test | Files |
|----|------|------|-------|
| 5.1 | Update README | Documents Java 17, new API | `README.md` |
| 5.2 | Create migration guide | Documents breaking changes | `MIGRATION.md` |
| 5.3 | Update CHANGELOG | Lists all changes | `CHANGELOG.md` |
| 5.4 | Bump version to 3.0.0 | Version correct | `build.gradle` |
| 5.5 | CI configuration | Tests on Java 17, 21 | `.github/workflows/` |

---

## 6. Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| JGit 7.x API incompatibility | Medium | High | Research JGit 7.x API changes, use compatibility shims |
| Git worktree edge cases | Medium | Medium | Comprehensive worktree tests (existing + new) |
| Custom property breakage | Low | High | Facade preserves exact Grgit API surface |
| Performance regression | Low | Medium | Benchmark before/after, especially for large repos |
| Shallow clone handling | Medium | Medium | Explicit tests for shallow clone scenarios |
| CI environment detection | Low | Low | Preserve existing CI env var logic |

### 6.1 JGit 7.x Known Changes

From JGit changelog:
- `RefDatabase#getRef(String)` removed - use `exactRef(String)` or `findRef(String)`
- Some internal API changes

Mitigation: Use public API only, test against JGit 7.x from start.

---

## 7. Estimated File Changes

### New Files (9)

| File | Purpose | Est. Lines |
|------|---------|------------|
| `src/main/groovy/.../jgit/GitFacade.groovy` | Main facade | ~150 |
| `src/main/groovy/.../jgit/GitCommit.groovy` | Commit wrapper | ~60 |
| `src/main/groovy/.../jgit/GitPerson.groovy` | Person DTO | ~15 |
| `src/main/groovy/.../jgit/GitBranch.groovy` | Branch service | ~30 |
| `src/main/groovy/.../jgit/GitBranchInfo.groovy` | Branch info DTO | ~15 |
| `src/main/groovy/.../jgit/GitStatus.groovy` | Status wrapper | ~40 |
| `src/main/groovy/.../jgit/GitTagService.groovy` | Tag service | ~40 |
| `src/main/groovy/.../jgit/GitTag.groovy` | Tag DTO | ~20 |
| `src/main/groovy/.../jgit/RepositoryFactory.groovy` | Factory with worktree | ~80 |

### Modified Files (21)

| File | Changes |
|------|---------|
| `build.gradle` | Deps: remove Grgit, add JGit 7.x, Java 17 |
| `GitProperties.groovy` | Use GitFacade instead of Grgit |
| `AbstractGitProperty.groovy` | Accept GitFacade |
| `BranchProperty.groovy` | Use GitFacade |
| `CommitIdProperty.groovy` | Use GitFacade |
| `CommitIdAbbrevProperty.groovy` | Use GitFacade |
| `CommitUserNameProperty.groovy` | Use GitFacade |
| `CommitUserEmailProperty.groovy` | Use GitFacade |
| `CommitMessageShortProperty.groovy` | Use GitFacade |
| `CommitMessageFullProperty.groovy` | Use GitFacade |
| `CommitTimeProperty.groovy` | Use GitFacade |
| `CommitIdDescribeProperty.groovy` | Use GitFacade |
| `DirtyProperty.groovy` | Use GitFacade |
| `TagsProperty.groovy` | Use GitFacade |
| `ClosestTagNameProperty.groovy` | Use GitFacade |
| `ClosestTagCommitCountProperty.groovy` | Use GitFacade |
| `TotalCommitCountProperty.groovy` | Use GitFacade |
| `RemoteOriginUrlProperty.groovy` | Use GitFacade |
| `BuildUserNameProperty.groovy` | Use GitFacade |
| `BuildUserEmailProperty.groovy` | Use GitFacade |
| `CacheSupport.groovy` | Use GitFacade |

### Test Files

| File | Status |
|------|--------|
| `src/test/groovy/.../jgit/JGitTestHelper.groovy` | NEW |
| `src/test/groovy/.../jgit/GitFacadeTest.groovy` | NEW |
| `src/test/groovy/.../jgit/RepositoryFactoryTest.groovy` | NEW |
| `src/test/groovy/.../properties/GitRepositoryBuilder.groovy` | MODIFY (use JGit) |
| Existing property tests | MODIFY (run against GitFacade) |

---

## 8. Success Criteria

1. `./gradlew check` passes
2. All 18 properties generate correctly with JGit 7.x
3. `RealWorktreeFunctionalTest` passes (worktree bug fixed)
4. `BackwardCompatibilityFunctionalTest` passes for Gradle 7.x, 8.x (skip 5.x/6.x with Java 17)
5. No `org.ajoberstar.grgit` classes in shadow JAR
6. CI passes on Java 17 and 21
7. Custom properties API (`it.head().id`, etc.) works as documented

---

## 9. Open Questions

1. **Gradle 5.x/6.x support?** With Java 17 requirement, older Gradle versions that don't support Java 17 will be dropped. Is this acceptable?
   - **Answer:** Yes per SPEC (major version bump to 3.0.0)

2. **JGit version pinning?** Should we pin to exact JGit version or allow range?
   - **Recommendation:** Pin to `7.0.0.202409031743-r` or latest stable 7.x

3. **Test helper rewrite scope?** `GitRepositoryBuilder` uses Grgit extensively. Full rewrite or gradual?
   - **Recommendation:** Full rewrite to JGit in Phase 0, enables clean TDD

---

## 10. Appendix: Grgit API Mapping

| Grgit API | JGit Equivalent |
|-----------|-----------------|
| `Grgit.open(dir: x)` | `Git.open(x)` or `FileRepositoryBuilder` |
| `repo.head()` | `repo.repository.parseCommit(repo.repository.resolve("HEAD"))` |
| `repo.head().id` | `revCommit.getName()` (40-char SHA) |
| `repo.head().abbreviatedId` | `repo.repository.newObjectReader().abbreviate(id).name()` |
| `repo.head().author.name` | `revCommit.authorIdent.name` |
| `repo.head().author.email` | `revCommit.authorIdent.emailAddress` |
| `repo.head().dateTime` | `Instant.ofEpochSecond(revCommit.commitTime)` |
| `repo.head().shortMessage` | `revCommit.shortMessage` |
| `repo.head().fullMessage` | `revCommit.fullMessage` |
| `repo.branch.current().name` | `repo.repository.branch` |
| `repo.describe()` | `git.describe().call()` |
| `repo.describe(longDescr: true)` | `git.describe().setLong(true).call()` |
| `repo.describe(tags: true)` | `git.describe().setTags(true).call()` |
| `repo.status().clean` | `git.status().call().isClean()` |
| `repo.tag.list()` | `git.tagList().call()` |
| `repo.log(maxCommits: n)` | `git.log().setMaxCount(n).call()` |
| `repo.repository.jgit.repository` | `git.repository` |
| `repo.repository.rootDir` | `git.repository.workTree` |

---

SIGNAL: TASK_DONE
