package com.gorylenko

import static org.junit.Assert.*

import java.io.File
import java.text.SimpleDateFormat
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.junit.After
import org.junit.Before
import org.junit.Test

class CommitTimePropertyTest {

    File projectDir
    Commit firstCommit
    Grgit repo

    @Before
    public void setUp() throws Exception {

        // Set up projectDir

        projectDir = File.createTempDir("BranchPropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            firstCommit = gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
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
    public void testDoCall() {

        assertEquals(firstCommit.dateTime.toInstant().epochSecond.toString(), new CommitTimeProperty(null, null).doCall(repo))
    }

    @Test
    public void testDoCallWithFormat() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ")
        sdf.setTimeZone(TimeZone.getTimeZone("PST"))
        String date = sdf.format(Date.from(firstCommit.dateTime.toInstant()))

        assertEquals(date, new CommitTimeProperty("yyyy-MM-dd'T'HH:mmZ", "PST").doCall(repo))
    }
}
