package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

/**
 * Tests for git FSMonitor compatibility.
 *
 * Related issues: #230, #270
 *
 * When Git's core.fsmonitor is enabled, it creates a socket file
 * .git/fsmonitor--daemon.ipc that Gradle cannot snapshot, causing
 * the generateGitProperties task to fail.
 */
class FSMonitorFunctionalTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    /**
     * Test that the plugin works when FSMonitor socket file exists.
     *
     * Issue #230: generateGitProperties fails when Git's core.fsmonitor is enabled
     */
    @Test
    void testPluginWorksWithFSMonitorSocketFile() {
        def projectDir = temporaryFolder.newFolder("fsmonitor-test")

        // Create git repo
        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Test commit")
        })

        // Create a socket file simulating FSMonitor daemon
        def socketFile = new File(projectDir, ".git/fsmonitor--daemon.ipc")
        createSocketFile(socketFile)

        assertTrue("Socket file should exist", socketFile.exists())

        // Setup Gradle build
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        // Run the plugin twice to test incremental build (where snapshot issues occur)
        def runner = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withGradleVersion('8.6')
            .withArguments('generateGitProperties', '--stacktrace')

        // First run
        def result1 = runner.build()
        assertEquals(TaskOutcome.SUCCESS, result1.task(":generateGitProperties").outcome)

        // Second run (up-to-date check - this is when snapshot happens)
        def result2 = runner.build()
        assertTrue("Second run should be SUCCESS or UP_TO_DATE",
            result2.task(":generateGitProperties").outcome in [TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE])

        // Verify git.properties was created
        def gitPropertiesFile = new File(projectDir, "build/resources/main/git.properties")
        assertTrue("git.properties should be created", gitPropertiesFile.exists())
    }

    /**
     * Test that the plugin works with other non-regular files in .git directory.
     */
    @Test
    void testPluginWorksWithOtherSocketFiles() {
        def projectDir = temporaryFolder.newFolder("socket-test")

        // Create git repo
        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Test commit")
        })

        // Create multiple socket files
        createSocketFile(new File(projectDir, ".git/some-daemon.sock"))
        createSocketFile(new File(projectDir, ".git/another.ipc"))

        // Setup Gradle build
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        // Run the plugin
        def result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments('generateGitProperties')
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)
    }

    /**
     * Creates a Unix domain socket file.
     * Falls back to creating a regular file on platforms that don't support sockets.
     */
    private void createSocketFile(File socketFile) {
        // Skip on Windows - no Unix domain sockets
        def os = System.getProperty("os.name").toLowerCase()
        if (os.contains("windows")) {
            org.junit.Assume.assumeTrue("Unix sockets not supported on Windows", false)
            return
        }

        try {
            // Create socket using Python
            def cmd = "import socket; s = socket.socket(socket.AF_UNIX); s.bind('${socketFile.absolutePath}')"
            def process = new ProcessBuilder("python3", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            process.waitFor()

            if (!socketFile.exists()) {
                // Fallback: try mkfifo (named pipe - also non-regular file)
                new ProcessBuilder("mkfifo", socketFile.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor()
            }

            if (!socketFile.exists()) {
                org.junit.Assume.assumeTrue("Cannot create socket file", false)
            }
        } catch (Exception e) {
            org.junit.Assume.assumeTrue("Cannot create socket file: " + e.message, false)
        }
    }
}
