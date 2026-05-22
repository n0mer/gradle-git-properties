package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade

class CommitMessageShortPropertyTest {

    File projectDir

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("CommitMessageShortPropertyTest", ".tmp")
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
            assertEquals('', new CommitMessageShortProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnOneCommit() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt\n\nSecond paragraph\n")
        })
        def facade = GitFacade.open(projectDir)
        try {
            // Should return first line of commit message
            assertEquals("Added hello.txt", new CommitMessageShortProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }

}
