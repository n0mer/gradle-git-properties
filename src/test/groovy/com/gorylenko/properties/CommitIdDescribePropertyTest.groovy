package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.junit.After
import org.junit.Before
import org.junit.Test

class CommitIdDescribePropertyTest {

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
    public void testDoCallEmptyRepo() {
        assertEquals('', new CommitIdDescribeProperty().doCall(repo))
    }

    @Test
    public void testDoCallNoTag() {
        Commit firstCommit
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            firstCommit = gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        assertEquals('', new CommitIdDescribeProperty().doCall(repo))
    }

    @Test
    public void testDoCallOnNoTagAndDirty() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        new File(projectDir, 'hello2.txt').text = 'Hello 2'

        assertEquals('', new CommitIdDescribeProperty().doCall(repo))
    }

    @Test
    public void testDoCallOneTag() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")

            // add TAGONE to firstCommit (current HEAD)
            gitRepoBuilder.addTag("TAGONE")
        })

        assertEquals("TAGONE", new CommitIdDescribeProperty().doCall(repo))
    }

    @Test
    public void testDoCallOneTagDirty() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")

            // add TAGONE to firstCommit (current HEAD)
            gitRepoBuilder.addTag("TAGONE")
            new File(projectDir, 'hello2.txt').text = 'Hello 2'
        })

        assertEquals("TAGONE-dirty", new CommitIdDescribeProperty().doCall(repo))
    }

    @Test
    public void testDoCallOneTagOneCommit() {
        Commit secondCommit
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")

            // add TAGONE to firstCommit (current HEAD)
            gitRepoBuilder.addTag("TAGONE")
            secondCommit = gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        assertEquals("TAGONE-1-g" + secondCommit.abbreviatedId, new CommitIdDescribeProperty().doCall(repo))
    }

    @Test
    public void testDoCallOneTagOneCommitDirty() {
        Commit secondCommit
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")

            // add TAGONE to firstCommit (current HEAD)
            gitRepoBuilder.addTag("TAGONE")
            secondCommit = gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            new File(projectDir, 'hello2.txt').text = 'Hello 2'
        })

        assertEquals("TAGONE-1-g" + secondCommit.abbreviatedId + "-dirty", new CommitIdDescribeProperty().doCall(repo))
    }

    @Test
    public void testDoCallOneTagOneCommitShallowClone() {

        File tmpDir = File.createTempDir("BranchPropertyTestShallowClone", ".tmp")
        Grgit repo1 = null

        try {
            InputStream is = CommitIdDescribePropertyTest.class.getResourceAsStream('/shallowclone3.zip')

            is.withStream { Files.copy(it, new File(tmpDir, "shallowclone3.zip").toPath(), StandardCopyOption.REPLACE_EXISTING) }

            AntBuilder ant  = new AntBuilder();

            ant.unzip(src: new File(tmpDir, "shallowclone3.zip") ,dest: tmpDir, overwrite:"true" )

            repo1 = Grgit.open(dir: new File(tmpDir, "shallowclone3"))

            assertEquals("dc3a7d8", new CommitIdDescribeProperty().doCall(repo1))

        } finally {
            repo1?.close()
            tmpDir.deleteDir()
        }

    }
}
