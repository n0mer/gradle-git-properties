# PLAN: Grgit to JGit 7.x Migration

**Created:** 2026-05-21
**Architect Output:** `.agency/results/architect.md`

---

## Approach

1. **Incremental migration** — harness green after each task
2. **Dual paths during transition** — Grgit and GitFacade coexist
3. **TDD** — each task = RED → GREEN (vertical slicing)
4. **Sensor:** `./gradlew check`

---

## Task Summary

| Phase | Tasks | Description |
|-------|-------|-------------|
| 0 | 1 | Test infrastructure |
| 1 | 11 | GitFacade core (standalone) |
| 2 | 20 | Property migration |
| 3 | 3 | Integration |
| 4 | 5 | Dependency swap |
| 5 | 5 | Finalize |
| **Total** | **45** | |

---

## Phase 0: Test Infrastructure

| ID | Task | Behavior to verify |
|----|------|-------------------|
| 0.1 | JGit test helper | Can create repo, commit, tag, worktree using pure JGit |

---

## Phase 1: GitFacade Core

| ID | Task | Behavior to verify |
|----|------|-------------------|
| 1.1 | RepositoryFactory.open() | Opens worktree dir, returns correct repository |
| 1.2 | GitFacade.open() + close() | Opens and closes without resource leak |
| 1.3 | GitFacade.isEmpty() | Returns true for empty repo, false otherwise |
| 1.4 | GitCommit + GitPerson | head() returns id, abbreviatedId, author, dateTime, messages |
| 1.5 | GitBranch.current() | Returns current branch name, works in worktree |
| 1.6 | GitStatus.clean | Returns true/false for dirty state |
| 1.7 | GitFacade.describe() | Returns describe output with tags/longDescr options |
| 1.8 | GitTagService.list() | Returns tags, empty list when none |
| 1.9 | GitFacade.log() | Returns commit history with maxCommits option |
| 1.10 | Escape hatches | jgit returns Repository, jgitCommands returns Git |
| 1.11 | Config access | Reads user.name, user.email, remote.origin.url |

---

## Phase 2: Property Migration

| ID | Task | Behavior to verify |
|----|------|-------------------|
| 2.1 | AbstractGitProperty | isEmpty() works with GitFacade |
| 2.2 | CommitIdProperty | Returns full SHA via GitFacade |
| 2.3 | CommitIdAbbrevProperty | Returns abbreviated SHA |
| 2.4 | CommitUserNameProperty | Returns author name |
| 2.5 | CommitUserEmailProperty | Returns author email |
| 2.6 | CommitMessageShortProperty | Returns first line |
| 2.7 | CommitMessageFullProperty | Returns full message |
| 2.8 | CommitTimeProperty | Returns formatted timestamp |
| 2.9 | BranchProperty | Returns branch name (incl. worktree, CI vars) |
| 2.10 | DirtyProperty | Returns dirty state |
| 2.11 | CommitIdDescribeProperty | Returns describe with dirty suffix |
| 2.12 | TagsProperty | Returns tags at HEAD |
| 2.13 | ClosestTagNameProperty | Returns nearest tag name |
| 2.14 | ClosestTagCommitCountProperty | Returns commits since tag |
| 2.15 | TotalCommitCountProperty | Returns total commit count |
| 2.16 | RemoteOriginUrlProperty | Returns sanitized remote URL |
| 2.17 | BuildUserNameProperty | Returns git config user.name |
| 2.18 | BuildUserEmailProperty | Returns git config user.email |
| 2.19 | BuildVersionProperty | Verify unchanged (no git dependency) |
| 2.20 | BuildHostProperty | Verify unchanged (no git dependency) |

---

## Phase 3: Integration

| ID | Task | Behavior to verify |
|----|------|-------------------|
| 3.1 | GitProperties uses GitFacade | All 18 properties generate correctly |
| 3.2 | Worktree functional test | Branch/commit correct in worktree |
| 3.3 | Custom properties | Closure receives GitFacade, escape hatch works |

---

## Phase 4: Dependency Swap

| ID | Task | Behavior to verify |
|----|------|-------------------|
| 4.1 | Remove Grgit dependency | Compiles without Grgit |
| 4.2 | Add JGit 7.x | All tests pass with JGit 7.x |
| 4.3 | Update shadow relocations | JGit relocated, no conflicts |
| 4.4 | Update Java version | Java 17 minimum enforced |
| 4.5 | Remove test Grgit usage | GitRepositoryBuilder uses JGit only |

---

## Phase 5: Finalize

| ID | Task | Behavior to verify |
|----|------|-------------------|
| 5.1 | Update README | Documents Java 17, facade API, escape hatch |
| 5.2 | Create migration guide | MIGRATION.md with breaking changes |
| 5.3 | Update CHANGELOG | All changes documented |
| 5.4 | Bump version | build.gradle shows 3.0.0 |
| 5.5 | CI configuration | Tests pass on Java 17 and 21 |

---

## Execution Order

Tasks execute sequentially within phases. Each task:
1. **RED:** Write failing test
2. **GREEN:** Minimal code to pass
3. **Verify:** `./gradlew check` green
4. **Commit:** Atomic commit for task

---

## Role Assignments

| Phase | Primary Role | Gate |
|-------|--------------|------|
| 0-1 | Implementer | Code Gate |
| 2 | Implementer | Code Gate (after each property group) |
| 3 | Implementer | Code Gate |
| 4 | Implementer | Code Gate |
| 5 | Documenter | Doc Gate |
| All | Tester | Test Gate (after impl) |
| All | Reviewer | Verification Gate |

---

## Success Criteria

1. `./gradlew check` passes
2. All 18 properties generate correctly
3. Worktree tests pass
4. No Grgit classes in plugin JAR
5. CI passes on Java 17 and 21
