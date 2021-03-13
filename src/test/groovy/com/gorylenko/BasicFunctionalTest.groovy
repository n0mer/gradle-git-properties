package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

public class BasicFunctionalTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    public void testPluginFailsWithoutGitDirectory() {
        def projectDir = temporaryFolder.newFolder()
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("generateGitProperties")
                .withProjectDir(projectDir)

        def result = runner.buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":generateGitProperties").outcome)
        assertTrue(result.output.contains("No Git repository found."))
    }

    @Test
    public void testPluginSucceedsWithoutGitDirectoryAndFailOnNoGitDirectoryFalse() {
        def projectDir = temporaryFolder.newFolder()
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                failOnNoGitDirectory = false
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("generateGitProperties")
                .withProjectDir(projectDir)

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
    }

    @Test
    public void testPluginSucceedsWithGitDirectory() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("generateGitProperties")
                .withProjectDir(projectDir)

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
    }
}
