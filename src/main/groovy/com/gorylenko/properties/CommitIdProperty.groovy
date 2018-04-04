package com.gorylenko.properties

import org.ajoberstar.grgit.Grgit

class CommitIdProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : repo.head().id
    }
}
