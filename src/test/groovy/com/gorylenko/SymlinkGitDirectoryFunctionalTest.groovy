package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Files

import static org.hamcrest.CoreMatchers.containsString
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

class SymlinkGitDirectoryFunctionalTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    void testPluginWorksWithSymlinkedGitDirectory() {
        // Skip on Windows where symlinks may require elevated privileges
        Assume.assumeTrue("Skipping symlink test on Windows", !System.getProperty("os.name").toLowerCase().contains("windows"))

        // Setup main repository
        def mainRepoDir = temporaryFolder.newFolder("main-repo")
        GitRepositoryBuilder.setupProjectDir(mainRepoDir, { builder ->
            builder.commitFile("test.txt", "test content", "Initial commit")
        })

        // Create a project directory with .git as a symlink (simulating Gerrit repo structure)
        def projectDir = temporaryFolder.newFolder("project-with-symlink")
        def mainGitDir = new File(mainRepoDir, ".git")
        def symlinkGitDir = new File(projectDir, ".git")

        // Create symlink: project/.git -> main-repo/.git
        Files.createSymbolicLink(symlinkGitDir.toPath(), mainGitDir.toPath())

        // Verify symlink was created
        assertTrue("Symlink should exist", symlinkGitDir.exists())
        assertTrue("Should be a symlink", Files.isSymbolicLink(symlinkGitDir.toPath()))

        // Setup build files
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments('generateGitProperties', '--info')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)
        // Verify dotGitDirectory was detected (path may vary due to macOS /var -> /private/var symlink)
        assertThat(runner.output, containsString("dotGitDirectory = ["))

        def gitPropertiesFile = new File(projectDir, "build/resources/main/git.properties")
        assertTrue("git.properties file should exist", gitPropertiesFile.exists())

        // Verify properties were generated correctly
        def properties = new Properties()
        gitPropertiesFile.withInputStream { properties.load(it) }
        assertTrue("Should have git.commit.id", properties.containsKey("git.commit.id"))
        assertTrue("Should have git.branch", properties.containsKey("git.branch"))
    }

    @Test
    void testPluginWorksWithRelativeSymlinkedGitDirectory() {
        // Skip on Windows where symlinks may require elevated privileges
        Assume.assumeTrue("Skipping symlink test on Windows", !System.getProperty("os.name").toLowerCase().contains("windows"))

        // Setup: Create structure like Gerrit repo
        // base/
        //   actual-repo/       (full git repo with working tree)
        //     .git/
        //   myproject/
        //     .git -> ../actual-repo/.git

        def baseDir = temporaryFolder.newFolder("base")

        // Create the actual git repository with working tree
        def actualRepoDir = new File(baseDir, "actual-repo")
        actualRepoDir.mkdirs()
        GitRepositoryBuilder.setupProjectDir(actualRepoDir, { builder ->
            builder.commitFile("test.txt", "test content", "Initial commit")
        })

        def actualGitDir = new File(actualRepoDir, ".git")

        // Create project directory
        def projectDir = new File(baseDir, "myproject")
        projectDir.mkdirs()

        // Create relative symlink: myproject/.git -> ../actual-repo/.git
        def symlinkGitDir = new File(projectDir, ".git")
        def relativePath = projectDir.toPath().relativize(actualGitDir.toPath())
        Files.createSymbolicLink(symlinkGitDir.toPath(), relativePath)

        // Setup build files
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments('generateGitProperties', '--info')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        def gitPropertiesFile = new File(projectDir, "build/resources/main/git.properties")
        assertTrue("git.properties file should exist", gitPropertiesFile.exists())

        // Verify properties were generated correctly
        def properties = new Properties()
        gitPropertiesFile.withInputStream { properties.load(it) }
        assertTrue("Should have git.commit.id", properties.containsKey("git.commit.id"))
    }
}
