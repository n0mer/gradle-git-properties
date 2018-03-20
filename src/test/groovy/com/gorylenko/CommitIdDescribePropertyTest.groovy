package com.gorylenko

import static org.junit.Assert.*

import java.io.File
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.junit.After
import org.junit.Before
import org.junit.Test

class CommitIdDescribePropertyTest {

    File projectDir
    Commit firstCommit
    Grgit repo

    @Before
    public void setUp() throws Exception {

        // Set up projectDir

        projectDir = File.createTempDir("BranchPropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            firstCommit = gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
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
    public void testDoCall() {

        assertEquals(firstCommit.abbreviatedId, new CommitIdDescribeProperty().doCall(repo))
    }


    @Test
    public void testDoCallOnDirty() {
        new File(projectDir, 'hello2.txt').text = 'Hello 2'

        assertEquals(firstCommit.abbreviatedId + '-dirty', new CommitIdDescribeProperty().doCall(repo))
    }
}
