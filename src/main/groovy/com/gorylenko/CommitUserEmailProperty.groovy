package com.gorylenko

import org.ajoberstar.grgit.Grgit

class CommitUserEmailProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : repo.head().author.email
    }
}
