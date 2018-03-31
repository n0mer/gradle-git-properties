package com.gorylenko

import org.ajoberstar.grgit.Grgit

class CommitUserNameProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : repo.head().author.name
    }
}
