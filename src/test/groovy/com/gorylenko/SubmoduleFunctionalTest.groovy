package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Tests for git submodule scenarios.
 *
 * Related issues: #264, #259, #168, #117, #175
 *
 * In a git submodule, the .git entry is a file (not a directory) containing:
 *   gitdir: ../.git/modules/submodule-name
 *
 * The actual git data is stored in the parent repository at:
 *   parent/.git/modules/submodule-name/
 */
class SubmoduleFunctionalTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    /**
     * Test that the plugin works when the project is a git submodule.
     * Submodules have a .git file pointing to parent's .git/modules/ directory.
     */
    @Test
    void testPluginWorksWithGitSubmodule() {
        def parentDir = temporaryFolder.newFolder("parent-repo")
        GitRepositoryBuilder.setupProjectDir(parentDir, { builder ->
            builder.commitFile("README.md", "Parent repo", "Initial commit in parent")
        })

        def submoduleDir = new File(parentDir, "my-submodule")
        setupSubmoduleGitDirectory(parentDir, submoduleDir, "my-submodule",
                "Initial submodule commit", false)

        setupGradleBuild(submoduleDir)

        def result = runGradle(submoduleDir)

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
        assertGitProperties(submoduleDir, "Initial submodule commit")
    }

    /**
     * Test that the plugin works in a nested project directory.
     * The nested project is NOT a submodule, just a subdirectory within a git repo.
     */
    @Test
    void testPluginWorksInNestedProjectDirectory() {
        def parentDir = temporaryFolder.newFolder("parent-with-nested")
        GitRepositoryBuilder.setupProjectDir(parentDir, { builder ->
            builder.commitFile("README.md", "Parent repo", "Parent commit message")
        })

        def nestedProjectDir = new File(parentDir, "nested-project")
        nestedProjectDir.mkdirs()

        setupGradleBuild(nestedProjectDir)

        def result = runGradle(nestedProjectDir)

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
        assertGitProperties(nestedProjectDir, "Parent commit message")
    }

    /**
     * Test with absolute path in .git file (some git versions use absolute paths).
     */
    @Test
    void testPluginWorksWithAbsoluteGitdirPath() {
        def parentDir = temporaryFolder.newFolder("parent-absolute")
        GitRepositoryBuilder.setupProjectDir(parentDir, { builder ->
            builder.commitFile("README.md", "Parent repo", "Initial commit")
        })

        def submoduleDir = new File(parentDir, "submodule-absolute")
        setupSubmoduleGitDirectory(parentDir, submoduleDir, "submodule-absolute",
                "Absolute path submodule commit", true)

        setupGradleBuild(submoduleDir)

        def result = runGradle(submoduleDir)

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
        assertGitProperties(submoduleDir, "Absolute path submodule commit")
    }

    /**
     * Sets up a submodule git directory structure.
     *
     * @param parentDir The parent repository directory
     * @param submoduleDir The submodule working directory
     * @param submoduleName The name of the submodule
     * @param commitMessage The commit message for the submodule's initial commit
     * @param useAbsolutePath Whether to use absolute path in .git file
     */
    private void setupSubmoduleGitDirectory(File parentDir, File submoduleDir,
                                            String submoduleName, String commitMessage,
                                            boolean useAbsolutePath) {
        submoduleDir.mkdirs()

        def parentGitDir = new File(parentDir, ".git")
        def modulesDir = new File(parentGitDir, "modules/${submoduleName}")

        // Initialize a temp git repo, then copy contents to modulesDir
        def tempDir = new File(parentDir, "temp-init-${submoduleName}")
        GitRepositoryBuilder.setupProjectDir(tempDir, { builder ->
            builder.commitFile("dummy.txt", "dummy", commitMessage)
        })

        // Copy .git contents to modulesDir (modulesDir IS the git directory for submodules)
        def tempGitDir = new File(tempDir, ".git")
        new AntBuilder().copy(todir: modulesDir.absolutePath) {
            fileset(dir: tempGitDir.absolutePath)
        }
        tempDir.deleteDir()

        // Create .git file pointing to the modules directory
        def submoduleGitFile = new File(submoduleDir, ".git")
        if (useAbsolutePath) {
            submoduleGitFile.text = "gitdir: ${modulesDir.absolutePath}\n"
        } else {
            submoduleGitFile.text = "gitdir: ../.git/modules/${submoduleName}\n"
        }

        // Configure the modules git to know about its worktree
        def modulesConfig = new File(modulesDir, "config")
        modulesConfig.append("\n[core]\n\tworktree = ${submoduleDir.absolutePath}\n")
    }

    private void setupGradleBuild(File projectDir) {
        new File(projectDir, "settings.gradle") << ""
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
                .withArguments('generateGitProperties')
                .build()
    }

    private void assertGitProperties(File projectDir, String expectedCommitMessage) {
        def gitPropertiesFile = new File(projectDir, "build/resources/main/git.properties")
        assertTrue("git.properties file should exist", gitPropertiesFile.exists())

        def properties = new Properties()
        gitPropertiesFile.withInputStream { properties.load(it) }

        def commitMessage = properties.getProperty("git.commit.message.short")
        assertEquals("Commit message should match expected git directory",
                expectedCommitMessage, commitMessage)
    }
}
