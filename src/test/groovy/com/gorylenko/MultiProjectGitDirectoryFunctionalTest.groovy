package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

/**
 * Functional tests for multi-project builds to verify .git directory resolution.
 * This test addresses issue #240: https://github.com/n0mer/gradle-git-properties/issues/240
 */
public class MultiProjectGitDirectoryFunctionalTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    public void testMultiProjectBuildWithoutExplicitDotGitDirectory() {
        def rootProjectDir = temporaryFolder.newFolder()
        def subProjectDir = new File(rootProjectDir, "subproject")
        subProjectDir.mkdirs()

        // Setup root project
        new File(rootProjectDir, "settings.gradle") << """\
            rootProject.name = 'multi-project-root'
            include 'subproject'
        """.stripIndent()

        new File(rootProjectDir, "build.gradle") << """\
            // Root project build file
        """.stripIndent()

        // Setup subproject with gradle-git-properties plugin
        new File(subProjectDir, "build.gradle") << """\
            plugins {
                id 'java'
                id 'com.gorylenko.gradle-git-properties'
            }
            
            gitProperties {
                // No explicit dotGitDirectory configuration - should fail in 2.5.0
                customProperty 'application.name', 'test-subproject'
            }
        """.stripIndent()

        // Create a simple Java source file in subproject
        def srcDir = new File(subProjectDir, "src/main/java/com/example")
        srcDir.mkdirs()
        new File(srcDir, "Application.java") << """\
            package com.example;
            
            public class Application {
                public static void main(String[] args) {
                    System.out.println("Hello from subproject!");
                }
            }
        """.stripIndent()

