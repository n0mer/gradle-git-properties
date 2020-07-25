package com.gorylenko.properties

import org.ajoberstar.grgit.Grgit

class TotalCommitCountProperty extends AbstractGitProperty {
    CacheSupport cacheSupport
    TotalCommitCountProperty(CacheSupport cacheSupport) {
        this.cacheSupport = cacheSupport
    }

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '0' : this.cacheSupport.totalCommitCount(repo).toString()
    }
}
