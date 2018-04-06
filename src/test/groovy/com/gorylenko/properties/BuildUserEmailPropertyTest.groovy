package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import java.text.SimpleDateFormat
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.junit.After
import org.junit.Before
import org.junit.Test

class BuildUserEmailPropertyTest {

    File projectDir
    Grgit repo

    @Before
    public void setUp() throws Exception {

        // Set up projectDir

        projectDir = File.createTempDir("BranchPropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { })

        // Set up repo
        repo = Grgit.open(dir: projectDir)

    }

    @After
    public void tearDown() throws Exception {
        repo?.close()
        projectDir.deleteDir()
    }


    @Test
    public void testDoCallWithoutConfiguredUserName() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.setConfigString("user", null, "email", null)
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        assertEquals('', new BuildUserEmailProperty().doCall(repo))
    }


    @Test
    public void testDoCallWithConfiguredUserName() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.setConfigString("user", null, "email", "test@example.com")
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        assertEquals("test@example.com", new BuildUserEmailProperty().doCall(repo))
    }


}
