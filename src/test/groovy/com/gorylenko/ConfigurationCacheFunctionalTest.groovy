package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertNotNull

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

    /**
     * Test configuration cache compatibility when extProperty is configured.
     * This addresses issue #268 and relates to issues #225, #241.
     *
     * When extProperty is set, the plugin exposes git properties to project.ext.
     * Note: The extProperty is primarily useful during configuration phase.
     * Accessing project.ext at execution time is not supported with configuration cache.
     */
    @Test
    public void testPluginSupportsConfigurationCacheWithExtProperty() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('java')
                id('com.gorylenko.gradle-git-properties')
            }

            gitProperties {
                extProperty = 'gitProps'
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("--configuration-cache", "generateGitProperties")
                .withProjectDir(projectDir)

        // First run - should succeed and store configuration cache
        def firstResult = runner.build()
        assertEquals(TaskOutcome.SUCCESS, firstResult.task(":generateGitProperties").outcome)

        // Verify git.properties was created
        def gitPropertiesFile = new File(projectDir, "build/resources/main/git.properties")
        assertTrue("git.properties should exist", gitPropertiesFile.exists())
        def properties = new Properties()
        gitPropertiesFile.withInputStream { properties.load(it) }
        assertNotNull("Should have git.branch", properties.getProperty("git.branch"))
        assertNotNull("Should have git.commit.id", properties.getProperty("git.commit.id"))

        // Second run - should reuse configuration cache
        // Note: generateGitProperties runs every time when extProperty is set (upToDateWhen returns false)
        def secondResult = runner.build()
        assertTrue("Should reuse configuration cache", secondResult.output.contains('Reusing configuration cache.'))
        // Task runs again because upToDateWhen returns false when extProperty is set
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":generateGitProperties").outcome)
    }

    /**
     * Test configuration cache with custom properties using closures.
     * Closures can be problematic with configuration cache if they capture project state.
     */
    @Test
    public void testPluginSupportsConfigurationCacheWithCustomProperties() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('java')
                id('com.gorylenko.gradle-git-properties')
            }

            gitProperties {
                customProperty 'custom.build.number', '12345'
                customProperty 'custom.project.name', 'test-project'
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("--configuration-cache", "generateGitProperties")
                .withProjectDir(projectDir)

        // First run
        def firstResult = runner.build()
        assertEquals(TaskOutcome.SUCCESS, firstResult.task(":generateGitProperties").outcome)

        // Verify custom properties in output
        def gitPropertiesFile = new File(projectDir, "build/resources/main/git.properties")
        assertTrue("git.properties should exist", gitPropertiesFile.exists())
        def properties = new Properties()
        gitPropertiesFile.withInputStream { properties.load(it) }
        assertEquals("12345", properties.getProperty("custom.build.number"))
        assertEquals("test-project", properties.getProperty("custom.project.name"))

        // Second run - should reuse configuration cache
        def secondResult = runner.build()
        assertTrue("Should reuse configuration cache", secondResult.output.contains('Reusing configuration cache.'))
        assertEquals(TaskOutcome.UP_TO_DATE, secondResult.task(":generateGitProperties").outcome)
    }
}
