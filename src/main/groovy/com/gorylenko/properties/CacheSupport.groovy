package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade
import groovy.transform.Memoized

import java.util.concurrent.ConcurrentHashMap

class CacheSupport {

    private static final Map cache = new ConcurrentHashMap()

    Object get(Object key) {
        return cache.get(key)
    }

    void put(Object key, Object value) {
        cache.put(key, value)
    }

    @Memoized
    String describe(GitFacade facade, boolean longDescr) {
        return facade.describe(longDescr: longDescr)
    }

    Integer totalCommitCount(GitFacade facade) {
        def headId = facade.headId
        if (headId == null) {
            return 0
        }
        if (get(headId) == null) {
            put(headId, facade.countCommits())
        }
        return get(headId)
    }
}
