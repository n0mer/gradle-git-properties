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
            gitRepoBuilder
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

}
