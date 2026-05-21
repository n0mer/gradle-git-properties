# JGit/Grgit Worktree Verification

## Versions
- JGit: 5.13.3.202401111512-r (strictly enforced in buildscript)
- Grgit: 4.1.1

## JGit Worktree Support Status

**Official Status:** JGit 7.0.0 added basic worktree read support (change 1194900). However, this plugin uses JGit 5.x.

**Empirically Verified:** JGit 5.x DOES support worktrees via `FileRepositoryBuilder.findGitDir()`. The key is how you open the repository.

Key GitHub issue: https://github.com/eclipse-jgit/jgit/issues/264 (Feature Request: Git Worktree Support)
- Basic worktree read support exists in BaseRepositoryBuilder
- `setupCommonDir()` handles $GIT_COMMON_DIR
- `setupWorkTree()` handles worktree-specific index/HEAD
- Management commands (add, remove, list) are NOT implemented yet

## Empirical Test Results

### Test: FileRepositoryBuilder.setGitDir(worktreeGitDir)
- Branch: `feature-branch` (CORRECT)
- Directory: `.git/worktrees/feature-worktree`
- isBare: true

### Test: FileRepositoryBuilder.setGitDir(worktreeGitDir).setWorkTree(worktreeDir)
- Branch: `feature-branch` (CORRECT)
- Directory: `.git/worktrees/feature-worktree`
- WorkTree: `/feature-worktree`
- isBare: false

### Test: FileRepositoryBuilder.findGitDir(worktreeDir)
- GitDir found: `.git/worktrees/feature-worktree` (CORRECT - follows .git file!)
- Branch: `feature-branch` (CORRECT)
- isBare: true

### Test: Git.open(mainGitDir) - CURRENT PLUGIN BEHAVIOR (THE BUG)
- Branch: `master` (WRONG - shows main repo branch, not worktree)
- WorkTree: `/main-repo` (WRONG - points to main repo)

### Test: Grgit.open(currentDir: worktreeDir) - POTENTIAL FIX
- Branch: `feature-branch` (CORRECT)
- Directory: `.git/worktrees/feature-worktree`
- **This approach WORKS**

### Test: Grgit.open(dir: worktreeGitDir)
- **FAILS with RepositoryNotFoundException**
- The `dir:` parameter expects a working directory, not a .git directory path

## Bug Confirmation

The functional test `RealWorktreeFunctionalTest.testBranchNameIsCorrectInWorktree` FAILS:
```
expected:<[feature-branch]> but was:<[master]>
```

This confirms the bug exists in the current plugin.

## Conclusion

### Does JGit/Grgit actually support worktrees properly?
**YES** - but only when opened correctly.

### Is Option A (`Grgit.open(currentDir:)`) viable?
**YES** - empirically verified to work.

### Root Cause Analysis

The plugin currently:
1. Finds the .git directory (which correctly resolves `.git` FILE to the worktree's git dir)
2. But then calls `Grgit.open(dir: dotGitDirectory)` passing the MAIN .git directory

The `dir:` parameter in Grgit expects a working directory and internally calls `Git.open(file)` which assumes it's a regular repo structure.

### Recommended Fix

Change `GitProperties.groovy` line 69 from:
```groovy
def repo = Grgit.open(dir: dotGitDirectory)
```

To:
```groovy
def repo = Grgit.open(currentDir: dotGitDirectory.parentFile ?: dotGitDirectory)
```

Or better: pass the project directory (working directory) to `GitProperties.generate()` and use:
```groovy
def repo = Grgit.open(currentDir: projectDir)
```

The `currentDir:` parameter uses `FileRepositoryBuilder.findGitDir()` which correctly:
1. Reads the .git file in worktrees
2. Follows the `gitdir:` pointer
3. Opens the worktree-specific repository with correct HEAD/branch

### Alternative: Direct JGit Approach

If more control is needed:
```groovy
def builder = new FileRepositoryBuilder()
    .findGitDir(projectDir)
    .readEnvironment()
def jgitRepo = builder.build()
def grgit = Grgit.open(dir: jgitRepo.workTree ?: projectDir)
```

But `Grgit.open(currentDir:)` handles this internally.
