package com.gorylenko.properties

import org.ajoberstar.grgit.Grgit

class CommitMessageShortProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : repo.head().shortMessage
    }
}
