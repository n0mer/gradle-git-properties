package com.gorylenko

import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.api.Git
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import com.gorylenko.properties.GitRepositoryBuilder
import com.gorylenko.jgit.GitFacade

import static org.junit.Assert.*

/**
 * Direct test of JGit worktree capabilities.
 * This helps verify GitFacade correctly handles git worktrees.
 */
class JGitWorktreeDirectTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    void testJGitWithWorktreeGitDir() {
        // Setup main repository on master
        def mainRepoDir = temporaryFolder.newFolder("main-repo")
        GitRepositoryBuilder.setupProjectDir(mainRepoDir, { builder ->
            builder.commitFile("test.txt", "test content", "Initial commit")
        })

        // Create worktree with different branch
        def worktreeDir = new File(temporaryFolder.root, "feature-worktree")
        def result = runGitCommand(mainRepoDir, "worktree", "add", "-b", "feature-branch", worktreeDir.absolutePath)
        assertEquals("Worktree creation should succeed", 0, result.exitCode)

        // Read .git file to get worktree git directory
        def gitFile = new File(worktreeDir, ".git")
        def gitDirPath = gitFile.text.trim().substring("gitdir: ".length())
        def worktreeGitDir = new File(gitDirPath)
        def mainGitDir = new File(mainRepoDir, ".git")

        println "Main .git dir: ${mainGitDir.absolutePath}"
        println "Worktree git dir: ${worktreeGitDir.absolutePath}"
        println "Worktree working dir: ${worktreeDir.absolutePath}"

        // Test 1: FileRepositoryBuilder with setGitDir pointing to worktree git dir
        println "\n=== Test 1: FileRepositoryBuilder.setGitDir(worktreeGitDir) ==="
        try {
            def builder1 = new FileRepositoryBuilder()
                .setGitDir(worktreeGitDir)
                .readEnvironment()
            def repo1 = builder1.build()
            println "  Branch: ${repo1.branch}"
            println "  Directory: ${repo1.directory}"
            println "  isBare: ${repo1.bare}"
            assertEquals("Branch should be worktree branch", "feature-branch", repo1.branch)
            repo1.close()
        } catch (Exception e) {
            println "  EXCEPTION: ${e.class.simpleName}: ${e.message}"
            fail("Test 1 failed: ${e.message}")
        }

        // Test 2: FileRepositoryBuilder with setGitDir + setWorkTree
        println "\n=== Test 2: FileRepositoryBuilder.setGitDir(worktreeGitDir).setWorkTree(worktreeDir) ==="
        try {
            def builder2 = new FileRepositoryBuilder()
                .setGitDir(worktreeGitDir)
                .setWorkTree(worktreeDir)
                .readEnvironment()
            def repo2 = builder2.build()
            println "  Branch: ${repo2.branch}"
            println "  Directory: ${repo2.directory}"
            println "  WorkTree: ${repo2.workTree}"
            println "  isBare: ${repo2.bare}"
            assertEquals("Branch should be worktree branch", "feature-branch", repo2.branch)
            assertEquals("WorkTree should be worktree dir", worktreeDir.absolutePath, repo2.workTree.absolutePath)
            repo2.close()
        } catch (Exception e) {
            println "  EXCEPTION: ${e.class.simpleName}: ${e.message}"
            fail("Test 2 failed: ${e.message}")
        }

        // Test 3: FileRepositoryBuilder.findGitDir starting from worktree working dir
        println "\n=== Test 3: FileRepositoryBuilder.findGitDir(worktreeDir) ==="
        try {
            def builder3 = new FileRepositoryBuilder()
                .findGitDir(worktreeDir)
                .readEnvironment()
            println "  GitDir found: ${builder3.gitDir}"
            def repo3 = builder3.build()
            println "  Branch: ${repo3.branch}"
            println "  Directory: ${repo3.directory}"
            println "  isBare: ${repo3.bare}"
            // Does JGit correctly find and use the worktree git dir?
            assertEquals("Branch should be worktree branch", "feature-branch", repo3.branch)
            repo3.close()
        } catch (Exception e) {
            println "  EXCEPTION: ${e.class.simpleName}: ${e.message}"
            // This might fail in JGit 5.x - that's what we want to learn
        }

        // Test 4: Git.open on main .git dir (current plugin behavior - THE BUG)
        println "\n=== Test 4: Git.open(mainGitDir) - CURRENT PLUGIN BEHAVIOR ==="
        try {
            def git4 = Git.open(mainGitDir)
            println "  Branch: ${git4.repository.branch}"
            println "  Directory: ${git4.repository.directory}"
            println "  WorkTree: ${git4.repository.workTree}"
            // This is the BUG - it shows master instead of feature-branch
            assertEquals("Branch will be MAIN branch (demonstrating the bug)", "master", git4.repository.branch)
            git4.close()
        } catch (Exception e) {
            println "  EXCEPTION: ${e.class.simpleName}: ${e.message}"
        }

        // Test 5: GitFacade.open with worktree directory (THE FIX)
        println "\n=== Test 5: GitFacade.open(worktreeDir) - THE FIX ==="
        def facade = GitFacade.open(worktreeDir)
        try {
            println "  Branch via GitFacade: ${facade.branch.current().name}"
            assertEquals("Branch should be worktree branch", "feature-branch", facade.branch.current().name)
        } finally {
            facade.close()
        }
    }

    private static GitCommandResult runGitCommand(File workDir, String... args) {
        def command = ["git"] + args.toList()
        def process = command.execute(null, workDir)
        def stdout = new StringBuilder()
        def stderr = new StringBuilder()
        process.consumeProcessOutput(stdout, stderr)
        process.waitFor()
        return new GitCommandResult(
                exitCode: process.exitValue(),
                stdout: stdout.toString(),
                stderr: stderr.toString()
        )
    }

    private static class GitCommandResult {
        int exitCode
        String stdout
        String stderr
    }
}
