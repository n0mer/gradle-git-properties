package com.gorylenko.properties

import org.ajoberstar.grgit.Grgit

class BranchProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        String branchName = null
        // Try to detect git branch from environment variables if executed by Hudson/Jenkins
        // See https://wiki.jenkins.io/display/JENKINS/Git+Plugin
        Map<String, String> env = getEnv()
        if (env.containsKey('JOB_NAME')) {
            branchName = env["GIT_LOCAL_BRANCH"] ?: env["GIT_BRANCH"]
        }
        if (!branchName && !isEmpty(repo)) {
            branchName = repo.branch.current().name
        }
        return branchName ?: ''
    }

    Map<String, String> getEnv() {
        return System.getenv()
    }
}
