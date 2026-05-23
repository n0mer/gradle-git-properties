package com.gorylenko.jgit

import org.eclipse.jgit.lib.Repository
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

/**
 * Tests for RepositoryFactory - opens JGit Repository from git dir or worktree.
 */
class RepositoryFactoryTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    private JGitTestHelper helper
    private Repository repository

    @After
    void tearDown() {
        repository?.close()
        helper?.close()
    }

    @Test
    void testOpenRegularRepository() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        repository = RepositoryFactory.open(repoDir)

        assertNotNull("Repository should be opened", repository)
        assertFalse("Repository should not be bare", repository.bare)
    }

    @Test
    void testOpenWorktreeRepository() {
        def mainRepoDir = temporaryFolder.newFolder("main-repo")
        def worktreeDir = new File(temporaryFolder.root, "worktree")

        helper = JGitTestHelper.create(mainRepoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.createWorktree(worktreeDir, "feature-branch")

        repository = RepositoryFactory.open(worktreeDir)

        assertNotNull("Repository should be opened from worktree", repository)
        // Note: JGit's Repository.branch returns main repo branch.
        // Worktree-specific branch resolution is handled in GitFacade.
        // This test verifies the repository can be opened successfully.
        assertNotNull("Repository should have a branch", repository.branch)
    }

    @Test
    void testOpenWorktreeRepositoryViaGitFacade() {
        // This test verifies GitFacade returns correct worktree branch
        def mainRepoDir = temporaryFolder.newFolder("main-repo")
        def worktreeDir = new File(temporaryFolder.root, "worktree")

        helper = JGitTestHelper.create(mainRepoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.createWorktree(worktreeDir, "feature-branch")

        def facade = GitFacade.open(worktreeDir)
        try {
            assertEquals("Should be on worktree branch", "feature-branch", facade.branch.current().name)
        } finally {
            facade.close()
        }
    }

    // ============================================
    // Tests for getDirectoriesToWatch
    // ============================================

    @Test
    void testGetDirectoriesToWatch_NullInput() {
        def result = RepositoryFactory.getDirectoriesToWatch(null)
        assertTrue("Null input should return empty list", result.isEmpty())
    }

    @Test
    void testGetDirectoriesToWatch_NonExistent() {
        def nonExistent = new File(temporaryFolder.root, "does-not-exist/.git")
        def result = RepositoryFactory.getDirectoriesToWatch(nonExistent)
        assertTrue("Non-existent file should return empty list", result.isEmpty())
    }

    @Test
    void testGetDirectoriesToWatch_RegularRepo() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        def dotGit = new File(repoDir, ".git")
        def result = RepositoryFactory.getDirectoriesToWatch(dotGit)

        assertEquals("Regular repo should return 1 directory", 1, result.size())
        assertEquals("Should return .git directory", dotGit.canonicalFile, result[0].canonicalFile)
    }

    @Test
    void testGetDirectoriesToWatch_Worktree() {
        def mainRepoDir = temporaryFolder.newFolder("main-repo")
        def worktreeDir = new File(temporaryFolder.root, "worktree")

        helper = JGitTestHelper.create(mainRepoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.createWorktree(worktreeDir, "feature-branch")

        def dotGit = new File(worktreeDir, ".git")
        def result = RepositoryFactory.getDirectoriesToWatch(dotGit)

        assertEquals("Worktree should return 2 directories", 2, result.size())
        // First should be worktree gitdir, second should be main git dir
        def mainGitDir = new File(mainRepoDir, ".git")
        assertTrue("Should include main git dir", result.any { it.canonicalPath == mainGitDir.canonicalPath })
    }
}
