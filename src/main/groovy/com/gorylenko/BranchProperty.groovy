package com.gorylenko

import org.ajoberstar.grgit.Grgit

class BranchProperty extends Closure<String> {

    BranchProperty() {
        super(null)
    }

    String doCall(Grgit repo) {
        String branchName = null
        // Try to detect git branch from environment variables if executed by Hudson/Jenkins
        // See https://wiki.jenkins.io/display/JENKINS/Git+Plugin
        def env = System.getenv()
        if (env.containsKey('JOB_NAME')) {
            branchName = env["GIT_LOCAL_BRANCH"] ?: env["GIT_BRANCH"]
        }
        if (!branchName) {
            branchName = repo.branch.current.name
        }
        return branchName
    }
}
