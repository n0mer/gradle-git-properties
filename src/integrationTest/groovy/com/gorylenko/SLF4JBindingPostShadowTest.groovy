package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SLF4JBindingPostShadowTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    void testSFL4JBindingPostShadow() {
        def projectDir = temporaryFolder.newFolder()

        def integrationPluginPath = System.properties.get("integration.plugin.path") // Fetch integration jar from shadowJar output
        Assert.assertNotNull("integration.plugin.path was null", integrationPluginPath)

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
                .withPluginClasspath(Arrays.asList(new File(integrationPluginPath as String))) // Use integration plugin jar
                .withArguments("generateGitProperties")
                .withProjectDir(projectDir)

        def result = runner.build()

        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
        Assert.assertFalse("Project improperly shadowed slf4j-api!", result.output.contains("org.slf4j.impl.StaticLoggerBinder"))
    }

}
