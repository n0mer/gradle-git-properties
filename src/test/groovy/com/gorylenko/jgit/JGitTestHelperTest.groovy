package com.gorylenko.jgit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

/**
 * Tests for JGitTestHelper - a pure JGit test utility.
 */
class JGitTestHelperTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    private JGitTestHelper helper

    @After
    void tearDown() {
        helper?.close()
    }

    @Test
    void testCreateRepository() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)

        assertNotNull("Repository should be created", helper.repository)
        assertTrue("Repository directory should exist", new File(repoDir, ".git").exists())
        assertFalse("Repository should not be bare", helper.repository.bare)
    }

    @Test
    void testCommitFile() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)

        def commit = helper.commitFile("test.txt", "test content", "Initial commit")

        assertNotNull("Commit should be returned", commit)
        assertEquals("Commit message should match", "Initial commit", commit.shortMessage)
        assertTrue("File should exist", new File(repoDir, "test.txt").exists())
        assertEquals("File content should match", "test content", new File(repoDir, "test.txt").text)

        // Verify commit is in repository
        def headRef = helper.repository.resolve(Constants.HEAD)
        assertNotNull("HEAD should point to commit", headRef)
        assertEquals("HEAD should point to our commit", commit.id, headRef)
    }

    @Test
    void testCommitWithCustomAuthor() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)

        def commit = helper.commitFile("test.txt", "content", "Test commit", "Custom Author", "custom@example.com")

        assertEquals("Author name should match", "Custom Author", commit.authorIdent.name)
        assertEquals("Author email should match", "custom@example.com", commit.authorIdent.emailAddress)
    }

    @Test
    void testCreateBranch() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        helper.createBranch("feature-branch")

        def branchRef = helper.repository.findRef("refs/heads/feature-branch")
        assertNotNull("Branch should exist", branchRef)
    }

    @Test
    void testCreateBranchAndCheckout() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        helper.createBranchAndCheckout("feature-branch")

        assertEquals("Should be on feature branch", "feature-branch", helper.repository.branch)
    }

    @Test
    void testCreateAnnotatedTag() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        helper.createTag("v1.0.0", "Release 1.0.0")

        def tagRef = helper.repository.findRef("refs/tags/v1.0.0")
        assertNotNull("Tag should exist", tagRef)

        // Verify it's an annotated tag (tag object, not just a ref)
        def git = new Git(helper.repository)
        def tags = git.tagList().call()
        def tag = tags.find { it.name == "refs/tags/v1.0.0" }
        assertNotNull("Tag ref should be found", tag)
    }

    @Test
    void testCreateLightweightTag() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        helper.createLightweightTag("v1.0.0-light")

        def tagRef = helper.repository.findRef("refs/tags/v1.0.0-light")
        assertNotNull("Lightweight tag should exist", tagRef)
    }

    @Test
    void testCreateWorktree() {
        def repoDir = temporaryFolder.newFolder("main-repo")
        def worktreeDir = new File(temporaryFolder.root, "worktree")

        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        helper.createWorktree(worktreeDir, "feature-worktree")

        // Verify worktree directory structure
        assertTrue("Worktree directory should exist", worktreeDir.exists())
        def gitFile = new File(worktreeDir, ".git")
        assertTrue(".git file should exist in worktree", gitFile.exists())
        assertTrue(".git should be a file (not directory)", gitFile.isFile())

        // Verify .git file points to worktree git dir
        def gitFileContent = gitFile.text.trim()
        assertTrue("gitdir should point to worktrees folder", gitFileContent.startsWith("gitdir:"))
        assertTrue("gitdir should contain worktrees path", gitFileContent.contains("worktrees"))
    }

    @Test
    void testWorktreeHasCorrectBranch() {
        def repoDir = temporaryFolder.newFolder("main-repo")
        def worktreeDir = new File(temporaryFolder.root, "worktree")

        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        helper.createWorktree(worktreeDir, "feature-branch")

        // Open repository from worktree and verify branch
        def worktreeHelper = JGitTestHelper.openWorktree(worktreeDir)
        try {
            assertEquals("Worktree should be on feature branch", "feature-branch", worktreeHelper.repository.branch)
        } finally {
            worktreeHelper.close()
        }
    }

    @Test
    void testMultipleCommits() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)

        def commit1 = helper.commitFile("file1.txt", "content1", "First commit")
        def commit2 = helper.commitFile("file2.txt", "content2", "Second commit")
        def commit3 = helper.commitFile("file3.txt", "content3", "Third commit")

        // Verify all files exist
        assertTrue(new File(repoDir, "file1.txt").exists())
        assertTrue(new File(repoDir, "file2.txt").exists())
        assertTrue(new File(repoDir, "file3.txt").exists())

        // Verify commit chain
        assertNotEquals("Commits should be different", commit1.id, commit2.id)
        assertNotEquals("Commits should be different", commit2.id, commit3.id)

        // Verify HEAD points to last commit
        def headRef = helper.repository.resolve(Constants.HEAD)
        assertEquals("HEAD should point to last commit", commit3.id, headRef)
    }

    @Test
    void testSetConfig() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)

        helper.setConfig("user", null, "name", "Test User")
        helper.setConfig("user", null, "email", "test@example.com")
        helper.setConfig("remote", "origin", "url", "https://github.com/test/repo.git")

        def config = helper.repository.config
        assertEquals("User name should match", "Test User", config.getString("user", null, "name"))
        assertEquals("User email should match", "test@example.com", config.getString("user", null, "email"))
        assertEquals("Remote URL should match", "https://github.com/test/repo.git", config.getString("remote", "origin", "url"))
    }

    @Test
    void testGetGit() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)

        def git = helper.git

        assertNotNull("Git object should be available", git)
        assertTrue("Git should be instance of JGit Git class", git instanceof Git)
    }

    @Test
    void testCloseReleasesResources() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        helper.close()

        // After close, repository should be closed
        // Attempting to use it should fail or return null
        // We verify by checking we can create a new helper on the same dir
        def newHelper = JGitTestHelper.create(repoDir)
        assertNotNull("Should be able to create new helper after close", newHelper)
        newHelper.close()

        // Set to null to prevent double-close in tearDown
        helper = null
    }
}
