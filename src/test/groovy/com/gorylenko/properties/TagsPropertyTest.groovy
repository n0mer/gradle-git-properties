package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade

class TagsPropertyTest {

    File projectDir
    GitFacade facade

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("TagsPropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // empty repo
        })
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
        assertEquals('', new TagsProperty().doCall(facade))
    }

    @Test
    public void testDoCallOneCommit() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        facade = openFacade()
        assertEquals('', new TagsProperty().doCall(facade))
    }

    @Test
    public void testDoCallWithOneTag() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAG-1")
        })
        facade = openFacade()
        assertEquals('TAG-1', new TagsProperty().doCall(facade))
    }


    @Test
    public void testDoCallWith2Tags() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAG-1")
            gitRepoBuilder.addTag("TAG-2")
        })
        facade = openFacade()
        assertEquals('TAG-1,TAG-2', new TagsProperty().doCall(facade))
    }

    @Test
    public void testDoCallWithNotCurrentTag() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAG-1")
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })
        facade = openFacade()
        assertEquals('', new TagsProperty().doCall(facade))
    }
}
