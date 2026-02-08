package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertNotNull

@RunWith(Parameterized.class)
public class BackwardCompatibilityFunctionalTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Parameterized.Parameters(name = "Gradle {0}")
    static List<Object[]> data() {
        return [
                // Gradle 9.x - requires Java 17+, supports up to Java 23
                ["9.0", ["generateGitProperties", "--configuration-cache", "--build-cache"], 17, 23],
                // Gradle 8.x - requires Java 8+, supports up to Java 21/23
                ["8.14.4", ["generateGitProperties", "--configuration-cache", "--build-cache"], 8, 23],
                ["8.5", ["generateGitProperties", "--configuration-cache", "--build-cache"], 8, 21],
                ["8.0", ["generateGitProperties", "--configuration-cache", "--build-cache"], 8, 19],
                // Gradle 7.x - requires Java 8+, supports up to Java 16-19
                ["7.6.4", ["generateGitProperties", "--configuration-cache", "--build-cache"], 8, 19],
                ["7.0", ["generateGitProperties", "--configuration-cache", "--build-cache"], 8, 16],
                // Gradle 6.x - requires Java 8+, supports up to Java 14-15
                ["6.8.3", ["generateGitProperties", "--configuration-cache", "--build-cache"], 8, 15],
                ["6.7.1", ["generateGitProperties", "--configuration-cache", "--build-cache"], 8, 15],
                ["6.6.1", ["generateGitProperties", "--configuration-cache", "--build-cache"], 8, 14],
                ["6.5.1", ["generateGitProperties"], 8, 14],
                ["6.4.1", ["generateGitProperties"], 8, 14],
                // ["6.3", ["generateGitProperties"], 8, 14], // StackOverflowError: https://github.com/gradle/gradle/issues/11466
                // ["6.2.2", ["generateGitProperties"], 8, 13], // StackOverflowError: https://github.com/gradle/gradle/issues/11466
                // ["6.1.1", ["generateGitProperties"], 8, 13], // StackOverflowError: https://github.com/gradle/gradle/issues/11466
                // ["6.0.1", ["generateGitProperties"], 8, 13], // StackOverflowError: https://github.com/gradle/gradle/issues/11466
                // Gradle 5.x - requires Java 8+, supports up to Java 11-12
                ["5.6.4", ["generateGitProperties"], 8, 12],
                ["5.5.1", ["generateGitProperties"], 8, 12],
                ["5.1", ["generateGitProperties"], 8, 11],
                // ["5.0", ["generateGitProperties"], 8, 11], // Doesn't support conventions: https://docs.gradle.org/5.1/release-notes.html#specify-a-convention-for-a-property
        ]*.toArray()
    }

    private final String gradleVersion;
    private final List<String> arguments;
    private final int minJavaVersion;
    private final int maxJavaVersion;

    BackwardCompatibilityFunctionalTest(String gradleVersion, List<String> arguments, int minJavaVersion, int maxJavaVersion) {
        this.gradleVersion = gradleVersion
        this.arguments = arguments
        this.minJavaVersion = minJavaVersion
        this.maxJavaVersion = maxJavaVersion
    }

    private static int getMajorJavaVersion() {
        String version = System.getProperty("java.version", "99")
        // Handle versions like "1.8.0_XXX" (Java 8) and "11.0.X", "17.0.X", "21.0.X" (Java 9+)
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.split("\\.")[1])
        }
        return Integer.parseInt(version.split("\\.")[0])
    }

    @Test
    public void testPluginSupportsConfigurationCache() {
        int javaVersion = getMajorJavaVersion()
        Assume.assumeTrue("Skipping Gradle ${gradleVersion} on Java ${javaVersion} (requires Java ${minJavaVersion}+)",
                javaVersion >= minJavaVersion)
        Assume.assumeTrue("Skipping Gradle ${gradleVersion} on Java ${javaVersion} (max supported: ${maxJavaVersion})",
                javaVersion <= maxJavaVersion)

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
                .withGradleVersion(gradleVersion)
                .withPluginClasspath()
                .withArguments(arguments)
                .withProjectDir(projectDir)

        def result = runner.build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)

        // Verify git.properties file was created with expected content
        def gitPropertiesFile = new File(projectDir, "build/resources/main/git.properties")
        assertTrue("git.properties should exist", gitPropertiesFile.exists())

        def properties = new Properties()
        gitPropertiesFile.withInputStream { properties.load(it) }

        // Verify essential properties are present and non-empty
        assertNotNull("Should have git.branch", properties.getProperty("git.branch"))
        assertNotNull("Should have git.commit.id", properties.getProperty("git.commit.id"))
        assertNotNull("Should have git.commit.id.abbrev", properties.getProperty("git.commit.id.abbrev"))
        assertNotNull("Should have git.commit.time", properties.getProperty("git.commit.time"))
        assertNotNull("Should have git.commit.message.short", properties.getProperty("git.commit.message.short"))
        assertEquals("Added hello.txt", properties.getProperty("git.commit.message.short"))

        // Verify up-to-date check works
        result = runner.build()
        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":generateGitProperties").outcome)
    }
}
