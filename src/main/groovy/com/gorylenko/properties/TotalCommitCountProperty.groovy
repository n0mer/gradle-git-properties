package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class TotalCommitCountProperty extends AbstractGitProperty {
    CacheSupport cacheSupport
    TotalCommitCountProperty(CacheSupport cacheSupport) {
        this.cacheSupport = cacheSupport
    }

    String doCall(GitFacade facade) {
        return isEmpty(facade) ? '0' : this.cacheSupport.totalCommitCount(facade).toString()
    }
}
