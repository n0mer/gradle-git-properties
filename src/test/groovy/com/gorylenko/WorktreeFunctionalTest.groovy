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
        
        // Create a worktree directory structure
        def worktreeDir = temporaryFolder.newFolder("worktree")
        def worktreeGitFile = new File(worktreeDir, ".git")
        
        // Create .git file pointing to main repository (simulating worktree)
        def mainGitPath = new File(mainRepoDir, ".git").absolutePath
        def worktreeGitPath = mainGitPath + "/worktrees/test-worktree"
        new File(worktreeGitPath).mkdirs()
        worktreeGitFile.text = "gitdir: ${worktreeGitPath}"
        
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
        assertThat(runner.output, containsString("dotGitDirectory = [${mainGitPath}]"))
        
        def gitPropertiesFile = new File(worktreeDir, "build/resources/main/git.properties")
        assertTrue("git.properties file should exist", gitPropertiesFile.exists())
    }
}