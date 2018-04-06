package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import java.text.SimpleDateFormat
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.junit.After
import org.junit.Before
import org.junit.Test

class TagsPropertyTest {

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
        assertEquals('', new TagsProperty().doCall(repo))
    }

    @Test
    public void testDoCallOneCommit() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        assertEquals('', new TagsProperty().doCall(repo))
    }

    @Test
    public void testDoCallWithOneTag() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAG-1")
        })
        assertEquals('TAG-1', new TagsProperty().doCall(repo))
    }


    @Test
    public void testDoCallWith2Tags() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAG-1")
            gitRepoBuilder.addTag("TAG-2")
        })
        assertEquals('TAG-1,TAG-2', new TagsProperty().doCall(repo))
    }

    @Test
    public void testDoCallWithNotCurrentTag() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAG-1")
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })
        assertEquals('', new TagsProperty().doCall(repo))
    }

}
