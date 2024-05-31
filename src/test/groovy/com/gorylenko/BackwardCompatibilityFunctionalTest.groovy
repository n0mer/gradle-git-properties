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

@RunWith(Parameterized.class)
public class BackwardCompatibilityFunctionalTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Parameterized.Parameters(name = "Gradle {0}")
    static List<Object[]> data() {
        return [
                ["8.7", ["generateGitProperties", "--configuration-cache", "--build-cache"], "17"],
                ["7.0", ["generateGitProperties", "--configuration-cache", "--build-cache"], "17"],
                ["6.8.3", ["generateGitProperties", "--configuration-cache", "--build-cache"], "15"],
                ["6.7.1", ["generateGitProperties", "--configuration-cache", "--build-cache"], "15"],
                ["6.6.1", ["generateGitProperties", "--configuration-cache", "--build-cache"], "15"],
                ["6.5.1", ["generateGitProperties"], "15"],
                ["6.4.1", ["generateGitProperties"], "15"],
                // ["6.3", ["generateGitProperties"], "15"], // StackOverflowError: https://github.com/gradle/gradle/issues/11466
                // ["6.2.2", ["generateGitProperties"], "15"], // StackOverflowError: https://github.com/gradle/gradle/issues/11466
                // ["6.1.1", ["generateGitProperties"], "15"], // StackOverflowError: https://github.com/gradle/gradle/issues/11466
                // ["6.0.1", ["generateGitProperties"], "15"], // StackOverflowError: https://github.com/gradle/gradle/issues/11466
                ["5.6.4", ["generateGitProperties"], "12"],
                ["5.5.1", ["generateGitProperties"], "12"],
                ["5.1", ["generateGitProperties"], "12"],
                // ["5.0", ["generateGitProperties"], "12"], // Doesn't support conventions: https://docs.gradle.org/5.1/release-notes.html#specify-a-convention-for-a-property
        ]*.toArray()
    }

    private final String gradleVersion;
    private final List<String> arguments;
    private final String maxJavaVersion;

    BackwardCompatibilityFunctionalTest(String gradleVersion, List<String> arguments, String maxJavaVersion) {
        this.gradleVersion = gradleVersion
        this.arguments = arguments
        this.maxJavaVersion = maxJavaVersion
    }

    @Test
    public void testPluginSupportsConfigurationCache() {
        def javaVersion = System.getProperty("java.version", "99");
        Assume.assumeTrue("Skipping test on Java ${javaVersion}", javaVersion <= maxJavaVersion);

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

        result = runner.build()
        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":generateGitProperties").outcome)
    }
}
