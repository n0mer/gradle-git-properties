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

        'BAMBOO_BUILDKEY' : ['BAMBOO_PLANREPOSITORY_BRANCH'] //https://confluence.atlassian.com/bamboo/bamboo-variables-289277087.html

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
                        if (env.containsKey(name)) {
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
}
