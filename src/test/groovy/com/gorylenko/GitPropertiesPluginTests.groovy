package com.gorylenko

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class GitPropertiesPluginTests {

    File projectDir

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("gradle-git-properties", ".tmp");
    }

    @After
    public void tearDown() throws Exception {
        projectDir.deleteDir()
    }

    @Test
    public void testGenerate() {

        // copy this project dir to temp directory (including git repository folder)
        new AntBuilder().copy(todir: projectDir) { fileset(dir : ".", defaultexcludes: false) }

        Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply 'com.gorylenko.gradle-git-properties'

        // FIXME: Didn't find any way to change `rootProject`, so just set the property.
        GitPropertiesPluginExtension ext = project.getExtensions().getByName("gitProperties")
        ext.dotGitDirectory.set(new File(projectDir, '.git'))

        def task = project.tasks.generateGitProperties
        assertTrue(task instanceof GenerateGitPropertiesTask)

        task.generate()

        def gitPropertiesFile = project.buildDir.getAbsolutePath() + '/resources/main/git.properties'

        Properties properties = new Properties()
        properties.load(new FileInputStream(gitPropertiesFile))
        GitProperties.standardProperties.each{
            assertNotNull(properties.getProperty(it))
        }
    }

    @Test
    public void testGenerateWithMissingGitRepoShouldNotFail() {

        Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply 'com.gorylenko.gradle-git-properties'

        // FIXME: Didn't find any way to change `rootProject`, so just set the property.
        GitPropertiesPluginExtension ext = project.getExtensions().getByName("gitProperties")
        ext.dotGitDirectory.set(projectDir)
        ext.failOnNoGitDirectory = false;

        def task = project.tasks.generateGitProperties
        assertTrue(task instanceof GenerateGitPropertiesTask)

        task.generate()

        def gitPropertiesFile = project.buildDir.getAbsolutePath() + '/resources/main/git.properties'
        assertFalse(new File(gitPropertiesFile).exists())
    }

    @Test
    public void testGenerateWithMissingGitRepoShouldFail() {

        Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply 'com.gorylenko.gradle-git-properties'

        // FIXME: Didn't find any way to change `rootProject`, so just set the property.
        GitPropertiesPluginExtension ext = project.getExtensions().getByName("gitProperties")
        ext.dotGitDirectory.set(projectDir)
        // failOnNoGitDirectory is true by default

        def task = project.tasks.getByName("generateGitProperties")
        assertTrue(task instanceof GenerateGitPropertiesTask)

        try {
            task.generate()
            fail('should have gotten a GradleException')
        } catch (Exception e) {
            assertEquals(GradleException, e.class)
            assertEquals("No Git repository found.", e.message)
        }
    }
}
