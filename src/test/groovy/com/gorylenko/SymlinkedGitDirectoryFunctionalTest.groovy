package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Files

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Functional tests for symlinked .git directories.
 *
 * Related issues: #287
 *
 * Some tools (Gerrit, repo tool) use symlinked .git directories where
 * the .git directory is a symbolic link to another location:
 *   - Absolute: .git -> /path/to/real/.git
 *   - Relative: .git -> ../other-location/.git
 *
 * This behavior was originally fixed in PR #287 for Grgit and is now
 * inherently supported by the JGit migration (#290).
 */
class SymlinkedGitDirectoryFunctionalTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    /**
     * Test that the plugin works when .git is an absolute symlink to another directory.
     * Example: project/.git -> /some/absolute/path/.git
     */
    @Test
    void testPluginWorksWithAbsoluteSymlinkedGitDirectory() {
        assumeNotWindows()

        // 1. Create git-storage directory with actual git repo
        def gitStorageDir = temporaryFolder.newFolder("git-storage")
        GitRepositoryBuilder.setupProjectDir(gitStorageDir, { builder ->
            builder.commitFile("README.md", "Test content", "Initial commit for symlink test")
        })

        // 2. Create working-project directory (no git)
        def workingProjectDir = temporaryFolder.newFolder("working-project")

        // 3. Create absolute symlink: working-project/.git -> git-storage/.git
        def storageGitDir = new File(gitStorageDir, ".git")
        def projectGitSymlink = new File(workingProjectDir, ".git")
        Files.createSymbolicLink(projectGitSymlink.toPath(), storageGitDir.toPath())

        // 4. Setup gradle build
        setupGradleBuild(workingProjectDir)

        // 5. Run generateGitProperties task
        def result = runGradle(workingProjectDir)

        // 6. Verify
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
        assertGitPropertiesValid(workingProjectDir, "Initial commit for symlink test")
    }

    /**
     * Test that the plugin works when .git is a relative symlink to another directory.
     * This mimics how Gerrit/repo tool sets up repositories.
     * Example: project/.git -> ../git-storage/.git
     */
    @Test
    void testPluginWorksWithRelativeSymlinkedGitDirectory() {
        assumeNotWindows()

        // 1. Create parent directory to contain both dirs at same level
        def parentDir = temporaryFolder.newFolder("parent")

        // 2. Create git-storage directory with actual git repo
        def gitStorageDir = new File(parentDir, "git-storage")
        gitStorageDir.mkdirs()
        GitRepositoryBuilder.setupProjectDir(gitStorageDir, { builder ->
            builder.commitFile("file.txt", "Hello", "Relative symlink test commit")
        })

        // 3. Create working-project directory
        def workingProjectDir = new File(parentDir, "working-project")
        workingProjectDir.mkdirs()

        // 4. Create relative symlink: working-project/.git -> ../git-storage/.git
        def projectGitSymlink = new File(workingProjectDir, ".git")
        def relativePath = new File("../git-storage/.git").toPath()
        Files.createSymbolicLink(projectGitSymlink.toPath(), relativePath)

        // Verify symlink was created correctly
        assertTrue("Symlink should exist", projectGitSymlink.exists())
        assertTrue("Symlink target should resolve", new File(workingProjectDir, "../git-storage/.git").exists())

        // 5. Setup gradle build
        setupGradleBuild(workingProjectDir)

        // 6. Run generateGitProperties task
        def result = runGradle(workingProjectDir)

        // 7. Verify
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
        assertGitPropertiesValid(workingProjectDir, "Relative symlink test commit")
    }

    /**
     * Test that branch information is correctly read through symlinked .git directory.
     */
    @Test
    void testSymlinkedGitDirectoryCorrectlyReadsBranch() {
        assumeNotWindows()

        // 1. Create git-storage with a specific branch
        def gitStorageDir = temporaryFolder.newFolder("git-storage-branch")
        GitRepositoryBuilder.setupProjectDir(gitStorageDir, { builder ->
            builder.commitFile("main.txt", "main content", "Commit on main")
            builder.addBranchAndCheckout("feature-branch")
            builder.commitFile("feature.txt", "feature content", "Commit on feature branch")
        })

        // 2. Create working-project with symlink
        def workingProjectDir = temporaryFolder.newFolder("working-project-branch")
        def storageGitDir = new File(gitStorageDir, ".git")
        def projectGitSymlink = new File(workingProjectDir, ".git")
        Files.createSymbolicLink(projectGitSymlink.toPath(), storageGitDir.toPath())

        // 3. Setup and run
        setupGradleBuild(workingProjectDir)
        def result = runGradle(workingProjectDir)

        // 4. Verify branch is correctly read
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)

        def propsFile = new File(workingProjectDir, "build/resources/main/git.properties")
        assertTrue("git.properties should exist", propsFile.exists())

        def props = new Properties()
        propsFile.withInputStream { props.load(it) }
        assertEquals("Should be on feature-branch", "feature-branch", props.getProperty("git.branch"))
    }

    /**
     * Skip tests on Windows where symlinks may require elevated privileges.
     */
    private void assumeNotWindows() {
        Assume.assumeFalse("Skipping on Windows - symlinks require elevated privileges",
                System.getProperty("os.name").toLowerCase().contains("windows"))
    }

    private void setupGradleBuild(File projectDir) {
        new File(projectDir, "settings.gradle") << "rootProject.name = 'symlink-test'"
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()
    }

    private def runGradle(File projectDir) {
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments('generateGitProperties', '--info')
                .build()
    }

    private void assertGitPropertiesValid(File projectDir, String expectedCommitMessage) {
        def gitPropertiesFile = new File(projectDir, "build/resources/main/git.properties")
        assertTrue("git.properties file should exist", gitPropertiesFile.exists())

        def properties = new Properties()
        gitPropertiesFile.withInputStream { properties.load(it) }

        // Verify commit ID is present and valid
        def commitId = properties.getProperty("git.commit.id")
        assertTrue("git.commit.id should be present", commitId != null && !commitId.isEmpty())
        assertTrue("git.commit.id should be a valid SHA", commitId.matches('[a-f0-9]{40}'))

        // Verify commit message
        def commitMessage = properties.getProperty("git.commit.message.short")
        assertEquals("Commit message should match", expectedCommitMessage, commitMessage)

        // Verify branch is present
        def branch = properties.getProperty("git.branch")
        assertTrue("git.branch should be present", branch != null && !branch.isEmpty())
    }
}
