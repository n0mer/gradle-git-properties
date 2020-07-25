package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.junit.After
import org.junit.Before
import org.junit.Test

class TotalCommitCountPropertyTest {

    File projectDir
    Grgit repo

    @Before
    public void setUp() throws Exception {

        // Set up projectDir

        projectDir = File.createTempDir("BranchPropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // empty git repo
        })

        // Set up repo
        repo = Grgit.open(dir: projectDir)
    }

    private TotalCommitCountProperty createTarget() {
        new TotalCommitCountProperty(new CacheSupport())
    }

    @After
    public void tearDown() throws Exception {
        repo?.close()
        projectDir.deleteDir()
    }

    @Test
    public void testDoCallOnEmptyRepo() {

        assertEquals('0', createTarget().doCall(repo))
    }

    @Test
    public void testDoCallOneCommit() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        assertEquals("1", createTarget().doCall(repo))
    }

    @Test
    public void testDoCallTwoCommits() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })

        assertEquals("2", createTarget().doCall(repo))
    }

    @Test
    public void testDoCallTwoCommitsNewBranchThenThirdCommit() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
            gitRepoBuilder.addBranchAndCheckout("my-branch")
            gitRepoBuilder.commitFile("hello3.txt", "Hello3", "Added hello3.txt")
        })

        assertEquals("3", createTarget().doCall(repo))
    }

}
