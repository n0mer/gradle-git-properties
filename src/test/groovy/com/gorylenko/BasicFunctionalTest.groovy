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
        assertThat(result.output, containsString("No Git repository found."))
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

    @Test
    public void testPluginSucceedsWithJavaBasePlugin() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('java-base')
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

    @Test
    public void testPluginSucceedsWithJavaPlugin() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('java')
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("classes")
                .withProjectDir(projectDir)

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
    }

    /**
     * Test for issue #209: processResources should have explicit dependency on generateGitProperties
     * when gitPropertiesResourceDir is set to a custom directory.
     *
     * Without the fix, Gradle 8.x strict validation would fail with:
     * "Task ':processResources' uses this output of task ':generateGitProperties'
     *  without declaring an explicit or implicit dependency."
     */
    @Test
    public void testProcessResourcesDependsOnGenerateGitPropertiesWithCustomResourceDir() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        def customDir = new File(projectDir, "custom-git-props")
        customDir.mkdirs()

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('java')
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                gitPropertiesResourceDir = file('custom-git-props')
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("processResources")
                .withProjectDir(projectDir)

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":processResources").outcome)
        // Verify git.properties was created in custom directory
        assert new File(customDir, "git.properties").exists()
    }
}
