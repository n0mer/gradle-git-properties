package com.gorylenko.jgit

import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

/**
 * Tests for GitBranch - branch operations.
 */
class GitBranchTest {

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
    void testCurrentBranchOnMaster() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        facade = GitFacade.open(repoDir)

        def branchInfo = facade.branch.current()

        assertNotNull("current() should return branch info", branchInfo)
        assertTrue("branchInfo should be GitBranchInfo", branchInfo instanceof GitBranchInfo)
        assertEquals("branch name should be master", "master", branchInfo.name)
    }

    @Test
    void testCurrentBranchOnFeatureBranch() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.createBranchAndCheckout("feature/my-feature")

        facade = GitFacade.open(repoDir)

        def branchInfo = facade.branch.current()

        assertEquals("branch name should be feature/my-feature", "feature/my-feature", branchInfo.name)
    }

    @Test
    void testCurrentBranchInWorktree() {
        def mainRepoDir = temporaryFolder.newFolder("main-repo")
        def worktreeDir = new File(temporaryFolder.root, "worktree")

        helper = JGitTestHelper.create(mainRepoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.createWorktree(worktreeDir, "worktree-branch")

        facade = GitFacade.open(worktreeDir)

        def branchInfo = facade.branch.current()

        assertEquals("worktree should report its own branch", "worktree-branch", branchInfo.name)
    }
}
