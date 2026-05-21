package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.hamcrest.CoreMatchers.containsString
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

class WorktreeFunctionalTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    void testPluginWorksWithGitWorktrees() {
        // Setup main repository
        def mainRepoDir = temporaryFolder.newFolder("main-repo")
        GitRepositoryBuilder.setupProjectDir(mainRepoDir, { builder ->
            builder.commitFile("test.txt", "test content", "Initial commit")
        })

        // Create a REAL worktree using git command
        def worktreeDir = new File(temporaryFolder.root, "worktree")
        def result = runGitCommand(mainRepoDir, "worktree", "add", "-b", "worktree-branch", worktreeDir.absolutePath)
        if (result.exitCode != 0) {
            throw new RuntimeException("Failed to create worktree: ${result.stderr}")
        }

        // Verify worktree was created correctly
        def gitFile = new File(worktreeDir, ".git")
        assertTrue(".git should exist", gitFile.exists())
        assertTrue(".git should be a file in worktree", gitFile.isFile())

        // Setup build files in worktree
        new File(worktreeDir, "settings.gradle") << ""
        new File(worktreeDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(worktreeDir)
                .withArguments('generateGitProperties', '--info')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        // Verify the worktree's gitdir is used (e.g., .git/worktrees/worktree/)
        // Use canonicalPath because macOS /var is symlinked to /private/var
        def worktreeGitDir = new File(mainRepoDir, ".git/worktrees/worktree").canonicalPath
        assertTrue("Output should contain worktree gitdir path",
            runner.output.contains(worktreeGitDir))

        def gitPropertiesFile = new File(worktreeDir, "build/resources/main/git.properties")
        assertTrue("git.properties file should exist", gitPropertiesFile.exists())

        // Verify properties are correct for the worktree
        def props = new Properties()
        gitPropertiesFile.withInputStream { props.load(it) }
        assertEquals("Branch should be worktree's branch", "worktree-branch", props.getProperty("git.branch"))
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