# Worktree Commit History Verification

## Test Setup
- Main repo: commit A on master
- Worktree: commits A+B on feature branch
- Test file: `WorktreeCommitHistoryTest.groovy`

## Results

### Commit ID Test (repo.head().id)

| Item | Value |
|------|-------|
| Main repo HEAD (commit A) | `68265c80f98f7135f6a8e6e37721d654d29b6c38` |
| Worktree HEAD (commit B) | `4d863da638f16c6f3e4786cdff07d2f91d8e30c3` |
| `grgit.head()?.id` via `Grgit.open(currentDir: worktreeDir)` | `null` |
| `repo.resolve(Constants.HEAD)` via JGit FileRepositoryBuilder | `null` |

**FAIL**: Neither Grgit nor JGit's standard API returns the worktree's HEAD commit.

### Commit History Test (repo.log())

| Item | Result |
|------|--------|
| `grgit.log()` | Not testable (throws NPE since head() is null) |
| JGit RevWalk | Not testable (resolve(HEAD) returns null) |

**FAIL**: Cannot retrieve commit history via standard API.

### Branch Name Test

| Item | Value |
|------|-------|
| Expected branch | `feature` |
| `grgit.branch.current().name` | `feature` |
| `repo.branch` (JGit) | `feature` |

**PASS**: Branch name is correctly detected for worktrees.

### Dirty Status Test

| Item | Result |
|------|--------|
| Modified file in worktree | `tracked.txt` |
| `grgit.status()` | **FAILS** (NoWorkTreeException - treats worktree as bare) |
| JGit `git.status().call()` with proper setWorkTree | `[tracked.txt]` |

**PARTIAL**: Dirty status works ONLY with JGit when `setWorkTree()` is explicitly called.

### Manual HEAD Resolution

The worktree's HEAD can be resolved manually by reading the files directly:

```
Worktree git dir: .git/worktrees/feature-worktree/
Worktree HEAD file content: ref: refs/heads/feature
Resolved ref SHA (from main .git/refs/heads/feature): 4d863da638f16c6f3e4786cdff07d2f91d8e30c3
```

**PASS**: Manual file reading provides correct HEAD.

## Summary Table

| Operation | Grgit.open(currentDir:) | JGit with setGitDir+setWorkTree |
|-----------|------------------------|--------------------------------|
| Branch name | WORKS | WORKS |
| head()/resolve(HEAD) | FAILS (null) | FAILS (null) |
| log() | FAILS | FAILS |
| status() | FAILS (bare repo error) | WORKS |
| isBare | false | false |

## Conclusion

**Does `Grgit.open(currentDir:)` fully support worktree's independent history?**

**NO** - Only partial support:

1. **Branch detection**: WORKS correctly
2. **HEAD commit retrieval**: FAILS - returns null
3. **Commit history (log)**: FAILS - cannot be retrieved
4. **Dirty status**: FAILS with Grgit, WORKS with JGit when setWorkTree is used

### Root Cause

JGit (and by extension Grgit) does not properly read the worktree-specific HEAD file located at `.git/worktrees/<worktree-name>/HEAD`. The `resolve(Constants.HEAD)` and `exactRef("HEAD")` methods look for HEAD in the wrong location.

### Workarounds

1. **For branch detection**: `Grgit.open(currentDir:)` works correctly
2. **For HEAD/log**: Must manually read the worktree's HEAD file and resolve the ref
3. **For dirty status**: Use JGit directly with explicit `setWorkTree(worktreeDir)` configuration

### Impact on gradle-git-properties

The plugin needs to:
1. Continue using `Grgit.open(currentDir:)` for branch detection (this works)
2. For commit-related properties (git.commit.id, git.commit.message, etc.), may need to implement manual HEAD resolution for worktrees
3. For dirty status detection in worktrees, ensure proper setWorkTree configuration is passed to JGit

## Test Output Reference

```
=== Grgit Worktree Summary ===
  Branch detection: WORKS (returns 'feature')
  head(): FAILS (returns null)
  log(): FAILS (would throw NPE)
  status(): FAILS (NoWorkTreeException - treats as bare)

=== JGit Direct Worktree Summary ===
  Branch: WORKS
  isBare: WORKS (false)
  WorkTree: WORKS
  resolve(HEAD): FAILS (null)

=== Isolation Summary ===
  Branch isolation: WORKS (main=master, worktree=feature)
  HEAD isolation: PARTIAL (requires manual read)

RESULT: Dirty status detection in worktree - PASS (with JGit setWorkTree)
```
