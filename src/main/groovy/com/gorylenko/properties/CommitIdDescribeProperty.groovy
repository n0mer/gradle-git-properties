package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class CommitIdDescribeProperty extends AbstractGitProperty {

    String doCall(GitFacade facade) {
        return isEmpty(facade) ? '' : commitIdDescribe(facade, '-dirty')
    }

    private String commitIdDescribe(GitFacade facade, String dirtyMark) {
        String describe
        try {
            describe = facade.describe()
            if (describe == null && isShallowClone(facade)) {
                // shallow clone, use the fallback value "<commit id>"
                describe = facade.head().abbreviatedId
            }
        } catch (org.eclipse.jgit.api.errors.JGitInternalException e) {
            if (isShallowClone(facade)) {
                // shallow clone, use the fallback value "<commit id>"
                describe = facade.head().abbreviatedId
            } else {
                throw e;
            }
        }

        if (describe && !facade.status().clean) {
            describe += dirtyMark
        }
        return describe ?: ''
    }

    private boolean isShallowClone(GitFacade facade) {
        // Check both worktree and regular repo locations for shallow file
        def gitDir = facade.jgit.directory
        def shallow = new File(gitDir, "shallow")
        if (shallow.exists()) {
            return true
        }
        // For worktrees, check the commondir file to find main repo
        def commonDirFile = new File(gitDir, "commondir")
        if (commonDirFile.exists()) {
            def commonDirPath = commonDirFile.text.trim()
            def commonDir = new File(gitDir, commonDirPath).canonicalFile
            shallow = new File(commonDir, "shallow")
            return shallow.exists()
        }
        return false
    }

}
