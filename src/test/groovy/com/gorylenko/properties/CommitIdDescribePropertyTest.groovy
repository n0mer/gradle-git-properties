package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade

class CommitIdDescribePropertyTest {

    File projectDir
    GitFacade facade

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("CommitIdDescribePropertyTest", ".tmp")
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
    public void testDoCallEmptyRepo() {
        facade = openFacade()
        assertEquals('', new CommitIdDescribeProperty().doCall(facade))
    }

    @Test
    public void testDoCallNoTag() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        facade = openFacade()
        assertEquals('', new CommitIdDescribeProperty().doCall(facade))
    }

    @Test
    public void testDoCallOnNoTagAndDirty() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        new File(projectDir, 'hello2.txt').text = 'Hello 2'
        facade = openFacade()
        assertEquals('', new CommitIdDescribeProperty().doCall(facade))
    }

    @Test
    public void testDoCallOneTag() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAGONE")
        })
        facade = openFacade()
        assertEquals("TAGONE", new CommitIdDescribeProperty().doCall(facade))
    }

    @Test
    public void testDoCallOneTagDirty() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAGONE")
        })
        // Modify tracked file to make repo dirty
        new File(projectDir, 'hello.txt').text = 'Modified'
        facade = openFacade()
        assertEquals("TAGONE-dirty", new CommitIdDescribeProperty().doCall(facade))
    }

    @Test
    public void testDoCallOneTagOneCommit() {
        String abbreviatedId = null
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAGONE")
            def secondCommit = gitRepoBuilder.commitFile("hello.txt", "Hello2", "Modified hello.txt")
            // Get abbreviated ID from RevCommit
            abbreviatedId = secondCommit.name.substring(0, 7)
        })
        facade = openFacade()
        def result = new CommitIdDescribeProperty().doCall(facade)
        assertTrue("Result should start with TAGONE-1-g", result.startsWith("TAGONE-1-g"))
    }

    @Test
    public void testDoCallOneTagOneCommitDirty() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAGONE")
            gitRepoBuilder.commitFile("hello.txt", "Hello2", "Modified hello.txt")
        })
        // Modify tracked file to make repo dirty
        new File(projectDir, 'hello.txt').text = 'Modified again'
        facade = openFacade()
        def result = new CommitIdDescribeProperty().doCall(facade)
        assertTrue("Result should start with TAGONE-1-g and end with -dirty",
                   result.startsWith("TAGONE-1-g") && result.endsWith("-dirty"))
    }

    @Test
    public void testDoCallShallowClone() {
        File tmpDir = File.createTempDir("CommitIdDescribePropertyTestShallowClone", ".tmp")
        GitFacade shallowFacade = null

        try {
            InputStream is = CommitIdDescribePropertyTest.class.getResourceAsStream('/shallowclone3.zip')

            is.withStream { Files.copy(it, new File(tmpDir, "shallowclone3.zip").toPath(), StandardCopyOption.REPLACE_EXISTING) }

            AntBuilder ant = new AntBuilder()
            ant.unzip(src: new File(tmpDir, "shallowclone3.zip"), dest: tmpDir, overwrite: "true")

            shallowFacade = GitFacade.open(new File(tmpDir, "shallowclone3"))
            // Should return abbreviated commit ID for shallow clones
            def result = new CommitIdDescribeProperty().doCall(shallowFacade)
            assertEquals("dc3a7d8", result)

        } finally {
            shallowFacade?.close()
            tmpDir.deleteDir()
        }
    }
}
