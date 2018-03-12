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
        String describe
        if (repo.status().clean) {
            describe = repo.head().abbreviatedId
        } else {
            describe = repo.head().abbreviatedId + dirtyMark
        }
    }

}
