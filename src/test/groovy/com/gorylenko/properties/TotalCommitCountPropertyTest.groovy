package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade

class TotalCommitCountPropertyTest {

    File projectDir
    GitFacade facade

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("TotalCommitCountPropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // empty git repo
        })
    }

    private TotalCommitCountProperty createTarget() {
        new TotalCommitCountProperty(new CacheSupport())
    }

    @After
    public void tearDown() throws Exception {
        facade?.close()
        projectDir.deleteDir()
    }

    private GitFacade openFacade() {
        return GitFacade.open(projectDir)
    }

    @Test
    public void testDoCallOnEmptyRepo() {
        facade = openFacade()
        assertEquals('0', createTarget().doCall(facade))
    }

    @Test
    public void testDoCallOneCommit() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        facade = openFacade()
        assertEquals("1", createTarget().doCall(facade))
    }

    @Test
    public void testDoCallTwoCommits() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })
        facade = openFacade()
        assertEquals("2", createTarget().doCall(facade))
    }

    @Test
    public void testDoCallTwoCommitsNewBranchThenThirdCommit() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
            gitRepoBuilder.addBranchAndCheckout("my-branch")
            gitRepoBuilder.commitFile("hello3.txt", "Hello3", "Added hello3.txt")
        })
        facade = openFacade()
        assertEquals("3", createTarget().doCall(facade))
    }
}
