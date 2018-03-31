package com.gorylenko

import org.ajoberstar.grgit.Grgit

class CommitIdDescribeProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : commitIdDescribe(repo, '-dirty')
    }

    private String commitIdDescribe(Grgit repo, String dirtyMark) {
        String describe = repo.describe()
        if (describe && !repo.status().clean) {
            describe += dirtyMark
        }
        return describe ?: ''
    }

}
