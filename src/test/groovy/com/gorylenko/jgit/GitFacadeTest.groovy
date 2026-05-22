package com.gorylenko.jgit

import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

/**
 * Tests for GitFacade - the main JGit wrapper API.
 */
class GitFacadeTest {

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
    void testOpenAndClose() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        facade = GitFacade.open(repoDir)

        assertNotNull("GitFacade should be opened", facade)
        assertTrue("GitFacade should implement AutoCloseable", facade instanceof AutoCloseable)

        // Should close without error
        facade.close()
        facade = null // prevent double-close in tearDown
    }

    @Test
    void testOpenWorktree() {
        def mainRepoDir = temporaryFolder.newFolder("main-repo")
        def worktreeDir = new File(temporaryFolder.root, "worktree")

        helper = JGitTestHelper.create(mainRepoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.createWorktree(worktreeDir, "feature-branch")

        facade = GitFacade.open(worktreeDir)

        assertNotNull("GitFacade should be opened from worktree", facade)
    }

    @Test
    void testIsEmptyOnEmptyRepo() {
        def repoDir = temporaryFolder.newFolder("empty-repo")
        helper = JGitTestHelper.create(repoDir)
        // No commits

        facade = GitFacade.open(repoDir)

        assertTrue("Empty repository should return true for isEmpty()", facade.isEmpty())
    }

    @Test
    void testIsEmptyOnRepoWithCommit() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        facade = GitFacade.open(repoDir)

        assertFalse("Repository with commits should return false for isEmpty()", facade.isEmpty())
    }

    @Test
    void testHeadReturnsCommitWithAllProperties() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        def jgitCommit = helper.commitFile("test.txt", "content", "Short message\n\nFull message body here.",
                "John Doe", "john@example.com")

        facade = GitFacade.open(repoDir)

        def commit = facade.head()

        assertNotNull("head() should return a commit", commit)
        assertTrue("commit should be GitCommit", commit instanceof GitCommit)

        // Verify commit properties
        assertEquals("id should match", jgitCommit.name, commit.id)
        assertEquals("abbreviatedId should be 7 chars", 7, commit.abbreviatedId.length())
        assertTrue("id should start with abbreviatedId", commit.id.startsWith(commit.abbreviatedId))
        assertEquals("shortMessage should match", "Short message", commit.shortMessage)
        assertEquals("fullMessage should match", "Short message\n\nFull message body here.", commit.fullMessage)
        assertNotNull("dateTime should not be null", commit.dateTime)

        // Verify author (GitPerson)
        def author = commit.author
        assertNotNull("author should not be null", author)
        assertTrue("author should be GitPerson", author instanceof GitPerson)
        assertEquals("author name should match", "John Doe", author.name)
        assertEquals("author email should match", "john@example.com", author.email)
    }

    @Test
    void testHeadOnEmptyRepoReturnsNull() {
        def repoDir = temporaryFolder.newFolder("empty-repo")
        helper = JGitTestHelper.create(repoDir)
        // No commits

        facade = GitFacade.open(repoDir)

        assertNull("head() should return null for empty repo", facade.head())
    }

    // Task 1.9: log() tests

    @Test
    void testLogReturnsCommitsInOrder() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        def commit1 = helper.commitFile("file1.txt", "content1", "First commit")
        def commit2 = helper.commitFile("file2.txt", "content2", "Second commit")
        def commit3 = helper.commitFile("file3.txt", "content3", "Third commit")

        facade = GitFacade.open(repoDir)

        def commits = facade.log()

        assertEquals("log() should return 3 commits", 3, commits.size())
        // Most recent first
        assertEquals("First commit should be most recent", commit3.name, commits[0].id)
        assertEquals("Second commit should be second", commit2.name, commits[1].id)
        assertEquals("Third commit should be oldest", commit1.name, commits[2].id)
    }

    @Test
    void testLogWithMaxCommitsLimit() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("file1.txt", "content1", "First commit")
        helper.commitFile("file2.txt", "content2", "Second commit")
        def commit3 = helper.commitFile("file3.txt", "content3", "Third commit")

        facade = GitFacade.open(repoDir)

        def commits = facade.log(maxCommits: 2)

        assertEquals("log(maxCommits: 2) should return 2 commits", 2, commits.size())
        assertEquals("First commit should be most recent", commit3.name, commits[0].id)
    }

    @Test
    void testLogOnEmptyRepoReturnsEmptyList() {
        def repoDir = temporaryFolder.newFolder("empty-repo")
        helper = JGitTestHelper.create(repoDir)
        // No commits

        facade = GitFacade.open(repoDir)

        def commits = facade.log()

        assertNotNull("log() should not return null", commits)
        assertTrue("log() on empty repo should return empty list", commits.isEmpty())
    }

    // Task 1.10: Escape hatches tests

    @Test
    void testJgitReturnsRawRepository() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        facade = GitFacade.open(repoDir)

        def repo = facade.jgit

        assertNotNull("jgit should return raw Repository", repo)
        assertTrue("jgit should be JGit Repository", repo instanceof org.eclipse.jgit.lib.Repository)
        // Verify it's usable
        assertNotNull("Repository should resolve HEAD", repo.resolve("HEAD"))
    }

    @Test
    void testJgitCommandsReturnsRawGitObject() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        facade = GitFacade.open(repoDir)

        def git = facade.jgitCommands

        assertNotNull("jgitCommands should return Git object", git)
        assertTrue("jgitCommands should be JGit Git", git instanceof org.eclipse.jgit.api.Git)
        // Verify it's usable
        def status = git.status().call()
        assertNotNull("Git object should execute commands", status)
    }

    // Task 1.11: Config access tests

    @Test
    void testGetConfigSimple() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.setConfig("user", null, "name", "Test User")
        helper.setConfig("user", null, "email", "test@example.com")

        facade = GitFacade.open(repoDir)

        assertEquals("getConfig should read user.name", "Test User", facade.getConfig("user", "name"))
        assertEquals("getConfig should read user.email", "test@example.com", facade.getConfig("user", "email"))
    }

    @Test
    void testGetConfigWithSubsection() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.setConfig("remote", "origin", "url", "https://github.com/example/repo.git")
        helper.setConfig("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*")

        facade = GitFacade.open(repoDir)

        assertEquals("getConfig should read remote.origin.url",
                "https://github.com/example/repo.git",
                facade.getConfig("remote", "origin", "url"))
        assertEquals("getConfig should read remote.origin.fetch",
                "+refs/heads/*:refs/remotes/origin/*",
                facade.getConfig("remote", "origin", "fetch"))
    }

    @Test
    void testGetConfigReturnsNullForMissing() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        facade = GitFacade.open(repoDir)

        assertNull("getConfig should return null for missing config", facade.getConfig("nonexistent", "key"))
        assertNull("getConfig should return null for missing subsection config", facade.getConfig("remote", "nonexistent", "url"))
    }

    // Issue #234: Configurable commit ID abbreviation length

    @Test
    void testGetAbbreviatedIdWithLength7() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        facade = GitFacade.open(repoDir)

        def abbrevId = facade.getAbbreviatedId(7)

        assertNotNull("getAbbreviatedId(7) should return a value", abbrevId)
        assertEquals("getAbbreviatedId(7) should return 7 chars", 7, abbrevId.length())
        assertTrue("abbreviatedId should be hex", abbrevId.matches('[a-f0-9]+'))
    }

    @Test
    void testGetAbbreviatedIdWithLength10() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        facade = GitFacade.open(repoDir)

        def abbrevId = facade.getAbbreviatedId(10)

        assertEquals("getAbbreviatedId(10) should return 10 chars", 10, abbrevId.length())
    }

    @Test
    void testGetAbbreviatedIdOnEmptyRepoReturnsNull() {
        def repoDir = temporaryFolder.newFolder("empty-repo")
        helper = JGitTestHelper.create(repoDir)
        // No commits

        facade = GitFacade.open(repoDir)

        assertNull("getAbbreviatedId on empty repo should return null", facade.getAbbreviatedId(7))
    }
}
