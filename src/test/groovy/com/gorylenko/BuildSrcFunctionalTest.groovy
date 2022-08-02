package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import groovy.json.JsonOutput
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

public class BuildSrcFunctionalTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    public void testPluginSucceedsDuringConfigurationPhase() {
        def projectDir = temporaryFolder.newFolder()
        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
        runner.withArguments('check', "-Dcom.gorylenko.gradle-git-properties.unit-test-classpath=${JsonOutput.toJson(runner.pluginClasspath*.path)}")

        def buildSrcDir = new File(projectDir, "buildSrc")
        buildSrcDir.mkdirs()
        new File(buildSrcDir, "build.gradle") << """\
            import groovy.json.JsonSlurper
            plugins {
                id("groovy-gradle-plugin")
            }
            repositories {
                gradlePluginPortal()
            }
            dependencies {
                runtimeClasspath files(new JsonSlurper().parseText(System.getProperty('com.gorylenko.gradle-git-properties.unit-test-classpath')))
            }
        """
        def pluginSourceDir = buildSrcDir.toPath().resolve("src").resolve("main").resolve("groovy").toFile()
        pluginSourceDir.mkdirs()
        new File(pluginSourceDir, "test-plugin.gradle") << """\
            plugins {
                id("java")
                id("com.gorylenko.gradle-git-properties")
            }
        """
        new File(projectDir, "settings.gradle") << """\
            pluginManagement {
                plugins {
                    id("com.gorylenko.gradle-git-properties")
                }
            }
        """.stripIndent()
        new File(projectDir, "build.gradle") << """\
            plugins {
                id("test-plugin")
            }
        """.stripIndent()

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
    }

    @Test
    public void testPluginCanOverrideGitPropertiesResourceDir() {
        def projectDir = temporaryFolder.newFolder()
        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("check")
                .withProjectDir(projectDir)
        runner.withArguments('check', "-Dcom.gorylenko.gradle-git-properties.unit-test-classpath=${JsonOutput.toJson(runner.pluginClasspath*.path)}")

        def buildSrcDir = new File(projectDir, "buildSrc")
        buildSrcDir.mkdirs()
        def gitPropDir = new File(projectDir, "gitProp")
        gitPropDir.mkdirs()
        new File(buildSrcDir, "build.gradle") << """\
            import groovy.json.JsonSlurper
            plugins {
                id("groovy-gradle-plugin")
            }
            repositories {
                gradlePluginPortal()
            }
            dependencies {
                runtimeClasspath files(new JsonSlurper().parseText(System.getProperty('com.gorylenko.gradle-git-properties.unit-test-classpath')))
            }
        """
        def pluginSourceDir = buildSrcDir.toPath().resolve("src").resolve("main").resolve("groovy").toFile()
        pluginSourceDir.mkdirs()
        new File(pluginSourceDir, "test-plugin.gradle") << """\
            plugins {
                id("java")
                id("com.gorylenko.gradle-git-properties")
            }
        """
        new File(projectDir, "settings.gradle") << """\
            pluginManagement {
                plugins {
                    id("com.gorylenko.gradle-git-properties")
                }
            }
        """.stripIndent()
        new File(projectDir, "build.gradle") << """\
            plugins {
                id("test-plugin")
            }
            
            gitProperties {
                gitPropertiesResourceDir = layout.projectDirectory.dir("gitProp")
            }
        """.stripIndent()

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
        assertTrue(gitPropDir.list().contains("git.properties"))
    }
}
