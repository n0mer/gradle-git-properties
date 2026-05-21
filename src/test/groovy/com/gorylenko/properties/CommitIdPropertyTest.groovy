package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade

class CommitIdPropertyTest {

    File projectDir

    @Before
    public void setUp() throws Exception {
        // Set up projectDir
        projectDir = File.createTempDir("CommitIdPropertyTest", ".tmp")
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
            assertEquals('', new CommitIdProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnOneCommit() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit once
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        def facade = GitFacade.open(projectDir)
        try {
            def result = new CommitIdProperty().doCall(facade)
            // Should be full 40-char SHA
            assertEquals(40, result.length())
            assertTrue(result.matches('[a-f0-9]+'))
        } finally {
            facade.close()
        }
    }

}
