package com.gorylenko.properties

import org.ajoberstar.grgit.Grgit

class BranchProperty extends AbstractGitProperty {
    String branch
    Map<String, String> env

    def branchEnvs = [

        'JOB_NAME' : ['GIT_LOCAL_BRANCH', 'GIT_BRANCH', 'BRANCH_NAME'], // jenkins/hudson

        'TRAVIS' : ['TRAVIS_BRANCH'], // TravisCI https://docs.travis-ci.com/user/environment-variables/#default-environment-variables

        'TEAMCITY_VERSION' : ['teamcity.build.branch'], // https://confluence.jetbrains.com/display/TCD9/Predefined+Build+Parameters

        'GITLAB_CI' : ['CI_COMMIT_REF_NAME'], // https://docs.gitlab.com/ee/ci/variables/#predefined-variables-environment-variables

        'BAMBOO_BUILDKEY' : ['BAMBOO_PLANREPOSITORY_BRANCH'], // https://confluence.atlassian.com/bamboo/bamboo-variables-289277087.html

        // Modern CI systems (issue #263)
        'GITHUB_ACTIONS' : ['GITHUB_HEAD_REF', 'GITHUB_REF_NAME'], // GitHub Actions - HEAD_REF for PRs, REF_NAME for pushes
                                                                   // https://docs.github.com/en/actions/learn-github-actions/variables

        'CIRCLECI' : ['CIRCLE_BRANCH'], // CircleCI https://circleci.com/docs/variables/

        'TF_BUILD' : ['BUILD_SOURCEBRANCH'], // Azure DevOps - returns refs/heads/branch, stripped below
                                              // https://learn.microsoft.com/en-us/azure/devops/pipelines/build/variables

        'BITBUCKET_BUILD_NUMBER' : ['BITBUCKET_BRANCH'], // Bitbucket Pipelines
                                                          // https://support.atlassian.com/bitbucket-cloud/docs/variables-and-secrets/

        'CODEBUILD_BUILD_ARN' : ['CODEBUILD_WEBHOOK_HEAD_REF', 'CODEBUILD_WEBHOOK_TRIGGER', 'CODEBUILD_SOURCE_VERSION']
                                // AWS CodeBuild - WEBHOOK_HEAD_REF (refs/heads/branch), WEBHOOK_TRIGGER (branch/name),
                                // SOURCE_VERSION (fallback, may be commit SHA)
                                // https://docs.aws.amazon.com/codebuild/latest/userguide/build-env-ref-env-vars.html

        ]
    BranchProperty(String branch) {
        this.branch = branch
    }

    String doCall(Grgit repo) {

        String branchName

        // if user didn't provide a branch name, try to detect using environment variables
        if (this.branch == null) {

            branchName = null
            Map<String, String> env = getEnv()
            outer: for ( e in branchEnvs ) {
                if (env.containsKey(e.key)) {
                    for (String name in e.value) {
                        // Check key exists AND value is non-empty (handles empty string env vars)
                        if (env.containsKey(name) && env[name]) {
                            branchName = env[name]
                            break outer
                        }
                    }
                }
            }

            // could not detect from env variables, use branch from repo
            if (!branchName && !isEmpty(repo)) {
                branchName = repo.branch.current().name
            }

            // Strip git ref prefixes (e.g., Azure DevOps returns refs/heads/branch-name)
            branchName = stripRefPrefix(branchName)

        } else {
            // user provided a branch value, use it
            branchName = this.branch
        }

        return branchName ?: ''
    }

    Map<String, String> getEnv() {
        return env != null ? env : System.getenv()
    }

	void setEnv(env) {
		this.env = env
	}

    /**
     * Strip git ref prefixes from branch names.
     * Some CI systems return full refs:
     * - Azure DevOps: "refs/heads/branch-name" or "refs/tags/tag-name"
     * - AWS CodeBuild WEBHOOK_TRIGGER: "branch/branch-name" or "tag/tag-name"
     */
    private static String stripRefPrefix(String branchName) {
        if (branchName == null) {
            return null
        }
        if (branchName.startsWith('refs/heads/')) {
            return branchName.substring('refs/heads/'.length())
        }
        if (branchName.startsWith('refs/tags/')) {
            return branchName.substring('refs/tags/'.length())
        }
        // AWS CodeBuild WEBHOOK_TRIGGER format: "branch/name" or "tag/name"
        if (branchName.startsWith('branch/')) {
            return branchName.substring('branch/'.length())
        }
        if (branchName.startsWith('tag/')) {
            return branchName.substring('tag/'.length())
        }
        // Don't strip refs/pull/ or pr/ as those are merge refs, not branch names
        return branchName
    }
}
