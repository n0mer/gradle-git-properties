package com.gorylenko.properties

import static org.junit.Assert.*

import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade

class BuildUserEmailPropertyTest {

    File projectDir

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("BuildUserEmailPropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { })
    }

    @After
    public void tearDown() throws Exception {
        projectDir.deleteDir()
    }


    @Test
    public void testDoCallWithoutConfiguredEmail() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.setConfigString("user", null, "email", null)
        })
        def facade = GitFacade.open(projectDir)
        try {
            assertEquals('', new BuildUserEmailProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }


    @Test
    public void testDoCallWithConfiguredEmail() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.setConfigString("user", null, "email", "test@example.com")
        })
        def facade = GitFacade.open(projectDir)
        try {
            assertEquals("test@example.com", new BuildUserEmailProperty().doCall(facade))
        } finally {
            facade.close()
        }
    }

}
