package com.gorylenko

import org.ajoberstar.grgit.Grgit
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.nio.file.Files
import java.nio.file.StandardCopyOption

import static org.junit.Assert.*

/**
 * Comprehensive tests for all git properties in shallow clone scenarios.
 *
 * Related issue: #266
 *
 * Shallow clones are common in CI/CD environments for faster checkout.
 * This test ensures all properties behave correctly with limited git history.
 *
 * The shallowclone3.zip contains:
 * - A shallow clone with depth=1
 * - Single visible commit: dc3a7d8 "commit_2"
 * - Branch: master
 * - Remote: origin
 * - No tags
 */
class ShallowClonePropertiesTest {

    File tmpDir
    File shallowCloneDir
    File dotGitDirectory
    Grgit repo
    GitProperties gitProperties

    @Before
    void setUp() {
        tmpDir = File.createTempDir("ShallowClonePropertiesTest", ".tmp")

        // Extract shallow clone from test resource
        InputStream is = ShallowClonePropertiesTest.class.getResourceAsStream('/shallowclone3.zip')
        is.withStream {
            Files.copy(it, new File(tmpDir, "shallowclone3.zip").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        new AntBuilder().unzip(src: new File(tmpDir, "shallowclone3.zip"), dest: tmpDir, overwrite: "true")

        shallowCloneDir = new File(tmpDir, "shallowclone3")
        dotGitDirectory = new File(shallowCloneDir, ".git")
        repo = Grgit.open(dir: shallowCloneDir)
        gitProperties = new GitProperties()
    }

    @After
    void tearDown() {
        repo?.close()
        tmpDir?.deleteDir()
    }

    private Map<String, String> generateProperties(List<String> keys) {
        return gitProperties.generate(dotGitDirectory, keys, null, null, null, "1.0.0", [:])
    }

    private String generateProperty(String key) {
        return generateProperties([key])[key]
    }

    // === Verify Shallow Clone State ===

    @Test
    void testShallowCloneMarkerFileExists() {
        def shallowFile = new File(dotGitDirectory, "shallow")
        assertTrue("Shallow clone marker file should exist", shallowFile.exists())
    }

    @Test
    void testShallowCloneHasLimitedHistory() {
        // Shallow clone should only have 1 commit visible
        def commits = repo.log()
        assertEquals("Shallow clone should have limited commits visible", 1, commits.size())
    }

    // === Branch Property ===

    @Test
    void testBranchProperty() {
        // Note: In CI, GITHUB_REF_NAME would override branch detection.
        // To test the shallow clone's actual branch, we'd need to mock env vars.
        // Here we verify the branch property is generated (non-null).
        def branch = generateProperty("git.branch")
        assertNotNull("Branch should not be null", branch)
        assertTrue("Branch should not be empty", branch.length() > 0)
    }

    @Test
    void testBranchPropertyWithExplicitValue() {
        // Test with explicit branch value (bypasses CI env var detection)
        def result = gitProperties.generate(dotGitDirectory,
            ["git.branch"], null, null, "explicit-branch", "1.0.0", [:])
        assertEquals("explicit-branch", result["git.branch"])
    }

    // === Commit ID Properties ===

    @Test
    void testCommitIdProperty() {
        def commitId = generateProperty("git.commit.id")
        assertEquals("dc3a7d8ef1bb07b3c170c04b2de5d2b11bdb435e", commitId)
    }

    @Test
    void testCommitIdAbbrevProperty() {
        def commitIdAbbrev = generateProperty("git.commit.id.abbrev")
        assertEquals("dc3a7d8", commitIdAbbrev)
    }

    @Test
    void testCommitIdDescribePropertyWithoutTags() {
        // In shallow clone without tags, describe returns abbreviated commit id
        def describe = generateProperty("git.commit.id.describe")
        // describe returns empty or abbreviated commit when no tags
        assertTrue("Describe should return empty or abbreviated commit",
            describe == "" || describe == "dc3a7d8")
    }

    // === Commit Message Properties ===

    @Test
    void testCommitMessageShortProperty() {
        def message = generateProperty("git.commit.message.short")
        assertEquals("commit_2", message)
    }

    @Test
    void testCommitMessageFullProperty() {
        def message = generateProperty("git.commit.message.full")
        // Full message may include trailing newline
        assertEquals("commit_2", message.trim())
    }

    // === Commit Time Property ===

    @Test
    void testCommitTimeProperty() {
        def commitTime = generateProperty("git.commit.time")
        assertNotNull("Commit time should not be null", commitTime)
        // Should be a valid timestamp format
        assertTrue("Commit time should not be empty", commitTime.length() > 0)
    }

    @Test
    void testCommitTimePropertyWithFormat() {
        def result = gitProperties.generate(dotGitDirectory,
            ["git.commit.time"], "yyyy-MM-dd", "UTC", null, "1.0.0", [:])
        def commitTime = result["git.commit.time"]
        // Should match yyyy-MM-dd format
        assertTrue("Commit time should match date format: ${commitTime}",
            commitTime ==~ /\d{4}-\d{2}-\d{2}/)
    }

    // === Commit User Properties ===

    @Test
    void testCommitUserNameProperty() {
        def userName = generateProperty("git.commit.user.name")
        assertNotNull("Commit user name should not be null", userName)
    }

    @Test
    void testCommitUserEmailProperty() {
        def userEmail = generateProperty("git.commit.user.email")
        assertNotNull("Commit user email should not be null", userEmail)
    }

    // === Build Properties (not affected by shallow clone) ===

    @Test
    void testBuildHostProperty() {
        def buildHost = generateProperty("git.build.host")
        assertNotNull("Build host should not be null", buildHost)
        assertTrue("Build host should not be empty", buildHost.length() > 0)
    }

    @Test
    void testBuildUserNameProperty() {
        def buildUserName = generateProperty("git.build.user.name")
        // Build user name may be empty if not configured
        assertNotNull("Build user name should not be null", buildUserName)
    }

    @Test
    void testBuildUserEmailProperty() {
        def buildUserEmail = generateProperty("git.build.user.email")
        // Build user email may be empty if not configured
        assertNotNull("Build user email should not be null", buildUserEmail)
    }

    @Test
    void testBuildVersionProperty() {
        def result = gitProperties.generate(dotGitDirectory,
            ["git.build.version"], null, null, null, "2.5.0", [:])
        assertEquals("2.5.0", result["git.build.version"])
    }

    // === Dirty Property ===

    @Test
    void testDirtyPropertyClean() {
        def dirty = generateProperty("git.dirty")
        assertEquals("false", dirty)
    }

    @Test
    void testDirtyPropertyWithUncommittedChanges() {
        // Add uncommitted changes
        new File(shallowCloneDir, "new-file.txt").text = "uncommitted content"

        def dirty = generateProperty("git.dirty")
        assertEquals("true", dirty)
    }

    // === Tags Property ===

    @Test
    void testTagsPropertyWithoutTags() {
        def tags = generateProperty("git.tags")
        assertEquals("", tags)
    }

    // === Remote Origin URL Property ===

    @Test
    void testRemoteOriginUrlProperty() {
        def remoteUrl = generateProperty("git.remote.origin.url")
        assertNotNull("Remote origin URL should not be null", remoteUrl)
        // The shallow clone has a remote configured
        assertTrue("Remote origin URL should not be empty", remoteUrl.length() > 0)
    }

    // === Closest Tag Properties ===

    @Test
    void testClosestTagNamePropertyWithoutTags() {
        def closestTag = generateProperty("git.closest.tag.name")
        assertEquals("", closestTag)
    }

    @Test
    void testClosestTagCommitCountPropertyWithoutTags() {
        def tagCommitCount = generateProperty("git.closest.tag.commit.count")
        assertEquals("", tagCommitCount)
    }

    // === Total Commit Count Property ===

    @Test
    void testTotalCommitCountProperty() {
        def totalCount = generateProperty("git.total.commit.count")
        // Shallow clone with depth=1 should only see 1 commit
        assertEquals("1", totalCount)
    }

    // === All Standard Properties at Once ===

    @Test
    void testAllStandardPropertiesGenerated() {
        def result = generateProperties(GitProperties.standardProperties)

        // Verify all standard properties are present and non-null
        GitProperties.standardProperties.each { key ->
            assertTrue("Property ${key} should be present", result.containsKey(key))
            assertNotNull("Property ${key} should not be null", result[key])
        }
    }

    @Test
    void testAllStandardPropertiesHaveValues() {
        def result = generateProperties(GitProperties.standardProperties)

        // Properties that should always have non-empty values in a valid shallow clone
        def nonEmptyProperties = [
            "git.branch",
            "git.commit.id",
            "git.commit.id.abbrev",
            "git.commit.time",
            "git.commit.message.short",
            "git.commit.message.full",
            "git.build.host",
            "git.build.version",
            "git.total.commit.count"
        ]

        nonEmptyProperties.each { key ->
            assertTrue("Property ${key} should have non-empty value: '${result[key]}'",
                result[key]?.length() > 0)
        }
    }

    // === Properties with Expected Empty Values in Shallow Clone Without Tags ===

    @Test
    void testPropertiesExpectedEmptyWithoutTags() {
        def result = generateProperties(GitProperties.standardProperties)

        // These properties are expected to be empty when there are no tags
        def expectedEmptyProperties = [
            "git.tags",
            "git.closest.tag.name",
            "git.closest.tag.commit.count"
        ]

        expectedEmptyProperties.each { key ->
            assertEquals("Property ${key} should be empty without tags", "", result[key])
        }
    }
}
