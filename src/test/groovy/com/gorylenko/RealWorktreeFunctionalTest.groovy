package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

/**
 * Tests for REAL git worktrees created via 'git worktree add'.
 *
 * These tests expose the actual bug reported in GitHub issue #288:
 * The plugin resolves to the main .git directory, but then reads
 * the wrong branch (main repo's branch instead of worktree's branch).
 *
 * The existing WorktreeFunctionalTest only simulates worktree structure
 * and doesn't verify branch name correctness.
 */
class RealWorktreeFunctionalTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    /**
     * Test that git.branch shows the worktree's branch, not the main repo's branch.
     *
     * This is the core bug from issue #288.
     */
    @Test
    void testBranchNameIsCorrectInWorktree() {
        // Setup main repository on master branch
        def mainRepoDir = temporaryFolder.newFolder("main-repo")
        GitRepositoryBuilder.setupProjectDir(mainRepoDir, { builder ->
            builder.commitFile("test.txt", "test content", "Initial commit")
        })

        // Create a real worktree on a different branch
        def worktreeDir = new File(temporaryFolder.root, "feature-worktree")
        def result = runGitCommand(mainRepoDir, "worktree", "add", "-b", "feature-branch", worktreeDir.absolutePath)
        if (result.exitCode != 0) {
            fail("Failed to create worktree: ${result.stderr}")
        }

        // Verify worktree was created correctly
        def gitFile = new File(worktreeDir, ".git")
        assertTrue(".git should be a file in worktree", gitFile.isFile())
        println "Worktree .git file content: ${gitFile.text}"

        // Setup build files in worktree
        new File(worktreeDir, "settings.gradle") << ""
        new File(worktreeDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        def runner = createCleanRunner(worktreeDir)
                .withArguments('generateGitProperties', '--info', '--stacktrace')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        // Load and verify git.properties
        def gitPropertiesFile = new File(worktreeDir, "build/resources/main/git.properties")
        assertTrue("git.properties file should exist", gitPropertiesFile.exists())

        def props = new Properties()
        gitPropertiesFile.withInputStream { props.load(it) }

        // THIS IS THE ACTUAL BUG: branch shows "master" instead of "feature-branch"
        def actualBranch = props.getProperty("git.branch")
        assertEquals("git.branch should be the worktree's branch, not main repo's branch",
                "feature-branch", actualBranch)
    }

    /**
     * Test that commit ID is correct for the worktree's HEAD.
     */
    @Test
    void testCommitIdIsCorrectInWorktree() {
        // Setup main repository
        def mainRepoDir = temporaryFolder.newFolder("main-repo")
        GitRepositoryBuilder.setupProjectDir(mainRepoDir, { builder ->
            builder.commitFile("test.txt", "test content", "Initial commit")
        })

        // Create worktree and make a new commit there
        def worktreeDir = new File(temporaryFolder.root, "feature-worktree")
        runGitCommand(mainRepoDir, "worktree", "add", "-b", "feature-branch", worktreeDir.absolutePath)

        // Make a new commit in the worktree (configure identity for CI environments)
        runGitCommand(worktreeDir, "config", "user.email", "test@example.com")
        runGitCommand(worktreeDir, "config", "user.name", "Test User")
        new File(worktreeDir, "worktree-file.txt").text = "worktree content"
        runGitCommand(worktreeDir, "add", "worktree-file.txt")
        runGitCommand(worktreeDir, "commit", "-m", "Worktree commit")

        // Get expected commit ID from worktree
        def expectedCommit = runGitCommand(worktreeDir, "rev-parse", "HEAD").stdout.trim()
        def mainCommit = runGitCommand(mainRepoDir, "rev-parse", "HEAD").stdout.trim()

        println "Main repo HEAD: ${mainCommit}"
        println "Worktree HEAD: ${expectedCommit}"
        assertTrue("Commits should be different", expectedCommit != mainCommit)

        // Setup build files
        new File(worktreeDir, "settings.gradle") << ""
        new File(worktreeDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        def runner = createCleanRunner(worktreeDir)
                .withArguments('generateGitProperties', '--stacktrace')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        def props = new Properties()
        new File(worktreeDir, "build/resources/main/git.properties").withInputStream { props.load(it) }

        def actualCommit = props.getProperty("git.commit.id")
        assertEquals("git.commit.id should be the worktree's commit",
                expectedCommit, actualCommit)
    }

    /**
     * Test worktree created from a bare repository.
     */
    @Test
    void testWorktreeFromBareRepo() {
        // Create a bare repository
        def bareRepoDir = temporaryFolder.newFolder("bare-repo.git")
        runGitCommand(bareRepoDir, "init", "--bare")

        // Create a regular repo, add content, push to bare
        def regularRepoDir = temporaryFolder.newFolder("regular-repo")
        GitRepositoryBuilder.setupProjectDir(regularRepoDir, { builder ->
            builder.commitFile("test.txt", "test content", "Initial commit")
        })
        runGitCommand(regularRepoDir, "remote", "add", "origin", bareRepoDir.absolutePath)
        runGitCommand(regularRepoDir, "push", "-u", "origin", "master")

        // Create worktree from bare repo
        def worktreeDir = new File(temporaryFolder.root, "bare-worktree")
        def result = runGitCommand(bareRepoDir, "worktree", "add", worktreeDir.absolutePath, "master")
        if (result.exitCode != 0) {
            println "Failed to create worktree from bare: ${result.stderr}"
            // Some git versions may not support this well, skip gracefully
            return
        }

        // Setup build files
        new File(worktreeDir, "settings.gradle") << ""
        new File(worktreeDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        def runner = createCleanRunner(worktreeDir)
                .withArguments('generateGitProperties', '--stacktrace')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        def gitPropertiesFile = new File(worktreeDir, "build/resources/main/git.properties")
        assertTrue("git.properties should exist for bare repo worktree", gitPropertiesFile.exists())

        def props = new Properties()
        gitPropertiesFile.withInputStream { props.load(it) }
        assertTrue("Should have commit id", props.containsKey("git.commit.id"))
    }

    /**
     * Test worktree with detached HEAD state.
     */
    @Test
    void testWorktreeWithDetachedHead() {
        // Setup main repository with multiple commits
        def mainRepoDir = temporaryFolder.newFolder("main-repo")
        GitRepositoryBuilder.setupProjectDir(mainRepoDir, { builder ->
            builder.commitFile("test.txt", "test content", "First commit")
            builder.commitFile("test2.txt", "more content", "Second commit")
        })

        // Get first commit SHA
        def firstCommit = runGitCommand(mainRepoDir, "rev-parse", "HEAD~1").stdout.trim()

        // Create worktree at specific commit (detached HEAD)
        def worktreeDir = new File(temporaryFolder.root, "detached-worktree")
        def result = runGitCommand(mainRepoDir, "worktree", "add", "--detach", worktreeDir.absolutePath, firstCommit)
        if (result.exitCode != 0) {
            fail("Failed to create detached worktree: ${result.stderr}")
        }

        // Setup build files
        new File(worktreeDir, "settings.gradle") << ""
        new File(worktreeDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        def runner = createCleanRunner(worktreeDir)
                .withArguments('generateGitProperties', '--stacktrace')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        def props = new Properties()
        new File(worktreeDir, "build/resources/main/git.properties").withInputStream { props.load(it) }

        // In detached HEAD, branch might be empty or contain commit SHA
        def branch = props.getProperty("git.branch")
        println "Detached HEAD branch value: ${branch}"

        // Commit should match the first commit we checked out
        def actualCommit = props.getProperty("git.commit.id")
        assertEquals("Commit should match detached HEAD", firstCommit, actualCommit)
    }

    /**
     * Creates a GradleRunner with CI branch environment variables cleared.
     * This ensures tests use git to detect the branch, not CI environment variables
     * (which would override the worktree's branch with the CI's current branch).
     */
    private static GradleRunner createCleanRunner(File projectDir) {
        def cleanEnv = System.getenv().findAll { k, v ->
            !['GITHUB_ACTIONS', 'GITHUB_HEAD_REF', 'GITHUB_REF_NAME',
              'TRAVIS', 'TRAVIS_BRANCH',
              'GITLAB_CI', 'CI_COMMIT_REF_NAME',
              'CIRCLECI', 'CIRCLE_BRANCH',
              'TF_BUILD', 'BUILD_SOURCEBRANCH',
              'BITBUCKET_BUILD_NUMBER', 'BITBUCKET_BRANCH',
              'JOB_NAME', 'GIT_LOCAL_BRANCH', 'GIT_BRANCH', 'BRANCH_NAME',
              'TEAMCITY_VERSION',
              'BAMBOO_BUILDKEY', 'BAMBOO_PLANREPOSITORY_BRANCH',
              'CODEBUILD_BUILD_ARN', 'CODEBUILD_WEBHOOK_HEAD_REF', 'CODEBUILD_WEBHOOK_TRIGGER', 'CODEBUILD_SOURCE_VERSION'
            ].contains(k)
        }
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withEnvironment(cleanEnv)
    }

    // Helper to run git commands
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
