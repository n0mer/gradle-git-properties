package com.gorylenko.properties

import static org.junit.Assert.*

import java.util.Map
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.gorylenko.jgit.GitFacade
import com.gorylenko.jgit.JGitTestHelper

class BranchPropertyTest {

    File projectDir
    JGitTestHelper helper

    @Before
    public void setUp() throws Exception {
        projectDir = File.createTempDir("BranchPropertyTest", ".tmp")
        helper = JGitTestHelper.create(projectDir)
    }

    @After
    public void tearDown() throws Exception {
        helper?.close()
        projectDir.deleteDir()
    }

    // Must always override getEnv() for object under test otherwise the tests will fail on CI servers because of environment variables
    BranchProperty getTestObject(String branch, Map<String, String> env) {
        BranchProperty instance = new BranchProperty(branch)
        instance.setEnv(env)
        return instance
    }

    @Test
    public void testDoCallOnEmptyRepo() {
        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [:])
            assertEquals('', prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnEmptyRepoWithUserDefinedBranch() {
        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject("mybranch", [:])
            assertEquals('mybranch', prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnMasterBranch() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")
        helper.createBranch("branch-1")
        helper.commitFile("hello2.txt", "Hello2", "Added hello2.txt")

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [:])
            assertEquals("master", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnBranch1() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")
        helper.createBranch("branch-1")
        helper.commitFile("hello2.txt", "Hello2", "Added hello2.txt")

        // Checkout branch-1
        helper.git.checkout().setName("branch-1").call()

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [:])
            assertEquals("branch-1", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallWithUserDefinedBranch() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")
        helper.createBranch("branch-1")
        helper.commitFile("hello2.txt", "Hello2", "Added hello2.txt")

        def facade = GitFacade.open(projectDir)
        try {
            assertEquals("mybranch", getTestObject("mybranch", [:]).doCall(facade))
            assertEquals("mybranch", getTestObject("mybranch", [JOB_NAME: 'MyJob', GIT_LOCAL_BRANCH: 'local-branch']).doCall(facade))
            assertEquals("mybranch", getTestObject("mybranch", [JOB_NAME: 'MyJob', GIT_BRANCH: 'git-branch']).doCall(facade))
            assertEquals("mybranch", getTestObject("mybranch", [TRAVIS: 'true', TRAVIS_BRANCH: 'local-branch']).doCall(facade))
            assertEquals("mybranch", getTestObject("mybranch", [TEAMCITY_VERSION: '1', 'teamcity.build.branch': 'local-branch']).doCall(facade))
            assertEquals("mybranch", getTestObject("mybranch", [GITLAB_CI: 'true', 'CI_COMMIT_REF_NAME': 'local-branch']).doCall(facade))
            assertEquals("mybranch", getTestObject("mybranch", [BAMBOO_BUILDKEY: 'true', 'BAMBOO_PLANREPOSITORY_BRANCH': 'local-branch']).doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnJenkinsServer() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")
        helper.createBranch("branch-1")

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [JOB_NAME: 'MyJob', GIT_LOCAL_BRANCH: 'local-branch'])
            assertEquals("local-branch", prop.doCall(facade))

            BranchProperty prop2 = getTestObject(null, [JOB_NAME: 'MyJob', GIT_BRANCH: 'git-branch'])
            assertEquals("git-branch", prop2.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnTravisServer() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")
        helper.createBranch("branch-1")

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [TRAVIS: 'true', TRAVIS_BRANCH: 'local-branch'])
            assertEquals("local-branch", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnTeamCityServer() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")
        helper.createBranch("branch-1")

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [TEAMCITY_VERSION: '1', 'teamcity.build.branch': 'local-branch'])
            assertEquals("local-branch", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnGitlab() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")
        helper.createBranch("branch-1")

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [GITLAB_CI: 'true', 'CI_COMMIT_REF_NAME': 'local-branch'])
            assertEquals("local-branch", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnBamboo() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")
        helper.createBranch("branch-1")

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [BAMBOO_BUILDKEY: 'true', 'BAMBOO_PLANREPOSITORY_BRANCH': 'local-branch'])
            assertEquals("local-branch", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    /**
     * Helper method to checkout a specific commit (creates detached HEAD state).
     */
    private void checkoutCommit(String commitId) {
        helper.git.checkout()
            .setName(commitId)
            .call()
    }

    /**
     * Test detached HEAD state - when checking out a specific commit instead of a branch.
     * In detached HEAD state, JGit returns the commit SHA (or abbreviated SHA) as the branch name.
     */
    @Test
    public void testDoCallOnDetachedHead() {
        def commit1 = helper.commitFile("hello.txt", "Hello", "Added hello.txt")
        helper.commitFile("hello2.txt", "Hello2", "Added hello2.txt")

        // Checkout the first commit by its ID (creates detached HEAD state)
        checkoutCommit(commit1.name)

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [:])
            // In detached HEAD state, JGit returns commit SHA or "HEAD"
            String result = prop.doCall(facade)
            // Should be either "HEAD" or a commit SHA (40 chars hex)
            assertTrue("In detached HEAD, result should be HEAD or commit SHA: ${result}",
                       result == "HEAD" || result.matches('[0-9a-f]{7,40}'))
        } finally {
            facade.close()
        }
    }

    /**
     * Test detached HEAD state with Jenkins environment variables.
     */
    @Test
    public void testDoCallOnDetachedHeadWithJenkins() {
        def commit1 = helper.commitFile("hello.txt", "Hello", "Added hello.txt")
        helper.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        checkoutCommit(commit1.name)

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [JOB_NAME: 'MyJob', GIT_BRANCH: 'feature/my-feature'])
            assertEquals("feature/my-feature", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    // =============================================================================
    // Modern CI Environment Tests (#263)
    // =============================================================================

    @Test
    public void testDoCallOnGitHubActions() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [
                GITHUB_ACTIONS: 'true',
                GITHUB_REF_NAME: 'feature/github-branch'
            ])
            assertEquals("feature/github-branch", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnGitHubActionsPullRequest() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [
                GITHUB_ACTIONS: 'true',
                GITHUB_HEAD_REF: 'feature/pr-source-branch',
                GITHUB_REF_NAME: '123/merge'
            ])
            assertEquals("feature/pr-source-branch", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnCircleCI() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [
                CIRCLECI: 'true',
                CIRCLE_BRANCH: 'feature/circle-branch'
            ])
            assertEquals("feature/circle-branch", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnAzureDevOps() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [
                TF_BUILD: 'True',
                BUILD_SOURCEBRANCH: 'refs/heads/feature/azure-branch'
            ])
            assertEquals("feature/azure-branch", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnBitbucketPipelines() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [
                BITBUCKET_BUILD_NUMBER: '123',
                BITBUCKET_BRANCH: 'feature/bitbucket-branch'
            ])
            assertEquals("feature/bitbucket-branch", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnAWSCodeBuildWebhookHeadRef() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [
                CODEBUILD_BUILD_ARN: 'arn:aws:codebuild:us-east-1:123456789:build/my-project:build-id',
                CODEBUILD_WEBHOOK_HEAD_REF: 'refs/heads/feature/webhook-branch',
                CODEBUILD_WEBHOOK_TRIGGER: 'branch/feature/webhook-branch',
                CODEBUILD_SOURCE_VERSION: 'abc123def456'
            ])
            assertEquals("feature/webhook-branch", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }

    @Test
    public void testDoCallOnAWSCodeBuildWebhookTrigger() {
        helper.commitFile("hello.txt", "Hello", "Added hello.txt")

        def facade = GitFacade.open(projectDir)
        try {
            BranchProperty prop = getTestObject(null, [
                CODEBUILD_BUILD_ARN: 'arn:aws:codebuild:us-east-1:123456789:build/my-project:build-id',
                CODEBUILD_WEBHOOK_TRIGGER: 'branch/feature/trigger-branch',
                CODEBUILD_SOURCE_VERSION: 'abc123def456'
            ])
            assertEquals("feature/trigger-branch", prop.doCall(facade))
        } finally {
            facade.close()
        }
    }
}
