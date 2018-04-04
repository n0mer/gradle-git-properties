package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.junit.After
import org.junit.Before
import org.junit.Test

class CommitMessageFullPropertyTest {

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
        assertEquals('', new CommitMessageFullProperty().doCall(repo))
    }

    @Test
    public void testDoCallOnOneCommit() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit once
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt\n\nSecond paragraph\n")
        })
        assertEquals("Added hello.txt\n\nSecond paragraph\n", new CommitMessageFullProperty().doCall(repo))
    }

}
