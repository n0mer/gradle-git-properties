package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.util.zip.ZipFile

import static org.hamcrest.CoreMatchers.containsString
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertThat
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

    // Issue #234: Configurable commit ID abbreviation length

    @Test
    public void testCustomCommitIdAbbrevLength() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                commitIdAbbrevLength = 10
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("generateGitProperties")
                .withProjectDir(projectDir)

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)

        // Verify the abbreviated commit ID length
        def propsFile = new File(projectDir, "build/generated/resources/git/git.properties")
        def props = new Properties()
        propsFile.withInputStream { props.load(it) }
        def abbrevId = props.getProperty("git.commit.id.abbrev")
        assertEquals("Should return 10 chars", 10, abbrevId.length())
        assert abbrevId.matches('[a-f0-9]+')
    }

    @Test
    public void testInvalidCommitIdAbbrevLengthFailsFast() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                commitIdAbbrevLength = 1
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("generateGitProperties")
                .withProjectDir(projectDir)

        def result = runner.buildAndFail()
        assertThat(result.output, containsString("commitIdAbbrevLength must be between 2 and 40"))
    }

    /**
     * Verify that when gitPropertiesResourceDir is set to a custom directory,
     * git.properties ends up packaged inside the JAR via sourceSets wiring.
     */
    @Test
    public void testGitPropertiesResourceDirInJar() {
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
                .withArguments("assemble")
                .withProjectDir(projectDir)

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":processResources").outcome)

        // Verify git.properties was written to the custom directory on disk
        assert new File(customDir, "git.properties").exists()

        // Verify git.properties was copied into build/resources/main and packaged in the JAR
        def libsDir = new File(projectDir, "build/libs")
        def jarFiles = libsDir.listFiles({ File f -> f.name.endsWith(".jar") } as FileFilter)
        assertNotNull("build/libs directory not found or empty", jarFiles)
        assert jarFiles.length > 0 : "no JAR found in build/libs"

        def zipFile = new ZipFile(jarFiles[0])
        try {
            def entry = zipFile.getEntry("git.properties")
            assertNotNull(
                "git.properties NOT found in JAR '${jarFiles[0].name}' " +
                "(entries: ${zipFile.entries().collect { it.name }.join(', ')})",
                entry
            )
        } finally {
            zipFile.close()
        }
    }

    /**
     * Verify that when the deprecated gitPropertiesDir is set:
     * - git.properties is written to the custom path
     * - a deprecation warning is emitted
     * - git.properties is NOT packaged in the JAR (no sourceSets wiring for deprecated property)
     */
    @Test
    public void testDeprecatedGitPropertiesDir() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        def customDir = new File(projectDir, "deprecated-git-props")
        customDir.mkdirs()

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('java')
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                gitPropertiesDir = file('deprecated-git-props')
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("assemble")
                .withProjectDir(projectDir)

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)

        // Verify git.properties was written to the custom directory on disk
        assert new File(customDir, "git.properties").exists()

        // Verify the deprecation warning was emitted
        assertThat(result.output, containsString("'gitPropertiesDir' is deprecated"))

        // Verify git.properties is NOT packaged in the JAR (deprecated property has no sourceSets wiring)
        def libsDir = new File(projectDir, "build/libs")
        def jarFiles = libsDir.listFiles({ File f -> f.name.endsWith(".jar") } as FileFilter)
        assertNotNull("build/libs directory not found or empty", jarFiles)
        assert jarFiles.length > 0 : "no JAR found in build/libs"

        def zipFile = new ZipFile(jarFiles[0])
        try {
            def entry = zipFile.getEntry("git.properties")
            assertNull(
                "git.properties SHOULD NOT be in JAR when gitPropertiesDir (deprecated) is used, " +
                "but was found in '${jarFiles[0].name}'",
                entry
            )
        } finally {
            zipFile.close()
        }
    }

    /**
     * Issue #304: extProperty broken when task reference (dependsOn generateGitProperties)
     * appears BEFORE the gitProperties { extProperty = 'gitProps' } block.
     *
     * This ordering forces the task to be realized (constructor runs) during the
     * dependsOn resolution, BEFORE extProperty is set on the extension.
     * As a result, the extProperty check in the constructor fires with extProperty == null,
     * and project.ext['gitProps'] is never pre-registered — causing the error:
     * "Cannot get property 'gitProps' on extra properties extension as it does not exist"
     *
     * RED test — should FAIL demonstrating the bug.
     */
    @Test
    public void testExtPropertyWorksWhenTaskReferenceBeforeGitPropertiesBlock() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        new File(projectDir, "settings.gradle") << ""
        // Reproduces user's exact failing order: task definition (with dependsOn) BEFORE gitProperties block
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }

            task printGitProperties {
                dependsOn generateGitProperties
                def ext = project.ext
                doLast {
                    println "Branch: " + ext.gitProps["git.branch"]
                }
            }

            gitProperties {
                extProperty = 'gitProps'
            }

            generateGitProperties.finalizedBy printGitProperties
            generateGitProperties.outputs.upToDateWhen { false }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("generateGitProperties")
                .withProjectDir(projectDir)

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
        assertThat(result.output, containsString("Branch:"))
    }

    /**
     * Issue #304: Control test — extProperty works correctly when gitProperties block
     * appears BEFORE the task reference (the "safe" order).
     *
     * GREEN test — should PASS.
     */
    @Test
    public void testExtPropertyWorksWhenGitPropertiesBlockBeforeTaskReference() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        new File(projectDir, "settings.gradle") << ""
        // Safe order: gitProperties block FIRST, then task reference
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }

            gitProperties {
                extProperty = 'gitProps'
            }

            task printGitProperties {
                dependsOn generateGitProperties
                def ext = project.ext
                doLast {
                    println "Branch: " + ext.gitProps["git.branch"]
                }
            }

            generateGitProperties.finalizedBy printGitProperties
            generateGitProperties.outputs.upToDateWhen { false }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("generateGitProperties")
                .withProjectDir(projectDir)

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
        assertThat(result.output, containsString("Branch:"))
    }
}
