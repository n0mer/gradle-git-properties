package com.gorylenko

import com.gorylenko.properties.GitRepositoryBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

/**
 * Unit tests for GitPropertiesPluginExtension.
 *
 * Related issue: #262
 */
class GitPropertiesPluginExtensionTest {

    File projectDir

    @Before
    void setUp() {
        projectDir = File.createTempDir("GitPropertiesPluginExtensionTest", ".tmp")
    }

    @After
    void tearDown() {
        projectDir?.deleteDir()
    }

    private Project createProjectWithGit() {
        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Test commit")
        })
        Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply 'com.gorylenko.gradle-git-properties'
        return project
    }

    private Project createProjectWithoutGit() {
        Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.pluginManager.apply 'com.gorylenko.gradle-git-properties'
        return project
    }

    private GitPropertiesPluginExtension getExtension(Project project) {
        return project.extensions.getByName("gitProperties") as GitPropertiesPluginExtension
    }

    // === Default Values Tests ===

    @Test
    void testDefaultGitPropertiesName() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        assertEquals("git.properties", ext.gitPropertiesName)
    }

    @Test
    void testDefaultDateFormat() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        assertEquals("yyyy-MM-dd'T'HH:mm:ssZ", ext.dateFormat)
    }

    @Test
    void testDefaultDateFormatTimeZoneIsNull() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        assertNull(ext.dateFormatTimeZone)
    }

    @Test
    void testDefaultFailOnNoGitDirectory() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        assertTrue(ext.failOnNoGitDirectory)
    }

    @Test
    void testDefaultForceIsFalse() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        assertFalse(ext.force)
    }

    @Test
    void testDefaultBranchIsNull() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        assertNull(ext.branch)
    }

    @Test
    void testDefaultExtPropertyIsNull() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        assertNull(ext.extProperty)
    }

    @Test
    void testDefaultKeysAreStandardProperties() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        assertEquals(GitProperties.standardProperties, ext.keys)
    }

    @Test
    void testDefaultCustomPropertiesIsEmpty() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        assertTrue(ext.customProperties.isEmpty())
    }

    @Test
    void testDefaultGitPropertiesDirNotPresent() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        assertFalse(ext.gitPropertiesDir.present)
    }

    @Test
    void testDefaultGitPropertiesResourceDirNotPresent() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        assertFalse(ext.gitPropertiesResourceDir.present)
    }

    // === Configuration Tests ===

    @Test
    void testSetGitPropertiesName() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        ext.gitPropertiesName = "custom.properties"
        assertEquals("custom.properties", ext.gitPropertiesName)
    }

    @Test
    void testSetDateFormat() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        ext.dateFormat = "yyyy-MM-dd"
        assertEquals("yyyy-MM-dd", ext.dateFormat)
    }

    @Test
    void testSetDateFormatTimeZone() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        ext.dateFormatTimeZone = "UTC"
        assertEquals("UTC", ext.dateFormatTimeZone)
    }

    @Test
    void testSetFailOnNoGitDirectory() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        ext.failOnNoGitDirectory = false
        assertFalse(ext.failOnNoGitDirectory)
    }

    @Test
    void testSetForce() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        ext.force = true
        assertTrue(ext.force)
    }

    @Test
    void testSetBranch() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        ext.branch = "custom-branch"
        assertEquals("custom-branch", ext.branch)
    }

    @Test
    void testSetExtProperty() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        ext.extProperty = "gitInfo"
        assertEquals("gitInfo", ext.extProperty)
    }

    @Test
    void testSetKeys() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        ext.keys = ["git.branch", "git.commit.id"]
        assertEquals(["git.branch", "git.commit.id"], ext.keys)
    }

    @Test
    void testSetGitPropertiesDir() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        ext.gitPropertiesDir.set(project.layout.buildDirectory.dir("custom"))

        assertTrue(ext.gitPropertiesDir.present)
        assertTrue(ext.gitPropertiesDir.get().asFile.absolutePath.contains("custom"))
    }

    @Test
    void testSetGitPropertiesResourceDir() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        ext.gitPropertiesResourceDir.set(project.layout.buildDirectory.dir("custom-resources"))

        assertTrue(ext.gitPropertiesResourceDir.present)
        assertTrue(ext.gitPropertiesResourceDir.get().asFile.absolutePath.contains("custom-resources"))
    }

    // === customProperty() Method Tests ===

    @Test
    void testCustomPropertyWithStringValue() {
        def project = createProjectWithGit()
        def ext = getExtension(project)

        ext.customProperty("custom.key", "custom-value")

        assertEquals(1, ext.customProperties.size())
        assertEquals("custom-value", ext.customProperties.get("custom.key"))
    }

    @Test
    void testCustomPropertyWithClosure() {
        def project = createProjectWithGit()
        def ext = getExtension(project)

        ext.customProperty("custom.closure", { "computed-value" })

        assertEquals(1, ext.customProperties.size())
        assertTrue(ext.customProperties.get("custom.closure") instanceof Closure)
    }

    @Test
    void testCustomPropertyMultipleCalls() {
        def project = createProjectWithGit()
        def ext = getExtension(project)

        ext.customProperty("key1", "value1")
        ext.customProperty("key2", "value2")
        ext.customProperty("key3", "value3")

        assertEquals(3, ext.customProperties.size())
        assertEquals("value1", ext.customProperties.get("key1"))
        assertEquals("value2", ext.customProperties.get("key2"))
        assertEquals("value3", ext.customProperties.get("key3"))
    }

    @Test
    void testCustomPropertyOverwrite() {
        def project = createProjectWithGit()
        def ext = getExtension(project)

        ext.customProperty("key", "original")
        ext.customProperty("key", "overwritten")

        assertEquals(1, ext.customProperties.size())
        assertEquals("overwritten", ext.customProperties.get("key"))
    }

    // === dotGitDirectory Tests ===

    @Test
    void testDotGitDirectoryConventionWithGitRepo() {
        def project = createProjectWithGit()
        def ext = getExtension(project)

        assertTrue(ext.dotGitDirectory.present)
        def gitDir = ext.dotGitDirectory.get().asFile
        assertTrue("dotGitDirectory should point to .git",
            gitDir.absolutePath.endsWith(".git"))
    }

    @Test
    void testDotGitDirectoryConventionWithoutGitRepo() {
        def project = createProjectWithoutGit()
        def ext = getExtension(project)

        // Without git repo, convention still sets a default (fallback to .git in project dir)
        assertTrue(ext.dotGitDirectory.present)
    }

    @Test
    void testDotGitDirectoryCanBeOverridden() {
        def project = createProjectWithGit()
        def ext = getExtension(project)

        def customGitDir = new File(projectDir, "custom-git")
        customGitDir.mkdir()
        ext.dotGitDirectory.set(customGitDir)

        assertEquals(customGitDir.absolutePath, ext.dotGitDirectory.get().asFile.absolutePath)
    }

    // === findGitDirectory Tests (nested project) ===

    @Test
    void testFindGitDirectoryInNestedProject() {
        // Create git repo in parent directory
        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Test commit")
        })

        // Create nested project directory
        def nestedDir = new File(projectDir, "nested/project")
        nestedDir.mkdirs()

        // Build project in nested directory
        Project project = ProjectBuilder.builder().withProjectDir(nestedDir).build()
        project.pluginManager.apply 'com.gorylenko.gradle-git-properties'
        def ext = getExtension(project)

        // Should find .git in parent directory
        assertTrue(ext.dotGitDirectory.present)
        def gitDir = ext.dotGitDirectory.get().asFile
        // Use canonicalPath to handle symlinks (macOS /var -> /private/var)
        assertEquals(new File(projectDir, ".git").canonicalPath, gitDir.canonicalPath)
    }

    // === resolveWorktreeGitDir Tests ===

    @Test
    void testResolveWorktreeGitDirWithRelativePath() {
        // Create main git repo
        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Test commit")
        })

        // Create a worktree-like structure
        def worktreeDir = new File(projectDir, "worktree")
        worktreeDir.mkdirs()

        // Create .git file (not directory) with relative gitdir path
        def gitFile = new File(worktreeDir, ".git")
        gitFile.text = "gitdir: ../.git/worktrees/worktree\n"

        // Create the worktrees directory in main .git
        def worktreesDir = new File(projectDir, ".git/worktrees/worktree")
        worktreesDir.mkdirs()

        // Build project in worktree directory
        Project project = ProjectBuilder.builder().withProjectDir(worktreeDir).build()
        project.pluginManager.apply 'com.gorylenko.gradle-git-properties'
        def ext = getExtension(project)

        // Should resolve to main .git directory (not the worktrees subdir)
        assertTrue(ext.dotGitDirectory.present)
        def gitDir = ext.dotGitDirectory.get().asFile
        // Use canonicalPath to handle symlinks (macOS /var -> /private/var)
        assertEquals(new File(projectDir, ".git").canonicalPath, gitDir.canonicalPath)
    }

    @Test
    void testResolveWorktreeGitDirWithAbsolutePath() {
        // Create main git repo
        GitRepositoryBuilder.setupProjectDir(projectDir, { builder ->
            builder.commitFile("test.txt", "content", "Test commit")
        })

        // Create a worktree-like structure
        def worktreeDir = new File(projectDir, "worktree-abs")
        worktreeDir.mkdirs()

        // Create .git file with absolute gitdir path
        def mainGitDir = new File(projectDir, ".git")
        def worktreesDir = new File(mainGitDir, "worktrees/worktree-abs")
        worktreesDir.mkdirs()

        def gitFile = new File(worktreeDir, ".git")
        gitFile.text = "gitdir: ${worktreesDir.absolutePath}\n"

        // Build project in worktree directory
        Project project = ProjectBuilder.builder().withProjectDir(worktreeDir).build()
        project.pluginManager.apply 'com.gorylenko.gradle-git-properties'
        def ext = getExtension(project)

        // Should resolve to main .git directory
        assertTrue(ext.dotGitDirectory.present)
        def gitDir = ext.dotGitDirectory.get().asFile
        // Use canonicalPath to handle symlinks (macOS /var -> /private/var)
        assertEquals(mainGitDir.canonicalPath, gitDir.canonicalPath)
    }

    // === toString() Test ===

    @Test
    void testToStringContainsProperties() {
        def project = createProjectWithGit()
        def ext = getExtension(project)
        ext.gitPropertiesName = "test.properties"
        ext.dateFormat = "yyyy"

        def str = ext.toString()
        assertTrue("toString should contain class name",
            str.contains("GitPropertiesPluginExtension"))
        assertTrue("toString should contain gitPropertiesName",
            str.contains("gitPropertiesName"))
    }
}
