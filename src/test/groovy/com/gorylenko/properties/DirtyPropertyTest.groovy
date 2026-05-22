package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade

class DirtyPropertyTest {

    File projectDir

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("DirtyPropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // empty repo
        })
    }

    @After
    public void tearDown() throws Exception {
        projectDir.deleteDir()
    }

    @Test
    public void testDoCallOnEmptyRepo() {
        def facade = GitFacade.open(projectDir)
        try {
            assertEquals('false', new DirtyProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnEmptyRepoDirty() {
        new File(projectDir, 'hello2.txt').text = 'Hello 2'
        def facade = GitFacade.open(projectDir)
        try {
            assertEquals('true', new DirtyProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnOneCommit() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        def facade = GitFacade.open(projectDir)
        try {
            assertEquals('false', new DirtyProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }


    @Test
    public void testDoCallOnOneCommitDirty() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        // Modify a tracked file
        new File(projectDir, 'hello.txt').text = 'Modified content'
        def facade = GitFacade.open(projectDir)
        try {
            assertEquals('true', new DirtyProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnOneCommitWithUntrackedFile() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        // Add untracked file
        new File(projectDir, 'hello2.txt').text = 'Hello 2'
        def facade = GitFacade.open(projectDir)
        try {
            assertEquals('true', new DirtyProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }
}
