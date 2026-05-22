package com.gorylenko.jgit

import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

/**
 * Tests for GitTagService - tag operations.
 */
class GitTagServiceTest {

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
    void testListTagsReturnsAllTags() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.createTag("v1.0.0", "Version 1.0.0")
        helper.commitFile("test2.txt", "more", "Second commit")
        helper.createTag("v2.0.0", "Version 2.0.0")

        facade = GitFacade.open(repoDir)

        def tags = facade.tag.list()

        assertNotNull("list() should return tags", tags)
        assertEquals("should have 2 tags", 2, tags.size())

        def tagNames = tags.collect { it.name }.sort()
        assertEquals("tag names should match", ["v1.0.0", "v2.0.0"], tagNames)
    }

    @Test
    void testListTagsReturnsGitTagObjects() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        def commit = helper.commitFile("test.txt", "content", "Initial commit")
        helper.createTag("v1.0.0", "Version 1.0.0")

        facade = GitFacade.open(repoDir)

        def tags = facade.tag.list()

        assertEquals("should have 1 tag", 1, tags.size())

        def tag = tags[0]
        assertTrue("tag should be GitTag", tag instanceof GitTag)
        assertEquals("tag name should match", "v1.0.0", tag.name)
        assertNotNull("tag should have commit", tag.commit)
        assertTrue("commit should be GitCommit", tag.commit instanceof GitCommit)
        assertEquals("commit id should match", commit.name, tag.commit.id)
    }

    @Test
    void testListTagsWithNoTags() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        facade = GitFacade.open(repoDir)

        def tags = facade.tag.list()

        assertNotNull("list() should return empty list, not null", tags)
        assertTrue("list() should return empty list", tags.isEmpty())
    }

    @Test
    void testListTagsIncludesLightweightTags() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.createLightweightTag("v1.0.0-light")
        helper.createTag("v1.0.0", "Version 1.0.0")

        facade = GitFacade.open(repoDir)

        def tags = facade.tag.list()

        assertEquals("should have 2 tags", 2, tags.size())
        def tagNames = tags.collect { it.name }.sort()
        assertEquals("should include both tag types", ["v1.0.0", "v1.0.0-light"], tagNames)
    }
}
