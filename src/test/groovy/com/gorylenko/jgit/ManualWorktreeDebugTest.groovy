package com.gorylenko.jgit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

/**
 * Debug test to understand JGit worktree handling
 */
class ManualWorktreeDebugTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    void debugWorktreeJGit() {
        // Create main repo
        def mainRepoDir = temporaryFolder.newFolder("main-repo")
        runGit(mainRepoDir, "init")
        new File(mainRepoDir, "test.txt").text = "test"
        runGit(mainRepoDir, "add", ".")
        runGit(mainRepoDir, "commit", "-m", "Initial")

        // Create worktree
        def worktreeDir = new File(temporaryFolder.root, "feature-worktree")
        runGit(mainRepoDir, "worktree", "add", "-b", "feature-branch", worktreeDir.absolutePath)

        println "=== Worktree .git file ==="
        def gitFile = new File(worktreeDir, ".git")
        println gitFile.text

        // Parse gitdir
        def gitDirPath = gitFile.text.trim().replace("gitdir: ", "")
        def gitDir = new File(gitDirPath)
        if (!gitDir.isAbsolute()) {
            gitDir = new File(worktreeDir, gitDirPath).canonicalFile
        }

        println "=== Testing JGit with gitDir only ==="
        def repo1 = new FileRepositoryBuilder()
            .setGitDir(gitDir)
            .build()
        println "  gitDir: ${repo1.directory}"
        println "  isBare: ${repo1.bare}"
        println "  branch: ${repo1.branch}"
        println "  HEAD: ${repo1.resolve(Constants.HEAD)}"
        repo1.close()

        println "\n=== Testing JGit with gitDir + workTree ==="
        def repo2 = new FileRepositoryBuilder()
            .setGitDir(gitDir)
            .setWorkTree(worktreeDir)
            .build()
        println "  gitDir: ${repo2.directory}"
        println "  workTree: ${repo2.workTree}"
        println "  branch: ${repo2.branch}"
        println "  HEAD: ${repo2.resolve(Constants.HEAD)}"
        repo2.close()

        println "\n=== Testing JGit with readEnvironment ==="
        def repo3 = new FileRepositoryBuilder()
            .setGitDir(gitDir)
            .setWorkTree(worktreeDir)
            .readEnvironment()
            .build()
        println "  gitDir: ${repo3.directory}"
        println "  workTree: ${repo3.workTree}"
        println "  branch: ${repo3.branch}"
        println "  HEAD: ${repo3.resolve(Constants.HEAD)}"
        repo3.close()

        println "\n=== Testing JGit with findGitDir from worktree ==="
        def repo4 = new FileRepositoryBuilder()
            .findGitDir(worktreeDir)
            .build()
        println "  gitDir: ${repo4.directory}"
        println "  isBare: ${repo4.bare}"
        println "  branch: ${repo4.branch}"
        println "  HEAD: ${repo4.resolve(Constants.HEAD)}"
        repo4.close()

        println "\n=== Testing JGit with setWorkTree only ==="
        def repo5 = new FileRepositoryBuilder()
            .setWorkTree(worktreeDir)
            .build()
        println "  gitDir: ${repo5.directory}"
        println "  workTree: ${repo5.workTree}"
        println "  branch: ${repo5.branch}"
        println "  HEAD: ${repo5.resolve(Constants.HEAD)}"
        repo5.close()

        // Read commondir to find object directory
        def commonDirFile = new File(gitDir, "commondir")
        def commonDirPath = commonDirFile.text.trim()
        def commonDir = new File(gitDir, commonDirPath).canonicalFile
        def objectDir = new File(commonDir, "objects")
        println "\n=== Testing JGit with explicit objectDirectory ==="
        println "  commonDir: ${commonDir}"
        println "  objectDir: ${objectDir}"
        def repo6 = new FileRepositoryBuilder()
            .setGitDir(gitDir)
            .setWorkTree(worktreeDir)
            .setObjectDirectory(objectDir)
            .build()
        println "  gitDir: ${repo6.directory}"
        println "  workTree: ${repo6.workTree}"
        println "  branch: ${repo6.branch}"
        println "  HEAD: ${repo6.resolve(Constants.HEAD)}"
        repo6.close()

        // Try opening the main repo but pointing at worktree's HEAD
        println "\n=== Testing: Open main repo with worktree's HEAD ==="
        def mainGitDir = commonDir
        def repo7 = new FileRepositoryBuilder()
            .setGitDir(mainGitDir)
            .setWorkTree(worktreeDir)
            .build()
        println "  gitDir: ${repo7.directory}"
        println "  workTree: ${repo7.workTree}"
        println "  branch: ${repo7.branch}"
        println "  HEAD: ${repo7.resolve(Constants.HEAD)}"
        // But this gives main repo's HEAD, not worktree's
        repo7.close()

        // Using Git.open() which is what Grgit uses internally
        println "\n=== Testing: Git.open(worktreeDir) - SKIPPED (RepositoryNotFoundException) ==="

        // Try FileRepositoryBuilder.create() which is similar to Git.open()
        println "\n=== Testing: FileRepositoryBuilder.create(gitDir) for main repo ==="
        def mainGitDir2 = new File(mainRepoDir, ".git")
        def repo9 = FileRepositoryBuilder.create(mainGitDir2)
        println "  gitDir: ${repo9.directory}"
        println "  workTree: ${repo9.workTree}"
        println "  branch: ${repo9.branch}"
        println "  HEAD: ${repo9.resolve(Constants.HEAD)}"
        repo9.close()
    }

    private static void runGit(File dir, String... args) {
        def cmd = ["git"] + args.toList()
        def proc = cmd.execute(null, dir)
        proc.waitFor()
        if (proc.exitValue() != 0) {
            throw new RuntimeException("git ${args.join(' ')} failed: ${proc.err.text}")
        }
    }
}
