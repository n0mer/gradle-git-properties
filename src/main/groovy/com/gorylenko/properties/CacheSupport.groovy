package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade
import groovy.transform.Memoized
import org.eclipse.jgit.lib.Constants

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
        def headId = facade.jgit.resolve(Constants.HEAD)
        if (get(headId) == null) {
            def commits = facade.log()
            put(headId, commits.size())
        }
        return get(headId)
    }
}
