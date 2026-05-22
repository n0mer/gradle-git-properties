package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade

class CommitTimePropertyTest {

    File projectDir

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("CommitTimePropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // empty git repo
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
            assertEquals('', new CommitTimeProperty(null, null).doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOneCommit() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        def facade = GitFacade.open(projectDir)
        try {
            def result = new CommitTimeProperty(null, null).doCall(facade)
            // Should return epoch seconds when no format specified
            assertTrue(result.matches('\\d+'))
            // Should be a reasonable timestamp (after year 2000)
            def epochSeconds = Long.parseLong(result)
            assertTrue(epochSeconds > 946684800) // Jan 1, 2000
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallWithFormat() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        def facade = GitFacade.open(projectDir)
        try {
            def result = new CommitTimeProperty("yyyy-MM-dd'T'HH:mmZ", "PST").doCall(facade)
            // Should be formatted date
            assertTrue(result.matches('\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}[+-]\\d{4}'))
        } finally {
            facade.close()
        }
    }
}
