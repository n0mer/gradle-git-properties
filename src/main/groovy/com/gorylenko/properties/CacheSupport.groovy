package com.gorylenko.properties

import groovy.transform.Memoized
import org.ajoberstar.grgit.Grgit
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId

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
    String describe(Grgit repo, boolean longDescr) {
        return repo.describe(longDescr: longDescr)
    }

    Integer totalCommitCount(Grgit repo) {
        ObjectId headId = repo.repository.jgit.repository.resolve(Constants.HEAD)
        if (get(headId) == null) {
            Iterable commits = repo.repository.jgit.log().call()
            int count = 0
            for( Object commit : commits ) {
                count++
            }
            put(headId, count)
        }
        return get(headId)
    }
}
