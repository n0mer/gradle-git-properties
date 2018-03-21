package com.gorylenko

import org.ajoberstar.grgit.Grgit

class CommitIdDescribeProperty extends Closure<String>{

    CommitIdDescribeProperty() {
        super(null)
    }

    String doCall(Grgit repo) {
        return commitIdDescribe(repo, '-dirty')
    }

    private String commitIdDescribe(Grgit repo, String dirtyMark) {

        String describe = repo.describe()
        if (describe && !repo.status().clean) {
            describe += dirtyMark
        }
        return describe ?: ''
    }

}
