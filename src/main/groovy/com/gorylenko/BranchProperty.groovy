package com.gorylenko

import org.ajoberstar.grgit.Grgit

class BranchProperty extends Closure<String> {

    BranchProperty() {
        super(null)
    }

    String doCall(Grgit repo) {
        String branchName = null
        // Try to detect git branch from environment variables if executed by Hudson/Jenkins
        // See https://github.com/ktoso/maven-git-commit-id-plugin/blob/master/src/main/java/pl/project13/maven/git/GitDataProvider.java#L170
        Map<String, String> env = System.getenv()
        if (env.containsKey("HUDSON_URL") || env.containsKey("JENKINS_URL") ||
                env.containsKey("HUDSON_HOME") || env.containsKey("JENKINS_HOME")) {
            branchName = env.get("GIT_LOCAL_BRANCH")
            if (!(branchName?.trim())) {
                branchName = env.get("GIT_BRANCH")
            }
        }
        if (!(branchName?.trim())) {
            branchName = repo.branch.current.name
        }
        return branchName
    }
}
