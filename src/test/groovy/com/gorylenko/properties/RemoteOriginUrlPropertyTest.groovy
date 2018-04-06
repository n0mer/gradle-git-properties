package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import java.text.SimpleDateFormat
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.junit.After
import org.junit.Before
import org.junit.Test

class RemoteOriginUrlPropertyTest {

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
        assertEquals('', new RemoteOriginUrlProperty().doCall(repo))
    }


    @Test
    public void testDoCallWithRemoteUrl() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.setConfigString("remote", "origin", "url", "git@github.com:n0mer/gradle-git-properties.git")
        })
        def repo1 = Grgit.open(dir: projectDir)

        assertEquals('git@github.com:n0mer/gradle-git-properties.git', new RemoteOriginUrlProperty().doCall(repo))
        repo1.close()
    }

    @Test
    public void testDoCallWithRemoteUrlHavingPassword() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.setConfigString("remote", "origin", "url", "https://user:password@myprivate.git.host/gitrepo.git")
        })
        def repo1 = Grgit.open(dir: projectDir)

        assertEquals('https://myprivate.git.host/gitrepo.git', new RemoteOriginUrlProperty().doCall(repo))
        repo1.close()
    }

    @Test
    public void testDoCallWithRemoteUrlWithoutPassword() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.setConfigString("remote", "origin", "url", "https://myprivate.git.host/gitrepo.git")
        })
        def repo1 = Grgit.open(dir: projectDir)

        assertEquals('https://myprivate.git.host/gitrepo.git', new RemoteOriginUrlProperty().doCall(repo))
        repo1.close()
    }
}
