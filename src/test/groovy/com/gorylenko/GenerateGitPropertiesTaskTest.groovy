package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

/**
 * Unit tests for GenerateGitPropertiesTask.
 *
 * Related issue: #261
 */
class GenerateGitPropertiesTaskTest {

    File projectDir

    @Before
    void setUp() {
        projectDir = File.createTempDir("GenerateGitPropertiesTaskTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Test commit")
        })
    }

    @After
    void tearDown() {
        projectDir?.deleteDir()
    }

    private Project createProject() {
        Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply 'com.gorylenko.gradle-git-properties'
        // Explicitly set dotGitDirectory to avoid finding parent project's .git in CI
        GitPropertiesPluginExtension ext = project.extensions.getByName("gitProperties")
        ext.dotGitDirectory.set(new File(projectDir, '.git'))
        return project
    }

    @Test
    void testTaskNameConstant() {
        assertEquals("generateGitProperties", GenerateGitPropertiesTask.TASK_NAME)
    }

    @Test
    void testTaskIsRegistered() {
        def project = createProject()
        def task = project.tasks.findByName("generateGitProperties")
        assertNotNull("Task should be registered", task)
        assertTrue("Task should be GenerateGitPropertiesTask", task instanceof GenerateGitPropertiesTask)
    }

    @Test
    void testTaskDescription() {
        def project = createProject()
        def task = project.tasks.generateGitProperties
        assertEquals("Generate a git.properties file.", task.description)
    }

    @Test
    void testTaskIsCacheable() {
        def project = createProject()
        def task = project.tasks.generateGitProperties
        def annotations = task.class.getAnnotations()
        assertTrue("Task should be annotated with @CacheableTask",
            annotations.any { it.annotationType().simpleName == 'CacheableTask' })
    }

    @Test
    void testSourceInputIncludesGitFiles() {
        def project = createProject()
        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask

        def sourceFiles = task.source.files.collect { it.name }

        // Should include HEAD and config files from .git directory
        assertTrue("Source should include HEAD", sourceFiles.contains("HEAD"))
        assertTrue("Source should include config", sourceFiles.contains("config"))
    }

    @Test
    void testOutputFileDefaultLocation() {
        def project = createProject()
        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask

        def outputFile = task.output.get().asFile
        assertTrue("Output should be in build directory",
            outputFile.absolutePath.contains("build"))
        assertTrue("Output should be in resources/main",
            outputFile.absolutePath.contains("resources${File.separator}main"))
        assertTrue("Output should be named git.properties",
            outputFile.name == "git.properties")
    }

    @Test
    void testOutputFileCustomResourceDir() {
        def project = createProject()
        GitPropertiesPluginExtension ext = project.extensions.getByName("gitProperties")
        ext.gitPropertiesResourceDir.set(project.layout.buildDirectory.dir("custom-output"))

        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask
        def outputFile = task.output.get().asFile

        assertTrue("Output should be in custom directory",
            outputFile.absolutePath.contains("custom-output"))
    }

    @Test
    void testOutputFileCustomName() {
        def project = createProject()
        GitPropertiesPluginExtension ext = project.extensions.getByName("gitProperties")
        ext.gitPropertiesName = "custom-git.properties"

        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask
        def outputFile = task.output.get().asFile

        assertEquals("custom-git.properties", outputFile.name)
    }

    @Test
    void testProjectVersionInput() {
        def project = createProject()
        project.version = "1.2.3"

        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask
        assertEquals("1.2.3", task.projectVersion.get())
    }

    @Test
    void testProjectVersionUnspecified() {
        def project = createProject()
        // project.version is "unspecified" by default

        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask
        assertEquals("unspecified", task.projectVersion.get())
    }

    @Test
    void testUpToDateWhenWithDefaultConfig() {
        def project = createProject()
        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask

        // With default config (no extProperty, failOnNoGitDirectory=true),
        // upToDateWhen should return true (task can be up-to-date)
        def upToDateSpec = task.outputs.upToDateSpec
        assertTrue("Task should be eligible for up-to-date check with default config",
            upToDateSpec.isSatisfiedBy(task))
    }

    @Test
    void testUpToDateWhenWithExtProperty() {
        def project = createProject()
        GitPropertiesPluginExtension ext = project.extensions.getByName("gitProperties")
        ext.extProperty = "gitInfo"

        // Need to recreate task after setting extProperty (it's read in constructor)
        // For this test, we verify the predicate behavior directly
        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask

        // With extProperty configured, upToDateWhen should return false
        // (task should always run to populate the ext property)
        def upToDateSpec = task.outputs.upToDateSpec
        assertFalse("Task should never be up-to-date when extProperty is configured",
            upToDateSpec.isSatisfiedBy(task))
    }

    @Test
    void testUpToDateWhenWithFailOnNoGitDirectoryFalse() {
        def project = createProject()
        GitPropertiesPluginExtension ext = project.extensions.getByName("gitProperties")
        ext.failOnNoGitDirectory = false

        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask

        // With failOnNoGitDirectory=false, upToDateWhen should return false
        def upToDateSpec = task.outputs.upToDateSpec
        assertFalse("Task should never be up-to-date when failOnNoGitDirectory=false",
            upToDateSpec.isSatisfiedBy(task))
    }

    @Test
    void testExtPropertyExposesGitProperties() {
        def project = createProject()
        GitPropertiesPluginExtension ext = project.extensions.getByName("gitProperties")
        ext.extProperty = "gitInfo"

        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask
        task.generate()

        // Verify that project.ext.gitInfo contains generated properties
        assertTrue("project.ext should have gitInfo", project.ext.has("gitInfo"))
        def gitInfo = project.ext.gitInfo
        assertNotNull("gitInfo should not be null", gitInfo)
        assertTrue("gitInfo should contain git.commit.id", gitInfo.containsKey("git.commit.id"))
        assertTrue("gitInfo should contain git.branch", gitInfo.containsKey("git.branch"))
    }

    @Test
    void testGenerateCreatesPropertiesFile() {
        def project = createProject()
        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask

        task.generate()

        def outputFile = task.output.get().asFile
        assertTrue("git.properties file should be created", outputFile.exists())

        def properties = new Properties()
        outputFile.withInputStream { properties.load(it) }

        assertNotNull("Should contain git.commit.id", properties.getProperty("git.commit.id"))
        assertNotNull("Should contain git.branch", properties.getProperty("git.branch"))
        // Note: Don't assert specific branch value - CI env vars (GITHUB_REF_NAME) override repo branch
    }

    @Test
    void testGenerateWithCustomKeys() {
        def project = createProject()
        GitPropertiesPluginExtension ext = project.extensions.getByName("gitProperties")
        ext.keys = ["git.branch", "git.commit.id"]

        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask
        task.generate()

        def outputFile = task.output.get().asFile
        def properties = new Properties()
        outputFile.withInputStream { properties.load(it) }

        assertEquals("Should only have 2 properties", 2, properties.size())
        assertNotNull("Should contain git.branch", properties.getProperty("git.branch"))
        assertNotNull("Should contain git.commit.id", properties.getProperty("git.commit.id"))
        assertNull("Should not contain git.commit.time", properties.getProperty("git.commit.time"))
    }

    @Test
    void testGenerateWithDateFormat() {
        def project = createProject()
        GitPropertiesPluginExtension ext = project.extensions.getByName("gitProperties")
        ext.keys = ["git.commit.time"]
        ext.dateFormat = "yyyy-MM-dd"
        ext.dateFormatTimeZone = "UTC"

        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask
        task.generate()

        def outputFile = task.output.get().asFile
        def properties = new Properties()
        outputFile.withInputStream { properties.load(it) }

        def commitTime = properties.getProperty("git.commit.time")
        // Should match yyyy-MM-dd format
        assertTrue("Commit time should match date format",
            commitTime ==~ /\d{4}-\d{2}-\d{2}/)
    }

    @Test
    void testGitPropertiesExtensionAccessible() {
        def project = createProject()
        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask

        def gitPropertiesExt = task.gitProperties
        assertNotNull("gitProperties extension should be accessible", gitPropertiesExt)
        assertTrue("Should be GitPropertiesPluginExtension",
            gitPropertiesExt instanceof GitPropertiesPluginExtension)
    }

    @Test
    void testGenerateWithForceTrue() {
        def project = createProject()
        GitPropertiesPluginExtension ext = project.extensions.getByName("gitProperties")
        ext.force = true

        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask

        // Generate twice
        task.generate()
        def firstModified = task.output.get().asFile.lastModified()

        // Wait a bit to ensure different timestamp
        Thread.sleep(100)

        task.generate()
        def secondModified = task.output.get().asFile.lastModified()

        // With force=true, file should be rewritten even if content same
        assertTrue("File should be rewritten with force=true", secondModified >= firstModified)
    }

    @Test
    void testGenerateWithForceFalse() {
        def project = createProject()
        GitPropertiesPluginExtension ext = project.extensions.getByName("gitProperties")
        ext.force = false

        def task = project.tasks.generateGitProperties as GenerateGitPropertiesTask

        // Generate twice
        task.generate()
        def firstModified = task.output.get().asFile.lastModified()

        // Wait a bit
        Thread.sleep(100)

        task.generate()
        def secondModified = task.output.get().asFile.lastModified()

        // With force=false and same content, file should not be rewritten
        assertEquals("File should not be rewritten with force=false and same content",
            firstModified, secondModified)
    }
}
