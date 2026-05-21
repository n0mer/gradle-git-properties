package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade
import com.gorylenko.jgit.JGitTestHelper

class CommitUserEmailPropertyTest {

    File projectDir

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("CommitUserEmailPropertyTest", ".tmp")
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
            assertEquals('', new CommitUserEmailProperty().doCall(facade))
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
            def result = new CommitUserEmailProperty().doCall(facade)
            // Should be the default testuser email from JGitTestHelper
            assertEquals(JGitTestHelper.DEFAULT_AUTHOR_EMAIL, result)
        } finally {
            facade.close()
        }
    }

}