        // Setup git repository in ROOT directory (not subproject)
        GitRepositoryBuilder.setupProjectDir(rootProjectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("README.md", "# Multi-project test", "Initial commit")
        })

        // Verify .git exists in root but not in subproject
        assertTrue("Git directory should exist in root project", new File(rootProjectDir, ".git").exists())
        assertFalse("Git directory should NOT exist in subproject", new File(subProjectDir, ".git").exists())

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments(":subproject:generateGitProperties", "--info", "--stacktrace")
                .withProjectDir(rootProjectDir)

        // With the fix, this should now succeed (resolves issue #240)
        def result = runner.build()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":subproject:generateGitProperties").outcome)
        
        // Verify that the git.properties file was created
        def gitPropertiesFile = new File(subProjectDir, "build/resources/main/git.properties")
        assertTrue("git.properties should exist", gitPropertiesFile.exists())
        
        // Verify that the custom properties are included
        def properties = new Properties()
        gitPropertiesFile.withInputStream { properties.load(it) }
        assertEquals("test-subproject", properties.getProperty("application.name"))
        
        // Verify that git properties are populated (should have commit info from root git repo)
        assertTrue("Should have git.commit.id", properties.getProperty("git.commit.id") != null)
        assertTrue("Should have git.branch", properties.getProperty("git.branch") != null)
    }

    @Test
    public void testMultiProjectBuildWithExplicitRootProjectDotGitDirectory() {
        def rootProjectDir = temporaryFolder.newFolder()
        def subProjectDir = new File(rootProjectDir, "subproject")
        subProjectDir.mkdirs()

        // Setup root project
        new File(rootProjectDir, "settings.gradle") << """\
            rootProject.name = 'multi-project-root'
            include 'subproject'
        """.stripIndent()

        new File(rootProjectDir, "build.gradle") << """\
            // Root project build file
        """.stripIndent()

        // Setup subproject with explicit dotGitDirectory configuration
        new File(subProjectDir, "build.gradle") << """\
            plugins {
                id 'java'
                id 'com.gorylenko.gradle-git-properties'
            }
            
            gitProperties {
                // Explicit configuration pointing to root project's .git directory
                dotGitDirectory = project.rootProject.layout.projectDirectory.dir(".git")
                customProperty 'application.name', 'test-subproject'
            }
        """.stripIndent()

        // Create a simple Java source file in subproject
        def srcDir = new File(subProjectDir, "src/main/java/com/example")
        srcDir.mkdirs()
        new File(srcDir, "Application.java") << """\
            package com.example;
            
            public class Application {
                public static void main(String[] args) {
                    System.out.println("Hello from subproject!");
                }
            }
        """.stripIndent()

        // Setup git repository in ROOT directory
        GitRepositoryBuilder.setupProjectDir(rootProjectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("README.md", "# Multi-project test", "Initial commit")
        })

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments(":subproject:generateGitProperties", "--info")
                .withProjectDir(rootProjectDir)

        // This should succeed with explicit configuration
        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":subproject:generateGitProperties").outcome)
        
        // Verify that the git.properties file was created
        def gitPropertiesFile = new File(subProjectDir, "build/resources/main/git.properties")
        assertTrue("git.properties should exist", gitPropertiesFile.exists())
        
        // Verify that the custom properties are included
        def properties = new Properties()
        gitPropertiesFile.withInputStream { properties.load(it) }
        assertEquals("test-subproject", properties.getProperty("application.name"))
        
        // Verify that git properties are populated (should have commit info from root git repo)
        assertTrue("Should have git.commit.id", properties.getProperty("git.commit.id") != null)
        assertTrue("Should have git.branch", properties.getProperty("git.branch") != null)
    }

    @Test
    public void testMultiProjectBuildWithConfigurationCacheCompatibleSolution() {
        def rootProjectDir = temporaryFolder.newFolder()
        def subProjectDir = new File(rootProjectDir, "subproject")
        subProjectDir.mkdirs()

        // Setup root project
        new File(rootProjectDir, "settings.gradle") << """\
            rootProject.name = 'multi-project-root'
            include 'subproject'
        """.stripIndent()

        new File(rootProjectDir, "build.gradle") << """\
            // Root project build file
        """.stripIndent()

        // Setup subproject with configuration cache compatible solution
        // Using rootProject.layout.projectDirectory as fallback for older Gradle versions
        new File(subProjectDir, "build.gradle") << """\
            plugins {
                id 'java'
                id 'com.gorylenko.gradle-git-properties'
            }
            
            gitProperties {
                // Configuration cache compatible solution (fallback for Gradle < 8.13)
                dotGitDirectory = project.rootProject.layout.projectDirectory.dir(".git")
                customProperty 'application.name', 'test-subproject-config-cache'
            }
        """.stripIndent()

        // Create a simple Java source file in subproject
        def srcDir = new File(subProjectDir, "src/main/java/com/example")
        srcDir.mkdirs()
        new File(srcDir, "Application.java") << """\
            package com.example;
            
            public class Application {
                public static void main(String[] args) {
                    System.out.println("Hello from subproject!");
                }
            }
        """.stripIndent()

        // Setup git repository in ROOT directory
        GitRepositoryBuilder.setupProjectDir(rootProjectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("README.md", "# Multi-project test", "Initial commit")
        })

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments(":subproject:generateGitProperties", "--info")
                .withProjectDir(rootProjectDir)

        // This should succeed with configuration cache compatible solution
        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":subproject:generateGitProperties").outcome)
        
        // Verify that the git.properties file was created
        def gitPropertiesFile = new File(subProjectDir, "build/resources/main/git.properties")
        assertTrue("git.properties should exist", gitPropertiesFile.exists())
        
        // Verify that the custom properties are included
        def properties = new Properties()
        gitPropertiesFile.withInputStream { properties.load(it) }
        assertEquals("test-subproject-config-cache", properties.getProperty("application.name"))
        
        // Verify that git properties are populated
        assertTrue("Should have git.commit.id", properties.getProperty("git.commit.id") != null)
        assertTrue("Should have git.branch", properties.getProperty("git.branch") != null)
    }

    /**
     * Test for monorepo scenario where .git is ABOVE the Gradle root project.
     * Directory structure:
     * monorepo/
     * ├── .git/                    <-- Git repo root
     * └── projects/
     *     └── gradle-project/      <-- Gradle root project (settings.gradle here)
     *         └── build.gradle
     */
    @Test
    public void testMonorepoWithGitAboveGradleRoot() {
        // Create monorepo structure: monorepo/.git and monorepo/projects/gradle-project
        def monorepoDir = temporaryFolder.newFolder("monorepo")
        def projectsDir = new File(monorepoDir, "projects")
        def gradleProjectDir = new File(projectsDir, "gradle-project")
        gradleProjectDir.mkdirs()

        // Setup git repository at monorepo root (ABOVE Gradle project)
        GitRepositoryBuilder.setupProjectDir(monorepoDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("README.md", "# Monorepo", "Initial commit")
        })

        // Verify .git is at monorepo level, NOT in gradle project
        assertTrue("Git should exist at monorepo root", new File(monorepoDir, ".git").exists())
        assertFalse("Git should NOT exist in gradle project", new File(gradleProjectDir, ".git").exists())

        // Setup Gradle project
        new File(gradleProjectDir, "settings.gradle") << """\
            rootProject.name = 'gradle-in-monorepo'
        """.stripIndent()

        new File(gradleProjectDir, "build.gradle") << """\
            plugins {
                id 'java'
                id 'com.gorylenko.gradle-git-properties'
            }

            gitProperties {
                // No explicit dotGitDirectory - should auto-detect from monorepo
                customProperty 'test.scenario', 'monorepo'
            }
        """.stripIndent()

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments("generateGitProperties", "--info", "--stacktrace")
                .withProjectDir(gradleProjectDir)

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateGitProperties").outcome)

        // Verify git.properties was created
        def gitPropertiesFile = new File(gradleProjectDir, "build/resources/main/git.properties")
        assertTrue("git.properties should exist", gitPropertiesFile.exists())

        def properties = new Properties()
        gitPropertiesFile.withInputStream { properties.load(it) }
        assertEquals("monorepo", properties.getProperty("test.scenario"))
        assertTrue("Should have git.commit.id", properties.getProperty("git.commit.id") != null)
    }

    @Test
    public void testNestedMultiProjectBuildWithDotGitInRoot() {
        def rootProjectDir = temporaryFolder.newFolder()
        def level1Dir = new File(rootProjectDir, "level1")
        def level2Dir = new File(level1Dir, "level2")
        level1Dir.mkdirs()
        level2Dir.mkdirs()

        // Setup root project with nested subprojects
        new File(rootProjectDir, "settings.gradle") << """\
            rootProject.name = 'nested-multi-project-root'
            include 'level1'
            include 'level1:level2'
        """.stripIndent()

        new File(rootProjectDir, "build.gradle") << """\
            // Root project build file
        """.stripIndent()

        // Setup level1 subproject
        new File(level1Dir, "build.gradle") << """\
            // Level 1 subproject
        """.stripIndent()

        // Setup deeply nested subproject with gradle-git-properties plugin
        new File(level2Dir, "build.gradle") << """\
            plugins {
                id 'java'
                id 'com.gorylenko.gradle-git-properties'
            }
            
            gitProperties {
                // Should work with explicit root project configuration
                dotGitDirectory = project.rootProject.layout.projectDirectory.dir(".git")
                customProperty 'application.name', 'nested-subproject'
                customProperty 'project.path', project.path
            }
        """.stripIndent()

        // Create a simple Java source file in deeply nested subproject
        def srcDir = new File(level2Dir, "src/main/java/com/example")
        srcDir.mkdirs()
        new File(srcDir, "NestedApplication.java") << """\
            package com.example;
            
            public class NestedApplication {
                public static void main(String[] args) {
                    System.out.println("Hello from nested subproject!");
                }
            }
        """.stripIndent()

        // Setup git repository in ROOT directory
        GitRepositoryBuilder.setupProjectDir(rootProjectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("README.md", "# Nested multi-project test", "Initial commit")
            gitRepoBuilder.commitFile("level1/README.md", "# Level 1", "Add level1 readme")
            gitRepoBuilder.commitFile("level1/level2/README.md", "# Level 2", "Add level2 readme")
        })

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments(":level1:level2:generateGitProperties", "--info")
                .withProjectDir(rootProjectDir)

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":level1:level2:generateGitProperties").outcome)
        
        // Verify that the git.properties file was created
        def gitPropertiesFile = new File(level2Dir, "build/resources/main/git.properties")
        assertTrue("git.properties should exist", gitPropertiesFile.exists())
        
        // Verify that the custom properties are included
        def properties = new Properties()
        gitPropertiesFile.withInputStream { properties.load(it) }
        assertEquals("nested-subproject", properties.getProperty("application.name"))
        assertEquals(":level1:level2", properties.getProperty("project.path"))
        
        // Verify that git properties are populated
        assertTrue("Should have git.commit.id", properties.getProperty("git.commit.id") != null)
        assertTrue("Should have git.branch", properties.getProperty("git.branch") != null)
    }

    /**
     * Test for composite build scenario where an included build needs to find .git
     * from the composing build's root.
     * Directory structure:
     * composite-root/
     * ├── .git/                    <-- Git repo root
     * ├── settings.gradle          <-- includeBuild 'included-build'
     * └── included-build/
     *     ├── settings.gradle      <-- Separate Gradle build
     *     └── build.gradle         <-- Plugin applied here
     *
     * This addresses the composite build scenario from issue #240:
     * https://github.com/n0mer/gradle-git-properties/issues/240#issuecomment-2710689037
     */
    @Test
    public void testCompositeBuildWithGitInComposingRoot() {
        // Create composite build structure
        def compositeRootDir = temporaryFolder.newFolder("composite-root")
        def includedBuildDir = new File(compositeRootDir, "included-build")
        includedBuildDir.mkdirs()

        // Setup git repository at composite root
        GitRepositoryBuilder.setupProjectDir(compositeRootDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("README.md", "# Composite build", "Initial commit")
        })

        // Verify .git is at composite root, NOT in included build
        assertTrue("Git should exist at composite root", new File(compositeRootDir, ".git").exists())
        assertFalse("Git should NOT exist in included build", new File(includedBuildDir, ".git").exists())

        // Setup composing build (root)
        new File(compositeRootDir, "settings.gradle") << """\
            rootProject.name = 'composite-root'
            includeBuild 'included-build'
        """.stripIndent()

        new File(compositeRootDir, "build.gradle") << """\
            // Composing build
        """.stripIndent()

        // Setup included build (separate Gradle build)
        new File(includedBuildDir, "settings.gradle") << """\
            rootProject.name = 'included-build'
        """.stripIndent()

        new File(includedBuildDir, "build.gradle") << """\
            plugins {
                id 'java'
                id 'com.gorylenko.gradle-git-properties'
            }

            gitProperties {
                // No explicit dotGitDirectory - should auto-detect from composite root
                customProperty 'test.scenario', 'composite-build'
            }
        """.stripIndent()

        // Run the included build's task from the composite root
        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withArguments(":included-build:generateGitProperties", "--info", "--stacktrace")
                .withProjectDir(compositeRootDir)

        def result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":included-build:generateGitProperties").outcome)

        // Verify git.properties was created in the included build
        def gitPropertiesFile = new File(includedBuildDir, "build/resources/main/git.properties")
        assertTrue("git.properties should exist", gitPropertiesFile.exists())

        def properties = new Properties()
        gitPropertiesFile.withInputStream { properties.load(it) }
        assertEquals("composite-build", properties.getProperty("test.scenario"))
        assertTrue("Should have git.commit.id", properties.getProperty("git.commit.id") != null)
        assertTrue("Should have git.branch", properties.getProperty("git.branch") != null)
    }
}
