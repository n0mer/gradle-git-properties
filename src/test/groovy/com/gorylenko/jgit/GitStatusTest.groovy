package com.gorylenko.jgit

import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

/**
 * Tests for GitStatus - working tree status.
 */
class GitStatusTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    private JGitTestHelper helper
    private GitFacade facade

    @After
    void tearDown() {
        facade?.close()
        helper?.close()
    }

    @Test
    void testCleanRepoReturnsTrue() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        facade = GitFacade.open(repoDir)

        def status = facade.status()

        assertNotNull("status() should return GitStatus", status)
        assertTrue("status should be GitStatus", status instanceof GitStatus)
        assertTrue("clean repo should return true for clean", status.clean)
    }

    @Test
    void testDirtyRepoReturnsFalse() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        // Modify file to make it dirty
        new File(repoDir, "test.txt").text = "modified content"

        facade = GitFacade.open(repoDir)

        def status = facade.status()

        assertFalse("dirty repo should return false for clean", status.clean)
    }

    @Test
    void testUntrackedFileMakesCleanFalse() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        // Add untracked file - should make repo "dirty" to match Grgit behavior
        new File(repoDir, "untracked.txt").text = "untracked"

        facade = GitFacade.open(repoDir)

        def status = facade.status()

        // Note: Grgit includes untracked files in dirty check, so we match that behavior
        // This is important for consistent git.dirty property behavior
        assertFalse("untracked files should make repo dirty (Grgit compatibility)", status.clean)
    }

    @Test
    void testStagedFileReturnsFalse() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        // Add new file and stage it
        new File(repoDir, "new.txt").text = "new content"
        helper.git.add().addFilepattern("new.txt").call()

        facade = GitFacade.open(repoDir)

        def status = facade.status()

        assertFalse("staged files should make repo dirty", status.clean)
    }
}
