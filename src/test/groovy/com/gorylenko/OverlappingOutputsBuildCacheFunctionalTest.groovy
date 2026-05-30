package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.util.zip.ZipFile

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

public class OverlappingOutputsBuildCacheFunctionalTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    public void testGitPropertiesAlwaysPresentInJarWithBuildCache() {
        def projectDir = temporaryFolder.newFolder("project")
        def cacheDir = temporaryFolder.newFolder("build-cache")

        // Set up git repo with one commit
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Initial commit")
        })

        // settings.gradle: point build cache to a local temp dir
        new File(projectDir, "settings.gradle") << """
            buildCache {
                local {
                    directory = '${cacheDir.absolutePath.replace('\\', '/')}'
                }
            }
        """.stripIndent()

        // build.gradle: java + git-properties plugin, default config only
        new File(projectDir, "build.gradle") << """
            plugins {
                id('java')
                id('com.gorylenko.gradle-git-properties')
            }
        """.stripIndent()

        // REQUIRED: existing resources dir — bug only triggers with resources present
        def resourcesDir = new File(projectDir, "src/main/resources")
        resourcesDir.mkdirs()
        new File(resourcesDir, "test.txt") << "some content"

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)

        // Run clean + assemble 6 times; git.properties must be in the JAR every time
        6.times { iteration ->
            int run = iteration + 1

            runner.withArguments("clean").build()

            def assembleResult = runner.withArguments("assemble", "--build-cache").build()
            assert assembleResult.task(":assemble").outcome in [TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE] :
                "Run ${run}: assemble outcome was ${assembleResult.task(":assemble").outcome}"

            // Find the built JAR
            def libsDir = new File(projectDir, "build/libs")
            def jarFiles = libsDir.listFiles({ File f -> f.name.endsWith(".jar") } as FileFilter)
            assertNotNull("Run ${run}: build/libs directory not found or empty", jarFiles)
            assert jarFiles.length > 0 : "Run ${run}: no JAR found in build/libs"

            def jar = jarFiles[0]
            def zipFile = new ZipFile(jar)
            try {
                def gitPropertiesEntry = zipFile.getEntry("git.properties")
                assertNotNull(
                    "Run ${run}: git.properties NOT found in JAR '${jar.name}' " +
                    "(entries: ${zipFile.entries().collect { it.name }.join(', ')})",
                    gitPropertiesEntry
                )
            } finally {
                zipFile.close()
            }
        }
    }
}
