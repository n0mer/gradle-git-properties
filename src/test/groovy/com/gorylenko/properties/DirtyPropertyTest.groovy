package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.junit.After
import org.junit.Before
import org.junit.Test

class DirtyPropertyTest {

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
        assertEquals('false', new DirtyProperty().doCall(repo))
    }

    @Test
    public void testDoCallOnEmptyRepoDirty() {
        new File(projectDir, 'hello2.txt').text = 'Hello 2'
        assertEquals('true', new DirtyProperty().doCall(repo))
    }

    @Test
    public void testDoCallOnOneCommit() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit once
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        assertEquals('false', new DirtyProperty().doCall(repo))
    }


    @Test
    public void testDoCallOnOneCommitDirty() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit once
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        new File(projectDir, 'hello2.txt').text = 'Hello 2'
        assertEquals('true', new DirtyProperty().doCall(repo))
    }
}
