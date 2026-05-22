package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade

class RemoteOriginUrlPropertyTest {

    File projectDir

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("RemoteOriginUrlPropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
    }

    @After
    public void tearDown() throws Exception {
        projectDir.deleteDir()
    }

    @Test
    public void testDoCallNoRemote() {
        def facade = GitFacade.open(projectDir)
        try {
            assertEquals('', new RemoteOriginUrlProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }


    @Test
    public void testDoCallWithRemoteUrl() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.setConfigString("remote", "origin", "url", "git@github.com:n0mer/gradle-git-properties.git")
        })
        def facade = GitFacade.open(projectDir)
        try {
            assertEquals('git@github.com:n0mer/gradle-git-properties.git', new RemoteOriginUrlProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallWithRemoteUrlHavingPassword() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.setConfigString("remote", "origin", "url", "https://user:password@myprivate.git.host/gitrepo.git")
        })
        def facade = GitFacade.open(projectDir)
        try {
            assertEquals('https://myprivate.git.host/gitrepo.git', new RemoteOriginUrlProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallWithRemoteUrlWithoutPassword() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.setConfigString("remote", "origin", "url", "https://myprivate.git.host/gitrepo.git")
        })
        def facade = GitFacade.open(projectDir)
        try {
            assertEquals('https://myprivate.git.host/gitrepo.git', new RemoteOriginUrlProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }
}
