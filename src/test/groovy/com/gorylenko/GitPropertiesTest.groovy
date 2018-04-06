package com.gorylenko

import static org.junit.Assert.*

import java.io.File

import org.ajoberstar.grgit.Grgit
import org.junit.After
import org.junit.Before
import org.junit.Test

import com.gorylenko.properties.GitRepositoryBuilder

class GitPropertiesTest {

    File projectDir
    File dotGitDirectory
    GitProperties props = new GitProperties()

    @Before
    public void setUp() throws Exception {

        // Set up projectDir

        projectDir = File.createTempDir("BranchPropertyTest", ".tmp")
        dotGitDirectory = new File(projectDir, '.git')

        GitRepositoryBuilder.setupProjectDir(projectDir, { })
    }

    @After
    public void tearDown() throws Exception {
        projectDir.deleteDir()
    }

    @Test
    public void getStandardProperties() {
        assertTrue(GitProperties.standardProperties.size() >= 20)
    }


    @Test
    public void testGenerateAllPropsOnEmptyRepo() {

        List<String> keys = GitProperties.standardProperties
        String dateFormat
        String dateFormatTimeZone
        String buildVersion = "1.0"
        Map<String, Closure> customProperties = [:]

        Map<String, String> generated = props.generate(dotGitDirectory, keys, dateFormat, dateFormatTimeZone, buildVersion, customProperties)

        GitProperties.standardProperties.each {
            assertNotNull(generated[it])
        }
    }

    @Test
    public void testGenerateAllPropsOnNonEmptyRepo() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        List<String> keys = GitProperties.standardProperties
        String dateFormat
        String dateFormatTimeZone
        String buildVersion = "1.0"
        Map<String, Closure> customProperties = ['test' : { return 10 }]

        Map<String, String> generated = props.generate(dotGitDirectory, keys, dateFormat, dateFormatTimeZone, buildVersion, customProperties)

        GitProperties.standardProperties.each {
            assertNotNull(generated[it])
        }
        assertEquals('10', generated['test'])
    }


    @Test
    public void testGenerateSelectedProps() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        List<String> keys = ['git.branch','git.commit.id','git.commit.time']
        String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
        String dateFormatTimeZone = "PST"
        String buildVersion = "1.0"
        Map<String, Closure> customProperties = [:]

        Map<String, String> generated = props.generate(dotGitDirectory, keys, dateFormat, dateFormatTimeZone, buildVersion, customProperties)

        assertEquals(3, generated.size())
        assertEquals('master', generated['git.branch'])
        assertNotNull('master', generated['git.commit.id'])
        assertNotNull('master', generated['git.commit.time'])
    }

}
