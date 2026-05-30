package com.gorylenko

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

/**
 * Functional test verifying that worktree builds detect changes
 * to shared refs (tags, branches) in the main repository.
 *
 * This tests the fix for issue #292 (issue #1): Worktree up-to-date check
 * misses shared refs changes.
 */
class WorktreeUpToDateFunctionalTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    void testWorktreeDetectsTagAddedInMainRepo() {
        // 1. Create main repo with initial commit
        def mainRepoDir = temporaryFolder.newFolder("main-repo")
        initGitRepo(mainRepoDir)
        commitFile(mainRepoDir, "test.txt", "content", "Initial commit")

        // 2. Create worktree
        def worktreeDir = new File(temporaryFolder.root, "worktree")
        createWorktree(mainRepoDir, worktreeDir, "feature-branch")

        // 3. Setup gradle build in worktree
        setupGradleBuild(worktreeDir)

        // 4. First build - should be SUCCESS
        def result1 = runGradle(worktreeDir, "generateGitProperties")
        assertEquals("First build should succeed", TaskOutcome.SUCCESS, result1.task(":generateGitProperties").outcome)

        // 5. Second build - should be UP-TO-DATE
        def result2 = runGradle(worktreeDir, "generateGitProperties")
        assertEquals("Second build should be UP-TO-DATE", TaskOutcome.UP_TO_DATE, result2.task(":generateGitProperties").outcome)

        // 6. Add tag in main repo (this changes shared refs)
        addTag(mainRepoDir, "v1.0.0")

        // 7. Build in worktree - should be SUCCESS (not UP-TO-DATE)
        // This verifies the fix: task detects the new tag in shared refs
        def result3 = runGradle(worktreeDir, "generateGitProperties")
        assertEquals("Build after tag should be SUCCESS (detect shared ref change)",
            TaskOutcome.SUCCESS, result3.task(":generateGitProperties").outcome)

        // 8. Verify git.properties was regenerated (file exists and was updated)
        def gitProperties = new File(worktreeDir, "build/generated/resources/git/git.properties")
        assertTrue("git.properties should exist", gitProperties.exists())
        // The core fix is verified: task detected the shared ref change and rebuilt.
        // The closest tag property may or may not show v1.0.0 depending on commit distance.
    }

    @Test
    void testNormalRepoStillWorksCorrectly() {
        // Regression test: ensure non-worktree repos still work
        def repoDir = temporaryFolder.newFolder("normal-repo")
        initGitRepo(repoDir)
        commitFile(repoDir, "test.txt", "content", "Initial commit")
        setupGradleBuild(repoDir)

        // First build
        def result1 = runGradle(repoDir, "generateGitProperties")
        assertEquals("First build should succeed", TaskOutcome.SUCCESS, result1.task(":generateGitProperties").outcome)

        // Second build - UP-TO-DATE
        def result2 = runGradle(repoDir, "generateGitProperties")
        assertEquals("Second build should be UP-TO-DATE", TaskOutcome.UP_TO_DATE, result2.task(":generateGitProperties").outcome)

        // Add tag
        addTag(repoDir, "v1.0.0")

        // Third build - should detect tag
        def result3 = runGradle(repoDir, "generateGitProperties")
        assertEquals("Build after tag should be SUCCESS", TaskOutcome.SUCCESS, result3.task(":generateGitProperties").outcome)
    }

    private void initGitRepo(File dir) {
        exec(dir, "git", "init")
        exec(dir, "git", "config", "user.email", "test@test.com")
        exec(dir, "git", "config", "user.name", "Test")
    }

    private void commitFile(File dir, String filename, String content, String message) {
        new File(dir, filename).text = content
        exec(dir, "git", "add", filename)
        exec(dir, "git", "commit", "-m", message)
    }

    private void createWorktree(File mainRepo, File worktreeDir, String branchName) {
        exec(mainRepo, "git", "worktree", "add", worktreeDir.absolutePath, "-b", branchName)
    }

    private void addTag(File dir, String tagName) {
        exec(dir, "git", "tag", tagName)
    }

    private void setupGradleBuild(File projectDir) {
        new File(projectDir, "settings.gradle").text = "rootProject.name = 'test-project'"
        new File(projectDir, "build.gradle").text = """
            plugins {
                id 'java'
                id 'com.gorylenko.gradle-git-properties'
            }
        """
    }

    private def runGradle(File projectDir, String... args) {
        return GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(args)
            .withPluginClasspath()
            .build()
    }

    private void exec(File workDir, String... command) {
        def process = new ProcessBuilder(command)
            .directory(workDir)
            .redirectErrorStream(true)
            .start()
        def output = process.text
        process.waitFor()
        if (process.exitValue() != 0) {
            throw new RuntimeException("Command failed: ${command.join(' ')}\n${output}")
        }
    }
}
