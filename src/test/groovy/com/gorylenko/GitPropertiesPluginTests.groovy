package com.gorylenko

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class GitPropertiesPluginTests {
    
    @Test
    public void testGenerate() {
        def projectDir = new File('.')

        Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply 'com.gorylenko.gradle-git-properties'

        // FIXME: Didn't find any way to change `rootProject`, so just set the property.
        project.gitProperties.gitRepositoryRoot = projectDir

        def task = project.tasks.generateGitProperties
        assertTrue(task instanceof GitPropertiesPlugin.GenerateGitPropertiesTask)
        
        task.generate()

        def gitPropertiesFile = project.buildDir.getAbsolutePath() + '/resources/main/git.properties'
        
        Properties properties = new Properties()
        properties.load(new FileInputStream(gitPropertiesFile))
        assertNotNull(properties.getProperty("git.branch"))
        assertNotNull(properties.getProperty("git.commit.id"))
        assertNotNull(properties.getProperty("git.commit.id.abbrev"))
        assertNotNull(properties.getProperty("git.commit.user.name"))
        assertNotNull(properties.getProperty("git.commit.user.email"))
        assertNotNull(properties.getProperty("git.commit.message.short"))
        assertNotNull(properties.getProperty("git.commit.message.full"))
        assertNotNull(properties.getProperty("git.commit.time"))
    }
    
}
