package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class ClosestTagCommitCountProperty extends AbstractGitProperty {
    CacheSupport cacheSupport
    ClosestTagCommitCountProperty(CacheSupport cacheSupport) {
        this.cacheSupport = cacheSupport
    }

    String doCall(GitFacade facade) {
        return isEmpty(facade) ? '' : closestTagCommitCount(facade)
    }

    private String closestTagCommitCount(GitFacade facade) {
        try {
            // Use long format: tag-N-gSHA
            String describe = this.cacheSupport.describe(facade, true)
            if (describe) {
                // remove commit ID (after last '-g')
                describe = describe.substring(0, describe.lastIndexOf('-'))
                // extract commit count (after tag name)
                describe = describe.substring(describe.lastIndexOf('-') + 1)
            }
            return describe ?: ''
        } catch (org.eclipse.jgit.api.errors.JGitInternalException e) {
            if (isShallowClone(facade)) {
                return ''
            } else {
                throw e
            }
        }
    }

    private boolean isShallowClone(GitFacade facade) {
        def gitDir = facade.jgit.directory
        def shallow = new File(gitDir, "shallow")
        if (shallow.exists()) {
            return true
        }
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
