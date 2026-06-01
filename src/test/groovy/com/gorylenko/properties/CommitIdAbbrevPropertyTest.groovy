package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade

class CommitIdAbbrevPropertyTest {

    File projectDir

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("CommitIdAbbrevPropertyTest", ".tmp")
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
            assertEquals('', new CommitIdAbbrevProperty().doCall(facade))
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
            def result = new CommitIdAbbrevProperty().doCall(facade)
            assertEquals("Default should be 7 chars", 7, result.length())
            assertTrue(result.matches('[a-f0-9]+'))
        } finally {
            facade.close()
        }
    }

    // Issue #234: Configurable commit ID abbreviation length

    @Test
    public void testDoCallWithLength10() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        def facade = GitFacade.open(projectDir)
        try {
            def result = new CommitIdAbbrevProperty(10).doCall(facade)
            assertEquals("Should return 10 chars", 10, result.length())
            assertTrue(result.matches('[a-f0-9]+'))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallWithLength2() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        def facade = GitFacade.open(projectDir)
        try {
            def result = new CommitIdAbbrevProperty(2).doCall(facade)
            // JGit may return more than 2 chars when needed for uniqueness within the repo
            // (e.g. when the commit SHA shares a 2-char prefix with another object like the tree or blob).
            // Assert >= 2 to reflect that 2 is a minimum request, not a guaranteed output length.
            assertTrue("Should return at least 2 chars", result.length() >= 2)
            assertTrue("Should return at most 40 chars", result.length() <= 40)
            assertTrue("Should be hex", result.matches('[a-f0-9]+'))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallWithLength40() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        def facade = GitFacade.open(projectDir)
        try {
            def result = new CommitIdAbbrevProperty(40).doCall(facade)
            assertEquals("Should return full SHA (40 chars)", 40, result.length())
        } finally {
            facade.close()
        }
    }

}
