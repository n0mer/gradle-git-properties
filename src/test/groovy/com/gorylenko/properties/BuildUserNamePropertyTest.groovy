package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade

class BuildUserNamePropertyTest {

    File projectDir

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("BuildUserNamePropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { })
    }

    @After
    public void tearDown() throws Exception {
        projectDir.deleteDir()
    }


    @Test
    public void testDoCallWithoutConfiguredUserName() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.setConfigString("user", null, "name", null)
        })
        def facade = GitFacade.open(projectDir)
        try {
            assertEquals('', new BuildUserNameProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }


    @Test
    public void testDoCallWithConfiguredUserName() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.setConfigString("user", null, "name", "Test User")
        })
        def facade = GitFacade.open(projectDir)
        try {
            assertEquals("Test User", new BuildUserNameProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }

}
