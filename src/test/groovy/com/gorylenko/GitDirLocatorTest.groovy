package com.gorylenko

import static org.junit.Assert.*

import java.io.File

import org.junit.After
import org.junit.Before
import org.junit.Test

import com.gorylenko.properties.GitRepositoryBuilder

class GitDirLocatorTest {
    File projectDir

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("BranchPropertyTest", ".tmp")
    }
    @After
    public void tearDown() throws Exception {
        projectDir.deleteDir()
    }

    @Test
    public void testNonGitProject() {
        GitDirLocator locator = new GitDirLocator(projectDir)

        assertNull(locator.lookupGitDirectory(null))
    }

    @Test
    public void testAutodetectedGitRepoInCurrentDirectory() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        GitDirLocator locator = new GitDirLocator(projectDir)

        assertEquals(new File(projectDir, '.git'), locator.lookupGitDirectory(null))
    }

    @Test
    public void testAutodetectedGitRepoInParentDirectory() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        File projectSubDir = new File(projectDir, "subDir")
        GitDirLocator locator = new GitDirLocator(projectSubDir)

        assertEquals(new File(projectDir, '.git'), locator.lookupGitDirectory(null))
    }

    @Test
    public void testManuallyConfiguredGitProject() {

        File projectDir2 = File.createTempDir("BranchPropertyTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir2, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })
        GitDirLocator locator = new GitDirLocator(projectDir)

        assertEquals(new File(projectDir2, '.git'), locator.lookupGitDirectory(new File(projectDir2, '.git')))
    }
}
