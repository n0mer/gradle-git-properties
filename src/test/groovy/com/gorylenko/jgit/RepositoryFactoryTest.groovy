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
}
