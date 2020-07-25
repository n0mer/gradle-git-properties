package com.gorylenko.properties

import org.ajoberstar.grgit.Grgit

class ClosestTagNameProperty extends AbstractGitProperty {
    CacheSupport cacheSupport
    ClosestTagNameProperty(CacheSupport cacheSupport) {
        this.cacheSupport = cacheSupport
    }

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : closestTagName(repo)
    }

    String closestTagName(Grgit repo) {
        try {

            String describe = this.cacheSupport.describe(repo, true)
            if (describe) {
                // remove commit ID
                describe = describe.substring(0, describe.lastIndexOf('-'))
                // remove commit number
                describe = describe.substring(0, describe.lastIndexOf('-'))
            }
            return describe ?: ''

        } catch (org.eclipse.jgit.api.errors.JGitInternalException e) {
            if (isShallowClone(repo)) {
                // shallow clone, use value ""
                return ''
            } else {
                throw e;
            }
        }
    }

    boolean isShallowClone(Grgit repo) {
        File shallow =  new File(repo.repository.rootDir, ".git/shallow")
        return shallow.exists()
    }
}
