package com.gorylenko.jgit

import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

/**
 * Tests for GitFacade.describe() - git describe functionality.
 */
class GitDescribeTest {

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
    void testDescribeWithAnnotatedTag() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.createTag("v1.0.0", "Version 1.0.0")

        facade = GitFacade.open(repoDir)

        def result = facade.describe()

        assertEquals("describe should return tag name", "v1.0.0", result)
    }

    @Test
    void testDescribeWithCommitsAfterTag() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.createTag("v1.0.0", "Version 1.0.0")
        helper.commitFile("test2.txt", "more content", "Second commit")

        facade = GitFacade.open(repoDir)

        def result = facade.describe()

        assertNotNull("describe should return value", result)
        assertTrue("describe should start with tag", result.startsWith("v1.0.0-1-g"))
    }

    @Test
    void testDescribeWithNoTags() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")

        facade = GitFacade.open(repoDir)

        def result = facade.describe()

        assertNull("describe should return null when no tags", result)
    }

    @Test
    void testDescribeWithLightweightTagAndTagsTrue() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.createLightweightTag("v1.0.0-light")

        facade = GitFacade.open(repoDir)

        // Without tags:true, lightweight tags are ignored
        def resultWithoutTags = facade.describe()
        assertNull("describe without tags option should not find lightweight tag", resultWithoutTags)

        // With tags:true, lightweight tags are included
        def resultWithTags = facade.describe(tags: true)
        assertEquals("describe with tags:true should find lightweight tag", "v1.0.0-light", resultWithTags)
    }

    @Test
    void testDescribeLongFormat() {
        def repoDir = temporaryFolder.newFolder("test-repo")
        helper = JGitTestHelper.create(repoDir)
        helper.commitFile("test.txt", "content", "Initial commit")
        helper.createTag("v1.0.0", "Version 1.0.0")

        facade = GitFacade.open(repoDir)

        def result = facade.describe(longDescr: true)

        assertNotNull("describe long should return value", result)
        assertTrue("describe long should include commit count and hash", result.startsWith("v1.0.0-0-g"))
    }
}
