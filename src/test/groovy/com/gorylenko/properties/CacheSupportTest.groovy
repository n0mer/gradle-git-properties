package com.gorylenko.properties

import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade

import static org.junit.Assert.*

/**
 * Tests for CacheSupport class.
 *
 * Related issue: #267
 */
class CacheSupportTest {

    File projectDir
    GitFacade facade

    @Before
    void setUp() {
        projectDir = File.createTempDir("CacheSupportTest", ".tmp")
        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("file1.txt", "content1", "First commit")
            builder.commitFile("file2.txt", "content2", "Second commit")
        })
    }

    @After
    void tearDown() {
        facade?.close()
        projectDir?.deleteDir()
    }

    private GitFacade openFacade() {
        return GitFacade.open(projectDir)
    }

    @Test
    void testGetReturnsNullForMissingKey() {
        def cache = new CacheSupport()
        assertNull(cache.get("nonexistent-key"))
    }

    @Test
    void testPutAndGetReturnsValue() {
        def cache = new CacheSupport()
        cache.put("test-key", "test-value")
        assertEquals("test-value", cache.get("test-key"))
    }

    @Test
    void testPutOverwritesExistingValue() {
        def cache = new CacheSupport()
        cache.put("key", "value1")
        cache.put("key", "value2")
        assertEquals("value2", cache.get("key"))
    }

    @Test
    void testTotalCommitCountReturnsCorrectCount() {
        facade = openFacade()
        def cache = new CacheSupport()
        assertEquals(2, cache.totalCommitCount(facade))
    }

    @Test
    void testTotalCommitCountCachesResult() {
        facade = openFacade()
        def cache = new CacheSupport()

        // First call should compute and cache
        def count1 = cache.totalCommitCount(facade)
        assertEquals(2, count1)

        // Close facade before modifying repo
        facade.close()

        // Add another commit
        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("file3.txt", "content3", "Third commit")
        })

        // Reopen facade - HEAD changes, so new count
        facade = openFacade()
        def count2 = cache.totalCommitCount(facade)
        assertEquals(3, count2)
    }

    @Test
    void testTotalCommitCountWithDifferentHeads() {
        facade = openFacade()
        def cache = new CacheSupport()

        // Get count at current HEAD
        def count1 = cache.totalCommitCount(facade)
        assertEquals(2, count1)

        // Close facade before modifying repo
        facade.close()

        // Create a branch and add a commit
        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.addBranchAndCheckout("feature")
            builder.commitFile("feature.txt", "feature", "Feature commit")
        })

        // Different HEAD should get different count
        facade = openFacade()
        def count2 = cache.totalCommitCount(facade)
        assertEquals(3, count2)
    }

    @Test
    void testDescribeReturnsNullWithoutTags() {
        facade = openFacade()
        def cache = new CacheSupport()
        // Without tags, describe returns null
        def describe = cache.describe(facade, false)
        assertNull(describe)
    }

    @Test
    void testDescribeWithLongFormat() {
        // Add a tag first
        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.addTag("v1.0.0")
        })

        facade = openFacade()
        def cache = new CacheSupport()

        def shortDescribe = cache.describe(facade, false)
        def longDescribe = cache.describe(facade, true)

        assertNotNull(shortDescribe)
        assertNotNull(longDescribe)
        // Long format includes commit count and hash suffix
        assertTrue("Long describe should be longer", longDescribe.length() >= shortDescribe.length())
    }

    @Test
    void testDescribeIsMemoized() {
        // Add a tag
        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.addTag("v1.0.0")
        })

        facade = openFacade()
        def cache = new CacheSupport()

        // Call describe multiple times with same parameters
        def result1 = cache.describe(facade, false)
        def result2 = cache.describe(facade, false)

        // Should return same result (memoized)
        assertEquals(result1, result2)
    }

    @Test
    void testCacheWorksWithDifferentKeyTypes() {
        def cache = new CacheSupport()

        cache.put("string-key", "string-value")
        cache.put(123, "int-value")
        cache.put(['list', 'key'], "list-value")

        assertEquals("string-value", cache.get("string-key"))
        assertEquals("int-value", cache.get(123))
        assertEquals("list-value", cache.get(['list', 'key']))
    }

    @Test
    void testCacheHandlesNullValue() {
        def cache = new CacheSupport()

        // ConcurrentHashMap doesn't allow null values, so this should throw
        try {
            cache.put("key", null)
            fail("Expected NullPointerException")
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    void testCacheHandlesNullKey() {
        def cache = new CacheSupport()

        // ConcurrentHashMap doesn't allow null keys
        try {
            cache.put(null, "value")
            fail("Expected NullPointerException")
        } catch (NullPointerException e) {
            // Expected
        }
    }
}
