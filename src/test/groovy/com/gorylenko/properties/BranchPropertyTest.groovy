package com.gorylenko.properties

import static org.junit.Assert.*

import java.util.Map

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Person
import org.junit.After
import org.junit.Before
import org.junit.Test

class BranchPropertyTest {

    File projectDir
    Grgit repo

    @Before
    public void setUp() throws Exception {

        // Set up projectDir

        projectDir = File.createTempDir("BranchPropertyTest", ".tmp")

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // empty repo
        })


        // Set up repo
        repo = Grgit.open(dir: projectDir)

    }

    @After
    public void tearDown() throws Exception {
        repo?.close()
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
        BranchProperty prop = getTestObject(null, [:])
        assertEquals('', prop.doCall(repo))
    }

    @Test
    public void testDoCallOnEmptyRepoWithUserDefinedBranch() {
        BranchProperty prop = getTestObject("mybranch", [:])
        assertEquals('mybranch', prop.doCall(repo))
    }

    @Test
    public void testDoCallOnMasterBranch() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->

            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")

            // create a new branch "branch-1" at current location
            gitRepoBuilder.addBranch("branch-1")

            // commit 1 new file "hello2.txt"
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })

        BranchProperty prop = getTestObject(null, [:])
        assertEquals("master", prop.doCall(repo))
    }

    @Test
    public void testDoCallOnBranch1() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->

            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")

            // create a new branch "branch-1" at current location
            gitRepoBuilder.addBranch("branch-1")

            // commit 1 new file "hello2.txt"
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })

        repo.checkout (branch : "branch-1")

        BranchProperty prop = getTestObject(null, [:])
        assertEquals("branch-1", prop.doCall(repo))
    }

    @Test
    public void testDoCallWithUserDefinedBranch() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->

            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")

            // create a new branch "branch-1" at current location
            gitRepoBuilder.addBranch("branch-1")

            // commit 1 new file "hello2.txt"
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })

        assertEquals("mybranch", getTestObject("mybranch", [:]).doCall(repo))
        assertEquals("mybranch", getTestObject("mybranch", [JOB_NAME: 'MyJob', GIT_LOCAL_BRANCH: 'local-branch']).doCall(repo))
        assertEquals("mybranch", getTestObject("mybranch", [JOB_NAME: 'MyJob', GIT_BRANCH: 'git-branch']).doCall(repo))
        assertEquals("mybranch", getTestObject("mybranch", [TRAVIS: 'true', TRAVIS_BRANCH: 'local-branch']).doCall(repo))
        assertEquals("mybranch", getTestObject("mybranch", [TEAMCITY_VERSION: '1', 'teamcity.build.branch': 'local-branch']).doCall(repo))
        assertEquals("mybranch", getTestObject("mybranch", [GITLAB_CI: 'true', 'CI_COMMIT_REF_NAME': 'local-branch']).doCall(repo))
        assertEquals("mybranch", getTestObject("mybranch", [BAMBOO_BUILDKEY: 'true', 'BAMBOO_PLANREPOSITORY_BRANCH': 'local-branch']).doCall(repo))
    }

    @Test
    public void testDoCallOnJenkinsServer() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->

            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")

            // create a new branch "branch-1" at current location
            gitRepoBuilder.addBranch("branch-1")

        })

        repo.checkout (branch : "master")

        BranchProperty prop = getTestObject(null, [JOB_NAME: 'MyJob', GIT_LOCAL_BRANCH: 'local-branch'])
        assertEquals("local-branch", prop.doCall(repo))


        BranchProperty prop2 = getTestObject(null, [JOB_NAME: 'MyJob', GIT_BRANCH: 'git-branch'])
        assertEquals("git-branch", prop2.doCall(repo))
    }

    @Test
    public void testDoCallOnTravisServer() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->

            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")

            // create a new branch "branch-1" at current location
            gitRepoBuilder.addBranch("branch-1")

        })

        repo.checkout (branch : "master")

        BranchProperty prop = getTestObject(null, [TRAVIS: 'true', TRAVIS_BRANCH: 'local-branch'])
        assertEquals("local-branch", prop.doCall(repo))


    }

    @Test
    public void testDoCallOnTeamCityServer() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->

            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")

            // create a new branch "branch-1" at current location
            gitRepoBuilder.addBranch("branch-1")

        })

        repo.checkout (branch : "master")

        BranchProperty prop = getTestObject(null, [TEAMCITY_VERSION: '1', 'teamcity.build.branch': 'local-branch'])
        assertEquals("local-branch", prop.doCall(repo))


    }

    @Test
    public void testDoCallOnGitlab() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->

            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")

            // create a new branch "branch-1" at current location
            gitRepoBuilder.addBranch("branch-1")

        })

        repo.checkout (branch : "master")

        BranchProperty prop = getTestObject(null, [GITLAB_CI: 'true', 'CI_COMMIT_REF_NAME': 'local-branch'])
        assertEquals("local-branch", prop.doCall(repo))
    }

    @Test
    public void testDoCallOnBamboo() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->

            // commit 1 new file "hello.txt"
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")

            // create a new branch "branch-1" at current location
            gitRepoBuilder.addBranch("branch-1")

        })

        repo.checkout (branch : "master")

        BranchProperty prop = getTestObject(null, [BAMBOO_BUILDKEY: 'true', 'BAMBOO_PLANREPOSITORY_BRANCH': 'local-branch'])
        assertEquals("local-branch", prop.doCall(repo))
    }

    /**
     * Helper method to checkout a specific commit (creates detached HEAD state).
     * Uses underlying JGit since grgit doesn't support revision checkout directly.
     */
    private void checkoutCommit(String commitId) {
        repo.repository.jgit.checkout()
            .setName(commitId)
            .call()
    }

    /**
     * Test detached HEAD state - when checking out a specific commit instead of a branch.
     * This addresses issue #265 and relates to issues #222, #150, #109.
     *
     * In detached HEAD state, repo.branch.current().name returns "HEAD".
     */
    @Test
    public void testDoCallOnDetachedHead() {

        String commitId = null
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            // commit 1 new file "hello.txt"
            def commit = gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            commitId = commit.id

            // commit another file to move HEAD forward
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })

        // Checkout the first commit by its ID (creates detached HEAD state)
        checkoutCommit(commitId)

        BranchProperty prop = getTestObject(null, [:])
        // In detached HEAD state, grgit returns "HEAD" as the branch name
        String result = prop.doCall(repo)
        assertEquals("HEAD", result)
    }

    /**
     * Test detached HEAD state with Jenkins environment variables.
     * CI systems should provide the actual branch name even in detached HEAD state.
     */
    @Test
    public void testDoCallOnDetachedHeadWithJenkins() {

        String commitId = null
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            def commit = gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            commitId = commit.id
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })

        // Checkout the first commit (detached HEAD)
        checkoutCommit(commitId)

        // Jenkins provides the branch name via environment variable
        BranchProperty prop = getTestObject(null, [JOB_NAME: 'MyJob', GIT_BRANCH: 'feature/my-feature'])
        assertEquals("feature/my-feature", prop.doCall(repo))

        // GIT_LOCAL_BRANCH takes priority over GIT_BRANCH
        BranchProperty prop2 = getTestObject(null, [JOB_NAME: 'MyJob', GIT_LOCAL_BRANCH: 'develop', GIT_BRANCH: 'origin/develop'])
        assertEquals("develop", prop2.doCall(repo))
    }

    /**
     * Test detached HEAD state with GitLab CI environment variables.
     */
    @Test
    public void testDoCallOnDetachedHeadWithGitlab() {

        String commitId = null
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            def commit = gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            commitId = commit.id
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })

        // Checkout the first commit (detached HEAD)
        checkoutCommit(commitId)

        // GitLab CI provides the branch name
        BranchProperty prop = getTestObject(null, [GITLAB_CI: 'true', CI_COMMIT_REF_NAME: 'main'])
        assertEquals("main", prop.doCall(repo))
    }

    /**
     * Test detached HEAD state with Travis CI environment variables.
     */
    @Test
    public void testDoCallOnDetachedHeadWithTravis() {

        String commitId = null
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            def commit = gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            commitId = commit.id
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })

        // Checkout the first commit (detached HEAD)
        checkoutCommit(commitId)

        // Travis CI provides the branch name
        BranchProperty prop = getTestObject(null, [TRAVIS: 'true', TRAVIS_BRANCH: 'release/v1.0'])
        assertEquals("release/v1.0", prop.doCall(repo))
    }

    /**
     * Test detached HEAD state with TeamCity environment variables.
     * This specifically addresses issue #222.
     */
    @Test
    public void testDoCallOnDetachedHeadWithTeamCity() {

        String commitId = null
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            def commit = gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            commitId = commit.id
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })

        // Checkout the first commit (detached HEAD)
        checkoutCommit(commitId)

        // TeamCity provides the branch name - refs/heads/ prefix is stripped
        BranchProperty prop = getTestObject(null, [TEAMCITY_VERSION: '2023.05', 'teamcity.build.branch': 'refs/heads/main'])
        assertEquals("main", prop.doCall(repo))
    }

    /**
     * Test detached HEAD state with user-defined branch - should always use user value.
     */
    @Test
    public void testDoCallOnDetachedHeadWithUserDefinedBranch() {

        String commitId = null
        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            def commit = gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
            commitId = commit.id
            gitRepoBuilder.commitFile("hello2.txt", "Hello2", "Added hello2.txt")
        })

        // Checkout the first commit (detached HEAD)
        checkoutCommit(commitId)

        // User-defined branch takes priority over everything
        BranchProperty prop = getTestObject("custom-branch", [:])
        assertEquals("custom-branch", prop.doCall(repo))

        // Even with CI env vars, user-defined branch wins
        BranchProperty prop2 = getTestObject("custom-branch", [JOB_NAME: 'MyJob', GIT_BRANCH: 'jenkins-branch'])
        assertEquals("custom-branch", prop2.doCall(repo))
    }

    // =============================================================================
    // Modern CI Environment Tests (#263)
    // =============================================================================

    /**
     * Test GitHub Actions environment variables for regular push builds.
     * GitHub Actions sets: GITHUB_ACTIONS=true, GITHUB_REF_NAME
     * See: https://docs.github.com/en/actions/learn-github-actions/variables
     */
    @Test
    public void testDoCallOnGitHubActions() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        // GitHub Actions regular push - uses GITHUB_REF_NAME
        BranchProperty prop = getTestObject(null, [
            GITHUB_ACTIONS: 'true',
            GITHUB_REF_NAME: 'feature/github-branch'
        ])

        assertEquals("feature/github-branch", prop.doCall(repo))
    }

    /**
     * Test GitHub Actions environment variables for pull request builds.
     * For PRs, GITHUB_HEAD_REF contains the source branch and takes priority.
     */
    @Test
    public void testDoCallOnGitHubActionsPullRequest() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        // GitHub Actions PR build - GITHUB_HEAD_REF takes priority over GITHUB_REF_NAME
        BranchProperty prop = getTestObject(null, [
            GITHUB_ACTIONS: 'true',
            GITHUB_HEAD_REF: 'feature/pr-source-branch',
            GITHUB_REF_NAME: '123/merge'  // PR merge ref - not useful
        ])

        assertEquals("feature/pr-source-branch", prop.doCall(repo))
    }

    /**
     * Test CircleCI environment variables.
     * CircleCI sets: CIRCLECI=true, CIRCLE_BRANCH
     * See: https://circleci.com/docs/variables/
     */
    @Test
    public void testDoCallOnCircleCI() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        BranchProperty prop = getTestObject(null, [
            CIRCLECI: 'true',
            CIRCLE_BRANCH: 'feature/circle-branch'
        ])

        assertEquals("feature/circle-branch", prop.doCall(repo))
    }

    /**
     * Test Azure DevOps Pipelines environment variables.
     * Azure DevOps sets: TF_BUILD=True, BUILD_SOURCEBRANCH (refs/heads/branch format)
     * See: https://learn.microsoft.com/en-us/azure/devops/pipelines/build/variables
     */
    @Test
    public void testDoCallOnAzureDevOps() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        // Azure DevOps - BUILD_SOURCEBRANCH has refs/heads/ prefix which should be stripped
        BranchProperty prop = getTestObject(null, [
            TF_BUILD: 'True',
            BUILD_SOURCEBRANCH: 'refs/heads/feature/azure-branch'
        ])

        assertEquals("feature/azure-branch", prop.doCall(repo))
    }

    /**
     * Test Azure DevOps with tag build.
     * Tag builds return refs/tags/v1.0.0 format.
     */
    @Test
    public void testDoCallOnAzureDevOpsTagBuild() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        BranchProperty prop = getTestObject(null, [
            TF_BUILD: 'True',
            BUILD_SOURCEBRANCH: 'refs/tags/v1.0.0'
        ])

        assertEquals("v1.0.0", prop.doCall(repo))
    }

    /**
     * Test Bitbucket Pipelines environment variables.
     * Bitbucket sets: BITBUCKET_BUILD_NUMBER, BITBUCKET_BRANCH
     * See: https://support.atlassian.com/bitbucket-cloud/docs/variables-and-secrets/
     */
    @Test
    public void testDoCallOnBitbucketPipelines() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        BranchProperty prop = getTestObject(null, [
            BITBUCKET_BUILD_NUMBER: '123',
            BITBUCKET_BRANCH: 'feature/bitbucket-branch'
        ])

        assertEquals("feature/bitbucket-branch", prop.doCall(repo))
    }

    /**
     * Test AWS CodeBuild environment variables with WEBHOOK_HEAD_REF (highest priority).
     * For webhook-triggered builds, CODEBUILD_WEBHOOK_HEAD_REF contains refs/heads/branch.
     * See: https://docs.aws.amazon.com/codebuild/latest/userguide/build-env-ref-env-vars.html
     */
    @Test
    public void testDoCallOnAWSCodeBuildWebhookHeadRef() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        // WEBHOOK_HEAD_REF takes priority over WEBHOOK_TRIGGER and SOURCE_VERSION
        BranchProperty prop = getTestObject(null, [
            CODEBUILD_BUILD_ARN: 'arn:aws:codebuild:us-east-1:123456789:build/my-project:build-id',
            CODEBUILD_WEBHOOK_HEAD_REF: 'refs/heads/feature/webhook-branch',
            CODEBUILD_WEBHOOK_TRIGGER: 'branch/feature/webhook-branch',
            CODEBUILD_SOURCE_VERSION: 'abc123def456'
        ])

        assertEquals("feature/webhook-branch", prop.doCall(repo))
    }

    /**
     * Test AWS CodeBuild environment variables with WEBHOOK_TRIGGER (second priority).
     * For webhook-triggered builds, CODEBUILD_WEBHOOK_TRIGGER contains branch/name or tag/name.
     */
    @Test
    public void testDoCallOnAWSCodeBuildWebhookTrigger() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        // WEBHOOK_TRIGGER takes priority over SOURCE_VERSION when HEAD_REF is not set
        BranchProperty prop = getTestObject(null, [
            CODEBUILD_BUILD_ARN: 'arn:aws:codebuild:us-east-1:123456789:build/my-project:build-id',
            CODEBUILD_WEBHOOK_TRIGGER: 'branch/feature/trigger-branch',
            CODEBUILD_SOURCE_VERSION: 'abc123def456'
        ])

        assertEquals("feature/trigger-branch", prop.doCall(repo))
    }

    /**
     * Test AWS CodeBuild with tag trigger via WEBHOOK_TRIGGER.
     */
    @Test
    public void testDoCallOnAWSCodeBuildTagTrigger() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        BranchProperty prop = getTestObject(null, [
            CODEBUILD_BUILD_ARN: 'arn:aws:codebuild:us-east-1:123456789:build/my-project:build-id',
            CODEBUILD_WEBHOOK_TRIGGER: 'tag/v1.2.3'
        ])

        assertEquals("v1.2.3", prop.doCall(repo))
    }

    /**
     * Test AWS CodeBuild environment variables with SOURCE_VERSION fallback.
     * For non-webhook builds, only CODEBUILD_SOURCE_VERSION is available.
     */
    @Test
    public void testDoCallOnAWSCodeBuildSourceVersion() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        // SOURCE_VERSION used as fallback when webhook vars not available
        BranchProperty prop = getTestObject(null, [
            CODEBUILD_BUILD_ARN: 'arn:aws:codebuild:us-east-1:123456789:build/my-project:build-id',
            CODEBUILD_SOURCE_VERSION: 'feature/codebuild-branch'
        ])

        assertEquals("feature/codebuild-branch", prop.doCall(repo))
    }

    /**
     * Test AWS CodeBuild PR build scenario.
     * For PR builds via webhook, WEBHOOK_TRIGGER contains "pr/123" format.
     * This is returned as-is (not stripped) since it's not a branch name.
     */
    @Test
    public void testDoCallOnAWSCodeBuildPullRequest() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        // PR builds have pr/number format - not stripped since it's not a branch
        BranchProperty prop = getTestObject(null, [
            CODEBUILD_BUILD_ARN: 'arn:aws:codebuild:us-east-1:123456789:build/my-project:build-id',
            CODEBUILD_WEBHOOK_TRIGGER: 'pr/42'
        ])

        // Returns "pr/42" as-is - user may want to handle PR builds differently
        assertEquals("pr/42", prop.doCall(repo))
    }

    /**
     * Test AWS CodeBuild with commit SHA as SOURCE_VERSION.
     * For builds started via API/console, SOURCE_VERSION may be a commit SHA.
     */
    @Test
    public void testDoCallOnAWSCodeBuildCommitSha() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        // When started via API/console, SOURCE_VERSION might be a commit SHA
        BranchProperty prop = getTestObject(null, [
            CODEBUILD_BUILD_ARN: 'arn:aws:codebuild:us-east-1:123456789:build/my-project:build-id',
            CODEBUILD_SOURCE_VERSION: 'abc123def456789012345678901234567890abcd'
        ])

        // Returns commit SHA as-is - this is expected behavior for non-branch builds
        assertEquals("abc123def456789012345678901234567890abcd", prop.doCall(repo))
    }

    /**
     * Test AWS CodeBuild with empty WEBHOOK_HEAD_REF falls through to WEBHOOK_TRIGGER.
     */
    @Test
    public void testDoCallOnAWSCodeBuildEmptyWebhookHeadRef() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        // Empty WEBHOOK_HEAD_REF should fall through to WEBHOOK_TRIGGER
        BranchProperty prop = getTestObject(null, [
            CODEBUILD_BUILD_ARN: 'arn:aws:codebuild:us-east-1:123456789:build/my-project:build-id',
            CODEBUILD_WEBHOOK_HEAD_REF: '',
            CODEBUILD_WEBHOOK_TRIGGER: 'branch/develop',
            CODEBUILD_SOURCE_VERSION: 'abc123'
        ])

        assertEquals("develop", prop.doCall(repo))
    }

    /**
     * Test AWS CodeBuild with empty WEBHOOK_TRIGGER falls through to SOURCE_VERSION.
     */
    @Test
    public void testDoCallOnAWSCodeBuildEmptyWebhookTrigger() {

        GitRepositoryBuilder.setupProjectDir(projectDir, { gitRepoBuilder ->
            gitRepoBuilder.commitFile("hello.txt", "Hello", "Added hello.txt")
        })

        // Empty webhook vars should fall through to SOURCE_VERSION
        BranchProperty prop = getTestObject(null, [
            CODEBUILD_BUILD_ARN: 'arn:aws:codebuild:us-east-1:123456789:build/my-project:build-id',
            CODEBUILD_WEBHOOK_HEAD_REF: '',
            CODEBUILD_WEBHOOK_TRIGGER: '',
            CODEBUILD_SOURCE_VERSION: 'main'
        ])

        assertEquals("main", prop.doCall(repo))
    }

}
