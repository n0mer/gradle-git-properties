package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

public class ConfigurationCacheFunctionalTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    public void testPluginSupportsConfigurationCache() {
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
                .withArguments("--configuration-cache", "generateGitProperties")
                .withProjectDir(projectDir)

        def firstResult = runner.build()
        assertEquals(TaskOutcome.SUCCESS, firstResult.task(":generateGitProperties").outcome)

        def secondResult = runner.build()
        assertTrue(secondResult.output.contains('Reusing configuration cache.'))
        assertEquals(TaskOutcome.UP_TO_DATE, secondResult.task(":generateGitProperties").outcome)
    }
}
