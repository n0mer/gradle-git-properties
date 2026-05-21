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

/**
 * Tests for custom properties using GitFacade API methods in closures.
 *
 * This verifies the Grgit-compatible facade API documented in SPEC:
 * - it.head().id, it.head().abbreviatedId, it.head().author.name, etc.
 * - it.describe(), it.describe(tags: true), it.describe(longDescr: true)
 * - it.branch.current().name
 * - it.tag.list()
 * - it.status().clean
 * - it.log(maxCommits: n)
 * - it.jgit (escape hatch: raw Repository)
 * - it.jgitCommands (escape hatch: raw Git)
 */
class CustomPropertiesFacadeTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    /**
     * Test that custom properties can use it.head() API.
     */
    @Test
    void testCustomPropertyWithHeadApi() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Test commit message")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                customProperty('custom.commit.id') { it.head().id }
                customProperty('custom.commit.abbrev') { it.head().abbreviatedId }
                customProperty('custom.author.name') { it.head().author.name }
                customProperty('custom.author.email') { it.head().author.email }
                customProperty('custom.short.message') { it.head().shortMessage }
                customProperty('custom.full.message') { it.head().fullMessage }
                customProperty('custom.datetime') { it.head().dateTime.toString() }
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments('generateGitProperties', '--stacktrace')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        def props = new Properties()
        new File(projectDir, "build/resources/main/git.properties").withInputStream { props.load(it) }

        // Verify head() API works
        assertTrue("custom.commit.id should be 40 chars", props.getProperty("custom.commit.id").length() == 40)
        assertTrue("custom.commit.abbrev should be 7 chars", props.getProperty("custom.commit.abbrev").length() == 7)
        assertNotNull("author.name should not be null", props.getProperty("custom.author.name"))
        assertNotNull("author.email should not be null", props.getProperty("custom.author.email"))
        assertEquals("Test commit message", props.getProperty("custom.short.message"))
        assertNotNull("full.message should not be null", props.getProperty("custom.full.message"))
        assertNotNull("datetime should not be null", props.getProperty("custom.datetime"))
    }

    /**
     * Test that custom properties can use it.branch.current() API.
     */
    @Test
    void testCustomPropertyWithBranchApi() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Initial commit")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                customProperty('custom.branch') { it.branch.current().name }
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments('generateGitProperties')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        def props = new Properties()
        new File(projectDir, "build/resources/main/git.properties").withInputStream { props.load(it) }

        assertEquals("master", props.getProperty("custom.branch"))
    }

    /**
     * Test that custom properties can use it.describe() API.
     */
    @Test
    void testCustomPropertyWithDescribeApi() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Initial commit")
            builder.addTag("v1.0.0")
            builder.commitFile("test2.txt", "content2", "Second commit")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                customProperty('custom.describe') { it.describe() ?: '' }
                customProperty('custom.describe.long') { it.describe(longDescr: true) ?: '' }
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments('generateGitProperties')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        def props = new Properties()
        new File(projectDir, "build/resources/main/git.properties").withInputStream { props.load(it) }

        assertTrue("describe should contain tag", props.getProperty("custom.describe").startsWith("v1.0.0"))
        assertTrue("describe long should contain -1-g", props.getProperty("custom.describe.long").contains("-1-g"))
    }

    /**
     * Test that custom properties can use it.tag.list() API.
     */
    @Test
    void testCustomPropertyWithTagListApi() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Initial commit")
            builder.addTag("v1.0.0")
            builder.addTag("v1.0.1")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                customProperty('custom.tag.count') { it.tag.list().size().toString() }
                customProperty('custom.tag.names') { it.tag.list().collect { t -> t.name }.sort().join(',') }
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments('generateGitProperties')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        def props = new Properties()
        new File(projectDir, "build/resources/main/git.properties").withInputStream { props.load(it) }

        assertEquals("2", props.getProperty("custom.tag.count"))
        assertEquals("v1.0.0,v1.0.1", props.getProperty("custom.tag.names"))
    }

    /**
     * Test that custom properties can use it.status().clean API.
     */
    @Test
    void testCustomPropertyWithStatusApi() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Initial commit")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                customProperty('custom.clean') { it.status().clean.toString() }
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments('generateGitProperties')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        def props = new Properties()
        new File(projectDir, "build/resources/main/git.properties").withInputStream { props.load(it) }

        // Clean because build.gradle and settings.gradle are untracked but not in working tree
        // Actually, they ARE in working tree - so they make it dirty
        assertEquals("false", props.getProperty("custom.clean"))
    }

    /**
     * Test that custom properties can use it.log() API.
     */
    @Test
    void testCustomPropertyWithLogApi() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("file1.txt", "content1", "First commit")
            builder.commitFile("file2.txt", "content2", "Second commit")
            builder.commitFile("file3.txt", "content3", "Third commit")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                customProperty('custom.total.commits') { it.log().size().toString() }
                customProperty('custom.recent.commits') { it.log(maxCommits: 2).size().toString() }
                customProperty('custom.last.message') { it.log(maxCommits: 1)[0].shortMessage }
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments('generateGitProperties')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        def props = new Properties()
        new File(projectDir, "build/resources/main/git.properties").withInputStream { props.load(it) }

        assertEquals("3", props.getProperty("custom.total.commits"))
        assertEquals("2", props.getProperty("custom.recent.commits"))
        assertEquals("Third commit", props.getProperty("custom.last.message"))
    }

    /**
     * Test escape hatch - it.jgit returns raw Repository.
     */
    @Test
    void testCustomPropertyWithJgitEscapeHatch() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Initial commit")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                // Use raw JGit Repository to get branch
                customProperty('custom.jgit.branch') { it.jgit.branch }
                // Verify it's the right type
                customProperty('custom.jgit.type') { it.jgit.class.simpleName }
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments('generateGitProperties')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        def props = new Properties()
        new File(projectDir, "build/resources/main/git.properties").withInputStream { props.load(it) }

        assertEquals("master", props.getProperty("custom.jgit.branch"))
        // JGit Repository implementation class name
        assertTrue("Should be a Repository type",
                props.getProperty("custom.jgit.type").contains("Repository"))
    }

    /**
     * Test escape hatch - it.jgitCommands returns raw Git.
     */
    @Test
    void testCustomPropertyWithJgitCommandsEscapeHatch() {
        def projectDir = temporaryFolder.newFolder()

        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Initial commit")
        })

        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
            gitProperties {
                // Use raw JGit Git commands
                customProperty('custom.jgitcmds.status.clean') {
                    it.jgitCommands.status().call().isClean().toString()
                }
                customProperty('custom.jgitcmds.type') { it.jgitCommands.class.simpleName }
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments('generateGitProperties')
                .forwardOutput()
                .build()

        assertEquals(TaskOutcome.SUCCESS, runner.task(":generateGitProperties").outcome)

        def props = new Properties()
        new File(projectDir, "build/resources/main/git.properties").withInputStream { props.load(it) }

        // Not clean because build.gradle and settings.gradle are untracked
        // JGit status().isClean() doesn't include untracked files by default
        // but our GitStatus wrapper does - so this tests raw JGit behavior
        assertEquals("false", props.getProperty("custom.jgitcmds.status.clean"))
        assertEquals("Git", props.getProperty("custom.jgitcmds.type"))
    }
}
