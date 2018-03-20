package com.gorylenko

import static org.junit.Assert.*

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Person
import org.junit.After
import org.junit.Before
import org.junit.Test

class BranchPropertyTest {

    File projectDir
    Grgit repo

    @Before
    public void setUp() throws Exception {

        // Set up projectDir

        projectDir = File.createTempDir("BranchPropertyTest", ".tmp")

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->

            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")

            // create a new branch "branch-1" at current location
            gitRepoBuilder.addBranch("branch-1")

            // commit 1 new file "hello2.txt"
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })


        // Set up repo
        repo = Grgit.open(dir: projectDir)

    }

    @After
    public void tearDown() throws Exception {
        repo?.close()
        projectDir.deleteDir()
    }

    @Test
    public void testDoCallOnMasterBranch() {

        assertEquals("master", new BranchProperty().doCall(repo))
    }

    @Test
    public void testDoCallOnBranch1() {
        repo.checkout (branch : "branch-1")

        assertEquals("branch-1", new BranchProperty().doCall(repo))
    }
}
