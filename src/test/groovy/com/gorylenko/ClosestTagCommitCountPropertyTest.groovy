package com.gorylenko

import static org.junit.Assert.*

import java.io.File
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.junit.After
import org.junit.Before
import org.junit.Test

class ClosestTagCommitCountPropertyTest {

    File projectDir
    Grgit repo

    @Before
    public void setUp() throws Exception {

        // Set up projectDir

        projectDir = File.createTempDir("BranchPropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // empty repo
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
    public void testDoCallOnEmptyRepo() {
        assertEquals('', new ClosestTagCommitCountProperty().doCall(repo))
    }

    @Test
    public void testDoCallNoTag() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        assertEquals('', new ClosestTagCommitCountProperty().doCall(repo))
    }

    @Test
    public void testDoCallOneTag() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            // add TAGONE to firstCommit (current HEAD)
            gitRepoBuilder.addTag("TAGONE")
        })

        assertEquals("0", new ClosestTagCommitCountProperty().doCall(repo))
    }

    @Test
    public void testDoCallOneTagOneCommit() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            // add TAGONE to firstCommit (current HEAD)
            gitRepoBuilder.addTag("TAGONE")
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        assertEquals("1", new ClosestTagCommitCountProperty().doCall(repo))
    }

    @Test
    public void testDoCallOneTagOneCommitSecondTag() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            // add TAGONE to firstCommit (current HEAD)
            gitRepoBuilder.addTag("TAGONE")
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAGTWO")
        })

        assertEquals("0", new ClosestTagCommitCountProperty().doCall(repo))
    }

}
