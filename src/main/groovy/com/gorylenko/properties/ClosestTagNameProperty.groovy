package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class ClosestTagNameProperty extends AbstractGitProperty {
    CacheSupport cacheSupport
    ClosestTagNameProperty(CacheSupport cacheSupport) {
        this.cacheSupport = cacheSupport
    }

    String doCall(GitFacade facade) {
        return isEmpty(facade) ? '' : closestTagName(facade)
    }

    private String closestTagName(GitFacade facade) {
        try {
            // Use long format: tag-N-gSHA
            String describe = this.cacheSupport.describe(facade, true)
            if (describe) {
                // remove commit ID (after last '-g')
                describe = describe.substring(0, describe.lastIndexOf('-'))
                // remove commit number
                describe = describe.substring(0, describe.lastIndexOf('-'))
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
