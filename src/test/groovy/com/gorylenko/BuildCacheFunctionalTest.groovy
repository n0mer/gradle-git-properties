package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals

public class BuildCacheFunctionalTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    public void testPluginSupportsBuildCache() {
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
                .withArguments("generateGitProperties", "--build-cache")
                .withProjectDir(projectDir)

        def firstResult = runner.build()
        assertEquals(TaskOutcome.SUCCESS, firstResult.task(":generateGitProperties").outcome)

        def secondResult = runner.build()
        assertEquals(TaskOutcome.UP_TO_DATE, secondResult.task(":generateGitProperties").outcome)

        runner.withArguments("clean").build()

        def thirdResult = runner.withArguments("generateGitProperties", "--build-cache").build()
        assertEquals(TaskOutcome.FROM_CACHE, thirdResult.task(":generateGitProperties").outcome)
    }
}
