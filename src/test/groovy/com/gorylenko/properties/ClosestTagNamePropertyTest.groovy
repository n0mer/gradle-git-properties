package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade

class ClosestTagNamePropertyTest {

    File projectDir
    GitFacade facade

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("ClosestTagNamePropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // empty repo
        })
    }

    private ClosestTagNameProperty createTarget() {
        new ClosestTagNameProperty(new CacheSupport())
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
        assertEquals('', createTarget().doCall(facade))
    }

    @Test
    public void testDoCallNoTag() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        facade = openFacade()
        assertEquals('', createTarget().doCall(facade))
    }

    @Test
    public void testDoCallOneTag() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAGONE")
        })
        facade = openFacade()
        assertEquals("TAGONE", createTarget().doCall(facade))
    }

    @Test
    public void testDoCallOneTagOneCommit() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAGONE")
            gitRepoBuilder.commitFile("hello.txt", "Hello2", "Modified hello.txt")
        })
        facade = openFacade()
        assertEquals("TAGONE", createTarget().doCall(facade))
    }

    @Test
    public void testDoCallOneTagOneCommitSecondTag() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            gitRepoBuilder.addTag("TAGONE")
            gitRepoBuilder.commitFile("hello.txt", "Hello2", "Modified hello.txt")
            gitRepoBuilder.addTag("TAGTWO")
        })
        facade = openFacade()
        assertEquals("TAGTWO", createTarget().doCall(facade))
    }

    @Test
    public void testDoCallShallowClone() {
        File tmpDir = File.createTempDir("ClosestTagNamePropertyTestShallowClone", ".tmp")
        GitFacade shallowFacade = null

        try {
            InputStream is = ClosestTagNamePropertyTest.class.getResourceAsStream('/shallowclone3.zip')

            is.withStream { Files.copy(it, new File(tmpDir, "shallowclone3.zip").toPath(), StandardCopyOption.REPLACE_EXISTING) }

            AntBuilder ant = new AntBuilder()
            ant.unzip(src: new File(tmpDir, "shallowclone3.zip"), dest: tmpDir, overwrite: "true")

            shallowFacade = GitFacade.open(new File(tmpDir, "shallowclone3"))
            assertEquals("", createTarget().doCall(shallowFacade))

        } finally {
            shallowFacade?.close()
            tmpDir.deleteDir()
        }
    }
}
