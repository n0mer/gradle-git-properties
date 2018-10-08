package com.gorylenko

import static org.junit.Assert.*

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
        String branch
        String buildVersion = "1.0"
        Map<String, Closure> customProperties = [:]

        Map<String, String> generated = props.generate(dotGitDirectory, keys, dateFormat, dateFormatTimeZone, branch, buildVersion, customProperties)

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
        String branch
        String buildVersion = "1.0"
        Map<String, Closure> customProperties = ['test' : { return 10 }]

        Map<String, String> generated = props.generate(dotGitDirectory, keys, dateFormat, dateFormatTimeZone, branch, buildVersion, customProperties)

        GitProperties.standardProperties.each {
            assertNotNull(generated[it])
        }
        assertEquals('10', generated['test'])
    }


    @Test
    public void testGenerateSelectedWithUserDefinedBranchProps() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            // add TAGONE to firstCommit (current HEAD)
            gitRepoBuilder.addTag("TAGONE")
        })

        List<String> keys = ['git.branch','git.commit.id.describe']
        String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
        String dateFormatTimeZone = "PST"
        String branch = "mybranch"
        String buildVersion = "1.0"
        Map<String, Closure> customProperties = [:]

        Map<String, String> generated = props.generate(dotGitDirectory, keys, dateFormat, dateFormatTimeZone, branch, buildVersion, customProperties)

        assertEquals(2, generated.size())
        assertEquals('mybranch', generated['git.branch'])
        assertEquals("TAGONE", generated['git.commit.id.describe'])
    }

    @Test
    public void testGenerateSelectedProps() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        List<String> keys = ['git.branch','git.commit.id','git.commit.time']
        String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
        String dateFormatTimeZone = "PST"
        String branch
        String buildVersion = "1.0"
        Map<String, Closure> customProperties = [:]

        Map<String, String> generated = props.generate(dotGitDirectory, keys, dateFormat, dateFormatTimeZone, branch, buildVersion, customProperties)

        assertEquals(3, generated.size())
        assertEquals('master', generated['git.branch'])
        assertNotNull(generated['git.commit.id'])
        assertNotNull(generated['git.commit.time'])
    }

    @Test
    public void testGenerateAllPropsOnShalowClonedRepo() {


        List<String> keys = GitProperties.standardProperties
        String dateFormat
        String dateFormatTimeZone
        String branch
        String buildVersion = "1.0"
        Map<String, Closure> customProperties = ['test' : { return 10 }]


        File tmpDir = File.createTempDir("BranchPropertyTestShallowClone", ".tmp")
        Grgit repo1 = null

        try {
            InputStream is = GitPropertiesTest.class.getResourceAsStream('/shallowclone3.zip')

            is.withStream { Files.copy(it, new File(tmpDir, "shallowclone3.zip").toPath(), StandardCopyOption.REPLACE_EXISTING) }

            AntBuilder ant  = new AntBuilder();

            ant.unzip(src: new File(tmpDir, "shallowclone3.zip") ,dest: tmpDir, overwrite:"true" )

            repo1 = Grgit.open(dir: new File(tmpDir, "shallowclone3"))

            Map<String, String> generated = props.generate(dotGitDirectory, keys, dateFormat, dateFormatTimeZone, branch, buildVersion, customProperties)

            GitProperties.standardProperties.each {
                assertNotNull(generated[it])
            }
            assertEquals('10', generated['test'])

        } finally {
            repo1?.close()
            tmpDir.deleteDir()
        }


    }

}
